# UI/UX DESIGN & PRODUCT QUALITY STANDARD – KUMALIFT

Setiap pengembangan, penambahan fitur, maupun perbaikan pada KumaLift wajib memperhatikan kualitas UI/UX secara menyeluruh.

Jangan hanya berfokus pada fungsionalitas dan logic. Setiap implementasi harus memiliki desain yang matang, penempatan fitur yang tepat, interaksi yang intuitif, serta konsistensi visual dengan keseluruhan aplikasi.

Tujuan utama adalah menjadikan KumaLift sebagai aplikasi fitness yang terasa seperti produk komersial profesional, bukan sekadar kumpulan fitur yang berfungsi.

---

## 1. User Experience & Feature Placement

Sebelum mengimplementasikan fitur apa pun, tentukan terlebih dahulu pengalaman penggunaan yang paling tepat.

Perhatikan:
- Di halaman mana fitur seharusnya ditempatkan?
- Apakah fitur lebih cocok berada di Home, Workout Log, Exercise Library, Routine, History, atau Settings?
- Apakah user dapat menemukan fitur dengan mudah?
- Apakah fitur membutuhkan halaman tersendiri, dialog, bottom sheet, atau cukup menjadi bagian dari halaman yang sudah ada?
- Apakah alur interaksinya sederhana dan efisien?
- Apakah penempatan fitur mengganggu fungsi utama halaman?

> **Aturan**: Jangan menambahkan komponen UI secara sembarangan hanya karena sebuah fitur membutuhkan akses. Setiap fitur harus ditempatkan berdasarkan konteks, kebutuhan, dan kebiasaan pengguna.

---

## 2. Visual Design & Design System

Pertahankan identitas visual KumaLift dan pastikan seluruh halaman memiliki bahasa desain yang konsisten.

Perhatikan:
- Color palette (`LocalKumaColors.current`, `KumaColors`).
- Typography dan font hierarchy (`MaterialTheme.typography`).
- Font size dan font weight.
- Spacing dan padding (skala 4dp / 8dp / 12dp / 16dp / 20dp).
- Border radius (`PillShape`, `RoundedCornerShape(12.dp)`, `16.dp`).
- Iconography (`Icons.Default.*` standard).
- Button styles.
- Input components.
- Card components (`KumaCard`).
- Navigation (`KumaBottomNavigation`, `KumaBackHandler`).
- Dialog dan bottom sheet.
- Visual feedback (ripple, focus, active states).

> **Aturan**: Gunakan design system yang konsisten dan hindari membuat gaya komponen baru tanpa kebutuhan yang jelas.

---

## 3. Professional & Premium Appearance

Hindari tampilan aplikasi yang terlihat seperti AI-generated UI, template dashboard generik, atau hasil implementasi yang hanya mengejar estetika artifisial.

**Hindari**:
- Gradient berlebihan.
- Glassmorphism yang tidak diperlukan.
- Glow dan shadow berlebihan.
- Terlalu banyak card bersarang (nested cards hell).
- Penggunaan warna aksen secara berlebihan.
- Emoji sebagai pengganti icon profesional.
- Statistik dekoratif tanpa fungsi nyata.
- Layout yang terlalu padat (cramped).
- Typography yang tidak konsisten.
- Elemen visual yang tidak memiliki tujuan.

**Prioritaskan desain yang**:
- **Clean**: Rapi, tidak ada elemen mengganggu mata.
- **Minimalist**: Esensial, to-the-point.
- **Professional**: Industrial standard gym tool look.
- **Functional**: Setiap elemen dapat digunakan.
- **Consistent**: Bahasa visual seragam di seluruh tab.
- **Purposeful**: Setiap elemen visual memiliki alasan keberadaannya.

---

## 4. Functional UI & Interaction Design

Pastikan setiap komponen memiliki perilaku yang jelas dan sesuai ekspektasi pengguna.

Perhatikan:
- Button states (normal, pressed, disabled).
- Press feedback (haptic / ripple / color change).
- Loading states.
- Empty states (instruktif, ada call-to-action yang jelas).
- Error states.
- Success feedback.
- Confirmation dialogs (untuk aksi destruktif).
- Form validation.
- Navigation transitions.
- Scroll behavior.
- Touch target size (minimal 48dp untuk tombol utama).
- State restoration.

