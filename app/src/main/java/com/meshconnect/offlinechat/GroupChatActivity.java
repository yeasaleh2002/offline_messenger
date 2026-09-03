package com.meshconnect.offlinechat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meshconnect.offlinechat.adapter.ChatAdapter;
import com.meshconnect.offlinechat.audio.VoiceRecorderHelper;
import com.meshconnect.offlinechat.db.ChatDatabaseHelper;
import com.meshconnect.offlinechat.model.ChatMessage;
import com.meshconnect.offlinechat.network.P2PSocketManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Activity for multi-peer offline group discussions (like WhatsApp groups)
 * using Wi-Fi Direct and store-and-forward mesh packet distribution.
 */
public class GroupChatActivity extends AppCompatActivity
        implements P2PSocketManager.SocketEventListener {

    private static final String TAG = "GroupChatActivity";

    private ImageButton btnBack;
    private TextView tvGroupInitial;
    private TextView tvGroupName;
    private TextView tvGroupStatus;
    private RecyclerView recyclerViewChat;
    private ImageButton btnAttachFile;
    private EditText etMessageInput;
    private FloatingActionButton btnRecordVoice;
    private FloatingActionButton btnSendMessage;

    private String groupId;
    private String groupName;
    private ChatAdapter chatAdapter;
    private ChatDatabaseHelper dbHelper;
    private P2PSocketManager socketManager;
    private VoiceRecorderHelper voiceRecorder;
    private ActivityResultLauncher<String> filePickerLauncher;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        dbHelper = ChatDatabaseHelper.getInstance(this);

        extractIntentData();
        initViews();
        setupRecyclerView();
        setupFilePicker();
        setupClickListeners();
        loadGroupMessages();
        startP2PServer();
    }

    private void extractIntentData() {
        groupId = getIntent().getStringExtra("EXTRA_GROUP_ID");
        groupName = getIntent().getStringExtra("EXTRA_GROUP_NAME");

        if (groupId == null) groupId = "GRP-GENERAL";
        if (groupName == null) groupName = "Offline Group";
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvGroupInitial = findViewById(R.id.tvGroupInitial);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupStatus = findViewById(R.id.tvGroupStatus);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        btnAttachFile = findViewById(R.id.btnAttachFile);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnRecordVoice = findViewById(R.id.btnRecordVoice);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        voiceRecorder = new VoiceRecorderHelper(this);

        tvGroupName.setText(groupName);
        if (!groupName.isEmpty()) {
            tvGroupInitial.setText(groupName.substring(0, 1).toUpperCase());
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnFileClickListener(this::openFile);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> sendGroupMessage());
        btnAttachFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnRecordVoice.setOnClickListener(v -> handleVoiceRecordClick());
    }

    private void startP2PServer() {
        socketManager = new P2PSocketManager(this, this);
        socketManager.startServer();
    }

    private void loadGroupMessages() {
        List<ChatMessage> messages = dbHelper.getMessagesForGroup(groupId);
        if (messages.isEmpty()) {
            ChatMessage welcome = new ChatMessage(
                    "system",
                    "System",
                    groupId,
                    "Welcome to \"" + groupName + "\"! Messages sent here reach all connected nearby group members offline.",
                    false
            );
            dbHelper.insertGroupMessage(welcome, groupId);
            chatAdapter.addMessage(welcome);
        } else {
            chatAdapter.setMessages(messages);
        }
        scrollToBottom();
    }

    private void sendGroupMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        ChatMessage msg = new ChatMessage(
                "my-node-id",
                "Me",
                groupId,
                text,
                true
        );
        msg.setStatus(ChatMessage.MessageStatus.DELIVERED);

        long rowId = dbHelper.insertGroupMessage(msg, groupId);
        msg.setDatabaseId(rowId);

        chatAdapter.addMessage(msg);
        etMessageInput.setText("");
        scrollToBottom();

        // Broadcast to default P2P gateway IP (192.168.49.1) or local clients
        if (socketManager != null) {
            socketManager.sendGroupMessage("192.168.49.1", groupId, groupName, android.os.Build.MODEL, text);
        }
    }

    private void handleVoiceRecordClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 103);
            return;
        }

        if (voiceRecorder.isRecording()) {
            File recordedVoice = voiceRecorder.stopRecording();
            btnRecordVoice.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.secondary_dark));

            if (recordedVoice != null && recordedVoice.exists()) {
                String fileName = recordedVoice.getName();
                ChatMessage voiceMsg = new ChatMessage(
                        "my-node-id",
                        "Me",
                        groupId,
                        "[Voice Note]",
                        ChatMessage.MessageType.AUDIO,
                        recordedVoice.getAbsolutePath(),
                        fileName,
                        recordedVoice.length(),
                        true
                );
                voiceMsg.setStatus(ChatMessage.MessageStatus.DELIVERED);
                dbHelper.insertGroupMessage(voiceMsg, groupId);
                chatAdapter.addMessage(voiceMsg);
                scrollToBottom();

                if (socketManager != null) {
                    socketManager.sendFile("192.168.49.1", fileName, recordedVoice);
                }
            }
        } else {
            try {
                voiceRecorder.startRecording();
                btnRecordVoice.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.status_offline));
                Toast.makeText(this, "Recording voice note for group... Tap again to send.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Voice record error", e);
            }
        }
    }

    private void handleSelectedFile(Uri uri) {
        try {
            String fileName = "attachment.dat";
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx);
                }
            }

            File sentDir = new File(getFilesDir(), "sent_files");
            if (!sentDir.exists()) sentDir.mkdirs();
            File localFile = new File(sentDir, System.currentTimeMillis() + "_" + fileName);

            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(localFile)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
                fos.flush();
            }

            ChatMessage fileMsg = new ChatMessage(
                    "my-node-id",
                    "Me",
                    groupId,
                    "[File: " + fileName + "]",
                    ChatMessage.MessageType.FILE,
                    localFile.getAbsolutePath(),
                    fileName,
                    localFile.length(),
                    true
            );
            fileMsg.setStatus(ChatMessage.MessageStatus.DELIVERED);
            dbHelper.insertGroupMessage(fileMsg, groupId);
            chatAdapter.addMessage(fileMsg);
            scrollToBottom();

            if (socketManager != null) {
                socketManager.sendFile("192.168.49.1", fileName, localFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Group file attach error", e);
        }
    }

    private void openFile(ChatMessage message) {
        if (message.getFilePath() == null) return;
        File file = new File(message.getFilePath());
        if (!file.exists()) return;

        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String mime = getContentResolver().getType(contentUri);
            if (mime == null) {
                String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                if (ext != null) mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            }
            if (mime == null) mime = "*/*";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open file with"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening group file", e);
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            recyclerViewChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    // =========================================================================
    // SocketEventListener
    // =========================================================================

    @Override
    public void onGroupMessageReceived(String rGroupId, String rGroupName, String senderName, String messageText, String senderIp) {
        if (rGroupId != null && rGroupId.equals(groupId)) {
            ChatMessage incoming = new ChatMessage(
                    senderIp,
                    senderName,
                    groupId,
                    messageText,
                    false
            );
            incoming.setStatus(ChatMessage.MessageStatus.DELIVERED);
            dbHelper.insertGroupMessage(incoming, groupId);
            chatAdapter.addMessage(incoming);
            scrollToBottom();
        }
    }

    @Override public void onHandshakeReceived(String peerName, String senderIp) {}
    @Override public void onMessageReceived(String messageText, String senderIp) {}
    @Override public void onFileReceived(File savedFile, String fileName, long fileSize, String senderIp) {}
    @Override public void onCallSignalingReceived(byte callSignal, String callerName, String callType, String senderIp) {}
    @Override public void onMessageSent(String messageText) {}
    @Override public void onFileSent(String fileName) {}
    @Override public void onNetworkError(String errorMessage) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatAdapter.releasePlayer();
        if (voiceRecorder != null) {
            voiceRecorder.cancelRecording();
        }
        if (socketManager != null) {
            socketManager.stopServer();
        }
    }
}
