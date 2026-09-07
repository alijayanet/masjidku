package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CctvCameraItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Kamera Masjid",
    val streamUrl: String = "",
    val isMuted: Boolean = true,
    val isEnabled: Boolean = true,
    val showInSlide: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("streamUrl", streamUrl)
            put("isMuted", isMuted)
            put("isEnabled", isEnabled)
            put("showInSlide", showInSlide)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): CctvCameraItem {
            return CctvCameraItem(
                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = obj.optString("name", "Kamera Masjid"),
                streamUrl = obj.optString("streamUrl", ""),
                isMuted = obj.optBoolean("isMuted", true),
                isEnabled = obj.optBoolean("isEnabled", true),
                showInSlide = obj.optBoolean("showInSlide", false)
            )
        }

        fun parseList(jsonStr: String): List<CctvCameraItem> {
            if (jsonStr.isBlank()) return emptyList()
            val list = mutableListOf<CctvCameraItem>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        list.add(fromJson(obj))
                    }
                }
            } catch (_: Exception) {}
            return list
        }

        fun listToJson(list: List<CctvCameraItem>): String {
            val array = JSONArray()
            for (item in list) {
                array.put(item.toJson())
            }
            return array.toString()
        }
    }
}

data class MosqueConfig(
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
    val calculationMethod: String = "KEMENAG", // KEMENAG, MWL, EGYPT
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
    val iqomahJumat: Int = 15,
    val iqomahAshar: Int = 10,
    val iqomahMaghrib: Int = 7,
    val iqomahIsya: Int = 10,
    val idulFitriEnabled: Boolean = false,
    val idulFitriDate: String = "",
    val idulFitriTime: String = "06:30",
    val idulFitriIqomahMinutes: Int = 15,
    val idulFitriSholatMinutes: Int = 20,
    val idulAdhaEnabled: Boolean = false,
    val idulAdhaDate: String = "",
    val idulAdhaTime: String = "06:30",
    val idulAdhaIqomahMinutes: Int = 15,
    val idulAdhaSholatMinutes: Int = 20,
    val tarawihEnabled: Boolean = false,
    val tarawihAutoDetectNight: Boolean = true,
    val tarawihManualNight: Int = 1,
    val tarawihShowSlide: Boolean = true,
    val tarawihKultumMinutes: Int = 15,
    val tarawihSholatMinutes: Int = 45,
    val tarawihTitleText: String = "JADWAL PETUGAS SHOLAT TARAWIH & KULTUM",
    val tarawihTitleColor: String = "#FFD700",
    val tarawihOfficerNameColor: String = "#FFFFFF",
    val tarawihOfficerLabelColor: String = "#38BDF8",
    val tarawihBgPreset: String = "PRESET_EMERALD_MIHRAB",
    val customTarawihBgPath: String = "",
    val sholatDurationMinutes: Int = 10,
    val hijriAdjustmentDays: Int = 0,
    val activeTheme: String = "EMERALD_GOLD", // EMERALD_GOLD, ROYAL_NAVY, SUNSET_AMBER, MIDNIGHT_CHARCOAL, MIHRAB_CLASSIC
    val displayLayoutModel: String = "CAROUSEL_BOTTOM", // CAROUSEL_BOTTOM, SPLIT_DASHBOARD, DUAL_INFO_COMPACT
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
    val fridayOfficerBgPreset: String = "PRESET_WHITE_PEARL_GOLD",
    val countdownBgPreset: String = "PRESET_EMERALD_MIHRAB",
    val customFinanceBgPath: String = "",
    val customFridayOfficerBgPath: String = "",
    val customCountdownBgPath: String = "",
    val centerCardTransparency: Float = 0.85f,
    val qrisImagePath: String = "",
    val qrisLabel: String = "Infaq & Shodaqoh Digital",
    val donationProgramTitle: String = "RENOVASI KUBAH & MENARA MASJID",
    val donationTargetAmount: Long = 150000000L,
    val donationCollectedAmount: Long = 98500000L,
    val showQrisCard: Boolean = true,
    val showDailyHadith: Boolean = true,
    val webAdminPassword: String = "123456",
    val enablePreAdhanCountdown: Boolean = true,
    val preAdhanCountdownSeconds: Int = 10,
    val imamSubuh: String = "Ust. H. Ahmad Dahlan",
    val imamDzuhur: String = "Ust. Muhammad Ridwan",
    val imamAshar: String = "Ust. Dr. Fathurrahman",
    val imamMaghrib: String = "Ust. H. Syaifullah, M.Ag",
    val imamIsya: String = "Ust. K.H. Zainuddin MZ",
    val muadzinSubuh: String = "Ust. Syukron",
    val muadzinDzuhur: String = "Ust. Hilman",
    val muadzinAshar: String = "Ust. Fauzan",
    val muadzinMaghrib: String = "Ust. Salman",
    val muadzinIsya: String = "Ust. Danial",
    val runningTextFontSize: Int = 20,
    val murottalEnabled: Boolean = false,
    val murottalSubuh: Boolean = true,
    val murottalDzuhur: Boolean = true,
    val murottalAshar: Boolean = true,
    val murottalMaghrib: Boolean = true,
    val murottalIsya: Boolean = true,
    val murottalJumat: Boolean = true,
    val murottalDurationMinutes: Int = 10,
    val murottalVolume: Int = 80,
    val murottalSourceType: String = "PRESET", // PRESET or MANUAL_UPLOAD
    val murottalSelectedPresetId: String = "al_kahf_alafasy",
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
    val clockFontFamily: String = "RADIOLAND", // RADIOLAND, ORBITRON, RAJDHANI, SANS_SERIF, SERIF, MONOSPACE
    val clockColor: String = "#FFFFFF",
    val clockColonColor: String = "#F59E0B",
    val clockSecondsColor: String = "#F59E0B",

    // --- Pengaturan WhatsApp Gateway (Fonnte) ---
    val waGatewayEnabled: Boolean = false,
    val waGatewayProvider: String = "FONNTE",
    val waGatewayToken: String = "",
    val waGatewaySendThursdayHour: Int = 9,
    val waGatewaySendFridayHour: Int = 9,
    val waGatewayLastSentThursdayDate: String = "",
    val waGatewayLastSentFridayDate: String = "",
    val waGatewayTemplateThursday: String = "",
    val waGatewayTemplateFriday: String = "",
    val waGatewayTemplateKajian: String = "",
    val waGatewayLastLogJson: String = ""
) {
    fun getCctvCameras(): List<CctvCameraItem> {
        val parsed = CctvCameraItem.parseList(cctvCamerasJson)
        if (parsed.isEmpty() && cctvStreamUrl.isNotBlank()) {
            return listOf(
                CctvCameraItem(
                    id = "cam_default",
                    name = cctvStreamTitle.ifBlank { "Kamera Utama" },
                    streamUrl = cctvStreamUrl,
                    isMuted = cctvStreamMuted,
                    isEnabled = cctvStreamEnabled,
                    showInSlide = showCctvSlide
                )
            )
        }
        return parsed
    }

    fun getActiveCctvCamera(): CctvCameraItem? {
        val list = getCctvCameras()
        if (list.isEmpty()) return null
        return if (cctvActiveCameraId.isNotBlank()) {
            list.firstOrNull { it.id == cctvActiveCameraId } ?: list.firstOrNull { it.isEnabled } ?: list.first()
        } else {
            list.firstOrNull { it.isEnabled } ?: list.first()
        }
    }

    fun getBackgroundCctvUrl(): String {
        if (backgroundCctvCameraId.isNotBlank()) {
            val cam = getCctvCameras().firstOrNull { it.id == backgroundCctvCameraId }
            if (cam != null && cam.streamUrl.isNotBlank()) return cam.streamUrl
        }
        val active = getActiveCctvCamera()
        if (active != null && active.streamUrl.isNotBlank()) return active.streamUrl
        return cctvStreamUrl
    }
}

