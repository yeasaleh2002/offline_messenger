package com.meshconnect.offlinechat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meshconnect.offlinechat.R;
import com.meshconnect.offlinechat.model.GroupModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying offline groups.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(GroupModel group);
    }

    private final List<GroupModel> groups = new ArrayList<>();
    private final OnGroupClickListener listener;

    public GroupAdapter(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<GroupModel> newGroups) {
        this.groups.clear();
        if (newGroups != null) {
            this.groups.addAll(newGroups);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupModel group = groups.get(position);
        holder.bind(group, listener);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGroupInitial;
        private final TextView tvGroupName;
        private final TextView tvGroupSubtitle;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupInitial = itemView.findViewById(R.id.tvGroupInitial);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvGroupSubtitle = itemView.findViewById(R.id.tvGroupSubtitle);
        }

        public void bind(GroupModel group, OnGroupClickListener listener) {
            tvGroupName.setText(group.getName());

            String initial = "?";
            if (group.getName() != null && !group.getName().trim().isEmpty()) {
                initial = group.getName().trim().substring(0, 1).toUpperCase();
            }
            tvGroupInitial.setText(initial);

            tvGroupSubtitle.setText(String.format("By %s • Offline Mesh Discussion", group.getCreatedBy()));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onGroupClick(group);
            });
        }
    }
}
