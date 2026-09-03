package com.meshconnect.offlinechat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.meshconnect.offlinechat.model.ChatMessage;
import com.meshconnect.offlinechat.model.DeviceItem;
import com.meshconnect.offlinechat.model.GroupModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteOpenHelper implementation for offline local message and contact storage.
 */
public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "ChatDatabaseHelper";

    private static final String DATABASE_NAME = "mesh_chat.db";
    private static final int DATABASE_VERSION = 3;

    // Table: contacts
    public static final String TABLE_CONTACTS = "contacts";
    public static final String COLUMN_CONTACT_ID = "id";
    public static final String COLUMN_DEVICE_NAME = "device_name";
    public static final String COLUMN_MAC_ADDRESS = "mac_address";

    // Table: groups
    public static final String TABLE_GROUPS = "groups_table";
    public static final String COLUMN_GROUP_ID = "id";
    public static final String COLUMN_GROUP_NAME = "name";
    public static final String COLUMN_GROUP_CREATED_BY = "created_by";
    public static final String COLUMN_GROUP_CREATED_AT = "created_at";

    // Table: messages
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_MESSAGE_ID = "id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_MESSAGE_TEXT = "message_text";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_CONTACT_MAC = "contact_mac";
    public static final String COLUMN_IS_SENT_BY_ME = "is_sent_by_me";
    public static final String COLUMN_MESSAGE_TYPE = "message_type";
    public static final String COLUMN_FILE_PATH = "file_path";
    public static final String COLUMN_FILE_NAME = "file_name";
    public static final String COLUMN_FILE_SIZE = "file_size";
    public static final String COLUMN_MSG_GROUP_ID = "group_id";

    // Singleton instance
    private static volatile ChatDatabaseHelper instance;

    public static synchronized ChatDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ChatDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public ChatDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create contacts table
        String createContactsTable = "CREATE TABLE " + TABLE_CONTACTS + " ("
                + COLUMN_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DEVICE_NAME + " TEXT NOT NULL, "
                + COLUMN_MAC_ADDRESS + " TEXT NOT NULL UNIQUE"
                + ");";

        // Create groups table
        String createGroupsTable = "CREATE TABLE " + TABLE_GROUPS + " ("
                + COLUMN_GROUP_ID + " TEXT PRIMARY KEY, "
                + COLUMN_GROUP_NAME + " TEXT NOT NULL, "
                + COLUMN_GROUP_CREATED_BY + " TEXT, "
                + COLUMN_GROUP_CREATED_AT + " INTEGER"
                + ");";

        // Create messages table
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SENDER + " TEXT NOT NULL, "
                + COLUMN_MESSAGE_TEXT + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP + " TEXT NOT NULL, "
                + COLUMN_STATUS + " TEXT NOT NULL, "
                + COLUMN_CONTACT_MAC + " TEXT NOT NULL, "
                + COLUMN_IS_SENT_BY_ME + " INTEGER NOT NULL DEFAULT 1, "
                + COLUMN_MESSAGE_TYPE + " TEXT NOT NULL DEFAULT 'TEXT', "
                + COLUMN_FILE_PATH + " TEXT, "
                + COLUMN_FILE_NAME + " TEXT, "
                + COLUMN_FILE_SIZE + " INTEGER DEFAULT 0, "
                + COLUMN_MSG_GROUP_ID + " TEXT, "
                + "FOREIGN KEY(" + COLUMN_CONTACT_MAC + ") REFERENCES " + TABLE_CONTACTS + "(" + COLUMN_MAC_ADDRESS + ")"
                + ");";

        // Create indexes for efficient querying
        String createIndexMessageMac = "CREATE INDEX idx_messages_contact_mac ON "
                + TABLE_MESSAGES + "(" + COLUMN_CONTACT_MAC + ");";
        String createIndexMessageGroupId = "CREATE INDEX idx_messages_group_id ON "
                + TABLE_MESSAGES + "(" + COLUMN_MSG_GROUP_ID + ");";

        db.execSQL(createContactsTable);
        db.execSQL(createGroupsTable);
        db.execSQL(createMessagesTable);
        db.execSQL(createIndexMessageMac);
        db.execSQL(createIndexMessageGroupId);

        Log.d(TAG, "Database tables created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_MESSAGE_TYPE + " TEXT NOT NULL DEFAULT 'TEXT';");
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_FILE_PATH + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_FILE_NAME + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_FILE_SIZE + " INTEGER DEFAULT 0;");
            } catch (Exception e) {
                Log.e(TAG, "Error altering messages table during onUpgrade v2", e);
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GROUPS + " ("
                        + COLUMN_GROUP_ID + " TEXT PRIMARY KEY, "
                        + COLUMN_GROUP_NAME + " TEXT NOT NULL, "
                        + COLUMN_GROUP_CREATED_BY + " TEXT, "
                        + COLUMN_GROUP_CREATED_AT + " INTEGER);");
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_MSG_GROUP_ID + " TEXT;");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_group_id ON " + TABLE_MESSAGES + "(" + COLUMN_MSG_GROUP_ID + ");");
            } catch (Exception e) {
                Log.e(TAG, "Error altering messages table during onUpgrade v3", e);
            }
        }
    }

    // =========================================================================
    // CRUD Operations: CONTACTS TABLE
    // =========================================================================

    /**
     * Inserts a new peer contact or updates the device name if the MAC address already exists.
     *
     * @param deviceName Human-readable name of the peer device
     * @param macAddress MAC/Hardware or Mesh address of the peer device
     * @return Row ID of the inserted/updated record, or -1 if an error occurred
     */
    public long insertOrUpdateContact(String deviceName, String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) {
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DEVICE_NAME, (deviceName != null && !deviceName.trim().isEmpty()) ? deviceName : "Unknown Peer");
        values.put(COLUMN_MAC_ADDRESS, macAddress.trim());

        // Conflict resolution: Replace on duplicate mac_address
        return db.insertWithOnConflict(TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Retrieves all saved peer contacts from local storage.
     *
     * @return List of DeviceItem objects representing saved contacts
     */
    public List<DeviceItem> getAllContacts() {
        List<DeviceItem> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + COLUMN_DEVICE_NAME + " ASC";
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_CONTACT_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_DEVICE_NAME);
                int macIdx = cursor.getColumnIndexOrThrow(COLUMN_MAC_ADDRESS);

                do {
                    long id = cursor.getLong(idIdx);
                    String name = cursor.getString(nameIdx);
                    String mac = cursor.getString(macIdx);

                    DeviceItem device = new DeviceItem(
                            String.valueOf(id),
                            name,
                            mac,
                            DeviceItem.DeviceType.BLUETOOTH_LE,
                            0,
                            true
                    );
                    contacts.add(device);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching contacts from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contacts;
    }

    /**
     * Retrieves a single contact by MAC address.
     *
     * @param macAddress The MAC address of the peer
     * @return DeviceItem if found, or null
     */
    public DeviceItem getContactByMac(String macAddress) {
        if (macAddress == null) return null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        DeviceItem device = null;

        try {
            cursor = db.query(
                    TABLE_CONTACTS,
                    new String[]{COLUMN_CONTACT_ID, COLUMN_DEVICE_NAME, COLUMN_MAC_ADDRESS},
                    COLUMN_MAC_ADDRESS + " = ?",
                    new String[]{macAddress.trim()},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEVICE_NAME));
                String mac = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MAC_ADDRESS));

                device = new DeviceItem(String.valueOf(id), name, mac, DeviceItem.DeviceType.BLUETOOTH_LE, 0, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching contact by MAC: " + macAddress, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return device;
    }

    /**
     * Deletes a contact and optionally their corresponding messages.
     *
     * @param macAddress The MAC address of the contact to delete
     * @return Number of rows affected
     */
    public int deleteContact(String macAddress) {
        if (macAddress == null) return 0;
        SQLiteDatabase db = this.getWritableDatabase();

        // Also delete associated messages
        db.delete(TABLE_MESSAGES, COLUMN_CONTACT_MAC + " = ?", new String[]{macAddress.trim()});
        return db.delete(TABLE_CONTACTS, COLUMN_MAC_ADDRESS + " = ?", new String[]{macAddress.trim()});
    }

    // =========================================================================
    // CRUD Operations: MESSAGES TABLE
    // =========================================================================

    /**
     * Inserts a new chat message into the local offline SQLite database.
     *
     * @param message ChatMessage model containing text, sender, timestamp, and status
     * @param contactMac MAC address of the peer device associated with this conversation
     * @return Row ID of the newly inserted message, or -1 if failed
     */
    public long insertMessage(ChatMessage message, String contactMac) {
        if (message == null || contactMac == null) {
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, message.getSenderName());
        values.put(COLUMN_MESSAGE_TEXT, message.getMessageText());
        values.put(COLUMN_TIMESTAMP, message.getTimestampString());
        values.put(COLUMN_STATUS, message.getStatus() != null ? message.getStatus().name() : ChatMessage.MessageStatus.SENT.name());
        values.put(COLUMN_CONTACT_MAC, contactMac.trim());
        values.put(COLUMN_IS_SENT_BY_ME, message.isSentByMe() ? 1 : 0);
        values.put(COLUMN_MESSAGE_TYPE, message.getMessageType() != null ? message.getMessageType().name() : "TEXT");
        values.put(COLUMN_FILE_PATH, message.getFilePath());
        values.put(COLUMN_FILE_NAME, message.getFileName());
        values.put(COLUMN_FILE_SIZE, message.getFileSize());

        long insertedId = db.insert(TABLE_MESSAGES, null, values);
        if (insertedId != -1) {
            message.setDatabaseId(insertedId);
        }
        return insertedId;
    }

    /**
     * Direct helper to insert a message using raw fields.
     */
    public long insertMessageRaw(String sender, String messageText, String timestamp, String status, String contactMac, boolean isSentByMe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, sender);
        values.put(COLUMN_MESSAGE_TEXT, messageText);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_CONTACT_MAC, contactMac);
        values.put(COLUMN_IS_SENT_BY_ME, isSentByMe ? 1 : 0);
        values.put(COLUMN_MESSAGE_TYPE, "TEXT");

        return db.insert(TABLE_MESSAGES, null, values);
    }

    /**
     * Fetches all chat history messages exchanged with a specific peer device identified by MAC address.
     *
     * @param contactMac MAC address of the specific peer device
     * @return List of ChatMessage objects ordered chronologically
     */
    public List<ChatMessage> getMessagesForDevice(String contactMac) {
        List<ChatMessage> messages = new ArrayList<>();
        if (contactMac == null) return messages;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_MESSAGES,
                    null,
                    COLUMN_CONTACT_MAC + " = ?",
                    new String[]{contactMac.trim()},
                    null,
                    null,
                    COLUMN_MESSAGE_ID + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID);
                int senderIdx = cursor.getColumnIndexOrThrow(COLUMN_SENDER);
                int textIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT);
                int timeIdx = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);
                int statusIdx = cursor.getColumnIndexOrThrow(COLUMN_STATUS);
                int sentByMeIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SENT_BY_ME);
                int typeIdx = cursor.getColumnIndex(COLUMN_MESSAGE_TYPE);
                int pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH);
                int nameIdx = cursor.getColumnIndex(COLUMN_FILE_NAME);
                int sizeIdx = cursor.getColumnIndex(COLUMN_FILE_SIZE);

                do {
                    long id = cursor.getLong(idIdx);
                    String sender = cursor.getString(senderIdx);
                    String text = cursor.getString(textIdx);
                    String timestamp = cursor.getString(timeIdx);
                    String statusStr = cursor.getString(statusIdx);
                    boolean isSentByMe = cursor.getInt(sentByMeIdx) == 1;

                    ChatMessage.MessageType msgType = ChatMessage.MessageType.TEXT;
                    if (typeIdx != -1 && !cursor.isNull(typeIdx)) {
                        try {
                            msgType = ChatMessage.MessageType.valueOf(cursor.getString(typeIdx));
                        } catch (Exception ignored) {}
                    }

                    String filePath = (pathIdx != -1 && !cursor.isNull(pathIdx)) ? cursor.getString(pathIdx) : null;
                    String fileName = (nameIdx != -1 && !cursor.isNull(nameIdx)) ? cursor.getString(nameIdx) : null;
                    long fileSize = (sizeIdx != -1 && !cursor.isNull(sizeIdx)) ? cursor.getLong(sizeIdx) : 0;

                    ChatMessage.MessageStatus status;
                    try {
                        status = ChatMessage.MessageStatus.valueOf(statusStr);
                    } catch (Exception e) {
                        status = ChatMessage.MessageStatus.SENT;
                    }

                    ChatMessage msg = new ChatMessage(
                            id,
                            sender,
                            contactMac,
                            text,
                            timestamp,
                            status,
                            msgType,
                            filePath,
                            fileName,
                            fileSize,
                            isSentByMe
                    );
                    messages.add(msg);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching messages for device: " + contactMac, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Retrieves all messages across all conversations.
     *
     * @return Complete list of all stored offline messages
     */
    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_MESSAGE_ID + " ASC", null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID);
                int senderIdx = cursor.getColumnIndexOrThrow(COLUMN_SENDER);
                int textIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT);
                int timeIdx = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);
                int statusIdx = cursor.getColumnIndexOrThrow(COLUMN_STATUS);
                int contactMacIdx = cursor.getColumnIndexOrThrow(COLUMN_CONTACT_MAC);
                int sentByMeIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SENT_BY_ME);
                int typeIdx = cursor.getColumnIndex(COLUMN_MESSAGE_TYPE);
                int pathIdx = cursor.getColumnIndex(COLUMN_FILE_PATH);
                int nameIdx = cursor.getColumnIndex(COLUMN_FILE_NAME);
                int sizeIdx = cursor.getColumnIndex(COLUMN_FILE_SIZE);

                do {
                    long id = cursor.getLong(idIdx);
                    String sender = cursor.getString(senderIdx);
                    String text = cursor.getString(textIdx);
                    String timestamp = cursor.getString(timeIdx);
                    String statusStr = cursor.getString(statusIdx);
                    String contactMac = cursor.getString(contactMacIdx);
                    boolean isSentByMe = cursor.getInt(sentByMeIdx) == 1;

                    ChatMessage.MessageType msgType = ChatMessage.MessageType.TEXT;
                    if (typeIdx != -1 && !cursor.isNull(typeIdx)) {
                        try {
                            msgType = ChatMessage.MessageType.valueOf(cursor.getString(typeIdx));
                        } catch (Exception ignored) {}
                    }

                    String filePath = (pathIdx != -1 && !cursor.isNull(pathIdx)) ? cursor.getString(pathIdx) : null;
                    String fileName = (nameIdx != -1 && !cursor.isNull(nameIdx)) ? cursor.getString(nameIdx) : null;
                    long fileSize = (sizeIdx != -1 && !cursor.isNull(sizeIdx)) ? cursor.getLong(sizeIdx) : 0;

                    ChatMessage.MessageStatus status;
                    try {
                        status = ChatMessage.MessageStatus.valueOf(statusStr);
                    } catch (Exception e) {
                        status = ChatMessage.MessageStatus.SENT;
                    }

                    ChatMessage msg = new ChatMessage(
                            id,
                            sender,
                            contactMac,
                            text,
                            timestamp,
                            status,
                            msgType,
                            filePath,
                            fileName,
                            fileSize,
                            isSentByMe
                    );
                    messages.add(msg);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Updates delivery status of a specific message.
     *
     * @param messageId ID of the message to update
     * @param newStatus New status string (e.g. "DELIVERED", "FAILED")
     * @return Number of rows updated
     */
    public int updateMessageStatus(long messageId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, newStatus);

        return db.update(
                TABLE_MESSAGES,
                values,
                COLUMN_MESSAGE_ID + " = ?",
                new String[]{String.valueOf(messageId)}
        );
    }

    /**
     * Deletes all messages associated with a specific device.
     *
     * @param contactMac MAC address of the peer device
     * @return Number of deleted rows
     */
    public int deleteMessagesForDevice(String contactMac) {
        if (contactMac == null) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, COLUMN_CONTACT_MAC + " = ?", new String[]{contactMac.trim()});
    }

    /**
     * Deletes all messages in the database.
     */
    public int clearAllMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, null, null);
    }

    // =========================================================================
    // CRUD Operations: OFFLINE GROUPS
    // =========================================================================

    /**
     * Creates or updates an offline chat group.
     */
    public long createGroup(String id, String name, String createdBy) {
        if (id == null || name == null) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP_ID, id);
        values.put(COLUMN_GROUP_NAME, name);
        values.put(COLUMN_GROUP_CREATED_BY, createdBy != null ? createdBy : "Me");
        values.put(COLUMN_GROUP_CREATED_AT, System.currentTimeMillis());

        return db.insertWithOnConflict(TABLE_GROUPS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Retrieves all saved offline chat groups.
     */
    public List<GroupModel> getAllGroups() {
        List<GroupModel> groups = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_GROUPS + " ORDER BY " + COLUMN_GROUP_CREATED_AT + " DESC";
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_GROUP_NAME);
                int authorIdx = cursor.getColumnIndexOrThrow(COLUMN_GROUP_CREATED_BY);
                int timeIdx = cursor.getColumnIndexOrThrow(COLUMN_GROUP_CREATED_AT);

                do {
                    String id = cursor.getString(idIdx);
                    String name = cursor.getString(nameIdx);
                    String author = cursor.getString(authorIdx);
                    long time = cursor.getLong(timeIdx);

                    groups.add(new GroupModel(id, name, author, time));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching groups", e);
        } finally {
            if (cursor != null) cursor.close();
        }

        return groups;
    }

    /**
     * Inserts a message belonging to an offline group.
     */
    public long insertGroupMessage(ChatMessage message, String groupId) {
        if (message == null || groupId == null) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SENDER, message.getSenderName());
        values.put(COLUMN_MESSAGE_TEXT, message.getMessageText());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_STATUS, message.getStatus().name());
        values.put(COLUMN_CONTACT_MAC, "GROUP_" + groupId);
        values.put(COLUMN_IS_SENT_BY_ME, message.isSentByMe() ? 1 : 0);
        values.put(COLUMN_MESSAGE_TYPE, message.getMessageType().name());
        values.put(COLUMN_FILE_PATH, message.getFilePath());
        values.put(COLUMN_FILE_NAME, message.getFileName());
        values.put(COLUMN_FILE_SIZE, message.getFileSize());
        values.put(COLUMN_MSG_GROUP_ID, groupId);

        return db.insert(TABLE_MESSAGES, null, values);
    }

    /**
     * Retrieves all messages for a specific group.
     */
    public List<ChatMessage> getMessagesForGroup(String groupId) {
        List<ChatMessage> messages = new ArrayList<>();
        if (groupId == null) return messages;

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE "
                + COLUMN_MSG_GROUP_ID + " = ? OR " + COLUMN_CONTACT_MAC + " = ? ORDER BY " + COLUMN_MESSAGE_ID + " ASC";
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, new String[]{groupId, "GROUP_" + groupId});
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID);
                int senderIdx = cursor.getColumnIndexOrThrow(COLUMN_SENDER);
                int textIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT);
                int timeIdx = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);
                int statusIdx = cursor.getColumnIndexOrThrow(COLUMN_STATUS);
                int isSentIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SENT_BY_ME);
                int typeIdx = cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE);
                int filePathIdx = cursor.getColumnIndexOrThrow(COLUMN_FILE_PATH);
                int fileNameIdx = cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME);
                int fileSizeIdx = cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE);

                do {
                    long id = cursor.getLong(idIdx);
                    String sender = cursor.getString(senderIdx);
                    String text = cursor.getString(textIdx);
                    String time = cursor.getString(timeIdx);
                    String statusStr = cursor.getString(statusIdx);
                    boolean isSent = cursor.getInt(isSentIdx) == 1;
                    String typeStr = cursor.getString(typeIdx);
                    String filePath = cursor.getString(filePathIdx);
                    String fileName = cursor.getString(fileNameIdx);
                    long fileSize = cursor.getLong(fileSizeIdx);

                    ChatMessage.MessageType type = ChatMessage.MessageType.TEXT;
                    try { if (typeStr != null) type = ChatMessage.MessageType.valueOf(typeStr); } catch (Exception ignored) {}

                    ChatMessage.MessageStatus status = ChatMessage.MessageStatus.DELIVERED;
                    try { if (statusStr != null) status = ChatMessage.MessageStatus.valueOf(statusStr); } catch (Exception ignored) {}

                    ChatMessage msg = new ChatMessage(
                            id, sender, groupId, text, time, status, type, filePath, fileName, fileSize, isSent
                    );
                    messages.add(msg);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching messages for group " + groupId, e);
        } finally {
            if (cursor != null) cursor.close();
        }

        return messages;
    }

    // =========================================================================
    // DATA WIPE & STORAGE CLEANUP
    // =========================================================================

    /**
     * Completely wipes all SQLite database records and permanently deletes all
     * stored files (voice notes, received attachments, sent cache, downloads).
     */
    public boolean clearAllData(Context context) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_MESSAGES, null, null);
            db.delete(TABLE_CONTACTS, null, null);
            db.delete(TABLE_GROUPS, null, null);
            Log.d(TAG, "All database tables cleared.");

            if (context != null) {
                // 1. Clear internal directories
                deleteDirectoryRecursively(new File(context.getFilesDir(), "received_files"));
                deleteDirectoryRecursively(new File(context.getFilesDir(), "sent_files"));
                deleteDirectoryRecursively(new File(context.getFilesDir(), "voice_notes"));

                // 2. Clear external download directory
                File extDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (extDir != null) {
                    deleteDirectoryRecursively(new File(extDir, "MeshConnect"));
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error while clearing all data and storage", e);
            return false;
        }
    }

    private static void deleteDirectoryRecursively(File fileOrDir) {
        if (fileOrDir != null && fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                File[] children = fileOrDir.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteDirectoryRecursively(child);
                    }
                }
            }
            fileOrDir.delete();
        }
    }
}
