package com.meshconnect.offlinechat.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.meshconnect.offlinechat.CallActivity;
import com.meshconnect.offlinechat.MainActivity;
import com.meshconnect.offlinechat.R;
import com.meshconnect.offlinechat.network.P2PSocketManager;
import com.meshconnect.offlinechat.network.ServerThread;

import java.io.File;

/**
 * Foreground Service that keeps the offline P2P mesh network socket active in the background.
 * Listens for incoming VoIP calls and messages even when the app is closed or the screen is turned off.
 */
public class MeshForegroundService extends Service implements P2PSocketManager.SocketEventListener {

    private static final String TAG = "MeshForegroundService";
    private static final String CHANNEL_ACTIVE_ID = "mesh_active_channel";
    private static final String CHANNEL_CALL_ID = "mesh_incoming_call_channel";
    private static final int NOTIFICATION_SERVICE_ID = 1001;
    public static final int NOTIFICATION_CALL_ID = 1002;

    private P2PSocketManager socketManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        startForegroundServiceNotification();

        socketManager = P2PSocketManager.getInstance(this);
        socketManager.registerListener(this);
        socketManager.startServer();

        Log.d(TAG, "MeshForegroundService created and listening for background calls.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (socketManager != null) {
            socketManager.startServer();
        }
        return START_STICKY;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            // 1. Ongoing service channel (Low importance, silent)
            NotificationChannel activeChannel = new NotificationChannel(
                    CHANNEL_ACTIVE_ID,
                    "MeshConnect Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            activeChannel.setDescription("Keeps offline direct link active for incoming calls");
            activeChannel.setShowBadge(false);
            manager.createNotificationChannel(activeChannel);

            // 2. High-priority Call channel (Ringtone, Vibration, Heads-up)
            NotificationChannel callChannel = new NotificationChannel(
                    CHANNEL_CALL_ID,
                    "Incoming Offline Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            callChannel.setDescription("Ringing alerts for offline VoIP calls");
            callChannel.enableLights(true);
            callChannel.setLightColor(Color.CYAN);
            callChannel.enableVibration(true);
            callChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            callChannel.setSound(ringtoneUri, audioAttributes);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            manager.createNotificationChannel(callChannel);
        }
    }

    private void startForegroundServiceNotification() {
        Intent appIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ACTIVE_ID)
                .setContentTitle("MeshConnect Offline Link Active")
                .setContentText("Listening for direct peer calls & messages...")
                .setSmallIcon(R.drawable.ic_call_24)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIFICATION_SERVICE_ID, notification);
        }
    }

    @Override
    public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {
        if (callSignal == ServerThread.TYPE_CALL_INVITE) {
            Log.d(TAG, "Background call invite received from " + callerName + " (" + senderIp + "), type: " + callType);
            handleIncomingCall(callerName, callType, senderIp);
        }
    }

    private void handleIncomingCall(String callerName, String callType, String senderIp) {
        // 1. Wake up screen and CPU
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            if (wakeLock != null && wakeLock.isHeld()) {
                try { wakeLock.release(); } catch (Exception ignored) {}
            }
            wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "meshconnect:incoming_call_wake"
            );
            wakeLock.acquire(15000); // Hold for 15s to ensure user sees call
        }

        // 2. Prepare CallActivity intent
        Intent callIntent = new Intent(this, CallActivity.class);
        callIntent.putExtra("EXTRA_PEER_IP", senderIp);
        callIntent.putExtra("EXTRA_PEER_NAME", callerName != null && !callerName.isEmpty() ? callerName : "Incoming Peer");
        callIntent.putExtra("EXTRA_CALL_TYPE", callType != null ? callType : "AUDIO");
        callIntent.putExtra("EXTRA_IS_INCOMING", true);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, NOTIFICATION_CALL_ID, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // 3. Trigger High-Priority Full Screen Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_CALL_ID)
                .setSmallIcon(R.drawable.ic_call_24)
                .setContentTitle("Incoming " + (callType != null ? callType : "VoIP") + " Call")
                .setContentText((callerName != null ? callerName : "Peer") + " is calling you offline...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_CALL_ID, builder.build());
        }

        // 4. Also launch activity directly
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting CallActivity from background service", e);
        }
    }

    @Override public void onHandshakeReceived(String peerName, String senderIp) {}
    @Override public void onMessageReceived(String messageText, String senderIp) {}
    @Override public void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp) {}
    @Override public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {}
    @Override public void onMessageSent(String messageText) {}
    @Override public void onFileSent(String fileName) {}
    @Override public void onNetworkError(String errorMessage) {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socketManager != null) {
            socketManager.unregisterListener(this);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
            wakeLock = null;
        }
        Log.d(TAG, "MeshForegroundService destroyed.");
    }
}
