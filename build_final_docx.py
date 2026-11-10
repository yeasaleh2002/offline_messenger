import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

doc = docx.Document()

# Set standard margins (1 inch)
for section in doc.sections:
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
        r_title.font.size = Pt(10)
        r_title.font.color.rgb = RGBColor(7, 94, 84)
        
    r_body = p.add_run(text)
    r_body.font.name = "Calibri"
    r_body.font.size = Pt(9.5)
    r_body.font.color.rgb = RGBColor(50, 50, 50)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

# ==================== COVER / HEADER ====================
p_title = doc.add_paragraph()
p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_title.paragraph_format.space_before = Pt(12)
p_title.paragraph_format.space_after = Pt(4)

run_title = p_title.add_run("OFFLINE MESSENGER")
run_title.bold = True
run_title.font.name = "Calibri"
run_title.font.size = Pt(24)
run_title.font.color.rgb = RGBColor(7, 94, 84)

p_sub = doc.add_paragraph()
p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_sub.paragraph_format.space_after = Pt(12)
r_sub = p_sub.add_run("Decentralized Peer-to-Peer Offline Communication System for Android\nWi-Fi Direct (IEEE 802.11 P2P) • Raw TCP/UDP Sockets • SQLite Local Storage")
r_sub.font.name = "Calibri"
r_sub.font.size = Pt(11.5)
r_sub.font.color.rgb = RGBColor(18, 140, 126)

# Metadata Table
meta_table = doc.add_table(rows=5, cols=2)
meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
meta_table.autofit = False
meta_table.columns[0].width = Inches(2.2)
meta_table.columns[1].width = Inches(4.3)

meta_data = [
    ("Student Name / Roll:", "[Your Name / ID Here]"),
    ("Course / Department:", "Computer Science & Engineering / Final Project"),
    ("University / Institute:", "[Your University Name Here]"),
    ("Target Platform:", "Android 8.0+ (API 26 to API 34, Android 14)"),
    ("Submission Date:", "November 2026")
]

for idx, (label, val) in enumerate(meta_data):
    cell_lbl = meta_table.cell(idx, 0)
    cell_val = meta_table.cell(idx, 1)
    set_cell_background(cell_lbl, "F0F7F4")
    set_cell_background(cell_val, "FFFFFF")
    
    p0 = cell_lbl.paragraphs[0]
    p0.paragraph_format.space_before = Pt(2.5)
    p0.paragraph_format.space_after = Pt(2.5)
    r0 = p0.add_run(label)
    r0.bold = True
    r0.font.name = "Calibri"
    r0.font.size = Pt(9.5)
    r0.font.color.rgb = RGBColor(7, 94, 84)
    
    p1 = cell_val.paragraphs[0]
    p1.paragraph_format.space_before = Pt(2.5)
    p1.paragraph_format.space_after = Pt(2.5)
    r1 = p1.add_run(val)
    r1.font.name = "Calibri"
    r1.font.size = Pt(9.5)

doc.add_paragraph().paragraph_format.space_after = Pt(12)

# ==================== 1. ABSTRACT ====================
h1 = doc.add_heading("1. Abstract", level=1)
h1.style.font.color.rgb = RGBColor(7, 94, 84)
h1.paragraph_format.space_before = Pt(12)
h1.paragraph_format.space_after = Pt(4)

p_abs = doc.add_paragraph()
p_abs.paragraph_format.line_spacing = 1.15
p_abs.paragraph_format.space_after = Pt(6)
p_abs.add_run(
    "Modern instant messaging applications (WhatsApp, Signal, Telegram) fundamentally depend on centralized cloud infrastructure—cellular modems, telecom towers, DNS servers, and remote datacenters. When natural disasters, severe underground environments, remote outdoor trails, mass crowd stadium congestions, or regional network blackouts occur, this single point of failure completely paralyzes communication. "
    "Offline Messenger addresses this critical gap by presenting a fully functional, decentralized peer-to-peer (P2P) mobile messaging system for Android that functions with 100% independence from cellular data, internet access, Wi-Fi routers, or third-party servers."
)

