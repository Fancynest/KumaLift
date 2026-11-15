import os
import subprocess
import time
import pypdf

EDGE_PATH = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
ROOT_DIR = "c:/Users/Gabriel/AndroidStudioProjects/KumaLift"
HTML_PATH = os.path.join(ROOT_DIR, "documentation/source/user_guide.html")
RAW_PDF = os.path.join(ROOT_DIR, "documentation/source/raw_manual.pdf")
FINAL_PDF = os.path.join(ROOT_DIR, "documentation/KumaLift_User_Manual.pdf")

def compile_pdf():
    print("Compiling HTML to PDF using Microsoft Edge headless engine...")
    cmd = [
        EDGE_PATH,
        "--headless=new",
        "--disable-gpu",
        "--run-all-compositor-stages-before-draw",
        "--no-pdf-header-footer",
        f"--print-to-pdf={RAW_PDF}",
        HTML_PATH
    ]
    subprocess.run(cmd, check=True)
    time.sleep(2)
    
    if not os.path.exists(RAW_PDF):
        raise FileNotFoundError(f"Raw PDF was not created: {RAW_PDF}")
    
    print(f"Raw PDF generated: {RAW_PDF} ({os.path.getsize(RAW_PDF)} bytes)")
    
    # Process with pypdf
    print("Post-processing PDF: adding metadata and structured outline bookmarks...")
    reader = pypdf.PdfReader(RAW_PDF)
    writer = pypdf.PdfWriter()
    writer.append(reader)
    
    total_pages = len(reader.pages)
    print(f"Total pages in document: {total_pages}")
    
    # Add metadata
    writer.add_metadata({
        "/Title": "KumaLift Application User Manual & Technical Guide",
        "/Author": "BearBones Engineering Team",
        "/Subject": "Official User Manual and Technical Documentation v1.0.0",
        "/Keywords": "KumaLift, Workout Logger, Hypertrophy, Fitness, Progressive Overload, SQLite, User Manual",
        "/Creator": "KumaLift Documentation Engine (Edge Headless + pypdf)",
        "/Producer": "KumaLift Publishing v1.0.0"
    })
    
    # Add hierarchical outline bookmarks
    # Page indices are 0-based
    writer.add_outline_item("Halaman Sampul (Cover)", 0)
    writer.add_outline_item("B. Daftar Isi & Navigasi Dokumen", 1)
    writer.add_outline_item("C. Pendahuluan & Filosofi Sistem", 2)
    writer.add_outline_item("D. Memulai Aplikasi & Navigasi Utama", 3)
    
    # Section E group
    e_parent = writer.add_outline_item("E. Dokumentasi Fitur Komprehensif", 4)
    writer.add_outline_item("E.1 Workout Routine: Konsep & Empty State", 4, parent=e_parent)
    writer.add_outline_item("E.1 Pembuatan & Kartu Workout Routine", 5, parent=e_parent)
    writer.add_outline_item("E.2 Katalog Gerakan & Multi-Tier Filter", 6, parent=e_parent)
    writer.add_outline_item("E.2 Visual Demo Player & Custom Exercise", 7, parent=e_parent)
    writer.add_outline_item("E.3 Active Workout Logger & Rest Timer", 8, parent=e_parent)
    writer.add_outline_item("E.3 Dialog Input Custom KG & Repetisi", 9, parent=e_parent)
    writer.add_outline_item("E.4 Penyelesaian Sesi & Selebrasi PR", 10, parent=e_parent)
    writer.add_outline_item("E.5 Analisis Progresi, 1RM & Kurva Beban", 11, parent=e_parent)
    writer.add_outline_item("E.6 Profil Atlet & Pengukuran Tubuh (Measures)", 12, parent=e_parent)
    
    writer.add_outline_item("G. Pengaturan Aplikasi & Preferensi", 13)
    writer.add_outline_item("H. Sistem Notifikasi Presisi & Keypad Reminder", 14)
    writer.add_outline_item("I. Manajemen Data & 7-Day Backup Cooldown", 15)
    writer.add_outline_item("F. Alur Penggunaan Skenario Nyata", 16)
    writer.add_outline_item("J. Panduan Pemecahan Masalah & FAQ", 17)
    writer.add_outline_item("K. Glosarium Istilah Fitness & Tabel RPE", 18)
    writer.add_outline_item("L. Penutup & Spesifikasi Rilis v1.0.0", 19)
    
    with open(FINAL_PDF, "wb") as f_out:
        writer.write(f_out)
        
    print(f"Final User Manual PDF successfully compiled: {FINAL_PDF}")
    print(f"File Size: {os.path.getsize(FINAL_PDF):,} bytes ({os.path.getsize(FINAL_PDF) / 1024 / 1024:.2f} MB)")
    print(f"Total Pages: {total_pages} pages")

if __name__ == "__main__":
    compile_pdf()
