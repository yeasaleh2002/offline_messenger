package com.meshconnect.offlinechat.audio;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Utility helper to record compressed AAC (.m4a) voice notes using Android MediaRecorder.
 */
public class VoiceRecorderHelper {

    private static final String TAG = "VoiceRecorderHelper";

    private final Context context;
    private MediaRecorder mediaRecorder;
    private File currentOutputFile;
    private boolean isRecording = false;
    private long recordingStartTime = 0;

    public VoiceRecorderHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Starts recording audio into a generated or specified .m4a file.
     */
    @SuppressWarnings("deprecation")
    public synchronized File startRecording() throws IOException {
        stopRecording(); // Ensure previous session is released

        File audioDir = new File(context.getFilesDir(), "voice_notes");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }

        currentOutputFile = new File(audioDir, "VOICE_" + System.currentTimeMillis() + ".m4a");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(context);
        } else {
            mediaRecorder = new MediaRecorder();
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(64000); // 64 kbps (crystal clear voice note)
        mediaRecorder.setAudioSamplingRate(44100);    // 44.1 kHz standard
        mediaRecorder.setOutputFile(currentOutputFile.getAbsolutePath());

        mediaRecorder.prepare();
        mediaRecorder.start();

        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        Log.d(TAG, "Voice recording started -> " + currentOutputFile.getAbsolutePath());

        return currentOutputFile;
    }

    /**
     * Stops recording and returns the recorded file if duration was valid (> 500ms).
     */
    public synchronized File stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            return null;
        }

        long durationMs = System.currentTimeMillis() - recordingStartTime;
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping MediaRecorder", e);
        } finally {
            mediaRecorder = null;
            isRecording = false;
        }

        // Drop accidental micro-clicks (< 500ms)
        if (durationMs < 500 && currentOutputFile != null && currentOutputFile.exists()) {
            currentOutputFile.delete();
            return null;
        }

        Log.d(TAG, "Voice recording stopped. File: " + (currentOutputFile != null ? currentOutputFile.length() : 0) + " bytes");
        return currentOutputFile;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void cancelRecording() {
        stopRecording();
        if (currentOutputFile != null && currentOutputFile.exists()) {
            currentOutputFile.delete();
            currentOutputFile = null;
        }
    }
}
