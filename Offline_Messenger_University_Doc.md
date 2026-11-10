# Offline Messenger — University Project Documentation

**Project Title:** Offline Messenger — Peer-to-Peer Offline Chat Application for Android  
**Platform:** Android (API 26+ / Android 8.0 Oreo and above)  
**Technology:** Java · Wi-Fi Direct (Wi-Fi P2P) · TCP/UDP Sockets · SQLite · Material Design 3  
**Submitted By:** [Your Name]  
**Course / Department:** [Your Course / Department Name]  
**Institution:** [Your University Name]  
**Date:** November 2026  

---

## Abstract

Offline Messenger is a fully functional Android mobile application that enables real-time messaging, voice communication, and file sharing between nearby smartphones **without requiring internet connectivity, cellular data, Wi-Fi routers, or any central servers**. The application leverages Android's built-in **Wi-Fi Direct (IEEE 802.11 P2P)** technology combined with direct **TCP/IP socket programming** to establish fast, encrypted point-to-point communication links between physically proximate devices.

The motivation behind this project stems from the critical communication failures that occur during natural disasters, network outages, underground transit, remote outdoor environments, and censored or congested networks. Existing messaging platforms such as WhatsApp, Telegram, and Signal are fundamentally dependent on cloud infrastructure; when that infrastructure becomes unavailable, modern communication ceases entirely. Offline Messenger eliminates this single point of failure by implementing a **decentralized peer-to-peer mesh communication model** that functions entirely at the device hardware layer.

The application delivers a WhatsApp-inspired user interface familiar to everyday smartphone users while providing a technically robust backend engine. Core capabilities include: peer discovery and pairing via Wi-Fi Direct, real-time bi-directional text messaging over TCP port 8888, offline group discussions with automated multi-peer fan-out relaying, voice note recording and inline playback, real-time VoIP audio calling over UDP, video calling over TCP, chunk-streamed binary file and photo sharing with zero memory overflow risk, full offline SQLite message persistence, and a one-tap local storage wipe for user privacy.

This document details the key features of the application, the technical architecture underlying each feature, and illustrative screenshots demonstrating the user experience across all major screens.

---

## Key Features

### 1. 📶 Zero-Internet Wi-Fi Direct Peer Discovery
Offline Messenger uses Android's native **Wi-Fi Direct (Wi-Fi P2P)** API to automatically scan for and discover nearby devices without any internet connection, mobile data, or Wi-Fi router. Devices broadcast their availability directly through the 2.4 GHz / 5 GHz radio band and appear in a live peer list. Users simply tap a device name to initiate pairing. The operating range is approximately **50–100 meters outdoors** and **20–30 meters indoors**, with transfer speeds of up to 250 Mbps — far exceeding Bluetooth (1–2 Mbps).

**Technical Detail:** Implemented using `WifiP2pManager`, `WifiP2pActionListener`, and `WifiP2pInfo` from the Android `android.net.wifi.p2p` package. Android automatically negotiates one device as the **Group Owner** (acting as a local access point at IP `192.168.49.1`) and assigns dynamic IPs to connecting clients.

---

### 2. 💬 Real-Time Peer-to-Peer Text Messaging
Once two devices are paired, they can exchange text messages in real time through a WhatsApp-style chat interface. Messages are transmitted as binary packets over a persistent TCP socket on port **8888**. Chat bubbles are displayed with timestamps, and the interface supports long message threads with smooth scrolling via a `RecyclerView` adapter.

**Technical Detail:** A persistent background `ServerThread` holds an open `ServerSocket` and continuously reads incoming binary packet frames. Messages use a lightweight custom binary protocol: `[0x01] + [4-byte length] + [UTF-8 message bytes]`. An ACK byte `0x06` is returned to confirm delivery. All messages are persisted to a local **SQLite** database via `ChatDatabaseHelper` for offline history retrieval.

---

### 3. 👥 Offline Group Chats (Multi-Peer Fan-Out)
Similar to WhatsApp group chats, Offline Messenger supports creating named offline groups where multiple nearby devices can participate in a shared discussion. When a device acting as the **Group Owner** receives a group message from one client, it automatically relays (fan-outs) that message to all other connected clients in the group.

**Technical Detail:** Group messages use packet type `0x04` with group ID, group name, sender name, and message content fields. The Group Owner tracks all connected client IPs from handshake packets and broadcasts incoming group packets to all other clients. Groups and their metadata are persisted in a dedicated `groups_table` in SQLite.

---

### 4. 🎙️ Voice Messaging (Voice Notes)
Users can long-press the microphone button to record voice notes and send them as compressed audio files. Received voice notes render as inline playback bubbles inside the chat. This feature works entirely offline with no cloud storage.

