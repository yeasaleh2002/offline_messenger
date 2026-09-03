package com.meshconnect.offlinechat.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Background ServerThread running a ServerSocket on port 8888
 * listening for incoming text transmissions and binary file/image transfers.
 */
public class ServerThread extends Thread {

    private static final String TAG = "ServerThread";
    public static final int DEFAULT_PORT = 8888;

    public static final byte TYPE_TEXT = 0x01;
    public static final byte TYPE_FILE = 0x02;
    public static final byte TYPE_HANDSHAKE = 0x03;
    public static final byte TYPE_ACK = 0x06;

    public interface OnMessageReceivedListener {
        void onHandshakeReceived(String peerName, String senderIp);
        void onMessageReceived(String messageText, String senderIp);
        void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp);
        void onServerError(String errorMessage);
    }

    private final Context context;
    private final int port;
    private final OnMessageReceivedListener listener;
    private final Handler mainHandler;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    public ServerThread(Context context, int port, OnMessageReceivedListener listener) {
        this.context = context.getApplicationContext();
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
        isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "ServerSocket listening on port " + port + " for text & file transfers...");

            while (isRunning && !isInterrupted()) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    Log.d(TAG, "Incoming connection from: " + clientIp);

                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    java.io.DataOutputStream dos = new java.io.DataOutputStream(clientSocket.getOutputStream());
                    byte packetType = dis.readByte();

                    if (packetType == TYPE_HANDSHAKE) {
                        // 0. Peer Handshake Packet for dynamic IP binding
                        short nameLen = dis.readShort();
                        byte[] nameBytes = new byte[nameLen];
                        dis.readFully(nameBytes);
                        String peerName = new String(nameBytes, StandardCharsets.UTF_8);
                        Log.d(TAG, "Received handshake from peer " + peerName + " at " + clientIp);

                        // Send back ACK
                        try {
                            dos.writeByte(TYPE_ACK);
                            dos.flush();
                        } catch (IOException ignored) {}

                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onHandshakeReceived(peerName, clientIp);
                            }
                        });

                    } else if (packetType == TYPE_TEXT) {
                        // 1. Read Text Packet
                        int textLength = dis.readInt();
                        byte[] textBytes = new byte[textLength];
                        dis.readFully(textBytes);
                        String receivedMessage = new String(textBytes, StandardCharsets.UTF_8);
                        Log.d(TAG, "Received text payload from " + clientIp + ": " + receivedMessage);

                        // Send back ACK
                        try {
                            dos.writeByte(TYPE_ACK);
                            dos.flush();
                        } catch (IOException ignored) {}

                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onMessageReceived(receivedMessage, clientIp);
                            }
                        });

                    } else if (packetType == TYPE_FILE) {
                        // 2. Read Binary File Packet
                        short nameLength = dis.readShort();
                        byte[] nameBytes = new byte[nameLength];
                        dis.readFully(nameBytes);
                        String originalFileName = new String(nameBytes, StandardCharsets.UTF_8);
                        long fileSize = dis.readLong();

                        Log.d(TAG, "Receiving file: " + originalFileName + " (" + fileSize + " bytes) from " + clientIp);

                        // Save incoming byte stream to app's internal storage
                        File storageDir = new File(context.getFilesDir(), "received_files");
                        if (!storageDir.exists()) {
                            storageDir.mkdirs();
                        }

                        // Avoid name collision
                        File destFile = new File(storageDir, System.currentTimeMillis() + "_" + originalFileName);
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            byte[] buffer = new byte[8192];
                            long totalBytesRead = 0;
                            while (totalBytesRead < fileSize) {
                                int toRead = (int) Math.min(buffer.length, fileSize - totalBytesRead);
                                int bytesRead = dis.read(buffer, 0, toRead);
                                if (bytesRead == -1) break;
                                fos.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                            }
                            fos.flush();
                        }

                        Log.d(TAG, "File successfully received and saved to: " + destFile.getAbsolutePath());

                        // Send back ACK so client knows transmission is verified
                        try {
                            dos.writeByte(TYPE_ACK);
                            dos.flush();
                        } catch (IOException ignored) {}

                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onFileReceived(destFile, originalFileName, fileSize, clientIp);
                            }
                        });
                    }

                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Error handling client packet stream", e);
                    }
                } finally {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                Log.e(TAG, "ServerSocket error on port " + port, e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onServerError("Server socket error: " + e.getMessage());
                    }
                });
            }
        } finally {
            stopServer();
        }
    }

    /**
     * Gracefully stops the server socket and terminates the listening loop.
     */
    public void stopServer() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                Log.d(TAG, "ServerSocket closed successfully.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing ServerSocket", e);
            }
        }
        interrupt();
    }
}
