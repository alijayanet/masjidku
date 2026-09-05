package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionType

@Entity(tableName = "mosque_config")
data class MosqueConfigEntity(
    @PrimaryKey val id: Int = 1,
    val mosqueName: String = "MASJID BAITURROHMAN",
    val tagline: String = "Pusat Ibadah, Pembinaan Umat, & Dakwah Islam",
    val address: String = "Desa Ujunggebang, Kecamatan Sukra - Indramayu",
    val city: String = "Indramayu, Jawa Barat",
    val phone: String = "0812-2214-534",
    val bankName: String = "Bank BRI (Bank Rakyat Indonesia)",
    val bankAccount: String = "4206 0101 2214 534",
    val bankAccountName: String = "DKM BAITUR ROHMAN",
    val latitude: Double = -6.3078,
    val longitude: Double = 107.9945,
    val timezone: Double = 7.0,
    val calculationMethod: String = "KEMENAG",
    val offsetImsak: Int = 0,
    val offsetSubuh: Int = 2,
    val offsetTerbit: Int = -2,
    val offsetDhuha: Int = 2,
    val offsetDzuhur: Int = 2,
    val offsetAshar: Int = 2,
    val offsetMaghrib: Int = 2,
    val offsetIsya: Int = 2,
    val iqomahSubuh: Int = 10,
    val iqomahDzuhur: Int = 10,
    val iqomahAshar: Int = 10,
    val iqomahMaghrib: Int = 7,
    val iqomahIsya: Int = 10,
    val sholatDurationMinutes: Int = 10,
    val hijriAdjustmentDays: Int = 0,
    val activeTheme: String = "EMERALD_GOLD",
    val displayLayoutModel: String = "CAROUSEL_BOTTOM",
    val carouselIntervalSeconds: Int = 15,
    val soundAlertEnabled: Boolean = true,
    val showImsak: Boolean = true,
    val showSubuh: Boolean = true,
    val showTerbit: Boolean = true,
    val showDhuha: Boolean = true,
    val showDzuhur: Boolean = true,
    val showAshar: Boolean = true,
    val showMaghrib: Boolean = true,
    val showIsya: Boolean = true,
    val showFinancialReport: Boolean = true,
    val showFridayOfficers: Boolean = true,
    val showActivities: Boolean = true,
    val showDailyMaklumat: Boolean = true,
    val customBackgroundImagePath: String = "",
    val customBackgroundDim: Float = 0.5f,
    val mainScreenBgPreset: String = "PRESET_EMERALD_MIHRAB",
    val financeBgPreset: String = "PRESET_WHITE_PEARL_GOLD",
    val fridayOfficerBgPreset: String = "PRESET_EMERALD_MIHRAB",
    val countdownBgPreset: String = "PRESET_COUNTDOWN_PLAQUE",
    val customFinanceBgPath: String = "",
    val customFridayOfficerBgPath: String = "",
    val customCountdownBgPath: String = "",
    val centerCardTransparency: Float = 0.35f,
    val qrisImagePath: String = "",
    val qrisLabel: String = "INFAQ & SHODAQOH VIA QRIS / REKENING",
    val donationProgramTitle: String = "Renovasi & Pemakmuran Masjid Baiturrohman",
    val donationTargetAmount: Long = 25000000L,
    val donationCollectedAmount: Long = 16850000L,
    val showQrisCard: Boolean = true,
    val showDailyHadith: Boolean = true,
    val webAdminPassword: String = "123456",
    val enablePreAdhanCountdown: Boolean = true,
    val preAdhanCountdownSeconds: Int = 30,
    val imamSubuh: String = "Ust. H. Ahmad Dahlan, Lc.",
    val imamDzuhur: String = "Ust. Ridwan Kamil",
    val imamAshar: String = "Ust. Dr. Muhammad Iqbal",
    val imamMaghrib: String = "Ust. H. Abdul Somad, Lc.",
    val imamIsya: String = "Ust. Farhan Al-Hafizh",
    val muadzinSubuh: String = "",
    val muadzinDzuhur: String = "",
    val muadzinAshar: String = "",
    val muadzinMaghrib: String = "",
    val muadzinIsya: String = "",
    val runningTextFontSize: Int = 20,
    val murottalEnabled: Boolean = false,
    val murottalSubuh: Boolean = true,
    val murottalDzuhur: Boolean = false,
    val murottalAshar: Boolean = false,
    val murottalMaghrib: Boolean = true,
    val murottalIsya: Boolean = false,
    val murottalJumat: Boolean = true,
    val murottalDurationMinutes: Int = 10,
    val murottalVolume: Int = 80,
    val murottalSourceType: String = "PRESET",
    val murottalSelectedPresetId: String = "mishary_ar_rahman",
    val murottalCustomAudioPath: String = "",
    val customLogoImagePath: String = "",
    val youtubeLiveUrl: String = "",
    val youtubeLiveEnabled: Boolean = false,
    val youtubeLiveTitle: String = "Live Streaming Masjid",
    val showYoutubeLiveSlide: Boolean = false,
    val youtubeLiveMuted: Boolean = false,

    // --- Video Lokal (Background & Fullscreen) & CCTV Background ---
    val backgroundType: String = "PRESET", // PRESET, IMAGE, VIDEO, CCTV
    val backgroundVideoPath: String = "",
    val backgroundCctvCameraId: String = "",
    val fullscreenVideoPath: String = "",
    val fullscreenVideoTitle: String = "",
    val fullscreenVideoMuted: Boolean = false,
    val fullscreenVideoPlaying: Boolean = false,

    // --- CCTV / IP Camera Live Streaming (RTSP) & Multi-Camera ---
    val cctvStreamUrl: String = "",
    val cctvStreamEnabled: Boolean = false,
    val cctvStreamTitle: String = "Live Kamera Masjid",
    val showCctvSlide: Boolean = false,
    val cctvStreamMuted: Boolean = true,
    val liveStreamType: String = "RTSP_CCTV", // RTSP_CCTV, YOUTUBE
    val cctvCamerasJson: String = "",
    val cctvActiveCameraId: String = "",
    val cctvAutoRotateEnabled: Boolean = false,
    val cctvAutoRotateIntervalSeconds: Int = 30,
    val cctvDisplayLayout: String = "SINGLE", // SINGLE, SPLIT_2, GRID_4
    val cctvAutoPlayAfterPrayer: Boolean = false,
    val cctvAfterPrayerDurationMinutes: Int = 30,
    val cctvAfterPrayerWaktu: String = "SUBUH,MAGHRIB,ISYA,JUMAT",
    val cctvAfterPrayerCameraId: String = "",
    val cctvScheduleEnabled: Boolean = false,
    val cctvScheduleStartTime: String = "18:30",
    val cctvScheduleEndTime: String = "19:30",
    val cctvScheduleDays: String = "ALL",
    val cctvScheduleCameraId: String = "",

    // --- Kustomisasi Teks & Warna Laporan Keuangan ---
    val financeTitleText: String = "LAPORAN KAS & KEUANGAN MASJID",
    val financeTitleColor: String = "#FFD700",
    val financeBalanceColor: String = "#FFE082",
    val financeIncomeColor: String = "#4ADE80",
    val financeExpenseColor: String = "#FB7185",
    val financeFooterColor: String = "#90CAF9",

    // --- Kustomisasi Teks & Warna Petugas Jum'at ---
    val fridayTitleText: String = "JADWAL PETUGAS SHOLAT JUM'AT",
    val fridayTitleColor: String = "#FFD700",
    val fridayOfficerNameColor: String = "#FFFFFF",
    val fridayOfficerLabelColor: String = "#38BDF8",

    // --- Kustomisasi Teks & Warna Mutiara Hadits ---
    val hadithTitleText: String = "MUTIARA HADITS SHAHIH",
    val hadithTitleColor: String = "#FFD700",
    val hadithThemeText: String = "Keutamaan Memakmurkan Masjid & Sholat Berjama'ah",
    val hadithThemeColor: String = "#FDE68A",
    val hadithArabicText: String = "مَنْ بَنَى مَسْجِدًا لِلَّهِ بَنَى اللَّهُ لَهُ فِي الْجَنَّةِ مِثْلَهُ",
    val hadithArabicColor: String = "#FFE082",
    val hadithTranslationText: String = "Barangsiapa membangun masjid karena Allah, maka Allah akan membangunkan untuknya rumah semisal di surga.",
    val hadithTranslationColor: String = "#FFFFFF",
    val hadithNarratorText: String = "HR. Bukhari no. 450 & Muslim no. 533",
    val hadithNarratorColor: String = "#94A3B8",

    // --- Kustomisasi Teks & Warna Maklumat & Adab Masjid ---
    val maklumatTitleLeft: String = "MUTIARA HIKMAH MASJID",
    val maklumatTitleLeftColor: String = "#FFD700",
    val maklumatTextLeft: String = "Sesungguhnya yang memakmurkan masjid-masjid Allah hanyalah orang-orang yang beriman kepada Allah dan hari kemudian, serta tetap mendirikan shalat dan menunaikan zakat.",
    val maklumatTextLeftColor: String = "#E2E8F0",
    val maklumatTitleRight: String = "TATA TERTIB & ADAB MASJID",
    val maklumatTitleRightColor: String = "#38BDF8",
    val maklumatPoint1: String = "Harap menonaktifkan atau senyapkan nada dering HP selama di masjid.",
    val maklumatPoint2: String = "Luruskan dan rapatkan shaf sebelum sholat berjamaah dimulai.",
    val maklumatPoint3: String = "Jagalah kebersihan, ketertiban, dan kesucian area masjid bersama.",
    val maklumatPointsColor: String = "#FFFFFF",

    // --- Kustomisasi Font & Warna Jam Digital TV ---
    val clockFontFamily: String = "RADIOLAND",
    val clockColor: String = "#FFFFFF",
    val clockColonColor: String = "#F59E0B",
    val clockSecondsColor: String = "#F59E0B"
)