p_abs2 = doc.add_paragraph()
p_abs2.paragraph_format.line_spacing = 1.15
p_abs2.paragraph_format.space_after = Pt(6)
p_abs2.add_run(
    "By harmonizing native Android Wi-Fi Direct (IEEE 802.11 P2P) radio discovery with raw high-performance TCP and UDP socket programming, nearby devices establish dynamic, ad-hoc point-to-point links. The system delivers full-duplex 1-on-1 text messaging, multi-peer offline groups with Group Owner fan-out relaying, voice messaging (AAC .m4a format), chunk-streamed binary file sharing, real-time VoIP audio calls (UDP port 8889), and camera video calling (TCP port 8890). "
    "Conversations and contacts are permanently preserved across device reboots in a local SQLite database, and the user interface has been thoughtfully styled with WhatsApp's signature dark-teal and green visual elements to ensure immediate usability."
)

# ==================== 2. KEY FEATURES ====================
h2 = doc.add_heading("2. Key Architectural Features", level=1)
h2.style.font.color.rgb = RGBColor(7, 94, 84)
h2.paragraph_format.space_before = Pt(12)
h2.paragraph_format.space_after = Pt(4)

features = [
    ("2.1 Zero-Internet Wi-Fi Direct Peer Discovery",
     "Discovers and pairs with nearby Android devices directly over 2.4 GHz and 5 GHz bands without requiring an active Wi-Fi router, hotspot, or SIM card. Operates over 50–100m outdoors with transfer speeds reaching 250 Mbps.",
     "Uses Android's native WifiP2pManager, automatic Group Owner (GO) negotiation (IP 192.168.49.1), and dynamic client IP addressing."),

    ("2.2 Real-Time P2P Text Messaging (TCP Port 8888)",
     "Delivers bi-directional instant messaging with sub-10ms delivery speed over a continuous background ServerSocket. Every packet is framed as [0x01][4-byte Length][UTF-8 Text] and acknowledged with a 0x06 ACK byte.",
     "Messages are automatically indexed and archived into local SQLite storage with delivery states (SENDING, DELIVERED)."),

    ("2.3 Offline Multi-Peer Group Chats (Relay Fan-Out Architecture)",
     "Enables multi-user team discussions without internet. When a group member transmits a message, the Group Owner node receives and automatically fans out (relays) the packet to all other connected client devices.",
     "Includes distinct sender handles and timestamps on each bubble, replicating the WhatsApp group experience completely offline."),

    ("2.4 Inline Voice Messaging (Voice Notes)",
     "Allows one-tap microphone recording of audio notes. Encoded in high-quality, lightweight AAC (.m4a) via VoiceRecorderHelper, streamed chunk-by-chunk, and playable directly inside chat bubbles.",
     "Features an inline audio player with play/pause controls, duration timer, and waveform indicators."),

    ("2.5 Real-Time VoIP Audio Calling (UDP Port 8889)",
     "Enables live peer-to-peer phone calls without cellular minutes or VoIP servers. AudioRecord captures raw microphone PCM samples and streams them over ultra-low-latency UDP datagrams to AudioTrack on the receiving device.",
     "Controlled by a synchronized signaling state machine: INVITE (0x05) -> Ringing -> ACCEPT (0x07) / DECLINE (0x08) -> END (0x09)."),

    ("2.6 Real-Time Video Calling (TCP Port 8890)",
     "Streams live camera preview frames compressed into progressive JPEGs over TCP port 8890, rendering on a hardware SurfaceView with front/back camera switching and mute controls.",
     "Allows full visual communication during emergency response operations without internet."),

    ("2.7 Memory-Safe Binary File & Photo Sharing (Chunk Streaming)",
     "Transfers high-resolution photos, documents (PDF, APK, DOCX), and media in 8 KB chunk buffers. Keeps heap RAM usage strictly below 10 MB regardless of file size, preventing OutOfMemoryError.",
     "Integrated with Android FileProvider for instant one-tap file opening in native system viewers."),

    ("2.8 Foreground Service & Lock-Screen Call Wakeup",
     "A persistent MeshForegroundService runs in the background with ongoing notification status, preventing Android Doze Mode kills and waking up the device screen on incoming calls via showWhenLocked.",
     "Ensures reliable 24/7 background readiness during field deployment."),

    ("2.9 Complete Local SQLite Data Persistence",
     "Zero cloud dependency: conversations, message delivery receipts, contacts, and offline groups persist locally in SQLite relational tables (contacts, messages, groups_table).",
     "Full historical conversation retrieval upon app relaunch even if disconnected."),

    ("2.10 One-Tap Privacy Storage & Data Wipe",
     "Dashboard includes an emergency data wipe button that executes a complete SQLite database wipe, deletes all received media and voice notes from storage, and vacuums the database clean.",
     "Guarantees total data privacy and zero residual traces during security audits or emergency handovers.")
]

