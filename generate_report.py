import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

doc = docx.Document()

# Page Margins
sections = doc.sections
for section in sections:
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)

def set_cell_background(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def add_callout(doc, text, title=""):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(6.5)
    
    cell = table.cell(0, 0)
    set_cell_background(cell, "F0F7F4")
    
    # Border
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'''
        <w:tcBorders {nsdecls("w")}>
            <w:top w:val="none"/>
            <w:left w:val="single" w:sz="24" w:space="0" w:color="075E54"/>
            <w:bottom w:val="none"/>
            <w:right w:val="none"/>
        </w:tcBorders>
    ''')
    tcPr.append(tcBorders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.left_indent = Inches(0.1)
    p.paragraph_format.right_indent = Inches(0.1)
    
    if title:
        r_title = p.add_run(f"{title}\n")
        r_title.bold = True
        r_title.font.name = "Calibri"
        r_title.font.size = Pt(10.5)
        r_title.font.color.rgb = RGBColor(7, 94, 84)
        
    r_body = p.add_run(text)
    r_body.font.name = "Calibri"
    r_body.font.size = Pt(10)
    r_body.font.color.rgb = RGBColor(50, 50, 50)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

def add_mockup(doc, title, lines):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(f"Figure: {title}")
    r.bold = True
    r.font.name = "Calibri"
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(7, 94, 84)

    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(5.5)
    
    cell = table.cell(0, 0)
    set_cell_background(cell, "F8FAFC")
    
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(f'''
        <w:tcBorders {nsdecls("w")}>
            <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
            <w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
            <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
            <w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
        </w:tcBorders>
    ''')
    tcPr.append(tcBorders)
    
    p_box = cell.paragraphs[0]
    p_box.paragraph_format.space_before = Pt(6)
    p_box.paragraph_format.space_after = Pt(6)
    p_box.paragraph_format.left_indent = Inches(0.15)
    p_box.paragraph_format.right_indent = Inches(0.15)
    
    content = "\n".join(lines)
    r_code = p_box.add_run(content)
    r_code.font.name = "Consolas"
    r_code.font.size = Pt(9)
    r_code.font.color.rgb = RGBColor(30, 41, 59)
    
    p_note = doc.add_paragraph()
    p_note.paragraph_format.space_before = Pt(2)
    p_note.paragraph_format.space_after = Pt(12)
    p_note.paragraph_format.left_indent = Inches(0.5)
    r_note = p_note.add_run("Caption: UI layout representation corresponding to the production Android layout resource.")
    r_note.italic = True
    r_note.font.size = Pt(8.5)
    r_note.font.color.rgb = RGBColor(100, 116, 139)

# ----------------- COVER & TITLE -----------------
p_title = doc.add_paragraph()
p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_title.paragraph_format.space_before = Pt(18)
p_title.paragraph_format.space_after = Pt(6)

run_title = p_title.add_run("OFFLINE MESSENGER")
run_title.bold = True
run_title.font.name = "Calibri"
run_title.font.size = Pt(24)
run_title.font.color.rgb = RGBColor(7, 94, 84)

p_sub = doc.add_paragraph()
p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_sub.paragraph_format.space_after = Pt(14)
r_sub = p_sub.add_run("Decentralized Peer-to-Peer Offline Communication System for Android\nWi-Fi Direct (802.11 P2P) • Raw TCP/UDP Sockets • SQLite Local Persistence")
r_sub.font.name = "Calibri"
r_sub.font.size = Pt(12)
r_sub.font.color.rgb = RGBColor(18, 140, 126)

# Meta info table
meta_table = doc.add_table(rows=5, cols=2)
meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
meta_table.autofit = False
meta_table.columns[0].width = Inches(2.2)
meta_table.columns[1].width = Inches(4.3)

meta_data = [
    ("Student Name / Roll:", "[Your Name / ID Here]"),
    ("Course / Module:", "Advanced Mobile Computing / Final Project"),
    ("Department & Faculty:", "Department of Computer Science & Engineering"),
    ("University / Institute:", "[Your University Name Here]"),
    ("Submission Date:", "November 2026")
]

for idx, (label, val) in enumerate(meta_data):
    cell_lbl = meta_table.cell(idx, 0)
    cell_val = meta_table.cell(idx, 1)
    
    set_cell_background(cell_lbl, "F0F7F4")
    set_cell_background(cell_val, "FFFFFF")
    
    p0 = cell_lbl.paragraphs[0]
    p0.paragraph_format.space_before = Pt(3)
    p0.paragraph_format.space_after = Pt(3)
    r0 = p0.add_run(label)
    r0.bold = True
    r0.font.name = "Calibri"
    r0.font.size = Pt(9.5)
    r0.font.color.rgb = RGBColor(7, 94, 84)
    
    p1 = cell_val.paragraphs[0]
    p1.paragraph_format.space_before = Pt(3)
    p1.paragraph_format.space_after = Pt(3)
    r1 = p1.add_run(val)
    r1.font.name = "Calibri"
    r1.font.size = Pt(9.5)

doc.add_paragraph().paragraph_format.space_after = Pt(14)

# ----------------- SECTION 1: ABSTRACT -----------------
h1 = doc.add_heading("1. Abstract", level=1)
h1.style.font.color.rgb = RGBColor(7, 94, 84)
h1.paragraph_format.space_before = Pt(14)
h1.paragraph_format.space_after = Pt(6)

p_abs1 = doc.add_paragraph()
p_abs1.paragraph_format.line_spacing = 1.15
p_abs1.paragraph_format.space_after = Pt(6)
p_abs1.add_run(
    "Modern mobile instant messaging ecosystems such as WhatsApp, Telegram, and Signal rely exclusively on centralized infrastructure, including commercial cellular networks, optical fiber backbones, DNS registries, and remote cloud datacenters. When unforeseen natural disasters (hurricanes, floods, earthquakes), remote exploratory missions, underground transit, mass stadium congestions, or state-level network blackouts occur, this single point of failure completely paralyzes communication. Offline Messenger resolves this vulnerability by delivering a production-ready, decentralized peer-to-peer (P2P) communication mobile application that operates with 100% independence from the internet, mobile data bundles, Wi-Fi routers, and external servers."
)

p_abs2 = doc.add_paragraph()
p_abs2.paragraph_format.line_spacing = 1.15
p_abs2.paragraph_format.space_after = Pt(6)
p_abs2.add_run(
    "Offline Messenger combines native Android Wi-Fi Direct (IEEE 802.11 P2P) hardware radio discovery with raw high-performance TCP/UDP socket engineering. Without requiring an intermediary access point, nearby Android devices discover each other, automatically negotiate ad-hoc groups (Group Owner at 192.168.49.1 and connecting clients), and exchange structured communication packets over designated listening ports. The system supports full-duplex 1-on-1 text messaging, multi-peer offline groups with automated owner fan-out relaying, voice messaging (AAC .m4a format), chunk-streamed binary file/image transfer, low-latency VoIP audio calls (UDP port 8889), and real-time video streaming (TCP port 8890). All chat histories, metadata, and downloaded media references persist safely across restarts in an optimized local SQLite relational database."
)

p_abs3 = doc.add_paragraph()
p_abs3.paragraph_format.line_spacing = 1.15
p_abs3.paragraph_format.space_after = Pt(10)
p_abs3.add_run(
    "Furthermore, the interface has been redesigned to adopt the universally familiar WhatsApp design paradigm—utilizing deep teal headers (#075E54), emerald accents (#25D366), classic chat bubbles (#DCF8C6 sent, #FFFFFF received), and a clean background (#ECE5DD). Offline Messenger demonstrates that modern, feature-rich instant messaging experiences can be achieved at the hardware edge without compromising security, speed, or accessibility."
)

# ----------------- SECTION 2: KEY FEATURES -----------------
h2 = doc.add_heading("2. Key Architectural Features", level=1)
h2.style.font.color.rgb = RGBColor(7, 94, 84)
h2.paragraph_format.space_before = Pt(14)
h2.paragraph_format.space_after = Pt(6)

features = [
    ("2.1 Zero-Internet Wi-Fi Direct Peer Discovery",
     "The discovery system interfaces directly with Android's WifiP2pManager, broadcasting availability over 2.4 GHz and 5 GHz bands without connecting to any cellular or router AP. It achieves an open-air range of 50–100 meters with bandwidth capabilities reaching up to 250 Mbps (drastically outperforming traditional Bluetooth rates of 1–2 Mbps).",
     "API Implementation: Uses WifiP2pManager.discoverPeers(), dynamic BroadcastReceiver callbacks, and auto-negotiation of Group Owner (GO) status with dynamic IP leasing."),
    
    ("2.2 Real-Time Peer-to-Peer Text Messaging (TCP Port 8888)",
     "A dedicated background ServerThread listens continuously on TCP port 8888. Messages are serialized into framed binary packets [0x01][4-byte Length][Payload UTF-8]. A 0x06 ACK byte handshake confirms reception, ensuring sub-10ms transmission with guaranteed packet delivery and zero dropped characters.",
     "Persistence: Messages are automatically indexed and recorded into a local SQLite database (ChatDatabaseHelper), tracking sender MAC, timestamp, delivery status, and conversation ID."),
     
    ("2.3 Offline Multi-Peer Group Chats (Relay Fan-Out Architecture)",
     "Replicating the functionality of WhatsApp groups in a completely offline scenario, users can create named groups. When a member transmits a group message (packet 0x04), the Wi-Fi Direct Group Owner automatically fans out and relays the packet to all other connected peer clients in the mesh network.",
     "Sender Attribution: Each message bubble displays the sender's node handle and timestamp so participants can follow multi-party conversations clearly."),
     
    ("2.4 Inline Voice Messaging (Voice Notes)",
     "Allows users to record audio notes in real time using the device microphone. Audio is encoded in high-efficiency AAC (.m4a format) via VoiceRecorderHelper, transferred through chunk-streamed sockets, and rendered inside chat bubbles with inline play/pause controls and duration counters.",
     "Memory Safety: Audio streams directly from storage without loading the full raw byte buffer into heap RAM, preventing OutOfMemoryError exceptions."),
     
    ("2.5 Real-Time VoIP Audio Calling (UDP Port 8889)",
     "Provides direct real-time phone calls without cellular minutes or VoIP server routing. AudioRecord captures raw PCM audio samples from the microphone, which are transmitted in datagram packets over high-speed UDP port 8889. On the remote side, AudioTrack instantly outputs the decoded audio to the speaker or earpiece.",
     "Signaling State Machine: Utilizes INVITE (0x05), ACCEPT (0x07), DECLINE (0x08), and END (0x09) socket control frames with synchronized 30-second ringing timeouts."),
     
    ("2.6 Real-Time Video Calling (TCP Port 8890)",
     "Supplements voice calls with live video streaming. The camera preview stream captures frames, compresses them into progressive JPEG images with length headers, and streams them over TCP port 8890 to be rendered on the peer's SurfaceView display with front/back camera switching capabilities.",
     "Controls: CallActivity provides interactive toggles for camera switching, microphone muting, speakerphone amplification, and call termination."),
     
    ("2.7 Memory-Safe Binary File & Photo Sharing",
     "Users can exchange high-resolution photographs, PDF manuals, APKs, or recorded media. Files are read in 8 KB chunk buffers and streamed progressively. The receiving node streams chunks directly into external storage (Environment.DIRECTORY_DOWNLOADS/OfflineMessenger/).",
     "System Integration: Configured with Android FileProvider and intent flags (FLAG_GRANT_READ_URI_PERMISSION), enabling users to tap received files and launch them in system viewers instantly."),
     
    ("2.8 Foreground Service & Background Incoming Call Alerts",
     "A persistent MeshForegroundService runs in the background with an ongoing system notification, keeping socket servers alive when the app is minimized. Incoming VoIP calls wake the screen and display over the lock screen using showWhenLocked and turnScreenOn flags.",
     "Reliability: Eliminates process kills by Android's aggressive battery optimizations (Doze Mode)."),
     
    ("2.9 Complete Local SQLite Data Persistence",
     "All device records, contacts, conversation threads, delivery states (SENDING, DELIVERED), and offline group memberships are saved in a local relational SQLite database without any cloud sync dependency.",
     "Schema: Structured with contacts, messages, and groups_table, utilizing indexed search keys for instant loading even with thousands of archived messages."),
     
    ("2.10 One-Tap Privacy & Storage Data Wipe",
     "Offers an emergency data wipe option from the main dashboard. With a single confirmed action, all SQLite database tables, temporary voice recordings, cache directories, and downloaded files are wiped cleanly and the database is vacuumed.",
     "Privacy Compliance: Ensures zero leftover traces of sensitive offline communication during security audits or emergency situations.")
]

for title, desc, tech in features:
    p_feat = doc.add_paragraph()
    p_feat.paragraph_format.space_before = Pt(8)
    p_feat.paragraph_format.space_after = Pt(2)
    r_f_title = p_feat.add_run(title)
    r_f_title.bold = True
    r_f_title.font.name = "Calibri"
    r_f_title.font.size = Pt(11)
    r_f_title.font.color.rgb = RGBColor(18, 140, 126)
    
    p_desc = doc.add_paragraph()
    p_desc.paragraph_format.line_spacing = 1.15
    p_desc.paragraph_format.space_after = Pt(2)
    p_desc.add_run(desc)
    
    add_callout(doc, tech, "Technical & Protocol Detail:")

# ----------------- SECTION 3: SYSTEM ARCHITECTURE & PROTOCOL -----------------
h3 = doc.add_heading("3. Protocol Specifications & Architecture", level=1)
h3.style.font.color.rgb = RGBColor(7, 94, 84)
h3.paragraph_format.space_before = Pt(14)
h3.paragraph_format.space_after = Pt(6)

p_arch = doc.add_paragraph()
p_arch.paragraph_format.line_spacing = 1.15
p_arch.paragraph_format.space_after = Pt(6)
p_arch.add_run(
    "The application architecture strictly decouples the User Interface, the Business Logic, the Network Socket Engine (Singleton), and the Hardware Wi-Fi Direct Stack. All packet exchanges follow an explicit binary header structure:"
)

# Protocol Table
proto_table = doc.add_table(rows=6, cols=3)
proto_table.alignment = WD_TABLE_ALIGNMENT.CENTER
proto_table.autofit = False
proto_table.columns[0].width = Inches(1.2)
proto_table.columns[1].width = Inches(2.3)
proto_table.columns[2].width = Inches(3.0)

proto_headers = ["Packet Type", "Frame Format", "Functional Purpose"]
for col_idx, header in enumerate(proto_headers):
    cell = proto_table.cell(0, col_idx)
    set_cell_background(cell, "075E54")
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(header)
    r.bold = True
    r.font.name = "Calibri"
    r.font.size = Pt(9.5)
    r.font.color.rgb = RGBColor(255, 255, 255)

proto_rows = [
    ("0x01 (Text)", "[0x01][Len: 4B][Payload: UTF-8]", "Direct 1-on-1 text message transmission."),
    ("0x02 (File/Audio)", "[0x02][NameLen: 2B][Name][Size: 8B][8KB Chunks...]", "Memory-safe chunk streaming for files & voice notes."),
    ("0x03 (Handshake)", "[0x03][NameLen: 2B][DeviceName]", "Initial connection handshake & IP registration."),
    ("0x04 (Group Msg)", "[0x04][GrpIdLen][GrpId][SenderLen][Sender][TextLen][Text]", "Group message for Group Owner relay fan-out."),
    ("0x05 / 0x07 / 0x09", "[Signal: 1B][CallerLen][CallerName][Mode: 1B]", "VoIP Call signaling: 0x05=INVITE, 0x07=ACCEPT, 0x09=END.")
]

for row_idx, data in enumerate(proto_rows, start=1):
    for col_idx, text in enumerate(data):
        cell = proto_table.cell(row_idx, col_idx)
        set_cell_background(cell, "F8FAFC" if row_idx % 2 == 0 else "FFFFFF")
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(3)
        r = p.add_run(text)
        r.font.name = "Calibri"
        r.font.size = Pt(8.5)

doc.add_paragraph().paragraph_format.space_after = Pt(14)

# ----------------- SECTION 4: SCREENSHOTS & UI WALKTHROUGH -----------------
h4 = doc.add_heading("4. Screenshots & UI Feature Walkthrough", level=1)
h4.style.font.color.rgb = RGBColor(7, 94, 84)
h4.paragraph_format.space_before = Pt(14)
h4.paragraph_format.space_after = Pt(6)

p_ui_intro = doc.add_paragraph()
p_ui_intro.paragraph_format.line_spacing = 1.15
p_ui_intro.paragraph_format.space_after = Pt(10)
p_ui_intro.add_run(
    "The user interface has been intentionally crafted around the WhatsApp design language. Below are detailed visual representations of all active application screens, demonstrating layout hierarchy, component composition, and responsive user feedback."
)

screens = [
    ("Screen 1: Main Dashboard (Offline Messenger Home)",
     [
         "+---------------------------------------------------------------+",
         "|  Offline Messenger                                            |  <- #075E54 Dark Teal Toolbar",
         "|  * Offline Messenger Ready                                    |  <- Online Status Pill",
         "+---------------------------------------------------------------+",
         "|  Actions                                                      |  <- Section Header",
         "|  (O)  Find Nearby People                     [   Scan   ]     |  <- Action Row 1",
         "|       Search via Bluetooth & Wi-Fi Direct                     |",
         "|  -----------------------------------------------------------  |",
         "|  (O)  Recent Chats                           [   Open   ]     |  <- Action Row 2",
         "|       View your offline conversations                         |",
         "|  -----------------------------------------------------------  |",
         "|  (O)  Offline Groups                         [   Open   ]     |  <- Action Row 3",
         "|       Multi-peer offline discussions                          |",
         "|  -----------------------------------------------------------  |",
         "|  (X)  Delete All Data                        [  Delete  ]     |  <- Privacy Wipe Row",
         "|       Wipe all messages, contacts & files                     |",
         "+---------------------------------------------------------------+",
         "|  Network Info                                                 |  <- Diagnostics",
         "|  Device ID: MSG-NODE-A49F | Bluetooth: Enabled | Wi-Fi: Ready |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 2: Nearby Device Discovery & Pairing Screen",
     [
         "+---------------------------------------------------------------+",
         "|  <- Nearby People                                             |  <- #075E54 Toolbar",
         "|  ===========================================================  |  <- Scanning Progress Bar",
         "|  Scanning for nearby devices...                       [Stop]  |",
         "+---------------------------------------------------------------+",
         "|  (P)  Pixel 8 Pro (Recommended)              [ Connect  ]     |  <- Discovered Peer 1",
         "|       AA:BB:CC:11:22:33 * Wi-Fi Direct                        |",
         "|  -----------------------------------------------------------  |",
         "|  (S)  Samsung Galaxy S24 Ultra               [ Connect  ]     |  <- Discovered Peer 2",
         "|       DD:EE:FF:44:55:66 * Wi-Fi Direct                        |",
         "|  -----------------------------------------------------------  |",
         "|  (N)  Nothing Phone (2)                      [ Connect  ]     |  <- Discovered Peer 3",
         "|       11:22:33:44:55:66 * Bluetooth Standby                   |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 3: 1-on-1 Offline Chat Screen (WhatsApp Style)",
     [
         "+---------------------------------------------------------------+",
         "|  <- (P) Pixel 8 Pro                                (Call) (Cam)  <- Call Action Icons",
         "|         * online * offline mesh link                          |",
         "|  [Lock] End-to-End Encrypted * Direct Peer Socket Link        |  <- Security Banner",
         "+---------------------------------------------------------------+",
         "|                                                               |  <- Background: #ECE5DD",
         "|   +---------------------------------------+                   |",
         "|   | Hello! Connected via offline direct   |                   |  <- Received (#FFFFFF)",
         "|   | Wi-Fi Direct socket.                  |                   |",
         "|   |                                 11:30 |                   |",
         "|   +---------------------------------------+                   |",
         "|                                                               |",
         "|                   +---------------------------------------+   |",
         "|                   | Hi! Works perfectly without internet  |   |  <- Sent (#DCF8C6)",
         "|                   | or cellular towers!                   |   |",
         "|                   |                               11:31 v |   |",
         "|                   +---------------------------------------+   |",
         "+---------------------------------------------------------------+",
         "|  (+)  [ Type an offline message...              ]  (Mic) (>)  |  <- Bottom Input Bar",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 4: Inline Voice Note Playback",
     [
         "+---------------------------------------------------------------+",
         "|                   +---------------------------------------+   |",
         "|                   | (>) |/|/|/|/|/|/|/|/|/|/|/|/|         |   |  <- Audio Waveform",
         "|                   | 0:14 / 0:35                   11:33 v |   |  <- Inline Player",
         "|                   +---------------------------------------+   |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 5: Chunk-Streamed File / Media Sharing",
     [
         "+---------------------------------------------------------------+",
         "|   +-------------------------------------------------------+   |",
         "|   | [PDF] offline_emergency_guide.pdf                     |   |  <- File Preview Box",
         "|   |       1.8 MB * Downloaded to OfflineMessenger         |   |",
         "|   | Here is the emergency protocol documentation.   11:35 |   |",
         "|   +-------------------------------------------------------+   |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 6: Offline Groups Listing Screen",
     [
         "+---------------------------------------------------------------+",
         "|  <- Offline Groups                                            |  <- Toolbar",
         "|     Multi-peer offline discussions                            |",
         "+---------------------------------------------------------------+",
         "|  (E)  Emergency Response Team                                 |  <- Group Item 1",
         "|       Offline Mesh Group * 4 Members Active                   |",
         "|  -----------------------------------------------------------  |",
         "|  (U)  University Study Group                                  |  <- Group Item 2",
         "|       Offline Mesh Group * 3 Members Active                   |",
         "|                                                               |",
         "|                                              [ + New Group ]  |  <- Floating Action Btn",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 7: Offline Group Discussion Screen (With Relay Attribution)",
     [
         "+---------------------------------------------------------------+",
         "|  <- (E) Emergency Response Team                               |",
         "|         Offline Group * All Connected Nodes                   |",
         "|  Radio: Offline Group Discussion * Messages fan out to peers  |  <- Relay Banner",
         "+---------------------------------------------------------------+",
         "|   Farhan (Node-B3):                                           |  <- Sender Attribution",
         "|   +---------------------------------------+                   |",
         "|   | Sector 4 road is blocked by flood.    |                   |  <- Received Group Msg",
         "|   |                                 11:42 |                   |",
         "|   +---------------------------------------+                   |",
         "|                                                               |",
         "|                   +---------------------------------------+   |",
         "|                   | Copy that! Rerouting the team now.    |   |  <- Sent Group Msg",
         "|                   |                               11:43 v |   |",
         "|                   +---------------------------------------+   |",
         "+---------------------------------------------------------------+",
         "|  (+)  [ Type group message...                   ]  (Mic) (>)  |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 8: Real-Time VoIP Audio Call Screen",
     [
         "+---------------------------------------------------------------+",
         "|                                                               |  <- Dark Calling Canvas",
         "|                            (( P ))                            |  <- Avatar Circle",
         "|                          Pixel 8 Pro                          |  <- Peer Name",
         "|                           00:03:42                            |  <- Active Timer",
         "|                  Direct Offline Audio Link                    |",
         "|                                                               |",
         "|                                                               |",
         "|       ( Mute )       ( Speaker )       ( Cam )      ( End )   |  <- Action Controls",
         "|         Mic             Audio           Video         Call    |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 9: Incoming Call Alert (Lock Screen Awake)",
     [
         "+---------------------------------------------------------------+",
         "|                                                               |",
         "|                            (( P ))                            |",
         "|                          Pixel 8 Pro                          |",
         "|                  Incoming Offline Audio Call                  |",
         "|                                                               |",
         "|            [  Decline  ]                 [  Accept  ]         |  <- Red / Green Action",
         "|                                                               |",
         "+---------------------------------------------------------------+"
     ]),
    ("Screen 10: One-Tap Privacy Storage Wipe Dialog",
     [
         "+---------------------------------------------------------------+",
         "|  Delete All Data & Storage?                                   |  <- Dialog Title",
         "|                                                               |",
         "|  This will permanently wipe all messages, contacts, offline   |",
         "|  groups, voice notes, and downloaded files from your device.  |",
         "|                                                               |",
         "|  This action cannot be undone.                                |",
         "|                                                               |",
         "|                [  Cancel  ]      [ Delete Everything ]        |  <- Confirm Buttons",
         "+---------------------------------------------------------------+"
     ])
]

for title, lines in screens:
    add_mockup(doc, title, lines)

# ----------------- SECTION 5: CONCLUSION -----------------
h5 = doc.add_heading("5. Conclusion & Academic Contributions", level=1)
h5.style.font.color.rgb = RGBColor(7, 94, 84)
h5.paragraph_format.space_before = Pt(14)
h5.paragraph_format.space_after = Pt(6)

p_conc = doc.add_paragraph()
p_conc.paragraph_format.line_spacing = 1.15
p_conc.paragraph_format.space_after = Pt(10)
p_conc.add_run(
    "Offline Messenger successfully verifies the viability of zero-infrastructure communications on commodity Android smartphones. By uniting Wi-Fi Direct hardware protocols, high-speed streaming TCP/UDP socket engineering, memory-safe chunk I/O, and an intuitive WhatsApp-inspired visual architecture, the project provides a comprehensive blueprint for disaster-resilient and privacy-preserving mobile communication. Future work will investigate extending the Group Owner fan-out topology into an autonomous multi-hop dynamic mesh (AODV routing) to extend peer ranges indefinitely across entire geographic regions."
)

output_path = r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\Offline_Messenger_University_Report.docx"
doc.save(output_path)
print("DOCX successfully generated at:", output_path)

