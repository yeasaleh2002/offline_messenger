package com.meshconnect.offlinechat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meshconnect.offlinechat.R;
import com.meshconnect.offlinechat.model.ChatMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter supporting heterogeneous message bubble types (Sent and Received).
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public interface OnFileClickListener {
        void onFileClick(ChatMessage message);
    }

    private final List<ChatMessage> messageList = new ArrayList<>();
    private OnFileClickListener fileClickListener;
    private static android.media.MediaPlayer mediaPlayer;
    private static String currentlyPlayingFilePath = null;

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.fileClickListener = listener;
    }

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

    public static void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
            currentlyPlayingFilePath = null;
        }
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
            ((SentMessageViewHolder) holder).bind(message, fileClickListener, this);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message, fileClickListener, this);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private void handleAttachmentClick(ChatMessage message, Context context) {
        boolean isAudio = message.getMessageType() == ChatMessage.MessageType.AUDIO
                || (message.getFileName() != null && (message.getFileName().endsWith(".m4a") || message.getFileName().endsWith(".mp3")));

        if (isAudio && message.getFilePath() != null) {
            File audioFile = new File(message.getFilePath());
            if (!audioFile.exists()) {
                if (fileClickListener != null) fileClickListener.onFileClick(message);
                return;
            }

            if (audioFile.getAbsolutePath().equals(currentlyPlayingFilePath) && mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentlyPlayingFilePath = null;
                notifyDataSetChanged();
            } else {
                releasePlayer();
                try {
                    mediaPlayer = new android.media.MediaPlayer();
                    mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    currentlyPlayingFilePath = audioFile.getAbsolutePath();
                    notifyDataSetChanged();

                    mediaPlayer.setOnCompletionListener(mp -> {
                        releasePlayer();
                        notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    android.util.Log.e("ChatAdapter", "Failed playing voice note", e);
                    releasePlayer();
                }
            }
        } else {
            if (fileClickListener != null) {
                fileClickListener.onFileClick(message);
            }
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final View layoutAttachment;
        private final android.widget.ImageView ivAttachmentIcon;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final TextView tvMessageBody;
        private final TextView tvMessageTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAttachment = itemView.findViewById(R.id.layoutAttachment);
            ivAttachmentIcon = itemView.findViewById(R.id.ivAttachmentIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvMessageBody = itemView.findViewById(R.id.tvMessageBody);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        public void bind(ChatMessage message, OnFileClickListener listener, ChatAdapter adapter) {
            boolean hasAttachment = message.getMessageType() != ChatMessage.MessageType.TEXT && message.getFileName() != null;
            if (hasAttachment) {
                layoutAttachment.setVisibility(View.VISIBLE);
                tvFileName.setText(message.getFileName());
                tvFileSize.setText(message.getFormattedFileSize());

                boolean isAudio = message.getMessageType() == ChatMessage.MessageType.AUDIO
                        || message.getFileName().endsWith(".m4a")
                        || message.getFileName().endsWith(".mp3");

                if (isAudio) {
                    boolean isPlaying = message.getFilePath() != null && message.getFilePath().equals(currentlyPlayingFilePath);
                    ivAttachmentIcon.setImageResource(isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_arrow_24);
                } else {
                    ivAttachmentIcon.setImageResource(R.drawable.ic_attach);
                }

                layoutAttachment.setOnClickListener(v -> adapter.handleAttachmentClick(message, v.getContext()));
            } else {
                layoutAttachment.setVisibility(View.GONE);
                layoutAttachment.setOnClickListener(null);
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
        private final android.widget.ImageView ivAttachmentIcon;
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final TextView tvMessageBody;
        private final TextView tvMessageTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            layoutAttachment = itemView.findViewById(R.id.layoutAttachment);
            ivAttachmentIcon = itemView.findViewById(R.id.ivAttachmentIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvMessageBody = itemView.findViewById(R.id.tvMessageBody);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        public void bind(ChatMessage message, OnFileClickListener listener, ChatAdapter adapter) {
            tvSenderName.setText(message.getSenderName());

            boolean hasAttachment = message.getMessageType() != ChatMessage.MessageType.TEXT && message.getFileName() != null;
            if (hasAttachment) {
                layoutAttachment.setVisibility(View.VISIBLE);
                tvFileName.setText(message.getFileName());
                tvFileSize.setText(message.getFormattedFileSize());

                boolean isAudio = message.getMessageType() == ChatMessage.MessageType.AUDIO
                        || message.getFileName().endsWith(".m4a")
                        || message.getFileName().endsWith(".mp3");

                if (isAudio) {
                    boolean isPlaying = message.getFilePath() != null && message.getFilePath().equals(currentlyPlayingFilePath);
                    ivAttachmentIcon.setImageResource(isPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_arrow_24);
                } else {
                    ivAttachmentIcon.setImageResource(R.drawable.ic_attach);
                }

                layoutAttachment.setOnClickListener(v -> adapter.handleAttachmentClick(message, v.getContext()));
            } else {
                layoutAttachment.setVisibility(View.GONE);
                layoutAttachment.setOnClickListener(null);
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
