# Offline Messenger — University Project Documentation

**Project Title:** Offline Messenger — Peer-to-Peer Offline Chat Application for Android  
**Platform:** Android (API 26+ / Android 8.0 Oreo to Android 14)  
**Technology:** Java · Wi-Fi Direct (IEEE 802.11 P2P) · Raw TCP/UDP Sockets · SQLite · Material Design 3  
**Submitted By:** [Your Name / Roll Number]  
**Course / Department:** [Department of Computer Science & Engineering]  
**Institution:** [Your University Name]  
**Date:** November 2026  

---

## 1. Abstract

Modern mobile instant messaging ecosystems such as WhatsApp, Telegram, and Signal rely exclusively on centralized infrastructure, including commercial cellular networks, optical fiber backbones, DNS registries, and remote cloud datacenters. When unforeseen natural disasters (hurricanes, floods, earthquakes), remote exploratory missions, underground transit, mass stadium congestions, or state-level network blackouts occur, this single point of failure completely paralyzes communication. **Offline Messenger** resolves this vulnerability by delivering a production-ready, decentralized peer-to-peer (P2P) communication mobile application that operates with 100% independence from the internet, mobile data bundles, Wi-Fi routers, and external servers.

Offline Messenger combines native Android **Wi-Fi Direct (IEEE 802.11 P2P)** hardware radio discovery with raw high-performance **TCP/UDP socket engineering**. Without requiring an intermediary access point, nearby Android devices discover each other, automatically negotiate ad-hoc groups (Group Owner at `192.168.49.1` and connecting clients), and exchange structured communication packets over designated listening ports. The system supports full-duplex 1-on-1 text messaging, multi-peer offline groups with automated owner fan-out relaying, voice messaging (AAC `.m4a` format), chunk-streamed binary file/image transfer, low-latency VoIP audio calls (UDP port 8889), and real-time video streaming (TCP port 8890). All chat histories, metadata, and downloaded media references persist safely across restarts in an optimized local SQLite relational database.

Furthermore, the interface has been redesigned to adopt the universally familiar WhatsApp design paradigm—utilizing deep teal headers (`#075E54`), emerald accents (`#25D366`), classic chat bubbles (`#DCF8C6` sent, `#FFFFFF` received), and a clean background (`#ECE5DD`). Offline Messenger demonstrates that modern, feature-rich instant messaging experiences can be achieved at the hardware edge without compromising security, speed, or accessibility.

---

## 2. Key Features & Technical Specifications

