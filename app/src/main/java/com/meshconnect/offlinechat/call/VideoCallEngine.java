package com.meshconnect.offlinechat.call;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

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
    private SurfaceTexture dummySurfaceTexture;
    private volatile boolean isRunning = false;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataOutputStream outStream;
    private Thread networkThread;
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private int previewWidth = 640;
    private int previewHeight = 480;
    private long lastFrameSentTime = 0;

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
                    Log.d(TAG, "VideoCallEngine initiator connecting to " + peerIp + ":" + VIDEO_PORT);
                    int retries = 0;
                    while (isRunning && retries < 25) {
                        try {
                            clientSocket = new Socket();
                            clientSocket.connect(new InetSocketAddress(peerIp, VIDEO_PORT), 3000);
                            break;
                        } catch (IOException e) {
                            retries++;
                            Thread.sleep(400);
                        }
                    }
                } else {
                    Log.d(TAG, "VideoCallEngine listening on port " + VIDEO_PORT);
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(VIDEO_PORT));
                    clientSocket = serverSocket.accept();
                }

                if (clientSocket != null && clientSocket.isConnected()) {
                    Log.d(TAG, "Video socket connection established!");
                    outStream = new DataOutputStream(clientSocket.getOutputStream());
                    DataInputStream inStream = new DataInputStream(clientSocket.getInputStream());

                    // Start listening for incoming video frames from peer
                    while (isRunning && !Thread.currentThread().isInterrupted()) {
                        int frameLength = inStream.readInt();
                        if (frameLength > 0 && frameLength < 2000000) { // Safety limit: 2MB
                            short rotationDegrees = inStream.readShort();
                            byte[] frameData = new byte[frameLength];
                            inStream.readFully(frameData);

                            Bitmap rawBitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
                            if (rawBitmap != null && frameListener != null) {
                                Bitmap finalBitmap;
                                if (rotationDegrees != 0) {
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(rotationDegrees);
                                    if (rotationDegrees == 270) {
                                        matrix.postScale(-1, 1); // Mirror front camera naturally
                                    }
                                    finalBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
                                } else {
                                    finalBitmap = rawBitmap;
                                }
                                mainHandler.post(() -> frameListener.onRemoteFrame(finalBitmap));
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
            if (numCameras == 0) {
                Log.e(TAG, "No camera found on this device.");
                return;
            }

            int targetCameraId = 0;
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == cameraFacing) {
                    targetCameraId = i;
                    break;
                }
            }

            stopCamera();

            camera = Camera.open(targetCameraId);
            Camera.Parameters params = camera.getParameters();

            // 1. Dynamic preview size discovery (never hardcode unsupported resolutions)
            List<Camera.Size> supported = params.getSupportedPreviewSizes();
            Camera.Size chosenSize = null;
            if (supported != null && !supported.isEmpty()) {
                // Priority 1: 640x480 (standard VGA supported by virtually every Android camera)
                for (Camera.Size s : supported) {
                    if (s.width == 640 && s.height == 480) {
                        chosenSize = s;
                        break;
                    }
                }
                // Priority 2: 320x240 (QVGA)
                if (chosenSize == null) {
                    for (Camera.Size s : supported) {
                        if (s.width == 320 && s.height == 240) {
                            chosenSize = s;
                            break;
                        }
                    }
                }
                // Priority 3: Any supported size with width >= 320
                if (chosenSize == null) {
                    for (Camera.Size s : supported) {
                        if (s.width >= 320 && s.height >= 240) {
                            if (chosenSize == null || (s.width * s.height < chosenSize.width * chosenSize.height)) {
                                chosenSize = s;
                            }
                        }
                    }
                }
                if (chosenSize == null) {
                    chosenSize = supported.get(0);
                }
            }

            if (chosenSize != null) {
                previewWidth = chosenSize.width;
                previewHeight = chosenSize.height;
            } else {
                previewWidth = 640;
                previewHeight = 480;
            }

            Log.d(TAG, "Selected camera preview size: " + previewWidth + "x" + previewHeight);
            params.setPreviewSize(previewWidth, previewHeight);
            params.setPreviewFormat(ImageFormat.NV21);

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            camera.setParameters(params);
            camera.setDisplayOrientation(90);

            // 2. Safe Preview Display:
            // If SurfaceView holder is ready, bind it; otherwise bind a dummy SurfaceTexture so preview starts immediately.
            boolean boundToSurface = false;
            if (localPreviewSurface != null && localPreviewSurface.getHolder() != null
                    && localPreviewSurface.getHolder().getSurface() != null
                    && localPreviewSurface.getHolder().getSurface().isValid()) {
                try {
                    camera.setPreviewDisplay(localPreviewSurface.getHolder());
                    boundToSurface = true;
                } catch (Exception e) {
                    Log.w(TAG, "Could not setPreviewDisplay directly", e);
                }
            }

            if (!boundToSurface) {
                try {
                    if (dummySurfaceTexture == null) {
                        dummySurfaceTexture = new SurfaceTexture(10);
                    }
                    camera.setPreviewTexture(dummySurfaceTexture);
                } catch (Exception e) {
                    Log.w(TAG, "Could not setPreviewTexture fallback", e);
                }
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!isRunning || outStream == null || data == null) return;

        // Throttle to ~18-20 FPS for low battery & network overhead
        long now = System.currentTimeMillis();
        if (now - lastFrameSentTime < 50) return;
        lastFrameSentTime = now;

        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewWidth, previewHeight, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), 45, baos);
            byte[] jpegBytes = baos.toByteArray();

            short rotationDegrees = (short) ((cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) ? 270 : 90);

            synchronized (this) {
                if (outStream != null) {
                    outStream.writeInt(jpegBytes.length);
                    outStream.writeShort(rotationDegrees);
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
        if (camera != null && isRunning) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(this);
                camera.startPreview();
            } catch (Exception e) {
                Log.w(TAG, "Error attaching surfaceCreated holder to camera", e);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Switch to dummy SurfaceTexture so frame streaming continues without stopping camera
        if (camera != null && isRunning) {
            try {
                if (dummySurfaceTexture == null) {
                    dummySurfaceTexture = new SurfaceTexture(10);
                }
                camera.setPreviewTexture(dummySurfaceTexture);
            } catch (Exception ignored) {}
        }
    }

    public synchronized void stop() {
        isRunning = false;
        stopCamera();
        if (dummySurfaceTexture != null) {
            try { dummySurfaceTexture.release(); } catch (Exception ignored) {}
            dummySurfaceTexture = null;
        }
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
