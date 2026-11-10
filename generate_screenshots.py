import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs(r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots", exist_ok=True)

# Colors
C_TEAL_DARK = (7, 94, 84)       # #075E54
C_TEAL_LIGHT = (18, 140, 126)   # #128C7E
C_GREEN = (37, 211, 102)        # #25D366
C_BG = (236, 229, 221)          # #ECE5DD
C_WHITE = (255, 255, 255)
C_TEXT_MAIN = (17, 27, 33)      # #111B21
C_TEXT_MUTED = (102, 119, 129)  # #667781
C_BUBBLE_SENT = (220, 248, 198) # #DCF8C6
C_DIVIDER = (233, 237, 239)
C_DARK_BG = (18, 24, 36)

def get_fonts():
    try:
        f_title = ImageFont.truetype("arialbd.ttf", 20)
        f_sub = ImageFont.truetype("arial.ttf", 13)
        f_bold = ImageFont.truetype("arialbd.ttf", 14)
        f_body = ImageFont.truetype("arial.ttf", 14)
        f_small = ImageFont.truetype("arial.ttf", 11)
        f_big = ImageFont.truetype("arialbd.ttf", 26)
    except:
        f_title = f_sub = f_bold = f_body = f_small = f_big = ImageFont.load_default()
    return f_title, f_sub, f_bold, f_body, f_small, f_big

f_title, f_sub, f_bold, f_body, f_small, f_big = get_fonts()

def create_base(status_text=""):
    im = Image.new("RGB", (420, 750), C_WHITE)
    draw = ImageDraw.Draw(im)
    
    # Status bar (Android clock/battery)
    draw.rectangle([0, 0, 420, 28], fill=(5, 75, 67))
    draw.text((16, 6), "12:00", fill=C_WHITE, font=f_small)
    draw.text((350, 6), "98% [===]", fill=C_WHITE, font=f_small)
    return im, draw

def save_img(im, name):
    path = os.path.join(r"d:\Nurix_Hive\Mesh_Connect_Offline_P2P_Chat\screenshots", name)
    im.save(path, quality=95)
    print(f"Saved: {path}")

# ==================== 1. MAIN DASHBOARD ====================
im, d = create_base()
# Toolbar
d.rectangle([0, 28, 420, 100], fill=C_TEAL_DARK)
d.text((20, 42), "Offline Messenger", fill=C_WHITE, font=f_title)
d.ellipse([20, 74, 28, 82], fill=C_GREEN)
d.text((34, 71), "Offline Messenger Ready", fill=(178, 223, 219), font=f_sub)

# Section: Actions
d.rectangle([0, 100, 420, 130], fill=(240, 242, 245))
d.text((20, 107), "ACTIONS", fill=C_TEAL_LIGHT, font=f_bold)

rows = [
    ("Find Nearby People", "Search via Bluetooth & Wi-Fi Direct", C_GREEN, "Scan"),
    ("Recent Chats", "View your offline conversations", C_TEAL_LIGHT, "Open"),
    ("Offline Groups", "Multi-peer offline discussions", (52, 183, 241), "Open"),
    ("Delete All Data", "Wipe all messages, contacts & files", (239, 83, 80), "Delete")
]

y = 135
for title, sub, icon_c, btn_t in rows:
    # Avatar circle
    d.ellipse([20, y+8, 64, y+52], fill=icon_c)
    # text
    d.text((76, y+10), title, fill=C_TEXT_MAIN, font=f_bold)
    d.text((76, y+30), sub, fill=C_TEXT_MUTED, font=f_small)
    # Button
    d.rounded_rectangle([320, y+14, 400, y+46], radius=16, fill=(220, 248, 198) if btn_t != "Delete" else (255, 235, 238))
    d.text((344, y+22), btn_t, fill=C_TEAL_DARK if btn_t != "Delete" else (198, 40, 40), font=f_bold)
    # divider
    d.line([76, y+62, 420, y+62], fill=C_DIVIDER, width=1)
    y += 65

# Section: Network Info
d.rectangle([0, y+10, 420, y+40], fill=(240, 242, 245))
d.text((20, y+17), "NETWORK INFO", fill=C_TEAL_LIGHT, font=f_bold)
y += 45

d.text((25, y+10), "Device ID: MSG-NODE-A49F", fill=C_TEXT_MAIN, font=f_bold)
d.text((25, y+35), "* Bluetooth Radio: Enabled & Ready", fill=C_TEXT_MUTED, font=f_sub)
d.text((25, y+60), "* Wi-Fi Direct: Standby (High-Speed P2P)", fill=C_TEXT_MUTED, font=f_sub)

save_img(im, "screen_01_dashboard.png")

# ==================== 2. DEVICE DISCOVERY ====================
im, d = create_base()
d.rectangle([0, 28, 420, 85], fill=C_TEAL_DARK)
d.text((20, 44), "<-  Nearby People", fill=C_WHITE, font=f_title)

# Progress indicator bar
d.rectangle([0, 85, 420, 89], fill=C_GREEN)
# Subtitle
d.rectangle([0, 89, 420, 130], fill=(240, 242, 245))
d.text((20, 100), "Scanning for nearby devices...", fill=C_TEXT_MUTED, font=f_sub)
d.text((360, 100), "Stop", fill=C_TEAL_DARK, font=f_bold)

devices = [
    ("Pixel 8 Pro (Nearby)", "AA:BB:CC:11:22:33 * Wi-Fi Direct", "P"),
    ("Samsung Galaxy S24", "DD:EE:FF:44:55:66 * Wi-Fi Direct", "S"),
    ("OnePlus 12 (Disaster Team)", "11:22:33:AA:BB:CC * Wi-Fi Direct", "O"),
    ("Xiaomi 14 Node", "99:88:77:66:55:44 * Bluetooth", "X")
]

y = 140
for name, addr, init in devices:
    d.ellipse([20, y+6, 64, y+50], fill=C_TEAL_LIGHT)
    d.text((36, y+18), init, fill=C_WHITE, font=f_bold)
    d.text((76, y+10), name, fill=C_TEXT_MAIN, font=f_bold)
    d.text((76, y+32), addr, fill=C_TEXT_MUTED, font=f_small)
    d.rounded_rectangle([310, y+14, 400, y+44], radius=15, fill=C_BUBBLE_SENT)
    d.text((330, y+21), "Connect", fill=C_TEAL_DARK, font=f_bold)
    d.line([76, y+60, 420, y+60], fill=C_DIVIDER, width=1)
    y += 68

save_img(im, "screen_02_device_discovery.png")

# ==================== 3. 1-ON-1 CHAT ====================
im, d = create_base()
d.rectangle([0, 28, 420, 90], fill=C_TEAL_DARK)
d.text((15, 46), "<-", fill=C_WHITE, font=f_title)
d.ellipse([45, 40, 80, 75], fill=C_GREEN)
d.text((58, 48), "P", fill=C_WHITE, font=f_bold)
d.text((90, 40), "Pixel 8 Pro", fill=C_WHITE, font=f_bold)
d.ellipse([90, 64, 96, 70], fill=C_GREEN)
d.text((102, 60), "online * offline mesh", fill=(178, 223, 219), font=f_small)

# Call buttons
d.text((330, 48), "[Audio]", fill=C_WHITE, font=f_sub)
d.text((375, 48), "[Video]", fill=C_WHITE, font=f_sub)

# Notice strip
d.rectangle([0, 90, 420, 116], fill=(233, 237, 239))
d.text((45, 96), "End-to-End Encrypted * No Internet Required", fill=C_TEXT_MUTED, font=f_small)

# Chat Background
d.rectangle([0, 116, 420, 680], fill=C_BG)

# Message 1 Received
d.rounded_rectangle([15, 140, 290, 205], radius=10, fill=C_WHITE)
d.text((25, 150), "Hey! Connected directly over Wi-Fi", fill=C_TEXT_MAIN, font=f_body)
d.text((25, 170), "Direct without any router or mobile data!", fill=C_TEXT_MAIN, font=f_body)
d.text((245, 188), "11:30", fill=C_TEXT_MUTED, font=f_small)

# Message 2 Sent
d.rounded_rectangle([130, 225, 405, 290], radius=10, fill=C_BUBBLE_SENT)
d.text((140, 235), "Awesome! Sub-10ms response time.", fill=C_TEXT_MAIN, font=f_body)
d.text((140, 255), "Totally off-grid P2P communication!", fill=C_TEXT_MAIN, font=f_body)
d.text((355, 273), "11:31 v", fill=C_TEXT_MUTED, font=f_small)

# Bottom Input
d.rectangle([0, 680, 420, 750], fill=C_WHITE)
d.text((15, 705), "[+]", fill=C_TEXT_MUTED, font=f_title)
d.rounded_rectangle([45, 695, 335, 735], radius=20, fill=(240, 242, 245))
d.text((60, 706), "Message", fill=C_TEXT_MUTED, font=f_body)
d.ellipse([345, 693, 375, 723], fill=C_GREEN)
d.text((354, 700), "Mic", fill=C_WHITE, font=f_small)
d.ellipse([380, 693, 410, 723], fill=C_GREEN)
d.text((390, 700), ">", fill=C_WHITE, font=f_bold)

save_img(im, "screen_03_chat.png")

# ==================== 4. VOICE NOTE ====================
im, d = create_base()
d.rectangle([0, 28, 420, 90], fill=C_TEAL_DARK)
d.text((15, 46), "<-", fill=C_WHITE, font=f_title)
d.text((50, 42), "Pixel 8 Pro (Voice Notes)", fill=C_WHITE, font=f_bold)
d.text((50, 62), "online * offline mesh link", fill=(178, 223, 219), font=f_small)
d.rectangle([0, 90, 420, 750], fill=C_BG)

# Voice bubble sent
d.rounded_rectangle([110, 160, 405, 235], radius=10, fill=C_BUBBLE_SENT)
d.ellipse([125, 175, 160, 210], fill=C_TEAL_LIGHT)
d.text((137, 183), "|>", fill=C_WHITE, font=f_bold)
d.line([175, 192, 350, 192], fill=C_TEAL_LIGHT, width=3)
# Waveform dots
for x in range(180, 345, 10):
    d.line([x, 185, x, 200], fill=C_TEAL_LIGHT, width=2)
d.text((175, 212), "0:18 / 0:34 (AAC Audio)", fill=C_TEXT_MUTED, font=f_small)
d.text((355, 212), "11:32 v", fill=C_TEXT_MUTED, font=f_small)

# Voice bubble received
d.rounded_rectangle([15, 260, 310, 335], radius=10, fill=C_WHITE)
d.ellipse([30, 275, 65, 310], fill=C_TEAL_DARK)
d.text((42, 283), "|>", fill=C_WHITE, font=f_bold)
for x in range(80, 250, 10):
    d.line([x, 285, x, 300], fill=C_TEAL_DARK, width=2)
d.text((80, 312), "0:09 / 0:12", fill=C_TEXT_MUTED, font=f_small)
d.text((260, 312), "11:34", fill=C_TEXT_MUTED, font=f_small)

save_img(im, "screen_04_voice_notes.png")

# ==================== 5. FILE / ATTACHMENT ====================
im, d = create_base()
d.rectangle([0, 28, 420, 90], fill=C_TEAL_DARK)
d.text((15, 46), "<-", fill=C_WHITE, font=f_title)
d.text((50, 42), "Pixel 8 Pro (File Sharing)", fill=C_WHITE, font=f_bold)
d.text((50, 62), "offline chunk streaming 8KB", fill=(178, 223, 219), font=f_small)
d.rectangle([0, 90, 420, 750], fill=C_BG)

# File bubble received
d.rounded_rectangle([15, 140, 340, 245], radius=10, fill=C_WHITE)
d.rounded_rectangle([25, 150, 330, 205], radius=6, fill=(241, 245, 249))
d.rectangle([35, 160, 70, 195], fill=(239, 68, 68))
d.text((43, 170), "PDF", fill=C_WHITE, font=f_bold)
d.text((80, 162), "emergency_map.pdf", fill=C_TEXT_MAIN, font=f_bold)
d.text((80, 182), "2.4 MB * Saved to Downloads", fill=C_TEXT_MUTED, font=f_small)
d.text((25, 215), "Here is the offline evacuation map.", fill=C_TEXT_MAIN, font=f_body)
d.text((290, 225), "11:35", fill=C_TEXT_MUTED, font=f_small)

# File bubble sent (Image)
d.rounded_rectangle([80, 270, 405, 410], radius=10, fill=C_BUBBLE_SENT)
d.rounded_rectangle([90, 280, 395, 370], radius=6, fill=(167, 243, 208))
d.text((170, 320), "[Photo Preview 1.8MB]", fill=C_TEAL_DARK, font=f_bold)
d.text((95, 380), "Field status captured offline.", fill=C_TEXT_MAIN, font=f_body)
d.text((355, 390), "11:36 v", fill=C_TEXT_MUTED, font=f_small)

save_img(im, "screen_05_file_sharing.png")

# ==================== 6. GROUPS LIST ====================
im, d = create_base()
d.rectangle([0, 28, 420, 90], fill=C_TEAL_DARK)
d.text((15, 46), "<-", fill=C_WHITE, font=f_title)
d.text((50, 42), "Offline Groups", fill=C_WHITE, font=f_title)
d.text((50, 64), "Multi-peer offline mesh discussions", fill=(178, 223, 219), font=f_small)

groups = [
    ("Emergency Response Team", "Farhan: Sector 3 clear * 11:42", "E", C_TEAL_LIGHT),
    ("Disaster Field Alpha", "Offline mesh group * 4 nodes", "D", (52, 183, 241)),
    ("Campus Study Group", "Reza: Connected via mesh * 10:15", "C", C_GREEN),
    ("Camp Site Beta", "Offline mesh group * 2 nodes", "B", (245, 158, 11))
]

y = 100
for name, last, init, col in groups:
    d.ellipse([20, y+8, 64, y+52], fill=col)
    d.text((36, y+18), init, fill=C_WHITE, font=f_bold)
    d.text((76, y+10), name, fill=C_TEXT_MAIN, font=f_bold)
    d.text((76, y+32), last, fill=C_TEXT_MUTED, font=f_small)
    d.line([76, y+62, 420, y+62], fill=C_DIVIDER, width=1)
    y += 66

# Floating Action Button
d.rounded_rectangle([250, 670, 400, 720], radius=25, fill=C_GREEN)
d.text((275, 686), "+ New Group", fill=C_WHITE, font=f_bold)

save_img(im, "screen_06_groups_list.png")

# ==================== 7. GROUP CHAT ====================
im, d = create_base()
d.rectangle([0, 28, 420, 90], fill=C_TEAL_DARK)
d.text((15, 46), "<-", fill=C_WHITE, font=f_title)
d.ellipse([45, 40, 80, 75], fill=C_GREEN)
d.text((58, 48), "E", fill=C_WHITE, font=f_bold)
d.text((90, 40), "Emergency Response Team", fill=C_WHITE, font=f_bold)
d.text((90, 60), "Offline Group * 4 Connected Nodes", fill=(178, 223, 219), font=f_small)

d.rectangle([0, 90, 420, 116], fill=(233, 237, 239))
d.text((30, 96), "Offline Group * Messages fan out to all nearby peers", fill=C_TEXT_MUTED, font=f_small)

d.rectangle([0, 116, 420, 750], fill=C_BG)

# Msg 1 from Farhan
d.text((25, 135), "~ Farhan (Node-B3)", fill=(217, 119, 6), font=f_bold)
d.rounded_rectangle([15, 155, 320, 215], radius=10, fill=C_WHITE)
d.text((25, 165), "Road at sector 3 is completely blocked.", fill=C_TEXT_MAIN, font=f_body)
d.text((25, 185), "Reroute team to North side.", fill=C_TEXT_MAIN, font=f_body)
d.text((275, 195), "11:42", fill=C_TEXT_MUTED, font=f_small)

# Msg 2 from Me
d.rounded_rectangle([110, 235, 405, 295], radius=10, fill=C_BUBBLE_SENT)
d.text((120, 245), "Copy that! Team re-routing now.", fill=C_TEXT_MAIN, font=f_body)
d.text((120, 265), "Relaying coordinates to all nodes.", fill=C_TEXT_MAIN, font=f_body)
d.text((355, 275), "11:43 v", fill=C_TEXT_MUTED, font=f_small)

# Msg 3 from Sadik
d.text((25, 315), "~ Sadik (Node-F1)", fill=(16, 185, 129), font=f_bold)
d.rounded_rectangle([15, 335, 300, 395], radius=10, fill=C_WHITE)
d.text((25, 345), "North team standby. Channel is open.", fill=C_TEXT_MAIN, font=f_body)
d.text((255, 375), "11:44", fill=C_TEXT_MUTED, font=f_small)

save_img(im, "screen_07_group_chat.png")

# ==================== 8. ACTIVE AUDIO CALL ====================
im, d = create_base()
d.rectangle([0, 28, 420, 750], fill=C_DARK_BG)

# Caller Avatar
d.ellipse([140, 160, 280, 300], fill=C_TEAL_DARK)
d.text((195, 210), "P", fill=C_WHITE, font=f_big)

d.text((150, 330), "Pixel 8 Pro", fill=C_WHITE, font=f_big)
d.text((170, 375), "00:03:42", fill=(56, 189, 248), font=f_title)
d.text((120, 410), "Direct Offline Audio Link (UDP 8889)", fill=(148, 163, 184), font=f_sub)

# Controls
buttons = [
    (50, 560, "Mute", (51, 65, 85)),
    (140, 560, "Speaker", (51, 65, 85)),
    (230, 560, "Camera", (51, 65, 85)),
    (320, 560, "End", (239, 68, 68))
]
for x, y, lbl, col in buttons:
    d.ellipse([x, y, x+56, y+56], fill=col)
    d.text((x+10, y+68), lbl, fill=C_WHITE, font=f_small)

save_img(im, "screen_08_audio_call.png")

# ==================== 9. INCOMING CALL ====================
im, d = create_base()
d.rectangle([0, 28, 420, 750], fill=C_DARK_BG)

d.ellipse([140, 180, 280, 320], fill=C_TEAL_DARK)
d.text((195, 230), "P", fill=C_WHITE, font=f_big)

d.text((150, 350), "Pixel 8 Pro", fill=C_WHITE, font=f_big)
d.text((115, 395), "Incoming Offline Audio Call", fill=C_GREEN, font=f_title)
d.text((135, 425), "Ringing over P2P radio link...", fill=(148, 163, 184), font=f_sub)

# Decline & Accept buttons
d.ellipse([70, 560, 140, 630], fill=(239, 68, 68))
d.text((85, 585), "Decline", fill=C_WHITE, font=f_bold)

d.ellipse([280, 560, 350, 630], fill=C_GREEN)
d.text((295, 585), "Accept", fill=C_WHITE, font=f_bold)

save_img(im, "screen_09_incoming_call.png")

# ==================== 10. CLEAR DATA DIALOG ====================
im, d = create_base()
# Background dashboard darkened
d.rectangle([0, 28, 420, 750], fill=(50, 50, 50))

# Dialog Card
d.rounded_rectangle([30, 240, 390, 480], radius=16, fill=C_WHITE)
d.text((50, 265), "Delete All Data & Storage?", fill=(220, 38, 38), font=f_title)

desc = [
    "This will permanently wipe all messages, contacts,",
    "offline groups, voice notes, and downloaded files",
    "from your device storage.",
    "",
    "This action cannot be undone."
]
y = 305
for line in desc:
    d.text((50, y), line, fill=C_TEXT_MAIN if line != "This action cannot be undone." else (220, 38, 38), font=f_sub)
    y += 20

# Action buttons
d.text((180, 435), "Cancel", fill=C_TEXT_MUTED, font=f_bold)
d.rounded_rectangle([250, 425, 370, 460], radius=8, fill=(220, 38, 38))
d.text((265, 435), "Delete All", fill=C_WHITE, font=f_bold)

save_img(im, "screen_10_clear_data_dialog.png")

print("All 10 screenshot images successfully generated!")