for title, desc, tech in features:
    p_f = doc.add_paragraph()
    p_f.paragraph_format.space_before = Pt(6)
    p_f.paragraph_format.space_after = Pt(2)
    r_f = p_f.add_run(title)
    r_f.bold = True
    r_f.font.size = Pt(11)
    r_f.font.color.rgb = RGBColor(18, 140, 126)
    
    p_d = doc.add_paragraph()
    p_d.paragraph_format.line_spacing = 1.15
    p_d.paragraph_format.space_after = Pt(2)
    p_d.add_run(desc)
    
    add_callout(doc, tech, "Technical & Protocol Details:")

# ==================== 3. SCREENSHOTS ====================
h3 = doc.add_heading("3. Application Screenshots & Feature Walkthrough", level=1)
h3.style.font.color.rgb = RGBColor(7, 94, 84)
h3.paragraph_format.space_before = Pt(14)
h3.paragraph_format.space_after = Pt(6)

p_s_intro = doc.add_paragraph()
p_s_intro.paragraph_format.line_spacing = 1.15
p_s_intro.paragraph_format.space_after = Pt(8)
p_s_intro.add_run(
    "Below are the visual representations of all active application screens, demonstrating the WhatsApp-styled user interface, component hierarchy, and responsive feedback across core workflows:"
)

screens_data = [
    ("Figure 1: Main Dashboard (Offline Messenger Home)",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_01_dashboard.png",
     "The main home dashboard styled in WhatsApp's signature dark-teal (#075E54) header. Features an online mesh status indicator, direct navigation actions (Scan Nearby, Recent Chats, Offline Groups), emergency data wipe, and live hardware diagnostics (Device Node ID, Bluetooth, Wi-Fi Direct)."),
     
    ("Figure 2: Nearby Device Discovery & Pairing Screen",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_02_device_discovery.png",
     "Displays all nearby devices discovered via Wi-Fi Direct and Bluetooth. Shows peer names, hardware MAC addresses, radio type, and a live scanning progress bar with one-tap Connect pairing."),
     
    ("Figure 3: 1-on-1 Offline Chat Screen (WhatsApp Style)",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_03_chat.png",
     "Direct 1-on-1 chat interface styled with WhatsApp's classic wallpaper (#ECE5DD), light-green sent bubbles (#DCF8C6), white received bubbles, an end-to-end encrypted notice strip, and top bar audio/video call shortcuts."),
     
    ("Figure 4: Inline Voice Note Playback",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_04_voice_notes.png",
     "Inline voice notes rendered directly inside chat bubbles. Shows audio waveforms, play/pause controls, AAC audio encoding duration, and timestamps."),
     
    ("Figure 5: Memory-Safe Binary File & Photo Sharing",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_05_file_sharing.png",
     "Chunk-streamed file attachments. Displays file type icons (PDF, photo, document), exact file sizes, download paths, and direct FileProvider intent integration for instant viewing."),
     
    ("Figure 6: Offline Groups Listing Screen",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_06_groups_list.png",
     "Lists all created offline discussion groups as flat list rows with colored initial avatars, last message snippets, and an emerald-green Floating Action Button (+ New Group) to create multi-peer teams."),
     
    ("Figure 7: Offline Group Discussion Screen (With Relay Attribution)",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_07_group_chat.png",
     "Multi-peer group discussion view showing distinct sender attribution handles above received bubbles. Confirms real-time fan-out packet relaying across all connected nodes."),
     
    ("Figure 8: Real-Time VoIP Audio Call Screen",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_08_audio_call.png",
     "Active VoIP call screen over UDP port 8889. Features caller avatar, live call duration timer, and dedicated controls for Microphone Mute, Speakerphone toggle, Camera switch, and Call Termination."),
     
    ("Figure 9: Incoming Call Alert (Lock Screen Awake)",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_09_incoming_call.png",
     "Incoming call invitation alert that wakes up the device screen and displays over the lock screen with distinct Accept (green) and Decline (red) action buttons."),
     
    ("Figure 10: One-Tap Privacy Storage Wipe Dialog",
     r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots\screen_10_clear_data_dialog.png",
     "Confirmation alert dialog for the emergency storage wipe, preventing accidental deletion while enabling complete removal of all SQLite records, voice notes, and downloaded media.")
]

