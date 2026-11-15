import os
from PIL import Image, ImageDraw, ImageFont

RAW_DIR = "documentation/screenshots/raw"
OUT_DIR = "documentation/screenshots/annotated"
os.makedirs(OUT_DIR, exist_ok=True)

# Load font
try:
    font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 46)
    subfont = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 32)
except Exception:
    font = ImageFont.load_default()
    subfont = ImageFont.load_default()

def annotate_image(filename, annotations, out_filename=None):
    if out_filename is None:
        out_filename = filename
    raw_path = os.path.join(RAW_DIR, filename)
    if not os.path.exists(raw_path):
        print(f"Skipping {filename}: not found")
        return
    
    img = Image.open(raw_path).convert("RGBA")
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    primary_color = (255, 87, 34, 255)       # Orange
    tint_color = (255, 87, 34, 32)            # Subtle highlight fill
    white = (255, 255, 255, 255)
    shadow = (0, 0, 0, 180)
    
    for item in annotations:
        num = item["num"]
        box = item["box"]
        x1, y1, x2, y2 = box
        
        # Draw translucent highlight fill and rounded outline
        draw.rounded_rectangle([x1, y1, x2, y2], radius=20, fill=tint_color, outline=primary_color, width=8)
        
        # Badge coordinates: preferred top-left or specified
        bx = item.get("badge_x", max(45, x1 + 10))
        by = item.get("badge_y", max(45, y1 + 10))
        r = 42
        
        # Shadow
        draw.ellipse([bx - r + 4, by - r + 4, bx + r + 4, by + r + 4], fill=shadow)
        # Outer crisp white ring
        draw.ellipse([bx - r, by - r, bx + r, by + r], fill=white)
        # Inner vibrant orange circle
        draw.ellipse([bx - r + 5, by - r + 5, bx + r - 5, by + r - 5], fill=(230, 74, 25, 255))
        
        # Badge number
        text = str(num)
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        draw.text((bx - tw / 2, by - th / 2 - 5), text, fill=white, font=font)
    
    # Composite overlay onto raw screenshot
    final = Image.alpha_composite(img, overlay).convert("RGB")
    out_path = os.path.join(OUT_DIR, out_filename)
    final.save(out_path, quality=95)
    print(f"Annotated: {out_filename} ({len(annotations)} callouts)")