data class DailyHadith(
    val arabic: String,
    val translation: String,
    val narrator: String,
    val topic: String
)

object DefaultHadiths {
    val list = listOf(
        DailyHadith(
            arabic = "صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            translation = "Shalat berjamaah lebih utama daripada shalat sendirian sebanyak dua puluh tujuh derajat.",
            narrator = "HR. Bukhari & Muslim",
            topic = "Keutamaan Sholat Berjamaah"
        ),
        DailyHadith(
            arabic = "إِنَّمَا يَعْمُرُ مَسَاجِدَ اللَّهِ مَنْ آمَنَ بِاللَّهِ وَالْيَوْمِ الْآخِرِ",
            translation = "Hanya yang memakmurkan masjid-masjid Allah ialah orang-orang yang beriman kepada Allah dan hari kemudian.",
            narrator = "QS. At-Taubah: 18",
            topic = "Memakmurkan Masjid"
        ),
        DailyHadith(
            arabic = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
            translation = "Sedekah itu tidak akan mengurangi harta sedikit pun.",
            narrator = "HR. Muslim",
            topic = "Keberkahan Sedekah"
        ),
        DailyHadith(
            arabic = "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ وَإِنَّهَا لَكَبِيرَةٌ إِلَّا عَلَى الْخَاشِعِينَ",
            translation = "Dan mohonlah pertolongan (kepada Allah) dengan sabar dan shalat. Dan sesungguhnya yang demikian itu sungguh berat kecuali bagi orang yang khusyu'.",
            narrator = "QS. Al-Baqarah: 45",
            topic = "Pertolongan Sabar & Shalat"
        ),
        DailyHadith(
            arabic = "مَنْ صَلَّى الْبَرْدَيْنِ دَخَلَ الْجَنَّةَ",
            translation = "Barangsiapa yang mengerjakan shalat pada dua waktu yang dingin (Subuh dan Ashar), niscaya ia akan masuk surga.",
            narrator = "HR. Bukhari & Muslim",
            topic = "Shalat Subuh & Ashar"
        ),
        DailyHadith(
            arabic = "الدُّعَاءُ لَا يُرَدُّ بَيْنَ الأَذَانِ وَالإِقَامَةِ",
            translation = "Doa tidak akan tertolak di antara adzan dan iqamah.",
            narrator = "HR. Abu Dawud & Tirmidzi",
            topic = "Waktu Mustajab Berdoa"
        ),
        DailyHadith(
            arabic = "كَلِمَةٌ طَيِّبَةٌ صَدَقَةٌ",
            translation = "Perkataan yang baik adalah sedekah.",
            narrator = "HR. Bukhari & Muslim",
            topic = "Akhlak & Tutur Kata Baik"
        ),
        DailyHadith(
            arabic = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            translation = "Barangsiapa menempuh jalan untuk mencari ilmu, maka Allah akan memudahkan baginya jalan menuju surga.",
            narrator = "HR. Muslim",
            topic = "Menuntut Ilmu di Masjid"
        )
    )
}

