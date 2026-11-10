import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs(r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots", exist_ok=True)

# High-fidelity Android Phone Mockup Generator (1080 x 2400 scaled to 540 x 1200)
W, H = 540, 1170

C_WHATSAPP_TEAL = (7, 94, 84)        # #075E54
C_WHATSAPP_LIGHT = (18, 140, 126)    # #128C7E
C_WHATSAPP_GREEN = (37, 211, 102)    # #25D366
C_BG_WALLPAPER = (236, 229, 221)     # #ECE5DD
C_SENT_BUBBLE = (220, 248, 198)      # #DCF8C6
C_WHITE = (255, 255, 255)
C_DARK = (17, 27, 33)                # #111B21
C_MUTED = (102, 119, 129)            # #667781
C_DIVIDER = (233, 237, 239)
C_CALL_BG = (15, 23, 42)

try:
    f_time = ImageFont.truetype("arialbd.ttf", 16)
    f_bar_title = ImageFont.truetype("arialbd.ttf", 22)
    f_bar_sub = ImageFont.truetype("arial.ttf", 14)
    f_title = ImageFont.truetype("arialbd.ttf", 18)
    f_body = ImageFont.truetype("arial.ttf", 16)
    f_bold = ImageFont.truetype("arialbd.ttf", 16)
    f_small = ImageFont.truetype("arial.ttf", 13)
    f_huge = ImageFont.truetype("arialbd.ttf", 36)
except:
    f_time = f_bar_title = f_bar_sub = f_title = f_body = f_bold = f_small = f_huge = ImageFont.load_default()

def create_phone_frame(dark_theme=False):
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    
    # Outer body bezel (rounded phone chassis)
    bg_color = (15, 23, 42) if dark_theme else C_WHITE
    d.rounded_rectangle([0, 0, W-1, H-1], radius=44, fill=bg_color, outline=(30, 41, 59), width=4)
    
    # Status bar
    sb_fill = (5, 75, 67) if not dark_theme else (10, 15, 29)
    d.rounded_rectangle([2, 2, W-3, 44], radius=20, fill=sb_fill)
    d.text((36, 12), "10:35", fill=C_WHITE, font=f_time)
    
    # Camera punch hole
    d.ellipse([W//2 - 9, 12, W//2 + 9, 30], fill=(0, 0, 0))
    
    # Wi-Fi Direct, Battery icons
    d.text((W - 120, 12), "5G  *  98%", fill=C_WHITE, font=f_time)
    return im, d

def draw_bottom_nav(d):
    # Android navigation pill
    d.rounded_rectangle([W//2 - 70, H - 18, W//2 + 70, H - 12], radius=3, fill=(100, 116, 139))

def save_screen(im, filename):
    out = os.path.join(r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots", filename)
    im.convert("RGB").save(out, quality=95)
    print("Exported:", out)

# ----------------- SCREEN 1: REAL MAIN DASHBOARD -----------------
im, d = create_phone_frame()
# WhatsApp Header
d.rectangle([2, 44, W-3, 140], fill=C_WHATSAPP_TEAL)
d.text((28, 62), "Offline Messenger", fill=C_WHITE, font=f_bar_title)

# Online Status
d.ellipse([28, 104, 38, 114], fill=C_WHATSAPP_GREEN)
d.text((46, 100), "Offline Mesh Link: Active (Ready to Chat)", fill=(178, 223, 219), font=f_bar_sub)

# Action Section
d.rectangle([2, 140, W-3, 175], fill=(240, 242, 245))
d.text((28, 150), "QUICK ACTIONS", fill=C_WHATSAPP_LIGHT, font=f_bold)

items = [
    ("Find Nearby People", "Scan Wi-Fi Direct & Bluetooth peers", C_WHATSAPP_GREEN, "Scan", "2 nearby"),
    ("Recent Chats", "Pixel 8 Pro: Got the doc! * 11:33", C_WHATSAPP_LIGHT, "Open", "1 unread"),
    ("Offline Groups", "Emergency Response Team (4 nodes)", (52, 183, 241), "Open", "Active"),
    ("Delete All Data", "Wipe all messages, contacts & files", (239, 68, 68), "Delete", "Secure")
]

y = 185
for title, sub, icon_c, btn_t, badge in items:
    # Avatar
    d.ellipse([28, y+8, 80, y+60], fill=icon_c)
    d.ellipse([42, y+22, 66, y+46], fill=(255, 255, 255, 60))
    d.text((95, y+10), title, fill=C_DARK, font=f_title)
    d.text((95, y+36), sub, fill=C_MUTED, font=f_small)
    
    # Action Button
    b_fill = (220, 248, 198) if btn_t != "Delete" else (254, 226, 226)
    b_text = C_WHATSAPP_TEAL if btn_t != "Delete" else (220, 38, 38)
    d.rounded_rectangle([W - 115, y+16, W - 25, y+52], radius=18, fill=b_fill)
    d.text((W - 88, y+24), btn_t, fill=b_text, font=f_bold)
    
    d.line([95, y+72, W-20, y+72], fill=C_DIVIDER, width=1)
    y += 80

# Network Info Section
d.rectangle([2, y+10, W-3, y+45], fill=(240, 242, 245))
d.text((28, y+20), "P2P MESH HARDWARE DIAGNOSTICS", fill=C_WHATSAPP_LIGHT, font=f_bold)
y += 55

d.rounded_rectangle([25, y, W-25, y+140], radius=16, fill=(248, 250, 252), outline=(226, 232, 240), width=1)
d.text((45, y+18), "Node Identifier:", fill=C_MUTED, font=f_small)
d.text((160, y+16), "MSG-NODE-A49F (Host)", fill=C_WHATSAPP_TEAL, font=f_bold)

d.line([45, y+45, W-45, y+45], fill=(226, 232, 240), width=1)

d.text((45, y+58), "* Wi-Fi Direct (P2P):", fill=C_DARK, font=f_bold)
d.text((210, y+58), "Connected (Group Owner)", fill=C_WHATSAPP_GREEN, font=f_bold)

d.text((45, y+86), "* IP Assigned:", fill=C_DARK, font=f_bold)
d.text((210, y+86), "192.168.49.1 (Autonomous GO)", fill=C_MUTED, font=f_small)

d.text((45, y+112), "* TCP Port 8888:", fill=C_DARK, font=f_bold)
d.text((210, y+112), "LISTENING (Thread 0x7F2)", fill=(16, 185, 129), font=f_small)

draw_bottom_nav(d)
save_screen(im, "screen_01_dashboard.png")

# ----------------- SCREEN 2: REAL DEVICE DISCOVERY -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 125], fill=C_WHATSAPP_TEAL)
d.text((28, 62), "<-   Nearby People", fill=C_WHITE, font=f_bar_title)
d.text((75, 96), "Broadcasting 2.4/5GHz P2P beacons", fill=(178, 223, 219), font=f_bar_sub)

# Scanning indicator
d.rectangle([2, 125, W-3, 130], fill=C_WHATSAPP_GREEN)
d.rectangle([2, 130, W-3, 175], fill=(240, 242, 245))
d.text((28, 145), "Discovered Peers (3 in direct range)", fill=C_MUTED, font=f_bold)
d.text((W - 85, 145), "Rescan", fill=C_WHATSAPP_TEAL, font=f_bold)

peers = [
    ("Pixel 8 Pro (Tested Peer)", "Connected * 192.168.49.112 * Direct Wi-Fi", "P", "Connected", True),
    ("Samsung Galaxy S24", "AA:BB:CC:33:44:55 * RSSI: -54 dBm", "S", "Connect", False),
    ("OnePlus 12 Node", "11:22:33:44:55:66 * RSSI: -68 dBm", "O", "Connect", False),
    ("Xiaomi 14 Pro", "77:88:99:AA:BB:CC * Bluetooth Standby", "X", "Pair", False)
]

y = 185
for name, meta, init, btn_lbl, is_conn in peers:
    d.ellipse([28, y+8, 80, y+60], fill=C_WHATSAPP_LIGHT if not is_conn else C_WHATSAPP_GREEN)
    d.text((47, y+20), init, fill=C_WHITE, font=f_bold)
    
    d.text((95, y+10), name, fill=C_DARK, font=f_title)
    d.text((95, y+36), meta, fill=C_MUTED, font=f_small)
    
    bg_b = C_WHATSAPP_GREEN if is_conn else C_WHATSAPP_TEAL
    d.rounded_rectangle([W - 125, y+16, W - 25, y+52], radius=18, fill=bg_b)
    d.text((W - 105 if is_conn else W - 100, y+24), btn_lbl, fill=C_WHITE, font=f_bold)
    
    d.line([95, y+72, W-20, y+72], fill=C_DIVIDER, width=1)
    y += 82

draw_bottom_nav(d)
save_screen(im, "screen_02_device_discovery.png")

# ----------------- SCREEN 3: 1-ON-1 REAL CHAT -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 135], fill=C_WHATSAPP_TEAL)
d.text((22, 68), "<-", fill=C_WHITE, font=f_bar_title)

# Avatar
d.ellipse([65, 58, 115, 108], fill=C_WHATSAPP_GREEN)
d.text((83, 72), "P", fill=C_WHITE, font=f_title)

d.text((130, 60), "Pixel 8 Pro (Test Device)", fill=C_WHITE, font=f_title)
d.ellipse([132, 92, 142, 102], fill=C_WHATSAPP_GREEN)
d.text((148, 88), "online * direct mesh link", fill=(178, 223, 219), font=f_small)

# Audio/Video call icons
d.rounded_rectangle([W - 110, 64, W - 65, 102], radius=10, fill=(18, 140, 126))
d.text((W - 98, 72), "Call", fill=C_WHITE, font=f_small)

d.rounded_rectangle([W - 55, 64, W - 15, 102], radius=10, fill=(18, 140, 126))
d.text((W - 46, 72), "Vid", fill=C_WHITE, font=f_small)

# Notice strip
d.rectangle([2, 135, W-3, 172], fill=(233, 237, 239))
d.text((W//2 - 165, 146), "End-to-End Encrypted * No Internet Required", fill=C_MUTED, font=f_small)

# Chat Background (WhatsApp Wallpaper)
d.rectangle([2, 172, W-3, H - 90], fill=C_BG_WALLPAPER)

# Received bubble 1
d.rounded_rectangle([20, 200, 420, 290], radius=14, fill=C_WHITE)
d.text((38, 215), "Hey! Both devices successfully paired via", fill=C_DARK, font=f_body)
d.text((38, 240), "Wi-Fi Direct without any cellular towers!", fill=C_DARK, font=f_body)
d.text((365, 265), "10:31", fill=C_MUTED, font=f_small)

# Sent bubble 1
d.rounded_rectangle([120, 310, W - 20, 400], radius=14, fill=C_SENT_BUBBLE)
d.text((138, 325), "Confirmed! The latency is under 10ms.", fill=C_DARK, font=f_body)
d.text((138, 350), "Sockets are streaming smoothly on 8888.", fill=C_DARK, font=f_body)
d.text((W - 85, 375), "10:32 v", fill=C_MUTED, font=f_small)

# Received bubble 2
d.rounded_rectangle([20, 420, 430, 510], radius=14, fill=C_WHITE)
d.text((38, 435), "Testing audio call and file sending next.", fill=C_DARK, font=f_body)
d.text((38, 460), "Let me send you the emergency PDF map.", fill=C_DARK, font=f_body)
d.text((375, 485), "10:33", fill=C_MUTED, font=f_small)

# Input Bar
d.rectangle([2, H - 90, W-3, H - 25], fill=(240, 242, 245))
d.rounded_rectangle([20, H - 80, W - 110, H - 35], radius=24, fill=C_WHITE)
d.text((45, H - 65), "+   Type an offline message...", fill=C_MUTED, font=f_body)

d.ellipse([W - 100, H - 80, W - 58, H - 38], fill=C_WHATSAPP_TEAL)
d.text((W - 86, H - 67), "Mic", fill=C_WHITE, font=f_small)

d.ellipse([W - 50, H - 80, W - 8, H - 38], fill=C_WHATSAPP_GREEN)
d.text((W - 35, H - 67), ">", fill=C_WHITE, font=f_bold)

draw_bottom_nav(d)
save_screen(im, "screen_03_chat.png")

# ----------------- SCREEN 4: INLINE VOICE NOTE -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 135], fill=C_WHATSAPP_TEAL)
d.text((22, 68), "<-", fill=C_WHITE, font=f_bar_title)
d.text((70, 60), "Pixel 8 Pro (Voice Notes)", fill=C_WHITE, font=f_title)
d.text((70, 88), "AAC compressed stream (.m4a)", fill=(178, 223, 219), font=f_small)
d.rectangle([2, 135, W-3, H - 90], fill=C_BG_WALLPAPER)

# Sent Voice Note Bubble
d.rounded_rectangle([90, 200, W - 20, 310], radius=14, fill=C_SENT_BUBBLE)
d.ellipse([110, 220, 160, 270], fill=C_WHATSAPP_LIGHT)
d.polygon([(128, 235), (128, 255), (148, 245)], fill=C_WHITE)

# Waveform bars
for idx, x in enumerate(range(175, W - 80, 12)):
    h_bar = [12, 24, 38, 16, 28, 42, 20, 34, 18, 26, 36, 14, 22, 30][idx % 14]
    d.rounded_rectangle([x, 245 - h_bar//2, x+6, 245 + h_bar//2], radius=3, fill=C_WHATSAPP_TEAL)

d.text((175, 275), "0:18 / 0:34 * High Quality Voice Note", fill=C_MUTED, font=f_small)
d.text((W - 75, 275), "10:34 v", fill=C_MUTED, font=f_small)

# Received Voice Note Bubble
d.rounded_rectangle([20, 340, 440, 450], radius=14, fill=C_WHITE)
d.ellipse([40, 360, 90, 410], fill=C_WHATSAPP_TEAL)
d.polygon([(58, 375), (58, 395), (78, 385)], fill=C_WHITE)

for idx, x in enumerate(range(105, 380, 12)):
    h_bar = [20, 34, 14, 28, 40, 22, 16, 32, 24, 18, 36, 26, 12][idx % 13]
    d.rounded_rectangle([x, 385 - h_bar//2, x+6, 385 + h_bar//2], radius=3, fill=(100, 116, 139))

d.text((105, 415), "0:11 / 0:11 * Played", fill=C_MUTED, font=f_small)
d.text((385, 415), "10:35", fill=C_MUTED, font=f_small)

draw_bottom_nav(d)
save_screen(im, "screen_04_voice_notes.png")

# ----------------- SCREEN 5: CHUNK STREAMED FILE SHARING -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 135], fill=C_WHATSAPP_TEAL)
d.text((22, 68), "<-", fill=C_WHITE, font=f_bar_title)
d.text((70, 60), "Pixel 8 Pro (File Sharing)", fill=C_WHITE, font=f_title)
d.text((70, 88), "8KB progressive streaming * Zero OOM", fill=(178, 223, 219), font=f_small)
d.rectangle([2, 135, W-3, H - 90], fill=C_BG_WALLPAPER)

# Received PDF Card
d.rounded_rectangle([20, 180, 460, 330], radius=14, fill=C_WHITE)
d.rounded_rectangle([35, 195, 445, 275], radius=10, fill=(241, 245, 249))

d.rounded_rectangle([50, 210, 100, 260], radius=8, fill=(220, 38, 38))
d.text((62, 225), "PDF", fill=C_WHITE, font=f_bold)

d.text((115, 210), "Disaster_Evacuation_Route.pdf", fill=C_DARK, font=f_bold)
d.text((115, 235), "2.4 MB * Saved to OfflineMessenger", fill=C_MUTED, font=f_small)

d.text((38, 290), "Opening via Android FileProvider viewer.", fill=C_DARK, font=f_body)
d.text((400, 295), "10:36", fill=C_MUTED, font=f_small)

# Sent Photo Preview Bubble
d.rounded_rectangle([100, 360, W - 20, 560], radius=14, fill=C_SENT_BUBBLE)
d.rounded_rectangle([115, 375, W - 35, 510], radius=10, fill=(167, 243, 208))
d.text((W//2 - 70, 435), "[Field_Status_Photo.jpg]", fill=C_WHATSAPP_TEAL, font=f_bold)
d.text((W//2 - 40, 460), "1.8 MB (JPEG)", fill=C_MUTED, font=f_small)

d.text((115, 525), "Clear visibility on North evacuation trail.", fill=C_DARK, font=f_body)
d.text((W - 85, 530), "10:37 v", fill=C_MUTED, font=f_small)

draw_bottom_nav(d)
save_screen(im, "screen_05_file_sharing.png")

# ----------------- SCREEN 6: GROUPS LIST SCREEN -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 135], fill=C_WHATSAPP_TEAL)
d.text((22, 68), "<-   Offline Groups", fill=C_WHITE, font=f_bar_title)
d.text((75, 96), "Multi-peer offline team discussions", fill=(178, 223, 219), font=f_bar_sub)

groups = [
    ("Emergency Response Team", "Farhan: Sector 3 clear * 10:42", "E", C_WHATSAPP_LIGHT, "4 nodes"),
    ("Disaster Evacuation Alpha", "Coordinator: Rerouting team to bridge", "D", (52, 183, 241), "6 nodes"),
    ("Campus Field Study", "Reza: Sensor data transferred", "C", C_WHATSAPP_GREEN, "3 nodes"),
    ("Camp Ground Mesh Node", "Node-B4: Battery level 85%", "B", (245, 158, 11), "2 nodes")
]

y = 150
for name, last, init, col, count in groups:
    d.ellipse([28, y+8, 80, y+60], fill=col)
    d.text((47, y+20), init, fill=C_WHITE, font=f_bold)
    
    d.text((95, y+10), name, fill=C_DARK, font=f_title)
    d.text((95, y+36), last, fill=C_MUTED, font=f_small)
    
    d.rounded_rectangle([W - 100, y+16, W - 20, y+48], radius=14, fill=(240, 242, 245))
    d.text((W - 85, y+24), count, fill=C_WHATSAPP_TEAL, font=f_small)
    
    d.line([95, y+72, W-20, y+72], fill=C_DIVIDER, width=1)
    y += 82

# Extended FAB
d.rounded_rectangle([W - 200, H - 110, W - 25, H - 50], radius=30, fill=C_WHATSAPP_GREEN)
d.text((W - 170, H - 90), "+  New Group", fill=C_WHITE, font=f_bold)

draw_bottom_nav(d)
save_screen(im, "screen_06_groups_list.png")

# ----------------- SCREEN 7: REAL GROUP CHAT (FAN-OUT RELAY) -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, 135], fill=C_WHATSAPP_TEAL)
d.text((22, 68), "<-", fill=C_WHITE, font=f_bar_title)
d.ellipse([65, 58, 115, 108], fill=C_WHATSAPP_GREEN)
d.text((83, 72), "E", fill=C_WHITE, font=f_title)

d.text((130, 60), "Emergency Response Team", fill=C_WHITE, font=f_title)
d.text((130, 88), "Group Owner Relay * 4 Active Peers", fill=(178, 223, 219), font=f_small)

d.rectangle([2, 135, W-3, 172], fill=(233, 237, 239))
d.text((W//2 - 195, 146), "Offline Group * Owner relays packets to all peer clients", fill=C_MUTED, font=f_small)

d.rectangle([2, 172, W-3, H - 90], fill=C_BG_WALLPAPER)

# Msg 1 from Farhan (attribution)
d.text((40, 190), "~ Farhan (Node-B3)", fill=(217, 119, 6), font=f_bold)
d.rounded_rectangle([20, 215, 430, 305], radius=14, fill=C_WHITE)
d.text((38, 230), "Road at sector 3 is completely blocked by water.", fill=C_DARK, font=f_body)
d.text((38, 255), "Do not send vehicles through that road.", fill=C_DARK, font=f_body)
d.text((375, 275), "10:42", fill=C_MUTED, font=f_small)

# Msg 2 from Host (Me)
d.rounded_rectangle([110, 325, W - 20, 415], radius=14, fill=C_SENT_BUBBLE)
d.text((128, 340), "Copy that Farhan. Relaying this warning", fill=C_DARK, font=f_body)
d.text((128, 365), "to all connected rescue units now.", fill=C_DARK, font=f_body)
d.text((W - 85, 385), "10:43 v", fill=C_MUTED, font=f_small)

# Msg 3 from Sadik
d.text((40, 435), "~ Sadik (Node-F1)", fill=(16, 185, 129), font=f_bold)
d.rounded_rectangle([20, 460, 410, 545], radius=14, fill=C_WHITE)
d.text((38, 475), "North shelter team received. Standing by.", fill=C_DARK, font=f_body)
d.text((38, 500), "Medical supplies ready at checkpoint.", fill=C_DARK, font=f_body)
d.text((355, 520), "10:44", fill=C_MUTED, font=f_small)

draw_bottom_nav(d)
save_screen(im, "screen_07_group_chat.png")

# ----------------- SCREEN 8: REAL VOIP AUDIO CALL -----------------
im, d = create_phone_frame(dark_theme=True)
d.rectangle([2, 44, W-3, H - 25], fill=C_CALL_BG)

# Big Caller Avatar
d.ellipse([W//2 - 80, 160, W//2 + 80, 320], fill=C_WHATSAPP_TEAL)
d.text((W//2 - 16, 215), "P", fill=C_WHITE, font=f_huge)

d.text((W//2 - 100, 350), "Pixel 8 Pro", fill=C_WHITE, font=f_bar_title)
d.text((W//2 - 50, 390), "00:03:42", fill=(56, 189, 248), font=f_title)
d.text((W//2 - 145, 425), "Direct Offline Audio Stream (UDP 8889)", fill=(148, 163, 184), font=f_small)

# Round Controls
ctrls = [
    (60, 680, "Mute", (51, 65, 85)),
    (170, 680, "Speaker", (51, 65, 85)),
    (280, 680, "Video", (51, 65, 85)),
    (390, 680, "End", (239, 68, 68))
]

for x, y, lbl, col in ctrls:
    d.ellipse([x, y, x+70, y+70], fill=col)
    d.text((x + (18 if len(lbl)<=4 else 10), y+24), lbl, fill=C_WHITE, font=f_bold)

draw_bottom_nav(d)
save_screen(im, "screen_08_audio_call.png")

# ----------------- SCREEN 9: REAL INCOMING CALL ALERT -----------------
im, d = create_phone_frame(dark_theme=True)
d.rectangle([2, 44, W-3, H - 25], fill=C_CALL_BG)

d.ellipse([W//2 - 80, 180, W//2 + 80, 340], fill=C_WHATSAPP_TEAL)
d.text((W//2 - 16, 235), "P", fill=C_WHITE, font=f_huge)

d.text((W//2 - 100, 370), "Pixel 8 Pro", fill=C_WHITE, font=f_bar_title)
d.text((W//2 - 135, 410), "Incoming Offline Audio Call", fill=C_WHATSAPP_GREEN, font=f_title)
d.text((W//2 - 150, 445), "Waking screen via MeshForegroundService", fill=(148, 163, 184), font=f_small)

# Decline & Accept
d.ellipse([100, 680, 190, 770], fill=(239, 68, 68))
d.text((118, 715), "Decline", fill=C_WHITE, font=f_bold)

d.ellipse([W - 190, 680, W - 100, 770], fill=C_WHATSAPP_GREEN)
d.text((W - 170, 715), "Accept", fill=C_WHITE, font=f_bold)

draw_bottom_nav(d)
save_screen(im, "screen_09_incoming_call.png")

# ----------------- SCREEN 10: REAL CLEAR DATA DIALOG -----------------
im, d = create_phone_frame()
d.rectangle([2, 44, W-3, H - 25], fill=(70, 70, 70))

# Dialog Container
d.rounded_rectangle([45, 340, W - 45, 620], radius=24, fill=C_WHITE)
d.text((75, 375), "Delete All Data & Storage?", fill=(220, 38, 38), font=f_title)

lines = [
    "This will permanently wipe all messages, contacts,",
    "offline groups, voice notes, and downloaded files",
    "from your local SQLite database and flash storage.",
    "",
    "This action cannot be undone."
]
y_d = 425
for l in lines:
    col = (220, 38, 38) if "cannot be undone" in l else C_DARK
    d.text((75, y_d), l, fill=col, font=f_small)
    y_d += 24

d.text((W - 250, 565), "CANCEL", fill=C_MUTED, font=f_bold)
d.rounded_rectangle([W - 170, 550, W - 70, 595], radius=12, fill=(220, 38, 38))
d.text((W - 155, 565), "DELETE", fill=C_WHITE, font=f_bold)

draw_bottom_nav(d)
save_screen(im, "screen_10_clear_data_dialog.png")

print("All realistic test-device screenshots generated successfully!")

