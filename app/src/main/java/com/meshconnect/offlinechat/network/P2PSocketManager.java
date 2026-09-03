package com.meshconnect.offlinechat.network;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager coordinating the background ServerThread listener
 * and asynchronous ClientTask transmissions for P2P chat, offline file transfers,
 * group discussions, and VoIP signaling without port collisions.
 */
public class P2PSocketManager {

    private static final String TAG = "P2PSocketManager";
    public static final int DEFAULT_PORT = 8888;
    public static final String DEFAULT_GROUP_OWNER_IP = "192.168.49.1";

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

    private static volatile P2PSocketManager instance;

    private final Context context;
    private final int port;
    private final CopyOnWriteArrayList<SocketEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> connectedClientIps = Collections.synchronizedSet(new LinkedHashSet<>());
    private ServerThread serverThread;

    public static synchronized P2PSocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new P2PSocketManager(context.getApplicationContext(), DEFAULT_PORT);
        }
        return instance;
    }

    /**
     * Backward-compatible constructor that attaches to the shared singleton instance.
     */
    public P2PSocketManager(Context context, SocketEventListener eventListener) {
        this(context, DEFAULT_PORT, eventListener);
    }

    public P2PSocketManager(Context context, int port, SocketEventListener eventListener) {
        this.context = context.getApplicationContext();
        this.port = port > 0 ? port : DEFAULT_PORT;
        if (eventListener != null) {
            addListener(eventListener);
        }
        // Ensure singleton reference is initialized
        synchronized (P2PSocketManager.class) {
            if (instance == null) {
                instance = this;
            }
        }
    }

    private P2PSocketManager(Context context, int port) {
        this.context = context.getApplicationContext();
        this.port = port > 0 ? port : DEFAULT_PORT;
    }

    public void addListener(SocketEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SocketEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void registerListener(SocketEventListener listener) {
        addListener(listener);
    }

    public void unregisterListener(SocketEventListener listener) {
        removeListener(listener);
    }

    /**
     * Tracks a peer IP discovered via Wi-Fi Direct handshake or incoming packet.
     */
    public void registerPeerIp(String ip) {
        if (ip != null && !ip.trim().isEmpty() && !ip.equals("127.0.0.1") && !ip.equals(DEFAULT_GROUP_OWNER_IP)) {
            connectedClientIps.add(ip.trim());
            Log.d(TAG, "Registered peer client IP: " + ip.trim() + " (Total active peers: " + connectedClientIps.size() + ")");
        }
    }

    public Set<String> getConnectedClientIps() {
        return new LinkedHashSet<>(connectedClientIps);
    }

    /**
     * Starts the ServerThread on port 8888 if not already running.
     * Prevents BindException collisions across activities.
     */
    public synchronized void startServer() {
        if (serverThread != null && serverThread.isAlive()) {
            Log.d(TAG, "P2P ServerThread is already active on port " + port);
            return;
        }

        serverThread = new ServerThread(context, port, new ServerThread.OnMessageReceivedListener() {
            @Override
            public void onHandshakeReceived(String peerName, String senderIp) {
                Log.d(TAG, "P2PSocketManager received handshake from " + peerName + " (" + senderIp + ")");
                registerPeerIp(senderIp);
                for (SocketEventListener l : listeners) {
                    try { l.onHandshakeReceived(peerName, senderIp); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }
            }

            @Override
            public void onMessageReceived(String messageText, String senderIp) {
                Log.d(TAG, "P2PSocketManager received message from " + senderIp);
                registerPeerIp(senderIp);
                for (SocketEventListener l : listeners) {
                    try { l.onMessageReceived(messageText, senderIp); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }
            }

            @Override
            public void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp) {
                Log.d(TAG, "P2PSocketManager received group message for [" + groupName + "] from " + senderName + " (" + senderIp + ")");
                registerPeerIp(senderIp);

                // 1. Notify local UI listeners
                for (SocketEventListener l : listeners) {
                    try { l.onGroupMessageReceived(groupId, groupName, senderName, messageText, senderIp); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }

                // 2. Automated Group Owner Fan-Out Relaying to all other connected clients
                relayGroupMessage(groupId, groupName, senderName, messageText, senderIp);
            }

            @Override
            public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {
                Log.d(TAG, "P2PSocketManager received file " + fileName + " from " + senderIp);
                registerPeerIp(senderIp);
                for (SocketEventListener l : listeners) {
                    try { l.onFileReceived(savedFile, fileName, fileSize, senderIp); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }
            }

            @Override
            public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {
                Log.d(TAG, "P2PSocketManager received call signal " + callSignal + " from " + senderIp);
                registerPeerIp(senderIp);
                for (SocketEventListener l : listeners) {
                    try { l.onCallSignalingReceived(callSignal, callerName, callType, senderIp); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }
            }

            @Override
            public void onServerError(String errorMessage) {
                Log.e(TAG, "P2PSocketManager server error: " + errorMessage);
                for (SocketEventListener l : listeners) {
                    try { l.onNetworkError(errorMessage); } catch (Exception e) { Log.e(TAG, "Listener error", e); }
                }
            }
        });

        serverThread.start();
        Log.d(TAG, "P2P ServerThread started on port " + port);
    }

    /**
     * Relays a group message to all other connected client devices (Group Owner fan-out).
     */
    private void relayGroupMessage(String groupId, String groupName, String senderName, String messageText, String originalSenderIp) {
        if (connectedClientIps.isEmpty()) return;

        for (String clientIp : connectedClientIps) {
            if (!clientIp.equals(originalSenderIp)) {
                Log.d(TAG, "Relaying group message to peer: " + clientIp);
                ClientTask.sendGroupMessage(clientIp, this.port, groupId, groupName, senderName, messageText, null);
            }
        }
    }

    /**
     * Broadcasts a group message to all known peers (if Group Owner) or to the Group Owner (if Client).
     */
    public void broadcastGroupMessage(String groupId, String groupName, String senderName, String messageText) {
        if (!connectedClientIps.isEmpty()) {
            // Act as Group Owner: send to all connected clients
            for (String targetIp : connectedClientIps) {
                sendGroupMessage(targetIp, groupId, groupName, senderName, messageText);
            }
        } else {
            // Act as Client: send to Group Owner gateway
            sendGroupMessage(DEFAULT_GROUP_OWNER_IP, groupId, groupName, senderName, messageText);
        }
    }

    /**
     * Broadcasts a file to all group peers (if Group Owner) or to Group Owner (if Client).
     */
    public void broadcastGroupFile(String fileName, File file) {
        if (!connectedClientIps.isEmpty()) {
            for (String targetIp : connectedClientIps) {
                sendFile(targetIp, fileName, file);
            }
        } else {
            sendFile(DEFAULT_GROUP_OWNER_IP, fileName, file);
        }
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
                for (SocketEventListener l : listeners) {
                    l.onNetworkError("Call invite failed: " + errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onNetworkError("Call signal error: " + errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onNetworkError("Handshake notice: " + errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onMessageSent(messageText);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                for (SocketEventListener l : listeners) {
                    l.onNetworkError("Group send failed: " + errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onMessageSent(messageText);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                for (SocketEventListener l : listeners) {
                    l.onNetworkError(errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onFileSent(fileName);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                for (SocketEventListener l : listeners) {
                    l.onNetworkError(errorMessage);
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
                for (SocketEventListener l : listeners) {
                    l.onFileSent(fileName);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                for (SocketEventListener l : listeners) {
                    l.onNetworkError(errorMessage);
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
