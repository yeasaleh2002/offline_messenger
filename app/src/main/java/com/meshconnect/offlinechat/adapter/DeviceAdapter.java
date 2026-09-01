package com.meshconnect.offlinechat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.meshconnect.offlinechat.R;
import com.meshconnect.offlinechat.model.DeviceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying discovered nearby peer devices.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    private final List<DeviceItem> deviceList = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setDevices(List<DeviceItem> devices) {
        this.deviceList.clear();
        if (devices != null) {
            this.deviceList.addAll(devices);
        }
        notifyDataSetChanged();
    }

    public void addDevice(DeviceItem device) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            notifyItemInserted(deviceList.size() - 1);
        }
    }

    public void clearDevices() {
        int count = deviceList.size();
        deviceList.clear();
        notifyItemRangeRemoved(0, count);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceItem device = deviceList.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardDevice;
        private final TextView tvDeviceName;
        private final TextView tvDeviceAddress;
        private final ImageView ivDeviceIcon;
        private final MaterialButton btnConnect;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDevice = itemView.findViewById(R.id.cardDevice);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }

        public void bind(final DeviceItem device, final OnDeviceClickListener listener) {
            tvDeviceName.setText(device.getName());
            tvDeviceAddress.setText(device.getFormattedSubtitle());

            if (device.getType() == DeviceItem.DeviceType.WIFI_DIRECT) {
                ivDeviceIcon.setImageResource(R.drawable.ic_radar);
            } else {
                ivDeviceIcon.setImageResource(R.drawable.ic_bluetooth);
            }

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            };

            cardDevice.setOnClickListener(clickListener);
            btnConnect.setOnClickListener(clickListener);
        }
    }
}
