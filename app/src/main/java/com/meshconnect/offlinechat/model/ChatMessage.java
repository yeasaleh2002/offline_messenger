package com.meshconnect.offlinechat.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Model representing a peer-to-peer chat message sent or received offline.
 */
public class ChatMessage implements Serializable {

    public enum MessageStatus {
        SENDING,
        SENT,
        DELIVERED,
        FAILED
    }

    public enum MessageType {
        TEXT,
        IMAGE,
        FILE
    }

    private long databaseId;
    private final String id;
    private final String senderId;
    private final String senderName;
    private final String recipientId;
    private final String messageText;
    private final long timestamp;
    private String timestampString;
    private final boolean isSentByMe;
    private MessageStatus status;
    private MessageType messageType = MessageType.TEXT;
    private String filePath;
    private String fileName;
    private long fileSize;

    public ChatMessage(String senderId, String senderName, String recipientId, String messageText, boolean isSentByMe) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.messageText = messageText;
        this.timestamp = System.currentTimeMillis();
        this.isSentByMe = isSentByMe;
        this.status = isSentByMe ? MessageStatus.DELIVERED : MessageStatus.SENT;
        this.timestampString = formatTimestamp(this.timestamp);
        this.messageType = MessageType.TEXT;
    }

    public ChatMessage(String senderId, String senderName, String recipientId, String messageText, MessageType type, String filePath, String fileName, long fileSize, boolean isSentByMe) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.messageText = messageText;
        this.timestamp = System.currentTimeMillis();
        this.isSentByMe = isSentByMe;
        this.status = isSentByMe ? MessageStatus.DELIVERED : MessageStatus.SENT;
        this.timestampString = formatTimestamp(this.timestamp);
        this.messageType = type;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public ChatMessage(long databaseId, String senderName, String recipientId, String messageText, String timestampString, MessageStatus status, boolean isSentByMe) {
        this.databaseId = databaseId;
        this.id = String.valueOf(databaseId);
        this.senderId = senderName;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.messageText = messageText;
        this.timestamp = parseTimestampString(timestampString);
        this.timestampString = timestampString;
        this.status = status;
        this.isSentByMe = isSentByMe;
        this.messageType = MessageType.TEXT;
    }

    public ChatMessage(long databaseId, String senderName, String recipientId, String messageText, String timestampString, MessageStatus status, MessageType messageType, String filePath, String fileName, long fileSize, boolean isSentByMe) {
        this.databaseId = databaseId;
        this.id = String.valueOf(databaseId);
        this.senderId = senderName;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.messageText = messageText;
        this.timestamp = parseTimestampString(timestampString);
        this.timestampString = timestampString;
        this.status = status;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isSentByMe = isSentByMe;
    }

    public long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(long databaseId) {
        this.databaseId = databaseId;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getMessageText() {
        return messageText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTimestampString() {
        if (timestampString == null || timestampString.isEmpty()) {
            timestampString = formatTimestamp(timestamp);
        }
        return timestampString;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public MessageType getMessageType() {
        return messageType != null ? messageType : MessageType.TEXT;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormattedFileSize() {
        if (fileSize <= 0) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", fileSize / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    public String getFormattedTime() {
        if (timestampString != null && !timestampString.isEmpty()) {
            return timestampString;
        }
        return formatTimestamp(timestamp);
    }

    private static String formatTimestamp(long timeMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeMs));
    }

    private static long parseTimestampString(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