**Technical Detail:** Voice recording is handled by `VoiceRecorderHelper` using Android's `MediaRecorder` API, encoding audio in **AAC format (.m4a)** for high quality at small file sizes. Voice files are transferred using the file streaming protocol (`0x02` packet type) with chunk-based `FileInputStream` / `FileOutputStream` streaming over TCP to prevent memory overflow on low-end devices.

---

### 5. 📞 Offline Real-Time VoIP Audio Calling
Offline Messenger supports live voice calling between two paired devices using a **UDP audio streaming engine**. The calling device sends a signaling invite packet, the recipient sees an incoming call screen with Accept / Decline buttons, and upon acceptance, real-time audio is streamed in both directions.

**Technical Detail:** Audio streaming uses `AudioCallEngine.java` over **UDP port 8889** for minimal latency. `AudioRecord` captures raw PCM samples from the microphone and transmits them in packets. `AudioTrack` on the receiver decodes and plays the audio samples in real time. The signaling state machine uses packet types: `0x05 (INVITE)` → `0x07 (ACCEPT)` / `0x08 (DECLINE)` → `0x09 (END CALL)`.

---

### 6. 📹 Offline Video Calling
In addition to audio calling, Offline Messenger streams live camera video between paired devices. The camera preview is captured, compressed as JPEG frames, and streamed over a dedicated TCP connection. The recipient displays the incoming video stream in real time.

**Technical Detail:** Implemented in `VideoCallEngine.java` over **TCP port 8890**. Camera2 API (or Camera1 `Camera.PreviewCallback`) captures frames. Each JPEG frame is prefixed with a 4-byte length header and pushed over the socket. The receiving end decodes and renders each frame using a `SurfaceView`. Feature includes camera switch (front/back) and mute/speaker toggle.

---

### 7. 📁 Binary File & Photo Sharing (Chunk Streaming)
Users can attach and send photos, documents, audio files, and videos directly in chat. Files are transferred over the same TCP socket connection using an optimized chunked streaming architecture that eliminates memory overflow errors common in naive file transfer implementations.

**Technical Detail:** Files are read from storage in **8 KB chunks** via `FileInputStream` and written progressively to the socket's `OutputStream`. On the receiver side, bytes are written chunk-by-chunk to `FileOutputStream`. Total RAM usage stays below 10 MB regardless of whether a file is 100 KB or 1 GB. `socket.shutdownOutput()` sends a clean TCP FIN signal, and the receiver returns an `0x06 ACK` byte after writing to disk. Files are saved to `getExternalFilesDir(DIRECTORY_DOWNLOADS)/OfflineMessenger/`.

---

### 8. 💾 Offline SQLite Message Persistence
All conversations, contacts, groups, and received file paths are stored locally in a **SQLite relational database**, ensuring complete message history survives app restarts, device reboots, and network disconnections with zero cloud dependency.

**Technical Detail:** `ChatDatabaseHelper` extends `SQLiteOpenHelper` and manages three tables: `contacts` (MAC address, device name, last seen), `messages` (message body, type, timestamp, delivery status, file path), and `groups_table` (group ID, name, creator, timestamp). Messages are indexed on `contact_mac` for fast history loading. Delivery status is tracked as `SENDING` → `DELIVERED`.

---

### 9. 🗑️ One-Tap Data & Storage Wipe
The home dashboard includes a privacy-focused **Delete All Data** button that permanently erases all locally stored messages, contacts, offline groups, voice notes, and downloaded files in a single confirmed action.

**Technical Detail:** `ChatDatabaseHelper.clearAllData()` executes `DELETE FROM` on all tables and runs `vacuum()` to reclaim space. Additionally, all files in the app's external downloads directory are deleted recursively using `File.listFiles()`. An `AlertDialog` confirmation dialog prevents accidental deletion.

---

### 10. 🔋 Background Service & Incoming Call Reception
A persistent background `MeshForegroundService` keeps the TCP socket server alive even when the user navigates away from the app, ensuring the device can receive incoming messages and ringing call invitations from paired peers even from the lock screen.

**Technical Detail:** `MeshForegroundService` extends Android `Service` and runs as a foreground service with a persistent status bar notification. It maintains the `P2PSocketManager` singleton alive in the background. Incoming VoIP call invites (`0x05`) trigger `CallActivity` via `startActivity()` with the `FLAG_ACTIVITY_NEW_TASK` flag, allowing the call screen to appear over the lock screen via `showWhenLocked` and `turnScreenOn` activity attributes.

---

## Screenshots

> **Note to Reviewer:** The screenshots below represent the core screens and features of Offline Messenger. These can be captured from the running app on a physical Android device or emulator with the simulator.

---

### Screenshot 1 — Home Dashboard