data class MediaSlide(
    val id: Long = 0,
    val filePath: String,
    val title: String,
    val durationSeconds: Int = 15,
    val isActive: Boolean = true,
    val order: Int = 0
)

enum class TransactionType {
    INCOME, EXPENSE
}

data class FinanceTransaction(
    val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: TransactionType,
    val category: String, // Kotak Amal Jumat, Infaq Pembangunan, Donasi Yatim, Operasional Listrik/Air, Honor Petugas, dll.
    val date: String,
    val notes: String = ""
)

data class FridaySchedule(
    val id: Long = 1,
    val date: String,
    val hijriDate: String,
    val khotib: String,
    val khotibPhone: String = "",
    val imam: String,
    val imamPhone: String = "",
    val muadzin: String,
    val muadzinPhone: String = "",
    val bilal: String,
    val bilalPhone: String = "",
    val khutbahTopic: String,
    val notes: String = ""
)

data class TarawihSchedule(
    val night: Int = 1, // 1 to 30
    val date: String = "",
    val penceramah: String = "",
    val penceramahPhone: String = "",
    val judulKultum: String = "",
    val imamTarawih: String = "",
    val imamTarawihPhone: String = "",
    val imamWitir: String = "",
    val imamWitirPhone: String = "",
    val bilalTarawih: String = "",
    val bilalTarawihPhone: String = "",
    val notes: String = ""
)

data class MosqueActivity(
    val id: Long = 0,
    val title: String,
    val speaker: String,
    val speakerPhone: String = "",
    val date: String,
    val time: String,
    val location: String = "Ruang Utama Masjid",
    val description: String = "",
    val category: String = "Kajian Rutin" // Kajian Rutin, Tabligh Akbar, TPQ, Kerja Bakti, PHBI
)

data class RunningTextItem(
    val id: Long = 0,
    val text: String,
    val isActive: Boolean = true,
    val order: Int = 0
)

data class PrayerSchedule(
    val imsak: String,
    val subuh: String,
    val terbit: String,
    val dhuha: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String,
    val dateStr: String,
    val hijriDateStr: String
)

