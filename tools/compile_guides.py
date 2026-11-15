import subprocess
import os
import sys
from pypdf import PdfReader, PdfWriter

EDGE_PATH = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if not os.path.exists(EDGE_PATH):
    EDGE_PATH = r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"

def build_pdf_from_html(html_path, raw_pdf_path):
    abs_html = os.path.abspath(html_path)
    abs_pdf = os.path.abspath(raw_pdf_path)
    
    cmd = [
        EDGE_PATH,
        "--headless",
        "--disable-gpu",
        "--run-all-compositor-stages-before-draw",
        "--print-to-pdf-no-header",
        "--no-pdf-header-footer",
        f"--print-to-pdf={abs_pdf}",
        f"file:///{abs_html}"
    ]
    print(f"Compiling {html_path} to PDF via Microsoft Edge...")
    res = subprocess.run(cmd, capture_output=True, text=True)
    if not os.path.exists(abs_pdf):
        print(f"Error: Failed to generate PDF {abs_pdf}")
        print("Stderr:", res.stderr)
        return False
    print(f"Raw PDF generated: {abs_pdf} ({os.path.getsize(abs_pdf)} bytes)")
    return True

def post_process_pdf(raw_pdf, final_pdf, lang="id"):
    reader = PdfReader(raw_pdf)
    writer = PdfWriter()
    
    for page in reader.pages:
        writer.add_page(page)
        
    num_pages = len(reader.pages)
    print(f"Total pages ({lang}): {num_pages}")
    
    if lang == "id":
        writer.add_metadata({
            "/Title": "KumaLift Panduan Pengguna & Referensi Teknis",
            "/Author": "BearBones Engineering Team",
            "/Subject": "Buku Panduan Resmi Aplikasi KumaLift v1.0.0 (Bahasa Indonesia)",
            "/Keywords": "KumaLift, Panduan Pengguna, Workout Tracker, Latihan Beban, Progressive Overload, SQLite",
            "/Creator": "BearBones Documentation Engine",
            "/Producer": "KumaLift Publishing v1.0.0"
        })
        
        # 24 Outlines for ID
        writer.add_outline_item("Halaman Sampul (Cover)", 0)
        writer.add_outline_item("Daftar Isi Dokumen", 1)
        writer.add_outline_item("Bab 1: Pendahuluan & Filosofi Desain", 2)
        writer.add_outline_item("Bab 2: Memulai Aplikasi & Navigasi Utama", 3)
        writer.add_outline_item("Bab 3: Workout Routine: Status Awal", 4)
        writer.add_outline_item("Bab 4: Pembuatan Blueprint Routine", 5)
        writer.add_outline_item("Bab 5: Manajemen Routine Aktif", 6)
        writer.add_outline_item("Bab 6: Katalog Gerakan Latihan", 7)
        writer.add_outline_item("Bab 7: Detail Gerakan & Panduan Eksekusi", 8)
        writer.add_outline_item("Bab 8: Penambahan Gerakan Kustom", 9)
        writer.add_outline_item("Bab 9: Pencatatan Latihan Aktif & Timer", 10)
        writer.add_outline_item("Bab 10: Input Beban (KG) & Repetisi Fleksibel", 11)
        writer.add_outline_item("Bab 11: Ringkasan Sesi & Evaluasi Latihan", 12)
        writer.add_outline_item("Bab 12: Analisis Progres & Estimasi 1RM", 13)
        writer.add_outline_item("Bab 13: Profil Atlet & Statistik Latihan", 14)
        writer.add_outline_item("Bab 14: Pengukuran Tubuh (Measures)", 15)
        writer.add_outline_item("Bab 15: Pengaturan Aplikasi & Preferensi", 16)
        writer.add_outline_item("Bab 16: Pengingat Latihan Harian", 17)
        writer.add_outline_item("Bab 17: Keypad Pengingat Waktu Kustom", 18)
        writer.add_outline_item("Bab 18: Pencadangan & Keamanan Data", 19)
        writer.add_outline_item("Bab 19: Alur Praktis Penggunaan di Gym", 20)
        writer.add_outline_item("Bab 20: Pemecahan Masalah & Pertanyaan Umum", 21)
        writer.add_outline_item("Bab 21: Glosarium Istilah & Panduan RPE", 22)
        writer.add_outline_item("Bab 22: Spesifikasi Teknis & Rilis", 23)

    else:
        writer.add_metadata({
            "/Title": "KumaLift User Guide & Technical Reference Manual",
            "/Author": "BearBones Engineering Team",
            "/Subject": "Official User Manual for KumaLift Application v1.0.0 (English Edition)",
            "/Keywords": "KumaLift, User Guide, Workout Logger, Strength Training, Progressive Overload, SQLite",
            "/Creator": "BearBones Documentation Engine",
            "/Producer": "KumaLift Publishing v1.0.0"
        })
        
        # 24 Outlines for EN
        writer.add_outline_item("Cover Page", 0)
        writer.add_outline_item("Table of Contents", 1)
        writer.add_outline_item("Chapter 1: Introduction & Design Philosophy", 2)
        writer.add_outline_item("Chapter 2: Getting Started & Navigation", 3)
        writer.add_outline_item("Chapter 3: Workout Routines: Initial State", 4)
        writer.add_outline_item("Chapter 4: Routine Blueprint Authoring", 5)
        writer.add_outline_item("Chapter 5: Active Routine Management", 6)
        writer.add_outline_item("Chapter 6: Exercise Catalog & Filtering", 7)
        writer.add_outline_item("Chapter 7: Movement Details & Execution Technique", 8)
        writer.add_outline_item("Chapter 8: Custom Exercise Creation", 9)
        writer.add_outline_item("Chapter 9: Active Workout Logging & Live Timers", 10)
        writer.add_outline_item("Chapter 10: Flexible Weight & Reps Input", 11)
        writer.add_outline_item("Chapter 11: Session Summary & PR Records", 12)
        writer.add_outline_item("Chapter 12: Progress Analytics & 1RM Curves", 13)
        writer.add_outline_item("Chapter 13: Athlete Profile & Lifetime Stats", 14)
        writer.add_outline_item("Chapter 14: Body Measures & Physical Tracking", 15)
        writer.add_outline_item("Chapter 15: Application Settings & Preferences", 16)
        writer.add_outline_item("Chapter 16: Accurate Notification Scheduling", 17)
        writer.add_outline_item("Chapter 17: Custom Notification Time Keypad", 18)
        writer.add_outline_item("Chapter 18: Data Backup, Restore & Safety", 19)
        writer.add_outline_item("Chapter 19: Practical Gym Training Workflow", 20)
        writer.add_outline_item("Chapter 20: Troubleshooting & FAQ", 21)
        writer.add_outline_item("Chapter 21: Reference Glossary & RPE Scale", 22)
        writer.add_outline_item("Chapter 22: Technical Specifications & Release", 23)

    with open(final_pdf, "wb") as f:
        writer.write(f)
        
    print(f"Final {lang.upper()} User Guide PDF ready: {final_pdf} ({os.path.getsize(final_pdf)} bytes)")

def compile_all():
    sys.path.append("tools")
    print("=== Step 1: Compiling Indonesian Guide ===")
    import build_user_guide_id
    build_user_guide_id.build()
    if build_pdf_from_html("documentation/source/user_guide_id.html", "documentation/source/raw_id.pdf"):
        post_process_pdf("documentation/source/raw_id.pdf", "documentation/KumaLift_User_Guide_ID.pdf", lang="id")

    print("\n=== Step 2: Compiling English Guide ===")
    import build_user_guide_en
    build_user_guide_en.build()
    if build_pdf_from_html("documentation/source/user_guide_en.html", "documentation/source/raw_en.pdf"):
        post_process_pdf("documentation/source/raw_en.pdf", "documentation/KumaLift_User_Guide_EN.pdf", lang="en")

    print("\n=== All User Guides successfully compiled! ===")

if __name__ == "__main__":
    compile_all()
