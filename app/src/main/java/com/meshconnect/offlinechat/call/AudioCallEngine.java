package com.meshconnect.offlinechat.call;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * High-performance real-time VoIP audio engine running over UDP socket (port 8889)
 * with low-latency AudioRecord microphone capture and AudioTrack speaker playback.
 */
public class AudioCallEngine {

    private static final String TAG = "AudioCallEngine";
    public static final int AUDIO_PORT = 8889;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final Context context;
    private final String peerIp;
    private volatile boolean isRunning = false;
    private volatile boolean isMuted = false;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private DatagramSocket udpSocket;
    private Thread captureThread;
    private Thread playThread;

    public AudioCallEngine(Context context, String peerIp) {
        this.context = context.getApplicationContext();
        this.peerIp = peerIp;
    }

    @SuppressLint("MissingPermission")
    public synchronized void startCall() {
        if (isRunning) return;
        isRunning = true;

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_ENCODING);
        int bufferSize = Math.max(minBufferSize, 1024);

        // 1. Initialize AudioRecord (Microphone)
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    AUDIO_ENCODING,
                    bufferSize
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Missing RECORD_AUDIO permission for call", e);
            isRunning = false;
            return;
        }

        // 2. Initialize AudioTrack (Speaker / Earpiece)
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AUDIO_ENCODING)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_OUT)
                .build();

        audioTrack = new AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );

        // 3. Initialize UDP socket on port 8889
        try {
            udpSocket = new DatagramSocket(AUDIO_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to bind UDP socket on " + AUDIO_PORT, e);
            stopCall();
            return;
        }

        // 4. Start Microphone capture & UDP transmission thread
        captureThread = new Thread(() -> {
            try {
                audioRecord.startRecording();
                InetAddress targetAddr = InetAddress.getByName(peerIp.trim());
                byte[] audioBuffer = new byte[bufferSize];

                Log.d(TAG, "VoIP capture started -> transmitting to " + peerIp + ":" + AUDIO_PORT);

                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    int readBytes = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (readBytes > 0) {
                        if (isMuted) {
                            // Zero out buffer if muted
                            java.util.Arrays.fill(audioBuffer, 0, readBytes, (byte) 0);
                        }
                        DatagramPacket packet = new DatagramPacket(audioBuffer, readBytes, targetAddr, AUDIO_PORT);
                        udpSocket.send(packet);
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Audio capture thread error", e);
            }
        });

        // 5. Start UDP reception & Speaker playback thread
        playThread = new Thread(() -> {
            try {
                audioTrack.play();
                byte[] recvBuffer = new byte[bufferSize * 2];
                DatagramPacket packet = new DatagramPacket(recvBuffer, recvBuffer.length);

                Log.d(TAG, "VoIP playback started -> listening on UDP " + AUDIO_PORT);

                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    udpSocket.receive(packet);
                    if (packet.getLength() > 0) {
                        audioTrack.write(packet.getData(), 0, packet.getLength());
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Audio playback thread error", e);
            }
        });

        captureThread.setPriority(Thread.MAX_PRIORITY);
        playThread.setPriority(Thread.MAX_PRIORITY);

        captureThread.start();
        playThread.start();
        Log.d(TAG, "AudioCallEngine VoIP engine successfully running!");
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    public void setSpeakerphoneOn(boolean on) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(on);
        }
    }

    public synchronized void stopCall() {
        isRunning = false;
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        if (playThread != null) {
            playThread.interrupt();
            playThread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            udpSocket = null;
        }
        Log.d(TAG, "AudioCallEngine stopped successfully.");
    }
}
