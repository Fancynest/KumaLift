import os
import base64

ROOT_DIR = "c:/Users/Gabriel/AndroidStudioProjects/KumaLift"
RAW_DIR = os.path.join(ROOT_DIR, "documentation/screenshots/raw")
ANN_DIR = os.path.join(ROOT_DIR, "documentation/screenshots/annotated")
OUT_HTML = os.path.join(ROOT_DIR, "documentation/source/user_guide.html")

def get_b64_image(filename):
    path = os.path.join(ANN_DIR, filename)
    if not os.path.exists(path):
        path = os.path.join(RAW_DIR, filename)
    if not os.path.exists(path):
        print(f"Warning: Image {filename} not found!")
        return ""
    with open(path, "rb") as f:
        data = base64.b64encode(f.read()).decode("utf-8")
    return f"data:image/png;base64,{data}"

def build_html():
    print("Loading screenshots into base64...")
    imgs = {
        "01": get_b64_image("01_home_dashboard.png"),
        "02": get_b64_image("02_routines_empty.png"),
        "03": get_b64_image("03_create_routine.png"),
        "04": get_b64_image("04_routines_list.png"),
        "05": get_b64_image("05_exercise_catalog.png"),
        "06": get_b64_image("06_exercise_detail.png"),
        "06b": get_b64_image("06b_create_exercise.png"),
        "08": get_b64_image("08_active_workout_with_timer.png"),
        "09": get_b64_image("09_dialog_custom_weight.png"),
        "10": get_b64_image("10_dialog_custom_reps.png"),
        "12": get_b64_image("12_workout_finish_summary.png"),
        "13": get_b64_image("13_progress_analytics.png"),
        "14": get_b64_image("14_profile_dashboard.png"),
        "15": get_b64_image("15_measures_modal.png"),
        "16": get_b64_image("16_settings_main.png"),
        "17": get_b64_image("17_settings_notifications.png"),
        "18": get_b64_image("18_dialog_custom_reminder.png"),
        "19": get_b64_image("19_backup_management.png"),
    }
    print("All base64 images ready. Generating HTML content...")

    css = """
    @page {
        size: A4;
        margin: 10mm 12mm 12mm 12mm;
    }
    *, *::before, *::after {
        box-sizing: border-box;
    }
    body {
        margin: 0;
        padding: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        background-color: #0B0D13;
        color: #E2E8F0;
        font-size: 9pt;
        line-height: 1.45;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
    }
    .page {
        page-break-after: always;
        height: 275mm;
        max-height: 275mm;
        position: relative;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        padding: 0 4mm;
    }
    .page-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #262B38;
        padding-bottom: 5px;
        margin-bottom: 8px;
        font-size: 8pt;
        color: #8B949E;
        text-transform: uppercase;
        letter-spacing: 0.8px;
    }
    .page-header .brand {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: 700;
        color: #FF5722;
    }
    .page-header .section-title {
        color: #CBD5E1;
        font-weight: 600;
    }
    .page-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #262B38;
        padding-top: 5px;
        margin-top: 6px;
        font-size: 7.5pt;
        color: #64748B;
    }
    .page-footer .doc-info {
        display: flex;
        gap: 12px;
    }
    .page-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
    }
    
    /* Typography */
    h1, h2, h3, h4 {
        margin: 0 0 6px 0;
        color: #FFFFFF;
        font-weight: 700;
        letter-spacing: -0.3px;
    }
    h1 { font-size: 17pt; color: #FFFFFF; line-height: 1.2; }
    h2 { font-size: 13pt; color: #FF5722; border-left: 3px solid #FF5722; padding-left: 8px; margin-bottom: 6px; }
    h3 { font-size: 10.5pt; color: #F1F5F9; margin-top: 4px; margin-bottom: 4px; }
    h4 { font-size: 9pt; color: #CBD5E1; text-transform: uppercase; letter-spacing: 0.5px; }
    p { margin: 0 0 6px 0; text-align: justify; }
    
    /* Badges & Pills */
    .badge {
        display: inline-block;
        padding: 2px 7px;
        border-radius: 4px;
        font-size: 7.5pt;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.4px;
    }
    .badge-orange { background: rgba(255, 87, 34, 0.2); color: #FF7043; border: 1px solid #FF5722; }
    .badge-green { background: rgba(16, 185, 129, 0.2); color: #34D399; border: 1px solid #10B981; }
    .badge-blue { background: rgba(59, 130, 246, 0.2); color: #60A5FA; border: 1px solid #3B82F6; }
    .badge-gray { background: #262B38; color: #CBD5E1; }

    /* Callout Boxes */
    .callout {
        border-radius: 6px;
        padding: 7px 10px;
        margin: 6px 0;
        font-size: 8pt;
        line-height: 1.35;
    }
    .callout-title {
        font-weight: 700;
        text-transform: uppercase;
        font-size: 7.5pt;
        letter-spacing: 0.5px;
        margin-bottom: 3px;
        display: flex;
        align-items: center;
        gap: 5px;
    }
    .callout-tip { background: rgba(16, 185, 129, 0.08); border-left: 3px solid #10B981; color: #D1FAE5; }
    .callout-tip .callout-title { color: #34D399; }
    .callout-note { background: rgba(59, 130, 246, 0.08); border-left: 3px solid #3B82F6; color: #DBEAFE; }
    .callout-note .callout-title { color: #60A5FA; }
    .callout-important { background: rgba(245, 158, 11, 0.08); border-left: 3px solid #F59E0B; color: #FEF3C7; }
    .callout-important .callout-title { color: #FBBF24; }
    .callout-warning { background: rgba(239, 68, 68, 0.08); border-left: 3px solid #EF4444; color: #FEE2E2; }
    .callout-warning .callout-title { color: #F87171; }

    /* Tables */
    table.data-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 7.8pt;
        margin: 5px 0 7px 0;
    }
    table.data-table th {
        background-color: #1A1F2C;
        color: #F8FAFC;
        text-align: left;
        padding: 5px 7px;
        font-weight: 700;
        border: 1px solid #283042;
        text-transform: uppercase;
        font-size: 7.2pt;
        letter-spacing: 0.4px;
    }
    table.data-table td {
        padding: 4px 7px;
        border: 1px solid #232938;
        background-color: #121620;
        color: #CBD5E1;
        vertical-align: middle;
    }
    table.data-table tr:nth-child(even) td {
        background-color: #161B26;
    }
    .num-col {
        width: 28px;
        text-align: center;
        font-weight: 700;
    }
    .callout-num {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 17px;
        height: 17px;
        border-radius: 50%;
        background-color: #E64A19;
        color: #FFFFFF;
        font-weight: 700;
        font-size: 7.5pt;
        box-shadow: 0 1px 3px rgba(0,0,0,0.5);
    }

    /* Grid Layouts for Visual Manual */
    .feature-layout {
        display: grid;
        grid-template-columns: 82mm 1fr;
        gap: 12px;
        align-items: start;
        margin-top: 4px;
        height: 100%;
    }
    .feature-layout-reverse {
        display: grid;
        grid-template-columns: 1fr 82mm;
        gap: 12px;
        align-items: start;
        margin-top: 4px;
    }
    .dual-feature-layout {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        margin-top: 4px;
    }
    .screenshot-card {
        background: #141822;
        border: 1px solid #262D3D;
        border-radius: 8px;
        padding: 5px;
        text-align: center;
        box-shadow: 0 4px 12px rgba(0,0,0,0.4);
    }
    .screenshot-card img {
        width: 100%;
        max-height: 180mm;
        object-fit: contain;
        border-radius: 5px;
        display: block;
    }
    .screenshot-card-mini img {
        width: 100%;
        max-height: 100mm;
        object-fit: contain;
        border-radius: 5px;
        display: block;
    }
    .screenshot-caption {
        font-size: 7.2pt;
        color: #94A3B8;
        margin-top: 4px;
        font-style: italic;
    }

    /* Cover Page Styling */
    .cover-container {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        align-items: center;
        text-align: center;
        padding: 25mm 10mm 15mm 10mm;
        background: radial-gradient(circle at 50% 30%, #1F1512 0%, #0B0D13 70%);
        border: 1px solid #232938;
        border-radius: 12px;
    }
    .cover-logo {
        width: 90px;
        height: 90px;
        border-radius: 22px;
        background: linear-gradient(135deg, #FF5722 0%, #BF360C 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 44pt;
        box-shadow: 0 10px 30px rgba(255, 87, 34, 0.35);
        border: 2px solid #FF8A65;
        margin-bottom: 12px;
    }
    .cover-title {
        font-size: 32pt;
        font-weight: 800;
        letter-spacing: -1px;
        color: #FFFFFF;
        margin-bottom: 6px;
    }
    .cover-subtitle {
        font-size: 14pt;
        color: #FF7043;
        font-weight: 600;
        margin-bottom: 16px;
        letter-spacing: 0.5px;
    }
    .cover-desc {
        max-width: 140mm;
        font-size: 9.5pt;
        color: #94A3B8;
        line-height: 1.5;
        margin-bottom: 25px;
    }
    .cover-badges {
        display: flex;
        gap: 10px;
        margin-bottom: 30px;
    }
    .cover-meta {
        font-size: 8pt;
        color: #64748B;
        line-height: 1.6;
        border-top: 1px solid #1E2433;
        padding-top: 12px;
        width: 100%;
        max-width: 130mm;
    }

    /* TOC Styling */
    .toc-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 15px;
        margin-top: 10px;
    }
    .toc-section {
        background: #141824;
        border: 1px solid #232A3B;
        border-radius: 6px;
        padding: 9px 12px;
    }
    .toc-header {
        font-size: 9pt;
        font-weight: 700;
        color: #FF5722;
        border-bottom: 1px solid #252D3F;
        padding-bottom: 4px;
        margin-bottom: 6px;
        display: flex;
        justify-content: space-between;
    }
    .toc-item {
        display: flex;
        justify-content: space-between;
        font-size: 7.8pt;
        color: #CBD5E1;
        padding: 2.5px 0;
        border-bottom: 1px dashed rgba(255,255,255,0.06);
    }
    .toc-item:last-child { border-bottom: none; }
    .toc-item a { color: inherit; text-decoration: none; }
    .toc-item a:hover { color: #FF7043; }
    .toc-page { color: #FF7043; font-weight: 700; }
    
    /* Step List */
    ol.step-list {
        margin: 4px 0 6px 16px;
        padding: 0;
        font-size: 8pt;
    }
    ol.step-list li {
        margin-bottom: 4px;
        color: #CBD5E1;
    }
    ol.step-list li strong {
        color: #F8FAFC;
    }
    ul.bullet-list {
        margin: 4px 0 6px 14px;
        padding: 0;
        font-size: 8pt;
    }
    ul.bullet-list li {
        margin-bottom: 3px;
        color: #CBD5E1;
    }
    </style>
    """

    # We will assemble all 20 pages
    pages = []

    # ==========================================
    # PAGE 1: COVER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-cover">
        <div class="cover-container">
            <div>
                <div style="display:flex; justify-content:center;">
                    <div class="cover-logo">🐻</div>
                </div>
                <div class="cover-title">KUMALIFT</div>
                <div class="cover-subtitle">PANDUAN PENGGUNA RESMI & MANUAL TEKNIS</div>
                <div class="cover-desc">
                    Dokumentasi resmi arsitektur sistem, navigasi, pencatatan beban hipertrofi, 
                    visualisasi progres, timer interval, dan manajemen data lokal 100% offline.
                </div>
                <div class="cover-badges" style="justify-content:center;">
                    <span class="badge badge-orange">Versi 1.0.0 Stabil</span>
                    <span class="badge badge-green">100% Offline & Privasi Terjamin</span>
                    <span class="badge badge-blue">Kotlin Multiplatform</span>
                    <span class="badge badge-gray">Edisi Komersial</span>
                </div>
            </div>
            <div class="cover-meta">
                <strong>Pengembang Aplikasi:</strong> BearBones Engineering Team<br>
                <strong>Target Platform:</strong> Android (API 26+) & KMP Ecosystem<br>
                <strong>Spesifikasi Basis Data:</strong> Local SQLDelight (SQLite Engine)<br>
                <strong>Tanggal Publikasi:</strong> November 2026 • Dokumentasi Resmi
            </div>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 2: TABLE OF CONTENTS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-toc">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">DAFTAR ISI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>B. Daftar Isi & Struktur Panduan</h2>
            <p>Panduan ini disusun secara terstruktur dari tingkat dasar pengenalan aplikasi hingga pengelolaan data tingkat lanjut. Gunakan tautan bab untuk navigasi cepat pada pembaca PDF.</p>
            
            <div class="toc-grid">
                <div class="toc-section">
                    <div class="toc-header"><span>PENGENALAN & DASAR</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-intro">C. Pendahuluan & Filosofi Sistem</a><span class="toc-page">3</span></div>
                    <div class="toc-item"><a href="#page-getting-started">D. Memulai Aplikasi & Navigasi Utama</a><span class="toc-page">4</span></div>
                    
                    <div class="toc-header" style="margin-top:10px;"><span>MANAJEMEN PROGRAM LATIHAN</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-routines-empty">E.1 Workout Routine: Konsep & Empty State</a><span class="toc-page">5</span></div>
                    <div class="toc-item"><a href="#page-routines-create">E.1 Pembuatan & Kartu Workout Routine</a><span class="toc-page">6</span></div>
                    
                    <div class="toc-header" style="margin-top:10px;"><span>KATALOG GERAKAN & VISUALISASI</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-exercise-catalog">E.2 Katalog Gerakan & Multi-Tier Filter</a><span class="toc-page">7</span></div>
                    <div class="toc-item"><a href="#page-exercise-detail">E.2 Visual Demo Player & Custom Exercise</a><span class="toc-page">8</span></div>
                </div>

                <div class="toc-section">
                    <div class="toc-header"><span>PENCATATAN AKTIF & LOGGING</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-workout-logger">E.3 Active Workout Logger & Rest Timer</a><span class="toc-page">9</span></div>
                    <div class="toc-item"><a href="#page-custom-input">E.3 Dialog Input Custom KG & Repetisi</a><span class="toc-page">10</span></div>
                    <div class="toc-item"><a href="#page-workout-finish">E.4 Penyelesaian Sesi & Selebrasi PR</a><span class="toc-page">11</span></div>

                    <div class="toc-header" style="margin-top:10px;"><span>ANALYTICS, PROFIL & PENGUKURAN</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-progress-analytics">E.5 Analisis Progresi, 1RM & Kurva Beban</a><span class="toc-page">12</span></div>
                    <div class="toc-item"><a href="#page-profile-measures">E.6 Profil Atlet & Pengukuran Tubuh</a><span class="toc-page">13</span></div>

                    <div class="toc-header" style="margin-top:10px;"><span>PENGATURAN, NOTIFIKASI & DATA</span><span>HAL</span></div>
                    <div class="toc-item"><a href="#page-settings-main">G. Konfigurasi Sistem & Preferensi</a><span class="toc-page">14</span></div>
                    <div class="toc-item"><a href="#page-settings-notifications">H. Pengingat Presisi & Keypad Reminder</a><span class="toc-page">15</span></div>
                    <div class="toc-item"><a href="#page-backup-management">I. Manajemen Data & 7-Day Backup Cooldown</a><span class="toc-page">16</span></div>
                </div>
            </div>

            <div class="toc-section" style="margin-top:10px;">
                <div class="toc-header"><span>ALUR PENGGUNAAN, SOLUSI & REFERENSI</span><span>HAL</span></div>
                <div style="display:grid; grid-template-columns:1fr 1fr 1fr; gap:10px;">
                    <div class="toc-item"><a href="#page-workflows">F. Alur Skenario Nyata & Praktik</a><span class="toc-page">17</span></div>
                    <div class="toc-item"><a href="#page-troubleshooting">J. Pemecahan Masalah & FAQ</a><span class="toc-page">18</span></div>
                    <div class="toc-item"><a href="#page-glossary">K. Glosarium & Tabel RPE</a><span class="toc-page">19</span></div>
                </div>
                <div class="toc-item" style="margin-top:4px;"><a href="#page-closing">L. Penutup & Catatan Rilis Versi 1.0.0</a><span class="toc-page">20</span></div>
            </div>

            <div class="callout callout-note" style="margin-top:12px;">
                <div class="callout-title">ℹ️ TENTANG DOKUMENTASI INI</div>
                Seluruh tangkapan layar, nomor penanda, koordinat, dan penjelasan fungsional dalam dokumen ini diambil langsung dari eksekusi aplikasi aktual KumaLift pada platform Android, merefleksikan kondisi rilis produksi terbaru tanpa ada mockup atau fitur rekayasa.
            </div>
        </div>
        <div class="page-footer">
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
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN C — PENDAHULUAN</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>C. Pendahuluan & Filosofi Aplikasi</h2>
            
            <p><strong>KumaLift</strong> adalah aplikasi pencatat latihan beban (<em>hypertrophy and strength workout logger</em>) kelas profesional yang dirancang khusus untuk binaragawan, atlet angkat beban, dan penggiat kebugaran yang menuntut efisiensi, akurasi, dan kepemilikan data penuh tanpa kompromi.</p>

            <h3>1. Masalah Utama yang Diselesaikan</h3>
            <p>Sebagian besar aplikasi kebugaran komersial saat ini mengalami penyakit "bloatware": mewajibkan pendaftaran akun pihak ketiga, menampilkan iklan yang mengganggu konsentrasi latihan, menyembunyikan fitur dasar di balik langganan bulanan mahal, serta memaksa sinkronisasi cloud yang lambat dan rentan kebocoran privasi.</p>
            <p>KumaLift membalikkan paradigma tersebut dengan pendekatan <strong>Local-First & Zero-Distraction</strong>:</p>
            <ul class="bullet-list">
                <li><strong>100% Offline & Mandiri:</strong> Seluruh data riwayat latihan, personal records (PR), pengukuran tubuh, dan template tersimpan secara lokal di mesin SQLite perangkat pengguna melalui SQLDelight. Tidak ada telemetri rahasia atau akun yang wajib dibuat.</li>
                <li><strong>Progressive Overload yang Akurat:</strong> Menghilangkan sistem penambahan beban kaku (seperti auto-increment kelipatan 5 kg). Pengguna memiliki kontrol bebas mengetik beban desimal (misal 32.5 kg) dan repetisi aktual.</li>
                <li><strong>Timer Wall-Clock Presisi:</strong> Timer latihan tidak pernah menggunakan nilai statis tiruan, melainkan menghitung detik aktual sejak tombol Start ditekan dengan kompensasi waktu jeda (pause) yang akurat.</li>
                <li><strong>Demonstrasi Visual Terkendali:</strong> Lebih dari 1.324 gerakan latihan dilengkapi pemutar animasi WebP/GIF dengan kendali kecepatan (0.5x hingga 2.0x) untuk validasi teknik form yang tepat.</li>
            </ul>

            <h3>2. Target Pengguna</h3>
            <p>Aplikasi ini dirancang untuk:</p>
            <ul class="bullet-list">
                <li><strong>Atlet Hipertrofi & Bodybuilding:</strong> Yang membutuhkan pelacakan volume tonase (kg) mingguan per kelompok otot serta monitoring RPE (Rate of Perceived Exertion) yang ketat.</li>
                <li><strong>Powerlifter & Penggiat Kekuatan:</strong> Yang memprioritaskan estimasi 1RM (One Rep Max) dan pencatatan rekor pribadi (PR) otomatis.</li>
                <li><strong>Pengguna yang Sadar Privasi:</strong> Yang menginginkan kendali mutlak atas data kesehatan pribadi mereka tanpa risiko dijual kepada broker periklanan.</li>
            </ul>

            <div class="callout callout-tip">
                <div class="callout-title">💡 PRINSIP DESAIN: TANPA TEMPLATE BAWAAN KAKU</div>
                Sesuai filosofi latihan terprogram, KumaLift tidak memaksakan template latihan generik bawaan pabrik. Pengguna dipersilakan membangun rutinitas latihan dari nol (from scratch) yang disesuaikan secara ilmiah dengan jadwal, alat, dan fisiologi tubuh masing-masing.
            </div>

            <h3>3. Arsitektur Teknis</h3>
            <p>Dibangun menggunakan stack teknologi modern <strong>Kotlin Multiplatform (KMP)</strong> dan <strong>Jetpack Compose Multiplatform</strong>, menjamin performa rendering 60-120 FPS yang mulus pada antarmuka Android, konsumsi memori hemat, dan lifecycle coroutine yang aman dari memory leak.</p>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 3</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 4: GETTING STARTED & HOME DASHBOARD
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-getting-started">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN D — MEMULAI & NAVIGASI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>D. Memulai Aplikasi & Navigasi Utama</h2>
            <p>Saat pertama kali diluncurkan, KumaLift langsung membawa pengguna ke Dashboard Utama tanpa meminta pendaftaran email, login, atau kartu kredit. Pengguna hanya perlu memberikan izin notifikasi jika ingin mengaktifkan alarm pengingat latihan harian.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['01']}" alt="Dashboard Home KumaLift">
                        <div class="screenshot-caption">Gambar 1: Antarmuka Dashboard Utama (Home) KumaLift</div>
                    </div>
                </div>
                <div>
                    <h3>Penjelasan Elemen Dashboard Utama</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Elemen UI</th>
                                <th>Fungsi & Perilaku Teknis</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Header & Sapaan</strong></td>
                                <td>Menampilkan fase siklus latihan aktif (misal: <em>HYPERTROPHY PHASE</em>) dan sapaan personal atlet berdasarkan waktu lokal.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Metrik Mingguan</strong></td>
                                <td>Rangkuman tonase beban minggu ini (kg), persentase target mingguan (contoh: 75% 3/4 sesi), dan jumlah Rekor Baru (PR).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Konsistensi 7 Hari</strong></td>
                                <td>Indikator kalender Senin–Minggu (M T W T F S S) yang menandai hari aktif latihan dengan warna oranye terang.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Jadwal Hari Ini</strong></td>
                                <td>Kartu program hari ini (misal: Push Day B Fokus Dada) beserta estimasi durasi dan jumlah latihan yang terdaftar.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Tombol START WORKOUT</strong></td>
                                <td>Tombol aksi utama (CTA) berlatar oranye menyala untuk langsung membuka <em>Active Workout Logger</em>.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">6</span></td>
                                <td><strong>Statistik Seumur Hidup</strong></td>
                                <td>Grid ringkasan total sesi seumur hidup (Lifetime Sessions), total tonase (Lifetime Tonnage), dan total rekor PR.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">7</span></td>
                                <td><strong>Riwayat Sesi Terbaru</strong></td>
                                <td>Daftar riwayat sesi terakhir lengkap dengan tanggal, durasi menit, total kg, dan lencana PR yang diraih.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">8</span></td>
                                <td><strong>Bottom Navigation Bar</strong></td>
                                <td>Bilah navigasi 5 tab: <em>Home</em>, <em>Workouts</em> (Routines), <em>Exercises</em> (Katalog Gerakan), <em>Progress</em>, dan <em>Profile</em>.</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-important">
                        <div class="callout-title">⚠️ IZIN NOTIFIKASI & ALARM SISTEM (ANDROID 12+)</div>
                        Pada Android 12 ke atas, sistem mewajibkan izin <code>POST_NOTIFICATIONS</code> dan <code>SCHEDULE_EXACT_ALARM</code> agar pengingat latihan dapat berbunyi tepat waktu saat perangkat berada dalam kondisi <em>Doze Mode</em> (layar mati).
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 4</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 5: WORKOUT ROUTINES: EMPTY STATE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-empty">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.1 — WORKOUT ROUTINES</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.1 Manajemen Workout Routine: Dasar & Empty State</h2>
            <p>Workout Routine adalah cetak biru (<em>blueprint</em>) terencana yang memuat urutan latihan, target intensitas RPE, estimasi durasi, dan kelompok otot target. KumaLift menganut prinsip <strong>Zero-Default Templates</strong>: seluruh template bawaan telah dihapus dari basis data agar pengguna bebas menyusun rutinitas latihan personal dari nol.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['02']}" alt="Tampilan Empty State Workout Routines">
                        <div class="screenshot-caption">Gambar 2: Kondisi Awal (Empty State) Layar Workout Routines</div>
                    </div>
                </div>
                <div>
                    <h3>Tampilan Status Kosong (Empty State)</h3>
                    <p>Ketika aplikasi baru dipasang atau seluruh routine lama telah dibersihkan, tab <em>Workouts</em> menampilkan panduan grafis yang rapi dan mengarahkan pengguna untuk membuat rutinitas pertama.</p>
                    
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Komponen UI</th>
                                <th>Keterangan & Cara Penggunaan</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Judul Layar</strong></td>
                                <td>Teks "Workout Routines" sebagai penanda tab manajemen program latihan.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Tombol Top Create</strong></td>
                                <td>Tombol cepat di pojok kanan atas untuk membuka layar pembuatan routine baru.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Kartu Training Program</strong></td>
                                <td>Menampilkan nama program aktif (<em>Hypertrophy Program</em>) dan bilah persentase kepatuhan mingguan (Weekly Adherence).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Chip Filter Kategori</strong></td>
                                <td>Filter horizontal: <em>All Programs</em>, <em>PPL Split</em>, <em>Upper / Lower</em>, dan <em>Full Body</em> untuk memilah kartu routine.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Wadah Empty State</strong></td>
                                <td>Ilustrasi barbel dan pesan informatif: "<em>No Routines Yet — Build your custom workout routines from scratch to fit your training program</em>" disertai tombol "Create Routine".</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-tip">
                        <div class="callout-title">💡 KEUNTUNGAN MEMBUAT ROUTINE SENDIRI</div>
                        Rutinitas latihan kustom memungkinkan Anda mengelompokkan latihan sesuai ketersediaan alat di gym lokal Anda (misal: dumbbell-only atau cable-focused), mencegah waktu terbuang mencari gerakan saat latihan sedang berlangsung.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 5</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 6: WORKOUT ROUTINES: CREATION & LIST
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-routines-create">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.1 — PEMBUATAN ROUTINE</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.1 Pembuatan & Kartu Workout Routine</h2>
            <p>Membuat routine di KumaLift dilakukan melalui formulir komprehensif yang memvalidasi nama, split latihan, target durasi, tingkat intensitas RPE, dan daftar gerakan.</p>

            <div class="dual-feature-layout">
                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['03']}" alt="Formulir Create Routine">
                        <div class="screenshot-caption">Gambar 3: Formulir Pembuatan Routine (Create Routine)</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Formulir</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Tombol Save</strong></td><td>Menyimpan routine ke SQLite lokal jika form valid.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Routine Name</strong></td><td>Kolom teks nama routine (contoh: Push Day A Focus).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Pilihan Kategori</strong></td><td>Chip split: PPL Split, Upper/Lower, Full Body, Custom.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Estimasi Durasi</strong></td><td>Pilihan estimasi waktu: 30, 45, 60, 75, atau 90 menit.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Target RPE Tier</strong></td><td>Target intensitas beban: Light (6-7), Moderate (7-8), Heavy (8-9), Maximal (9-10).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">6</span></td><td><strong>Bagian Latihan</strong></td><td>Daftar gerakan terpilih dan tombol <em>Add Exercise</em>.</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['04']}" alt="Kartu Routine Aktif">
                        <div class="screenshot-caption">Gambar 4: Tampilan Kartu Routine yang Telah Disimpan</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Kartu</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Program Adherence</strong></td><td>Bilah pencapaian target latihan dalam 1 minggu berjalan.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Filter Split</strong></td><td>Menyaring daftar routine sesuai kategori yang diketuk.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Wadah Kartu Routine</strong></td><td>Area visual kartu yang memuat metadata latihan lengkap.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Ikon Delete (Sampah)</strong></td><td>Menghapus routine dari penyimpanan permanen dengan satu ketukan.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Chip Gerakan</strong></td><td>Menampilkan pratinjau nama gerakan dalam routine.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">6</span></td><td><strong>Tombol Start Routine</strong></td><td>Memulai workout aktif langsung dengan template routine ini.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="callout callout-note" style="margin-top:6px;">
                <div class="callout-title">ℹ️ DETEKSI FOKUS OTOT OTOMATIS</div>
                Saat Anda menambahkan gerakan ke dalam routine, KumaLift secara otomatis menganalisis kelompok otot primer dari seluruh latihan terpilih dan merangkumnya menjadi teks <em>Muscle Focus</em> (misal: "Dada • 60 min • Berat, RPE 8–9").
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 6</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 7: EXERCISE CATALOG & FILTERING
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-exercise-catalog">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.2 — KATALOG GERAKAN</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.2 Katalog Gerakan & Multi-Tier Filter Library</h2>
            <p>KumaLift menyertakan basis data internal komprehensif berisi lebih dari <strong>1.324 gerakan latihan</strong> yang mencakup seluruh kelompok anatomi tubuh manusia. Sistem filtering 3 lapis memungkinkan pencarian instan dalam hitungan milidetik.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['05']}" alt="Katalog Gerakan Latihan KumaLift">
                        <div class="screenshot-caption">Gambar 5: Layar Katalog Gerakan & Sistem Filter 3 Lapis</div>
                    </div>
                </div>
                <div>
                    <h3>Komponen Filter & Daftar Gerakan</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Komponen UI</th>
                                <th>Keterangan & Logika Penyaringan</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Pencarian & Create</strong></td>
                                <td>Kolom teks pencarian instan (nama gerakan) dan tombol "Create" di pojok kanan untuk membuat gerakan kustom baru.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Filter Kelompok Otot</strong></td>
                                <td>Baris chip horizontal: <em>All Muscles</em>, <em>Chest</em> (Dada), <em>Back</em> (Punggung), <em>Legs</em> (Kaki), <em>Shoulders</em>, <em>Arms</em>, dan <em>Core</em>.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Filter Alat (Equipment)</strong></td>
                                <td>Baris chip horizontal: <em>All Equipment</em>, <em>Barbell</em>, <em>Dumbbell</em>, <em>Cable</em>, <em>Machine</em>, <em>Bodyweight</em>, dll.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Filter Tipe Mekanika</strong></td>
                                <td>Baris chip horizontal: <em>All Types</em>, <em>Compound</em> (Multi-sendi), dan <em>Isolation</em> (Satu sendi/isolasi).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Badge Jumlah Gerakan</strong></td>
                                <td>Menampilkan jumlah total gerakan yang sesuai dengan kombinasi filter aktif secara dinamis (misal: <em>Exercises (1324)</em>).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">6</span></td>
                                <td><strong>Daftar Kartu Gerakan</strong></td>
                                <td>Daftar virtual (LazyColumn) yang menampilkan thumbnail animasi WebP, nama resmi gerakan, tag otot/alat, dan tombol interaksi.</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-tip">
                        <div class="callout-title">💡 KOMBINASI FILTER PRESISI</div>
                        Anda dapat menggabungkan filter otot <em>Chest</em> + alat <em>Barbell</em> + tipe <em>Compound</em> untuk langsung menemukan variasi Bench Press terbaik tanpa perlu mengetik nama gerakan.
                    </div>
                    
                    <div class="callout callout-note">
                        <div class="callout-title">ℹ️ CACHE BITMAP BERPERFORMA TINGGI</div>
                        Animasi thumbnail pada daftar gerakan menggunakan LRU Cache berbasis memori dengan kompresi WebP untuk memastikan scroll tetap mulus 120Hz tanpa membebani RAM ponsel.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 7</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 8: EXERCISE DETAIL & CUSTOM CREATION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-exercise-detail">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.2 — DETAIL & CUSTOM EXERCISE</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.2 Visual Demo Player & Pembuatan Custom Exercise</h2>
            <p>Melihat detail gerakan menyajikan panduan biomekanika visual lengkap dengan pemutar animasi beresolusi tinggi, kontrol kecepatan frame, dan instruksi langkah demi langkah.</p>

            <div class="dual-feature-layout">
                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['06']}" alt="Detail Gerakan & Demo Player">
                        <div class="screenshot-caption">Gambar 6: Layar Detail Gerakan & Pemutar Animasi</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Detail</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Header & Judul</strong></td><td>Nama gerakan dan tombol panah kembali ke katalog.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Pemutar Animasi</strong></td><td>Menampilkan animasi perulangan gerakan dari sudut optimal.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Kontrol Kecepatan</strong></td><td>Tombol Play/Pause dan chip pemilih kecepatan: 0.5x, 1.0x, 1.5x, 2.0x.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Target Otot</strong></td><td>Lencana otot primer dan otot sinergis sekunder.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Spesifikasi Alat</strong></td><td>Alat yang diperlukan dan kategori biomekanika sendi.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">6</span></td><td><strong>Petunjuk Langkah</strong></td><td>Instruksi teks eksekusi gerakan: posisi awal, fase eksentrik, dan konsentrik.</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['06b']}" alt="Formulir Create Exercise">
                        <div class="screenshot-caption">Gambar 7: Pembuatan Gerakan Kustom Baru</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Input</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Tombol Save Gerakan</strong></td><td>Menyimpan gerakan kustom baru ke basis data lokal.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Nama Gerakan</strong></td><td>Kolom teks nama latihan unik yang belum terdaftar.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Pilihan Otot Primer</strong></td><td>Dropdown penentuan kelompok otot utama yang dilatih.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Kategori Alat</strong></td><td>Pilihan jenis alat (Barbell, Dumbbell, Band, dll).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Mekanika Gerakan</strong></td><td>Klasifikasi gerakan: Compound atau Isolation.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">6</span></td><td><strong>Instruksi Eksekusi</strong></td><td>Catatan teknik pribadi, tips posisi kaki, atau cue pernapasan.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="callout callout-tip" style="margin-top:6px;">
                <div class="callout-title">💡 PETUNJUK FRAME ACCURACY</div>
                Pemutar animasi KumaLift menghitung durasi aktual masing-masing frame GIF/WebP (bukan asumsi statis) sehingga ritme tempo repetisi (fase angkat dan turun) terlihat natural sesuai tempo manusia sebenarnya.
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 8</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 9: ACTIVE WORKOUT LOGGER & REST TIMER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-workout-logger">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.3 — ACTIVE WORKOUT LOGGER</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.3 Active Workout Logger: Pelacakan Sesi Real-Time</h2>
            <p>Active Workout Logger adalah antarmuka utama tempat atlet mencatat beban dan repetisi saat berada di lantai gym. Dirancang dengan kontras tinggi dan perataan sel presisi piksel agar mudah dibaca di tengah kelelahan fisik intens.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['08']}" alt="Active Workout Logger dengan Rest Timer">
                        <div class="screenshot-caption">Gambar 8: Active Workout Logger dengan Set Aktif & Rest Timer Mengambang</div>
                    </div>
                </div>
                <div>
                    <h3>Elemen Active Workout Logger</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Komponen UI</th>
                                <th>Perilaku Teknis & Logika Kerja</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Timer Wall-Clock</strong></td>
                                <td>Menghitung waktu sejak workout dimulai (mulai dari 00:00). Dilengkapi tombol interaktif Play/Pause untuk menjeda sesi.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Progress Set Selesai</strong></td>
                                <td>Bilah visual dan teks persentase target set terselesaikan (misal: <em>1 of 3 sets (33%)</em>).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Header Blok Latihan</strong></td>
                                <td>Nama gerakan aktif, badge kategori otot/alat, dan menu konteks (ganti gerakan/hapus gerakan).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Header Tabel Sejajar</strong></td>
                                <td>Kolom sejajar piksel: <code>SET</code>, <code>PREV</code>, <code>KG</code>, <code>REPS</code>, <code>RPE</code>, dan <code>CHECK</code>.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Baris Set Selesai</strong></td>
                                <td>Ditandai dengan latar hijau dan centang terkonfirmasi. Otomatis memicu timer istirahat.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">6</span></td>
                                <td><strong>Baris Set Pending</strong></td>
                                <td>Mengetuk angka KG atau REPS akan membuka dialog input numerik bebas.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">7</span></td>
                                <td><strong>Add Set & Add Exercise</strong></td>
                                <td>Tombol cepat untuk menambah set pada latihan ini atau menyisipkan gerakan tambahan dari katalog.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">8</span></td>
                                <td><strong>Banner Rest Timer</strong></td>
                                <td>Banner istirahat mengambang dengan hitungan mundur detik (misal: 01:19), tombol cepat <strong>-15s</strong>, <strong>+30s</strong>, dan opsi skip.</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-tip">
                        <div class="callout-title">💡 ALIGNMENT HEADER & SEL YANG PRESISI</div>
                        Seluruh judul kolom (SET, PREV, KG, REPS, RPE, CHECK) diselaraskan secara simetris hingga titik tengah piksel yang sama persis dengan angka input di bawahnya, mencegah salah ketuk saat tangan berkeringat.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 9</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 10: CUSTOM KG & REPS INPUT DIALOGS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-custom-input">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.3 — CUSTOM INPUT DIALOG</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.3 Dialog Input Custom KG & Repetisi</h2>
            <p>KumaLift menghapus kebiasaan lama aplikasi fitness yang memaksakan penambahan beban kelipatan 5 kg secara kaku. Dalam hipertrofi ilmiah (<em>micro-loading</em>), penambahan 0.5 kg atau 1.25 kg sangat krusial bagi progres beban jangka panjang.</p>

            <div class="dual-feature-layout">
                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['09']}" alt="Dialog Edit Weight KG">
                        <div class="screenshot-caption">Gambar 9: Dialog Input Beban Desimal Bebas (KG)</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Dialog</th><th>Keterangan</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Judul Dialog</strong></td><td>Teks penanda "Edit Weight (kg)".</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Input Desimal</strong></td><td>Kolom teks angka yang mendukung desimal (contoh: <code>32.5</code> kg).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Tombol Pintas (+/-)</strong></td><td>Chip penyesuaian cepat mikro: -5, -1, +1, dan +5 kg.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Aksi Cancel & Save</strong></td><td>Membatalkan input atau menyimpan beban ke set latihan.</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['10']}" alt="Dialog Edit Repetitions">
                        <div class="screenshot-caption">Gambar 10: Dialog Input Repetisi Bebas</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Dialog</th><th>Keterangan</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Judul Dialog</strong></td><td>Teks penanda "Edit Repetitions".</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Input Reps Integer</strong></td><td>Kolom angka bebas memasukkan repetisi (jangkauan 1 hingga 999).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Tombol Pintas (+/-)</strong></td><td>Chip penyesuaian cepat repetisi: -5, -1, +1, dan +5 reps.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Aksi Cancel & Save</strong></td><td>Menyimpan jumlah repetisi ke set latihan aktif.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="callout callout-important" style="margin-top:6px;">
                <div class="callout-title">⚠️ SISTEM VALIDASI NILAI TIDAK MASUK AKAL</div>
                Input beban KG divalidasi secara ketat antara <strong>0.1 kg hingga 999.9 kg</strong>, sedangkan repetisi divalidasi antara <strong>1 hingga 999</strong>. Sistem akan menolak format karakter non-angka atau nilai negatif guna menjaga integritas basis data statistik.
            </div>

            <h3>Cara Penggunaan:</h3>
            <ol class="step-list">
                <li>Ketuk langsung sel angka <strong>KG</strong> pada baris set yang ingin diubah. Dialog beban akan muncul dengan keyboard numerik aktif.</li>
                <li>Ketik angka desimal yang diinginkan (misal: <code>47.5</code>) atau gunakan tombol <code>+1</code> / <code>+5</code> untuk menaikkan nilai cepat.</li>
                <li>Ketuk <strong>Save</strong>. Nilai beban pada tabel set akan diperbarui seketika dan disimpan di memori sesi.</li>
                <li>Lakukan hal yang sama pada sel <strong>REPS</strong> untuk memasukkan target atau capaian repetisi sebenarnya.</li>
            </ol>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 10</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 11: WORKOUT COMPLETION & PR SUMMARY
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-workout-finish">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.4 — SELESAI & RINGKASAN</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.4 Penyelesaian Sesi, Ringkasan & Deteksi Rekor Baru / PR</h2>
            <p>Ketika atlet menekan tombol <em>Finish</em> di pojok kanan atas, KumaLift menghentikan pencatatan wall-clock, memotong durasi jeda istirahat yang tidak terpakai, dan menyajikan layar audit performa komprehensif.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['12']}" alt="Ringkasan Workout Selesai">
                        <div class="screenshot-caption">Gambar 11: Layar Workout Summary & Selebrasi Rekor Baru (PR)</div>
                    </div>
                </div>
                <div>
                    <h3>Komponen Layar Workout Summary</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Elemen Ringkasan</th>
                                <th>Perhitungan & Makna Statistik</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Header & Lencana RPE</strong></td>
                                <td>Status "WORKOUT COMPLETED" dan badge intensitas rata-rata sesi (contoh: <code>RPE 8.5</code>).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Grid Metrik Utama</strong></td>
                                <td>
                                    • <strong>DURATION:</strong> Durasi aktif aktual (contoh: 25:17).<br>
                                    • <strong>TOTAL VOLUME:</strong> Akumulasi tonase (contoh: 325 kg).<br>
                                    • <strong>COMPLETED SETS:</strong> Rasio set selesai (1 / 3 - 33%).<br>
                                    • <strong>TARGET REST:</strong> Waktu istirahat rata-rata antar-set.
                                </td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Kartu Selebrasi PR</strong></td>
                                <td>Kartu oranye khusus yang menyorot rekor beban tertinggi baru (misal: <em>3/4 Sit-up: 32.5 kg • 10 reps</em>).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Distribusi Fokus Otot</strong></td>
                                <td>Diagram persentase beban yang diserap masing-masing kelompok otot (contoh: <em>Core: 100%</em>).</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Tombol Simpan Sesi</strong></td>
                                <td>Tombol <em>SAVE & FINISH SESSION</em> untuk menyimpan seluruh set ke database dan memperbarui grafik progres.</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-tip">
                        <div class="callout-title">💡 ALGORITMA PERHITUNGAN REKOR (PR)</div>
                        KumaLift membandingkan beban maksimal yang diangkat pada repetisi tertentu dengan seluruh riwayat masa lalu gerakan tersebut. Jika beban saat ini melebihi rekor sebelumnya, lencana emas <strong>PR</strong> otomatis disematkan dan disimpan permanen.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 11</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 12: PROGRESS & ANALYTICS CHARTS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-progress-analytics">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.5 — ANALISIS PROGRES</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.5 Analisis Progresi, Kurva 1RM & Riwayat Latihan</h2>
            <p>Tab <em>Progress</em> menyajikan visualisasi data ilmiah untuk membuktikan keberhasilan prinsip <em>Progressive Overload</em> dari waktu ke waktu. Tidak ada spekulasi atau perkiraan buta; seluruh grafik digambar berdasarkan data latihan riil.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['13']}" alt="Layar Progress & Analytics">
                        <div class="screenshot-caption">Gambar 12: Visualisasi Progres Latihan, 1RM, & Volume Tonnage</div>
                    </div>
                </div>
                <div>
                    <h3>Elemen Layar Analisis Progres</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Elemen Grafik</th>
                                <th>Deskripsi Teknis</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Sub-Tab Metrik</strong></td>
                                <td>Pemisah kategori: <em>History</em> (Daftar Riwayat Sesi), <em>Charts</em> (Grafik Tren), dan <em>1RM & Volume</em>.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Ringkasan Akumulasi</strong></td>
                                <td>Kartu metrik total beban yang berhasil diangkat seumur hidup (Lifetime Tonnage) dan rata-rata durasi latihan.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Grafik Progresi Beban</strong></td>
                                <td>Grafik garis interaktif yang memplot peningkatan estimasi 1RM per gerakan sepanjang minggu dan bulan.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Log Riwayat Terperinci</strong></td>
                                <td>Daftar kronologis sesi lampau yang dapat diperluas (expandable) untuk memeriksa bobot dan reps setiap set individual.</td>
                            </tr>
                        </tbody>
                    </table>

                    <h3>Rumus Estimasi 1RM (One Rep Max)</h3>
                    <p>KumaLift menghitung proyeksi 1RM menggunakan formula standar industri fisiologi olahraga (Formula Epley):</p>
                    <div style="background:#141824; border:1px solid #232A3B; padding:6px 12px; border-radius:5px; font-family:monospace; font-size:8.5pt; color:#FF7043; text-align:center;">
                        1RM = Beban × (1 + 0.0333 × Repetisi)
                    </div>
                    <p style="margin-top:4px;">Formula ini memberikan akurasi tinggi untuk set latihan dengan repetisi antara 1 hingga 10 repetisi.</p>

                    <div class="callout callout-note">
                        <div class="callout-title">ℹ️ TREN VOLUME MINGGUAN (TONNAGE)</div>
                        Memantau total volume (Beban × Reps × Set) adalah indikator terbaik untuk mendeteksi kelelahan berlebih (<em>overreaching</em>) atau waktu yang tepat untuk melakukan siklus <em>Deload</em>.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 12</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 13: ATHLETE PROFILE & MEASURES
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-profile-measures">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN E.6 — PROFIL & PENGUKURAN</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>E.6 Profil Atlet & Pelacakan Pengukuran Tubuh (Measures)</h2>
            <p>Tab <em>Profile</em> (Me) berfungsi sebagai pusat data atlet pribadi. Sesuai spesifikasi rilis stabil, KumaLift telah menghapus nilai default statis (seperti 75 kg / 175 cm) dan menampilkan <strong>Empty State murni</strong> hingga pengguna memasukkan data pengukuran sendiri.</p>

            <div class="dual-feature-layout">
                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['14']}" alt="Dashboard Profile Atlet">
                        <div class="screenshot-caption">Gambar 13: Layar Profil Atlet & Status Kosong Measures</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Profil</th><th>Keterangan</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Header & Settings</strong></td><td>Avatar atlet, nama profil, dan ikon roda gigi Settings.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Kartu Lifetime Stats</strong></td><td>Total Workouts, Rekor PR, dan Tonase Volume Seumur Hidup.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Performa Total</strong></td><td>Ringkasan akumulasi durasi waktu, tonase kg, dan repetisi.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Kartu Measures</strong></td><td>Status kosong: <em>"No measurements yet"</em> (bebas data dummy).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Kalender Latihan</strong></td><td>Heatmap frekuensi sesi mingguan (target 4 hari/minggu).</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['15']}" alt="Modal Pencatatan Measures">
                        <div class="screenshot-caption">Gambar 14: Modal Input Pengukuran Tubuh Baru</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Input Tubuh</th><th>Keterangan</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Judul Modal</strong></td><td>Teks "Record Measurement" sebagai penanda dialog.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Berat & Tinggi</strong></td><td>Kolom input berat badan (kg) dan tinggi badan (cm).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Lingkar Tubuh</strong></td><td>Kolom input lingkar dada (Chest), lengan (Arms), pinggang (Waist), paha (Thighs), dan betis (Calves).</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Tombol Simpan</strong></td><td>Menyimpan entri pengukuran ke basis data lokal.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="callout callout-tip" style="margin-top:6px;">
                <div class="callout-title">💡 TIPS PENGUKURAN KONSISTEN</div>
                Lakukan pencatatan berat dan lingkar badan seminggu sekali di pagi hari setelah bangun tidur dan buang air, sebelum mengonsumsi makanan atau cairan apapun, untuk meminimalkan fluktuasi retensi air.
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 13</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 14: SETTINGS & CUSTOMIZATION
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-settings-main">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN G — PENGATURAN APLIKASI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>G. Pengaturan Aplikasi & Kustomisasi Preferensi</h2>
            <p>Menu Pengaturan (Settings) memberikan kendali menyeluruh atas perilaku operasional KumaLift, mulai dari durasi istirahat bawaan, efek audio, satuan ukuran, hingga bahasa antarmuka.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['16']}" alt="Menu Pengaturan Utama KumaLift">
                        <div class="screenshot-caption">Gambar 15: Menu Pengaturan Utama & Preferensi Sistem</div>
                    </div>
                </div>
                <div>
                    <h3>Struktur Menu Pengaturan</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Menu Pengaturan</th>
                                <th>Dampak Perubahan & Konfigurasi</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Account & Profile</strong></td>
                                <td>Mengatur nama panggilan atlet, bio singkat, dan avatar visual yang tampil di dashboard.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Notifications</strong></td>
                                <td>Pintu masuk menuju pengaturan alarm pengingat latihan dan timer istirahat.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Workouts & Logging</strong></td>
                                <td>Menentukan durasi bawaan Rest Timer (60s, 90s, 120s, atau kustom), auto-start timer, dan getaran haptic.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Units & Formatting</strong></td>
                                <td>Mengubah satuan beban antara <strong>Kilogram (KG)</strong> atau <strong>Pounds (LBS)</strong>, serta format tanggal.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Language (Bahasa)</strong></td>
                                <td>Beralih secara instan antara <strong>Bahasa Indonesia</strong> dan <strong>English</strong> tanpa perlu memuat ulang aplikasi.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">6</span></td>
                                <td><strong>Theme & Appearance</strong></td>
                                <td>Pilihan tema tampilan: Dark Modern atau OLED Pure Black untuk efisiensi baterai layar AMOLED.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">7</span></td>
                                <td><strong>Export & Import Data</strong></td>
                                <td>Pusat pencadangan basis data lokal dan pemulihan (restore) berkas JSON.</td>
                            </tr>
                        </tbody>
                    </table>

                    <div class="callout callout-note">
                        <div class="callout-title">ℹ️ PERSISTENSI PREFERENSI</div>
                        Semua preferensi pengguna disimpan menggunakan enkapsulasi <code>SharedPreferences</code> lokal terstruktur yang bertahan aman saat aplikasi ditutup, diperbarui ke versi baru, atau perangkat direstart.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 14</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 15: NOTIFICATIONS & CUSTOM REMINDER
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-settings-notifications">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN H — PENGINGAT & NOTIFIKASI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>H. Sistem Notifikasi Presisi & Custom Reminder Modern</h2>
            <p>KumaLift mengimplementasikan sistem penjadwalan alarm presisi tinggi menggunakan <code>SCHEDULE_EXACT_ALARM</code> dan <code>setExactAndAllowWhileIdle</code>. Hal ini mengatasi masalah keterlambatan notifikasi (2–3 menit) akibat pembatasan <em>Doze Mode</em> sistem operasi Android, dengan toleransi keterlambatan maksimal di bawah 1 menit.</p>

            <div class="dual-feature-layout">
                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['17']}" alt="Pengaturan Notifikasi KumaLift">
                        <div class="screenshot-caption">Gambar 16: Pengaturan Pengingat & Jadwal Hari Aktif</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Komponen UI</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Rest Timer Sound</strong></td><td>Sakelar nada bel dan getar saat waktu istirahat set habis.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Master Reminder Toggle</strong></td><td>Sakelar utama pengaktifan pengingat latihan harian.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Status Izin Sistem</strong></td><td>Informasi status izin notifikasi dan tautan ke setelan OS.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Preset Jam Cepat</strong></td><td>Pilihan cepat jam pengingat: 06:00, 07:00, 17:00, 18:30, 20:00.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">5</span></td><td><strong>Tombol Custom...</strong></td><td>Membuka dialog keypad penentuan jam dan menit bebas.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">6</span></td><td><strong>Hari Aktif (M-S)</strong></td><td>Chip penanda hari pengingat berbunyi (Senin s/d Minggu).</td></tr>
                        </tbody>
                    </table>
                </div>

                <div>
                    <div class="screenshot-card screenshot-card-mini">
                        <img src="{imgs['18']}" alt="Dialog Custom Reminder Keypad">
                        <div class="screenshot-caption">Gambar 17: Desain Ulang Keypad Jam & Menit Bebas</div>
                    </div>
                    <table class="data-table" style="margin-top:4px;">
                        <thead>
                            <tr><th class="num-col">No</th><th>Elemen Keypad</th><th>Fungsi</th></tr>
                        </thead>
                        <tbody>
                            <tr><td class="num-col"><span class="callout-num">1</span></td><td><strong>Judul Dialog</strong></td><td>Teks "Select Custom Reminder Time".</td></tr>
                            <tr><td class="num-col"><span class="callout-num">2</span></td><td><strong>Kartu Display Jam</strong></td><td>Kartu besar menampilkan Jam (17) dan Menit (00) aktif.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">3</span></td><td><strong>Grid Tombol Angka</strong></td><td>Keypad responsif (1–9, 0, 00, Del) untuk input angka presisi.</td></tr>
                            <tr><td class="num-col"><span class="callout-num">4</span></td><td><strong>Tombol Eksekusi</strong></td><td>Cancel untuk membatalkan, Save untuk menjadwalkan alarm.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <h3>Variasi Pesan Notifikasi Harian (Bilingual)</h3>
            <p>Untuk menjaga motivasi atlet tanpa rasa jenuh, sistem menyertakan 5 variasi pesan motivasi yang dirotasi secara otomatis setiap hari (berdasarkan rumus kalender <code>dayOfYear % 5</code>). Pesan disajikan 100% konsisten dalam bahasa yang sedang dipilih atlet:</p>
            <table class="data-table">
                <thead>
                    <tr><th>Variasi</th><th>Versi Bahasa Indonesia</th><th>Versi Bahasa Inggris</th></tr>
                </thead>
                <tbody>
                    <tr><td><strong>Pesan 1</strong></td><td>Waktunya latihan! Bangun momentum dan capai target hari ini.</td><td>Time to train! Build your momentum and hit today's goals.</td></tr>
                    <tr><td><strong>Pesan 2</strong></td><td>Konsistensi adalah kunci. Buka KumaLift dan taklukkan sesi latihanmu!</td><td>Consistency is key. Open KumaLift and conquer your workout!</td></tr>
                    <tr><td><strong>Pesan 3</strong></td><td>Setiap repetisi membawamu lebih dekat ke target fisik ideal. Ayo mulai!</td><td>Every rep brings you closer to your goals. Let's get moving!</td></tr>
                    <tr><td><strong>Pesan 4</strong></td><td>Jangan lewatkan latihan hari ini! Tubuhmu akan berterima kasih besok.</td><td>Don't skip today's session! Your future self will thank you.</td></tr>
                    <tr><td><strong>Pesan 5</strong></td><td>Waktunya mengangkat beban dan membuat rekor baru (PR) hari ini!</td><td>Time to lift and set new PR records today!</td></tr>
                </tbody>
            </table>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 15</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 16: DATA MANAGEMENT & BACKUP
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-backup-management">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN I — MANAJEMEN DATA</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>I. Manajemen Data Lokal, Ekspor/Impor JSON & Cooldown Reminder</h2>
            <p>Data kebugaran Anda adalah aset pribadi yang sangat sensitif. KumaLift menganut filosofi kedaulatan data mutlak: seluruh riwayat latihan disimpan di dalam database lokal perangkat Anda dan dapat dicadangkan kapan saja ke berkas format terbuka JSON.</p>

            <div class="feature-layout">
                <div>
                    <div class="screenshot-card">
                        <img src="{imgs['19']}" alt="Layar Manajemen Data & Backup">
                        <div class="screenshot-caption">Gambar 18: Layar Ekspor, Impor Data, & Pengaturan Cooldown Backup</div>
                    </div>
                </div>
                <div>
                    <h3>Elemen Layar Manajemen Data</h3>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th class="num-col">No</th>
                                <th>Bagian Layar</th>
                                <th>Mekanisme & Cara Kerja Teknis</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="num-col"><span class="callout-num">1</span></td>
                                <td><strong>Header Data Management</strong></td>
                                <td>Penjelasan peran pencadangan lokal demi keselamatan data riwayat latihan Anda.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">2</span></td>
                                <td><strong>Ekspor Data (Export JSON)</strong></td>
                                <td>Mengompilasi seluruh tabel (Workouts, Sets, Routines, Measures, PR) menjadi satu berkas terenkapsulasi <code>.json</code> yang dapat disimpan ke folder Dokumen atau Google Drive Anda.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">3</span></td>
                                <td><strong>Impor Data (Restore JSON)</strong></td>
                                <td>Memulihkan seluruh data riwayat dari berkas cadangan JSON saat berganti perangkat baru.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">4</span></td>
                                <td><strong>Jaminan Privasi 100%</strong></td>
                                <td>Pernyataan kepatuhan privasi: aplikasi tidak pernah mengirimkan paket data ke server luar.</td>
                            </tr>
                            <tr>
                                <td class="num-col"><span class="callout-num">5</span></td>
                                <td><strong>Pengingat Backup Berkala</strong></td>
                                <td>Sakelar pengingat backup mingguan agar atlet tidak lupa mencadangkan progres latihan rutin.</td>
                            </tr>
                        </tbody>
                    </table>

                    <h3>Mekanisme 7-Day Backup Cooldown (Pencegahan Spam)</h3>
                    <p>Pada versi stabil terbaru, banner pengingat cadangan data (<em>Backup Reminder</em>) di Dashboard Home tidak akan pernah muncul secara menjengkelkan setiap kali aplikasi dibuka. Sistem menerapkan algoritma <strong>7-Day Cooldown</strong>:</p>
                    <ul class="bullet-list">
                        <li>Ketika atlet berinteraksi dengan banner (mengetuk tombol cadangkan atau tombol silang ✕ untuk menutup), timestamp saat itu (epoch milliseconds) disimpan secara permanen di preferensi <code>lastReminderDismissedEpochMs</code>.</li>
                        <li>Saat aplikasi dibuka kembali, sistem menghitung selisih waktu: <code>(Waktu_Sekarang - Waktu_Tutup_Terakhir) &lt; 7 Hari</code>.</li>
                        <li>Jika belum melewati 7 hari penuh (604.800.000 ms), banner pengingat dipastikan 100% tersembunyi, memberikan pengalaman navigasi yang bersih dan tenang.</li>
                    </ul>

                    <div class="callout callout-important">
                        <div class="callout-title">⚠️ SARAN KEAMANAN DATA</div>
                        Karena KumaLift tidak menyimpan data di cloud server, selalu lakukan ekspor data JSON sebulan sekali dan simpan salinannya di tempat aman sebelum melakukan reset pabrik (factory reset) pada ponsel Anda.
                    </div>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 16</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 17: REAL-WORLD WORKFLOWS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-workflows">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN F — ALUR PENGGUNAAN PRAKTIS</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>F. Alur Penggunaan Aplikasi dalam Skenario Nyata</h2>
            <p>Berikut adalah 4 panduan skenario langkah demi langkah yang merefleksikan alur kerja atlet dalam aktivitas sehari-hari di dunia nyata.</p>

            <h3>Skenario 1: Hari Pertama Menggunakan KumaLift (Setup Awal)</h3>
            <ol class="step-list">
                <li>Buka aplikasi KumaLift. Buka tab <strong>Profile</strong> &gt; ketuk ikon <strong>Settings</strong> di pojok kanan atas.</li>
                <li>Atur preferensi satuan bobot (<em>KG</em> atau <em>LBS</em>) dan durasi <em>Rest Timer</em> bawaan yang diinginkan (misal: 90 detik).</li>
                <li>Buka menu <strong>Notifications</strong>, aktifkan <em>Daily Workout Reminder</em>, pilih hari aktif Anda (misal: Sen, Sel, Kam, Jum), dan atur jam pengingat latihan menggunakan keypad kustom.</li>
                <li>Buka tab <strong>Workouts</strong>, ketuk <strong>Create Routine</strong>, beri nama program latihan Anda (misal: "Push Day A"), pilih split <em>PPL Split</em>, tambahkan gerakan bench press dan triceps, lalu simpan.</li>
            </ol>

            <h3>Skenario 2: Menjalankan Sesi Latihan Terjadwal di Gym</h3>
            <ol class="step-list">
                <li>Saat tiba di gym, buka KumaLift. Pada Dashboard Home, kartu <em>Today's Schedule</em> akan menampilkan rutinitas hari ini.</li>
                <li>Ketuk tombol oranye besar <strong>START WORKOUT</strong>. Timer sesi akan mulai berjalan dari detik <code>00:00</code>.</li>
                <li>Lakukan pemanasan. Pada set pertama, ketuk sel <strong>KG</strong> dan masukkan beban latihan aktual (misal: <code>60.0</code> kg). Masukkan repetisi (misal: <code>10</code>).</li>
                <li>Setelah menyelesaikan set, ketuk kotak <strong>CHECK</strong> di kolom paling kanan. Baris set akan berubah menjadi hijau dan banner <strong>Rest Timer</strong> otomatis melayang menghitung mundur waktu istirahat Anda.</li>
                <li>Lanjutkan seluruh set dan latihan hingga tuntas. Ketuk <strong>Finish</strong> di pojok kanan atas, tinjau ringkasan tonase dan rekor PR yang dicapai, lalu ketuk <strong>SAVE &amp; FINISH SESSION</strong>.</li>
            </ol>

            <h3>Skenario 3: Latihan Spontan Tanpa Template (Freestyle Workout)</h3>
            <ol class="step-list">
                <li>Jika Anda ingin berlatih secara bebas tanpa rutinitas kaku, cukup ketuk <strong>START WORKOUT</strong> langsung dari Dashboard Home saat tidak ada jadwal aktif.</li>
                <li>Layar logger kosong akan terbuka. Ketuk tombol <strong>Add Exercise</strong> di bagian tengah layar.</li>
                <li>Pilih gerakan yang diinginkan dari katalog 1.324 gerakan menggunakan filter otot atau kolom pencarian.</li>
                <li>Tambahkan jumlah set sesuka hati dengan tombol <strong>Add Set</strong>, catat beban, dan selesaikan latihan seperti biasa.</li>
            </ol>

            <h3>Skenario 4: Evaluasi Mingguan & Pencadangan Data</h3>
            <ol class="step-list">
                <li>Di akhir pekan, buka tab <strong>Progress</strong> untuk melihat grafik lonjakan estimasi 1RM dan akumulasi tonase mingguan.</li>
                <li>Buka tab <strong>Profile</strong>, ketuk kartu <strong>Measures</strong>, lalu masukkan berat badan dan lingkar tubuh terbaru.</li>
                <li>Buka <strong>Settings &gt; Export &amp; Import Data</strong>, ketuk <strong>Export JSON Data</strong>, dan simpan berkas cadangan ke Google Drive atau memori eksternal.</li>
            </ol>

            <div class="callout callout-tip">
                <div class="callout-title">💡 TIPS EFISIENSI LATIHAN</div>
                Jika Anda terpaksa beristirahat sejenak untuk menjawab telepon saat sesi aktif, ketuk tombol <strong>Pause</strong> pada kotak timer agar catatan durasi efektif sesi latihan Anda tetap murni tanpa bias waktu istirahat non-latihan.
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 17</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 18: TROUBLESHOOTING & FAQ
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-troubleshooting">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN J — PEMECAHAN MASALAH & FAQ</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>J. Panduan Pemecahan Masalah & Pertanyaan Umum (FAQ)</h2>
            <p>Halaman ini merangkum diagnosis teknis dan solusi praktis atas kendala operasional yang mungkin ditemui pengguna.</p>

            <h3>Panduan Pemecahan Masalah (Troubleshooting)</h3>
            <table class="data-table">
                <thead>
                    <tr><th style="width:25%;">Gejala Masalah</th><th style="width:35%;">Penyebab Utama</th><th>Solusi Langkah Demi Langkah</th></tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Notifikasi Pengingat Terlambat atau Tidak Muncul</strong></td>
                        <td>Fitur penghemat daya agresif (Battery Optimization) menahan eksekusi alarm saat ponsel dalam keadaan Doze (layar mati lama).</td>
                        <td>Buka <em>Pengaturan Ponsel &gt; Aplikasi &gt; KumaLift &gt; Baterai</em>, lalu ubah dari "Dioptimalkan" menjadi <strong>"Tidak Dibatasi" (Unrestricted)</strong>. Pastikan juga izin <em>Alarms &amp; Reminders</em> aktif pada Android 12 ke atas.</td>
                    </tr>
                    <tr>
                        <td><strong>Suara Bel Rest Timer Tidak Berbunyi</strong></td>
                        <td>Volume media ponsel dalam kondisi senyap (mute) atau izin audio latar belakang dibatasi oleh mode fokus/DND.</td>
                        <td>Pastikan tombol volume media perangkat dinaikkan (bukan hanya volume nada dering). Periksa apakah sakelar <em>Rest Timer Sound</em> pada Settings &gt; Notifications dalam kondisi aktif (ON).</td>
                    </tr>
                    <tr>
                        <td><strong>Beban Latihan Terpencet Tidak Sengaja</strong></td>
                        <td>Pengguna menyenggol angka tabel saat meletakkan ponsel di kantong atau lantai.</td>
                        <td>Nilai beban tidak akan berubah jika dialog tidak dibuka dan tombol <em>Save</em> tidak ditekan. Anda dapat mengetuk kembali sel bersangkutan kapan saja untuk mengembalikan nilai sebelumnya.</td>
                    </tr>
                    <tr>
                        <td><strong>Gagal Memulihkan (Restore) File Backup JSON</strong></td>
                        <td>Format file rusak akibat pengeditan manual atau versi file JSON berasal dari aplikasi lain.</td>
                        <td>Pastikan file JSON yang diimpor adalah file hasil ekspor asli dari KumaLift. Jangan mengubah struktur kurung kurawal atau nama field di dalam file teks JSON secara manual.</td>
                    </tr>
                </tbody>
            </table>

            <h3>Pertanyaan yang Sering Diajukan (FAQ)</h3>
            <div style="font-size:7.8pt; line-height:1.4;">
                <p><strong>T: Apakah KumaLift membutuhkan koneksi internet untuk memutar animasi gerakan?</strong><br>
                <em>J:</em> Tidak. Seluruh 1.324+ animasi WebP dan metadata latihan sudah tersimpan terkompresi di dalam paket instalasi aplikasi (APK). Anda dapat menggunakannya di basement gym tanpa sinyal seluler sedikit pun.</p>

                <p><strong>T: Mengapa saya tidak menemukan program latihan bawaan saat pertama kali menginstal?</strong><br>
                <em>J:</em> KumaLift menghapus seluruh template kaku bawaan demi fleksibilitas maksimal atlet. Anda dipersilakan menyusun routine kustom sendiri melalui menu <em>Workouts &gt; Create Routine</em>.</p>

                <p><strong>T: Bagaimana jika saya ingin mengganti ponsel tanpa kehilangan riwayat latihan?</strong><br>
                <em>J:</em> Buka menu <em>Settings &gt; Export &amp; Import Data</em> pada ponsel lama, ketuk <em>Export Data</em> dan kirim file JSON ke ponsel baru. Di ponsel baru, instal KumaLift lalu pilih <em>Import Data</em>.</p>

                <p><strong>T: Apakah KumaLift memungut biaya langganan bulanan di masa depan?</strong><br>
                <em>J:</em> Tidak. KumaLift menganut filosofi transparansi dan kebebasan open-source. Semua fitur inti tersedia penuh tanpa sistem paywall atau langganan.</p>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 18</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 19: GLOSSARY & RPE REFERENCE TABLE
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-glossary">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN K — GLOSARIUM & REFERENSI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content">
            <h2>K. Glosarium Istilah Fitness & Tabel Referensi RPE</h2>
            <p>Kamus referensi terminologi teknis kebugaran dan angkat beban untuk memudahkan pemahaman atlet pemula maupun profesional.</p>

            <h3>Glosarium Istilah Utama</h3>
            <table class="data-table">
                <thead>
                    <tr><th style="width:20%;">Istilah</th><th style="width:25%;">Definisi Formal</th><th>Makna Praktis di KumaLift</th></tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>1RM (One Rep Max)</strong></td>
                        <td>Beban maksimal teoritis atau aktual yang mampu diangkat seseorang untuk 1 repetisi penuh.</td>
                        <td>Dihitung otomatis oleh KumaLift menggunakan Formula Epley untuk mengukur peningkatan kekuatan absolut.</td>
                    </tr>
                    <tr>
                        <td><strong>RPE</strong></td>
                        <td><em>Rate of Perceived Exertion</em> — Skala 1–10 untuk mengukur tingkat kepayahan atau intensitas subjektif atlet.</td>
                        <td>Diinput pada setiap set latihan untuk memastikan atlet berlatih dekat dengan titik kegagalan otot (failure) tanpa cedera.</td>
                    </tr>
                    <tr>
                        <td><strong>RIR</strong></td>
                        <td><em>Reps In Reserve</em> — Jumlah repetisi cadangan yang masih bisa dilakukan sebelum otot benar-benar gagal mengangkat.</td>
                        <td>Kebalikan langsung dari RPE (misal: RPE 8 setara dengan 2 RIR; artinya atlet masih mampu melakukan 2 reps lagi).</td>
                    </tr>
                    <tr>
                        <td><strong>Tonase (Volume Load)</strong></td>
                        <td>Total beban kerja yang dihasilkan dari perkalian: <code>Beban (kg) × Repetisi × Set</code>.</td>
                        <td>Metrik utama penentu hipertrofi otot yang dirangkum KumaLift per sesi dan per kelompok otot mingguan.</td>
                    </tr>
                    <tr>
                        <td><strong>Progressive Overload</strong></td>
                        <td>Prinsip peningkatan stimulus fisik bertahap (beban, reps, atau volume) dari waktu ke waktu.</td>
                        <td>Tujuan inti penggunaan KumaLift: memandu atlet selalu melampaui catatan rekor sesi sebelumnya.</td>
                    </tr>
                    <tr>
                        <td><strong>Compound vs Isolation</strong></td>
                        <td>Gerakan multi-sendi (melibatkan banyak otot) vs gerakan isolasi satu sendi (fokus pada 1 otot spesifik).</td>
                        <td>Dapat disaring melalui filter mekanika pada tab katalog latihan KumaLift.</td>
                    </tr>
                </tbody>
            </table>

            <h3>Tabel Panduan Skala RPE vs RIR</h3>
            <table class="data-table">
                <thead>
                    <tr>
                        <th style="width:12%; text-align:center;">Skala RPE</th>
                        <th style="width:15%; text-align:center;">Nilai RIR</th>
                        <th style="width:30%;">Tingkat Kepayahan Fisik</th>
                        <th>Rekomendasi Penggunaan Latihan</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="text-align:center;"><span class="badge badge-gray">RPE 6.0</span></td>
                        <td style="text-align:center;">4 RIR</td>
                        <td>Beban sangat ringan, kecepatan angkat sangat tinggi.</td>
                        <td>Pemanasan spesifik, latihan kecepatan (speed work), atau sesi pemulihan aktif (deload).</td>
                    </tr>
                    <tr>
                        <td style="text-align:center;"><span class="badge badge-blue">RPE 7.0</span></td>
                        <td style="text-align:center;">3 RIR</td>
                        <td>Beban moderat, mulai terasa ada usaha, bisa 3 reps lagi.</td>
                        <td>Set kerja awal pada blok latihan akumulasi volume tinggi.</td>
                    </tr>
                    <tr>
                        <td style="text-align:center;"><span class="badge badge-green">RPE 8.0</span></td>
                        <td style="text-align:center;">2 RIR</td>
                        <td>Beban berat, kecepatan bar melambat, tersisa 2 reps solid.</td>
                        <td><strong>Sweet spot hipertrofi standar:</strong> menstimulasi serat otot optimal dengan kelelahan saraf terkontrol.</td>
                    </tr>
                    <tr>
                        <td style="text-align:center;"><span class="badge badge-orange">RPE 9.0</span></td>
                        <td style="text-align:center;">1 RIR</td>
                        <td>Sangat berat, perjuangan keras, hanya bisa 1 repetisi lagi.</td>
                        <td>Set penutup pada latihan beban utama atau set latihan isolasi akhir.</td>
                    </tr>
                    <tr>
                        <td style="text-align:center;"><span class="badge badge-orange" style="background:#B91C1C; color:#FFF; border-color:#EF4444;">RPE 10.0</span></td>
                        <td style="text-align:center;">0 RIR (Failure)</td>
                        <td>Kegagalan absolut, tidak ada lagi repetisi yang bisa dilakukan.</td>
                        <td>Tes kekuatan maksimal (PR attempt). Batasi frekuensinya agar sistem saraf tidak kelelahan.</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 19</span>
        </div>
    </div>
    """)

    # ==========================================
    # PAGE 20: CLOSING & RELEASE SPECIFICATIONS
    # ==========================================
    pages.append(f"""
    <div class="page" id="page-closing">
        <div class="page-header">
            <span class="brand">🐻 KUMALIFT MANUAL</span>
            <span class="section-title">BAGIAN L — PENUTUP & SPESIFIKASI</span>
            <span>EDISI RESMI V1.0</span>
        </div>
        <div class="page-content" style="justify-content:space-between;">
            <div>
                <h2>L. Penutup & Catatan Rilis Dokumentasi</h2>
                <p>Terima kasih telah memilih <strong>KumaLift</strong> sebagai pendamping setia perjalanan transformasi fisik dan kekuatan Anda. Aplikasi ini dibangun dengan dedikasi tinggi terhadap integritas data, etika privasi, dan kepuasan binaragawan sejati.</p>
                
                <h3>Spesifikasi Teknis Rilis Stabil v1.0.0</h3>
                <table class="data-table">
                    <thead>
                        <tr><th style="width:30%;">Parameter Sistem</th><th>Spesifikasi Implementasi Aktual</th></tr>
                    </thead>
                    <tbody>
                        <tr><td><strong>Nama Aplikasi</strong></td><td>KumaLift (The Elite Hypertrophy Workout Logger)</td></tr>
                        <tr><td><strong>Versi Stabil</strong></td><td>1.0.0 (Build Stabil Produksi)</td></tr>
                        <tr><td><strong>Arsitektur Bahasa</strong></td><td>Kotlin Multiplatform (KMP) • Jetpack Compose Multiplatform</td></tr>
                        <tr><td><strong>Mesin Basis Data</strong></td><td>Local SQLDelight / SQLite Engine (100% Offline &amp; On-Device)</td></tr>
                        <tr><td><strong>Katalog Gerakan</strong></td><td>1.324+ Gerakan Terverifikasi Biomekanika dengan WebP Animation</td></tr>
                        <tr><td><strong>Sistem Penjadwalan</strong></td><td>Android Exact Alarm Scheduler (<code>SCHEDULE_EXACT_ALARM</code> API 26-34+)</td></tr>
                        <tr><td><strong>Manajemen State UI</strong></td><td>Single Source of Truth, Hoisted LazyListState, Saver Restoration</td></tr>
                        <tr><td><strong>Kompabilitas Platform</strong></td><td>Android 8.0 (Oreo / API 26) hingga Android 15+</td></tr>
                        <tr><td><strong>Format Pencadangan</strong></td><td>Standar Terbuka Universal JSON Data Serialization</td></tr>
                    </tbody>
                </table>

                <div class="callout callout-tip" style="margin-top:10px;">
                    <div class="callout-title">💡 KOMITMEN PENGEMBANG</div>
                    KumaLift berkomitmen untuk selalu menghormati privasi atlet: tidak ada iklan paksa, tidak ada pelacakan lokasi, dan tidak ada penguncian fitur esensial di balik sistem langganan. Aplikasi ini adalah milik Anda seutuhnya.
                </div>
            </div>

            <div style="background:#141822; border:1px solid #232D3F; border-radius:8px; padding:12px; text-align:center; margin-top:10px;">
                <div style="font-size:11pt; font-weight:700; color:#FFFFFF; margin-bottom:4px;">SELAMAT BERLATIH & TAKLUKKAN SETIAP SESI!</div>
                <div style="font-size:8pt; color:#94A3B8; max-width:130mm; margin:0 auto;">
                    "Konsistensi dalam mengangkat beban bukan sekadar membentuk otot, melainkan menempa disiplin, ketangguhan mental, dan karakter hidup sejati."
                </div>
                <div style="margin-top:10px; font-size:7.5pt; color:#64748B;">
                    Dokumentasi Disusun Oleh <strong>BearBones Engineering &amp; QA Team</strong> • Hak Cipta © 2026 KumaLift Project.
                </div>
            </div>
        </div>
        <div class="page-footer">
            <span>KumaLift User Manual • Edisi 1.0.0</span>
            <span>Halaman 20 (Selesai)</span>
        </div>
    </div>
    """)

    # Wrap in complete HTML structure
    html_content = f"""<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="utf-8">
<title>KumaLift Official User Manual & Technical Guide</title>
<style>
{css}
</style>
</head>
<body>
{''.join(pages)}
</body>
</html>
"""
    with open(OUT_HTML, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"Generated HTML successfully: {OUT_HTML} ({len(html_content)} bytes, {len(pages)} pages)")

if __name__ == "__main__":
    build_html()
