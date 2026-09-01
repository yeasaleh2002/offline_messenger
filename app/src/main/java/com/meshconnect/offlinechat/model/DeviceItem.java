package com.meshconnect.offlinechat.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a discovered nearby P2P peer device (Bluetooth or Wi-Fi Direct).
 */
public class DeviceItem implements Serializable {

    public enum DeviceType {
        BLUETOOTH_LE,
        BLUETOOTH_CLASSIC,
        WIFI_DIRECT
    }

    private final String id;
    private final String name;
    private final String address;
    private final DeviceType type;
    private final int rssi; // Signal strength in dBm
    private boolean isPaired;

    public DeviceItem(String id, String name, String address, DeviceType type, int rssi, boolean isPaired) {
        this.id = id != null ? id : address;
        this.name = name != null && !name.isEmpty() ? name : "Unknown Device";
        this.address = address;
        this.type = type;
        this.rssi = rssi;
        this.isPaired = isPaired;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public DeviceType getType() {
        return type;
    }

    public int getRssi() {
        return rssi;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public void setPaired(boolean paired) {
        isPaired = paired;
    }

    public String getFormattedSubtitle() {
        String typeLabel = type == DeviceType.WIFI_DIRECT ? "Wi-Fi Direct" : "Bluetooth";
        if (rssi != 0) {
            return String.format("%s • %s • %d dBm", address, typeLabel, rssi);
        }
        return String.format("%s • %s", address, typeLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceItem that = (DeviceItem) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
