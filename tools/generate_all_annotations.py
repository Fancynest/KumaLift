import os
from PIL import Image, ImageDraw, ImageFont

RAW_DIR = "documentation/screenshots/original"
OUT_DIR = "documentation/screenshots/annotated"
os.makedirs(OUT_DIR, exist_ok=True)

try:
    font = ImageFont.truetype("arialbd.ttf", 34)
except:
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 34)
    except:
        font = ImageFont.load_default()

def annotate_image(filename, annotations):
    raw_path = os.path.join(RAW_DIR, filename)
    if not os.path.exists(raw_path):
        print(f"Skipping {filename}: not found in {RAW_DIR}")
        return
    
    img = Image.open(raw_path).convert("RGBA")
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    # Modern, refined annotation style:
    # Sky Blue primary accent - visible against dark UI and lime buttons
    stroke_color = (56, 189, 248, 255)     # Sky-400
    tint_color = (56, 189, 248, 18)        # Very subtle 7% highlight tint
    badge_bg = (14, 165, 233, 255)         # Sky-500
    white = (255, 255, 255, 255)
    shadow = (0, 0, 0, 160)
    
    for item in annotations:
        num = item["num"]
        box = item["box"]
        x1, y1, x2, y2 = box
        
        # Subtle, crisp rounded outline with thin 4px stroke
        draw.rounded_rectangle([x1, y1, x2, y2], radius=16, fill=tint_color, outline=stroke_color, width=4)
        
        # Determine badge placement:
        bx = item.get("badge_x", x1 + 28)
        by = item.get("badge_y", y1 - 4)
        
        # Keep inside screen bounds
        bx = max(38, min(img.size[0] - 38, bx))
        by = max(38, min(img.size[1] - 38, by))
        
        r = 28 # badge radius (56px diameter)
        
        # Drop shadow for badge
        draw.ellipse([bx - r + 3, by - r + 3, bx + r + 3, by + r + 3], fill=shadow)
        # Outer crisp white ring
        draw.ellipse([bx - r, by - r, bx + r, by + r], fill=white)
        # Inner vibrant sky-blue circle
        draw.ellipse([bx - r + 3, by - r + 3, bx + r - 3, by + r - 3], fill=badge_bg)
        
        # Crisp badge number
        text = str(num)
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        draw.text((bx - tw / 2, by - th / 2 - 3), text, fill=white, font=font)
    
    # Composite overlay onto raw screenshot
    final = Image.alpha_composite(img, overlay).convert("RGB")
    out_path = os.path.join(OUT_DIR, filename)
    final.save(out_path, quality=95)
    print(f"Annotated: {filename} ({len(annotations)} callouts)")

