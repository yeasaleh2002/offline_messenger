package com.meshconnect.offlinechat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meshconnect.offlinechat.R;
import com.meshconnect.offlinechat.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter supporting heterogeneous message bubble types (Sent and Received).
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messageList = new ArrayList<>();

    public void setMessages(List<ChatMessage> messages) {
        this.messageList.clear();
        if (messages != null) {
            this.messageList.addAll(messages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        return message.isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final View layoutAttachment;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final TextView tvMessageBody;
        private final TextView tvMessageTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAttachment = itemView.findViewById(R.id.layoutAttachment);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvMessageBody = itemView.findViewById(R.id.tvMessageBody);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        public void bind(ChatMessage message) {
            if (message.getMessageType() != ChatMessage.MessageType.TEXT && message.getFileName() != null) {
                layoutAttachment.setVisibility(View.VISIBLE);
                tvFileName.setText(message.getFileName());
                tvFileSize.setText(message.getFormattedFileSize());
            } else {
                layoutAttachment.setVisibility(View.GONE);
            }

            if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                tvMessageBody.setVisibility(View.VISIBLE);
                tvMessageBody.setText(message.getMessageText());
            } else {
                tvMessageBody.setVisibility(View.GONE);
            }

            String statusSuffix = "";
            switch (message.getStatus()) {
                case SENDING:
                    statusSuffix = " • Sending…";
                    break;
                case DELIVERED:
                    statusSuffix = " • Delivered";
                    break;
                case FAILED:
                    statusSuffix = " • Failed";
                    break;
                case SENT:
                default:
                    statusSuffix = " • Sent";
                    break;
            }
            tvMessageTime.setText(String.format("%s%s", message.getFormattedTime(), statusSuffix));
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderName;
        private final View layoutAttachment;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final TextView tvMessageBody;
        private final TextView tvMessageTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            layoutAttachment = itemView.findViewById(R.id.layoutAttachment);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvMessageBody = itemView.findViewById(R.id.tvMessageBody);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        public void bind(ChatMessage message) {
            tvSenderName.setText(message.getSenderName());

            if (message.getMessageType() != ChatMessage.MessageType.TEXT && message.getFileName() != null) {
                layoutAttachment.setVisibility(View.VISIBLE);
                tvFileName.setText(message.getFileName());
                tvFileSize.setText(message.getFormattedFileSize());
            } else {
                layoutAttachment.setVisibility(View.GONE);
            }

            if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                tvMessageBody.setVisibility(View.VISIBLE);
                tvMessageBody.setText(message.getMessageText());
            } else {
                tvMessageBody.setVisibility(View.GONE);
            }

            tvMessageTime.setText(message.getFormattedTime());
        }
    }
}
