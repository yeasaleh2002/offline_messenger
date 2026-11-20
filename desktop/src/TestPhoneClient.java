package com.meshconnect.desktop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestPhoneClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8888;
        String message = args.length > 0 ? String.join(" ", args) : "Hi Desktop! I am Android Client (Galaxy S24). Received your link!";

        System.out.println("Connecting to Desktop Station on " + host + ":" + port + "...");
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            dos.writeByte(0x01); // TYPE_TEXT
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

            byte ack = dis.readByte();
            System.out.println("SUCCESS: Delivered message to Desktop Station! ACK byte: " + ack);
            System.out.println("Message: \"" + message + "\"");

        } catch (Exception e) {
            System.err.println("Error sending to Desktop Station: " + e.getMessage());
        }
    }
}
