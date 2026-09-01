package com.meshconnect.offlinechat.wifi;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

/**
 * Manager wrapper for Android Wi-Fi Direct (P2P) discovery and connection establishment.
 */
public class WiFiDirectManager {

    private static final String TAG = "WiFiDirectManager";

    /**
     * Interface for Wi-Fi Direct lifecycle events and connection callbacks.
     */
    public interface WiFiDirectListener {
        void onWiFiDirectStateChanged(boolean isEnabled);
        void onPeersDiscovered(WifiP2pDeviceList peerList);
        void onConnectionInitiated();
        void onConnectionSuccess(WifiP2pInfo info, String groupOwnerAddress, boolean isGroupOwner);
        void onDisconnected();
        void onThisDeviceChanged(WifiP2pDevice device);
        void onError(String errorReason);
    }

    private final Context context;
    private final WiFiDirectListener listener;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean isReceiverRegistered = false;

    public WiFiDirectManager(Context context, WiFiDirectListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initP2p();
    }

    private void initP2p() {
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(context, Looper.getMainLooper(), () -> {
                Log.w(TAG, "Wi-Fi P2P channel disconnected. Re-initializing...");
                if (listener != null) {
                    listener.onError("Channel lost. Wi-Fi P2P reinitializing.");
                }
            });
        }

        // Setup intent filter for Wi-Fi Direct actions
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    /**
     * Registers the BroadcastReceiver with the host context.
     * Should be called in Activity/Fragment onResume().
     */
    public void registerReceiver(Context activityContext) {
        if (!isReceiverRegistered && manager != null && channel != null) {
            receiver = new WiFiDirectBroadcastReceiver(manager, channel, listener);
            activityContext.registerReceiver(receiver, intentFilter);
            isReceiverRegistered = true;
            Log.d(TAG, "WiFiDirectBroadcastReceiver registered.");
        }
    }

    /**
     * Unregisters the BroadcastReceiver.
     * Should be called in Activity/Fragment onPause() or onDestroy().
     */
    public void unregisterReceiver(Context activityContext) {
        if (isReceiverRegistered && receiver != null) {
            try {
                activityContext.unregisterReceiver(receiver);
                isReceiverRegistered = false;
                Log.d(TAG, "WiFiDirectBroadcastReceiver unregistered.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was already unregistered.", e);
            }
        }
    }

    /**
     * Initiates discovery of nearby Wi-Fi Direct peers.
     */
    public void discoverPeers() {
        if (manager == null || channel == null) {
            if (listener != null) {
                listener.onError("Wi-Fi P2P framework is not supported on this device.");
            }
            return;
        }

        try {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "discoverPeers: Discovery process initiated successfully.");
                }

                @Override
                public void onFailure(int reasonCode) {
                    String reason = getFailureReason(reasonCode);
                    Log.e(TAG, "discoverPeers failed: " + reason);
                    if (listener != null) {
                        listener.onError("Peer discovery failed: " + reason);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during discoverPeers", e);
            if (listener != null) {
                listener.onError("Missing Wi-Fi / Location permissions for peer discovery.");
            }
        }
    }

    /**
     * Stops an ongoing peer discovery scan.
     */
    public void stopPeerDiscovery() {
        if (manager != null && channel != null) {
            manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "stopPeerDiscovery: Peer discovery stopped.");
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.w(TAG, "stopPeerDiscovery failed: " + getFailureReason(reasonCode));
                }
            });
        }
    }

    /**
     * Initiates a direct Wi-Fi P2P connection to a selected peer device.
     *
     * @param device The target WifiP2pDevice to connect with
     */
    public void connect(WifiP2pDevice device) {
        if (device == null || manager == null || channel == null) {
            if (listener != null) {
                listener.onError("Cannot connect: Invalid device or P2P service uninitialized.");
            }
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC; // Push Button Configuration for easy pairing

        if (listener != null) {
            listener.onConnectionInitiated();
        }

        try {
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "connect: Connection request initiated to " + device.deviceName + " [" + device.deviceAddress + "]");
                }

                @Override
                public void onFailure(int reasonCode) {
                    String reason = getFailureReason(reasonCode);
                    Log.e(TAG, "connect failed: " + reason);
                    if (listener != null) {
                        listener.onError("Connection failed: " + reason);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during connect", e);
            if (listener != null) {
                listener.onError("Missing Wi-Fi / Nearby Devices permissions to connect.");
            }
        }
    }

    /**
     * Disconnects from the current P2P group and removes group configuration.
     */
    public void disconnect() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "removeGroup: Successfully disconnected from Wi-Fi Direct group.");
                    if (listener != null) {
                        listener.onDisconnected();
                    }
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.w(TAG, "removeGroup failed: " + getFailureReason(reasonCode));
                }
            });
        }
    }

    /**
     * Manually request current connection information (group owner status & IP).
     */
    public void requestConnectionInfo() {
        if (manager != null && channel != null) {
            manager.requestConnectionInfo(channel, info -> {
                if (info != null && info.groupFormed && listener != null) {
                    String groupOwnerAddress = info.groupOwnerAddress != null
                            ? info.groupOwnerAddress.getHostAddress()
                            : null;
                    listener.onConnectionSuccess(info, groupOwnerAddress, info.isGroupOwner);
                }
            });
        }
    }

    private String getFailureReason(int reasonCode) {
        switch (reasonCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "Wi-Fi Direct is not supported on this device";
            case WifiP2pManager.ERROR:
                return "Internal framework error";
            case WifiP2pManager.BUSY:
                return "Wi-Fi P2P framework is busy";
            default:
                return "Unknown error (" + reasonCode + ")";
        }
    }
}
