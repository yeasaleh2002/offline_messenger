package com.meshconnect.offlinechat.network;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * High-level manager coordinating background ServerThread listener
 * and asynchronous ClientTask transmissions for P2P chat and offline file transfers.
 */
public class P2PSocketManager {

    private static final String TAG = "P2PSocketManager";
    public static final int DEFAULT_PORT = 8888;

    public interface SocketEventListener {
        void onHandshakeReceived(String peerName, String senderIp);
        void onMessageReceived(String messageText, String senderIp);
        void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp);
        void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp);
        void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp);
        void onMessageSent(String messageText);
        void onFileSent(String fileName);
        void onNetworkError(String errorMessage);
    }

    private final Context context;
    private final int port;
    private final SocketEventListener eventListener;
    private ServerThread serverThread;

    public P2PSocketManager(Context context, SocketEventListener eventListener) {
        this(context, DEFAULT_PORT, eventListener);
    }

    public P2PSocketManager(Context context, int port, SocketEventListener eventListener) {
        this.context = context.getApplicationContext();
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.eventListener = eventListener;
    }

    /**
     * Starts the ServerThread in the background to accept incoming peer socket connections.
     */
    public synchronized void startServer() {
        stopServer(); // Ensure previous instance is stopped

        serverThread = new ServerThread(context, port, new ServerThread.OnMessageReceivedListener() {
            @Override
            public void onHandshakeReceived(String peerName, String senderIp) {
                Log.d(TAG, "P2PSocketManager received handshake from " + peerName + " (" + senderIp + ")");
                if (eventListener != null) {
                    eventListener.onHandshakeReceived(peerName, senderIp);
                }
            }

            @Override
            public void onMessageReceived(String messageText, String senderIp) {
                Log.d(TAG, "P2PSocketManager received message from " + senderIp);
                if (eventListener != null) {
                    eventListener.onMessageReceived(messageText, senderIp);
                }
            }

            @Override
            public void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp) {
                Log.d(TAG, "P2PSocketManager received group message for [" + groupName + "] from " + senderName + " (" + senderIp + ")");
                if (eventListener != null) {
                    eventListener.onGroupMessageReceived(groupId, groupName, senderName, messageText, senderIp);
                }
            }

            @Override
            public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {
                Log.d(TAG, "P2PSocketManager received file " + fileName + " from " + senderIp);
                if (eventListener != null) {
                    eventListener.onFileReceived(savedFile, fileName, fileSize, senderIp);
                }
            }

            @Override
            public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {
                Log.d(TAG, "P2PSocketManager received call signal " + callSignal + " from " + senderIp);
                if (eventListener != null) {
                    eventListener.onCallSignalingReceived(callSignal, callerName, callType, senderIp);
                }
            }

            @Override
            public void onServerError(String errorMessage) {
                Log.e(TAG, "P2PSocketManager server error: " + errorMessage);
                if (eventListener != null) {
                    eventListener.onNetworkError(errorMessage);
                }
            }
        });

        serverThread.start();
        Log.d(TAG, "P2P ServerThread started on port " + port);
    }

    /**
     * Sends a call invite to a peer IP.
     */
    public void sendCallInvite(String targetIp, String callerName, boolean isVideo) {
        ClientTask.sendCallInvite(targetIp, this.port, callerName, isVideo, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call invite dispatched to " + targetIp);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError("Call invite failed: " + errorMessage);
                }
            }
        });
    }

    /**
     * Sends a call signaling byte (ACCEPT, DECLINE, END) to a peer IP.
     */
    public void sendCallSignal(String targetIp, byte signal) {
        ClientTask.sendCallSignal(targetIp, this.port, signal, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call signal " + signal + " sent to " + targetIp);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError("Call signal error: " + errorMessage);
                }
            }
        });
    }

    /**
     * Sends a handshake packet to register this client's identity and IP with the peer / Group Owner.
     */
    public void sendHandshake(String targetIp, String localDeviceName) {
        ClientTask.sendHandshake(targetIp, this.port, localDeviceName, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Handshake dispatched successfully to " + targetIp);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.w(TAG, "Handshake attempt failed: " + errorMessage);
                if (eventListener != null) {
                    eventListener.onNetworkError("Handshake notice: " + errorMessage);
                }
            }
        });
    }

    /**
     * Sends an offline group message to a target peer or Group Owner.
     */
    public void sendGroupMessage(String targetIp, String groupId, String groupName, String senderName, String messageText) {
        ClientTask.sendGroupMessage(targetIp, this.port, groupId, groupName, senderName, messageText, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                if (eventListener != null) {
                    eventListener.onMessageSent(messageText);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError("Group send failed: " + errorMessage);
                }
            }
        });
    }

    /**
     * Sends a text payload to a remote peer IP asynchronously.
     */
    public void sendMessage(String targetIp, String messageText) {
        ClientTask.sendText(targetIp, this.port, messageText, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                if (eventListener != null) {
                    eventListener.onMessageSent(messageText);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError(errorMessage);
                }
            }
        });
    }

    /**
     * Streams a File object to the remote peer IP asynchronously.
     */
    public void sendFile(String targetIp, String fileName, File file) {
        ClientTask.sendFile(targetIp, this.port, fileName, file, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                if (eventListener != null) {
                    eventListener.onFileSent(fileName);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError(errorMessage);
                }
            }
        });
    }

    /**
     * Sends a binary file payload to a remote peer IP asynchronously (byte array fallback).
     */
    public void sendFile(String targetIp, String fileName, byte[] fileBytes) {
        ClientTask.sendFile(targetIp, this.port, fileName, fileBytes, new ClientTask.OnSendListener() {
            @Override
            public void onSuccess() {
                if (eventListener != null) {
                    eventListener.onFileSent(fileName);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (eventListener != null) {
                    eventListener.onNetworkError(errorMessage);
                }
            }
        });
    }

    /**
     * Stops the background server thread and releases port resources.
     */
    public synchronized void stopServer() {
        if (serverThread != null) {
            serverThread.stopServer();
            serverThread = null;
            Log.d(TAG, "P2P ServerThread stopped.");
        }
    }
}
