import os, subprocess, time

adb = os.path.join(os.environ['LOCALAPPDATA'], 'Android', 'Sdk', 'platform-tools', 'adb.exe')
RAW_DIR = 'documentation/screenshots/raw'

def adb_cmd(*args):
    return subprocess.run([adb] + list(args), capture_output=True, text=True).stdout.strip()

def tap(x, y, delay=1.0):
    adb_cmd('shell', 'input', 'tap', str(x), str(y))
    time.sleep(delay)

def keyevent(code, delay=0.8):
    adb_cmd('shell', 'input', 'keyevent', code)
    time.sleep(delay)

def screencap(filename):
    remote = f'/sdcard/{filename}'
    local = os.path.join(RAW_DIR, filename)
    adb_cmd('shell', 'screencap', '-p', remote)
    adb_cmd('pull', remote, local)
    print(f'Captured {filename}: {os.path.getsize(local)} bytes')

# Refresh 01_home_dashboard
screencap('01_home_dashboard.png')

# Go to Profile
tap(1276, 2917, delay=1.2) # Profile tab
screencap('14_profile_dashboard.png')

# Tap Settings
tap(1307, 256, delay=1.2)
screencap('16_settings_main.png')

# Tap Notifications
tap(720, 1036, delay=1.2)
screencap('17_settings_notifications.png')

# Tap Custom... button
tap(1190, 2240, delay=1.2)
screencap('18_dialog_custom_reminder.png')

# Close Custom Reminder dialog by tapping Cancel
tap(779, 2289, delay=1.0)

# Back to Settings
keyevent('KEYCODE_BACK', delay=1.0)

# Tap Export & Import Data
tap(720, 2149, delay=1.2)
screencap('19_backup_management.png')

# Back to Profile, then Home
keyevent('KEYCODE_BACK', delay=1.0)
tap(155, 2917, delay=1.0)

print("Settings flow capture finished!")
