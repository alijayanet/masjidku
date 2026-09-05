# 🔒 Kebijakan Privasi (Privacy Policy) - MasjidKU TV

**Terakhir Diperbarui:** 30 Agustus 2026  
**Aplikasi:** MasjidKU TV  
**Pengembang:** DKM Digital Signage / Tim Pengembang MasjidKU  
**Kontak:** support@masjidku.id / dkm@masjidku.id  

---

## 1. Pendahuluan
Aplikasi **MasjidKU TV** ("kami", "aplikasi") berkomitmen untuk melindungi privasi pengguna dan jamaah masjid. Dokumen Kebijakan Privasi ini menjelaskan bagaimana aplikasi kami mengelola, menggunakan, dan melindungi data saat Anda menggunakan aplikasi MasjidKU TV pada perangkat Android TV, Smart TV Box, atau perangkat Android lainnya.

Kami menganut prinsip **Privasi Berkelanjutan & Tanpa Pelacakan (*Zero Tracking*)**. Aplikasi ini dirancang untuk operasional masjid yang aman, amanah, dan mandiri.

---

## 2. Informasi yang Kami Kumpulkan
**Kami TIDAK mengumpulkan, memperjualbelikan, atau membagikan data pribadi pengguna kepada pihak ketiga mana pun.**

Rincian pemrosesan data pada aplikasi adalah sebagai berikut:

### A. Data Konfigurasi Masjid
- **Jenis Data**: Nama masjid, alamat/kota, teks maklumat, pengumuman running text, rincian petugas sholat Jum'at (Khatib, Imam, Muadzin), laporan kas keuangan, dan gambar kode QRIS donasi.
- **Penyimpanan**: **100% Disimpan Secara Lokal** di penyimpanan internal perangkat TV Anda melalui basis data SQLite/Room terenkripsi sistem Android. Data ini tidak pernah dikirimkan atau diunggah ke server cloud eksternal kami.

### B. Gambar QRIS & Logo Masjid
- Gambar barcode QRIS dan logo masjid yang diunggah oleh pengurus melalui Web Remote HP hanya disimpan di direktori internal aplikasi (`context.filesDir/qris_images`). File ini hanya digunakan untuk ditampilkan pada layar TV Anda sendiri.

### C. Data Lokasi & Waktu
- Koordinat lintang/bujur atau nama kota yang Anda pilih digunakan secara lokal oleh algoritma perhitungan astronomi hisab untuk menghitung jadwal sholat yang akurat. Kami tidak melacak lokasi fisik perangkat Anda secara real-time.

---

## 3. Izin Perangkat (Permissions) yang Digunakan
Aplikasi MasjidKU TV hanya meminta izin yang mutlak diperlukan untuk fungsionalitas utama aplikasi:

| Izin (*Permission*) | Tujuan Penggunaan |
| :--- | :--- |
| `android.permission.INTERNET` | Mengunduh jadwal sholat resmi dari API Kemenag RI (opsional saat terhubung) dan mengaktifkan Web Server lokal untuk remote control HP. |
| `android.permission.ACCESS_NETWORK_STATE` | Memeriksa ketersediaan koneksi jaringan Wi-Fi lokal. |
| `android.permission.ACCESS_WIFI_STATE` | Mendapatkan alamat IP lokal TV (misalnya `192.168.1.15:8080`) untuk ditampilkan di layar agar pengurus dapat membuka Web Remote. |
| `android.permission.WAKE_LOCK` | Menjaga layar TV tetap menyala selama jam operasional masjid agar informasi sholat dan iqomah tidak padam tiba-tiba. |

---

## 4. Keamanan Jaringan Lokal (Local Web Remote)
- Fitur Web Remote menggunakan server HTTP internal yang hanya berjalan di dalam jaringan area lokal (Local Area Network / Wi-Fi yang sama).
- Tidak ada pintu belakang (*backdoor*), pelacakan internet publik, atau lalu lintas data yang keluar dari jaringan lokal masjid Anda.

---

## 5. Layanan Pihak Ketiga
Aplikasi MasjidKU TV **TIDAK** menyertakan:
- SDK Iklan (Google AdMob, Unity, Facebook Ads, dll.).
- SDK Pelacak Perilaku Pengguna (Google Analytics for Firebase, AppsFlyer, dll.).
- Layanan pengenalan wajah atau perekaman suara.

Aplikasi ini sepenuhnya bebas dari iklan yang dapat mengganggu kekhusyukan dan kesucian masjid.

---

## 6. Privasi Anak-Anak
Aplikasi ini ditujukan untuk audiens umum dan pengurus rumah ibadah (Semua Umur / *Everyone*). Kami tidak pernah dengan sengaja mengumpulkan informasi pribadi dari anak-anak di bawah umur 13 tahun.

---

## 7. Perubahan Kebijakan Privasi
Kami dapat memperbarui Kebijakan Privasi ini dari waktu ke waktu jika terdapat penambahan fitur baru. Setiap perubahan akan dicantumkan pada halaman ini dengan tanggal pembaruan terbaru.

---

## 8. Kontak Kami
Jika Anda memiliki pertanyaan, saran, atau kendala terkait privasi aplikasi MasjidKU TV, silakan hubungi tim kami melalui:
- **Email:** support@masjidku.id / pengembang@masjidku.id
- **Situs Web:** https://masjidku.id

---

# 🌐 English Version: Privacy Policy

**Effective Date:** August 30, 2026  
**Application:** MasjidKU TV  
**Developer:** DKM Digital Signage / MasjidKU Team  

### Overview
MasjidKU TV respects the privacy of all users and mosque congregations. This application operates under a strict **Zero Tracking & Local Storage** policy.

1. **No Personal Data Collection**: We do not collect, transmit, sell, or share any personally identifiable information (PII) to external servers.
2. **Local Data Storage**: All mosque profile information, financial reports, Friday officer schedules, and QRIS donation images are stored solely within your device's internal local database (Android Room/SQLite).
3. **Local Network Remote Control**: The embedded Web Remote runs exclusively on your local Wi-Fi network. No configuration data leaves your local network.
4. **Permissions**:
   - `INTERNET`: For fetching public prayer schedules from national APIs and hosting the local HTTP remote server.
   - `ACCESS_NETWORK_STATE` & `ACCESS_WIFI_STATE`: To display the local IP address for remote access.
   - `WAKE_LOCK`: To maintain the display active during mosque operating hours.
5. **No Third-Party Advertising or Tracking**: MasjidKU TV contains zero ads, zero analytics trackers, and zero behavioral monitoring tools.

For any inquiries, please contact us at **support@masjidku.id**.
