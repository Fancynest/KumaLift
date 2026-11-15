import os
import base64

IMG_DIR = "documentation/screenshots/annotated"
OUT_FILE = "documentation/source/user_guide_id.html"

def get_b64(filename):
    path = os.path.join(IMG_DIR, filename)
    if not os.path.exists(path):
        print(f"Warning: {filename} not found in {IMG_DIR}")
        return ""
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("utf-8")
    return f"data:image/png;base64,{data}"

def get_b64_file(path):
    if not os.path.exists(path):
        return ""
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("utf-8")
    return f"data:image/png;base64,{data}"

def build():
    print("Loading screenshots for Indonesian guide...")
    app_icon = get_b64_file("composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher.png")
    imgs = {
        "01": get_b64("01_home_dashboard.png"),
        "02": get_b64("02_routines_empty.png"),
        "03": get_b64("03_create_routine.png"),
        "04": get_b64("04_routines_list.png"),
        "05": get_b64("05_exercise_catalog.png"),
        "06": get_b64("06_exercise_detail.png"),
        "06b": get_b64("06b_create_exercise.png"),
        "08": get_b64("08_active_workout_with_timer.png"),
        "09": get_b64("09_dialog_custom_weight.png"),
        "10": get_b64("10_dialog_custom_reps.png"),
        "12": get_b64("12_workout_finish_summary.png"),
        "13": get_b64("13_progress_analytics.png"),
        "14": get_b64("14_profile_dashboard.png"),
        "15": get_b64("15_measures_modal.png"),
        "16": get_b64("16_settings_main.png"),
        "17": get_b64("17_settings_notifications.png"),
        "18": get_b64("18_dialog_custom_reminder.png"),
        "19": get_b64("19_backup_management.png"),
    }
    print("Screenshots ready. Generating HTML...")

    css = """
    @page {
        size: 210mm 297mm;
        margin: 0;
    }
    * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
    }
    body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        background-color: #0B0F17;
        color: #F1F5F9;
        font-size: 8.6pt;
        line-height: 1.48;
        width: 210mm;
    }
    .page {
        width: 210mm;
        height: 297mm;
        max-height: 297mm;
        padding: 12mm 15mm 10mm 15mm;
        page-break-after: always;
        break-after: page;
        page-break-inside: avoid;
        break-inside: avoid;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        position: relative;
        overflow: hidden;
        background: #0B0F17;
    }
    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #1E293B;
        padding-bottom: 4px;
        margin-bottom: 8px;
        font-size: 7pt;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: #64748B;
    }
    .header .brand {
        font-weight: 700;
        color: #38BDF8;
    }
    .footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #1E293B;
        padding-top: 4px;
        margin-top: 8px;
        font-size: 7pt;
        color: #64748B;
    }
    .content {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        overflow: hidden;
    }
    h1 {
        font-size: 17pt;
        font-weight: 800;
        color: #F8FAFC;
        letter-spacing: -0.3px;
        margin-bottom: 4px;
    }
    h2 {
        font-size: 11.5pt;
        font-weight: 700;
        color: #F8FAFC;
        letter-spacing: -0.2px;
        margin-bottom: 4px;
    }
    h3 {
        font-size: 9pt;
        font-weight: 700;
        color: #E2E8F0;
        margin-top: 6px;
        margin-bottom: 3px;
    }
    p {
        color: #94A3B8;
        margin-bottom: 6px;
    }
    .lead {
        font-size: 9pt;
        line-height: 1.5;
        color: #CBD5E1;
        margin-bottom: 8px;
    }
    
    /* Layout Patterns */
    .grid-2col {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
    }
    .feature-layout {
        display: grid;
        grid-template-columns: 74mm 1fr;
        gap: 16px;
        align-items: start;
    }
    
    /* Screenshot Container */
    .screenshot-frame {
        background: #000;
        border: 1px solid #1E293B;
        border-radius: 9px;
        overflow: hidden;
        box-shadow: 0 4px 14px rgba(0,0,0,0.45);
        text-align: center;
    }
    .screenshot-frame img {
        width: 100%;
        max-height: 160mm;
        object-fit: contain;
        display: block;
    }
    .caption {
        font-size: 6.8pt;
        color: #64748B;
        padding: 3px;
        background: #0D131E;
        border-top: 1px solid #1E293B;
    }
    
    /* Callout & Legend styling */
    .callout-list {
        list-style: none;
        margin: 4px 0 6px 0;
    }
    .callout-item {
        display: flex;
        align-items: flex-start;
        margin-bottom: 6px;
        font-size: 8.2pt;
        line-height: 1.42;
    }
    .callout-num {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 17px;
        height: 17px;
        border-radius: 50%;
        background: #0284C7;
        color: #FFFFFF;
        font-size: 7.2pt;
        font-weight: 700;
        margin-right: 7px;
        margin-top: 1px;
        flex-shrink: 0;
    }
    .callout-title {
        color: #F1F5F9;
        font-weight: 600;
        margin-right: 4px;
    }
    .callout-desc {
        color: #94A3B8;
    }
    
    /* Step list */
    .step-list {
        margin: 4px 0 6px 14px;
        color: #94A3B8;
        font-size: 8.2pt;
    }
    .step-list li {
        margin-bottom: 3px;
        line-height: 1.42;
    }
    .step-list strong {
        color: #E2E8F0;
    }
    
    /* Notes & Tips */
    .note-box {
        border-left: 3px solid #38BDF8;
        background: #111827;
        border-radius: 0 5px 5px 0;
        padding: 6px 10px;
        margin: 8px 0;
        font-size: 8pt;
        color: #94A3B8;
        line-height: 1.42;
    }
    .note-box strong {
        color: #F1F5F9;
    }
    
    /* Minimal Tables */
    .clean-table {
        width: 100%;
        border-collapse: collapse;
        margin: 6px 0;
        font-size: 8pt;
    }
    .clean-table th {
        text-align: left;
        color: #CBD5E1;
        font-weight: 600;
        padding: 4px 6px;
        border-bottom: 1px solid #334155;
        background: #0F172A;
    }
    .clean-table td {
        padding: 4px 6px;
        color: #94A3B8;
        border-bottom: 1px solid #1E293B;
        vertical-align: top;
    }
    .clean-table tr:last-child td {
        border-bottom: none;
    }
    
    /* Cover Page */
    .cover-wrap {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        padding: 24mm 10mm 15mm 10mm;
        text-align: center;
        background: radial-gradient(circle at 50% 25%, #172033 0%, #0B0F17 75%);
        border: 1px solid #1E293B;
        border-radius: 12px;
    }
    .cover-badge {
        display: inline-block;
        padding: 4px 14px;
        background: rgba(56, 189, 248, 0.12);
        color: #38BDF8;
        border: 1px solid rgba(56, 189, 248, 0.3);
        border-radius: 20px;
        font-size: 8.5pt;
        font-weight: 600;
        letter-spacing: 1px;
        text-transform: uppercase;
        margin-bottom: 24px;
    }
    .cover-title {
        font-size: 34pt;
        font-weight: 800;
        color: #FFFFFF;
        letter-spacing: -0.5px;
        margin-bottom: 8px;
    }
    .cover-subtitle {
        font-size: 13pt;
        color: #94A3B8;
        font-weight: 400;
        max-width: 130mm;
        margin: 0 auto 30px auto;
        line-height: 1.5;
    }
    .cover-meta {
        font-size: 8.5pt;
        color: #64748B;
        line-height: 1.6;
    }
    .cover-meta strong {
        color: #CBD5E1;
    }
    
    /* Q&A layout for Troubleshooting */
    .qa-group {
        margin-bottom: 10px;
    }
    .qa-question {
        color: #F1F5F9;
        font-weight: 700;
        font-size: 8.5pt;
        margin-bottom: 2px;
    }
    .qa-answer {
        color: #94A3B8;
        font-size: 8.2pt;
        line-height: 1.42;
    }
    </style>
    """

    pages = []

    # ==========================================
    # PAGE 1: COVER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-cover">
        <div class="cover-wrap">
            <div>
                <div class="cover-badge">Dokumentasi Resmi Produk</div>
                <div style="margin-bottom: 14px;"><img src="{app_icon}" style="width: 76px; height: 76px; border-radius: 18px; box-shadow: 0 8px 24px rgba(0,0,0,0.6);" alt="KumaLift Icon"></div>
                <div class="cover-title">KumaLift</div>
                <div class="cover-subtitle">Buku Panduan Pengguna & Referensi Fitur Lengkap Aplikasi Pencatat Latihan Beban</div>
            </div>
            
            <div style="max-width: 140mm; margin: 0 auto; text-align: left; background: #111827; border: 1px solid #1E293B; border-radius: 8px; padding: 14px 18px;">
                <div style="font-size: 9pt; color: #E2E8F0; font-weight: 700; margin-bottom: 6px;">Tentang Dokumen Ini</div>
                <div style="font-size: 8.2pt; color: #94A3B8; line-height: 1.5;">
                    Panduan ini disusun untuk memberikan instruksi penggunaan yang jelas, praktis, dan sistematis mengenai seluruh fitur KumaLift versi 1.0.0. Setiap penjelasan disertai screenshot aktual dan penanda visual untuk mempermudah navigasi dan pemahaman antarmuka.
                </div>
            </div>

            <div class="cover-meta">
                <div>Versi Aplikasi: <strong>1.0.0 (Build Produksi)</strong></div>
                <div>Format Distribusi: <strong>Android APK (Offline-First SQLite)</strong></div>
                <div>Tanggal Pembaruan: <strong>November 2026</strong></div>
                <div style="margin-top: 8px; color: #475569;">© 2026 BearBones Development. Hak cipta dilindungi undang-undang.</div>
            </div>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 2: TABLE OF CONTENTS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-toc">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Daftar Isi Dokumen</span>
            <span>Halaman 2</span>
        </div>
        <div class="content">
            <h1>Daftar Isi</h1>
            <p class="lead">Struktur navigasi panduan pengguna KumaLift edisi Bahasa Indonesia.</p>

            <table class="clean-table" style="margin-top: 4px;">
                <thead>
                    <tr>
                        <th style="width: 55px;">Bab</th>
                        <th>Topik & Pembahasan Fitur</th>
                        <th style="width: 75px; text-align: right;">Halaman</th>
                    </tr>
                </thead>
                <tbody>
                    <tr><td>Bab 1</td><td><strong>Pendahuluan & Filosofi Desain:</strong> Tujuan aplikasi, privasi, dan prinsip zero-default.</td><td style="text-align: right;">Halaman 3</td></tr>
                    <tr><td>Bab 2</td><td><strong>Memulai Aplikasi & Navigasi:</strong> Tampilan Dashboard Home dan bilah navigasi 5 tab.</td><td style="text-align: right;">Halaman 4</td></tr>
                    <tr><td>Bab 3</td><td><strong>Workout Routine: Status Awal:</strong> Tampilan status kosong tanpa template dummy.</td><td style="text-align: right;">Halaman 5</td></tr>
                    <tr><td>Bab 4</td><td><strong>Pembuatan Blueprint Routine:</strong> Merancang nama, split kategori, durasi, dan RPE.</td><td style="text-align: right;">Halaman 6</td></tr>
                    <tr><td>Bab 5</td><td><strong>Manajemen Routine Aktif:</strong> Memulai routine, kartu program, dan filter split.</td><td style="text-align: right;">Halaman 7</td></tr>
                    <tr><td>Bab 6</td><td><strong>Katalog Gerakan Latihan:</strong> Perpustakaan 1.324+ gerakan dan filter multi-tier.</td><td style="text-align: right;">Halaman 8</td></tr>
                    <tr><td>Bab 7</td><td><strong>Detail Gerakan & Panduan:</strong> Animasi peraga gerakan, target anatomi, dan PR.</td><td style="text-align: right;">Halaman 9</td></tr>
                    <tr><td>Bab 8</td><td><strong>Penambahan Gerakan Kustom:</strong> Mendaftarkan latihan baru ke basis data lokal.</td><td style="text-align: right;">Halaman 10</td></tr>
                    <tr><td>Bab 9</td><td><strong>Pencatatan Latihan Aktif (Logger):</strong> Pencatatan set, beban, repetisi, dan rest timer.</td><td style="text-align: right;">Halaman 11</td></tr>
                    <tr><td>Bab 10</td><td><strong>Input Beban (KG) & Repetisi:</strong> Dialog input angka bebas dan nilai desimal.</td><td style="text-align: right;">Halaman 12</td></tr>
                    <tr><td>Bab 11</td><td><strong>Ringkasan Sesi & Evaluasi:</strong> Volume tonase, durasi bersih, dan selebrasi PR.</td><td style="text-align: right;">Halaman 13</td></tr>
                    <tr><td>Bab 12</td><td><strong>Analisis Progres & Estimasi 1RM:</strong> Kurva kekuatan dan diagram volume mingguan.</td><td style="text-align: right;">Halaman 14</td></tr>
                    <tr><td>Bab 13</td><td><strong>Profil Atlet & Statistik Latihan:</strong> Metrik seumur hidup dan navigasi profil.</td><td style="text-align: right;">Halaman 15</td></tr>
                    <tr><td>Bab 14</td><td><strong>Pengukuran Tubuh (Measures):</strong> Catatan berat dan tinggi badan tanpa dummy.</td><td style="text-align: right;">Halaman 16</td></tr>
                    <tr><td>Bab 15</td><td><strong>Pengaturan Aplikasi:</strong> Satuan (kg/lbs), tema visual OLED, dan bahasa.</td><td style="text-align: right;">Halaman 17</td></tr>
                    <tr><td>Bab 16</td><td><strong>Pengingat Latihan Harian:</strong> Alarm presisi dan pengaturan hari aktif.</td><td style="text-align: right;">Halaman 18</td></tr>
                    <tr><td>Bab 17</td><td><strong>Keypad Pengingat Waktu Kustom:</strong> Penentuan jam dan menit spesifik bebas.</td><td style="text-align: right;">Halaman 19</td></tr>
                    <tr><td>Bab 18</td><td><strong>Pencadangan & Keamanan Data:</strong> Ekspor JSON/CSV dan aturan cooldown 7 hari.</td><td style="text-align: right;">Halaman 20</td></tr>
                    <tr><td>Bab 19</td><td><strong>Alur Praktis Penggunaan di Gym:</strong> Skenario alur latihan nyata dari awal hingga selesai.</td><td style="text-align: right;">Halaman 21</td></tr>
                    <tr><td>Bab 20</td><td><strong>Pemecahan Masalah & FAQ:</strong> Izin baterai Android, timer latar belakang, dan backup.</td><td style="text-align: right;">Halaman 22</td></tr>
                    <tr><td>Bab 21</td><td><strong>Glosarium Istilah & Panduan RPE:</strong> Tabel intensitas Borg 1–10 dan istilah fitness.</td><td style="text-align: right;">Halaman 23</td></tr>
                    <tr><td>Bab 22</td><td><strong>Spesifikasi Teknis & Rilis:</strong> Kebutuhan OS, arsitektur SQLite, dan lisensi.</td><td style="text-align: right;">Halaman 24</td></tr>
                </tbody>
            </table>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 2</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 3: INTRODUCTION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-intro">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 1 — Pendahuluan</span>
            <span>Halaman 3</span>
        </div>
        <div class="content">
            <h1>Bab 1: Pendahuluan & Filosofi Desain</h1>
            <p class="lead">KumaLift adalah aplikasi pelacak latihan beban yang dirancang untuk membantu atlet memantau prinsip progressive overload tanpa distraksi.</p>

            <div class="grid-2col" style="margin-top: 8px;">
                <div>
                    <h2>Tujuan Aplikasi</h2>
                    <p>Dalam latihan beban (strength training dan hypertrophy), mencatat beban, repetisi, dan intensitas setiap sesi latihan adalah kunci utama untuk memastikan tubuh terus beradaptasi. Seringkali, aplikasi fitness komersial dipenuhi iklan, fitur sosial yang tidak dibutuhkan, dan sistem langganan yang membebani.</p>
                    <p>KumaLift diciptakan untuk menyelesaikan masalah tersebut: aplikasi yang cepat dibuka di ruang gym, tidak membutuhkan koneksi internet, dan memberikan kendali penuh pada pengguna untuk menyusun rutinitas latihan personal.</p>
                    
                    <h3>Siapa yang Membutuhkan KumaLift?</h3>
                    <ul class="step-list">
                        <li><strong>Penggiat Gym Mandiri:</strong> Pengguna yang membutuhkan alat pencatat set, beban (KG), dan repetisi yang responsif saat berlatih.</li>
                        <li><strong>Atlet Hypertrophy & Powerlifting:</strong> Pengguna yang memantau kurva beban, estimasi 1RM, dan kepatuhan program mingguan secara akurat.</li>
                        <li><strong>Pengguna yang Mengutamakan Privasi:</strong> Seluruh data riwayat tersimpan lokal di perangkat dan tidak diunggah ke server pihak ketiga.</li>
                    </ul>
                </div>

                <div>
                    <h2>Prinsip Desain Utama</h2>
                    
                    <h3>1. Zero-Default Clutter</h3>
                    <p>Aplikasi tidak memaksakan template bawaan atau data ukuran tubuh dummy. Bagian Workout Routine dan Measures dimulai dalam kondisi bersih sehingga Anda bebas mengisi program sesuai kebutuhan aktual Anda.</p>

                    <h3>2. Input Bebas Tanpa Batasan Kelipatan</h3>
                    <p>Setiap input angka—baik berat beban (KG), repetisi, maupun waktu pengingat—dapat diisi angka apa pun secara spesifik tanpa dibatasi kelipatan 5 atau preset kaku.</p>

                    <h3>3. Offline-First & Privasi Mutlak</h3>
                    <p>Basis data aplikasi menggunakan engine SQLite lokal. Anda dapat mengekspor seluruh basis data ke dalam file JSON atau spreadsheet CSV kapan saja untuk cadangan mandiri.</p>

                    <div class="note-box" style="margin-top: 10px;">
                        <strong>Catatan Privasi:</strong> KumaLift tidak memerlukan pendaftaran akun, login email, atau izin pelacakan lokasi. Aplikasi dapat berjalan penuh dalam mode pesawat (airplane mode).
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 3</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 4: DASHBOARD
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-dashboard">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 2 — Memulai & Navigasi</span>
            <span>Halaman 4</span>
        </div>
        <div class="content">
            <h1>Bab 2: Memulai Aplikasi & Navigasi Utama</h1>
            <p class="lead">Mengenal antarmuka utama aplikasi dan struktur navigasi untuk penggunaan sehari-hari.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['01']}" alt="Dashboard Home KumaLift">
                        <div class="caption">Gambar 1: Layar Utama (Dashboard Home)</div>
                    </div>
                </div>
                <div>
                    <h2>Layar Utama (Home Dashboard)</h2>
                    <p>Saat aplikasi dibuka, Dashboard menyajikan ringkasan status latihan Anda dalam satu pandangan.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Header Status & Metrik Cepat:</span>
                                <span class="callout-desc">Menampilkan fase program aktif, sapaan atlet, total volume minggu berjalan, persentase target, dan jumlah rekor pribadi (PR).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Pelacak Kepatuhan Mingguan:</span>
                                <span class="callout-desc">Indikator 7 hari (Senin–Minggu) yang otomatis tercentang hijau setiap kali sesi latihan diselesaikan pada hari bersangkutan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Kartu Latihan Hari Ini & Tombol Start:</span>
                                <span class="callout-desc">Menampilkan rekomendasi routine terjadwal. Ketuk tombol hijau besar "START WORKOUT" untuk langsung memulai pencatatan latihan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Statistik Seumur Hidup:</span>
                                <span class="callout-desc">Tiga metrik kumulatif: Total Sessions (jumlah sesi selesai), Total Volume (tonase beban terangkat), dan Records (PR baru).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Bilah Navigasi Bawah:</span>
                                <span class="callout-desc">Akses cepat ke 5 tab utama aplikasi: Home, Workouts, Exercises, Progress, dan Profile.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Izin Notifikasi:</strong> Pada pembukaan pertama kali di Android 13+, sistem akan meminta izin notifikasi agar pengatur waktu istirahat dan pengingat latihan dapat memberi sinyal saat aplikasi di latar belakang.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 4</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 5: ROUTINES EMPTY STATE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-empty">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 3 — Workout Routine Status Awal</span>
            <span>Halaman 5</span>
        </div>
        <div class="content">
            <h1>Bab 3: Workout Routine — Status Awal</h1>
            <p class="lead">KumaLift menerapkan sistem tanpa template bawaan agar setiap pengguna menyusun rutinitas latihan personal dari nol.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['02']}" alt="Empty State Workout Routines">
                        <div class="caption">Gambar 2: Tampilan Status Kosong Tab Workouts</div>
                    </div>
                </div>
                <div>
                    <h2>Tampilan Status Kosong (Empty State)</h2>
                    <p>Ketika aplikasi baru dipasang atau jika seluruh routine lama telah dihapus, tab Workouts menyajikan antarmuka bersih yang memandu Anda untuk membuat routine pertama.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Judul Layar:</span>
                                <span class="callout-desc">Teks "Workout Routines" sebagai penanda tab manajemen pola latihan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Tombol Cepat Top Bar:</span>
                                <span class="callout-desc">Tombol "Create Routine" di sudut kanan atas untuk langsung membuka formulir pembuatan routine baru.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Ilustrasi & Petunjuk Status:</span>
                                <span class="callout-desc">Ikon barbel dan pesan informatif yang menegaskan bahwa belum ada routine yang tersimpan. Elemen kartu program dan filter kategori disembunyikan agar tampilan tetap rapi.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol Utama Pembuatan Routine:</span>
                                <span class="callout-desc">Tombol aksi utama (CTA) hijau terang di tengah layar untuk mulai menyusun rutinitas pertama Anda.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Mengapa Dimulai Dari Nol?</h3>
                    <p>Setiap orang memiliki ketersediaan alat yang berbeda di tempat latihannya. Menghapus template bawaan mencegah daftar latihan dipenuhi program yang tidak relevan dengan fasilitas gym Anda.</p>

                    <div class="note-box">
                        <strong>Perilaku Otomatis:</strong> Kartu target program (Training Program) dan bilah filter kategori akan otomatis muncul segera setelah Anda menyimpan minimal satu workout routine.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 5</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 6: CREATE ROUTINE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-create-routine">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 4 — Pembuatan Routine</span>
            <span>Halaman 6</span>
        </div>
        <div class="content">
            <h1>Bab 4: Pembuatan Blueprint Routine</h1>
            <p class="lead">Langkah-langkah menyusun cetak biru (blueprint) latihan yang terstruktur dan mudah diakses.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['03']}" alt="Layar Pembuatan Routine">
                        <div class="caption">Gambar 3: Formulir Perancangan Routine</div>
                    </div>
                </div>
                <div>
                    <h2>Formulir Perancangan (Create Routine)</h2>
                    <p>Gunakan layar ini untuk menentukan parameter latihan sebelum menyusun daftar gerakan.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Tombol Simpan (Save):</span>
                                <span class="callout-desc">Menyimpan rancangan routine ke penyimpanan basis data lokal perangkat.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Nama Routine:</span>
                                <span class="callout-desc">Ketik nama sesi latihan spesifik, misalnya "Push Day Dada Fokus" atau "Leg Day Quads".</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Kategori Split:</span>
                                <span class="callout-desc">Pilih kategori organisasi: PPL Split, Upper / Lower, Full Body, atau buat kategori kustom sendiri.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Estimasi Durasi & Target RPE:</span>
                                <span class="callout-desc">Tentukan estimasi waktu latihan (30–90 menit) dan target intensitas RPE (misal: Heavy, RPE 8–9).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Tombol Tambah Gerakan ("Add Exercise"):</span>
                                <span class="callout-desc">Membuka katalog latihan untuk memilih dan menyusun urutan gerakan ke dalam routine ini.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Deteksi Fokus Otot Otomatis:</strong> Saat Anda memasukkan latihan ke dalam formulir, KumaLift secara otomatis menganalisis kelompok otot primer dan merangkumnya menjadi teks fokus otot (misal: "Dada • 60 min • Berat, RPE 8–9").
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 6</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 7: ROUTINES LIST
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-list">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 5 — Manajemen Routine Aktif</span>
            <span>Halaman 7</span>
        </div>
        <div class="content">
            <h1>Bab 5: Manajemen Routine Aktif</h1>
            <p class="lead">Memantau kepatuhan program mingguan dan meluncurkan sesi latihan dari daftar routine yang tersimpan.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['04']}" alt="Daftar Routine Aktif">
                        <div class="caption">Gambar 4: Tampilan Daftar Routine yang Tersimpan</div>
                    </div>
                </div>
                <div>
                    <h2>Daftar Routine & Kartu Program</h2>
                    <p>Setelah Anda menyimpan minimal satu routine, tab Workouts akan menampilkan kartu kepatuhan mingguan dan kartu routine aktif.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Kartu Program Latihan:</span>
                                <span class="callout-desc">Menampilkan nama program aktif, rasio target mingguan (misal: 3 / 4 this week), dan persentase kepatuhan mingguan (Weekly Adherence).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Chip Filter Kategori:</span>
                                <span class="callout-desc">Bilah filter horizontal (misal: All Programs, PPL Split) untuk memilah kartu latihan dengan satu ketukan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Wadah Kartu Routine:</span>
                                <span class="callout-desc">Memuat nama routine, ringkasan fokus otot, durasi, target intensitas RPE, pratinjau chip gerakan, dan ikon tempat sampah untuk menghapus routine.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol "Start Routine":</span>
                                <span class="callout-desc">Memulai sesi latihan aktif langsung menggunakan susunan latihan dalam kartu ini. Timer latihan dimulai saat tombol ini ditekan.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Penghapusan Aman:</strong> Menghapus routine dari daftar ini tidak akan menghapus riwayat sesi latihan lama yang pernah Anda selesaikan menggunakan routine tersebut.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 7</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 8: EXERCISE CATALOG
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-catalog">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 6 — Katalog Gerakan</span>
            <span>Halaman 8</span>
        </div>
        <div class="content">
            <h1>Bab 6: Katalog Gerakan Latihan</h1>
            <p class="lead">KumaLift dilengkapi basis data internal berisi lebih dari 1.324 gerakan latihan dengan filter multi-dimensi.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['05']}" alt="Katalog Gerakan KumaLift">
                        <div class="caption">Gambar 5: Layar Katalog Gerakan & Sistem Pencarian</div>
                    </div>
                </div>
                <div>
                    <h2>Pencarian & Penyaringan Gerakan</h2>
                    <p>Navigasikan tab <strong>Exercises</strong> pada bilah bawah untuk menjelajahi seluruh perpustakaan gerakan latihan.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Tombol Buat Gerakan ("Create"):</span>
                                <span class="callout-desc">Membuka formulir untuk menambahkan variasi latihan kustom jika gerakan yang Anda inginkan belum ada di basis data.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Kolom Pencarian Cepat:</span>
                                <span class="callout-desc">Ketik nama gerakan dalam bahasa Inggris atau Indonesia (misal: "Bench Press", "Squat", "Curl") untuk filter real-time.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Baris Filter Multi-Kategori:</span>
                                <span class="callout-desc">Tiga baris filter horizontal independen:
                                    <ul style="margin: 3px 0 0 14px;">
                                        <li><strong>Kelompok Otot:</strong> Chest, Back, Legs, Shoulders, Arms, Core.</li>
                                        <li><strong>Peralatan:</strong> Barbell, Dumbbell, Cable, Bodyweight, Machine.</li>
                                        <li><strong>Jenis Gerakan:</strong> Compound (majemuk) atau Isolation (isolasi).</li>
                                    </ul>
                                </span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Item Daftar Gerakan:</span>
                                <span class="callout-desc">Menampilkan thumbnail visual, nama gerakan, dan anatomi otot target. Ketuk kartu untuk melihat panduan eksekusi dan rekor.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Pencarian Offline Cepat:</strong> Basis data gerakan dioptimalkan secara lokal sehingga penyaringan teks berjalan instan tanpa lag dan tanpa membutuhkan koneksi internet.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 8</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 9: EXERCISE DETAIL
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-exercise-detail">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 7 — Detail Gerakan</span>
            <span>Halaman 9</span>
        </div>
        <div class="content">
            <h1>Bab 7: Detail Gerakan & Panduan Eksekusi</h1>
            <p class="lead">Peragaan teknik visual dengan kontrol animasi dan rekam jejak rekor pribadi untuk setiap gerakan.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['06']}" alt="Detail Gerakan Latihan">
                        <div class="caption">Gambar 6: Tampilan Panduan Detail Gerakan</div>
                    </div>
                </div>
                <div>
                    <h2>Panduan Visual & Catatan Rekor</h2>
                    <p>Mengetuk salah satu gerakan pada katalog akan membuka halaman panduan komprehensif ini.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Animasi Peragaan Gerakan:</span>
                                <span class="callout-desc">Demonstrasi visual animasi looping yang memperlihatkan rentang gerak penuh (range of motion) dengan tombol pause/play.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Target Anatomi & Otot Penopang:</span>
                                <span class="callout-desc">Menjelaskan kelompok otot primer yang menjadi sasaran utama dan otot sekunder yang menstabilkan gerakan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Petunjuk Teknis ("How to"):</span>
                                <span class="callout-desc">Tab instruksi langkah demi langkah posisi tubuh, titik tumpu, teknik tarikan/dorongan, dan pola pernapasan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Rekor Pribadi (Personal Records):</span>
                                <span class="callout-desc">Catatan prestasi beban terberat (Heaviest Weight), estimasi One-Rep Max (1RM), dan volume set terbaik yang pernah Anda capai pada latihan ini.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Tips Keamanan:</strong> Selalu periksa petunjuk teknik gerakan sebelum menaikkan beban kerja untuk menjaga postur tubuh yang benar dan mencegah cedera sendi.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 9</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 10: CUSTOM EXERCISE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-custom-exercise">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 8 — Gerakan Kustom</span>
            <span>Halaman 10</span>
        </div>
        <div class="content">
            <h1>Bab 8: Penambahan Gerakan Kustom</h1>
            <p class="lead">Mendaftarkan variasi latihan baru yang belum tersedia di katalog bawaan ke dalam database pribadi.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['06b']}" alt="Formulir Buat Latihan Kustom">
                        <div class="caption">Gambar 7: Formulir Penambahan Gerakan Kustom</div>
                    </div>
                </div>
                <div>
                    <h2>Menambah Variasi Latihan Sendiri</h2>
                    <p>Ketuk tombol <strong>Create</strong> di sudut kanan atas tab Exercises untuk membuka formulir ini.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Tombol Simpan (Save):</span>
                                <span class="callout-desc">Menyimpan gerakan kustom ke basis data lokal perangkat sehingga dapat langsung digunakan di Workout Log.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Nama Latihan:</span>
                                <span class="callout-desc">Ketik nama gerakan yang jelas dan mudah dikenali (misal: "Incline Cable Fly" atau "Bulgarian Split Squat Dumbbell").</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Pilihan Peralatan (Equipment):</span>
                                <span class="callout-desc">Pilih jenis alat yang digunakan: Barbell, Dumbbell, Cable, Machine, Bodyweight, atau Resistance Band.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Kelompok Otot Target:</span>
                                <span class="callout-desc">Tentukan otot primer (Primary Muscle Group) dan otot pendukung opsional (Other Muscles) agar analitik distribusi beban tercatat akurat.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Integrasi Penuh:</strong> Gerakan kustom yang Anda buat memiliki fitur yang sama persis dengan gerakan bawaan: dapat disisipkan ke dalam routine, dilacak rekor PR-nya, dan dihitung kurva 1RM-nya.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 10</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 11: WORKOUT LOGGER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-logger">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 9 — Workout Log Aktif</span>
            <span>Halaman 11</span>
        </div>
        <div class="content">
            <h1>Bab 9: Pencatatan Latihan Aktif & Timer</h1>
            <p class="lead">Antarmuka utama saat berlatih di gym untuk mencatat beban, repetisi, dan mengatur waktu istirahat.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['08']}" alt="Active Workout Logger">
                        <div class="caption">Gambar 8: Layar Sesi Latihan Aktif & Rest Timer</div>
                    </div>
                </div>
                <div>
                    <h2>Komponen Workout Log Aktif</h2>
                    <p>Dirancang untuk pengoperasian cepat satu tangan di antara set latihan.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Header Sesi & Timer Berjalan:</span>
                                <span class="callout-desc">Menampilkan nama workout, timer durasi aktif (format MM:SS yang hanya berjalan saat latihan dimulai), dan bilah capaian target set.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Kolom Input Beban (KG) & Reps:</span>
                                <span class="callout-desc">Kolom angka yang sejajar rapi dengan header tabel (Set, Prev, KG, Reps, RPE, Check). Ketuk angka untuk membuka dialog custom input.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Tombol Centang Set (Check):</span>
                                <span class="callout-desc">Ketuk untuk menandai set telah diselesaikan. Tombol akan berubah menjadi hijau terang dan otomatis menyalakan Rest Timer.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol Tambah Gerakan ("Add Exercise"):</span>
                                <span class="callout-desc">Menyisipkan latihan tambahan sewaktu-waktu ke dalam sesi aktif tanpa merusak struktur routine asli.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Bilah Pengatur Waktu Istirahat (Rest Timer):</span>
                                <span class="callout-desc">Panel melayang di bagian bawah dengan countdown detik, tombol cepat -15s / +30s, dan tombol tutup (X).</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Ketelitian Waktu Latihan:</strong> Durasi latihan dihitung berdasarkan timestamp absolut saat tombol Start ditekan, bukan saat template dipilih di menu awal.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 11</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 12: CUSTOM KG & REPS INPUT
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-custom-inputs">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 10 — Input Beban & Repetisi</span>
            <span>Halaman 12</span>
        </div>
        <div class="content">
            <h1>Bab 10: Input Beban (KG) & Repetisi Fleksibel</h1>
            <p class="lead">Dialog pengisian angka bebas tanpa batasan kelipatan kaku, mendukung nilai desimal akurat.</p>

            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['09']}" alt="Dialog Edit Weight">
                        <div class="caption">Gambar 9: Dialog Input Berat Beban (KG)</div>
                    </div>
                    <h3 style="margin-top: 6px;">Pengisian Berat Beban (KG)</h3>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div><span class="callout-title">Kolom Angka Bebas:</span> <span class="callout-desc">Mendukung angka bulat maupun desimal (contoh: <code>32.5</code>, <code>17.25</code>, <code>67.5</code>). Rentang valid 0.1 hingga 999.9 kg.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div><span class="callout-title">Tombol Penyesuaian Cepat:</span> <span class="callout-desc">Empat tombol praktis (<code>-5</code>, <code>-1</code>, <code>+1</code>, <code>+5</code>) untuk menaikkan atau menurunkan beban dengan satu ketukan.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div><span class="callout-title">Tombol Simpan:</span> <span class="callout-desc">Menerapkan bobot ke baris set aktif dan memperbarui catatan latihan.</span></div>
                        </li>
                    </ul>
                </div>

                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['10']}" alt="Dialog Edit Reps">
                        <div class="caption">Gambar 10: Dialog Input Jumlah Repetisi</div>
                    </div>
                    <h3 style="margin-top: 6px;">Pengisian Jumlah Repetisi</h3>
                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div><span class="callout-title">Kolom Repetisi Mandiri:</span> <span class="callout-desc">Masukkan jumlah repetisi yang berhasil Anda eksekusi (1 hingga 999 repetisi) secara fleksibel tanpa batasan angka tertentu.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div><span class="callout-title">Tombol Stepper Cepat:</span> <span class="callout-desc">Gunakan tombol pintas untuk menambah atau mengurangi repetisi tanpa perlu mengetik ulang pada keyboard.</span></div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div><span class="callout-title">Tombol Simpan:</span> <span class="callout-desc">Menerapkan jumlah repetisi ke dalam catatan latihan secara instan.</span></div>
                        </li>
                    </ul>
                </div>
            </div>

            <div class="note-box" style="margin-top: 8px;">
                <strong>Pentingnya Input Desimal:</strong> Dalam latihan progressive overload modern, penambahan beban mikro (misal menggunakan microplates 1.25 kg atau 0.5 kg) sangat menentukan keberlanjutan progres tanpa memicu cedera otot.
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 12</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 13: WORKOUT SUMMARY
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-summary">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 11 — Ringkasan Sesi</span>
            <span>Halaman 13</span>
        </div>
        <div class="content">
            <h1>Bab 11: Ringkasan Sesi & Evaluasi Latihan</h1>
            <p class="lead">Tinjau seluruh pencapaian volume beban, durasi efektif, dan selebrasi rekor pribadi baru setelah sesi selesai.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['12']}" alt="Workout Summary Screen">
                        <div class="caption">Gambar 11: Layar Evaluasi Ringkasan Sesi Latihan</div>
                    </div>
                </div>
                <div>
                    <h2>Evaluasi Sesi Latihan</h2>
                    <p>Setelah menekan tombol <strong>Finish</strong> pada bilah atas, Anda akan diarahkan ke layar ringkasan ini sebelum data disimpan permanen.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Judul Latihan & Rating RPE:</span>
                                <span class="callout-desc">Menampilkan nama workout, rentang jam latihan, dan skor tingkat kesulitan subjektif (RPE) sesi tersebut.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Matriks Kinerja 4 Segmen:</span>
                                <span class="callout-desc">
                                    <ul style="margin: 3px 0 0 14px;">
                                        <li><strong>Duration:</strong> Durasi aktif bersih selama sesi berlangsung.</li>
                                        <li><strong>Total Volume:</strong> Total tonase beban (KG × Reps dari seluruh set).</li>
                                        <li><strong>Completed Sets:</strong> Rasio set yang diselesaikan terhadap target.</li>
                                        <li><strong>Target Rest:</strong> Rata-rata interval waktu istirahat antarsesi.</li>
                                    </ul>
                                </span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Distribusi Fokus Otot (Muscle Focus):</span>
                                <span class="callout-desc">Bilah grafis persentase kelompok anatomi tubuh yang dilatih (misal: Core: 100% atau Chest: 60%, Triceps: 40%).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol "Save & Finish Session":</span>
                                <span class="callout-desc">Menyimpan sesi ke database SQLite lokal, memperbarui grafik analitik progres, dan mencatat rekor pribadi baru (jika ada).</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Deteksi Rekor Otomatis (PR):</strong> Jika pada salah satu set Anda mengangkat beban terberat atau repetisi tertinggi dalam sejarah latihan gerakan tersebut, KumaLift akan menandainya sebagai Personal Record (PR).
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 13</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 14: PROGRESS ANALYTICS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-analytics">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 12 — Analisis Progres</span>
            <span>Halaman 14</span>
        </div>
        <div class="content">
            <h1>Bab 12: Analisis Progres & Estimasi 1RM</h1>
            <p class="lead">Visualisasi perkembangan kekuatan dan tonase volume latihan jangka panjang melalui diagram interaktif.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['13']}" alt="Progress and Analytics Screen">
                        <div class="caption">Gambar 12: Layar Visualisasi Progres & Kurva 1RM</div>
                    </div>
                </div>
                <div>
                    <h2>Pemantauan Progres Performa</h2>
                    <p>Buka tab <strong>Progress</strong> pada bilah bawah untuk melihat visualisasi performa latihan Anda dari waktu ke waktu.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Filter Rentang Waktu (Horizon Filter):</span>
                                <span class="callout-desc">Pilihan rentang analisis: 1M (1 Bulan), 3M (3 Bulan), 6M (6 Bulan), 1Y (1 Tahun), atau ALL (Semua Riwayat).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Kurva Estimasi One-Rep Max (1RM):</span>
                                <span class="callout-desc">Kalkulasi kekuatan maksimal teoritis berdasarkan rumus standar Brzycki/Epley dari set terberat yang berhasil dilakukan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Diagram Batang Volume Mingguan:</span>
                                <span class="callout-desc">Grafik batang yang membandingkan total volume angkatan (tonase dalam KG) antarminggu untuk memantau beban latihan kumulatif.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Linimasa Rekor Pribadi (PR):</span>
                                <span class="callout-desc">Daftar rekor pencapaian terbaru untuk setiap gerakan lengkap dengan bobot, repetisi, estimasi 1RM, dan tanggal pencapaian.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Mengapa 1RM Penting?</strong> Estimasi 1RM memungkinkan Anda mengukur kenaikan kekuatan murni tanpa harus melakukan uji coba angkatan satu repetisi maksimal yang berisiko cedera tinggi.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 14</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 15: PROFILE DASHBOARD
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-profile">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 13 — Profil Atlet</span>
            <span>Halaman 15</span>
        </div>
        <div class="content">
            <h1>Bab 13: Profil Atlet & Statistik Latihan</h1>
            <p class="lead">Dashboard riwayat personal atlet, akumulasi tonase angkatan seumur hidup, dan navigasi data.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['14']}" alt="Profile Dashboard Screen">
                        <div class="caption">Gambar 13: Layar Profil & Ringkasan Performa</div>
                    </div>
                </div>
                <div>
                    <h2>Dashboard Profil Atlet</h2>
                    <p>Tab <strong>Profile</strong> menyajikan ringkasan dedikasi latihan Anda sejak hari pertama memasang aplikasi.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Ikon Pengaturan (Settings Gear):</span>
                                <span class="callout-desc">Ketuk roda gigi di pojok kanan atas untuk membuka konfigurasi sistem, satuan berat, tema, dan penjadwalan alarm.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Statistik Utama Seumur Hidup:</span>
                                <span class="callout-desc">Tiga metrik kumulatif utama: Workouts (total sesi selesai), PR Records (rekor dipecahkan), dan Volume (total tonase beban kg).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Diagram Volume Per Sesi:</span>
                                <span class="callout-desc">Bilah diagram komparasi beban antarsesi yang dapat difilter berdasarkan Volume, Duration, atau Repetisi.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Grid Navigasi Cepat (Dashboard):</span>
                                <span class="callout-desc">Empat ubin jalan pintas: Statistics (analisis beban), Exercises (katalog), Measures (ukuran tubuh), dan Calendar (jadwal latihan).</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Kustomisasi Nama:</strong> Anda dapat mengubah nama panggilan atlet melalui menu Settings &gt; Profile agar aplikasi menyapa Anda secara personal di layar Home.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 15</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 16: BODY MEASUREMENTS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-measures">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 14 — Pengukuran Tubuh</span>
            <span>Halaman 16</span>
        </div>
        <div class="content">
            <h1>Bab 14: Pengukuran Tubuh (Measures)</h1>
            <p class="lead">Pencatatan metrik fisik tubuh dengan kondisi awal bersih tanpa data dummy bawaan.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['15']}" alt="Measures Modal Screen">
                        <div class="caption">Gambar 14: Panel Pencatatan Ukuran Tubuh</div>
                    </div>
                </div>
                <div>
                    <h2>Pencatatan Metrik Tubuh (Measures)</h2>
                    <p>Ketuk ubin <strong>Measures</strong> pada profil untuk membuka panel pengisian ukuran fisik ini.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Judul Panel:</span>
                                <span class="callout-desc">Header "Body Measurements" pada bottom sheet yang dapat digeser (draggable sheet).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Kolom Berat Badan (Bodyweight):</span>
                                <span class="callout-desc">Masukkan berat badan aktual Anda dalam satuan kilogram (kg). Dimulai dalam kondisi bersih (0.0) tanpa data palsu.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Kolom Tinggi Badan (Height):</span>
                                <span class="callout-desc">Masukkan tinggi badan Anda dalam sentimeter (cm) sebagai dasar kalkulasi proporsi tubuh.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol Simpan ("Save Measurements"):</span>
                                <span class="callout-desc">Menyimpan data pengukuran ke basis data lokal perangkat beserta tanggal dan waktu pencatatan.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Kondisi Awal Kosong:</strong> KumaLift menghapus seluruh data default bawaan. Saat pertama kali dibuka, layar menyajikan empty state bersih agar seluruh data yang tercatat 100% murni catatan asli Anda.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 16</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 17: SETTINGS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-settings">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 15 — Pengaturan Aplikasi</span>
            <span>Halaman 17</span>
        </div>
        <div class="content">
            <h1>Bab 15: Pengaturan Aplikasi & Preferensi</h1>
            <p class="lead">Kustomisasi perilaku aplikasi, satuan pengukuran, preferensi tampilan, dan bahasa antarmuka.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['16']}" alt="Settings Screen">
                        <div class="caption">Gambar 15: Menu Utama Pengaturan Aplikasi</div>
                    </div>
                </div>
                <div>
                    <h2>Navigasi Menu Pengaturan</h2>
                    <p>Akses menu ini dengan mengetuk ikon roda gigi pada sudut kanan atas tab <strong>Profile</strong>.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Profil & Akun (Account):</span>
                                <span class="callout-desc">Ubah nama panggilan atlet, fase tujuan program latihan (Hypertrophy, Strength, atau General Fitness), dan target hari latihan per minggu.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Latihan & Pencatatan (Workouts & Logging):</span>
                                <span class="callout-desc">Atur durasi default timer istirahat otomatis (misal: 60 detik, 90 detik, atau 120 detik) serta getaran penanda set selesai.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Satuan & Format (Units & Formatting):</span>
                                <span class="callout-desc">Pilih satuan beban utama antara Kilogram (kg) atau Pounds (lbs). Seluruh data akan otomatis dikonversi dengan akurat.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Bahasa (Language):</span>
                                <span class="callout-desc">Ganti bahasa antarmuka aplikasi secara instan antara <strong>Bahasa Indonesia</strong> dan <strong>English</strong> tanpa perlu memulai ulang aplikasi.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Ekspor & Impor Data:</span>
                                <span class="callout-desc">Akses ke pusat pencadangan basis data, pemulihan backup JSON, dan ekspor spreadsheet CSV.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Sinkronisasi Tema Visual:</strong> KumaLift mendukung mode gelap murni (OLED Dark Mode) yang menghemat baterai saat Anda mencatat latihan di gym dengan layar menyala terus-menerus.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 17</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 18: NOTIFICATIONS MENU
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-notifications">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 16 — Pengingat Latihan Harian</span>
            <span>Halaman 18</span>
        </div>
        <div class="content">
            <h1>Bab 16: Pengingat Latihan Harian</h1>
            <p class="lead">Konfigurasi jadwal pengingat latihan dengan sistem alarm presisi dan toleransi keterlambatan maksimal 1 menit.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['17']}" alt="Notifications Screen">
                        <div class="caption">Gambar 16: Menu Pengaturan Notifikasi & Pengingat</div>
                    </div>
                </div>
                <div>
                    <h2>Pengaturan Pengingat Harian</h2>
                    <p>Buka menu <strong>Settings &gt; Notifications</strong> untuk mengelola seluruh perilaku pengingat aplikasi.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Alarm Rest Timer:</span>
                                <span class="callout-desc">Toggle pemberitahuan saat hitung mundur istirahat selesai saat layar mati atau saat aplikasi berada di latar belakang.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Master Switch Pengingat Harian:</span>
                                <span class="callout-desc">Saklar utama untuk mengaktifkan pengingat latihan terjadwal (Daily Workout Reminder).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Pilihan Jam Preset:</span>
                                <span class="callout-desc">Pilihan jam cepat: 06:00, 07:00, 17:00, 18:30, 20:00 untuk penjadwalan instan.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Tombol Kustom Jam ("Custom..."):</span>
                                <span class="callout-desc">Membuka dialog keypad untuk menentukan jam dan menit latihan secara bebas dan spesifik.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Pemilihan Hari Latihan Aktif:</span>
                                <span class="callout-desc">Tujuh lingkaran hari (Senin–Minggu) untuk menentukan hari apa saja alarm pengingat berbunyi.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Presisi Waktu Android:</strong> KumaLift menggunakan mekanisme scheduling sistem operasi paling akurat dengan toleransi keterlambatan maksimal 1 menit dan proteksi anti-duplicate alarm.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 18</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 19: CUSTOM REMINDER KEYPAD
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-reminder-keypad">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 17 — Keypad Jam Kustom</span>
            <span>Halaman 19</span>
        </div>
        <div class="content">
            <h1>Bab 17: Keypad Pengingat Waktu Kustom</h1>
            <p class="lead">Desain dialog keypad modern untuk menentukan jam dan menit alarm secara bebas tanpa kelipatan 5 menit.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['18']}" alt="Custom Reminder Dialog">
                        <div class="caption">Gambar 17: Keypad Pemilihan Jam Kustom</div>
                    </div>
                </div>
                <div>
                    <h2>Keypad Pemilihan Waktu Kustom</h2>
                    <p>Ketuk tombol <strong>Custom...</strong> pada baris Reminder Time untuk membuka dialog interaktif ini.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Indikator Digital Jam & Menit:</span>
                                <span class="callout-desc">Dua kotak angka yang menampilkan jam (00–23) dan menit (00–59) yang sedang aktif dipilih.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Keypad Numerik Modern:</span>
                                <span class="callout-desc">Gunakan tombol 0–9, tombol 00, dan tombol hapus (backspace) untuk memasukkan waktu spesifik apa pun seperti <code>08:03</code>, <code>17:15</code>, atau <code>21:42</code>.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Tombol Simpan (Save):</span>
                                <span class="callout-desc">Menyimpan waktu yang dipilih dan mendaftarkannya ke AlarmManager perangkat Android.</span>
                            </div>
                        </li>
                    </ul>

                    <h3>Variasi Pesan Notifikasi Harian</h3>
                    <p>Sistem KumaLift menyertakan 5 variasi pesan pengingat yang dipilih secara acak setiap hari dalam bahasa aktif Anda (Indonesia/Inggris):</p>
                    <ul class="step-list">
                        <li>"Waktunya latihan! Bangun konsistensi dan capai targetmu hari ini."</li>
                        <li>"Tubuhmu siap berkembang. Jangan lewatkan sesi latihan hari ini!"</li>
                        <li>"Disiplin adalah kunci hasil nyata. Yuk, selesaikan workout-mu!"</li>
                        <li>"Setiap set membawamu lebih dekat ke target. Gas latihan sekarang!"</li>
                        <li>"Jadwal latihan sudah tiba! Luangkan waktu untuk dirimu hari ini."</li>
                    </ul>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 19</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 20: BACKUP MANAGEMENT
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-backup">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 18 — Manajemen Data & Backup</span>
            <span>Halaman 20</span>
        </div>
        <div class="content">
            <h1>Bab 18: Pencadangan & Keamanan Data</h1>
            <p class="lead">Ekspor riwayat latihan, pemulihan cadangan data, dan mekanisme kebijakan pengingat backup 7 hari.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-frame">
                        <img src="{imgs['19']}" alt="Backup and Export Screen">
                        <div class="caption">Gambar 18: Layar Ekspor, Impor, & Cadangan Data</div>
                    </div>
                </div>
                <div>
                    <h2>Pusat Pengelolaan Data Lokal</h2>
                    <p>Karena seluruh data disimpan di penyimpanan lokal perangkat Anda, menjaga file cadangan secara berkala sangat disarankan.</p>

                    <ul class="callout-list">
                        <li class="callout-item">
                            <span class="callout-num">1</span>
                            <div>
                                <span class="callout-title">Ekspor Cadangan Lengkap (JSON):</span>
                                <span class="callout-desc">Menyimpan seluruh basis data—meliputi riwayat workout, set, routine, gerakan kustom, profil, dan rekor—ke dalam satu file JSON terenkripsi schema v1.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">2</span>
                            <div>
                                <span class="callout-title">Pulihkan Data dari Backup:</span>
                                <span class="callout-desc">Memilih file cadangan JSON sebelumnya dengan dua opsi: <em>Merge</em> (menggabungkan tanpa menghapus data baru) atau <em>Replace</em> (menimpa basis data).</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">3</span>
                            <div>
                                <span class="callout-title">Snapshot Keamanan Otomatis:</span>
                                <span class="callout-desc">Sebelum proses restore dijalankan, aplikasi otomatis membuat snapshot darurat untuk mencegah hilangnya data akibat kesalahan file.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">4</span>
                            <div>
                                <span class="callout-title">Ekspor Spreadsheet (CSV):</span>
                                <span class="callout-desc">Menghasilkan file CSV tabel latihan yang dapat dibuka langsung di Microsoft Excel atau Google Sheets untuk analisis mandiri.</span>
                            </div>
                        </li>
                        <li class="callout-item">
                            <span class="callout-num">5</span>
                            <div>
                                <span class="callout-title">Status Penyimpanan & Cooldown 7 Hari:</span>
                                <span class="callout-desc">Menampilkan jumlah data tersimpan dan tanggal backup terakhir. Pengingat backup diatur dengan cooldown 7 hari agar tidak mengganggu penggunaan harian.</span>
                            </div>
                        </li>
                    </ul>

                    <div class="note-box">
                        <strong>Aturan Cooldown Pengingat Backup:</strong> Dialog pengingat backup hanya akan muncul maksimal 1 kali dalam seminggu. Interval ini dihitung dari waktu interaksi terakhir Anda dengan dialog (menekan "Cadangkan Sekarang" atau "Nanti"), dan tetap bertahan setelah aplikasi ditutup maupun perangkat direstart.
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 20</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 21: WORKFLOW
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-workflow">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 19 — Alur Praktis Penggunaan</span>
            <span>Halaman 21</span>
        </div>
        <div class="content">
            <h1>Bab 19: Alur Praktis Penggunaan di Gym</h1>
            <p class="lead">Panduan langkah demi langkah penggunaan aplikasi dari persiapan sebelum latihan hingga sesi selesai.</p>

            <div class="grid-2col" style="margin-top: 8px;">
                <div>
                    <h2>Langkah 1: Persiapan Sebelum Latihan</h2>
                    <ul class="step-list">
                        <li><strong>Buka Tab Workouts:</strong> Pilih routine yang ingin dijalankan hari ini (misal: "Push Day"). Jika belum memiliki routine, buat blueprint baru terlebih dahulu.</li>
                        <li><strong>Ketuk "Start Routine":</strong> Sesi latihan aktif akan segera dibuat. Timer latihan dimulai tepat saat tombol ini ditekan.</li>
                        <li><strong>Opsi Freestyle:</strong> Jika Anda ingin latihan fleksibel tanpa routine, buka Dashboard Home dan ketuk "Start Workout" untuk mode bebas.</li>
                    </ul>

                    <h2>Langkah 2: Saat Latihan Berlangsung</h2>
                    <ul class="step-list">
                        <li><strong>Periksa Beban Set Sebelumnya:</strong> Kolom <code>PREV</code> menampilkan bobot dan repetisi yang Anda lakukan pada sesi terakhir sebagai patokan beban target.</li>
                        <li><strong>Sesuaikan Beban (KG):</strong> Ketuk kolom KG untuk memasukkan angka beban aktual yang dipasang pada barbel atau mesin.</li>
                        <li><strong>Eksekusi Set & Centang:</strong> Setelah menyelesaikan repetisi, ketuk ikon kotak centang (Check). Baris akan berwarna hijau dan Rest Timer otomatis menyala.</li>
                        <li><strong>Manfaatkan Rest Timer:</strong> Beristirahatlah hingga alarm countdown berbunyi. Gunakan tombol +30s jika napas belum pulih optimal.</li>
                    </ul>
                </div>

                <div>
                    <h2>Langkah 3: Menyelesaikan Sesi Latihan</h2>
                    <ul class="step-list">
                        <li><strong>Ketuk Tombol Finish:</strong> Di sudut kanan atas layar Workout Log, tekan tombol "Finish" saat seluruh set selesai.</li>
                        <li><strong>Evaluasi Metrik Sesi:</strong> Periksa ringkasan total volume angkatan (tonase), durasi bersih, dan rasio set selesai.</li>
                        <li><strong>Simpan Sesi:</strong> Tekan "Save & Finish Session". Rekor pribadi baru (PR) akan dihitung otomatis oleh sistem.</li>
                    </ul>

                    <h2>Langkah 4: Evaluasi Berkala Mingguan</h2>
                    <ul class="step-list">
                        <li><strong>Cek Kepatuhan di Home:</strong> Pastikan target hari latihan mingguan Anda (misal 4 hari/minggu) terpenuhi.</li>
                        <li><strong>Tinjau Kurva 1RM di Progress:</strong> Pantau apakah beban angkatan maksimal Anda mengalami kenaikan dalam rentang 1 bulan atau 3 bulan.</li>
                        <li><strong>Catat Pengukuran Tubuh:</strong> Masukkan berat badan aktual Anda secara berkala pada ubin Measures di halaman profil.</li>
                        <li><strong>Cadangkan Data:</strong> Buat file backup JSON secara berkala dan simpan di Google Drive atau penyimpanan awan pribadi Anda.</li>
                    </ul>
                </div>
            </div>

            <div class="note-box" style="margin-top: 10px;">
                <strong>Kiat Progressive Overload:</strong> Jangan memaksakan kenaikan beban besar setiap sesi. Kenaikan 1 repetisi dengan beban yang sama atau kenaikan mikro 0.5–1.0 kg adalah bukti nyata perkembangan adaptasi otot yang aman dan berkelanjutan.
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 21</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 22: TROUBLESHOOTING
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-troubleshooting">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 20 — Pemecahan Masalah</span>
            <span>Halaman 22</span>
        </div>
        <div class="content">
            <h1>Bab 20: Pemecahan Masalah & Pertanyaan Umum</h1>
            <p class="lead">Solusi atas kendala operasional yang mungkin dihadapi saat menggunakan KumaLift pada berbagai perangkat Android.</p>

            <div class="qa-group" style="margin-top: 8px;">
                <div class="qa-question">T: Mengapa notifikasi pengingat latihan saya datang terlambat beberapa menit?</div>
                <div class="qa-answer">J: Beberapa produsen ponsel Android (seperti Xiaomi/MIUI, Samsung/OneUI, atau Oppo/ColorOS) menerapkan manajemen baterai latar belakang yang agresif (Doze Mode). Buka Pengaturan Ponsel &gt; Aplikasi &gt; KumaLift &gt; Penggunaan Baterai, lalu pilih <strong>"Tidak Dibatasi" (Unrestricted)</strong> agar sistem operasi mengizinkan alarm presisi berbunyi tepat waktu.</div>
            </div>

            <div class="qa-group">
                <div class="qa-question">T: Mengapa tidak ada template latihan bawaan saat pertama kali membuka aplikasi?</div>
                <div class="qa-answer">J: Ini adalah rancangan yang disengaja. KumaLift menganut filosofi tanpa clutter dummy. Kami ingin Anda bebas menyusun program latihan yang benar-benar sesuai dengan alat di gym Anda daripada harus menghapus puluhan template dummy yang tidak pernah digunakan.</div>
            </div>

            <div class="qa-group">
                <div class="qa-question">T: Apakah timer latihan tetap berjalan jika saya keluar aplikasi atau layar mati?</div>
                <div class="qa-answer">J: Ya. Waktu mulai latihan disimpan sebagai timestamp absolut. Ketika Anda kembali membuka aplikasi atau menyalakan layar, durasi akan langsung diperbarui ke detik yang tepat tanpa kehilangan waktu.</div>
            </div>

            <div class="qa-group">
                <div class="qa-question">T: Bagaimana jika ponsel saya ganti atau di-factory reset? Apakah data saya hilang?</div>
                <div class="qa-answer">J: Karena KumaLift menyimpan data di database lokal, data akan hilang jika aplikasi di-uninstall tanpa dicadangkan. Sangat disarankan untuk mengekspor backup JSON melalui menu <em>Settings &gt; Export & Import Data</em> dan menyimpannya ke Google Drive atau mengirimkannya ke email pribadi secara berkala.</div>
            </div>

            <div class="qa-group">
                <div class="qa-question">T: Bagaimana rumus penghitungan Estimasi 1RM (One-Rep Max) di KumaLift?</div>
                <div class="qa-answer">J: KumaLift menggunakan rumus Brzycki standar industri: <code>1RM = Beban / (1.0278 - (0.0278 × Repetisi))</code> untuk repetisi di bawah 10, yang memberikan akurasi tinggi dalam memprediksi potensi kekuatan satu repetisi maksimal Anda.</div>
            </div>

            <div class="qa-group">
                <div class="qa-question">T: Mengapa popup pengingat backup tidak muncul setiap hari?</div>
                <div class="qa-answer">J: KumaLift menerapkan kebijakan cooldown 7 hari. Popup pengingat backup dirancang agar tidak mengganggu Anda saat sedang fokus latihan, sehingga hanya akan muncul maksimal satu kali dalam seminggu setelah interaksi terakhir.</div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 22</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 23: GLOSSARY & RPE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-glossary">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 21 — Glosarium & Skala RPE</span>
            <span>Halaman 23</span>
        </div>
        <div class="content">
            <h1>Bab 21: Glosarium Istilah & Panduan RPE</h1>
            <p class="lead">Panduan standar skala intensitas latihan (Rate of Perceived Exertion) dan istilah teknis kebugaran.</p>

            <h2>Tabel Skala Intensitas RPE (Borg Scale Modifikasi)</h2>
            <p>Skala RPE digunakan untuk mengukur tingkat kedekatan suatu set latihan dengan kegagalan otot (failure).</p>

            <table class="clean-table" style="margin-top: 4px;">
                <thead>
                    <tr>
                        <th style="width: 50px;">Nilai RPE</th>
                        <th style="width: 90px;">Tingkat Usaha</th>
                        <th>Kondisi Sisa Tenaga (Reps in Reserve / RIR)</th>
                        <th>Tujuan Pelatihan yang Cocok</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>RPE 10</strong></td>
                        <td>Usaha Maksimal</td>
                        <td><strong>0 Reps Tersisa:</strong> Beban maksimal mutlak, tidak mampu melakukan 1 repetisi lagi meskipun dipaksakan.</td>
                        <td>Uji coba kekuatan puncak, kompetisi powerlifting.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE 9.5</strong></td>
                        <td>Sangat Berat</td>
                        <td><strong>0–1 Rep:</strong> Tidak bisa menambah repetisi, tetapi mungkin beban bisa dinaikkan sedikit.</td>
                        <td>Set penutup latihan kekuatan tinggi.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE 9</strong></td>
                        <td>Berat</td>
                        <td><strong>1 Rep Tersisa:</strong> Mampu melakukan tepat 1 repetisi lagi dengan teknik yang benar sebelum failure.</td>
                        <td>Fase intensifikasi hypertrophy dan strength.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE 8</strong></td>
                        <td>Moderat Berat</td>
                        <td><strong>2 Reps Tersisa:</strong> Mampu melakukan 2 repetisi lagi dengan kontrol gerakan yang solid.</td>
                        <td>Beban kerja standar (working set) paling efektif untuk pertumbuhan otot.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE 7</strong></td>
                        <td>Moderat</td>
                        <td><strong>3 Reps Tersisa:</strong> Beban bergerak dengan kecepatan baik, masih tersisa 3 repetisi sebelum lelah.</td>
                        <td>Fase akumulasi volume dan pemanasan beban kerja.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE &le; 6</strong></td>
                        <td>Ringan</td>
                        <td><strong>4+ Reps Tersisa:</strong> Beban terasa sangat ringan, minim kelelahan metabolik.</td>
                        <td>Pemanasan (warm-up), pemulihan aktif (deload).</td>
                    </tr>
                </tbody>
            </table>

            <h2 style="margin-top: 10px;">Glosarium Istilah Kebugaran</h2>
            <div class="grid-2col" style="margin-top: 4px;">
                <div>
                    <p><strong>Progressive Overload:</strong> Peningkatan bertahap pada beban, repetisi, atau kualitas kontrol gerakan seiring berjalannya waktu untuk menstimulasi pertumbuhan otot dan kekuatan.</p>
                    <p><strong>One-Rep Max (1RM):</strong> Beban terberat yang dapat diangkat seseorang untuk satu kali repetisi penuh dengan teknik yang benar.</p>
                </div>
                <div>
                    <p><strong>Total Volume (Tonase):</strong> Jumlah total beban yang diangkat dalam satu sesi, dihitung dengan rumus: <code>Volume = Berat (KG) × Repetisi × Jumlah Set</code>.</p>
                    <p><strong>Personal Record (PR):</strong> Catatan prestasi terbaik pribadi yang pernah diraih atlet pada gerakan tertentu.</p>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 23</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 24: SPECIFICATIONS & APPENDIX
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-specs">
        <div class="header">
            <span class="brand">KumaLift</span>
            <span>Bab 22 — Spesifikasi Rilis</span>
            <span>Halaman 24</span>
        </div>
        <div class="content">
            <h1>Bab 22: Spesifikasi Teknis & Rilis</h1>
            <p class="lead">Spesifikasi teknis aplikasi, dukungan platform, dan catatan penutup edisi resmi versi 1.0.0.</p>

            <div class="grid-2col" style="margin-top: 8px;">
                <div>
                    <h2>Spesifikasi Perangkat Lunak</h2>
                    <table class="clean-table">
                        <tr>
                            <td><strong>Nama Aplikasi</strong></td>
                            <td>KumaLift</td>
                        </tr>
                        <tr>
                            <td><strong>Versi Produksi</strong></td>
                            <td>1.0.0</td>
                        </tr>
                        <tr>
                            <td><strong>Sistem Operasi Minimum</strong></td>
                            <td>Android 8.0 (API Level 26)</td>
                        </tr>
                        <tr>
                            <td><strong>Sistem Operasi Target</strong></td>
                            <td>Android 15 (API Level 35)</td>
                        </tr>
                        <tr>
                            <td><strong>Engine Basis Data</strong></td>
                            <td>SQLite Lokal (SQLDelight Driver)</td>
                        </tr>
                        <tr>
                            <td><strong>Arsitektur UI</strong></td>
                            <td>Jetpack Compose Multiplatform</td>
                        </tr>
                        <tr>
                            <td><strong>Konektivitas Internet</strong></td>
                            <td>Tidak Diperlukan (100% Offline-First)</td>
                        </tr>
                        <tr>
                            <td><strong>Format Cadangan</strong></td>
                            <td>JSON Terenkapsulasi Schema v1 & CSV</td>
                        </tr>
                    </table>
                </div>

                <div>
                    <h2>Komitmen Kualitas & Privasi</h2>
                    <p>KumaLift dirancang dengan komitmen tanpa kompromi terhadap privasi dan kecepatan pengguna. Aplikasi ini tidak mengandung iklan pihak ketiga, tidak memerlukan login sosial, dan tidak melakukan analitik pelacakan tersembunyi.</p>

                    <h3>Dukungan Pengembang</h3>
                    <p>Untuk melaporkan kendala, saran fitur baru, atau kontribusi terhadap katalog gerakan latihan, silakan hubungi tim pengembang melalui repository resmi atau email dukungan pengembang.</p>

                    <div style="margin-top: 20px; padding: 12px; background: #111827; border: 1px solid #1E293B; border-radius: 8px; text-align: center;">
                        <div style="font-size: 20pt; margin-bottom: 4px;">🐻</div>
                        <div style="font-weight: 700; color: #F1F5F9; font-size: 9pt;">KumaLift Engineering Team</div>
                        <div style="color: #64748B; font-size: 7.8pt; margin-top: 2px;">BearBones Development Group • Edisi 2026</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 24</span>
        </div>
    </div>
    """)

    html_content = f"""<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title>KumaLift User Guide (Indonesian)</title>
    <style>
    {css}
    </style>
</head>
<body>
    {''.join(pages)}
</body>
</html>
"""
    with open(OUT_FILE, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"Indonesian User Guide HTML saved to: {OUT_FILE} ({len(pages)} pages)")

if __name__ == "__main__":
    build()
