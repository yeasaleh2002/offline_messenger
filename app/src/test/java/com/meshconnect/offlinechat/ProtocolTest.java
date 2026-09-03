package com.meshconnect.offlinechat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.meshconnect.offlinechat.network.ServerThread;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtocolTest {

    @Test
    public void testHandshakePacketProtocol() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String deviceName = "Pixel 8 Pro";
        byte[] nameBytes = deviceName.getBytes(StandardCharsets.UTF_8);

        // Handshake format: TYPE_HANDSHAKE (0x03) + Short length + UTF-8 bytes
        dos.writeByte(ServerThread.TYPE_HANDSHAKE);
        dos.writeShort(nameBytes.length);
        dos.write(nameBytes);
        dos.flush();

        // Read and verify
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        byte packetType = dis.readByte();
        assertEquals(ServerThread.TYPE_HANDSHAKE, packetType);

        short len = dis.readShort();
        byte[] readBytes = new byte[len];
        dis.readFully(readBytes);
        String decodedName = new String(readBytes, StandardCharsets.UTF_8);

        assertEquals(deviceName, decodedName);
    }

    @Test
    public void testTextMessagePacketProtocol() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String textMessage = "Offline P2P Mesh Test Message";
        byte[] textBytes = textMessage.getBytes(StandardCharsets.UTF_8);

        // Text format: TYPE_TEXT (0x01) + Int length + UTF-8 bytes
        dos.writeByte(ServerThread.TYPE_TEXT);
        dos.writeInt(textBytes.length);
        dos.write(textBytes);
        dos.flush();

        // Read and verify
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        byte packetType = dis.readByte();
        assertEquals(ServerThread.TYPE_TEXT, packetType);

        int len = dis.readInt();
        byte[] readBytes = new byte[len];
        dis.readFully(readBytes);
        String decodedMessage = new String(readBytes, StandardCharsets.UTF_8);

        assertEquals(textMessage, decodedMessage);
    }

    @Test
    public void testFilePacketProtocol() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String fileName = "sample_photo.jpg";
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] dummyFileContent = "FAKE_BINARY_IMAGE_DATA_12345".getBytes(StandardCharsets.UTF_8);

        // File format: TYPE_FILE (0x02) + Short filename length + Filename + Long file size + Raw bytes
        dos.writeByte(ServerThread.TYPE_FILE);
        dos.writeShort(nameBytes.length);
        dos.write(nameBytes);
        dos.writeLong(dummyFileContent.length);
        dos.write(dummyFileContent);
        dos.flush();

        // Read and verify
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        byte packetType = dis.readByte();
        assertEquals(ServerThread.TYPE_FILE, packetType);

        short nameLen = dis.readShort();
        byte[] readNameBytes = new byte[nameLen];
        dis.readFully(readNameBytes);
        String decodedName = new String(readNameBytes, StandardCharsets.UTF_8);
        assertEquals(fileName, decodedName);

        long fileSize = dis.readLong();
        assertEquals(dummyFileContent.length, fileSize);

        byte[] readContent = new byte[(int) fileSize];
        dis.readFully(readContent);
        assertArrayEquals(dummyFileContent, readContent);
    }

    @Test
    public void testAckByteConstant() {
        assertEquals(0x06, ServerThread.TYPE_ACK);
    }

    @Test
    public void testCallSignalingPacketProtocol() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String callerName = "Alice";
        byte[] nameBytes = callerName.getBytes(StandardCharsets.UTF_8);
        boolean isVideo = true;

        // Invite format: TYPE_CALL_INVITE (0x05) + Short length + Caller Name + Byte callMode (1=Video, 0=Audio)
        dos.writeByte(ServerThread.TYPE_CALL_INVITE);
        dos.writeShort(nameBytes.length);
        dos.write(nameBytes);
        dos.writeByte(isVideo ? 1 : 0);
        dos.flush();

        // Read and verify
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        byte packetType = dis.readByte();
        assertEquals(ServerThread.TYPE_CALL_INVITE, packetType);

        short nameLen = dis.readShort();
        byte[] readBytes = new byte[nameLen];
        dis.readFully(readBytes);
        String decodedCaller = new String(readBytes, StandardCharsets.UTF_8);
        assertEquals(callerName, decodedCaller);

        byte callMode = dis.readByte();
        assertEquals(1, callMode);

        // Verify call response signal constants
        assertEquals(0x07, ServerThread.TYPE_CALL_ACCEPT);
        assertEquals(0x08, ServerThread.TYPE_CALL_DECLINE);
        assertEquals(0x09, ServerThread.TYPE_CALL_END);
    }
}
