package com.meshconnect.offlinechat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.meshconnect.offlinechat.model.ChatMessage;
import com.meshconnect.offlinechat.model.DeviceItem;

import org.junit.Test;

public class ChatMessageTest {

    @Test
    public void testTextMessageCreation() {
        ChatMessage msg = new ChatMessage("sender-1", "Alice", "recipient-1", "Hello P2P Mesh!", true);

        assertEquals("sender-1", msg.getSenderId());
        assertEquals("Alice", msg.getSenderName());
        assertEquals("recipient-1", msg.getRecipientId());
        assertEquals("Hello P2P Mesh!", msg.getMessageText());
        assertTrue(msg.isSentByMe());
        assertEquals(ChatMessage.MessageType.TEXT, msg.getMessageType());
        assertEquals(ChatMessage.MessageStatus.DELIVERED, msg.getStatus());
        assertNotNull(msg.getFormattedTime());
    }

    @Test
    public void testFileMessageAndFormattedSizes() {
        ChatMessage fileMsgBytes = new ChatMessage(
                "sender-1", "Alice", "recipient-1", "[File: doc.pdf]",
                ChatMessage.MessageType.FILE, "/path/doc.pdf", "doc.pdf", 512, true
        );
        assertEquals("512 B", fileMsgBytes.getFormattedFileSize());

        ChatMessage fileMsgKb = new ChatMessage(
                "sender-1", "Alice", "recipient-1", "[File: img.png]",
                ChatMessage.MessageType.IMAGE, "/path/img.png", "img.png", 2048, true
        );
        assertEquals("2.0 KB", fileMsgKb.getFormattedFileSize());

        ChatMessage fileMsgMb = new ChatMessage(
                "sender-1", "Alice", "recipient-1", "[File: video.mp4]",
                ChatMessage.MessageType.FILE, "/path/video.mp4", "video.mp4", 5 * 1024 * 1024, true
        );
        assertEquals("5.0 MB", fileMsgMb.getFormattedFileSize());
    }

    @Test
    public void testDeviceItemCreation() {
        DeviceItem device = new DeviceItem(
                "dev-01", "Pixel 8 Pro", "00:11:22:33:44:55",
                DeviceItem.DeviceType.WIFI_DIRECT, -50, true
        );

        assertEquals("dev-01", device.getId());
        assertEquals("Pixel 8 Pro", device.getName());
        assertEquals("00:11:22:33:44:55", device.getAddress());
        assertEquals(DeviceItem.DeviceType.WIFI_DIRECT, device.getType());
        assertEquals(-50, device.getRssi());
        assertTrue(device.isPaired());
    }
}
