import os
import subprocess
import time

ADB = os.path.expandvars(r"$LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
RAW_DIR = "documentation/screenshots/raw"
os.makedirs(RAW_DIR, exist_ok=True)

def adb_cmd(*args):
    cmd = [ADB] + list(args)
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip()

def tap(x, y, delay=0.8):
    adb_cmd("shell", "input", "tap", str(x), str(y))
    time.sleep(delay)

def keyevent(code, delay=0.8):
    adb_cmd("shell", "input", "keyevent", code)
    time.sleep(delay)

def type_text(text, delay=0.8):
    adb_cmd("shell", "input", "text", text)
    time.sleep(delay)

def screencap(filename):
    remote = f"/sdcard/{filename}"
    local = os.path.join(RAW_DIR, filename)
    adb_cmd("shell", "screencap", "-p", remote)
    adb_cmd("pull", remote, local)
    print(f"Captured: {filename} ({os.path.getsize(local)} bytes)")

def run_workflow():
    print("Beginning automated live screen capture workflow...")
    
    # 1. Home Dashboard
    tap(155, 2917, delay=1.2) # Tap Home tab
    screencap("01_home_dashboard.png")
    
    # 2 & 4. Routines Tab & Routines List
    tap(417, 2917, delay=1.2) # Tap Workouts tab
    screencap("04_routines_list.png")
    
    # 3. Create Routine Screen
    tap(1168, 249, delay=1.2) # Tap Create Routine
    screencap("03_create_routine.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 2. Routines Empty State (delete existing routine)
    tap(1269, 1263, delay=1.2) # Tap delete icon
    screencap("02_routines_empty.png")
    
    # Recreate routine so user manual shows active routine
    tap(1168, 249, delay=1.0)
    tap(300, 620, delay=0.8)
    type_text("Push%sDay%sA%sFocus", delay=0.8)
    tap(720, 2448, delay=1.0) # Add exercise
    tap(1143, 1243, delay=1.0) # Select 3/4 Sit-up
    tap(1268, 277, delay=1.2) # Save routine
    
    # 5. Exercises Catalog
    tap(716, 2917, delay=1.2) # Exercises tab
    screencap("05_exercise_catalog.png")
    
    # 6. Exercise Detail
    tap(500, 1200, delay=1.5) # Open first exercise
    screencap("06_exercise_detail.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 6b. Create Exercise
    tap(1258, 249, delay=1.2) # Create button
    screencap("06b_create_exercise.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 8. Active Workout Logger with Rest Timer
    tap(155, 2917, delay=1.0) # Home tab
    tap(720, 1621, delay=1.5) # Start Workout
    tap(720, 1066, delay=1.2) # Add Exercise
    tap(1143, 1243, delay=1.2) # Select exercise
    tap(1181, 1441, delay=1.5) # Check set 1 -> starts rest timer
    screencap("08_active_workout_with_timer.png")
    
    # 9. Dialog Custom Weight
    tap(623, 1680, delay=1.2) # Tap KG cell set 2
    screencap("09_dialog_custom_weight.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 10. Dialog Custom Reps
    tap(831, 1680, delay=1.2) # Tap Reps cell set 2
    screencap("10_dialog_custom_reps.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 12. Workout Finish Summary
    tap(1248, 267, delay=1.0) # Top right Finish
    tap(1088, 1791, delay=1.5) # Confirm Finish in dialog
    screencap("12_workout_finish_summary.png")
    tap(720, 2699, delay=1.5) # Save & Finish session
    
    # 13. Progress Analytics
    tap(1010, 2917, delay=1.2) # Progress tab
    screencap("13_progress_analytics.png")
    
    # 14. Profile Dashboard
    tap(1276, 2917, delay=1.2) # Profile tab
    screencap("14_profile_dashboard.png")
    
    # 15. Measures Modal
    tap(384, 2321, delay=1.2) # Measures card
    screencap("15_measures_modal.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    
    # 16. Settings Main
    tap(1307, 256, delay=1.2) # Settings gear
    screencap("16_settings_main.png")
    
    # 17. Settings Notifications
    tap(720, 1036, delay=1.2) # Notifications row
    screencap("17_settings_notifications.png")
    
    # 18. Dialog Custom Reminder
    tap(1190, 2240, delay=1.2) # Custom... button
    screencap("18_dialog_custom_reminder.png")
    keyevent("KEYCODE_BACK", delay=1.0) # Close reminder dialog
    keyevent("KEYCODE_BACK", delay=1.0) # Back to Settings
    
    # 19. Backup Management
    tap(720, 2149, delay=1.2) # Export & Import Data
    screencap("19_backup_management.png")
    keyevent("KEYCODE_BACK", delay=1.0)
    keyevent("KEYCODE_BACK", delay=1.0) # Back to Profile
    tap(155, 2917, delay=1.0) # Back to Home
    
    print("Capture workflow completed successfully!")

if __name__ == "__main__":
    run_workflow()