# Precise, verified, non-overlapping annotations for all 18 screens
ANNOTATIONS_CONFIG = {
    "01_home_dashboard.png": [
        {"num": 1, "box": [60, 200, 1380, 500], "badge_x": 90, "badge_y": 190},
        {"num": 2, "box": [70, 560, 1370, 1000], "badge_x": 100, "badge_y": 550},
        {"num": 3, "box": [70, 1070, 1370, 1920], "badge_x": 100, "badge_y": 1060},
        {"num": 4, "box": [70, 1980, 1370, 2290], "badge_x": 100, "badge_y": 1970},
        {"num": 5, "box": [0, 2810, 1440, 3050], "badge_x": 100, "badge_y": 2800}
    ],
    "02_routines_empty.png": [
        {"num": 1, "box": [70, 185, 680, 315], "badge_x": 90, "badge_y": 175},
        {"num": 2, "box": [965, 180, 1370, 320], "badge_x": 985, "badge_y": 170},
        {"num": 3, "box": [100, 1030, 1340, 1560], "badge_x": 130, "badge_y": 1020},
        {"num": 4, "box": [270, 1640, 1170, 1830], "badge_x": 300, "badge_y": 1630}
    ],
    "03_create_routine.png": [
        {"num": 1, "box": [1150, 205, 1385, 350], "badge_x": 1170, "badge_y": 195},
        {"num": 2, "box": [70, 530, 1370, 690], "badge_x": 100, "badge_y": 520},
        {"num": 3, "box": [70, 810, 1150, 1040], "badge_x": 100, "badge_y": 800},
        {"num": 4, "box": [70, 1220, 1370, 1720], "badge_x": 100, "badge_y": 1210},
        {"num": 5, "box": [500, 2300, 940, 2450], "badge_x": 520, "badge_y": 2290}
    ],
    "04_routines_list.png": [
        {"num": 1, "box": [70, 370, 1370, 850], "badge_x": 100, "badge_y": 360},
        {"num": 2, "box": [70, 900, 780, 1050], "badge_x": 100, "badge_y": 890},
        {"num": 3, "box": [70, 1110, 1370, 1880], "badge_x": 100, "badge_y": 1100},
        {"num": 4, "box": [140, 1620, 1300, 1800], "badge_x": 170, "badge_y": 1610}
    ],
    "05_exercise_catalog.png": [
        {"num": 1, "box": [1190, 180, 1380, 310], "badge_x": 1210, "badge_y": 170},
        {"num": 2, "box": [70, 370, 1370, 530], "badge_x": 100, "badge_y": 360},
        {"num": 3, "box": [70, 580, 1370, 940], "badge_x": 100, "badge_y": 570},
        {"num": 4, "box": [70, 1100, 1370, 1350], "badge_x": 100, "badge_y": 1090}
    ],
    "06_exercise_detail.png": [
        {"num": 1, "box": [0, 520, 1440, 1300], "badge_x": 60, "badge_y": 530},
        {"num": 2, "box": [70, 1340, 600, 1530], "badge_x": 90, "badge_y": 1330},
        {"num": 3, "box": [70, 1560, 1370, 1720], "badge_x": 100, "badge_y": 1550},
        {"num": 4, "box": [70, 2350, 1370, 2600], "badge_x": 100, "badge_y": 2340}
    ],
    "06b_create_exercise.png": [
        {"num": 1, "box": [1160, 200, 1385, 345], "badge_x": 1180, "badge_y": 190},
        {"num": 2, "box": [70, 570, 1370, 750], "badge_x": 100, "badge_y": 560},
        {"num": 3, "box": [70, 1050, 1370, 1430], "badge_x": 100, "badge_y": 1040},
        {"num": 4, "box": [70, 1720, 1370, 2280], "badge_x": 100, "badge_y": 1710}
    ],
    "08_active_workout_with_timer.png": [
        {"num": 1, "box": [70, 380, 1370, 850], "badge_x": 100, "badge_y": 370},
        {"num": 2, "box": [530, 1320, 890, 1480], "badge_x": 550, "badge_y": 1310},
        {"num": 3, "box": [1080, 1330, 1260, 1470], "badge_x": 1100, "badge_y": 1320},
        {"num": 4, "box": [70, 2360, 1370, 2530], "badge_x": 100, "badge_y": 2350},
        {"num": 5, "box": [180, 2660, 1260, 2850], "badge_x": 210, "badge_y": 2650}
    ],
    "09_dialog_custom_weight.png": [
        {"num": 1, "box": [240, 1290, 1200, 1490], "badge_x": 210, "badge_y": 1280},
        {"num": 2, "box": [230, 1600, 1210, 1735], "badge_x": 210, "badge_y": 1590},
        {"num": 3, "box": [900, 1835, 1200, 1965], "badge_x": 880, "badge_y": 1825}
    ],
    "10_dialog_custom_reps.png": [
        {"num": 1, "box": [240, 1290, 1200, 1490], "badge_x": 210, "badge_y": 1280},
        {"num": 2, "box": [230, 1600, 1210, 1735], "badge_x": 210, "badge_y": 1590},
        {"num": 3, "box": [900, 1835, 1200, 1965], "badge_x": 880, "badge_y": 1825}
    ],
    "12_workout_finish_summary.png": [
        {"num": 1, "box": [70, 330, 1370, 770], "badge_x": 100, "badge_y": 320},
        {"num": 2, "box": [70, 840, 1370, 1540], "badge_x": 100, "badge_y": 830},
        {"num": 3, "box": [70, 1610, 1370, 1980], "badge_x": 100, "badge_y": 1600},
        {"num": 4, "box": [70, 2080, 1370, 2270], "badge_x": 100, "badge_y": 2070}
    ],
    "13_progress_analytics.png": [
        {"num": 1, "box": [70, 340, 1370, 510], "badge_x": 100, "badge_y": 330},
        {"num": 2, "box": [70, 580, 1370, 1420], "badge_x": 100, "badge_y": 570},
        {"num": 3, "box": [70, 1490, 1370, 2150], "badge_x": 100, "badge_y": 1480},
        {"num": 4, "box": [70, 2330, 1370, 2600], "badge_x": 100, "badge_y": 2320}
    ],
    "14_profile_dashboard.png": [
        {"num": 1, "box": [1240, 180, 1380, 310], "badge_x": 1260, "badge_y": 170},
        {"num": 2, "box": [70, 370, 1370, 690], "badge_x": 100, "badge_y": 360},
        {"num": 3, "box": [70, 1000, 1370, 1650], "badge_x": 100, "badge_y": 990},
        {"num": 4, "box": [70, 1820, 1370, 2340], "badge_x": 100, "badge_y": 1810}
    ],
    "15_measures_modal.png": [
        {"num": 1, "box": [70, 2060, 750, 2140], "badge_x": 60, "badge_y": 2040},
        {"num": 2, "box": [80, 2270, 1360, 2430], "badge_x": 90, "badge_y": 2240},
        {"num": 3, "box": [80, 2520, 1360, 2680], "badge_x": 90, "badge_y": 2490},
        {"num": 4, "box": [80, 2770, 1360, 2920], "badge_x": 100, "badge_y": 2760}
    ],
    "16_settings_main.png": [
        {"num": 1, "box": [70, 550, 1370, 1080], "badge_x": 100, "badge_y": 540},
        {"num": 2, "box": [70, 1260, 1370, 1430], "badge_x": 100, "badge_y": 1250},
        {"num": 3, "box": [70, 1430, 1370, 1590], "badge_x": 100, "badge_y": 1420},
        {"num": 4, "box": [70, 1600, 1370, 1760], "badge_x": 100, "badge_y": 1590},
        {"num": 5, "box": [70, 1940, 1370, 2100], "badge_x": 100, "badge_y": 1930}
    ],
    "17_settings_notifications.png": [
        {"num": 1, "box": [70, 560, 1370, 820], "badge_x": 100, "badge_y": 550},
        {"num": 2, "box": [70, 1370, 1370, 1630], "badge_x": 100, "badge_y": 1360},
        {"num": 3, "box": [70, 1750, 1040, 1855], "badge_x": 90, "badge_y": 1740},
        {"num": 4, "box": [1060, 1750, 1370, 1855], "badge_x": 1080, "badge_y": 1740},
        {"num": 5, "box": [70, 1980, 1370, 2130], "badge_x": 100, "badge_y": 1970}
    ],
    "18_dialog_custom_reminder.png": [
        {"num": 1, "box": [320, 920, 1150, 1180], "badge_x": 300, "badge_y": 910},
        {"num": 2, "box": [270, 1260, 1160, 2050], "badge_x": 260, "badge_y": 1250},
        {"num": 3, "box": [910, 2140, 1210, 2290], "badge_x": 890, "badge_y": 2130}
    ],
    "19_backup_management.png": [
        {"num": 1, "box": [70, 500, 1370, 730], "badge_x": 100, "badge_y": 490},
        {"num": 2, "box": [70, 740, 1370, 960], "badge_x": 100, "badge_y": 730},
        {"num": 3, "box": [70, 970, 1370, 1190], "badge_x": 100, "badge_y": 960},
        {"num": 4, "box": [70, 1290, 1370, 1510], "badge_x": 100, "badge_y": 1280},
        {"num": 5, "box": [70, 1960, 1370, 2370], "badge_x": 100, "badge_y": 1950}
    ]
}

def run():
    print("Generating refined, calibrated annotations...")
    for filename, anns in ANNOTATIONS_CONFIG.items():
        annotate_image(filename, anns)
    print("Done!")

if __name__ == "__main__":
    run()
