package com.meshconnect.offlinechat.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * BroadcastReceiver to listen for Wi-Fi P2P (Wi-Fi Direct) system intents.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectReceiver";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WiFiDirectManager.WiFiDirectListener listener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       WiFiDirectManager.WiFiDirectListener listener) {
        this.manager = manager;
        this.channel = channel;
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // Check if Wi-Fi P2P is enabled
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean isP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED: isEnabled=" + isP2pEnabled);
                if (listener != null) {
                    listener.onWiFiDirectStateChanged(isP2pEnabled);
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // Peer list has changed, request available peers
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED: Requesting peer list...");
                if (manager != null && channel != null) {
                    try {
                        manager.requestPeers(channel, peers -> {
                            Log.d(TAG, "Peers discovered count: " + (peers != null ? peers.getDeviceList().size() : 0));
                            if (listener != null && peers != null) {
                                listener.onPeersDiscovered(peers);
                            }
                        });
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException while requesting peers", e);
                    }
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Connection state has changed
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED: isConnected=" + (networkInfo != null && networkInfo.isConnected()));

                if (networkInfo != null && networkInfo.isConnected()) {
                    // We are connected to the peer, request connection info (IP & group owner)
                    if (manager != null && channel != null) {
                        manager.requestConnectionInfo(channel, info -> {
                            if (info != null && info.groupFormed) {
                                String groupOwnerAddress = info.groupOwnerAddress != null
                                        ? info.groupOwnerAddress.getHostAddress()
                                        : null;
                                boolean isGroupOwner = info.isGroupOwner;
                                Log.d(TAG, "Connection established! GroupOwner=" + isGroupOwner + ", IP=" + groupOwnerAddress);
                                if (listener != null) {
                                    listener.onConnectionSuccess(info, groupOwnerAddress, isGroupOwner);
                                }
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "Disconnected from Wi-Fi Direct peer.");
                    if (listener != null) {
                        listener.onDisconnected();
                    }
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // This device's Wi-Fi Direct details changed
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (device != null) {
                    Log.d(TAG, "Local device changed: " + device.deviceName + " [" + device.deviceAddress + "]");
                    if (listener != null) {
                        listener.onThisDeviceChanged(device);
                    }
                }
                break;
        }
    }
}
