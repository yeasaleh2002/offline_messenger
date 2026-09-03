package com.meshconnect.offlinechat;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.meshconnect.offlinechat.adapter.DeviceAdapter;
import com.meshconnect.offlinechat.db.ChatDatabaseHelper;
import com.meshconnect.offlinechat.model.DeviceItem;
import com.meshconnect.offlinechat.wifi.WiFiDirectManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for discovering nearby Wi-Fi Direct peers and viewing offline saved contacts.
 */
public class DeviceListActivity extends AppCompatActivity implements
        WiFiDirectManager.WiFiDirectListener,
        DeviceAdapter.OnDeviceClickListener {

    private static final String TAG = "DeviceListActivity";

    private Toolbar toolbar;
    private ProgressBar scanProgressBar;
    private TextView tvScanningStatus;
    private MaterialButton btnToggleScan;
    private RecyclerView recyclerViewDevices;
    private LinearLayout layoutEmptyState;

    private DeviceAdapter deviceAdapter;
    private ChatDatabaseHelper dbHelper;
    private WiFiDirectManager wiFiDirectManager;

    private boolean isHistoryMode = false;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());

    // Tracks if connection was explicitly initiated so we don't loop redirect on back press
    private boolean userInitiatedConnect = false;

    // Map to keep reference to actual WifiP2pDevice objects by MAC address
    private final Map<String, WifiP2pDevice> p2pDeviceMap = new HashMap<>();
    private DeviceItem connectingDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        dbHelper = ChatDatabaseHelper.getInstance(this);
        dbHelper.deleteDummyContacts(); // Clean any historical mock devices

        wiFiDirectManager = new WiFiDirectManager(this, this);

        String mode = getIntent().getStringExtra("MODE");
        isHistoryMode = "HISTORY".equals(mode);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupScanControls();

        if (isHistoryMode) {
            loadSavedContactsFromDatabase();
        } else {
            startScanning();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        wiFiDirectManager.registerReceiver(this);
        if (isHistoryMode) {
            loadSavedContactsFromDatabase();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isScanning) {
            stopScanning();
        }
        wiFiDirectManager.unregisterReceiver(this);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        scanProgressBar = findViewById(R.id.scanProgressBar);
        tvScanningStatus = findViewById(R.id.tvScanningStatus);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (isHistoryMode) {
            toolbar.setTitle(R.string.btn_chat_history);
        } else {
            toolbar.setTitle(R.string.device_list_title);
        }
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceAdapter(this);
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void setupScanControls() {
        if (isHistoryMode) {
            scanProgressBar.setVisibility(View.GONE);
            btnToggleScan.setVisibility(View.GONE);
            tvScanningStatus.setText("Offline Saved Nodes & Contacts");
            return;
        }

        btnToggleScan.setOnClickListener(v -> {
            if (isScanning) {
                stopScanning();
            } else {
                startScanning();
            }
        });
    }

    private void loadSavedContactsFromDatabase() {
        List<DeviceItem> savedContacts = dbHelper.getAllContacts();
        List<DeviceItem> realContacts = new ArrayList<>();
        for (DeviceItem item : savedContacts) {
            String name = item.getName() != null ? item.getName() : "";
            if (!name.contains("Pixel 8") && !name.contains("Samsung")) {
                realContacts.add(item);
            }
        }
        deviceAdapter.setDevices(realContacts);
        updateEmptyState();
    }

    private void startScanning() {
        isScanning = true;
        scanProgressBar.setVisibility(View.VISIBLE);
        tvScanningStatus.setText(R.string.scanning_peers);
        btnToggleScan.setText(R.string.stop_scan);
        layoutEmptyState.setVisibility(View.GONE);

        deviceAdapter.clearDevices();
        p2pDeviceMap.clear();

        // Trigger real Android Wi-Fi Direct peer discovery
        wiFiDirectManager.discoverPeers();

        // Scan timeout after 12 seconds
        scanHandler.postDelayed(this::stopScanning, 12000);
    }

    private void stopScanning() {
        isScanning = false;
        scanProgressBar.setVisibility(View.INVISIBLE);
        tvScanningStatus.setText("Scan completed. Found " + deviceAdapter.getItemCount() + " peers.");
        btnToggleScan.setText(R.string.rescan);
        wiFiDirectManager.stopPeerDiscovery();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (deviceAdapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewDevices.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewDevices.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================================
    // WiFiDirectManager Callbacks
    // =========================================================================

    @Override
    public void onWiFiDirectStateChanged(boolean isEnabled) {
        if (!isEnabled) {
            Toast.makeText(this, "Please enable Wi-Fi on your device to discover peers.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersDiscovered(WifiP2pDeviceList peerList) {
        if (peerList != null && !peerList.getDeviceList().isEmpty()) {
            for (WifiP2pDevice device : peerList.getDeviceList()) {
                p2pDeviceMap.put(device.deviceAddress, device);

                DeviceItem deviceItem = new DeviceItem(
                        device.deviceAddress,
                        device.deviceName != null && !device.deviceName.isEmpty() ? device.deviceName : "Wi-Fi Direct Peer",
                        device.deviceAddress,
                        DeviceItem.DeviceType.WIFI_DIRECT,
                        -50,
                        device.status == WifiP2pDevice.CONNECTED
                );

                dbHelper.insertOrUpdateContact(deviceItem.getName(), deviceItem.getAddress());
                deviceAdapter.addDevice(deviceItem);
            }
            updateEmptyState();
        }
    }

    @Override
    public void onConnectionInitiated() {
        userInitiatedConnect = true;
        Toast.makeText(this, "Wi-Fi Direct connection requested...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuccess(WifiP2pInfo info, String groupOwnerAddress, boolean isGroupOwner) {
        Toast.makeText(this, "Wi-Fi Direct connected! Role: " + (isGroupOwner ? "Group Owner" : "Client"), Toast.LENGTH_LONG).show();

        // If connection was already established and the user backed out to DeviceListActivity,
        // do not auto-redirect them back into the chat screen!
        if (!userInitiatedConnect) {
            Log.d(TAG, "Connection already active; suppressing auto-redirect to preserve back navigation.");
            return;
        }
        userInitiatedConnect = false;

        String peerIp = isGroupOwner ? null : groupOwnerAddress;

        DeviceItem target = connectingDevice != null ? connectingDevice : new DeviceItem(
                "p2p-node",
                "Connected Peer",
                groupOwnerAddress != null ? groupOwnerAddress : "192.168.49.1",
                DeviceItem.DeviceType.WIFI_DIRECT,
                0,
                true
        );

        openChatScreen(target, peerIp, isGroupOwner);
    }

    @Override
    public void onDisconnected() {
        userInitiatedConnect = false;
        Toast.makeText(this, "Wi-Fi Direct peer disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onThisDeviceChanged(WifiP2pDevice device) {
        // Local device info updated
    }

    @Override
    public void onError(String errorReason) {
        userInitiatedConnect = false;
        Toast.makeText(this, errorReason, Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // DeviceAdapter Click Handling
    // =========================================================================

    @Override
    public void onDeviceClick(DeviceItem device) {
        this.connectingDevice = device;
        this.userInitiatedConnect = true;
        Toast.makeText(this, "Connecting to " + device.getName() + "...", Toast.LENGTH_SHORT).show();

        WifiP2pDevice p2pDevice = p2pDeviceMap.get(device.getAddress());
        if (p2pDevice != null) {
            // Initiate real Wi-Fi Direct connection
            wiFiDirectManager.connect(p2pDevice);
        } else {
            // Open direct chat for saved peers
            openChatScreen(device, "192.168.49.1", false);
        }
    }

    private void openChatScreen(DeviceItem device, String peerIpAddress, boolean isGroupOwner) {
        userInitiatedConnect = false;
        Intent intent = new Intent(DeviceListActivity.this, ChatActivity.class);
        intent.putExtra("EXTRA_PEER_ID", device.getId());
        intent.putExtra("EXTRA_PEER_NAME", device.getName());
        intent.putExtra("EXTRA_PEER_ADDRESS", device.getAddress());
        intent.putExtra("EXTRA_PEER_TYPE", device.getType().name());
        intent.putExtra("EXTRA_PEER_IP", peerIpAddress);
        intent.putExtra("EXTRA_IS_GROUP_OWNER", isGroupOwner);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacksAndMessages(null);
        wiFiDirectManager.stopPeerDiscovery();
    }
}
