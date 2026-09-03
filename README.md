# 📡 MeshConnect — Offline P2P Mesh Chat & File Transfer

> **Zero Cellular Data. Zero Wi-Fi Routers. Zero Central Servers. 100% Offline Peer-to-Peer.**

MeshConnect is a production-ready Android offline messaging and file-sharing application. It connects nearby smartphones directly using **Wi-Fi Direct (Wi-Fi P2P)** and local TCP/IP sockets, enabling real-time private communication and media exchange even during cellular network blackouts, natural disasters, underground transit, remote hiking, or crowded stadiums.

---

## 📖 Table of Contents
1. [Why This App? (Problem Statement & Purpose)](#-why-this-app-problem-statement--purpose)
2. [Key Features](#-key-features)
3. [Network Topology, Range & Ecosystem Scalability](#-network-topology-range--ecosystem-scalability)
4. [Architecture & How It Works](#-architecture--how-it-works)
5. [Packet Protocol Specification](#-packet-protocol-specification)
6. [Folder Structure & Codebase Map](#-folder-structure--codebase-map)
7. [What Was Built & Fixed (Engineering Log)](#-what-was-built--fixed-engineering-log)
8. [Junior Developer Quickstart (Run It in 5 Minutes)](#-junior-developer-quickstart-run-it-in-5-minutes)
9. [Troubleshooting & Gotchas](#-troubleshooting--gotchas)

---

## 💡 Why This App? (Problem Statement & Purpose)

Modern communication apps (WhatsApp, Telegram, Signal) depend completely on centralized infrastructure: cell towers, optical fiber cables, DNS servers, and cloud datacenters. When that infrastructure fails, modern communication ceases entirely.

### Real-World Scenarios Where MeshConnect Excels:
- **Disaster Relief & Emergencies**: Hurricanes, floods, or earthquakes destroying local cellular towers and power grids.
- **Underground / Transit**: Subways, basements, and tunnels with zero mobile reception.
- **Outdoor Expeditions**: Hiking, camping, marine sailing, or remote wilderness far beyond cell tower coverage.
- **Congested Stadiums & Concerts**: Thousands of devices overloading cell towers where standard messages take minutes to send.
- **Privacy & Censorship Resistance**: Point-to-point direct communication with no ISP, no logging server, and no middleman.

---

## ⚡ Key Features

- 📶 **Zero-Internet Wi-Fi Direct Discovery**: Scan and pair nearby devices without needing an existing Wi-Fi router or hotspot.
- 💬 **Real-Time Bidirectional Chat**: Sub-10ms local message delivery off the main thread via raw TCP sockets.
- 📁 **Chunk-Streamed File & Photo Sharing**: Transfer large photos, audio, documents, and videos directly without crashing device memory (zero-`OutOfMemoryError` streaming).
- 🤝 **Dynamic Peer Handshake**: Automated IP address resolution between Wi-Fi Direct Group Owners and Clients.
- 💾 **Offline SQLite Persistence**: Contacts, conversation threads, timestamps, and received file paths are preserved locally in SQLite.
- 🔋 **Live Link Health & Connection Drop Indicators**: Real-time visual feedback if the radio link disconnects or needs re-pairing.

---

## 🌐 Network Topology, Range & Ecosystem Scalability

A frequent question when building offline mesh systems is: **"Does this work only between 2 devices, or does it cover an entire ecosystem range when installed by thousands of people?"**

### 1. Direct Radio Range (Point-to-Point Wi-Fi Direct)
- **Open Outdoors**: Approximately **50 – 100 meters** line-of-sight.
- **Indoors (Through Walls)**: Approximately **20 – 30 meters**.
- **Bandwidth**: Up to **250 Mbps** (Wi-Fi 802.11n/ac/ax direct speeds, far superior to Bluetooth).

### 2. Wi-Fi Direct Group Topology (Star Network)
When two or more devices connect:
- Android negotiates one device as the **Group Owner (GO)** (acting as an autonomous local access point).
- The other devices connect as **Group Clients**.
- The Group Owner always takes the internal IP `192.168.49.1`, while clients get dynamic IPs (`192.168.49.xxx`).
- Multiple clients can connect to a single Group Owner simultaneously, creating a local hub.

### 3. Scaling to an Entire Ecosystem (True Multi-Hop Mesh)
When thousands of users install MeshConnect across a campus, stadium, or city:
```
[User A] ──(50m)──> [User B (Relay)] ──(50m)──> [User C (Relay)] ──(50m)──> [User D]
```
- **Hop-by-Hop Relaying (Store-and-Forward)**: If User A wants to send a message to User D (150 meters away, out of radio range), intermediate devices (User B and User C) act as relay nodes.
- **The Core Transport**: The TCP socket engine, handshake system, and stream protocol built in this repository provide the **high-speed transport layer** required for packet forwarding across multi-hop hops.

---

## 🛠 Architecture & How It Works

MeshConnect decouples connection discovery, TCP socket communication, and UI rendering cleanly into distinct layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interface Layer                     │
│  MainActivity  ◄──►  DeviceListActivity  ◄──►  ChatActivity  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Business / Adapter Layer                 │
│         ChatAdapter  •  DeviceAdapter  •  ChatDatabaseHelper│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                     Network Engine Layer                    │
│    P2PSocketManager ◄──► ServerThread (port 8888 listener)  │
│                     ◄──► ClientTask   (async TCP sender)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                  Wi-Fi Direct Hardware Layer                │
│    WiFiDirectManager ◄──► WiFiDirectBroadcastReceiver       │
└─────────────────────────────────────────────────────────────┘
```

1. **Discovery (`WiFiDirectManager`)**: Discovers nearby peers using Android's `WifiP2pManager`.
2. **Pairing**: `connect()` pairs the two devices via Push Button Configuration (PBC).
3. **Listening (`ServerThread`)**: Every device launches a background `ServerSocket` on port `8888`.
4. **Handshake (`ClientTask.sendHandshake`)**: The client immediately informs the Group Owner of its local identity and IP.
5. **Transmission (`ClientTask.sendText` & `sendFile`)**: Sends binary packets asynchronously on background threads without freezing the UI.
6. **Local Persistence (`ChatDatabaseHelper`)**: Saves every sent/received message and local file path to local SQLite.

---

## 📦 Packet Protocol Specification

All communication occurs over TCP port `8888` using binary header frames:

### 1. Handshake Packet (`0x03`)
Sent automatically by the Client to the Group Owner upon connection to register its IP address and device model:
```
[0x03] (1 byte) + [Name Length] (2 bytes short) + [Device Name UTF-8 bytes]
```
*Receiver returns a `0x06` ACK byte.*

### 2. Text Message Packet (`0x01`)
```
[0x01] (1 byte) + [Text Length] (4 bytes int) + [Message UTF-8 bytes]
```
*Receiver returns a `0x06` ACK byte.*

### 3. File / Image Packet (`0x02`)
```
[0x02] (1 byte) + [Filename Length] (2 bytes short) + [Filename UTF-8 bytes] + [File Size] (8 bytes long) + [Raw File Stream in 8KB Chunks...]
```
*Client executes `socket.shutdownOutput()` cleanly, and Receiver returns a `0x06` ACK byte upon writing to disk.*

---

## 📂 Folder Structure & Codebase Map

```
Mesh_Connect_Offline_P2P_Chat/
├── app/
│   ├── build.gradle                            # App-level dependencies & Android SDK target
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml             # Wi-Fi Direct, Bluetooth & Location permissions
│           ├── java/com/meshconnect/offlinechat/
│           │   │
│           │   ├── MainActivity.java           # Entry dashboard with system status & action cards
│           │   ├── DeviceListActivity.java     # Scan, discover, and pair with nearby P2P peers
│           │   ├── ChatActivity.java           # Main messaging screen (text, files, status, SQLite)
│           │   │
│           │   ├── wifi/                       # Hardware Wi-Fi Direct layer
│           │   │   ├── WiFiDirectManager.java  # Discovery, connection initiation, group handling
│           │   │   └── WiFiDirectBroadcastReceiver.java # Listens for system Wi-Fi Direct state intents
│           │   │
│           │   ├── network/                    # Core TCP socket engine
│           │   │   ├── P2PSocketManager.java   # Facade coordinating background server & client tasks
│           │   │   ├── ServerThread.java       # ServerSocket (port 8888) reading text, files, handshakes
│           │   │   └── ClientTask.java         # Async socket client streaming text & files with ACKs
│           │   │
│           │   ├── model/                      # Data models
│           │   │   ├── ChatMessage.java        # Message model (id, text, status, file path, size)
│           │   │   └── DeviceItem.java         # Peer device model (name, mac address, RSSI, type)
│           │   │
│           │   ├── db/                         # Persistence layer
│           │   │   └── ChatDatabaseHelper.java # SQLiteOpenHelper storing contacts and messages
│           │   │
│           │   └── adapter/                    # RecyclerView UI adapters
│           │       ├── ChatAdapter.java        # Heterogeneous bubbles (sent vs received, files/images)
│           │       └── DeviceAdapter.java      # Nearby peer device list item adapter
│           │
│           └── res/                            # Android UI resources
│               ├── layout/                     # XML layouts (activity_main, activity_chat, etc.)
│               ├── values/                     # Colors, strings, themes, styles
│               └── drawable/                   # Icons, vectors, bubble backgrounds
│
├── build.gradle                                # Root Gradle configuration
├── settings.gradle                             # Project module settings
├── gradlew & gradlew.bat                       # Gradle wrapper scripts
└── README.md                                   # Project documentation
```

---

## 🔧 What Was Built & Fixed (Engineering Log)

If you are reviewing recent commits, here are the critical fixes implemented:

1. **Fixed Group Owner Loopback / Self-Echo**:
   - *Issue*: In Android Wi-Fi Direct, `info.groupOwnerAddress` is `192.168.49.1`. The owner device was setting `peerIp` to its own IP, causing it to send messages to itself and instantly echo them back without sending to the client.
   - *Fix*: The Group Owner leaves `peerIp` unset until the client registers via handshake. Loopback transmission is blocked.

2. **Added Automated Client Handshake (`TYPE_HANDSHAKE = 0x03`)**:
   - *Issue*: Android does not inform the Group Owner of the Client's dynamic IP address (`192.168.49.xxx`).
   - *Fix*: When the client connects, it immediately sends a handshake packet to `192.168.49.1:8888`. The Group Owner extracts the client's actual socket address and dynamically binds `peerIp = clientIp`.

3. **Resolved File Transfer Failures & Out-Of-Memory Risks**:
   - *Issue*: Sockets were being closed immediately after `flush()`, sending a TCP Reset (`RST`) and killing the file reception. Furthermore, reading full files into `byte[]` risked RAM crashes on large files.
   - *Fix*: Files are now streamed directly from disk in 8KB chunks. After transmission, the sender calls `socket.shutdownOutput()` (clean TCP FIN) and waits for a `0x06` ACK verification from the receiver.

---

## 🚀 Junior Developer Quickstart (Run It in 5 Minutes)

### Prerequisites
- **JDK 17+** installed (`java -version`).
- **Android Studio** (Koala / Ladybug or newer recommended).
- **2 Physical Android Devices** (recommended) with Wi-Fi enabled. *(Emulators cannot simulate physical Wi-Fi Direct radio links between each other).*

### 1. Build the APK
Open your terminal in the project root:
```bash
# On Windows PowerShell
.\gradlew.bat assembleDebug

# On macOS / Linux
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Install on Both Devices via ADB
Connect both devices via USB (with Developer Mode & USB Debugging enabled):
```bash
# Check connected devices
adb devices

# Install APK to Device 1
adb -s <DEVICE_1_SERIAL> install app/build/outputs/apk/debug/app-debug.apk

# Install APK to Device 2
adb -s <DEVICE_2_SERIAL> install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Step-by-Step Testing Walkthrough
1. **Enable Wi-Fi and Location** on both phones (Wi-Fi does not need to be connected to any router; just have the Wi-Fi toggle **ON**).
2. Open **MeshConnect** on both phones.
3. Grant **Nearby Devices / Location** permissions when prompted.
4. Tap **"Find Nearby Nodes"** on Phone A. Phone B will appear in the peer list.
5. Tap on Phone B's name to initiate pairing.
6. Accept the Wi-Fi Direct connection prompt on Phone B.
7. The chat window will open automatically on both phones:
   - Phone A will show `"Connected (Wi-Fi Direct • Owner/Client)"`.
   - Phone B will show `"Connected (Wi-Fi Direct • Owner/Client)"`.
8. **Test Text Chat**: Type a message and tap Send. It should appear on the other phone in <10ms.
9. **Test File Sharing**: Tap the **attachment clip (+)**, pick an image or document, and send. The other phone will receive, save it to internal storage, and render it in the chat timeline.

---

## ⚠️ Troubleshooting & Gotchas

| Symptom | Cause | Solution |
|---|---|---|
| **Peers not discovering each other** | Wi-Fi toggle is OFF or Location service is disabled | Ensure Wi-Fi is toggled **ON** on both phones and Location / GPS is turned ON (required by Android OS for Wi-Fi Direct beaconing). |
| **"Missing permissions" warning** | Runtime permissions denied | On Android 13+, ensure `NEARBY_WIFI_DEVICES` permission is granted. On Android 12 and below, ensure `ACCESS_FINE_LOCATION` is granted. |
| **Connection dropped abruptly** | One device moved out of radio range (>100m) or Wi-Fi was toggled off | Tap the Back button, tap **Rescan**, and reconnect. The app detects disconnections and displays offline status. |
| **"Waiting for peer handshake..." toast** | Group Owner attempted to send before Client handshake arrived | The client automatically handshakes within 600ms of pairing. Ensure the client has connected successfully. |

---

## 📄 License
This project is open-source under the [MIT License](LICENSE). Contributions, bug reports, and pull requests are warmly welcomed!
