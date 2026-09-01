package com.meshconnect.offlinechat.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background ClientTask to transmit text payloads and binary files (images/documents)
 * to a connected peer's IP address over TCP/IP sockets off the main UI thread.
 */
public class ClientTask {

    private static final String TAG = "ClientTask";
    private static final int SOCKET_TIMEOUT_MS = 8000;

    public interface OnSendListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Transmits a UTF-8 text message payload to the target peer.
     */
    public static void sendText(String targetIp, int targetPort, String messageText, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        if (messageText == null || messageText.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Cannot send empty text message.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            try {
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                byte[] textBytes = messageText.getBytes(StandardCharsets.UTF_8);

                // Header Protocol: TYPE_TEXT (0x01) + Length (int) + UTF-8 bytes
                dos.writeByte(ServerThread.TYPE_TEXT);
                dos.writeInt(textBytes.length);
                dos.write(textBytes);
                dos.flush();

                Log.d(TAG, "Text message sent successfully (" + textBytes.length + " bytes).");
                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending text to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("Send failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Transmits a binary file payload (image, document, audio) to the target peer.
     *
     * @param targetIp Target peer IP
     * @param targetPort Target port
     * @param fileName Name of the file
     * @param fileBytes Raw byte array of the file
     * @param listener Callback listener
     */
    public static void sendFile(String targetIp, int targetPort, String fileName, byte[] fileBytes, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        if (fileBytes == null || fileBytes.length == 0) {
            if (listener != null) listener.onFailure("File is empty.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            try {
                Log.d(TAG, "Connecting to " + targetIp + ":" + targetPort + " to send file: " + fileName);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                byte[] nameBytes = (fileName != null ? fileName : "attachment.dat").getBytes(StandardCharsets.UTF_8);

                // Header Protocol: TYPE_FILE (0x02) + Filename Length (short) + Filename + File Size (long) + Raw Bytes
                dos.writeByte(ServerThread.TYPE_FILE);
                dos.writeShort(nameBytes.length);
                dos.write(nameBytes);
                dos.writeLong(fileBytes.length);
                dos.write(fileBytes);
                dos.flush();

                Log.d(TAG, "File transmission completed: " + fileName + " (" + fileBytes.length + " bytes)");
                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending file to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("File transfer failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }
    }
}