> **Aturan**: Jangan membuat tombol atau elemen yang hanya terlihat interaktif tetapi tidak memiliki fungsi. Hindari interaksi yang membutuhkan terlalu banyak langkah untuk menyelesaikan tugas sederhana.

---

## 5. Mobile-First & Real-World Usability

KumaLift merupakan aplikasi mobile yang digunakan dalam aktivitas latihan sehari-hari (di gym, tangan berkeringat, terburu-buru antar set).

Karena itu:
- Optimalkan tampilan untuk penggunaan satu tangan (one-handed operation).
- Pastikan tombol mudah disentuh (target sentuh besar & tegas).
- Pastikan informasi penting mudah dibaca (angka beban, reps, timer sisa).
- Hindari elemen yang terlalu kecil (< 40dp tap target).
- Pastikan layout responsif terhadap berbagai ukuran layar (layar compact maupun layar tinggi).
- Hindari komponen yang saling bertumpuk.
- Pastikan keyboard tidak menutupi input penting (`imePadding`, `WindowInsets`).
- Perhatikan system bars dan safe areas (`statusBarsPadding`, `navigationBarsPadding`).
- Pastikan scrolling dan navigasi terasa natural.

> **Aturan**: Prioritaskan kenyamanan penggunaan nyata di gym dibandingkan dekorasi visual.

---

## 6. Information Architecture & Visual Hierarchy

Setiap halaman harus memiliki struktur informasi yang jelas.

- Informasi utama harus lebih menonjol daripada informasi sekunder.
- Kelompokkan informasi yang saling berkaitan.
- Hindari pengulangan informasi.
- Gunakan whitespace secara proporsional.
- Jangan memenuhi layar dengan elemen yang tidak diperlukan.
- Pastikan user memahami fungsi halaman tanpa harus menebak-nebak.
- Jangan memaksakan layout yang sama untuk semua jenis halaman.

> **Aturan**: Setiap halaman harus memiliki struktur yang sesuai dengan tujuan penggunaannya.

---

## 7. UI/UX Quality Assurance

Setiap implementasi fitur baru dan perbaikan fitur lama wajib melalui pemeriksaan UI/UX.

Periksa:
- Apakah penempatan fitur sudah tepat?
- Apakah visual hierarchy sudah jelas?
- Apakah spacing dan alignment konsisten?
- Apakah typography mudah dibaca?
- Apakah terdapat text overflow?
- Apakah terdapat layout shift?
- Apakah komponen saling bertumpuk?
- Apakah navigasi mudah dipahami?
- Apakah seluruh state memiliki feedback visual?
- Apakah tampilan konsisten pada berbagai ukuran layar?

> **Aturan**: Jika tersedia emulator atau perangkat pengujian, periksa hasil rendering aktual. Jangan menyimpulkan kualitas UI hanya berdasarkan source code. Lakukan visual review setelah implementasi dan perbaiki temuan yang ada.

---

## 8. Final Product Quality Standard

Setiap pekerjaan harus memenuhi tiga pilar:
1. **Functional Quality**: Seluruh fitur benar-benar berfungsi.
2. **Logical Quality**: Data, perhitungan, state, dan alur aplikasi berjalan dengan benar.
3. **Design Quality**: Tampilan, penempatan, navigasi, dan interaksi memberikan pengalaman pengguna yang baik.

> **Aturan**: Jangan mengorbankan salah satu aspek demi menyelesaikan pekerjaan lebih cepat.

---

## PRINCIPAL DIRECTIVE

Setiap kali mengembangkan atau memperbaiki KumaLift, selalu pikirkan aplikasi sebagai satu produk utuh, bukan sekadar kumpulan komponen dan fitur.

Jangan hanya bertanya apakah sebuah fitur bisa dibuat dan berfungsi. Pertimbangkan juga apakah fitur tersebut mudah ditemukan, mudah digunakan, ditempatkan dengan benar, dan memiliki tampilan yang sesuai dengan keseluruhan identitas KumaLift.

Target akhirnya adalah aplikasi fitness yang **ringan, intuitif, profesional, konsisten, dan nyaman digunakan dalam aktivitas sehari-hari**.
