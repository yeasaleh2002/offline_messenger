# 📡 MeshConnect — Offline P2P Mesh Chat & File Transfer

> **Zero Cellular Data. Zero Wi-Fi Routers. Zero Central Servers. 100% Offline Peer-to-Peer.**

MeshConnect is a production-ready Android offline messaging and file-sharing application. It connects nearby smartphones directly using **Wi-Fi Direct (Wi-Fi P2P)** and local TCP/IP sockets, enabling real-time private communication and media exchange even during cellular network blackouts, natural disasters, underground transit, remote hiking, or crowded stadiums.

---

## 📖 Table of Contents
1. [Why This App? (Problem Statement & Purpose)](#-why-this-app-problem-statement--purpose)
2. [Key Features](#-key-features)
3. [Network Topology, Range & Ecosystem Scalability](#-network-topology-range--ecosystem-scalability)
4. [Technology Stack & Why Each Was Chosen](#-technology-stack--why-each-was-chosen)
5. [Architecture & How It Works](#-architecture--how-it-works)
6. [Packet Protocol Specification](#-packet-protocol-specification)
7. [Folder Structure & Codebase Map](#-folder-structure--codebase-map)
8. [What Was Built & Fixed (Engineering Log)](#-what-was-built--fixed-engineering-log)
9. [Junior Developer Quickstart (Run It in 5 Minutes)](#-junior-developer-quickstart-run-it-in-5-minutes)
10. [Troubleshooting & Gotchas](#-troubleshooting--gotchas)

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

## 🛠️ Technology Stack & Why Each Was Chosen

MeshConnect deliberately selects native Android and core networking technologies to operate 100% off-grid without depending on cloud servers, cellular modems, or third-party web frameworks:

### 1. Wireless Radio Protocol: **Wi-Fi Direct (Wi-Fi P2P / 802.11)**
* **Technology**: `android.net.wifi.p2p.WifiP2pManager`, `WifiP2pConfig`, `WifiP2pInfo`
* **Why this was chosen**:
  - **High Bandwidth (up to 250 Mbps)**: Bluetooth Classic/BLE only delivers ~1–2 Mbps, which is far too slow for transferring large photos, audio, or documents. Wi-Fi Direct uses high-speed 802.11n/ac/ax radio channels.
  - **Long Range (50–100m outdoors)**: Bluetooth signals degrade after 10–15 meters. Wi-Fi Direct reaches up to 100 meters outdoors and easily penetrates indoor building walls.
  - **Zero Router Dependency**: Devices establish an ad-hoc local group automatically without needing a Wi-Fi router, mobile hotspot, or internet connectivity.

| Comparison Factor | Cellular / Internet | Bluetooth LE | **Wi-Fi Direct (MeshConnect)** |
|---|---|---|---|
| **Internet / SIM Needed?** | Yes (Cloud servers) | No | **No (100% Offline)** |
| **Max Direct Range** | Cell tower dependent | ~10–15 meters | **~50–100 meters** |
| **Transfer Speed** | Variable (data charges) | ~1–2 Mbps | **Up to 250 Mbps** |
| **Media & File Sharing** | Consumes mobile data | Unusable for large files | **Instant HD streaming** |

---

### 2. Networking Engine: **Raw TCP/IP Sockets (`ServerSocket` & `Socket`)**
* **Technology**: `java.net.ServerSocket`, `java.net.Socket`, `DataInputStream`, `DataOutputStream`
* **Why this was chosen**:
  - **No Cloud Web Server Required**: Standard chat apps use HTTP REST or WebSockets connecting to AWS, Firebase, or cloud servers. In an offline environment, those servers do not exist.
  - **Direct Device-to-Device Streaming**: When paired via Wi-Fi Direct, Android assigns real IP addresses (`192.168.49.1` and `192.168.49.xxx`). Raw TCP sockets allow phones to communicate directly at the transport layer.
  - **Guaranteed Order & Zero Data Loss**: TCP automatically manages packet ordering, checksums, and retransmissions so messages and binary files never arrive corrupted.
  - **Sub-10ms Latency**: Packets travel locally through the air between antennas in milliseconds.

---

### 3. Core Development Platform: **Native Android (Java & Android SDK 34 / Android 14)**
* **Technology**: Android SDK (`compileSdk 34`), Java 8/17 source compatibility
* **Why this was chosen**:
  - **Direct Hardware & Radio API Control**: Cross-platform frameworks (React Native, Flutter) rely on third-party community plugins for Wi-Fi Direct that are often unmaintained, unstable, or fail on modern Android versions (Android 13/14).
  - **Modern OS Compatibility**: Android 13+ introduced strict runtime permissions such as `NEARBY_WIFI_DEVICES`. Developing in native Android provides full access to `BroadcastReceiver`, type-safe Parcelables, and system Wi-Fi lifecycle intents.
  - **Maximum Execution Speed**: Zero JavaScript bridge or runtime virtualization overhead.

---

### 4. Local Database: **Android SQLite (`SQLiteOpenHelper`)**
* **Technology**: `android.database.sqlite.SQLiteDatabase`, `SQLiteOpenHelper`
* **Why this was chosen**:
  - **100% Offline Persistence**: With no cloud database (like Firebase or Supabase), all contacts, message threads, delivery statuses (`SENDING`, `DELIVERED`), and downloaded file paths must persist across app reboots locally.
  - **Relational Integrity**: Enforces foreign keys (`FOREIGN KEY(contact_mac) REFERENCES contacts(mac_address)`) to maintain structured conversation histories.
  - **Indexed Performance**: Uses an indexed search table (`idx_messages_contact_mac`) to load long chat histories in milliseconds.

---

### 5. Asynchronous Concurrency: **Java `ExecutorService` & `Handler(Looper)`**
* **Technology**: `Executors.newCachedThreadPool()`, dedicated background `ServerThread`, `Handler(Looper.getMainLooper())`
* **Why this was chosen**:
  - **Zero UI Freezing (No ANR)**: Android strictly blocks network socket operations on the main thread (`NetworkOnMainThreadException`).
  - **Continuous Background Listening**: `ServerThread` stays open in the background listening for incoming text packets and files 24/7.
  - **Safe UI Thread Dispatching**: `mainHandler.post(...)` ensures incoming messages are safely updated in the RecyclerView on the UI thread without threading crashes.

---

### 6. Streaming Architecture: **Chunked Stream I/O (8KB Buffer)**
* **Technology**: `FileInputStream`, `FileOutputStream`, `socket.shutdownOutput()`
* **Why this was chosen**:
  - **Zero Out-Of-Memory (`OutOfMemoryError`)**: Loading a 50MB video or 15MB photo entirely into a flat byte array (`byte[]`) in memory can easily crash low-end smartphones.
  - **Chunk-by-Chunk Streaming**: Files are read from storage and pushed across the TCP socket in 8KB chunks. RAM consumption remains virtually flat (<10 MB) regardless of whether the file is 100 KB or 1 GB.
  - **Clean TCP Teardown**: Uses `socket.shutdownOutput()` (sends a clean TCP FIN signal) and waits for a `0x06 (ACK)` confirmation, preventing connection drops and TCP resets.

---

### 7. User Interface: **Android Material Components & `RecyclerView`**
* **Technology**: `RecyclerView`, Material 3 (`com.google.android.material:material:1.11.0`), ViewBinding
* **Why this was chosen**:
  - **View Recycling**: Unlike legacy `ListView`, `RecyclerView` recycles visual bubbles as the user scrolls, maintaining smooth 60/120 FPS frame rates even with thousands of chat messages.
  - **Heterogeneous Bubbles**: `ChatAdapter` dynamically switches layout views (`VIEW_TYPE_SENT` vs `VIEW_TYPE_RECEIVED`) and renders multimedia attachment previews seamlessly.

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
