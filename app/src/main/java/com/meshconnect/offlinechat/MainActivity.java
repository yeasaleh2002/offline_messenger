package com.meshconnect.offlinechat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Home dashboard activity for MeshConnect offline P2P chat application.
 */
public class MainActivity extends AppCompatActivity {

    private MaterialCardView cardScanDevices;
    private MaterialCardView cardChatHistory;
    private MaterialButton btnScanDevices;
    private MaterialButton btnChatHistory;
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
    }

    private void initViews() {
        cardScanDevices = findViewById(R.id.cardScanDevices);
        cardChatHistory = findViewById(R.id.cardChatHistory);
        btnScanDevices = findViewById(R.id.btnScanDevices);
        btnChatHistory = findViewById(R.id.btnChatHistory);
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

        // Android 13+ Wi-Fi direct permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
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
}
