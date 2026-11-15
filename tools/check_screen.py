import os, subprocess, re

adb = os.path.join(os.environ['LOCALAPPDATA'], 'Android', 'Sdk', 'platform-tools', 'adb.exe')
subprocess.run([adb, 'shell', 'uiautomator', 'dump', '/sdcard/fg.xml'])
txt = subprocess.run([adb, 'shell', 'cat', '/sdcard/fg.xml'], capture_output=True, text=True).stdout
matches = re.findall(r'text="([^"]+)"', txt)
print("Foreground UI text elements:")
for m in matches[:25]:
    print(" -", m)