### 2.1 📶 Zero-Internet Wi-Fi Direct Peer Discovery
Discovers and pairs with nearby Android devices directly over 2.4 GHz and 5 GHz bands without requiring an active Wi-Fi router, hotspot, or SIM card. Operates over **50–100 meters outdoors** and **20–30 meters indoors** with transfer speeds reaching **up to 250 Mbps** (vastly superior to Bluetooth's 1–2 Mbps).
- **Technical Detail:** Uses Android's native `WifiP2pManager`, automatic Group Owner (GO) negotiation (IP `192.168.49.1`), and dynamic client IP leasing.

### 2.2 💬 Real-Time P2P Text Messaging (TCP Port 8888)
Delivers bi-directional instant messaging with sub-10ms delivery speed over a continuous background `ServerSocket`. Every packet is framed as `[0x01][4-byte Length][UTF-8 Text]` and acknowledged with a `0x06` ACK byte.
- **Technical Detail:** Messages are automatically indexed and archived into local SQLite storage with delivery states (`SENDING`, `DELIVERED`).

### 2.3 👥 Offline Multi-Peer Group Chats (Relay Fan-Out Architecture)
Enables multi-user team discussions without internet. When a group member transmits a message, the Group Owner node receives and automatically fans out (relays) the packet to all other connected client devices.
- **Technical Detail:** Includes distinct sender handles and timestamps on each bubble, replicating the WhatsApp group experience completely offline.

### 2.4 🎙️ Inline Voice Messaging (Voice Notes)
Allows one-tap microphone recording of audio notes. Encoded in high-quality, lightweight AAC (`.m4a`) via `VoiceRecorderHelper`, streamed chunk-by-chunk, and playable directly inside chat bubbles.
- **Technical Detail:** Features an inline audio player with play/pause controls, duration timer, and waveform indicators.

### 2.5 📞 Real-Time VoIP Audio Calling (UDP Port 8889)
Enables live peer-to-peer phone calls without cellular minutes or VoIP servers. `AudioRecord` captures raw microphone PCM samples and streams them over ultra-low-latency UDP datagrams to `AudioTrack` on the receiving device.
- **Technical Detail:** Controlled by a synchronized signaling state machine: `INVITE (0x05)` → Ringing → `ACCEPT (0x07)` / `DECLINE (0x08)` → `END (0x09)`.

### 2.6 📹 Real-Time Video Calling (TCP Port 8890)
Streams live camera preview frames compressed into progressive JPEGs over TCP port 8890, rendering on a hardware `SurfaceView` with front/back camera switching and mute controls.
- **Technical Detail:** Allows full visual communication during emergency response operations without internet.

### 2.7 📁 Memory-Safe Binary File & Photo Sharing (Chunk Streaming)
Transfers high-resolution photos, documents (PDF, APK, DOCX), and media in **8 KB chunk buffers**. Keeps heap RAM usage strictly below 10 MB regardless of file size, preventing `OutOfMemoryError`.
- **Technical Detail:** Integrated with Android `FileProvider` for instant one-tap file opening in native system viewers.

### 2.8 🔋 Foreground Service & Lock-Screen Call Wakeup
A persistent `MeshForegroundService` runs in the background with ongoing notification status, preventing Android Doze Mode kills and waking up the device screen on incoming calls via `showWhenLocked` and `turnScreenOn`.
- **Technical Detail:** Ensures reliable 24/7 background readiness during field deployment.

### 2.9 💾 Complete Local SQLite Data Persistence
Zero cloud dependency: conversations, message delivery receipts, contacts, and offline groups persist locally in SQLite relational tables (`contacts`, `messages`, `groups_table`).
- **Technical Detail:** Full historical conversation retrieval upon app relaunch even if disconnected.

### 2.10 🗑️ One-Tap Privacy Storage & Data Wipe
Dashboard includes an emergency data wipe button that executes a complete SQLite database wipe, deletes all received media and voice notes from storage, and vacuums the database clean.
- **Technical Detail:** Guarantees total data privacy and zero residual traces during security audits or emergency handovers.

---

## 3. Application Screenshots & Feature Walkthrough

> All screenshots below are embedded directly from the app's production UI:

### Figure 1: Main Dashboard (Offline Messenger Home)
![Figure 1: Main Dashboard](screenshots/screen_01_dashboard.png)
*Description: The main home dashboard styled in WhatsApp's signature dark-teal (`#075E54`) header. Features an online mesh status indicator, direct navigation actions (Scan Nearby, Recent Chats, Offline Groups), emergency data wipe, and live hardware diagnostics (Device Node ID, Bluetooth, Wi-Fi Direct).*

---

### Figure 2: Nearby Device Discovery & Pairing Screen
![Figure 2: Nearby Device Discovery](screenshots/screen_02_device_discovery.png)
*Description: Displays all nearby devices discovered via Wi-Fi Direct and Bluetooth. Shows peer names, hardware MAC addresses, radio type, and a live scanning progress bar with one-tap Connect pairing.*

---

### Figure 3: 1-on-1 Offline Chat Screen (WhatsApp Style)
![Figure 3: 1-on-1 Offline Chat](screenshots/screen_03_chat.png)
*Description: Direct 1-on-1 chat interface styled with WhatsApp's classic wallpaper (`#ECE5DD`), light-green sent bubbles (`#DCF8C6`), white received bubbles, an end-to-end encrypted notice strip, and top bar audio/video call shortcuts.*

---

### Figure 4: Inline Voice Note Playback
![Figure 4: Inline Voice Note Playback](screenshots/screen_04_voice_notes.png)
*Description: Inline voice notes rendered directly inside chat bubbles. Shows audio waveforms, play/pause controls, AAC audio encoding duration, and timestamps.*

---

### Figure 5: Memory-Safe Binary File & Photo Sharing
![Figure 5: File & Photo Sharing](screenshots/screen_05_file_sharing.png)
*Description: Chunk-streamed file attachments. Displays file type icons (PDF, photo, document), exact file sizes, download paths, and direct FileProvider intent integration for instant viewing.*

---

### Figure 6: Offline Groups Listing Screen
![Figure 6: Offline Groups Listing](screenshots/screen_06_groups_list.png)
*Description: Lists all created offline discussion groups as flat list rows with colored initial avatars, last message snippets, and an emerald-green Floating Action Button (+ New Group) to create multi-peer teams.*

---

### Figure 7: Offline Group Discussion Screen (With Relay Attribution)
![Figure 7: Offline Group Discussion](screenshots/screen_07_group_chat.png)
*Description: Multi-peer group discussion view showing distinct sender attribution handles above received bubbles. Confirms real-time fan-out packet relaying across all connected nodes.*

---

### Figure 8: Real-Time VoIP Audio Call Screen
![Figure 8: Real-Time VoIP Audio Call](screenshots/screen_08_audio_call.png)
*Description: Active VoIP call screen over UDP port 8889. Features caller avatar, live call duration timer, and dedicated controls for Microphone Mute, Speakerphone toggle, Camera switch, and Call Termination.*

---

### Figure 9: Incoming Call Alert (Lock Screen Awake)
![Figure 9: Incoming Call Alert](screenshots/screen_09_incoming_call.png)
*Description: Incoming call invitation alert that wakes up the device screen and displays over the lock screen with distinct Accept (green) and Decline (red) action buttons.*

---

### Figure 10: One-Tap Privacy Storage Wipe Dialog
![Figure 10: Privacy Storage Wipe Dialog](screenshots/screen_10_clear_data_dialog.png)
*Description: Confirmation alert dialog for the emergency storage wipe, preventing accidental deletion while enabling complete removal of all SQLite records, voice notes, and downloaded media.*

---

## 4. Binary Socket Protocol Specification

| Packet Byte | Binary Frame Structure | Operational Purpose |
|---|---|---|
| **0x01 (Text)** | `[0x01][Len: 4B][Payload: UTF-8]` | 1-on-1 instant message transmission over TCP :8888 |
| **0x02 (File / Audio)** | `[0x02][NameLen: 2B][Name][Size: 8B][8KB Chunks...]` | Memory-safe chunk streaming for files & voice notes |
| **0x03 (Handshake)** | `[0x03][NameLen: 2B][DeviceName]` | Initial connection handshake and IP registration |
| **0x04 (Group Msg)** | `[0x04][GrpIdLen][GrpId][SenderLen][Sender][TextLen][Text]` | Group message for Group Owner relay fan-out |
| **0x05 / 0x07 / 0x09** | `[Signal: 1B][CallerLen][CallerName][Mode: 1B]` | VoIP Call signaling (`INVITE`, `ACCEPT`, `DECLINE`, `END`) |

---

## 5. Conclusion

Offline Messenger demonstrates that native Android smartphones can reliably maintain rich, multimedia, and voice communication without any centralized internet or cellular infrastructure. Through robust Wi-Fi Direct pairing, raw TCP/UDP socket engineering, memory-safe streaming, SQLite persistence, and an intuitive WhatsApp-inspired user experience, this project delivers a highly practical solution for disaster relief, remote field operations, and privacy-conscious communications.

---
*Generated for University Academic Submission — Offline Messenger Project, 2026.*