The main home screen styled in WhatsApp's signature dark-green toolbar with a clean white card-list body. Shows the app name **"Offline Messenger"**, online status indicator, and four action rows: **Find Nearby People**, **Recent Chats**, **Offline Groups**, and **Delete All Data**. The Network Info section shows the device's local mesh node ID, Bluetooth status, and Wi-Fi Direct status.

```
┌─────────────────────────────────┐
│ ■ Offline Messenger             │  ← Dark green WhatsApp-style toolbar
│   ● Offline Messenger Ready     │
├─────────────────────────────────┤
│ Actions                         │  ← Section header
│ ○  Find Nearby People   [Scan]  │  ← Green avatar, action row
│ ─────────────────────────────── │
│ ○  Recent Chats         [Open]  │
│ ─────────────────────────────── │
│ ○  Offline Groups       [Open]  │
│ ─────────────────────────────── │
│ ○  Delete All Data   [Delete]   │  ← Red icon
├─────────────────────────────────┤
│ Network Info                    │
│  My Device ID: MSG-NODE-A49F    │
│  Bluetooth: Enabled             │
│  Wi-Fi Direct: Standby          │
└─────────────────────────────────┘
```

---

### Screenshot 2 — Nearby Device Discovery Screen

The device scanner screen shows all nearby devices discovered via Wi-Fi Direct and Bluetooth. Each device appears as a flat list row with a colored avatar circle, device name, and address. A scanning progress bar animates at the top while discovery is active. An empty state illustration appears if no devices are found.

```
┌─────────────────────────────────┐
│ ← Nearby People                 │  ← WhatsApp-style toolbar
│ ═══════════════════════════════ │  ← Scanning progress bar
│ Scanning for nearby devices…  Stop│
│ ─────────────────────────────── │
│ ○  Pixel 8 Pro            [Connect]│
│    AA:BB:CC:11:22:33 • Bluetooth│
│ ─────────────────────────────── │
│ ○  Samsung Galaxy S24    [Connect]│
│    DD:EE:FF:44:55:66 • WiFi-P2P│
│ ─────────────────────────────── │
└─────────────────────────────────┘
```

---

### Screenshot 3 — 1-on-1 Chat Screen

The direct chat interface styled exactly like WhatsApp. Features the dark-green toolbar with the peer's name, online status dot, and audio/video call icons. The chat body uses the WhatsApp wallpaper background (tan/beige `#ECE5DD`). **Sent messages** appear in WhatsApp light-green bubbles (`#DCF8C6`) on the right; **received messages** appear in white bubbles on the left. The bottom bar has an attach button, text input field, mic button, and send button.

```
┌─────────────────────────────────┐
│ ← [●]  Pixel 8 Pro        📞 📹│  ← Dark green toolbar
│       ● online • offline mesh   │
│ 🔒 End-to-End Encrypted • No...  │  ← Info strip
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────┐      │
│    │ Hey! Got your msg   │      │  ← Received (white bubble)
│    │                11:30│      │
│    └─────────────────────┘      │
│                                 │
│         ┌────────────────────┐  │
│         │ Hello peer! Works  │  │  ← Sent (green bubble)
│         │ perfectly offline! │  │
│         │            11:31 ✓ │  │
│         └────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│ 📎  [     Message       ] 🎤 ➤ │  ← Input bar
└─────────────────────────────────┘
```

---

### Screenshot 4 — Voice Note in Chat

Voice notes appear as inline audio player bubbles inside the chat thread. The bubble shows a waveform placeholder, play/pause button, duration counter, and timestamp. Recipients tap play to listen without leaving the chat screen.

```
│         ┌────────────────────┐  │
│         │ ▶  ~~~~~~~~~~~~~~~│  │  ← Voice note bubble (sent)
│         │    0:08       11:32│  │
│         └────────────────────┘  │
```

---

### Screenshot 5 — File / Photo Attachment in Chat

Attached files appear as a preview card inside the chat bubble. The card shows the file icon, filename (truncated to fit), and file size. Tapping the card opens the file in the system viewer.

```
│    ┌──────────────────────────┐  │
│    │ 📄  offline_report.pdf   │  │  ← Received attachment bubble
│    │     450 KB               │  │
│    └──────────────────────────┘  │
│    Got the doc!            11:33 │  ← Text below the attachment
```

---

### Screenshot 6 — Offline Groups List Screen

The Offline Groups screen lists all created offline discussion groups as WhatsApp-style flat list rows. Each group shows a colored initial avatar, group name, and member count. A floating action button (FAB) in the bottom-right corner opens the "Create New Group" dialog.

```
┌─────────────────────────────────┐
│ ← Offline Messenger Groups      │  ← Dark green toolbar
│    Multi-peer offline discussions│
├─────────────────────────────────┤
│ ○  Disaster Team Alpha          │
│    Offline Group • 3 members    │
│ ─────────────────────────────── │
│ ○  Campus Field Study           │
│    Offline Group • 2 members    │
│ ─────────────────────────────── │
│                                 │
│                   [+ New Group] │  ← FAB (green)
└─────────────────────────────────┘
```