enum class PrayerName(val displayName: String, val arabicName: String) {
    IMSAK("Imsak", "الإمساك"),
    SUBUH("Subuh", "الفجر"),
    TERBIT("Terbit", "الشروق"),
    DHUHA("Dhuha", "الضحى"),
    DZUHUR("Dzuhur", "الظهر"),
    ASHAR("Ashar", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISYA("Isya'", "العشاء"),
    IDUL_FITRI("Idul Fitri", "عيد الفطر"),
    IDUL_ADHA("Idul Adha", "عيد الأضحى");

    fun getDisplayName(isFriday: Boolean = false): String =
        if (isFriday && this == DZUHUR) "Jum'at" else displayName

    fun getArabicName(isFriday: Boolean = false): String =
        if (isFriday && this == DZUHUR) "الجمعة" else arabicName
}

data class ActivePrayerOfficers(
    val imam: String,
    val muadzin: String = "",
    val khotib: String = "",
    val bilal: String = "",
    val isFriday: Boolean = false,
    val isHariRaya: Boolean = false,
    val eventTitle: String = ""
)

data class NextPrayerInfo(
    val prayer: PrayerName,
    val timeStr: String,
    val remainingSeconds: Long,
    val formattedCountdown: String,
    val isAdhanTime: Boolean = false
)

enum class TVDisplayMode {
    NORMAL,
    PRE_ADHAN_COUNTDOWN,
    ADHAN,
    IQOMAH_COUNTDOWN,
    SHOLAT_SILENT,
    FULLSCREEN_VIDEO,
    FULLSCREEN_CCTV
}

data class LocalVideoItem(
    val id: String,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val formattedSize: String = "0 MB",
    val durationSeconds: Int = 0,
    val formattedDuration: String = "00:00",
    val isBackgroundActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyImamSchedule(
    val id: Long = 0,
    val date: String, // Format: yyyy-MM-dd
    val prayerName: PrayerName,
    val imamName: String,
    val muadzinName: String = "",
    val notes: String = ""
)

data class YouTubeLivePreset(
    val id: String,
    val name: String,
    val url: String,
    val channelName: String,
    val description: String
)

object YouTubeLiveHelper {
    val PRESETS = listOf(
        YouTubeLivePreset(
            id = "makkah_live",
            name = "🕋 Makkah Live (Masjidil Haram 24 Jam)",
            url = "https://www.youtube.com/channel/UC-m8iLY2bN7DqG268H64Y0A/live",
            channelName = "Saudi Quran TV / Makkah Live",
            description = "Siaran langsung 24 jam Ka'bah & Thawaf Masjidil Haram Makkah"
        ),
        YouTubeLivePreset(
            id = "madinah_live",
            name = "🕌 Madinah Live (Masjid Nabawi 24 Jam)",
            url = "https://www.youtube.com/channel/UCu-H53HjJ7Jz6_2-9aY_o8Q/live",
            channelName = "Saudi Sunnah TV / Madinah Live",
            description = "Siaran langsung 24 jam Raudhah & Kubah Hijau Masjid Nabawi"
        ),
        YouTubeLivePreset(
            id = "rodja_live",
            name = "📡 Rodja TV (Kajian Sunnah 24 Jam)",
            url = "https://www.youtube.com/channel/UCn8aN9sC4yHj2bC3c_Qv84Q/live",
            channelName = "Radio Rodja & Rodja TV",
            description = "Siaran langsung ceramah dan kajian ilmiah Ahlussunnah 24 Jam"
        ),
        YouTubeLivePreset(
            id = "surau_live",
            name = "📡 Surau TV Live Streaming",
            url = "https://www.youtube.com/channel/UCq3yYhK_pC_fG26t4h9N0kg/live",
            channelName = "Surau TV",
            description = "Saluran Televisi Dakwah Islam & Pembinaan Ummah"
        ),
        YouTubeLivePreset(
            id = "custom_kajian",
            name = "🎙️ Siaran Live Masjid Sendiri",
            url = "",
            channelName = "Channel YouTube Masjid",
            description = "Masukkan tautan live streaming sholat Jumat, tarawih, atau kajian masjid"
        )
    )

    fun extractVideoId(url: String): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()

        // 1. Raw 11-character video ID
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }

        // 2. If user pasted iframe HTML tag e.g. <iframe src="https://www.youtube.com/embed/XXXXX"...>
        if (trimmed.contains("<iframe", ignoreCase = true)) {
            val srcMatch = Regex("src=[\"']([^\"']+)[\"']").find(trimmed)
            if (srcMatch != null) {
                return extractVideoId(srcMatch.groupValues[1])
            }
        }

        // 3. YouTube URL Patterns (watch?v=, youtu.be/, embed/, live/, v/)
        val patterns = listOf(
            Regex("(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:watch\\?.*v=|embed\\/|v\\/|live\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"),
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be\\/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com\\/live\\/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com\\/embed\\/([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun extractChannelId(url: String): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()
        if (trimmed.contains("@SaudiQuranTv", ignoreCase = true)) {
            return "UC-m8iLY2bN7DqG268H64Y0A"
        }
        if (trimmed.contains("@SaudiSunnahTv", ignoreCase = true)) {
            return "UCu-H53HjJ7Jz6_2-9aY_o8Q"
        }
        if (trimmed.contains("radiorodja", ignoreCase = true) || trimmed.contains("rodjatv", ignoreCase = true)) {
            return "UCn8aN9sC4yHj2bC3c_Qv84Q"
        }
        if (trimmed.contains("surautv", ignoreCase = true)) {
            return "UCq3yYhK_pC_fG26t4h9N0kg"
        }

        val channelParamMatch = Regex("[?&]channel=([a-zA-Z0-9_-]+)").find(trimmed)
        if (channelParamMatch != null && channelParamMatch.groupValues.size > 1) {
            return channelParamMatch.groupValues[1]
        }

        val channelMatch = Regex("channel\\/([a-zA-Z0-9_-]+)").find(trimmed)
        if (channelMatch != null && channelMatch.groupValues.size > 1) {
            return channelMatch.groupValues[1]
        }
        return null
    }

    fun getEmbedUrl(url: String, isMuted: Boolean = false): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()
        val muteParam = if (isMuted) "1" else "0"

        val videoId = extractVideoId(trimmed)
        if (videoId != null) {
            return "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=$muteParam&controls=0&modestbranding=1&rel=0&iv_load_policy=3&showinfo=0&disablekb=1&fs=0&playsinline=1&enablejsapi=1"
        }

        val channelId = extractChannelId(trimmed)
        if (channelId != null) {
            return "https://www.youtube-nocookie.com/embed/live_stream?channel=$channelId&autoplay=1&mute=$muteParam&controls=0&modestbranding=1&rel=0&iv_load_policy=3&showinfo=0&disablekb=1&fs=0&playsinline=1&enablejsapi=1"
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return null
    }

    /**
     * Builds a complete, pure, borderless HTML5 container page for YouTube video playback.
     * All controls, title bars, and branding are cleanly hidden so ONLY the pure video is displayed.
     * Includes automatic watchdog and retry mechanisms to keep live stream playing reliably on TV.
     */
    fun buildPlayerHtml(youtubeUrl: String, isMuted: Boolean = false): String {
        val videoId = extractVideoId(youtubeUrl)
        val channelId = extractChannelId(youtubeUrl)
        val muteParam = if (isMuted) "1" else "0"

        val embedSrc = when {
            videoId != null -> "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=$muteParam&controls=0&modestbranding=1&rel=0&iv_load_policy=3&showinfo=0&disablekb=1&fs=0&playsinline=1&enablejsapi=1"
            channelId != null -> "https://www.youtube-nocookie.com/embed/live_stream?channel=$channelId&autoplay=1&mute=$muteParam&controls=0&modestbranding=1&rel=0&iv_load_policy=3&showinfo=0&disablekb=1&fs=0&playsinline=1&enablejsapi=1"
            else -> getEmbedUrl(youtubeUrl, isMuted) ?: "https://www.youtube.com"
        }

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                        background-color: #000000;
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000;
                        margin: 0;
                        padding: 0;
                    }
                    .video-container {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    iframe {
                        position: absolute;
                        top: 50%;
                        left: 50%;
                        width: 100vw;
                        height: 100vh;
                        min-width: 100%;
                        min-height: 100%;
                        transform: translate(-50%, -50%) scale(1.16);
                        transform-origin: center center;
                        border: 0;
                        outline: 0;
                        pointer-events: none;
                    }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <iframe
                        id="masjidku_tv_player"
                        src="$embedSrc"
                        referrerpolicy="strict-origin-when-cross-origin"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen>
                    </iframe>
                </div>
                <script>
                    var isMutedSetting = $isMuted;
                    function sendPlayerCommand(cmd, args) {
                        try {
                            var p = document.getElementById('masjidku_tv_player');
                            if (p && p.contentWindow) {
                                p.contentWindow.postMessage(JSON.stringify({
                                    "event": "command",
                                    "func": cmd,
                                    "args": args || ""
                                }), '*');
                            }
                        } catch(e) {}
                    }

                    function triggerPlay() {
                        sendPlayerCommand('playVideo');
                        if (isMutedSetting) {
                            sendPlayerCommand('mute');
                        } else {
                            sendPlayerCommand('unMute');
                            sendPlayerCommand('setVolume', [100]);
                        }
                    }

                    window.addEventListener('load', function() {
                        triggerPlay();
                        setTimeout(triggerPlay, 800);
                        setTimeout(triggerPlay, 2000);
                        setTimeout(triggerPlay, 4500);
                    });

                    // Watchdog: Auto keep alive and recover if interrupted
                    setInterval(function() {
                        triggerPlay();
                    }, 12000);
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}

object BackgroundPresetHelper {
    const val PRESET_EMERALD_MIHRAB = "PRESET_EMERALD_MIHRAB"
    const val PRESET_WHITE_PEARL_GOLD = "PRESET_WHITE_PEARL_GOLD"
    const val PRESET_MIDNIGHT_KISWAH = "PRESET_MIDNIGHT_KISWAH"
    const val PRESET_OTTOMAN_SAPPHIRE = "PRESET_OTTOMAN_SAPPHIRE"
    const val PRESET_COUNTDOWN_PLAQUE = "PRESET_COUNTDOWN_PLAQUE"
    const val PRESET_MIDNIGHT_LANTERNS = "PRESET_MIDNIGHT_LANTERNS"
    const val PRESET_GRADIENT_SKY = "PRESET_GRADIENT_SKY"
    const val SAME_AS_MAIN = "SAME_AS_MAIN"
    const val CUSTOM_UPLOAD = "CUSTOM_UPLOAD"

    fun getDrawableResId(preset: String): Int? {
        return when (preset) {
            PRESET_EMERALD_MIHRAB -> com.example.R.drawable.bg_preset_emerald_mihrab
            PRESET_WHITE_PEARL_GOLD -> com.example.R.drawable.bg_preset_white_pearl_gold
            PRESET_MIDNIGHT_KISWAH -> com.example.R.drawable.bg_preset_midnight_kiswah
            PRESET_OTTOMAN_SAPPHIRE -> com.example.R.drawable.bg_preset_ottoman_sapphire
            PRESET_COUNTDOWN_PLAQUE -> com.example.R.drawable.bg_preset_countdown_plaque
            PRESET_MIDNIGHT_LANTERNS -> com.example.R.drawable.bg_preset_midnight_lanterns
            else -> null
        }
    }
}

data class MurottalPreset(
    val id: String,
    val qariName: String,
    val surahName: String,
    val audioUrl: String,
    val durationText: String,
    val description: String,
    val category: String = "Surah Pilihan" // "Surah Pilihan", "Juz 'Amma", "Khusus Subuh & Jum'at", "Shalawat & Doa"
)

object MurottalPresetCatalogue {
    val list = listOf(
        // === Syaikh Mishary Rashid Alafasy ===
        MurottalPreset(
            id = "mishary_al_fatihah_kursi",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Al-Fatihah & Ayat Kursi",
            audioUrl = "https://server8.mp3quran.net/afs/001.mp3",
            durationText = "~4 Menit",
            description = "Ummul Kitab & Ayat Perlindungan Agung",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "mishary_ar_rahman",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server8.mp3quran.net/afs/055.mp3",
            durationText = "~10 Menit",
            description = "Lantunan merdu penuh penghayatan QS. Ar-Rahman",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "mishary_al_mulk",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server8.mp3quran.net/afs/067.mp3",
            durationText = "~8 Menit",
            description = "Surah penyelamat siksa kubur (Isya / Maghrib)",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "mishary_al_waqiah",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server8.mp3quran.net/afs/056.mp3",
            durationText = "~11 Menit",
            description = "Surah keutamaan rezeki dan ketakwaan",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "mishary_yasin",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Yasin (QS. 36)",
            audioUrl = "https://server8.mp3quran.net/afs/036.mp3",
            durationText = "~14 Menit",
            description = "Jantung Al-Qur'an tartil syahdu dan menggetarkan hati",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "mishary_al_kahfi",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Al-Kahfi (QS. 18)",
            audioUrl = "https://server8.mp3quran.net/afs/018.mp3",
            durationText = "~24 Menit",
            description = "Sunnah hari Jum'at & penerang di antara dua Jum'at",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "mishary_as_sajdah",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah As-Sajdah (QS. 32)",
            audioUrl = "https://server8.mp3quran.net/afs/032.mp3",
            durationText = "~9 Menit",
            description = "Sunnah dibaca pada waktu Sholat Subuh hari Jum'at",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "mishary_al_insan",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah Al-Insan (QS. 76)",
            audioUrl = "https://server8.mp3quran.net/afs/076.mp3",
            durationText = "~8 Menit",
            description = "Kisah penciptaan manusia dan nikmat surga",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "mishary_juz_amma",
            qariName = "Syaikh Mishary Rashid Alafasy",
            surahName = "Surah An-Naba s/d An-Nas (Juz 30)",
            audioUrl = "https://server8.mp3quran.net/afs/078.mp3",
            durationText = "~6 Menit",
            description = "Surah An-Naba pembuka Juz 'Amma",
            category = "Juz 'Amma"
        ),

        // === Syaikh Abdurrahman As-Sudais (Imam Besar Masjidil Haram) ===
        MurottalPreset(
            id = "sudais_ar_rahman",
            qariName = "Syaikh Abdurrahman As-Sudais",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server11.mp3quran.net/sds/055.mp3",
            durationText = "~10 Menit",
            description = "Khas lantunan Imam Besar Masjidil Haram Mekkah",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "sudais_al_kahfi",
            qariName = "Syaikh Abdurrahman As-Sudais",
            surahName = "Surah Al-Kahfi (QS. 18)",
            audioUrl = "https://server11.mp3quran.net/sds/018.mp3",
            durationText = "~18 Menit",
            description = "Karakter vokal berwibawa khas Masjidil Haram",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "sudais_al_waqiah",
            qariName = "Syaikh Abdurrahman As-Sudais",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server11.mp3quran.net/sds/056.mp3",
            durationText = "~9 Menit",
            description = "Tartil khusyuk penuh ketenangan",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "sudais_al_mulk",
            qariName = "Syaikh Abdurrahman As-Sudais",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server11.mp3quran.net/sds/067.mp3",
            durationText = "~7 Menit",
            description = "Lantunan tegas dan penuh keagungan",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "sudais_yasin",
            qariName = "Syaikh Abdurrahman As-Sudais",
            surahName = "Surah Yasin (QS. 36)",
            audioUrl = "https://server11.mp3quran.net/sds/036.mp3",
            durationText = "~13 Menit",
            description = "Surah Yasin lengkap lantunan Masjidil Haram",
            category = "Surah Pilihan"
        ),

        // === Syaikh Maher Al-Muaiqly (Imam Masjidil Haram) ===
        MurottalPreset(
            id = "muaiqly_ar_rahman",
            qariName = "Syaikh Maher Al-Muaiqly",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server12.mp3quran.net/maher/055.mp3",
            durationText = "~10 Menit",
            description = "Nada merdu dan syahdu khas Syaikh Maher",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "muaiqly_al_waqiah",
            qariName = "Syaikh Maher Al-Muaiqly",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server12.mp3quran.net/maher/056.mp3",
            durationText = "~10 Menit",
            description = "Imam Masjidil Haram dengan nada merdu dan tegas",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "muaiqly_al_mulk",
            qariName = "Syaikh Maher Al-Muaiqly",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server12.mp3quran.net/maher/067.mp3",
            durationText = "~8 Menit",
            description = "Suara hangat dan menyejukkan hati",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "muaiqly_al_kahfi",
            qariName = "Syaikh Maher Al-Muaiqly",
            surahName = "Surah Al-Kahfi (QS. 18)",
            audioUrl = "https://server12.mp3quran.net/maher/018.mp3",
            durationText = "~20 Menit",
            description = "Surah Al-Kahfi syahdu untuk hari Jum'at",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "muaiqly_maryam",
            qariName = "Syaikh Maher Al-Muaiqly",
            surahName = "Surah Maryam (QS. 19)",
            audioUrl = "https://server12.mp3quran.net/maher/019.mp3",
            durationText = "~14 Menit",
            description = "Lantunan kisah Maryam dan Nabi Isa 'alaihissalam",
            category = "Surah Pilihan"
        ),

        // === Syaikh Saad Al-Ghamdi ===
        MurottalPreset(
            id = "ghamdi_ar_rahman",
            qariName = "Syaikh Saad Al-Ghamdi",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server7.mp3quran.net/s_gmd/055.mp3",
            durationText = "~9 Menit",
            description = "Tartil jernih dan tenang dari Syaikh Saad Al-Ghamdi",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "ghamdi_al_waqiah",
            qariName = "Syaikh Saad Al-Ghamdi",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server7.mp3quran.net/s_gmd/056.mp3",
            durationText = "~10 Menit",
            description = "Surah Al-Waqi'ah tartil syahdu",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "ghamdi_al_mulk",
            qariName = "Syaikh Saad Al-Ghamdi",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server7.mp3quran.net/s_gmd/067.mp3",
            durationText = "~8 Menit",
            description = "Surah Al-Mulk dengan artikulasi sangat jelas",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "ghamdi_yasin",
            qariName = "Syaikh Saad Al-Ghamdi",
            surahName = "Surah Yasin (QS. 36)",
            audioUrl = "https://server7.mp3quran.net/s_gmd/036.mp3",
            durationText = "~12 Menit",
            description = "Surah Yasin tartil jernih",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "ghamdi_al_kahfi",
            qariName = "Syaikh Saad Al-Ghamdi",
            surahName = "Surah Al-Kahfi (QS. 18)",
            audioUrl = "https://server7.mp3quran.net/s_gmd/018.mp3",
            durationText = "~21 Menit",
            description = "Tartil lengkap surah Al-Kahfi",
            category = "Khusus Subuh & Jum'at"
        ),

        // === Syaikh Hani Ar-Rifai ===
        MurottalPreset(
            id = "hani_al_mulk",
            qariName = "Syaikh Hani Ar-Rifai",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server8.mp3quran.net/rifai/067.mp3",
            durationText = "~9 Menit",
            description = "Karakter vokal syahdu dan menggetarkan hati",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "hani_ar_rahman",
            qariName = "Syaikh Hani Ar-Rifai",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server8.mp3quran.net/rifai/055.mp3",
            durationText = "~12 Menit",
            description = "Lantunan penuh tangis dan tadabbur mendalam",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "hani_yasin",
            qariName = "Syaikh Hani Ar-Rifai",
            surahName = "Surah Yasin (QS. 36)",
            audioUrl = "https://server8.mp3quran.net/rifai/036.mp3",
            durationText = "~16 Menit",
            description = "Surah Yasin dengan penghayatan emosional tinggi",
            category = "Surah Pilihan"
        ),

        // === Syaikh Saud Asy-Syuraim ===
        MurottalPreset(
            id = "syuraim_al_insan",
            qariName = "Syaikh Saud Asy-Syuraim",
            surahName = "Surah Al-Insan (QS. 76)",
            audioUrl = "https://server7.mp3quran.net/shur/076.mp3",
            durationText = "~9 Menit",
            description = "Tartil khusyuk dan menyentuh hati",
            category = "Khusus Subuh & Jum'at"
        ),
        MurottalPreset(
            id = "syuraim_ar_rahman",
            qariName = "Syaikh Saud Asy-Syuraim",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server7.mp3quran.net/shur/055.mp3",
            durationText = "~9 Menit",
            description = "Khas Imam legendaris Masjidil Haram",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "syuraim_al_mulk",
            qariName = "Syaikh Saud Asy-Syuraim",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server7.mp3quran.net/shur/067.mp3",
            durationText = "~8 Menit",
            description = "Lantunan berwibawa dan penuh energi",
            category = "Surah Pilihan"
        ),

        // === Syaikh Yasser Ad-Dossari ===
        MurottalPreset(
            id = "dossari_ar_rahman",
            qariName = "Syaikh Yasser Ad-Dossari",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server11.mp3quran.net/yasser/055.mp3",
            durationText = "~11 Menit",
            description = "Lantunan merdu memukau khas Imam Masjidil Haram",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "dossari_al_mulk",
            qariName = "Syaikh Yasser Ad-Dossari",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server11.mp3quran.net/yasser/067.mp3",
            durationText = "~8 Menit",
            description = "Irama bertingkat syahdu dan menggetarkan",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "dossari_al_waqiah",
            qariName = "Syaikh Yasser Ad-Dossari",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server11.mp3quran.net/yasser/056.mp3",
            durationText = "~10 Menit",
            description = "Surah Al-Waqi'ah penuh kekhusyukan",
            category = "Surah Pilihan"
        ),

        // === Syaikh Mahmud Khalil Al-Hussary (Klasik & Tajwid) ===
        MurottalPreset(
            id = "hussary_mujawwad",
            qariName = "Syaikh Mahmud Khalil Al-Hussary",
            surahName = "Surah Maryam (QS. 19)",
            audioUrl = "https://server13.mp3quran.net/hussary/019.mp3",
            durationText = "~15 Menit",
            description = "Master tajwid & makharijul huruf klasik dunia Islam",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "hussary_ar_rahman",
            qariName = "Syaikh Mahmud Khalil Al-Hussary",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server13.mp3quran.net/hussary/055.mp3",
            durationText = "~14 Menit",
            description = "Standar emas tartil Al-Qur'an dunia Islam",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "hussary_al_mulk",
            qariName = "Syaikh Mahmud Khalil Al-Hussary",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server13.mp3quran.net/hussary/067.mp3",
            durationText = "~9 Menit",
            description = "Tartil tenang dan tajwid sempurna",
            category = "Surah Pilihan"
        ),

        // === Syaikh Abdul Basit Abdul Samad ===
        MurottalPreset(
            id = "basit_ar_rahman",
            qariName = "Syaikh Abdul Basit Abdul Samad",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server7.mp3quran.net/basit/055.mp3",
            durationText = "~15 Menit",
            description = "Mujawwad legendaris suara emas dunia Islam",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "basit_al_waqiah",
            qariName = "Syaikh Abdul Basit Abdul Samad",
            surahName = "Surah Al-Waqi'ah (QS. 56)",
            audioUrl = "https://server7.mp3quran.net/basit/056.mp3",
            durationText = "~12 Menit",
            description = "Nafas panjang dan teknik qira'ah tingkat tinggi",
            category = "Surah Pilihan"
        ),

        // === Syaikh Ali Jaber (Rahimahullah) ===
        MurottalPreset(
            id = "ali_jaber_ar_rahman",
            qariName = "Syaikh Ali Jaber (Rahimahullah)",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server11.mp3quran.net/a_jbr/055.mp3",
            durationText = "~10 Menit",
            description = "Tartil khusyuk penuh kehangatan dari Syaikh Ali Jaber",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "ali_jaber_al_mulk",
            qariName = "Syaikh Ali Jaber (Rahimahullah)",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server11.mp3quran.net/a_jbr/067.mp3",
            durationText = "~8 Menit",
            description = "Surah Al-Mulk penuh kelembutan",
            category = "Surah Pilihan"
        ),

        // === Syaikh Nasser Al-Qatami ===
        MurottalPreset(
            id = "qatami_ar_rahman",
            qariName = "Syaikh Nasser Al-Qatami",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server6.mp3quran.net/qtm/055.mp3",
            durationText = "~10 Menit",
            description = "Lantunan sangat lembut dan menentramkan jiwa",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "qatami_al_mulk",
            qariName = "Syaikh Nasser Al-Qatami",
            surahName = "Surah Al-Mulk (QS. 67)",
            audioUrl = "https://server6.mp3quran.net/qtm/067.mp3",
            durationText = "~8 Menit",
            description = "Tartil syahdu waktu malam",
            category = "Surah Pilihan"
        ),

        // === Syaikh Idris Abkar ===
        MurottalPreset(
            id = "abkar_yasin",
            qariName = "Syaikh Idris Abkar",
            surahName = "Surah Yasin (QS. 36)",
            audioUrl = "https://server6.mp3quran.net/abkr/036.mp3",
            durationText = "~14 Menit",
            description = "Doa dan tilawah menggetarkan kalbu",
            category = "Surah Pilihan"
        ),
        MurottalPreset(
            id = "abkar_ar_rahman",
            qariName = "Syaikh Idris Abkar",
            surahName = "Surah Ar-Rahman (QS. 55)",
            audioUrl = "https://server6.mp3quran.net/abkr/055.mp3",
            durationText = "~11 Menit",
            description = "Penghayatan ayat nikmat Allah",
            category = "Surah Pilihan"
        ),

        // === Shalawat Tarhim & Doa Sebelum Sholat ===
        MurottalPreset(
            id = "tarhim_subuh",
            qariName = "Syaikh Mahmud Khalil Al-Hussary",
            surahName = "Shalawat Tarhim Sebelum Subuh",
            audioUrl = "https://server13.mp3quran.net/hussary/tarhim.mp3",
            durationText = "~5 Menit",
            description = "Ash-shalatu was-salamu 'alaik ya sayyidil mursalin (Tarhim Subuh Klasik)",
            category = "Shalawat & Doa"
        ),
        MurottalPreset(
            id = "tarhim_tibbil_qulub",
            qariName = "Pelantun Shalawat Nusantara",
            surahName = "Shalawat Tibbil Qulub & Asyghil",
            audioUrl = "https://server8.mp3quran.net/afs/112.mp3",
            durationText = "~4 Menit",
            description = "Shalawat penyejuk hati dan doa keselamatan umat",
            category = "Shalawat & Doa"
        )
    )

    fun getPresetById(id: String): MurottalPreset =
        list.find { it.id == id } ?: list.first()

    fun getQariOptions(): List<String> =
        list.map { it.qariName }.distinct()

    fun getCategoryOptions(): List<String> =
        listOf("Semua", "Surah Pilihan", "Khusus Subuh & Jum'at", "Juz 'Amma", "Shalawat & Doa")
}

data class MurottalAudioItem(
    val id: Long = 0,
    val title: String,
    val qari: String,
    val surah: String,
    val durationSeconds: Int = 0,
    val filePath: String,
    val prayerTime: String = "ALL", // "ALL", "SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA", "JUMAT"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