# Definitions for all 18 screens
ANNOTATIONS_CONFIG = {
    "01_home_dashboard.png": [
        {"num": 1, "box": [70, 180, 1000, 360]},
        {"num": 2, "box": [70, 400, 1370, 490]},
        {"num": 3, "box": [70, 520, 1370, 920]},
        {"num": 4, "box": [70, 950, 1370, 1500]},
        {"num": 5, "box": [140, 1530, 1300, 1712]},
        {"num": 6, "box": [70, 1750, 1370, 2220]},
        {"num": 7, "box": [70, 2250, 1370, 2600]},
        {"num": 8, "box": [0, 2800, 1440, 3050]}
    ],
    "02_routines_empty.png": [
        {"num": 1, "box": [70, 185, 680, 315]},
        {"num": 2, "box": [965, 180, 1370, 320]},
        {"num": 3, "box": [100, 1030, 1340, 1560]},
        {"num": 4, "box": [270, 1640, 1170, 1830]}
    ],
    "03_create_routine.png": [
        {"num": 1, "box": [70, 160, 1380, 350]},
        {"num": 2, "box": [70, 480, 1370, 720]},
        {"num": 3, "box": [70, 750, 1370, 980]},
        {"num": 4, "box": [70, 1020, 1370, 1260]},
        {"num": 5, "box": [70, 1300, 1370, 1680]},
        {"num": 6, "box": [70, 1950, 1370, 2600]}
    ],
    "04_routines_list.png": [
        {"num": 1, "box": [70, 375, 1370, 862]},
        {"num": 2, "box": [70, 900, 1370, 1060]},
        {"num": 3, "box": [70, 1150, 1370, 1920]},
        {"num": 4, "box": [1185, 1179, 1353, 1347]},
        {"num": 5, "box": [140, 1330, 1300, 1630]},
        {"num": 6, "box": [140, 1680, 1300, 1850]}
    ],
    "05_exercise_catalog.png": [
        {"num": 1, "box": [70, 160, 1370, 330]},
        {"num": 2, "box": [70, 370, 1370, 500]},
        {"num": 3, "box": [70, 520, 1370, 650]},
        {"num": 4, "box": [70, 670, 1370, 800]},
        {"num": 5, "box": [70, 850, 600, 930]},
        {"num": 6, "box": [70, 950, 1370, 2750]}
    ],
    "06_exercise_detail.png": [
        {"num": 1, "box": [70, 160, 1370, 330]},
        {"num": 2, "box": [70, 350, 1370, 1350]},
        {"num": 3, "box": [100, 1220, 1340, 1330]},
        {"num": 4, "box": [70, 1380, 1370, 1580]},
        {"num": 5, "box": [70, 1600, 1370, 1800]},
        {"num": 6, "box": [70, 1820, 1370, 2750]}
    ],
    "06b_create_exercise.png": [
        {"num": 1, "box": [70, 160, 1370, 330]},
        {"num": 2, "box": [70, 450, 1370, 700]},
        {"num": 3, "box": [70, 730, 1370, 1000]},
        {"num": 4, "box": [70, 1030, 1370, 1300]},
        {"num": 5, "box": [70, 1330, 1370, 1550]},
        {"num": 6, "box": [70, 1580, 1370, 2100]}
    ],
    "08_active_workout_with_timer.png": [
        {"num": 1, "box": [70, 160, 1370, 480]},
        {"num": 2, "box": [70, 500, 1370, 680]},
        {"num": 3, "box": [70, 720, 1370, 950]},
        {"num": 4, "box": [140, 970, 1300, 1080]},
        {"num": 5, "box": [140, 1090, 1300, 1260]},
        {"num": 6, "box": [140, 1270, 1300, 1600]},
        {"num": 7, "box": [70, 1620, 1370, 1980]},
        {"num": 8, "box": [70, 2020, 1370, 2300]}
    ],
    "09_dialog_custom_weight.png": [
        {"num": 1, "box": [180, 1080, 1260, 1220]},
        {"num": 2, "box": [180, 1240, 1260, 1470]},
        {"num": 3, "box": [180, 1500, 1260, 1680]},
        {"num": 4, "box": [180, 1720, 1260, 1940]}
    ],
    "10_dialog_custom_reps.png": [
        {"num": 1, "box": [180, 1080, 1260, 1220]},
        {"num": 2, "box": [180, 1240, 1260, 1470]},
        {"num": 3, "box": [180, 1500, 1260, 1680]},
        {"num": 4, "box": [180, 1720, 1260, 1940]}
    ],
    "12_workout_finish_summary.png": [
        {"num": 1, "box": [70, 160, 1370, 520]},
        {"num": 2, "box": [70, 550, 1370, 1200]},
        {"num": 3, "box": [70, 1230, 1370, 1650]},
        {"num": 4, "box": [70, 1680, 1370, 2100]},
        {"num": 5, "box": [70, 2600, 1370, 2790]}
    ],
    "13_progress_analytics.png": [
        {"num": 1, "box": [70, 160, 1370, 450]},
        {"num": 2, "box": [70, 480, 1370, 950]},
        {"num": 3, "box": [70, 980, 1370, 1750]},
        {"num": 4, "box": [70, 1800, 1370, 2750]}
    ],
    "14_profile_dashboard.png": [
        {"num": 1, "box": [70, 160, 1370, 360]},
        {"num": 2, "box": [70, 380, 1370, 850]},
        {"num": 3, "box": [70, 880, 1370, 1250]},
        {"num": 4, "box": [70, 2160, 699, 2480]},
        {"num": 5, "box": [720, 2160, 1370, 2480]}
    ],
    "15_measures_modal.png": [
        {"num": 1, "box": [100, 800, 1340, 950]},
        {"num": 2, "box": [100, 980, 1340, 1300]},
        {"num": 3, "box": [100, 1330, 1340, 2100]},
        {"num": 4, "box": [100, 2150, 1340, 2350]}
    ],
    "16_settings_main.png": [
        {"num": 1, "box": [56, 520, 1384, 880]},
        {"num": 2, "box": [56, 945, 1384, 1128]},
        {"num": 3, "box": [56, 1150, 1384, 1500]},
        {"num": 4, "box": [56, 1520, 1384, 1860]},
        {"num": 5, "box": [56, 1880, 1384, 2040]},
        {"num": 6, "box": [56, 2058, 1384, 2241]},
        {"num": 7, "box": [56, 2260, 1384, 2440]}
    ],
    "17_settings_notifications.png": [
        {"num": 1, "box": [70, 580, 1370, 840]},
        {"num": 2, "box": [70, 1330, 1370, 1570]},
        {"num": 3, "box": [70, 1600, 1370, 2020]},
        {"num": 4, "box": [70, 2080, 1370, 2320]},
        {"num": 5, "box": [1050, 2180, 1330, 2300]},
        {"num": 6, "box": [70, 2360, 1370, 2600]}
    ],
    "18_dialog_custom_reminder.png": [
        {"num": 1, "box": [150, 800, 1290, 950]},
        {"num": 2, "box": [150, 980, 1290, 1350]},
        {"num": 3, "box": [150, 1400, 1290, 2150]},
        {"num": 4, "box": [150, 2180, 1290, 2380]}
    ],
    "19_backup_management.png": [
        {"num": 1, "box": [70, 160, 1370, 500]},
        {"num": 2, "box": [70, 530, 1370, 950]},
        {"num": 3, "box": [70, 980, 1370, 1400]},
        {"num": 4, "box": [70, 1430, 1370, 1850]},
        {"num": 5, "box": [70, 1880, 1370, 2300]}
    ]
}

def main():
    print("Starting screenshot annotation...")
    for filename, items in ANNOTATIONS_CONFIG.items():
        annotate_image(filename, items)
    print("All annotations completed!")

if __name__ == "__main__":
    main()
