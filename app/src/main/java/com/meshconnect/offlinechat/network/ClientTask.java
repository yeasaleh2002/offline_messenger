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
     * Transmits a handshake packet to register this client's identity and IP with the peer / Group Owner.
     */
    public static void sendHandshake(String targetIp, int targetPort, String localDeviceName, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            java.io.DataInputStream dis = null;
            try {
                Log.d(TAG, "Sending handshake to " + targetIp + ":" + targetPort + " from " + localDeviceName);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dis = new java.io.DataInputStream(socket.getInputStream());

                byte[] nameBytes = (localDeviceName != null ? localDeviceName : "Peer Device").getBytes(StandardCharsets.UTF_8);
                dos.writeByte(ServerThread.TYPE_HANDSHAKE);
                dos.writeShort(nameBytes.length);
                dos.write(nameBytes);
                dos.flush();

                // Wait for ACK from receiver
                try {
                    byte ack = dis.readByte();
                    Log.d(TAG, "Handshake ACK received: " + ack);
                } catch (Exception ignored) {}

                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending handshake to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("Handshake failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dis);
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Transmits a call invitation packet to the remote peer.
     */
    public static void sendCallInvite(String targetIp, int targetPort, String callerName, boolean isVideo, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            try {
                Log.d(TAG, "Sending call invite (" + (isVideo ? "VIDEO" : "AUDIO") + ") to " + targetIp + ":" + targetPort);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                byte[] nameBytes = (callerName != null ? callerName : "Caller").getBytes(StandardCharsets.UTF_8);

                dos.writeByte(ServerThread.TYPE_CALL_INVITE);
                dos.writeShort(nameBytes.length);
                dos.write(nameBytes);
                dos.writeByte(isVideo ? 1 : 0);
                dos.flush();

                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending call invite to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("Call request failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Sends a simple call signal byte (ACCEPT, DECLINE, END) to the remote peer.
     */
    public static void sendCallSignal(String targetIp, int targetPort, byte signal, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            try {
                Log.d(TAG, "Sending call signal " + signal + " to " + targetIp + ":" + targetPort);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dos.writeByte(signal);
                dos.flush();

                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending call signal to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("Call signal failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Transmits an offline group message payload to the target peer or Group Owner.
     */
    public static void sendGroupMessage(String targetIp, int targetPort, String groupId, String groupName, String senderName, String messageText, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            java.io.DataInputStream dis = null;
            try {
                Log.d(TAG, "Sending group message to " + targetIp + ":" + targetPort + " for group [" + groupName + "]");
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dis = new java.io.DataInputStream(socket.getInputStream());

                byte[] gIdBytes = (groupId != null ? groupId : "").getBytes(StandardCharsets.UTF_8);
                byte[] gNameBytes = (groupName != null ? groupName : "Group").getBytes(StandardCharsets.UTF_8);
                byte[] sNameBytes = (senderName != null ? senderName : "Me").getBytes(StandardCharsets.UTF_8);
                byte[] textBytes = (messageText != null ? messageText : "").getBytes(StandardCharsets.UTF_8);

                dos.writeByte(ServerThread.TYPE_GROUP_MESSAGE);
                dos.writeShort(gIdBytes.length);
                dos.write(gIdBytes);
                dos.writeShort(gNameBytes.length);
                dos.write(gNameBytes);
                dos.writeShort(sNameBytes.length);
                dos.write(sNameBytes);
                dos.writeInt(textBytes.length);
                dos.write(textBytes);
                dos.flush();

                // Wait for ACK
                socket.setSoTimeout(3000);
                try {
                    byte ack = dis.readByte();
                    if (ack == ServerThread.TYPE_ACK) {
                        Log.d(TAG, "Group message transmission verified by ACK from " + targetIp);
                    }
                } catch (Exception ignored) {}

                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error sending group message to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("Group send failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(dis);
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

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
            java.io.DataInputStream dis = null;
            try {
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dis = new java.io.DataInputStream(socket.getInputStream());

                byte[] textBytes = messageText.getBytes(StandardCharsets.UTF_8);

                // Header Protocol: TYPE_TEXT (0x01) + Length (int) + UTF-8 bytes
                dos.writeByte(ServerThread.TYPE_TEXT);
                dos.writeInt(textBytes.length);
                dos.write(textBytes);
                dos.flush();

                // Wait for ACK
                try {
                    byte ack = dis.readByte();
                    Log.d(TAG, "Text delivery ACK received: " + ack);
                } catch (Exception ignored) {}

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
                closeQuietly(dis);
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Transmits a file by streaming directly from disk (handles files of any size without OOM).
     */
    public static void sendFile(String targetIp, int targetPort, String fileName, File file, OnSendListener listener) {
        if (targetIp == null || targetIp.trim().isEmpty()) {
            if (listener != null) listener.onFailure("Invalid target IP address.");
            return;
        }

        if (file == null || !file.exists() || file.length() == 0) {
            if (listener != null) listener.onFailure("Invalid or empty file.");
            return;
        }

        executor.execute(() -> {
            Socket socket = null;
            DataOutputStream dos = null;
            java.io.DataInputStream dis = null;
            FileInputStream fis = null;
            try {
                long fileSize = file.length();
                Log.d(TAG, "Connecting to " + targetIp + ":" + targetPort + " to stream file: " + fileName + " (" + fileSize + " bytes)");

                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dis = new java.io.DataInputStream(socket.getInputStream());

                byte[] nameBytes = (fileName != null ? fileName : file.getName()).getBytes(StandardCharsets.UTF_8);

                // Protocol: TYPE_FILE (0x02) + Filename Length (short) + Filename + File Size (long) + Stream
                dos.writeByte(ServerThread.TYPE_FILE);
                dos.writeShort(nameBytes.length);
                dos.write(nameBytes);
                dos.writeLong(fileSize);

                fis = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalSent = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                }
                dos.flush();

                // Signal clean EOF on TCP channel
                try {
                    socket.shutdownOutput();
                } catch (IOException ignored) {}

                // Wait for receiver ACK confirmation
                try {
                    byte ack = dis.readByte();
                    Log.d(TAG, "File delivery ACK received: " + ack);
                } catch (Exception ignored) {}

                Log.d(TAG, "File streaming transmission completed: " + fileName + " (" + totalSent + " bytes)");
                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess();
                });

            } catch (IOException e) {
                Log.e(TAG, "Error streaming file to " + targetIp + ":" + targetPort, e);
                mainHandler.post(() -> {
                    if (listener != null) listener.onFailure("File transfer failed: " + e.getMessage());
                });
            } finally {
                closeQuietly(fis);
                closeQuietly(dis);
                closeQuietly(dos);
                closeQuietly(socket);
            }
        });
    }

    /**
     * Transmits a binary byte array payload to the target peer (backward compatibility).
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
            java.io.DataInputStream dis = null;
            try {
                Log.d(TAG, "Connecting to " + targetIp + ":" + targetPort + " to send file: " + fileName);
                socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(targetIp.trim(), targetPort), SOCKET_TIMEOUT_MS);

                dos = new DataOutputStream(socket.getOutputStream());
                dis = new java.io.DataInputStream(socket.getInputStream());

                byte[] nameBytes = (fileName != null ? fileName : "attachment.dat").getBytes(StandardCharsets.UTF_8);

                dos.writeByte(ServerThread.TYPE_FILE);
                dos.writeShort(nameBytes.length);
                dos.write(nameBytes);
                dos.writeLong(fileBytes.length);
                dos.write(fileBytes);
                dos.flush();

                try {
                    socket.shutdownOutput();
                } catch (IOException ignored) {}

                try {
                    byte ack = dis.readByte();
                    Log.d(TAG, "File ACK received: " + ack);
                } catch (Exception ignored) {}

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
                closeQuietly(dis);
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
