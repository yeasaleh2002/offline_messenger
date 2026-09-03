package com.meshconnect.offlinechat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meshconnect.offlinechat.call.AudioCallEngine;
import com.meshconnect.offlinechat.call.VideoCallEngine;
import com.meshconnect.offlinechat.network.P2PSocketManager;
import com.meshconnect.offlinechat.network.ServerThread;

import java.io.File;

/**
 * Activity for real-time offline P2P Audio and Video calling over direct Wi-Fi Direct radio links.
 * Handles call signaling (INVITE, ACCEPT, DECLINE, END) with zero server infrastructure.
 */
public class CallActivity extends AppCompatActivity implements P2PSocketManager.SocketEventListener {

    private static final String TAG = "CallActivity";
    private static final long RING_TIMEOUT_MS = 30000; // 30s timeout

    private ImageView ivRemoteVideo;
    private LinearLayout layoutAudioAvatar;
    private ImageView ivCallerAvatar;
    private TextView tvCallPeerName;
    private TextView tvCallStatus;
    private TextView tvCallDuration;
    private CardView cardLocalPreview;
    private SurfaceView surfaceLocalPreview;
    private LinearLayout layoutIncomingActions;
    private FloatingActionButton btnDeclineCall;
    private FloatingActionButton btnAcceptCall;
    private LinearLayout layoutControls;
    private FloatingActionButton btnMuteMic;
    private FloatingActionButton btnSwitchCam;
    private FloatingActionButton btnSpeaker;
    private FloatingActionButton btnEndCall;

    private String peerIp;
    private String peerName;
    private String callType = "AUDIO";
    private boolean isIncoming = false;

    private AudioCallEngine audioEngine;
    private VideoCallEngine videoEngine;
    private P2PSocketManager socketManager;

    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private boolean isCallActive = false;

