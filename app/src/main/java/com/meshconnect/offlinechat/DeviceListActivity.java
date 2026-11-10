package com.meshconnect.offlinechat;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    // Navigation tracking to prevent unwanted back-redirect loops
    private boolean isOpeningChat = false;
    private boolean justReturnedFromChat = false;

    // Map to keep reference to actual WifiP2pDevice objects by MAC address
    private final Map<String, WifiP2pDevice> p2pDeviceMap = new HashMap<>();
    private DeviceItem connectingDevice = null;

    // Continuous discovery loop while scanning screen is active
    private final Runnable discoveryLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                Log.d(TAG, "Re-triggering Wi-Fi Direct peer discovery pulse...");
                wiFiDirectManager.discoverPeers();
                scanHandler.postDelayed(this, 10000); // Pulse discovery every 10 seconds
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        dbHelper = ChatDatabaseHelper.getInstance(this);
        dbHelper.deleteDummyContacts(); // Clean only exact legacy dummy entries

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

        if (isOpeningChat) {
            isOpeningChat = false;
            justReturnedFromChat = true;
        }

        if (isHistoryMode) {
            loadSavedContactsFromDatabase();
        } else if (!isScanning) {
            startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isHistoryMode) {
            menu.add(0, 101, 0, "Reset Wi-Fi Link")
                .setIcon(R.drawable.ic_radar)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 101) {
            Toast.makeText(this, "Resetting Wi-Fi Direct connection...", Toast.LENGTH_SHORT).show();
            wiFiDirectManager.disconnect();
            startScanning();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            if (!name.equals("Pixel 8 Pro (Relay Node)") && !name.equals("Samsung Galaxy S24 Ultra")) {
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

        // 1. Initial peer discovery
        wiFiDirectManager.discoverPeers();

        // 2. Start continuous discovery loop so radio stays active
        scanHandler.removeCallbacks(discoveryLoopRunnable);
        scanHandler.postDelayed(discoveryLoopRunnable, 10000);
    }

    private void stopScanning() {
        isScanning = false;
        scanHandler.removeCallbacks(discoveryLoopRunnable);
        scanProgressBar.setVisibility(View.INVISIBLE);
        tvScanningStatus.setText("Scan paused. Found " + deviceAdapter.getItemCount() + " peers.");
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
        Log.d(TAG, "onPeersDiscovered called with " + (peerList != null ? peerList.getDeviceList().size() : 0) + " devices");
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
            tvScanningStatus.setText("Found " + deviceAdapter.getItemCount() + " peer(s). Tap to connect.");
            updateEmptyState();
        }
    }

    @Override
    public void onConnectionInitiated() {
        Toast.makeText(this, "Wi-Fi Direct connection requested...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuccess(WifiP2pInfo info, String groupOwnerAddress, boolean isGroupOwner) {
        Log.d(TAG, "onConnectionSuccess: groupOwner=" + isGroupOwner + ", GO IP=" + groupOwnerAddress);
        Toast.makeText(this, "Wi-Fi Direct connected! Role: " + (isGroupOwner ? "Group Owner" : "Client"), Toast.LENGTH_LONG).show();

        String peerIp = isGroupOwner ? null : groupOwnerAddress;

        DeviceItem target = connectingDevice != null ? connectingDevice : new DeviceItem(
                "p2p-connected",
                "Connected Peer (" + (isGroupOwner ? "Client" : "Group Owner") + ")",
                groupOwnerAddress != null ? groupOwnerAddress : "192.168.49.1",
                DeviceItem.DeviceType.WIFI_DIRECT,
                0,
                true
        );

        // Always show the connected peer in the list
        deviceAdapter.addDevice(target);
        updateEmptyState();

        // If the user just pressed back from ChatActivity, do NOT force them back in!
        if (justReturnedFromChat) {
            Log.d(TAG, "Ignoring auto-redirect: user intentionally backed out of chat.");
            justReturnedFromChat = false;
            return;
        }

        openChatScreen(target, peerIp, isGroupOwner);
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Wi-Fi Direct peer disconnected", Toast.LENGTH_SHORT).show();
        connectingDevice = null;
    }

    @Override
    public void onThisDeviceChanged(WifiP2pDevice device) {
        if (device != null) {
            Log.d(TAG, "This device Wi-Fi Direct status: " + device.deviceName + " (" + device.deviceAddress + ")");
        }
    }

    @Override
    public void onError(String errorReason) {
        Toast.makeText(this, errorReason, Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // DeviceAdapter Click Handling
    // =========================================================================

    @Override
    public void onDeviceClick(DeviceItem device) {
        this.connectingDevice = device;
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
        isOpeningChat = true;
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
