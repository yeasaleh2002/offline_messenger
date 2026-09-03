package com.meshconnect.offlinechat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.meshconnect.offlinechat.db.ChatDatabaseHelper;
import com.meshconnect.offlinechat.network.P2PSocketManager;
import com.meshconnect.offlinechat.network.ServerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Home dashboard activity for MeshConnect offline P2P chat application.
 */
public class MainActivity extends AppCompatActivity implements P2PSocketManager.SocketEventListener {

    private MaterialCardView cardScanDevices;
    private MaterialCardView cardChatHistory;
    private MaterialCardView cardGroupChats;
    private MaterialCardView cardClearData;
    private MaterialButton btnScanDevices;
    private MaterialButton btnChatHistory;
    private MaterialButton btnGroupChats;
    private MaterialButton btnClearData;
    private TextView tvMeshStatus;
    private TextView tvMyNodeId;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNodeDiagnostics();
        setupPermissionHandler();
        setupClickListeners();
        checkAndRequestPermissions();
        initP2PSocketService();
    }

    private void initP2PSocketService() {
        P2PSocketManager socketManager = P2PSocketManager.getInstance(this);
        socketManager.registerListener(this);
        socketManager.startServer();

        // Start background service to maintain socket and wake screen for incoming calls
        try {
            Intent serviceIntent = new Intent(this, com.meshconnect.offlinechat.service.MeshForegroundService.class);
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to start MeshForegroundService", e);
        }
    }

    private void initViews() {
        cardScanDevices = findViewById(R.id.cardScanDevices);
        cardChatHistory = findViewById(R.id.cardChatHistory);
        cardGroupChats = findViewById(R.id.cardGroupChats);
        cardClearData = findViewById(R.id.cardClearData);
        btnScanDevices = findViewById(R.id.btnScanDevices);
        btnChatHistory = findViewById(R.id.btnChatHistory);
        btnGroupChats = findViewById(R.id.btnGroupChats);
        btnClearData = findViewById(R.id.btnClearData);
        tvMeshStatus = findViewById(R.id.tvMeshStatus);
        tvMyNodeId = findViewById(R.id.tvMyNodeId);
    }

    private void setupNodeDiagnostics() {
        // Generate or load a persistent unique local node ID for mesh discovery
        String shortNodeId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        tvMyNodeId.setText(String.format("My Node ID: Mesh-NODE-%s", shortNodeId));
    }

    private void setupPermissionHandler() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : result.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        tvMeshStatus.setText(R.string.status_mesh_ready);
                        Toast.makeText(this, "P2P Mesh Permissions Granted", Toast.LENGTH_SHORT).show();
                    } else {
                        tvMeshStatus.setText("Mesh Engine: Permissions Needed");
                        Toast.makeText(this, R.string.permission_rationale, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Android 12 (API 31+) Bluetooth runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }

        // Location is required for BLE and Wi-Fi Direct peer discovery
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Android 13+ Wi-Fi direct and Notifications permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        }
    }

    private void setupClickListeners() {
        // Navigation: Scan Devices
        cardScanDevices.setOnClickListener(v -> navigateToDeviceScan());
        btnScanDevices.setOnClickListener(v -> navigateToDeviceScan());

        // Navigation: Chat History
        cardChatHistory.setOnClickListener(v -> navigateToChatHistory());
        btnChatHistory.setOnClickListener(v -> navigateToChatHistory());

        // Navigation: Offline Groups
        cardGroupChats.setOnClickListener(v -> navigateToGroups());
        btnGroupChats.setOnClickListener(v -> navigateToGroups());

        // Privacy & Storage: Delete All Data
        cardClearData.setOnClickListener(v -> confirmDeleteAllData());
        btnClearData.setOnClickListener(v -> confirmDeleteAllData());
    }

    private void navigateToDeviceScan() {
        Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
        intent.putExtra("MODE", "SCAN");
        startActivity(intent);
    }

    private void navigateToChatHistory() {
        // Navigates to DeviceListActivity in HISTORY mode or opens the paired nodes
        Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
        intent.putExtra("MODE", "HISTORY");
        startActivity(intent);
    }

    private void navigateToGroups() {
        Intent intent = new Intent(MainActivity.this, GroupListActivity.class);
        startActivity(intent);
    }

    private void confirmDeleteAllData() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Data & Storage?")
                .setMessage("This will permanently wipe all messages, contacts, offline groups, voice notes, and downloaded files from your device storage.\n\nThis action cannot be undone.")
                .setIcon(R.drawable.ic_delete_forever_24)
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    boolean success = ChatDatabaseHelper.getInstance(this).clearAllData(this);
                    if (success) {
                        Toast.makeText(this, "All local data and storage wiped successfully.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to completely clear storage.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // =========================================================================
    // Background P2PSocketManager Listeners for Dashboard
    // =========================================================================

    @Override
    public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {
        if (callSignal == ServerThread.TYPE_CALL_INVITE) {
            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("EXTRA_PEER_IP", senderIp);
            intent.putExtra("EXTRA_PEER_NAME", callerName != null && !callerName.isEmpty() ? callerName : "Incoming Peer");
            intent.putExtra("EXTRA_CALL_TYPE", callType != null ? callType : "AUDIO");
            intent.putExtra("EXTRA_IS_INCOMING", true);
            startActivity(intent);
        }
    }

    @Override public void onHandshakeReceived(String peerName, String senderIp) {}
    @Override public void onMessageReceived(String messageText, String senderIp) {}
    @Override public void onGroupMessageReceived(String groupId, String groupName, String senderName, String messageText, String senderIp) {}
    @Override public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {}
    @Override public void onMessageSent(String messageText) {}
    @Override public void onFileSent(String fileName) {}
    @Override public void onNetworkError(String errorMessage) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        P2PSocketManager.getInstance(this).unregisterListener(this);
    }
}