    private Ringtone ringtone;
    private Vibrator vibrator;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long callStartTime = 0;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCallActive) {
                long elapsed = (SystemClock.elapsedRealtime() - callStartTime) / 1000;
                long minutes = elapsed / 60;
                long seconds = elapsed % 60;
                tvCallDuration.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final Runnable ringTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCallActive && !isFinishing()) {
                tvCallStatus.setText("No Answer");
                Toast.makeText(CallActivity.this, "Peer did not answer.", Toast.LENGTH_SHORT).show();
                endCall(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Wake up screen and show over keyguard/lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        extractIntentData();
        initViews();
        setupSocketListener();

        if (isIncoming) {
            setupIncomingCallUi();
        } else {
            setupOutgoingCall();
        }
    }

    private void extractIntentData() {
        peerIp = getIntent().getStringExtra("EXTRA_PEER_IP");
        peerName = getIntent().getStringExtra("EXTRA_PEER_NAME");
        callType = getIntent().getStringExtra("EXTRA_CALL_TYPE");
        isIncoming = getIntent().getBooleanExtra("EXTRA_IS_INCOMING", false);

        if (peerName == null) peerName = "Connected Peer";
        if (callType == null) callType = "AUDIO";
    }

    private void initViews() {
        ivRemoteVideo = findViewById(R.id.ivRemoteVideo);
        layoutAudioAvatar = findViewById(R.id.layoutAudioAvatar);
        ivCallerAvatar = findViewById(R.id.ivCallerAvatar);
        tvCallPeerName = findViewById(R.id.tvCallPeerName);
        tvCallStatus = findViewById(R.id.tvCallStatus);
        tvCallDuration = findViewById(R.id.tvCallDuration);
        cardLocalPreview = findViewById(R.id.cardLocalPreview);
        surfaceLocalPreview = findViewById(R.id.surfaceLocalPreview);
        layoutIncomingActions = findViewById(R.id.layoutIncomingActions);
        btnDeclineCall = findViewById(R.id.btnDeclineCall);
        btnAcceptCall = findViewById(R.id.btnAcceptCall);
        layoutControls = findViewById(R.id.layoutControls);
        btnMuteMic = findViewById(R.id.btnMuteMic);
        btnSwitchCam = findViewById(R.id.btnSwitchCam);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnEndCall = findViewById(R.id.btnEndCall);

        tvCallPeerName.setText(peerName);

        if ("VIDEO".equals(callType)) {
            cardLocalPreview.setVisibility(View.VISIBLE);
            btnSwitchCam.setVisibility(View.VISIBLE);
            ivRemoteVideo.setVisibility(View.VISIBLE);
        }

        btnEndCall.setOnClickListener(v -> endCall(true));
        btnDeclineCall.setOnClickListener(v -> declineCall());
        btnAcceptCall.setOnClickListener(v -> acceptCall());

        btnMuteMic.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnSwitchCam.setOnClickListener(v -> {
            if (videoEngine != null) {
                videoEngine.switchCamera();
            }
        });
    }

    private void setupSocketListener() {
        socketManager = P2PSocketManager.getInstance(this);
        socketManager.registerListener(this);
        socketManager.startServer();
    }

    private void setupOutgoingCall() {
        layoutIncomingActions.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        tvCallStatus.setText("Calling (Direct Offline Link)...");

        // Check permissions if starting video call
        if ("VIDEO".equals(callType)) {
            boolean hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!hasCamera || !hasAudio) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 203);
            }
        }

        // Send invite packet to peer over port 8888
        if (peerIp != null) {
            socketManager.sendCallInvite(peerIp, android.os.Build.MODEL, "VIDEO".equals(callType));
        }

        // Set 30s ringing timeout
        timerHandler.postDelayed(ringTimeoutRunnable, RING_TIMEOUT_MS);
    }

    private void setupIncomingCallUi() {
        layoutIncomingActions.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.GONE);
        tvCallStatus.setText("Incoming " + callType + " Call...");
        startRinging();
    }

    private void startRinging() {
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, alert);
            if (ringtone != null) {
                ringtone.play();
            }
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 800, 1000, 800};
                vibrator.vibrate(pattern, 1);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error starting ringtone", e);
        }
    }

    private void stopRinging() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                ringtone = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(1002);
            }
        } catch (Exception ignored) {}
    }

    private void acceptCall() {
        if ("VIDEO".equals(callType)) {
            boolean hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!hasCamera || !hasAudio) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 201);
                return;
            }
        } else {
            boolean hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!hasAudio) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 202);
                return;
            }
        }

        layoutIncomingActions.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        timerHandler.removeCallbacks(ringTimeoutRunnable);
        stopRinging();

        if (peerIp != null) {
            socketManager.sendCallSignal(peerIp, ServerThread.TYPE_CALL_ACCEPT);
        }
        startActiveCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = grantResults.length > 0;
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            if (requestCode == 201 || requestCode == 202) {
                acceptCall();
            }
        } else {
            Toast.makeText(this, "Camera/Microphone permission required for call.", Toast.LENGTH_SHORT).show();
            if (requestCode == 201 || requestCode == 202) {
                declineCall();
            }
        }
    }

    private void declineCall() {
        timerHandler.removeCallbacks(ringTimeoutRunnable);
        if (peerIp != null) {
            socketManager.sendCallSignal(peerIp, ServerThread.TYPE_CALL_DECLINE);
        }
        finish();
    }

    private void startActiveCall() {
        if (isCallActive) return;
        timerHandler.removeCallbacks(ringTimeoutRunnable);

        isCallActive = true;
        tvCallStatus.setText("Connected");
        tvCallDuration.setVisibility(View.VISIBLE);
        callStartTime = SystemClock.elapsedRealtime();
        timerHandler.post(timerRunnable);

        // 1. Launch VoIP Audio Engine over UDP (port 8889)
        audioEngine = new AudioCallEngine(this, peerIp);
        audioEngine.startCall();
        audioEngine.setSpeakerphoneOn(isSpeakerOn);

        // 2. Launch Video Engine if Video call (port 8890)
        if ("VIDEO".equals(callType)) {
            cardLocalPreview.setVisibility(View.VISIBLE);
            btnSwitchCam.setVisibility(View.VISIBLE);
            ivRemoteVideo.setVisibility(View.VISIBLE);
            layoutAudioAvatar.setVisibility(View.GONE);

            videoEngine = new VideoCallEngine(this, peerIp, bitmap -> {
                if (ivRemoteVideo != null && bitmap != null) {
                    if (ivRemoteVideo.getVisibility() != View.VISIBLE) {
                        ivRemoteVideo.setVisibility(View.VISIBLE);
                    }
                    if (layoutAudioAvatar.getVisibility() != View.GONE) {
                        layoutAudioAvatar.setVisibility(View.GONE);
                    }
                    ivRemoteVideo.setImageBitmap(bitmap);
                }
            });
            videoEngine.setupLocalPreview(surfaceLocalPreview);
            videoEngine.start();
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (audioEngine != null) {
            audioEngine.setMuted(isMuted);
        }
        btnMuteMic.setBackgroundTintList(ContextCompat.getColorStateList(this, isMuted ? R.color.status_offline : R.color.secondary_dark));
        Toast.makeText(this, isMuted ? "Microphone muted" : "Microphone unmuted", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        if (audioEngine != null) {
            audioEngine.setSpeakerphoneOn(isSpeakerOn);
        }
        btnSpeaker.setBackgroundTintList(ContextCompat.getColorStateList(this, isSpeakerOn ? R.color.secondary_dark : R.color.status_offline));
    }

    private void endCall(boolean notifyPeer) {
        timerHandler.removeCallbacks(ringTimeoutRunnable);
        timerHandler.removeCallbacks(timerRunnable);

        if (notifyPeer && peerIp != null && socketManager != null) {
            socketManager.sendCallSignal(peerIp, ServerThread.TYPE_CALL_END);
        }

        isCallActive = false;

        if (audioEngine != null) {
            audioEngine.stopCall();
            audioEngine = null;
        }

        if (videoEngine != null) {
            videoEngine.stop();
            videoEngine = null;
        }

        tvCallStatus.setText("Call Ended");
        timerHandler.postDelayed(this::finish, 600);
    }

    // =========================================================================
    // Call Signaling Callbacks
    // =========================================================================

    @Override
    public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {
        Log.d(TAG, "Call signaling received: " + callSignal + " from " + senderIp);
        if (callSignal == ServerThread.TYPE_CALL_ACCEPT) {
            if (!isCallActive) {
                Log.d(TAG, "Call accepted by peer -> starting engines.");
                startActiveCall();
            }
        } else if (callSignal == ServerThread.TYPE_CALL_DECLINE) {
            Toast.makeText(this, "Call declined by peer", Toast.LENGTH_SHORT).show();
            endCall(false);
        } else if (callSignal == ServerThread.TYPE_CALL_END) {
            Toast.makeText(this, "Call ended by peer", Toast.LENGTH_SHORT).show();
            endCall(false);
        }
    }

    @Override public void onHandshakeReceived(String peerName, String senderIp) {}
    @Override public void onMessageReceived(String messageText, String senderIp) {}
    @Override public void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp) {}
    @Override public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {}
    @Override public void onMessageSent(String messageText) {}
    @Override public void onFileSent(String fileName) {}
    @Override public void onNetworkError(String errorMessage) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRinging();
        endCall(false);
        if (socketManager != null) {
            socketManager.unregisterListener(this);
        }
    }
}
