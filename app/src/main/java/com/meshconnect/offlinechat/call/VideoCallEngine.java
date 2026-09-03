package com.meshconnect.offlinechat.call;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Real-time Offline Video Call Engine streaming live camera frames
 * over a dedicated high-speed local P2P socket (port 8890).
 */
@SuppressWarnings("deprecation")
public class VideoCallEngine implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private static final String TAG = "VideoCallEngine";
    public static final int VIDEO_PORT = 8890;

    public interface OnFrameReceivedListener {
        void onRemoteFrame(Bitmap bitmap);
    }

    private final Context context;
    private final String peerIp;
    private final boolean isInitiator;
    private final OnFrameReceivedListener frameListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Camera camera;
    private SurfaceView localPreviewSurface;
    private volatile boolean isRunning = false;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataOutputStream outStream;
    private Thread networkThread;
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public VideoCallEngine(Context context, String peerIp, boolean isInitiator, OnFrameReceivedListener listener) {
        this.context = context.getApplicationContext();
        this.peerIp = peerIp;
        this.isInitiator = isInitiator;
        this.frameListener = listener;
    }

    public void setupLocalPreview(SurfaceView surfaceView) {
        this.localPreviewSurface = surfaceView;
        if (localPreviewSurface != null && localPreviewSurface.getHolder() != null) {
            localPreviewSurface.getHolder().addCallback(this);
        }
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        startCamera();
        startVideoNetwork();
    }

    private void startVideoNetwork() {
        networkThread = new Thread(() -> {
            try {
                if (isInitiator) {
                    // Try to connect to peer's video server
                    Log.d(TAG, "VideoCallEngine initiator connecting to " + peerIp + ":" + VIDEO_PORT);
                    int retries = 0;
                    while (isRunning && retries < 15) {
                        try {
                            clientSocket = new Socket();
                            clientSocket.connect(new InetSocketAddress(peerIp, VIDEO_PORT), 3000);
                            break;
                        } catch (IOException e) {
                            retries++;
                            Thread.sleep(500);
                        }
                    }
                } else {
                    // Accept connection as video server
                    Log.d(TAG, "VideoCallEngine listening on port " + VIDEO_PORT);
                    serverSocket = new ServerSocket(VIDEO_PORT);
                    clientSocket = serverSocket.accept();
                }

                if (clientSocket != null && clientSocket.isConnected()) {
                    Log.d(TAG, "Video socket connection established!");
                    outStream = new DataOutputStream(clientSocket.getOutputStream());
                    DataInputStream inStream = new DataInputStream(clientSocket.getInputStream());

                    // Start listening for incoming video frames from peer
                    while (isRunning && !Thread.currentThread().isInterrupted()) {
                        int frameLength = inStream.readInt();
                        if (frameLength > 0 && frameLength < 1000000) { // Safety limit: 1MB per frame
                            byte[] frameData = new byte[frameLength];
                            inStream.readFully(frameData);

                            Bitmap bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
                            if (bitmap != null && frameListener != null) {
                                mainHandler.post(() -> frameListener.onRemoteFrame(bitmap));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Video network engine error", e);
            }
        });
        networkThread.start();
    }

    private void startCamera() {
        try {
            int numCameras = Camera.getNumberOfCameras();
            int targetCameraId = 0;
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == cameraFacing) {
                    targetCameraId = i;
                    break;
                }
            }

            camera = Camera.open(targetCameraId);
            Camera.Parameters params = camera.getParameters();
            int previewWidth = 320;
            int previewHeight = 240;
            params.setPreviewSize(previewWidth, previewHeight); // Optimized for ultra-fast real-time P2P transmission
            params.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(params);
            camera.setDisplayOrientation(90);

            if (localPreviewSurface != null && localPreviewSurface.getHolder() != null) {
                camera.setPreviewDisplay(localPreviewSurface.getHolder());
            }

            camera.setPreviewCallback(this);
            camera.startPreview();
            Log.d(TAG, "Camera preview started successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error starting camera for video call", e);
        }
    }

    public void switchCamera() {
        cameraFacing = (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                ? Camera.CameraInfo.CAMERA_FACING_BACK
                : Camera.CameraInfo.CAMERA_FACING_FRONT;
        stopCamera();
        startCamera();
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
        }
    }

    private long lastFrameSentTime = 0;
    private static final int PREVIEW_W = 320;
    private static final int PREVIEW_H = 240;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!isRunning || outStream == null || data == null) return;

        // Throttle to ~18-20 FPS for smooth performance
        long now = System.currentTimeMillis();
        if (now - lastFrameSentTime < 50) return;
        lastFrameSentTime = now;

        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, PREVIEW_W, PREVIEW_H, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, PREVIEW_W, PREVIEW_H), 40, baos); // 40% JPEG quality for high speed
            byte[] jpegBytes = baos.toByteArray();

            synchronized (this) {
                if (outStream != null) {
                    outStream.writeInt(jpegBytes.length);
                    outStream.write(jpegBytes);
                    outStream.flush();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed sending camera frame", e);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    public synchronized void stop() {
        isRunning = false;
        stopCamera();
        if (networkThread != null) {
            networkThread.interrupt();
            networkThread = null;
        }
        if (outStream != null) {
            try { outStream.close(); } catch (Exception ignored) {}
            outStream = null;
        }
        if (clientSocket != null) {
            try { clientSocket.close(); } catch (Exception ignored) {}
            clientSocket = null;
        }
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (Exception ignored) {}
            serverSocket = null;
        }
        Log.d(TAG, "VideoCallEngine stopped.");
    }
}