@Entity(tableName = "finance_transactions")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: String, // INCOME, EXPENSE
    val category: String,
    val date: String,
    val notes: String = ""
)

@Entity(tableName = "friday_schedules")
data class FridayOfficerEntity(
    @PrimaryKey val id: Long = 1,
    val date: String,
    val hijriDate: String,
    val khotib: String,
    val imam: String,
    val muadzin: String,
    val bilal: String,
    val khutbahTopic: String,
    val notes: String = ""
)

@Entity(tableName = "mosque_activities")
data class MosqueActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val speaker: String,
    val date: String,
    val time: String,
    val location: String = "Ruang Utama Masjid",
    val description: String = "",
    val category: String = "Kajian Rutin"
)

@Entity(tableName = "running_texts")
data class RunningTextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isActive: Boolean = true,
    val itemOrder: Int = 0
)

@Entity(tableName = "media_slides")
data class MediaSlideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val durationSeconds: Int = 15,
    val isActive: Boolean = true,
    val itemOrder: Int = 0
)

@Entity(tableName = "daily_imam_schedules")
data class DailyImamScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // Format: yyyy-MM-dd
    val prayerName: String, // "SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA"
    val imamName: String,
    val muadzinName: String = "",
    val notes: String = ""
)

@Entity(tableName = "murottal_audio_items")
data class MurottalAudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val qari: String,
    val surah: String,
    val durationSeconds: Int = 0,
    val filePath: String,
    val prayerTime: String = "ALL", // "ALL", "SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA", "JUMAT"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

