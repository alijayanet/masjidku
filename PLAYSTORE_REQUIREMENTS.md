# 🚀 Panduan & Persyaratan Unggah Google Play Console - MasjidKU TV

Dokumen ini memandu Anda langkah demi langkah dalam menyiapkan aset grafis, mengisi kuesioner kebijakan, dan mengunggah berkas **Android App Bundle (.aab)** ke **Google Play Console**.

---

## 🎨 1. Daftar Kebutuhan Aset Grafis (Telah Digenerate Otomatis!)

Seluruh aset gambar wajib di bawah ini **telah digenerate dengan resolusi presisi 1:1** dan tersimpan rapi di dalam folder:  
📁 [`playstore_assets/`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets)

| Aset Grafis | Dimensi Wajib | Berkas yang Tersedia | Status |
| :--- | :--- | :--- | :--- |
| **Ikon Aplikasi (App Icon)** | `512 x 512 px` | [`playstore_assets/app_icon_512x512.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/app_icon_512x512.png) | ✅ Siap Upload |
| **Gambar Unggulan (Feature Graphic)** | `1024 x 500 px` | [`playstore_assets/feature_graphic_1024x500.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/feature_graphic_1024x500.png) | ✅ Siap Upload |
| **Banner Android TV (TV Banner)** | `1280 x 720 px` | [`playstore_assets/tv_banner_1280x720.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/tv_banner_1280x720.png) | ✅ Siap Upload |
| **Tangkapan Layar 1 (Grand Royal Mihrab)** | `1920 x 1080 px` | [`playstore_assets/tv_screenshot_1_1920x1080.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/tv_screenshot_1_1920x1080.png) | ✅ Siap Upload |
| **Tangkapan Layar 2 (Cordoba Moorish)** | `1920 x 1080 px` | [`playstore_assets/tv_screenshot_2_1920x1080.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/tv_screenshot_2_1920x1080.png) | ✅ Siap Upload |
| **Tangkapan Layar 3 (Web Remote HP)** | `1920 x 1080 px` | [`playstore_assets/tv_screenshot_3_1920x1080.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/tv_screenshot_3_1920x1080.png) | ✅ Siap Upload |
| **Tangkapan Layar 4 (Modern Minimalist)** | `1920 x 1080 px` | [`playstore_assets/tv_screenshot_4_1920x1080.png`](file:///C:/Users/DELL/antigravity/MasjidKU-TV/playstore_assets/tv_screenshot_4_1920x1080.png) | ✅ Siap Upload |

> [!TIP]
> **Saran Tangkapan Layar (Screenshots) yang Menarik:**
> 1. Tangkapan Layar Tema 7: *Grand Royal Mihrab* (Kubah Kurung Kurawa Emas Ganda + Kanopi Hijau Zamrud)
> 2. Tangkapan Layar Tema 8: *Cordoba Moorish* (Lengkung Tapal Kuda Andalusia)
> 3. Tangkapan Layar Slide Laporan Kas Keuangan & Petugas Sholat Jum'at
> 4. Tangkapan Layar Infaq QRIS Donasi Digital
> 5. Tangkapan Layar Tampilan Web Remote Smartphone (menunjukkan kemudahan setting via HP)

---

## 📋 2. Kuesioner Kebijakan Google Play (App Content & Policy)

Di Google Play Console, masuk ke menu **Konten Aplikasi (*App Content*)** di bilah navigasi kiri bawah, lalu isi formulir berikut:

### A. Kebijakan Privasi (Privacy Policy)
- Masukkan tautan/URL Kebijakan Privasi Anda (Gunakan isi dokumen `PRIVACY_POLICY.md` yang telah dibuat, unggah ke GitHub Pages, Google Sites, atau website masjid Anda).

### B. Akses Aplikasi (App Access)
- Pilih: **"Semua fungsionalitas tersedia tanpa batasan akses"** (*All functionality is available without special access restrictions*).
- Aplikasi MasjidKU TV tidak memerlukan akun login atau langganan berbayar.

### C. Iklan (Ads)
- Pilih: **"Tidak, aplikasi saya tidak berisi iklan"** (*No, my app does not contain ads*).

### D. Rating Konten (Content Rating)
- Mulai kuesioner rating baru.
- Kategori: Pilih **"Semua Jenis Aplikasi Lainnya / Utilitas"** (*Utility, Productivity, Communication, or Other*).
- Jawab **"Tidak"** untuk semua pertanyaan terkait kekerasan, konten seksual, ujaran kebencian, atau perjudian.
- Hasil rating: Aplikasi akan mendapatkan sertifikasi **PEGI 3 / Everyone (Semua Umur)**.

### E. Audiens Target & Konten (Target Audience)
- Kelompok Umur: Centang **18 tahun ke atas** atau **Semua Umur**.
- Banding untuk Anak-anak: Pilih **"Tidak"** (aplikasi bukan ditujukan secara spesifik sebagai game edukasi anak, melainkan digital signage masjid umum).

### F. Keamanan Data (Data Safety Declaration)
Ini adalah bagian penting yang diawasi ketat oleh Google:
- **Apakah aplikasi Anda mengumpulkan atau membagikan data pengguna?**  
  👉 Pilih: **"TIDAK"** (*No, this app does not collect or share user data*).
- Semua data konfigurasi (nama masjid, kas, QRIS) disimpan 100% secara lokal di dalam TV pengguna.

### G. Deklarasi Aplikasi Berita, Finansial & COVID-19
- Pilih: **Bukan aplikasi berita, bukan aplikasi pinjaman keuangan/perbankan resmi, dan bukan aplikasi pelacak COVID-19**.

---

## 📺 3. Menambahkan Tipe Perangkat Android TV

Di Google Play Console:
1. Masuk ke **Penyiapan (*Setup*)** > **Pengaturan Lanjutan (*Advanced settings*)**.
2. Buka tab **Faktor Bentuk (*Form factors*)**.
3. Klik **Tambahkan faktor bentuk (*Add form factor*)**, lalu pilih **Android TV**.
4. Setujui ketentuan persyaratan aplikasi TV.
5. Unggah **Android TV Banner (1280x720 atau 320x180 px)** dan tangkapan layar TV.

---

## 📦 4. Mengunggah Berkas Android App Bundle (.aab)

1. Di menu kiri Play Console, buka **Rilis (*Release*)** > **Produksi (*Production*)** (atau *Closed Testing* jika ingin uji coba tertutup terlebih dahulu).
2. Klik tombol **Buat Rilis Baru (*Create new release*)**.
3. Pada bagian **App Bundle**, klik tombol **Unggah (*Upload*)**.
4. Pilih berkas bundle rilis:
   ```text
   app\build\outputs\bundle\release\MasjidKU-TV.aab
   (atau app-release.aab)
   ```
5. Beri **Nama Rilis**: `1.0.0 (1) - Grand Royal Mihrab & Multi-Theme Edition`.
6. Salin teks dari berkas `RELEASE_NOTES.md` ke dalam kotak **Catatan Rilis (*Release Notes*)**.
7. Klik **Simpan (*Save*)**, kemudian klik **Tinjau Rilis (*Review release*)**.
8. Jika tidak ada peringatan kesalahan kritis, klik **Mulai Peluncuran ke Produksi (*Start rollout to Production*)**!
9. Aplikasi Anda akan ditinjau oleh tim Google (biasanya memakan waktu 1 hingga 3 hari kerja).