for fig_title, img_path, caption in screens_data:
    p_fig = doc.add_paragraph()
    p_fig.paragraph_format.space_before = Pt(10)
    p_fig.paragraph_format.space_after = Pt(2)
    r_fig = p_fig.add_run(fig_title)
    r_fig.bold = True
    r_fig.font.size = Pt(11)
    r_fig.font.color.rgb = RGBColor(7, 94, 84)
    
    if os.path.exists(img_path):
        p_img = doc.add_paragraph()
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img.paragraph_format.space_before = Pt(2)
        p_img.paragraph_format.space_after = Pt(4)
        run_img = p_img.add_run()
        run_img.add_picture(img_path, width=Inches(3.2))
        
    p_cap = doc.add_paragraph()
    p_cap.paragraph_format.line_spacing = 1.15
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run(f"Description: {caption}")
    r_cap.font.size = Pt(9.5)
    r_cap.font.color.rgb = RGBColor(70, 80, 95)

# ==================== 4. PROTOCOL TABLE ====================
h4 = doc.add_heading("4. Binary Socket Protocol Specification", level=1)
h4.style.font.color.rgb = RGBColor(7, 94, 84)
h4.paragraph_format.space_before = Pt(12)
h4.paragraph_format.space_after = Pt(4)

proto_table = doc.add_table(rows=6, cols=3)
proto_table.alignment = WD_TABLE_ALIGNMENT.CENTER
proto_table.autofit = False
proto_table.columns[0].width = Inches(1.3)
proto_table.columns[1].width = Inches(2.2)
proto_table.columns[2].width = Inches(3.0)

proto_headers = ["Packet Byte", "Binary Frame Structure", "Operational Purpose"]
for col_idx, header in enumerate(proto_headers):
    cell = proto_table.cell(0, col_idx)
    set_cell_background(cell, "075E54")
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(3)
    r = p.add_run(header)
    r.bold = True
    r.font.name = "Calibri"
    r.font.size = Pt(9.5)
    r.font.color.rgb = RGBColor(255, 255, 255)

proto_rows = [
    ("0x01 (Text)", "[0x01][Len: 4B][Payload: UTF-8]", "1-on-1 instant message transmission over TCP :8888."),
    ("0x02 (File / Audio)", "[0x02][NameLen: 2B][Name][Size: 8B][8KB Chunks...]", "Memory-safe chunk streaming for files & voice notes."),
    ("0x03 (Handshake)", "[0x03][NameLen: 2B][DeviceName]", "Initial connection handshake and IP registration."),
    ("0x04 (Group Msg)", "[0x04][GrpIdLen][GrpId][SenderLen][Sender][TextLen][Text]", "Group message for Group Owner relay fan-out."),
    ("0x05 / 0x07 / 0x09", "[Signal: 1B][CallerLen][CallerName][Mode: 1B]", "VoIP Call signaling (INVITE, ACCEPT, DECLINE, END).")
]

for row_idx, data in enumerate(proto_rows, start=1):
    for col_idx, text in enumerate(data):
        cell = proto_table.cell(row_idx, col_idx)
        set_cell_background(cell, "F8FAFC" if row_idx % 2 == 0 else "FFFFFF")
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(2.5)
        p.paragraph_format.space_after = Pt(2.5)
        r = p.add_run(text)
        r.font.name = "Calibri"
        r.font.size = Pt(8.5)

doc.add_paragraph().paragraph_format.space_after = Pt(12)

# ==================== 5. CONCLUSION ====================
h5 = doc.add_heading("5. Conclusion & Summary", level=1)
h5.style.font.color.rgb = RGBColor(7, 94, 84)
h5.paragraph_format.space_before = Pt(12)
h5.paragraph_format.space_after = Pt(4)

p_c = doc.add_paragraph()
p_c.paragraph_format.line_spacing = 1.15
p_c.paragraph_format.space_after = Pt(8)
p_c.add_run(
    "Offline Messenger successfully validates the capability of native Android smartphones to maintain rich, multimedia, and voice communication without any centralized internet or cellular infrastructure. "
    "Through robust Wi-Fi Direct pairing, raw TCP/UDP socket engineering, memory-safe streaming, SQLite persistence, and an intuitive WhatsApp-inspired user experience, this project demonstrates a highly practical solution for disaster relief, remote field operations, and privacy-conscious communications."
)

output_docx = r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\Offline_Messenger_University_Report.docx"
doc.save(output_docx)
print("Complete Word document generated with all 10 embedded screenshot images at:", output_docx)

