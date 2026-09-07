package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MasjidRepository(
    private val database: MasjidDatabase,
    private val context: android.content.Context? = null
) {

    val configFlow: Flow<MosqueConfig> = database.mosqueConfigDao().getConfigFlow().map { entity ->
        if (entity != null) {
            MosqueConfig(
                mosqueName = entity.mosqueName,
                tagline = entity.tagline,
                address = entity.address,
                city = entity.city,
                phone = entity.phone,
                bankName = entity.bankName,
                bankAccount = entity.bankAccount,
                bankAccountName = entity.bankAccountName,
                latitude = entity.latitude,
                longitude = entity.longitude,
                timezone = entity.timezone,
                calculationMethod = entity.calculationMethod,
                offsetImsak = entity.offsetImsak,
                offsetSubuh = entity.offsetSubuh,
                offsetTerbit = entity.offsetTerbit,
                offsetDhuha = entity.offsetDhuha,
                offsetDzuhur = entity.offsetDzuhur,
                offsetAshar = entity.offsetAshar,
                offsetMaghrib = entity.offsetMaghrib,
                offsetIsya = entity.offsetIsya,
                iqomahSubuh = entity.iqomahSubuh,
                iqomahDzuhur = entity.iqomahDzuhur,
                iqomahAshar = entity.iqomahAshar,
                iqomahMaghrib = entity.iqomahMaghrib,
                iqomahIsya = entity.iqomahIsya,
                sholatDurationMinutes = entity.sholatDurationMinutes,
                hijriAdjustmentDays = entity.hijriAdjustmentDays,
                activeTheme = entity.activeTheme,
                displayLayoutModel = entity.displayLayoutModel,
                carouselIntervalSeconds = entity.carouselIntervalSeconds,
                soundAlertEnabled = entity.soundAlertEnabled,
                showImsak = entity.showImsak,
                showSubuh = entity.showSubuh,
                showTerbit = entity.showTerbit,
                showDhuha = entity.showDhuha,
                showDzuhur = entity.showDzuhur,
                showAshar = entity.showAshar,
                showMaghrib = entity.showMaghrib,
                showIsya = entity.showIsya,
                showFinancialReport = entity.showFinancialReport,
                showFridayOfficers = entity.showFridayOfficers,
                showActivities = entity.showActivities,
                showDailyMaklumat = entity.showDailyMaklumat,
                customBackgroundImagePath = entity.customBackgroundImagePath,
                customBackgroundDim = entity.customBackgroundDim,
                mainScreenBgPreset = entity.mainScreenBgPreset,
                financeBgPreset = entity.financeBgPreset,
                fridayOfficerBgPreset = entity.fridayOfficerBgPreset,
                countdownBgPreset = entity.countdownBgPreset,
                customFinanceBgPath = entity.customFinanceBgPath,
                customFridayOfficerBgPath = entity.customFridayOfficerBgPath,
                customCountdownBgPath = entity.customCountdownBgPath,
                centerCardTransparency = entity.centerCardTransparency,
                qrisImagePath = entity.qrisImagePath,
                qrisLabel = entity.qrisLabel,
                donationProgramTitle = entity.donationProgramTitle,
                donationTargetAmount = entity.donationTargetAmount,
                donationCollectedAmount = entity.donationCollectedAmount,
                showQrisCard = entity.showQrisCard,
                showDailyHadith = entity.showDailyHadith,
                webAdminPassword = entity.webAdminPassword,
                enablePreAdhanCountdown = entity.enablePreAdhanCountdown,
                preAdhanCountdownSeconds = entity.preAdhanCountdownSeconds,
                imamSubuh = entity.imamSubuh,
                imamDzuhur = entity.imamDzuhur,
                imamAshar = entity.imamAshar,
                imamMaghrib = entity.imamMaghrib,
                imamIsya = entity.imamIsya,
                muadzinSubuh = entity.muadzinSubuh,
                muadzinDzuhur = entity.muadzinDzuhur,
                muadzinAshar = entity.muadzinAshar,
                muadzinMaghrib = entity.muadzinMaghrib,
                muadzinIsya = entity.muadzinIsya,
                runningTextFontSize = entity.runningTextFontSize,
                murottalEnabled = entity.murottalEnabled,
                murottalSubuh = entity.murottalSubuh,
                murottalDzuhur = entity.murottalDzuhur,
                murottalAshar = entity.murottalAshar,
                murottalMaghrib = entity.murottalMaghrib,
                murottalIsya = entity.murottalIsya,
                murottalJumat = entity.murottalJumat,
                murottalDurationMinutes = entity.murottalDurationMinutes,
                murottalVolume = entity.murottalVolume,
                murottalSourceType = entity.murottalSourceType,
                murottalSelectedPresetId = entity.murottalSelectedPresetId,
                murottalCustomAudioPath = entity.murottalCustomAudioPath,
                customLogoImagePath = entity.customLogoImagePath,
                youtubeLiveUrl = entity.youtubeLiveUrl,
                youtubeLiveEnabled = entity.youtubeLiveEnabled,
                youtubeLiveTitle = entity.youtubeLiveTitle,
                showYoutubeLiveSlide = entity.showYoutubeLiveSlide,
                youtubeLiveMuted = entity.youtubeLiveMuted,

                // --- Video Lokal (Background & Fullscreen) & CCTV Background ---
                backgroundType = entity.backgroundType,
                backgroundVideoPath = entity.backgroundVideoPath,
                backgroundCctvCameraId = entity.backgroundCctvCameraId,
                fullscreenVideoPath = entity.fullscreenVideoPath,
                fullscreenVideoTitle = entity.fullscreenVideoTitle,
                fullscreenVideoMuted = entity.fullscreenVideoMuted,
                fullscreenVideoPlaying = entity.fullscreenVideoPlaying,

                // --- CCTV / IP Camera Live Streaming (RTSP) & Multi-Camera ---
                cctvStreamUrl = entity.cctvStreamUrl,
                cctvStreamEnabled = entity.cctvStreamEnabled,
                cctvStreamTitle = entity.cctvStreamTitle,
                showCctvSlide = entity.showCctvSlide,
                cctvStreamMuted = entity.cctvStreamMuted,
                liveStreamType = entity.liveStreamType,
                cctvCamerasJson = entity.cctvCamerasJson,
                cctvActiveCameraId = entity.cctvActiveCameraId,
                cctvAutoRotateEnabled = entity.cctvAutoRotateEnabled,
                cctvAutoRotateIntervalSeconds = entity.cctvAutoRotateIntervalSeconds,
                cctvDisplayLayout = entity.cctvDisplayLayout,
                cctvAutoPlayAfterPrayer = entity.cctvAutoPlayAfterPrayer,
                cctvAfterPrayerDurationMinutes = entity.cctvAfterPrayerDurationMinutes,
                cctvAfterPrayerWaktu = entity.cctvAfterPrayerWaktu,
                cctvAfterPrayerCameraId = entity.cctvAfterPrayerCameraId,
                cctvScheduleEnabled = entity.cctvScheduleEnabled,
                cctvScheduleStartTime = entity.cctvScheduleStartTime,
                cctvScheduleEndTime = entity.cctvScheduleEndTime,
                cctvScheduleDays = entity.cctvScheduleDays,
                cctvScheduleCameraId = entity.cctvScheduleCameraId,

                // --- Kustomisasi Teks & Warna Laporan Keuangan ---
                financeTitleText = entity.financeTitleText,
                financeTitleColor = entity.financeTitleColor,
                financeBalanceColor = entity.financeBalanceColor,
                financeIncomeColor = entity.financeIncomeColor,
                financeExpenseColor = entity.financeExpenseColor,
                financeFooterColor = entity.financeFooterColor,

                // --- Kustomisasi Teks & Warna Petugas Jum'at ---
                fridayTitleText = entity.fridayTitleText,
                fridayTitleColor = entity.fridayTitleColor,
                fridayOfficerNameColor = entity.fridayOfficerNameColor,
                fridayOfficerLabelColor = entity.fridayOfficerLabelColor,

                // --- Kustomisasi Teks & Warna Mutiara Hadits ---
                hadithTitleText = entity.hadithTitleText,
                hadithTitleColor = entity.hadithTitleColor,
                hadithThemeText = entity.hadithThemeText,
                hadithThemeColor = entity.hadithThemeColor,
                hadithArabicText = entity.hadithArabicText,
                hadithArabicColor = entity.hadithArabicColor,
                hadithTranslationText = entity.hadithTranslationText,
                hadithTranslationColor = entity.hadithTranslationColor,
                hadithNarratorText = entity.hadithNarratorText,
                hadithNarratorColor = entity.hadithNarratorColor,

                // --- Kustomisasi Teks & Warna Maklumat & Adab Masjid ---
                maklumatTitleLeft = entity.maklumatTitleLeft,
                maklumatTitleLeftColor = entity.maklumatTitleLeftColor,
                maklumatTextLeft = entity.maklumatTextLeft,
                maklumatTextLeftColor = entity.maklumatTextLeftColor,
                maklumatTitleRight = entity.maklumatTitleRight,
                maklumatTitleRightColor = entity.maklumatTitleRightColor,
                maklumatPoint1 = entity.maklumatPoint1,
                maklumatPoint2 = entity.maklumatPoint2,
                maklumatPoint3 = entity.maklumatPoint3,
                maklumatPointsColor = entity.maklumatPointsColor,

                // --- Kustomisasi Font & Warna Jam Digital TV ---
                clockFontFamily = entity.clockFontFamily,
                clockColor = entity.clockColor,
                clockColonColor = entity.clockColonColor,
                clockSecondsColor = entity.clockSecondsColor,

                // --- Pengaturan WhatsApp Gateway (Fonnte) ---
                waGatewayEnabled = entity.waGatewayEnabled,
                waGatewayProvider = entity.waGatewayProvider,
                waGatewayToken = entity.waGatewayToken,
                waGatewaySendThursdayHour = entity.waGatewaySendThursdayHour,
                waGatewaySendFridayHour = entity.waGatewaySendFridayHour,
                waGatewayLastSentThursdayDate = entity.waGatewayLastSentThursdayDate,
                waGatewayLastSentFridayDate = entity.waGatewayLastSentFridayDate,
                waGatewayTemplateThursday = entity.waGatewayTemplateThursday,
                waGatewayTemplateFriday = entity.waGatewayTemplateFriday,
                waGatewayTemplateKajian = entity.waGatewayTemplateKajian,
                waGatewayLastLogJson = entity.waGatewayLastLogJson
            )
        } else {
            MosqueConfig()
        }
    }

    val financesFlow: Flow<List<FinanceTransaction>> = database.financeDao().getAllTransactionsFlow().map { list ->
        list.map { entity ->
            FinanceTransaction(
                id = entity.id,
                title = entity.title,
                amount = entity.amount,
                type = if (entity.type == "EXPENSE") TransactionType.EXPENSE else TransactionType.INCOME,
                category = entity.category,
                date = entity.date,
                notes = entity.notes
            )
        }
    }

    val allFridaySchedulesFlow: Flow<List<FridaySchedule>> = database.fridayOfficerDao().getAllSchedulesFlow().map { list ->
        list.map { entity ->
            FridaySchedule(
                id = entity.id,
                date = entity.date,
                hijriDate = entity.hijriDate,
                khotib = entity.khotib,
                khotibPhone = entity.khotibPhone,
                imam = entity.imam,
                imamPhone = entity.imamPhone,
                muadzin = entity.muadzin,
                muadzinPhone = entity.muadzinPhone,
                bilal = entity.bilal,
                bilalPhone = entity.bilalPhone,
                khutbahTopic = entity.khutbahTopic,
                notes = entity.notes
            )
        }
    }

    val fridayScheduleFlow: Flow<FridaySchedule> = database.fridayOfficerDao().getScheduleFlow(1).map { entity ->
        if (entity != null) {
            FridaySchedule(
                id = entity.id,
                date = entity.date,
                hijriDate = entity.hijriDate,
                khotib = entity.khotib,
                khotibPhone = entity.khotibPhone,
                imam = entity.imam,
                imamPhone = entity.imamPhone,
                muadzin = entity.muadzin,
                muadzinPhone = entity.muadzinPhone,
                bilal = entity.bilal,
                bilalPhone = entity.bilalPhone,
                khutbahTopic = entity.khutbahTopic,
                notes = entity.notes
            )
        } else {
            FridaySchedule(
                id = 1,
                date = "Jum'at",
                hijriDate = "Jum'at Barakah",
                khotib = "Prof. Dr. KH. Ahmad Syakir, M.A.",
                khotibPhone = "",
                imam = "Ustadz H. M. Firdaus Al-Hafidz",
                imamPhone = "",
                muadzin = "Ustadz Bilal Ramadhan",
                muadzinPhone = "",
                bilal = "Akhi Muhammad Syahril",
                bilalPhone = "",
                khutbahTopic = "Menjaga Keistiqomahan Ibadah & Keikhlasan Hati"
            )
        }
    }

    val activitiesFlow: Flow<List<MosqueActivity>> = database.mosqueActivityDao().getAllActivitiesFlow().map { list ->
        list.map { entity ->
            MosqueActivity(
                id = entity.id,
                title = entity.title,
                speaker = entity.speaker,
                speakerPhone = entity.speakerPhone,
                date = entity.date,
                time = entity.time,
                location = entity.location,
                description = entity.description,
                category = entity.category
            )
        }
    }

    val activeRunningTextsFlow: Flow<List<RunningTextItem>> = database.runningTextDao().getActiveRunningTextsFlow().map { list ->
        list.map { entity ->
            RunningTextItem(
                id = entity.id,
                text = entity.text,
                isActive = entity.isActive,
                order = entity.itemOrder
            )
        }
    }

    val allRunningTextsFlow: Flow<List<RunningTextItem>> = database.runningTextDao().getAllRunningTextsFlow().map { list ->
        list.map { entity ->
            RunningTextItem(
                id = entity.id,
                text = entity.text,
                isActive = entity.isActive,
                order = entity.itemOrder
            )
        }
    }

    val activeMediaSlidesFlow: Flow<List<MediaSlide>> = database.mediaSlideDao().getActiveSlidesFlow().map { list ->
        list.map { entity ->
            MediaSlide(
                id = entity.id,
                filePath = entity.filePath,
                title = entity.title,
                durationSeconds = entity.durationSeconds,
                isActive = entity.isActive,
                order = entity.itemOrder
            )
        }
    }

    val dailyImamSchedulesFlow: Flow<List<DailyImamSchedule>> = database.dailyImamScheduleDao().getAllSchedulesFlow().map { list ->
        list.map { entity ->
            val pName = try {
                PrayerName.valueOf(entity.prayerName)
            } catch (_: Exception) {
                PrayerName.DZUHUR
            }
            DailyImamSchedule(
                id = entity.id,
                date = entity.date,
                prayerName = pName,
                imamName = entity.imamName,
                muadzinName = entity.muadzinName,
                notes = entity.notes
            )
        }
    }

    val allMediaSlidesFlow: Flow<List<MediaSlide>> = database.mediaSlideDao().getAllSlidesFlow().map { list ->
        list.map { entity ->
            MediaSlide(
                id = entity.id,
                filePath = entity.filePath,
                title = entity.title,
                durationSeconds = entity.durationSeconds,
                isActive = entity.isActive,
                order = entity.itemOrder
            )
        }
    }

    val allMurottalAudiosFlow: Flow<List<MurottalAudioItem>> = database.murottalAudioDao().getAllAudiosFlow().map { list ->
        list.map { entity ->
            MurottalAudioItem(
                id = entity.id,
                title = entity.title,
                qari = entity.qari,
                surah = entity.surah,
                durationSeconds = entity.durationSeconds,
                filePath = entity.filePath,
                prayerTime = entity.prayerTime,
                isDefault = entity.isDefault,
                createdAt = entity.createdAt
            )
        }
    }


    suspend fun saveConfig(config: MosqueConfig) {
        database.mosqueConfigDao().insertOrUpdate(
            MosqueConfigEntity(
                id = 1,
                mosqueName = config.mosqueName,
                tagline = config.tagline,
                address = config.address,
                city = config.city,
                phone = config.phone,
                bankName = config.bankName,
                bankAccount = config.bankAccount,
                bankAccountName = config.bankAccountName,
                latitude = config.latitude,
                longitude = config.longitude,
                timezone = config.timezone,
                calculationMethod = config.calculationMethod,
                offsetImsak = config.offsetImsak,
                offsetSubuh = config.offsetSubuh,
                offsetTerbit = config.offsetTerbit,
                offsetDhuha = config.offsetDhuha,
                offsetDzuhur = config.offsetDzuhur,
                offsetAshar = config.offsetAshar,
                offsetMaghrib = config.offsetMaghrib,
                offsetIsya = config.offsetIsya,
                iqomahSubuh = config.iqomahSubuh,
                iqomahDzuhur = config.iqomahDzuhur,
                iqomahAshar = config.iqomahAshar,
                iqomahMaghrib = config.iqomahMaghrib,
                iqomahIsya = config.iqomahIsya,
                sholatDurationMinutes = config.sholatDurationMinutes,
                hijriAdjustmentDays = config.hijriAdjustmentDays,
                activeTheme = config.activeTheme,
                displayLayoutModel = config.displayLayoutModel,
                carouselIntervalSeconds = config.carouselIntervalSeconds,
                soundAlertEnabled = config.soundAlertEnabled,
                showImsak = config.showImsak,
                showSubuh = config.showSubuh,
                showTerbit = config.showTerbit,
                showDhuha = config.showDhuha,
                showDzuhur = config.showDzuhur,
                showAshar = config.showAshar,
                showMaghrib = config.showMaghrib,
                showIsya = config.showIsya,
                showFinancialReport = config.showFinancialReport,
                showFridayOfficers = config.showFridayOfficers,
                showActivities = config.showActivities,
                showDailyMaklumat = config.showDailyMaklumat,
                customBackgroundImagePath = config.customBackgroundImagePath,
                customBackgroundDim = config.customBackgroundDim,
                mainScreenBgPreset = config.mainScreenBgPreset,
                financeBgPreset = config.financeBgPreset,
                fridayOfficerBgPreset = config.fridayOfficerBgPreset,
                countdownBgPreset = config.countdownBgPreset,
                customFinanceBgPath = config.customFinanceBgPath,
                customFridayOfficerBgPath = config.customFridayOfficerBgPath,
                customCountdownBgPath = config.customCountdownBgPath,
                centerCardTransparency = config.centerCardTransparency,
                qrisImagePath = config.qrisImagePath,
                qrisLabel = config.qrisLabel,
                donationProgramTitle = config.donationProgramTitle,
                donationTargetAmount = config.donationTargetAmount,
                donationCollectedAmount = config.donationCollectedAmount,
                showQrisCard = config.showQrisCard,
                showDailyHadith = config.showDailyHadith,
                webAdminPassword = config.webAdminPassword,
                enablePreAdhanCountdown = config.enablePreAdhanCountdown,
                preAdhanCountdownSeconds = config.preAdhanCountdownSeconds,
                imamSubuh = config.imamSubuh,
                imamDzuhur = config.imamDzuhur,
                imamAshar = config.imamAshar,
                imamMaghrib = config.imamMaghrib,
                imamIsya = config.imamIsya,
                muadzinSubuh = config.muadzinSubuh,
                muadzinDzuhur = config.muadzinDzuhur,
                muadzinAshar = config.muadzinAshar,
                muadzinMaghrib = config.muadzinMaghrib,
                muadzinIsya = config.muadzinIsya,
                runningTextFontSize = config.runningTextFontSize,
                murottalEnabled = config.murottalEnabled,
                murottalSubuh = config.murottalSubuh,
                murottalDzuhur = config.murottalDzuhur,
                murottalAshar = config.murottalAshar,
                murottalMaghrib = config.murottalMaghrib,
                murottalIsya = config.murottalIsya,
                murottalJumat = config.murottalJumat,
                murottalDurationMinutes = config.murottalDurationMinutes,
                murottalVolume = config.murottalVolume,
                murottalSourceType = config.murottalSourceType,
                murottalSelectedPresetId = config.murottalSelectedPresetId,
                murottalCustomAudioPath = config.murottalCustomAudioPath,
                customLogoImagePath = config.customLogoImagePath,
                youtubeLiveUrl = config.youtubeLiveUrl,
                youtubeLiveEnabled = config.youtubeLiveEnabled,
                youtubeLiveTitle = config.youtubeLiveTitle,
                showYoutubeLiveSlide = config.showYoutubeLiveSlide,
                youtubeLiveMuted = config.youtubeLiveMuted,

                // --- Video Lokal (Background & Fullscreen) & CCTV Background ---
                backgroundType = config.backgroundType,
                backgroundVideoPath = config.backgroundVideoPath,
                backgroundCctvCameraId = config.backgroundCctvCameraId,
                fullscreenVideoPath = config.fullscreenVideoPath,
                fullscreenVideoTitle = config.fullscreenVideoTitle,
                fullscreenVideoMuted = config.fullscreenVideoMuted,
                fullscreenVideoPlaying = config.fullscreenVideoPlaying,

                // --- CCTV / IP Camera Live Streaming (RTSP) & Multi-Camera ---
                cctvStreamUrl = config.cctvStreamUrl,
                cctvStreamEnabled = config.cctvStreamEnabled,
                cctvStreamTitle = config.cctvStreamTitle,
                showCctvSlide = config.showCctvSlide,
                cctvStreamMuted = config.cctvStreamMuted,
                liveStreamType = config.liveStreamType,
                cctvCamerasJson = config.cctvCamerasJson,
                cctvActiveCameraId = config.cctvActiveCameraId,
                cctvAutoRotateEnabled = config.cctvAutoRotateEnabled,
                cctvAutoRotateIntervalSeconds = config.cctvAutoRotateIntervalSeconds,
                cctvDisplayLayout = config.cctvDisplayLayout,
                cctvAutoPlayAfterPrayer = config.cctvAutoPlayAfterPrayer,
                cctvAfterPrayerDurationMinutes = config.cctvAfterPrayerDurationMinutes,
                cctvAfterPrayerWaktu = config.cctvAfterPrayerWaktu,
                cctvAfterPrayerCameraId = config.cctvAfterPrayerCameraId,
                cctvScheduleEnabled = config.cctvScheduleEnabled,
                cctvScheduleStartTime = config.cctvScheduleStartTime,
                cctvScheduleEndTime = config.cctvScheduleEndTime,
                cctvScheduleDays = config.cctvScheduleDays,
                cctvScheduleCameraId = config.cctvScheduleCameraId,

                // --- Kustomisasi Teks & Warna Laporan Keuangan ---
                financeTitleText = config.financeTitleText,
                financeTitleColor = config.financeTitleColor,
                financeBalanceColor = config.financeBalanceColor,
                financeIncomeColor = config.financeIncomeColor,
                financeExpenseColor = config.financeExpenseColor,
                financeFooterColor = config.financeFooterColor,

                // --- Kustomisasi Teks & Warna Petugas Jum'at ---
                fridayTitleText = config.fridayTitleText,
                fridayTitleColor = config.fridayTitleColor,
                fridayOfficerNameColor = config.fridayOfficerNameColor,
                fridayOfficerLabelColor = config.fridayOfficerLabelColor,

                // --- Kustomisasi Teks & Warna Mutiara Hadits ---
                hadithTitleText = config.hadithTitleText,
                hadithTitleColor = config.hadithTitleColor,
                hadithThemeText = config.hadithThemeText,
                hadithThemeColor = config.hadithThemeColor,
                hadithArabicText = config.hadithArabicText,
                hadithArabicColor = config.hadithArabicColor,
                hadithTranslationText = config.hadithTranslationText,
                hadithTranslationColor = config.hadithTranslationColor,
                hadithNarratorText = config.hadithNarratorText,
                hadithNarratorColor = config.hadithNarratorColor,

                // --- Kustomisasi Teks & Warna Maklumat & Adab Masjid ---
                maklumatTitleLeft = config.maklumatTitleLeft,
                maklumatTitleLeftColor = config.maklumatTitleLeftColor,
                maklumatTextLeft = config.maklumatTextLeft,
                maklumatTextLeftColor = config.maklumatTextLeftColor,
                maklumatTitleRight = config.maklumatTitleRight,
                maklumatTitleRightColor = config.maklumatTitleRightColor,
                maklumatPoint1 = config.maklumatPoint1,
                maklumatPoint2 = config.maklumatPoint2,
                maklumatPoint3 = config.maklumatPoint3,
                maklumatPointsColor = config.maklumatPointsColor,

                // --- Kustomisasi Font & Warna Jam Digital TV ---
                clockFontFamily = config.clockFontFamily,
                clockColor = config.clockColor,
                clockColonColor = config.clockColonColor,
                clockSecondsColor = config.clockSecondsColor,

                // --- Pengaturan WhatsApp Gateway (Fonnte) ---
                waGatewayEnabled = config.waGatewayEnabled,
                waGatewayProvider = config.waGatewayProvider,
                waGatewayToken = config.waGatewayToken,
                waGatewaySendThursdayHour = config.waGatewaySendThursdayHour,
                waGatewaySendFridayHour = config.waGatewaySendFridayHour,
                waGatewayLastSentThursdayDate = config.waGatewayLastSentThursdayDate,
                waGatewayLastSentFridayDate = config.waGatewayLastSentFridayDate,
                waGatewayTemplateThursday = config.waGatewayTemplateThursday,
                waGatewayTemplateFriday = config.waGatewayTemplateFriday,
                waGatewayTemplateKajian = config.waGatewayTemplateKajian,
                waGatewayLastLogJson = config.waGatewayLastLogJson
            )
        )
    }

    suspend fun updateWaGatewayLastSent(thursdayDate: String? = null, fridayDate: String? = null, logJson: String? = null) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                waGatewayLastSentThursdayDate = thursdayDate ?: current.waGatewayLastSentThursdayDate,
                waGatewayLastSentFridayDate = fridayDate ?: current.waGatewayLastSentFridayDate,
                waGatewayLastLogJson = logJson ?: current.waGatewayLastLogJson
            )
        )
    }

    suspend fun addFinance(title: String, amount: Long, type: TransactionType, category: String, date: String, notes: String): Long {
        return database.financeDao().insert(
            FinanceEntity(
                title = title,
                amount = amount,
                type = type.name,
                category = category,
                date = date,
                notes = notes
            )
        )
    }

    suspend fun updateFinance(id: Long, title: String, amount: Long, type: TransactionType, category: String, date: String, notes: String) {
        database.financeDao().update(
            FinanceEntity(
                id = id,
                title = title,
                amount = amount,
                type = type.name,
                category = category,
                date = date,
                notes = notes
            )
        )
    }

    suspend fun deleteFinance(id: Long) {
        database.financeDao().deleteById(id)
    }

    suspend fun saveFridaySchedule(schedule: FridaySchedule) {
        database.fridayOfficerDao().insertOrUpdate(
            FridayOfficerEntity(
                id = if (schedule.id in 1..5) schedule.id else 1,
                date = schedule.date,
                hijriDate = schedule.hijriDate,
                khotib = schedule.khotib,
                khotibPhone = schedule.khotibPhone,
                imam = schedule.imam,
                imamPhone = schedule.imamPhone,
                muadzin = schedule.muadzin,
                muadzinPhone = schedule.muadzinPhone,
                bilal = schedule.bilal,
                bilalPhone = schedule.bilalPhone,
                khutbahTopic = schedule.khutbahTopic,
                notes = schedule.notes
            )
        )
    }

    suspend fun addActivity(title: String, speaker: String, speakerPhone: String = "", date: String, time: String, location: String, description: String, category: String): Long {
        return database.mosqueActivityDao().insert(
            MosqueActivityEntity(
                title = title,
                speaker = speaker,
                speakerPhone = speakerPhone,
                date = date,
                time = time,
                location = location,
                description = description,
                category = category
            )
        )
    }

    suspend fun updateActivity(id: Long, title: String, speaker: String, speakerPhone: String = "", date: String, time: String, location: String, description: String, category: String) {
        database.mosqueActivityDao().update(
            MosqueActivityEntity(
                id = id,
                title = title,
                speaker = speaker,
                speakerPhone = speakerPhone,
                date = date,
                time = time,
                location = location,
                description = description,
                category = category
            )
        )
    }

    suspend fun deleteActivity(id: Long) {
        database.mosqueActivityDao().deleteById(id)
    }

    suspend fun addRunningText(text: String, isActive: Boolean = true, order: Int = 0): Long {
        return database.runningTextDao().insert(
            RunningTextEntity(
                text = text,
                isActive = isActive,
                itemOrder = order
            )
        )
    }

    suspend fun updateRunningText(id: Long, text: String, isActive: Boolean, order: Int) {
        database.runningTextDao().update(
            RunningTextEntity(
                id = id,
                text = text,
                isActive = isActive,
                itemOrder = order
            )
        )
    }

    suspend fun deleteRunningText(id: Long) {
        database.runningTextDao().deleteById(id)
    }

    suspend fun addMediaSlide(filePath: String, title: String, durationSeconds: Int = 15, isActive: Boolean = true, order: Int = 0): Long {
        return database.mediaSlideDao().insert(
            MediaSlideEntity(
                filePath = filePath,
                title = title,
                durationSeconds = durationSeconds,
                isActive = isActive,
                itemOrder = order
            )
        )
    }

    suspend fun updateMediaSlide(id: Long, title: String, durationSeconds: Int, isActive: Boolean, order: Int) {
        val existing = database.mediaSlideDao().getById(id) ?: return
        database.mediaSlideDao().update(
            existing.copy(
                title = title,
                durationSeconds = durationSeconds,
                isActive = isActive,
                itemOrder = order
            )
        )
    }

    suspend fun deleteMediaSlide(id: Long) {
        database.mediaSlideDao().deleteById(id)
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentConfig = database.mosqueConfigDao().getConfig()
        if (currentConfig == null ||
            currentConfig.mosqueName == "Masjid Raya Baitul Muttaqin" ||
            currentConfig.mosqueName == "Masjid Agung Al-Falah" ||
            currentConfig.mosqueName.isBlank()) {
            database.mosqueConfigDao().insertOrUpdate(
                (currentConfig ?: MosqueConfigEntity(id = 1)).copy(
                    mosqueName = "MASJID BAITURROHMAN",
                    tagline = "Pusat Ibadah, Pembinaan Umat, & Dakwah Islam",
                    address = "Desa Ujunggebang, Kecamatan Sukra - Indramayu",
                    city = "Indramayu, Jawa Barat",
                    phone = "0812-2214-534",
                    bankName = "Bank BRI (Bank Rakyat Indonesia)",
                    bankAccount = "4206 0101 2214 534",
                    bankAccountName = "DKM BAITUR ROHMAN",
                    latitude = -6.3078,
                    longitude = 107.9945,
                    timezone = 7.0,
                    donationProgramTitle = "Renovasi & Pemakmuran Masjid Baiturrohman"
                )
            )
        }

        val runningTexts = database.runningTextDao().getAllRunningTexts()
        if (runningTexts.isEmpty() || runningTexts.any { it.text.contains("Al-Falah") || it.text.contains("Baitul Muttaqin") }) {
            database.runningTextDao().deleteAll()
            val newRunningTexts = listOf(
                RunningTextEntity(
                    text = "Selamat Datang di MASJID BAITURROHMAN Desa Ujunggebang Kecamatan Sukra - Indramayu Jawa Barat. Mohon luruskan dan rapatkan shaf sholat demi kesempurnaan sholat berjamaah.",
                    isActive = true,
                    itemOrder = 1
                ),
                RunningTextEntity(
                    text = "Rasulullah ﷺ bersabda: 'Barangsiapa membangun masjid karena Allah, maka Allah akan membangunkan baginya rumah di surga.' (HR. Bukhari & Muslim)",
                    isActive = true,
                    itemOrder = 2
                ),
                RunningTextEntity(
                    text = "Mohon menjaga kesucian dan kebersihan tempat wudhu, menonaktifkan nada dering handphone, dan menjaga ketertiban bersama.",
                    isActive = true,
                    itemOrder = 3
                ),
                RunningTextEntity(
                    text = "Infaq & Shadaqah dapat disalurkan melalui Bank BRI No. Rek: 4206 0101 2214 534 a.n DKM BAITUR ROHMAN atau melalui tromol kotak infaq masjid.",
                    isActive = true,
                    itemOrder = 4
                )
            )
            for (rt in newRunningTexts) {
                database.runningTextDao().insert(rt)
            }
        }

        val finances = database.financeDao().getAllTransactions()
        if (finances.isEmpty()) {
            database.financeDao().insert(
                FinanceEntity(
                    title = "Infaq Kotak Amal Jum'at",
                    amount = 14850000,
                    type = "INCOME",
                    category = "Kotak Amal Jum'at",
                    date = "Jum'at",
                    notes = "Kotak keliling jamaah"
                )
            )
            database.financeDao().insert(
                FinanceEntity(
                    title = "Infaq QRIS & Donatur Tetap",
                    amount = 6750000,
                    type = "INCOME",
                    category = "Infaq Digital",
                    date = "Senin",
                    notes = "Rekening BRI DKM BAITUR ROHMAN"
                )
            )
            database.financeDao().insert(
                FinanceEntity(
                    title = "Infaq Tromol / Kotak Harian",
                    amount = 3200000,
                    type = "INCOME",
                    category = "Kotak Harian",
                    date = "Rabu",
                    notes = "Kotak serbaguna selasar"
                )
            )
            database.financeDao().insert(
                FinanceEntity(
                    title = "Tagihan Listrik PLN & Air Bersih PAM",
                    amount = 2850000,
                    type = "EXPENSE",
                    category = "Operasional Listrik & Air",
                    date = "Selasa",
                    notes = "Beban daya AC & Sound System"
                )
            )
            database.financeDao().insert(
                FinanceEntity(
                    title = "Honor Penceramah & Imam Rawatib",
                    amount = 2500000,
                    type = "EXPENSE",
                    category = "Insentif Asatidz",
                    date = "Kamis",
                    notes = "Insentif bulanan"
                )
            )
        }

        val sampleOfficers = listOf(
            Triple("Prof. Dr. KH. Ahmad Syakir, M.A.", "Ustadz H. M. Firdaus Al-Hafidz", "Menjaga Keistiqomahan Ibadah & Keikhlasan Hati"),
            Triple("Dr. KH. Abdullah Gymnastiar", "Ustadz M. Ridwan Al-Banjari", "Merajut Ukhuwah Islamiyah di Era Digital"),
            Triple("Ustadz Adi Hidayat, Lc., M.A.", "Ustadz H. Ahmad Dahlan, S.Q.", "Kunci Ketenangan Jiwa Melalui Sholat Khusyuk"),
            Triple("Prof. Dr. KH. Nasaruddin Umar, M.A.", "Ustadz Salman Al-Farisi", "Membangun Generasi Rabbani Pecinta Al-Qur'an"),
            Triple("Ustadz Abdul Somad, Lc., D.E.S.A.", "Ustadz H. M. Firdaus Al-Hafidz", "Urgensi Sedekah & Pembersihan Harta Jamaah")
        )

        val fridaySchedules = database.fridayOfficerDao().getAllSchedules()
        if (fridaySchedules.isEmpty()) {
            for (i in 1..5) {
                val (khotibName, imamName, topic) = sampleOfficers[i - 1]
                database.fridayOfficerDao().insertOrUpdate(
                    FridayOfficerEntity(
                        id = i.toLong(),
                        date = "Jum'at Ke-$i",
                        hijriDate = "Jum'at Barakah",
                        khotib = khotibName,
                        imam = imamName,
                        muadzin = "Ustadz Bilal Ramadhan",
                        bilal = "Akhi Muhammad Syahril",
                        khutbahTopic = topic,
                        notes = "Jadwal Petugas Sholat Jum'at Pekan Ke-$i"
                    )
                )
            }
        } else {
            fun isPlaceholder(str: String): Boolean {
                val lower = str.trim().lowercase()
                return str.isBlank() || str == "-" ||
                        lower == "khotib" || lower == "khotib sholat" || lower == "khotib sholat jum'at" || lower == "ustadz khotib" || lower == "ustadz khatib" ||
                        lower == "imam" || lower == "imam sholat" || lower == "ustadz imam" ||
                        lower == "muadzin" || lower == "ustadz muadzin" ||
                        lower == "bilal" || lower == "bilal / muraqqi" || lower == "ustadz bilal"
            }

            // Update existing if any empty/blank or placeholder fields
            for (schedule in fridaySchedules) {
                val idx = ((schedule.id - 1) % 5).toInt().coerceIn(0, 4)
                val (khotibName, imamName, topic) = sampleOfficers[idx]
                val needsUpdate = isPlaceholder(schedule.khotib) || isPlaceholder(schedule.imam) ||
                        isPlaceholder(schedule.muadzin) || isPlaceholder(schedule.bilal) ||
                        schedule.khutbahTopic.isBlank() || schedule.khutbahTopic == "-" || schedule.khutbahTopic == "Taqwa Kepada Allah SWT"
                if (needsUpdate) {
                    database.fridayOfficerDao().insertOrUpdate(
                        schedule.copy(
                            khotib = if (!isPlaceholder(schedule.khotib)) schedule.khotib else khotibName,
                            imam = if (!isPlaceholder(schedule.imam)) schedule.imam else imamName,
                            muadzin = if (!isPlaceholder(schedule.muadzin)) schedule.muadzin else "Ustadz Bilal Ramadhan",
                            bilal = if (!isPlaceholder(schedule.bilal)) schedule.bilal else "Akhi Muhammad Syahril",
                            khutbahTopic = if (schedule.khutbahTopic.isNotBlank() && schedule.khutbahTopic != "-" && schedule.khutbahTopic != "Taqwa Kepada Allah SWT") schedule.khutbahTopic else topic
                        )
                    )
                }
            }

            // Ensure Idul Fitri (id=6) and Idul Adha (id=7) exist
            val existingIds = fridaySchedules.map { it.id }.toSet()
            if (!existingIds.contains(6L)) {
                database.fridayOfficerDao().insertOrUpdate(
                    FridayOfficerEntity(
                        id = 6L,
                        date = "1 Syawal (Idul Fitri)",
                        hijriDate = "1 Syawal",
                        khotib = "KH. Ahmad Dahlan, M.A.",
                        imam = "Ustadz H. M. Firdaus Al-Hafidz",
                        muadzin = "Ustadz Bilal Ramadhan",
                        bilal = "Akhi Muhammad Syahril",
                        khutbahTopic = "Meraih Kemenangan Hakiki & Mempererat Ukhuwah Islamiyah",
                        notes = "Jadwal Petugas Sholat Idul Fitri"
                    )
                )
            }
            if (!existingIds.contains(7L)) {
                database.fridayOfficerDao().insertOrUpdate(
                    FridayOfficerEntity(
                        id = 7L,
                        date = "10 Dzulhijjah (Idul Adha)",
                        hijriDate = "10 Dzulhijjah",
                        khotib = "Dr. H. Muchlis Muhammad, Lc., M.A.",
                        imam = "Ustadz Hasan Basri Al-Hafidz",
                        muadzin = "Ustadz Salman Al-Farisi",
                        bilal = "Akhi Rizky Ramadhan",
                        khutbahTopic = "Meneladani Keikhlasan dan Semangat Berqurban Nabi Ibrahim AS",
                        notes = "Jadwal Petugas Sholat Idul Adha"
                    )
                )
            }
        }
    }

    suspend fun addDailyImamSchedule(date: String, prayerName: PrayerName, imamName: String, muadzinName: String = "", notes: String = ""): Long {
        return database.dailyImamScheduleDao().insert(
            DailyImamScheduleEntity(
                date = date,
                prayerName = prayerName.name,
                imamName = imamName,
                muadzinName = muadzinName,
                notes = notes
            )
        )
    }

    suspend fun deleteDailyImamSchedule(id: Long) {
        database.dailyImamScheduleDao().deleteById(id)
    }

    suspend fun getSpecificDailyImamSchedule(date: String, prayer: PrayerName): DailyImamSchedule? {
        val entity = database.dailyImamScheduleDao().getScheduleByDateAndPrayer(date, prayer.name) ?: return null
        return DailyImamSchedule(
            id = entity.id,
            date = entity.date,
            prayerName = try { PrayerName.valueOf(entity.prayerName) } catch (_: Exception) { PrayerName.DZUHUR },
            imamName = entity.imamName,
            muadzinName = entity.muadzinName,
            notes = entity.notes
        )
    }

    suspend fun getActivePrayerOfficers(
        date: String,
        prayer: PrayerName,
        isFriday: Boolean,
        config: MosqueConfig,
        fridaySchedule: FridaySchedule? = null
    ): ActivePrayerOfficers {
        val specific = database.dailyImamScheduleDao().getScheduleByDateAndPrayer(date, prayer.name)

        if (isFriday && prayer == PrayerName.DZUHUR) {
            val fri = fridaySchedule
            val imam = specific?.imamName?.takeIf { it.isNotBlank() }
                ?: fri?.imam?.takeIf { it.isNotBlank() }
                ?: fri?.khotib?.takeIf { it.isNotBlank() }
                ?: config.imamDzuhur
            val muadzin = specific?.muadzinName?.takeIf { it.isNotBlank() }
                ?: fri?.muadzin?.takeIf { it.isNotBlank() }
                ?: fri?.bilal?.takeIf { it.isNotBlank() }
                ?: config.muadzinDzuhur
            val khotib = fri?.khotib ?: ""
            val bilal = fri?.bilal ?: ""
            return ActivePrayerOfficers(
                imam = imam,
                muadzin = muadzin,
                khotib = khotib,
                bilal = bilal,
                isFriday = true
            )
        }

        if (specific != null && specific.imamName.isNotBlank()) {
            val mName = if (specific.muadzinName.isNotBlank()) specific.muadzinName else getDefaultMuadzinForPrayer(prayer, config)
            return ActivePrayerOfficers(imam = specific.imamName, muadzin = mName, isFriday = false)
        }
        val defaultImam = getDefaultImamForPrayer(prayer, config)
        val defaultMuadzin = getDefaultMuadzinForPrayer(prayer, config)
        return ActivePrayerOfficers(imam = defaultImam, muadzin = defaultMuadzin, isFriday = false)
    }

    suspend fun getActiveImamAndMuadzin(date: String, prayer: PrayerName, config: MosqueConfig): Pair<String, String> {
        val officers = getActivePrayerOfficers(date, prayer, false, config)
        return Pair(officers.imam, officers.muadzin)
    }

    fun getDefaultImamForPrayer(prayer: PrayerName, config: MosqueConfig): String {
        return when (prayer) {
            PrayerName.SUBUH -> config.imamSubuh
            PrayerName.DZUHUR -> config.imamDzuhur
            PrayerName.ASHAR -> config.imamAshar
            PrayerName.MAGHRIB -> config.imamMaghrib
            PrayerName.ISYA -> config.imamIsya
            else -> ""
        }
    }

    fun getDefaultMuadzinForPrayer(prayer: PrayerName, config: MosqueConfig): String {
        return when (prayer) {
            PrayerName.SUBUH -> config.muadzinSubuh
            PrayerName.DZUHUR -> config.muadzinDzuhur
            PrayerName.ASHAR -> config.muadzinAshar
            PrayerName.MAGHRIB -> config.muadzinMaghrib
            PrayerName.ISYA -> config.muadzinIsya
            else -> ""
        }
    }

    suspend fun addMurottalAudio(title: String, qari: String, surah: String, durationSeconds: Int, filePath: String, prayerTime: String = "ALL", isDefault: Boolean = false): Long {
        return database.murottalAudioDao().insert(
            MurottalAudioEntity(
                title = title,
                qari = qari,
                surah = surah,
                durationSeconds = durationSeconds,
                filePath = filePath,
                prayerTime = prayerTime,
                isDefault = isDefault
            )
        )
    }

    suspend fun updateMurottalAudio(id: Long, title: String, qari: String, surah: String, durationSeconds: Int, filePath: String, prayerTime: String, isDefault: Boolean) {
        database.murottalAudioDao().update(
            MurottalAudioEntity(
                id = id,
                title = title,
                qari = qari,
                surah = surah,
                durationSeconds = durationSeconds,
                filePath = filePath,
                prayerTime = prayerTime,
                isDefault = isDefault
            )
        )
    }

    suspend fun deleteMurottalAudio(id: Long) {
        database.murottalAudioDao().deleteById(id)
    }

    suspend fun getMurottalAudiosForPrayer(prayer: String): List<MurottalAudioItem> {
        return database.murottalAudioDao().getForPrayer(prayer).map { entity ->
            MurottalAudioItem(
                id = entity.id,
                title = entity.title,
                qari = entity.qari,
                surah = entity.surah,
                durationSeconds = entity.durationSeconds,
                filePath = entity.filePath,
                prayerTime = entity.prayerTime,
                isDefault = entity.isDefault,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun setDefaultMurottalAudio(id: Long) {
        database.murottalAudioDao().clearAllDefaults()
        database.murottalAudioDao().setDefaultById(id)
        val audio = database.murottalAudioDao().getById(id)
        if (audio != null) {
            val current = configFlow.first()
            saveConfig(
                current.copy(
                    murottalSourceType = "MANUAL_UPLOAD",
                    murottalCustomAudioPath = audio.filePath
                )
            )
        }
    }

    suspend fun setMurottalPreset(presetId: String) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                murottalSourceType = "PRESET",
                murottalSelectedPresetId = presetId
            )
        )
    }

    suspend fun updateYoutubeLiveConfig(
        url: String,
        enabled: Boolean,
        title: String = "Live Streaming Masjid",
        showSlide: Boolean = false,
        isMuted: Boolean = false
    ) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                youtubeLiveUrl = url.trim(),
                youtubeLiveEnabled = enabled,
                youtubeLiveTitle = title.trim().ifBlank { "Live Streaming Masjid" },
                showYoutubeLiveSlide = showSlide,
                youtubeLiveMuted = isMuted
            )
        )
    }

    // --- Video Lokal Management ---
    fun getLocalVideos(): List<com.example.data.model.LocalVideoItem> {
        val videoDir = context?.filesDir?.resolve("videos") ?: return emptyList()
        if (!videoDir.exists()) {
            videoDir.mkdirs()
            return emptyList()
        }

        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "ts")

        val files = videoDir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.extension.lowercase() in videoExtensions }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val sizeBytes = file.length()
                val sizeFormatted = when {
                    sizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
                    sizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0)
                    else -> "$sizeBytes B"
                }
                com.example.data.model.LocalVideoItem(
                    id = file.name,
                    title = file.nameWithoutExtension.replace("_", " "),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = sizeBytes,
                    formattedSize = sizeFormatted,
                    isBackgroundActive = false,
                    createdAt = file.lastModified()
                )
            }
    }

    suspend fun deleteLocalVideo(fileName: String): Boolean {
        val videoDir = context?.filesDir?.resolve("videos") ?: return false
        val file = videoDir.resolve(fileName)
        if (file.exists() && file.isFile) {
            val deleted = file.delete()
            val current = configFlow.first()
            if (current.backgroundVideoPath == file.absolutePath) {
                saveConfig(current.copy(backgroundVideoPath = "", backgroundType = "PRESET"))
            }
            if (current.fullscreenVideoPath == file.absolutePath) {
                saveConfig(current.copy(fullscreenVideoPath = "", fullscreenVideoPlaying = false))
            }
            return deleted
        }
        return false
    }

    suspend fun setBackgroundVideo(filePath: String, isEnabled: Boolean = true) {
        val current = configFlow.first()
        if (isEnabled && filePath.isNotBlank()) {
            saveConfig(
                current.copy(
                    backgroundType = "VIDEO",
                    backgroundVideoPath = filePath
                )
            )
        } else {
            saveConfig(
                current.copy(
                    backgroundType = "PRESET",
                    backgroundVideoPath = ""
                )
            )
        }
    }

    suspend fun setBackgroundCctv(cameraId: String = "", isEnabled: Boolean = true) {
        val current = configFlow.first()
        if (isEnabled) {
            saveConfig(
                current.copy(
                    backgroundType = "CCTV",
                    backgroundCctvCameraId = cameraId
                )
            )
        } else {
            saveConfig(
                current.copy(
                    backgroundType = "PRESET",
                    backgroundCctvCameraId = ""
                )
            )
        }
    }

    suspend fun playFullscreenVideo(filePath: String, title: String = "", isMuted: Boolean = false) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                fullscreenVideoPath = filePath,
                fullscreenVideoTitle = title.ifBlank { "Video Dokumentasi Masjid" },
                fullscreenVideoMuted = isMuted,
                fullscreenVideoPlaying = true
            )
        )
    }

    suspend fun stopFullscreenVideo() {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                fullscreenVideoPlaying = false,
                fullscreenVideoPath = ""
            )
        )
    }

    // --- CCTV / IP Camera Streaming Methods ---
    suspend fun updateCctvConfig(
        url: String,
        enabled: Boolean,
        title: String = "Live Kamera Masjid",
        showSlide: Boolean = false,
        isMuted: Boolean = true,
        liveStreamType: String = "RTSP_CCTV"
    ) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                cctvStreamUrl = url.trim(),
                cctvStreamEnabled = enabled,
                cctvStreamTitle = title.trim().ifBlank { "Live Kamera Masjid" },
                showCctvSlide = showSlide,
                cctvStreamMuted = isMuted,
                liveStreamType = liveStreamType
            )
        )
    }

    suspend fun playFullscreenCctv(url: String = "", title: String = "", isMuted: Boolean = true) {
        val current = configFlow.first()
        val streamUrl = url.ifBlank { current.cctvStreamUrl }
        val streamTitle = title.ifBlank { current.cctvStreamTitle }.ifBlank { "Live Kamera Masjid" }
        saveConfig(
            current.copy(
                cctvStreamUrl = streamUrl,
                cctvStreamTitle = streamTitle,
                cctvStreamMuted = isMuted,
                cctvStreamEnabled = true,
                liveStreamType = "RTSP_CCTV"
            )
        )
    }

    suspend fun stopFullscreenCctv() {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                cctvStreamEnabled = false
            )
        )
    }

    suspend fun saveCctvCameras(
        cameras: List<CctvCameraItem>,
        activeCameraId: String? = null,
        autoRotate: Boolean? = null,
        autoRotateInterval: Int? = null,
        displayLayout: String? = null
    ) {
        val current = configFlow.first()
        val json = CctvCameraItem.listToJson(cameras)
        val activeId = activeCameraId ?: current.cctvActiveCameraId
        val activeCam = cameras.firstOrNull { it.id == activeId } ?: cameras.firstOrNull { it.isEnabled } ?: cameras.firstOrNull()

        saveConfig(
            current.copy(
                cctvCamerasJson = json,
                cctvActiveCameraId = activeCam?.id ?: activeId,
                cctvAutoRotateEnabled = autoRotate ?: current.cctvAutoRotateEnabled,
                cctvAutoRotateIntervalSeconds = autoRotateInterval ?: current.cctvAutoRotateIntervalSeconds,
                cctvDisplayLayout = displayLayout ?: current.cctvDisplayLayout,
                cctvStreamUrl = activeCam?.streamUrl ?: current.cctvStreamUrl,
                cctvStreamTitle = activeCam?.name ?: current.cctvStreamTitle,
                cctvStreamMuted = activeCam?.isMuted ?: current.cctvStreamMuted
            )
        )
    }

    suspend fun switchActiveCctvCamera(cameraId: String) {
        val current = configFlow.first()
        val cameras = current.getCctvCameras()
        val targetCam = cameras.firstOrNull { it.id == cameraId }
        if (targetCam != null) {
            saveConfig(
                current.copy(
                    cctvActiveCameraId = cameraId,
                    cctvStreamUrl = targetCam.streamUrl,
                    cctvStreamTitle = targetCam.name,
                    cctvStreamMuted = targetCam.isMuted,
                    cctvStreamEnabled = true,
                    liveStreamType = "RTSP_CCTV"
                )
            )
        }
    }

    suspend fun saveCctvScheduleConfig(
        autoPlayAfterPrayer: Boolean,
        afterPrayerDuration: Int,
        afterPrayerWaktu: String,
        afterPrayerCameraId: String,
        scheduleEnabled: Boolean,
        scheduleStartTime: String,
        scheduleEndTime: String,
        scheduleDays: String,
        scheduleCameraId: String
    ) {
        val current = configFlow.first()
        saveConfig(
            current.copy(
                cctvAutoPlayAfterPrayer = autoPlayAfterPrayer,
                cctvAfterPrayerDurationMinutes = afterPrayerDuration,
                cctvAfterPrayerWaktu = afterPrayerWaktu,
                cctvAfterPrayerCameraId = afterPrayerCameraId,
                cctvScheduleEnabled = scheduleEnabled,
                cctvScheduleStartTime = scheduleStartTime,
                cctvScheduleEndTime = scheduleEndTime,
                cctvScheduleDays = scheduleDays,
                cctvScheduleCameraId = scheduleCameraId
            )
        )
    }
}