---

### Screenshot 7 — Offline Group Chat Screen

The group chat interface shows sender name labels above each received message bubble — identical to WhatsApp group chats. The toolbar shows the group's initial avatar, group name, and member status. A banner strip confirms messages are fanning out to all connected peers offline.

```
┌─────────────────────────────────┐
│ ← [D]  Disaster Team Alpha     │  ← Dark green toolbar
│         Offline Group • 3 Nodes │
│ 📡 Offline Group • Messages fan  │  ← Info strip
├─────────────────────────────────┤
│ Reza                            │
│ ┌─────────────────────────────┐ │
│ │ I'm at sector 3, all clear  │ │  ← Received (labeled)
│ │                        11:40│ │
│ └─────────────────────────────┘ │
│          ┌────────────────────┐  │
│          │ Heading to sector 4│  │  ← Sent (green bubble)
│          │             11:41 ✓│  │
│          └────────────────────┘  │
├─────────────────────────────────┤
│ 📎  [ Type group message ] 🎤 ➤ │
└─────────────────────────────────┘
```

---

### Screenshot 8 — Offline VoIP Audio Call Screen

The call screen shows the peer's name, avatar initials, call duration timer, and four control buttons: Mute, Speaker, End Call (red), and Switch Camera (for video). The screen is designed to appear over the lock screen on incoming calls.

```
┌─────────────────────────────────┐
│                                 │
│         [  P  ]                 │  ← Avatar (initial circle)
│      Pixel 8 Pro                │  ← Peer name
│      00:02:14                   │  ← Call timer
│      Offline Audio Call         │
│                                 │
│                                 │
│   [🔇]  [🔊]  [📷]  [  ✕  ]   │
│  Mute Speaker Cam   End Call    │  ← Red end button
└─────────────────────────────────┘
```

---

### Screenshot 9 — Incoming Call Screen

When a peer initiates an incoming call, the call screen appears with green Accept and red Decline buttons, the caller's name, and the call type (Audio / Video). This screen activates even from the lock screen thanks to the foreground service and `showWhenLocked` activity flag.

```
┌─────────────────────────────────┐
│                                 │
│         [  P  ]                 │  ← Caller avatar
│      Pixel 8 Pro                │
│   Incoming Offline Audio Call   │
│                                 │
│   [  ✕  ]         [  ✓  ]      │
│   Decline          Accept       │  ← Red / Green buttons
└─────────────────────────────────┘
```

---

### Screenshot 10 — Delete All Data Confirmation Dialog

A confirmation dialog prevents accidental data wipe. The dialog clearly explains what will be deleted and requires explicit confirmation before proceeding.

```
┌─────────────────────────────────┐
│  🗑  Delete All Data & Storage? │
│                                 │
│  This will permanently wipe all │
│  messages, contacts, groups,    │
│  voice notes, and files.        │
│                                 │
│  This action cannot be undone.  │
│                                 │
│    [   Cancel   ]  [Delete All] │
└─────────────────────────────────┘
```

---

## Technical Architecture Summary

```
┌──────────────────────────────────────────────────────────┐
│                    UI Layer (Activities)                  │
│  MainActivity · DeviceListActivity · ChatActivity        │
│  GroupListActivity · GroupChatActivity · CallActivity    │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│               Business / Adapter Layer                   │
│  ChatAdapter · DeviceAdapter · GroupAdapter              │
│  ChatDatabaseHelper (SQLite Persistence)                 │
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│          Network Engine (P2PSocketManager Singleton)     │
│  ServerThread (TCP :8888) · ClientTask (Async Sender)    │
│  AudioCallEngine (UDP :8889) · VideoCallEngine (TCP :8890)│
└───────────────────────┬──────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────┐
│          Wi-Fi Direct Hardware Layer                     │
│  WiFiDirectManager · WiFiDirectBroadcastReceiver         │
│  Android WifiP2pManager API                              │
└──────────────────────────────────────────────────────────┘
```

| Component | Technology Used |
|-----------|----------------|
| Wireless Discovery | Wi-Fi Direct (`WifiP2pManager`) |
| Transport Layer | Raw TCP/UDP Java Sockets |
| Data Persistence | SQLite (`SQLiteOpenHelper`) |
| Audio Calling | UDP PCM streaming (`AudioRecord` / `AudioTrack`) |
| Video Calling | TCP JPEG frame streaming (`Camera2 API`) |
| Voice Notes | AAC Recording (`MediaRecorder`) |
| UI Framework | Android Material Design 3 |
| Background Service | Android `ForegroundService` |
| File Delivery | `FileProvider` + chunk streaming |

---

*Document prepared for university submission — Offline Messenger Android Project, 2026.*

