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
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

/**
 * High-performance real-time offline Video Call Engine streaming live camera frames
 * over a peer-to-peer UDP socket (port 8890) with dynamic resolution discovery and MTU chunking.
 * Symmetrical architecture: both peers send & receive UDP datagrams without NAT/client-server blockers.
 */
@SuppressWarnings("deprecation")
public class VideoCallEngine implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private static final String TAG = "VideoCallEngine";
    public static final int VIDEO_PORT = 8890;
    private static final int CHUNK_SIZE = 1200; // Safe chunk size below standard 1500-byte MTU

    public interface OnFrameReceivedListener {
        void onRemoteFrame(Bitmap bitmap);
    }

    private final Context context;
    private final String peerIp;
    private final OnFrameReceivedListener frameListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Camera camera;
    private SurfaceView localPreviewSurface;
    private SurfaceTexture dummySurfaceTexture;
    private volatile boolean isRunning = false;

    private DatagramSocket udpSocket;
    private Thread receiveThread;
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private int previewWidth = 640;
    private int previewHeight = 480;
    private long lastFrameSentTime = 0;
    private int nextFrameId = 0;

    // Frame assembly state on receiver side
    private int currentAssembleFrameId = -1;
    private int currentTotalChunks = 0;
    private short currentRotation = 0;
    private final SparseArray<byte[]> receivedChunks = new SparseArray<>();
    private int receivedChunkCount = 0;

    public VideoCallEngine(Context context, String peerIp, OnFrameReceivedListener listener) {
        this.context = context.getApplicationContext();
        this.peerIp = peerIp;
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

        initUdpSocket();
        startCamera();
        startReceiveThread();
    }

    private void initUdpSocket() {
        try {
            udpSocket = new DatagramSocket(VIDEO_PORT);
            udpSocket.setReuseAddress(true);
            udpSocket.setReceiveBufferSize(512 * 1024);
            udpSocket.setSendBufferSize(512 * 1024);
            Log.d(TAG, "Video UDP socket bound to port " + VIDEO_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to bind video UDP socket on " + VIDEO_PORT, e);
        }
    }

    private void startReceiveThread() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    if (udpSocket == null || udpSocket.isClosed()) break;
                    udpSocket.receive(packet);

                    int length = packet.getLength();
                    if (length < 12) continue; // Must contain at least header

                    byte[] data = packet.getData();

                    // Parse header:
                    // int frameId (4 bytes)
                    // short chunkIndex (2 bytes)
                    // short totalChunks (2 bytes)
                    // short rotationDegrees (2 bytes)
                    // short chunkLength (2 bytes)
                    int frameId = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                    int chunkIndex = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
                    int totalChunks = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                    short rotationDegrees = (short) (((data[8] & 0xFF) << 8) | (data[9] & 0xFF));
                    int chunkLength = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);

                    if (chunkLength <= 0 || chunkLength > (length - 12)) continue;

                    byte[] chunkBytes = new byte[chunkLength];
                    System.arraycopy(data, 12, chunkBytes, 0, chunkLength);

                    processIncomingChunk(frameId, chunkIndex, totalChunks, rotationDegrees, chunkBytes);

                } catch (IOException e) {
                    if (isRunning) Log.w(TAG, "Video UDP receive error: " + e.getMessage());
                }
            }
        });
        receiveThread.start();
    }

    private synchronized void processIncomingChunk(int frameId, int chunkIndex, int totalChunks, short rotationDegrees, byte[] chunkBytes) {
        // Discard older frames if newer frame arrived
        if (frameId > currentAssembleFrameId) {
            currentAssembleFrameId = frameId;
            currentTotalChunks = totalChunks;
            currentRotation = rotationDegrees;
            receivedChunks.clear();
            receivedChunkCount = 0;
        }

        if (frameId == currentAssembleFrameId && receivedChunks.get(chunkIndex) == null) {
            receivedChunks.put(chunkIndex, chunkBytes);
            receivedChunkCount++;

            if (receivedChunkCount == currentTotalChunks) {
                // All chunks for this frame have arrived! Assemble into single JPEG
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int i = 0; i < currentTotalChunks; i++) {
                    byte[] part = receivedChunks.get(i);
                    if (part != null) {
                        baos.write(part, 0, part.length);
                    }
                }

                byte[] fullJpeg = baos.toByteArray();
                Bitmap rawBitmap = BitmapFactory.decodeByteArray(fullJpeg, 0, fullJpeg.length);

                if (rawBitmap != null && frameListener != null) {
                    Bitmap finalBitmap;
                    if (currentRotation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(currentRotation);
                        if (currentRotation == 270) {
                            matrix.postScale(-1, 1); // Mirror front camera naturally
                        }
                        finalBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
                    } else {
                        finalBitmap = rawBitmap;
                    }
                    mainHandler.post(() -> frameListener.onRemoteFrame(finalBitmap));
                }

                // Reset for next frame
                receivedChunks.clear();
                receivedChunkCount = 0;
            }
        }
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

            // Dynamic resolution discovery (prevents hardware crash)
            List<Camera.Size> supported = params.getSupportedPreviewSizes();
            Camera.Size chosenSize = null;
            if (supported != null && !supported.isEmpty()) {
                // Priority 1: 640x480 (standard VGA supported by virtually all devices)
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

            Log.d(TAG, "Camera preview configured at " + previewWidth + "x" + previewHeight);
            params.setPreviewSize(previewWidth, previewHeight);
            params.setPreviewFormat(ImageFormat.NV21);

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            camera.setParameters(params);
            camera.setDisplayOrientation(90);

            // Safe Preview Display:
            // Bind SurfaceView if valid, otherwise bind headless SurfaceTexture fallback
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
        if (!isRunning || udpSocket == null || data == null || peerIp == null) return;

        // Throttle to ~18-20 FPS
        long now = System.currentTimeMillis();
        if (now - lastFrameSentTime < 50) return;
        lastFrameSentTime = now;

        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewWidth, previewHeight, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), 38, baos); // 38% quality for ultra-fast Wi-Fi UDP streaming
            byte[] jpegBytes = baos.toByteArray();

            int frameId = nextFrameId++;
            short rotationDegrees = (short) ((cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) ? 270 : 90);

            int totalLength = jpegBytes.length;
            int totalChunks = (totalLength + CHUNK_SIZE - 1) / CHUNK_SIZE;

            InetAddress peerAddress = InetAddress.getByName(peerIp);

            for (int i = 0; i < totalChunks; i++) {
                int offset = i * CHUNK_SIZE;
                int chunkLen = Math.min(CHUNK_SIZE, totalLength - offset);

                // Packet layout: 12-byte header + chunk payload
                byte[] packetData = new byte[12 + chunkLen];

                // frameId (4 bytes)
                packetData[0] = (byte) ((frameId >> 24) & 0xFF);
                packetData[1] = (byte) ((frameId >> 16) & 0xFF);
                packetData[2] = (byte) ((frameId >> 8) & 0xFF);
                packetData[3] = (byte) (frameId & 0xFF);

                // chunkIndex (2 bytes)
                packetData[4] = (byte) ((i >> 8) & 0xFF);
                packetData[5] = (byte) (i & 0xFF);

                // totalChunks (2 bytes)
                packetData[6] = (byte) ((totalChunks >> 8) & 0xFF);
                packetData[7] = (byte) (totalChunks & 0xFF);

                // rotationDegrees (2 bytes)
                packetData[8] = (byte) ((rotationDegrees >> 8) & 0xFF);
                packetData[9] = (byte) (rotationDegrees & 0xFF);

                // chunkLen (2 bytes)
                packetData[10] = (byte) ((chunkLen >> 8) & 0xFF);
                packetData[11] = (byte) (chunkLen & 0xFF);

                // payload
                System.arraycopy(jpegBytes, offset, packetData, 12, chunkLen);

                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, peerAddress, VIDEO_PORT);
                udpSocket.send(packet);
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed sending video UDP frame", e);
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
        if (udpSocket != null) {
            try { udpSocket.close(); } catch (Exception ignored) {}
            udpSocket = null;
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        Log.d(TAG, "VideoCallEngine stopped.");
    }
}
