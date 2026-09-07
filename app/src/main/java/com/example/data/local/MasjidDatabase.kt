package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MosqueConfigEntity::class,
        FinanceEntity::class,
        FridayOfficerEntity::class,
        MosqueActivityEntity::class,
        RunningTextEntity::class,
        MediaSlideEntity::class,
        DailyImamScheduleEntity::class,
        MurottalAudioEntity::class,
        TarawihScheduleEntity::class
    ],
    version = 22,
    exportSchema = false
)
abstract class MasjidDatabase : RoomDatabase() {
    abstract fun mosqueConfigDao(): MosqueConfigDao
    abstract fun financeDao(): FinanceDao
    abstract fun fridayOfficerDao(): FridayOfficerDao
    abstract fun mosqueActivityDao(): MosqueActivityDao
    abstract fun runningTextDao(): RunningTextDao
    abstract fun mediaSlideDao(): MediaSlideDao
    abstract fun dailyImamScheduleDao(): DailyImamScheduleDao
    abstract fun murottalAudioDao(): MurottalAudioDao
    abstract fun tarawihScheduleDao(): TarawihScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: MasjidDatabase? = null

        private fun safeExecSQL(db: SupportSQLiteDatabase, sql: String) {
            try {
                db.execSQL(sql)
            } catch (_: Exception) {}
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showFinancialReport INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showFridayOfficers INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showActivities INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showDailyMaklumat INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN qrisImagePath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN qrisLabel TEXT NOT NULL DEFAULT 'INFAQ & SHODAQOH VIA QRIS'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN donationProgramTitle TEXT NOT NULL DEFAULT 'Renovasi Fasilitas Masjid'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN donationTargetAmount INTEGER NOT NULL DEFAULT 25000000")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN donationCollectedAmount INTEGER NOT NULL DEFAULT 16850000")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showQrisCard INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showDailyHadith INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_5 = object : androidx.room.migration.Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN webAdminPassword TEXT NOT NULL DEFAULT '123456'")
            }
        }

        val MIGRATION_4_6 = object : androidx.room.migration.Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN enablePreAdhanCountdown INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN preAdhanCountdownSeconds INTEGER NOT NULL DEFAULT 30")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN imamSubuh TEXT NOT NULL DEFAULT 'Ust. H. Ahmad Dahlan, Lc.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN imamDzuhur TEXT NOT NULL DEFAULT 'Ust. Ridwan Kamil'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN imamAshar TEXT NOT NULL DEFAULT 'Ust. M. Syukron'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN imamMaghrib TEXT NOT NULL DEFAULT 'Ust. Dr. H. Fathurrahman'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN imamIsya TEXT NOT NULL DEFAULT 'Ust. Bilal Al-Banjari'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showImamDaily INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN runningTextFontSize INTEGER NOT NULL DEFAULT 15")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN runningTextSpeed INTEGER NOT NULL DEFAULT 60")
                safeExecSQL(db, "CREATE TABLE IF NOT EXISTS daily_imam_schedules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dayOfWeek INTEGER NOT NULL, prayerName TEXT NOT NULL, imamName TEXT NOT NULL, title TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themeDesignModel TEXT NOT NULL DEFAULT 'MIHRAB_GRAND_ROYAL'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themePrimaryColor TEXT NOT NULL DEFAULT '#0D5C3A'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themeAccentColor TEXT NOT NULL DEFAULT '#D4AF37'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themeBackgroundColor TEXT NOT NULL DEFAULT '#062317'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockStyle TEXT NOT NULL DEFAULT 'DIGITAL_NEON'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockShowSeconds INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN prayerCardShape TEXT NOT NULL DEFAULT 'ARCH_ISLAMIC'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN prayerCardGlow INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themeArabicFontFamily TEXT NOT NULL DEFAULT 'Amiri'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN themeLatinFontFamily TEXT NOT NULL DEFAULT 'Cinzel'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN isDarkMode INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN prayerTimeFontSize INTEGER NOT NULL DEFAULT 22")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockFontSize INTEGER NOT NULL DEFAULT 48")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN mosqueNameFontSize INTEGER NOT NULL DEFAULT 26")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalSubuh INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalDzuhur INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalAshar INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalMaghrib INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalIsya INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalJumat INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalDurationMinutes INTEGER NOT NULL DEFAULT 10")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalVolume INTEGER NOT NULL DEFAULT 80")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalSourceType TEXT NOT NULL DEFAULT 'PRESET'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalSelectedPresetId TEXT NOT NULL DEFAULT 'mishary_ar_rahman'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN murottalCustomAudioPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "CREATE TABLE IF NOT EXISTS murottal_audio_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, qari TEXT NOT NULL, surah TEXT NOT NULL, durationSeconds INTEGER NOT NULL DEFAULT 0, filePath TEXT NOT NULL, prayerTime TEXT NOT NULL DEFAULT 'ALL', isDefault INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN customLogoImagePath TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN youtubeLiveUrl TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN youtubeLiveEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN youtubeLiveTitle TEXT NOT NULL DEFAULT 'Live Streaming Masjid'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showYoutubeLiveSlide INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN youtubeLiveMuted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN mainScreenBgPreset TEXT NOT NULL DEFAULT 'PRESET_EMERALD_MIHRAB'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeBgPreset TEXT NOT NULL DEFAULT 'PRESET_WHITE_PEARL_GOLD'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fridayOfficerBgPreset TEXT NOT NULL DEFAULT 'PRESET_EMERALD_MIHRAB'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN countdownBgPreset TEXT NOT NULL DEFAULT 'PRESET_COUNTDOWN_PLAQUE'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN customFinanceBgPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN customFridayOfficerBgPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN customCountdownBgPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN centerCardTransparency REAL NOT NULL DEFAULT 0.35")
            }
        }

        val MIGRATION_1_12 = object : androidx.room.migration.Migration(1, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
                MIGRATION_6_7.migrate(db)
                MIGRATION_7_8.migrate(db)
                MIGRATION_8_9.migrate(db)
                MIGRATION_9_10.migrate(db)
                MIGRATION_10_11.migrate(db)
                MIGRATION_11_12.migrate(db)
            }
        }

        val MIGRATION_1_13 = object : androidx.room.migration.Migration(1, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_12.migrate(db)
                MIGRATION_12_13.migrate(db)
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeTitleText TEXT NOT NULL DEFAULT 'LAPORAN KAS & KEUANGAN MASJID'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeTitleColor TEXT NOT NULL DEFAULT '#FFD700'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeBalanceColor TEXT NOT NULL DEFAULT '#FFE082'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeIncomeColor TEXT NOT NULL DEFAULT '#4ADE80'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeExpenseColor TEXT NOT NULL DEFAULT '#FB7185'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN financeFooterColor TEXT NOT NULL DEFAULT '#90CAF9'")

                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fridayTitleText TEXT NOT NULL DEFAULT 'JADWAL PETUGAS SHOLAT JUM''AT'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fridayTitleColor TEXT NOT NULL DEFAULT '#FFD700'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fridayOfficerNameColor TEXT NOT NULL DEFAULT '#FFFFFF'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fridayOfficerLabelColor TEXT NOT NULL DEFAULT '#38BDF8'")

                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithTitleText TEXT NOT NULL DEFAULT 'MUTIARA HADITS SHAHIH'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithTitleColor TEXT NOT NULL DEFAULT '#FFD700'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithThemeText TEXT NOT NULL DEFAULT 'Keutamaan Memakmurkan Masjid & Sholat Berjama''ah'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithThemeColor TEXT NOT NULL DEFAULT '#FDE68A'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithArabicText TEXT NOT NULL DEFAULT 'مَنْ بَنَى مَسْجِدًا لِلَّهِ بَنَى اللَّهُ لَهُ فِي الْجَنَّةِ مِثْلَهُ'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithArabicColor TEXT NOT NULL DEFAULT '#FFE082'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithTranslationText TEXT NOT NULL DEFAULT 'Barangsiapa membangun masjid karena Allah, maka Allah akan membangunkan untuknya rumah semisal di surga.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithTranslationColor TEXT NOT NULL DEFAULT '#FFFFFF'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithNarratorText TEXT NOT NULL DEFAULT 'HR. Bukhari no. 450 & Muslim no. 533'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN hadithNarratorColor TEXT NOT NULL DEFAULT '#94A3B8'")

                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTitleLeft TEXT NOT NULL DEFAULT 'MUTIARA HIKMAH MASJID'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTitleLeftColor TEXT NOT NULL DEFAULT '#FFD700'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTextLeft TEXT NOT NULL DEFAULT 'Sesungguhnya yang memakmurkan masjid-masjid Allah hanyalah orang-orang yang beriman kepada Allah dan hari kemudian, serta tetap mendirikan shalat dan menunaikan zakat.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTextLeftColor TEXT NOT NULL DEFAULT '#E2E8F0'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTitleRight TEXT NOT NULL DEFAULT 'TATA TERTIB & ADAB MASJID'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatTitleRightColor TEXT NOT NULL DEFAULT '#38BDF8'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatPoint1 TEXT NOT NULL DEFAULT 'Harap menonaktifkan atau senyapkan nada dering HP selama di masjid.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatPoint2 TEXT NOT NULL DEFAULT 'Luruskan dan rapatkan shaf sebelum sholat berjamaah dimulai.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatPoint3 TEXT NOT NULL DEFAULT 'Jagalah kebersihan, ketertiban, dan kesucian area masjid bersama.'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN maklumatPointsColor TEXT NOT NULL DEFAULT '#FFFFFF'")
            }
        }

        val MIGRATION_1_14 = object : androidx.room.migration.Migration(1, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_13.migrate(db)
                MIGRATION_13_14.migrate(db)
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN backgroundType TEXT NOT NULL DEFAULT 'PRESET'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN backgroundVideoPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fullscreenVideoPath TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fullscreenVideoTitle TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fullscreenVideoMuted INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN fullscreenVideoPlaying INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_15 = object : androidx.room.migration.Migration(1, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_14.migrate(db)
                MIGRATION_14_15.migrate(db)
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvStreamUrl TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvStreamEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvStreamTitle TEXT NOT NULL DEFAULT 'Live Kamera Masjid'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN showCctvSlide INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvStreamMuted INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN liveStreamType TEXT NOT NULL DEFAULT 'RTSP_CCTV'")
            }
        }

        val MIGRATION_1_16 = object : androidx.room.migration.Migration(1, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_15.migrate(db)
                MIGRATION_15_16.migrate(db)
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvCamerasJson TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvActiveCameraId TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAutoRotateEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAutoRotateIntervalSeconds INTEGER NOT NULL DEFAULT 30")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvDisplayLayout TEXT NOT NULL DEFAULT 'SINGLE'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAutoPlayAfterPrayer INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAfterPrayerDurationMinutes INTEGER NOT NULL DEFAULT 30")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAfterPrayerWaktu TEXT NOT NULL DEFAULT 'SUBUH,MAGHRIB,ISYA,JUMAT'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvAfterPrayerCameraId TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvScheduleEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvScheduleStartTime TEXT NOT NULL DEFAULT '18:30'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvScheduleEndTime TEXT NOT NULL DEFAULT '19:30'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvScheduleDays TEXT NOT NULL DEFAULT 'ALL'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN cctvScheduleCameraId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_17 = object : androidx.room.migration.Migration(1, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_16.migrate(db)
                MIGRATION_16_17.migrate(db)
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockFontFamily TEXT NOT NULL DEFAULT 'RADIOLAND'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockColor TEXT NOT NULL DEFAULT '#FFFFFF'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockColonColor TEXT NOT NULL DEFAULT '#F59E0B'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN clockSecondsColor TEXT NOT NULL DEFAULT '#F59E0B'")
            }
        }

        val MIGRATION_1_18 = object : androidx.room.migration.Migration(1, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_17.migrate(db)
                MIGRATION_17_18.migrate(db)
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN backgroundCctvCameraId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_19 = object : androidx.room.migration.Migration(1, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_18.migrate(db)
                MIGRATION_18_19.migrate(db)
            }
        }

        val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Mosque Config - WhatsApp Gateway (Fonnte)
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayProvider TEXT NOT NULL DEFAULT 'FONNTE'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayToken TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewaySendThursdayHour INTEGER NOT NULL DEFAULT 9")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewaySendFridayHour INTEGER NOT NULL DEFAULT 9")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayLastSentThursdayDate TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayLastSentFridayDate TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayTemplateThursday TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayTemplateFriday TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayTemplateKajian TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN waGatewayLastLogJson TEXT NOT NULL DEFAULT ''")

                // Friday Schedules - Officer Phone Numbers
                safeExecSQL(db, "ALTER TABLE friday_schedules ADD COLUMN khotibPhone TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE friday_schedules ADD COLUMN imamPhone TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE friday_schedules ADD COLUMN muadzinPhone TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE friday_schedules ADD COLUMN bilalPhone TEXT NOT NULL DEFAULT ''")

                // Mosque Activities - Speaker Phone Number
                safeExecSQL(db, "ALTER TABLE mosque_activities ADD COLUMN speakerPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_20 = object : androidx.room.migration.Migration(1, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_19.migrate(db)
                MIGRATION_19_20.migrate(db)
            }
        }

        val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN iqomahJumat INTEGER NOT NULL DEFAULT 15")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulFitriEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulFitriDate TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulFitriTime TEXT NOT NULL DEFAULT '06:30'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulFitriIqomahMinutes INTEGER NOT NULL DEFAULT 15")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulFitriSholatMinutes INTEGER NOT NULL DEFAULT 20")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulAdhaEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulAdhaDate TEXT NOT NULL DEFAULT ''")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulAdhaTime TEXT NOT NULL DEFAULT '06:30'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulAdhaIqomahMinutes INTEGER NOT NULL DEFAULT 15")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN idulAdhaSholatMinutes INTEGER NOT NULL DEFAULT 20")
            }
        }

        val MIGRATION_1_21 = object : androidx.room.migration.Migration(1, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_20.migrate(db)
                MIGRATION_20_21.migrate(db)
            }
        }

        val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExecSQL(db, """
                    CREATE TABLE IF NOT EXISTS tarawih_schedules (
                        night INTEGER PRIMARY KEY NOT NULL,
                        date TEXT NOT NULL DEFAULT '',
                        penceramah TEXT NOT NULL DEFAULT '',
                        penceramahPhone TEXT NOT NULL DEFAULT '',
                        judulKultum TEXT NOT NULL DEFAULT '',
                        imamTarawih TEXT NOT NULL DEFAULT '',
                        imamTarawihPhone TEXT NOT NULL DEFAULT '',
                        imamWitir TEXT NOT NULL DEFAULT '',
                        imamWitirPhone TEXT NOT NULL DEFAULT '',
                        bilalTarawih TEXT NOT NULL DEFAULT '',
                        bilalTarawihPhone TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihEnabled INTEGER NOT NULL DEFAULT 0")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihAutoDetectNight INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihManualNight INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihShowSlide INTEGER NOT NULL DEFAULT 1")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihKultumMinutes INTEGER NOT NULL DEFAULT 15")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihSholatMinutes INTEGER NOT NULL DEFAULT 45")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihTitleText TEXT NOT NULL DEFAULT 'JADWAL PETUGAS SHOLAT TARAWIH & KULTUM'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihTitleColor TEXT NOT NULL DEFAULT '#FFD700'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihOfficerNameColor TEXT NOT NULL DEFAULT '#FFFFFF'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihOfficerLabelColor TEXT NOT NULL DEFAULT '#38BDF8'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN tarawihBgPreset TEXT NOT NULL DEFAULT 'PRESET_EMERALD_MIHRAB'")
                safeExecSQL(db, "ALTER TABLE mosque_config ADD COLUMN customTarawihBgPath TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_22 = object : androidx.room.migration.Migration(1, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_21.migrate(db)
                MIGRATION_21_22.migrate(db)
            }
        }

        // Direct migration from version 1, 2, 3, 4, 5, 6, 7 to 9
        val MIGRATION_1_9 = object : androidx.room.migration.Migration(1, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
                MIGRATION_6_7.migrate(db)
                MIGRATION_7_8.migrate(db)
                MIGRATION_8_9.migrate(db)
            }
        }

        val MIGRATION_7_9 = object : androidx.room.migration.Migration(7, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_7_8.migrate(db)
                MIGRATION_8_9.migrate(db)
            }
        }

        val MIGRATION_1_10 = object : androidx.room.migration.Migration(1, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_9.migrate(db)
                MIGRATION_9_10.migrate(db)
            }
        }

        val MIGRATION_1_11 = object : androidx.room.migration.Migration(1, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_9.migrate(db)
                MIGRATION_9_10.migrate(db)
                MIGRATION_10_11.migrate(db)
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): MasjidDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MasjidDatabase::class.java,
                    "masjidku_tv_database"
                )
                    .addMigrations(
                        MIGRATION_1_9,
                        MIGRATION_1_10,
                        MIGRATION_1_11,
                        MIGRATION_1_12,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_3_5,
                        MIGRATION_5_6,
                        MIGRATION_4_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_7_9,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_1_13,
                        MIGRATION_13_14,
                        MIGRATION_1_14,
                        MIGRATION_14_15,
                        MIGRATION_1_15,
                        MIGRATION_15_16,
                        MIGRATION_1_16,
                        MIGRATION_16_17,
                        MIGRATION_1_17,
                        MIGRATION_17_18,
                        MIGRATION_1_18,
                        MIGRATION_18_19,
                        MIGRATION_1_19,
                        MIGRATION_19_20,
                        MIGRATION_1_20,
                        MIGRATION_20_21,
                        MIGRATION_1_21,
                        MIGRATION_21_22,
                        MIGRATION_1_22
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: MasjidDatabase) {
            // 1. Initial Mosque Config
            db.mosqueConfigDao().insertOrUpdate(
                MosqueConfigEntity(
                    id = 1,
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
                    calculationMethod = "KEMENAG",
                    offsetImsak = 0,
                    offsetSubuh = 2,
                    offsetTerbit = -2,
                    offsetDhuha = 2,
                    offsetDzuhur = 2,
                    offsetAshar = 2,
                    offsetMaghrib = 2,
                    offsetIsya = 2,
                    iqomahSubuh = 10,
                    iqomahDzuhur = 10,
                    iqomahJumat = 15,
                    iqomahAshar = 10,
                    iqomahMaghrib = 7,
                    iqomahIsya = 10,
                    idulFitriEnabled = false,
                    idulFitriDate = "",
                    idulFitriTime = "06:30",
                    idulFitriIqomahMinutes = 15,
                    idulFitriSholatMinutes = 20,
                    idulAdhaEnabled = false,
                    idulAdhaDate = "",
                    idulAdhaTime = "06:30",
                    idulAdhaIqomahMinutes = 15,
                    idulAdhaSholatMinutes = 20,
                    sholatDurationMinutes = 10,
                    hijriAdjustmentDays = 0,
                    activeTheme = "EMERALD_GOLD",
                    displayLayoutModel = "CAROUSEL_BOTTOM",
                    carouselIntervalSeconds = 15,
                    soundAlertEnabled = true,
                    showImsak = true,
                    showSubuh = true,
                    showTerbit = true,
                    showDhuha = true,
                    showDzuhur = true,
                    showAshar = true,
                    showMaghrib = true,
                    showIsya = true,
                    showFinancialReport = true,
                    showFridayOfficers = true,
                    showActivities = true,
                    showDailyMaklumat = true,
                    webAdminPassword = "123456",
                    enablePreAdhanCountdown = true,
                    preAdhanCountdownSeconds = 30,
                    imamSubuh = "Ust. H. Ahmad Dahlan, Lc.",
                    imamDzuhur = "Ust. Ridwan Kamil",
                    imamAshar = "Ust. Dr. Muhammad Iqbal",
                    imamMaghrib = "Ust. H. Abdul Somad, Lc.",
                    imamIsya = "Ust. Farhan Al-Hafizh",
                    muadzinSubuh = "",
                    muadzinDzuhur = "",
                    muadzinAshar = "",
                    muadzinMaghrib = "",
                    muadzinIsya = ""
                )
            )

            // 2. Initial Friday Schedules (Jum'at 1 s/d 5)
            val initialFridaySchedules = listOf(
                FridayOfficerEntity(
                    id = 1,
                    date = "Jum'at Ke-1",
                    hijriDate = "Jum'at Barakah",
                    khotib = "Prof. Dr. KH. Ahmad Syakir, M.A.",
                    imam = "Ustadz H. M. Firdaus Al-Hafidz",
                    muadzin = "Ustadz Bilal Ramadhan",
                    bilal = "Akhi Muhammad Syahril",
                    khutbahTopic = "Menjaga Keistiqomahan Ibadah & Keikhlasan Hati di Era Digital",
                    notes = "Jadwal Petugas Sholat Jum'at Pekan Pertama"
                ),
                FridayOfficerEntity(
                    id = 2,
                    date = "Jum'at Ke-2",
                    hijriDate = "Jum'at Barakah",
                    khotib = "Dr. H. Muchlis Muhammad, Lc., M.A.",
                    imam = "Ustadz Fahmi Idris Al-Hafidz",
                    muadzin = "Ustadz Salman Al-Farisi",
                    bilal = "Akhi Rizky Ramadhan",
                    khutbahTopic = "Membangun Generasi Rabbani Berakhlak Qur'ani",
                    notes = "Jadwal Petugas Sholat Jum'at Pekan Kedua"
                ),
                FridayOfficerEntity(
                    id = 3,
                    date = "Jum'at Ke-3",
                    hijriDate = "Jum'at Barakah",
                    khotib = "KH. Abdullah Gymnastiar (Aa Gym)",
                    imam = "Ustadz M. Syakir Al-Bantani",
                    muadzin = "Ustadz Zaidan Mubarok",
                    bilal = "Akhi Farhan Kurniawan",
                    khutbahTopic = "Manajemen Qalbu: Meraih Ketenangan Hidup Hakiki",
                    notes = "Jadwal Petugas Sholat Jum'at Pekan Ketiga"
                ),
                FridayOfficerEntity(
                    id = 4,
                    date = "Jum'at Ke-4",
                    hijriDate = "Jum'at Barakah",
                    khotib = "Ustadz Adi Hidayat, Lc., M.A.",
                    imam = "Ustadz Rahmat Hidayatullah",
                    muadzin = "Ustadz Ahmad Fauzi",
                    bilal = "Akhi Dani Ramdani",
                    khutbahTopic = "Meneladani Kepemimpinan Rasulullah ﷺ dalam Kehidupan",
                    notes = "Jadwal Petugas Sholat Jum'at Pekan Keempat"
                ),
                FridayOfficerEntity(
                    id = 5,
                    date = "Jum'at Ke-5",
                    hijriDate = "Jum'at Barakah",
                    khotib = "Habib Nabiel Al-Musawa, M.Si.",
                    imam = "Ustadz Hasan Basri Al-Hafidz",
                    muadzin = "Ustadz Wildan Hakim",
                    bilal = "Akhi Ilham Maulana",
                    khutbahTopic = "Mempererat Ukhuwah Islamiyah untuk Kemaslahatan Umat",
                    notes = "Jadwal Petugas Sholat Jum'at Pekan Kelima"
                ),
                FridayOfficerEntity(
                    id = 6,
                    date = "1 Syawal (Idul Fitri)",
                    hijriDate = "1 Syawal",
                    khotib = "KH. Ahmad Dahlan, M.A.",
                    imam = "Ustadz H. M. Firdaus Al-Hafidz",
                    muadzin = "Ustadz Bilal Ramadhan",
                    bilal = "Akhi Muhammad Syahril",
                    khutbahTopic = "Meraih Kemenangan Hakiki & Mempererat Ukhuwah Islamiyah",
                    notes = "Jadwal Petugas Sholat Idul Fitri"
                ),
                FridayOfficerEntity(
                    id = 7,
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
            for (officer in initialFridaySchedules) {
                db.fridayOfficerDao().insertOrUpdate(officer)
            }

            // 3. Initial Financial Bookkeeping
            val initialFinances = listOf(
                FinanceEntity(
                    title = "Infaq Kotak Amal Jum'at Pekan Lalu",
                    amount = 14850000,
                    type = "INCOME",
                    category = "Kotak Amal Jum'at",
                    date = "Jum'at Pekan Lalu",
                    notes = "Alhamdulillah dari kotak keliling"
                ),
                FinanceEntity(
                    title = "Infaq Transfer QRIS & Donatur Tetap",
                    amount = 6750000,
                    type = "INCOME",
                    category = "Infaq Digital & Donatur",
                    date = "Awal Pekan",
                    notes = "Rekening BRI DKM BAITUR ROHMAN"
                ),
                FinanceEntity(
                    title = "Infaq Tromol / Kotak Harian",
                    amount = 3200000,
                    type = "INCOME",
                    category = "Kotak Harian",
                    date = "Rabu",
                    notes = "Kotak serbaguna selasar"
                ),
                FinanceEntity(
                    title = "Tagihan Listrik PLN & Air Bersih PAM",
                    amount = 3450000,
                    type = "EXPENSE",
                    category = "Operasional Listrik/Air",
                    date = "Awal Bulan",
                    notes = "AC Ruang Utama dan Pompa Air"
                ),
                FinanceEntity(
                    title = "Honor Marbot & Petugas Kebersihan",
                    amount = 4000000,
                    type = "EXPENSE",
                    category = "Honor Petugas",
                    date = "Awal Bulan",
                    notes = "2 Orang Marbot Masjid"
                ),
                FinanceEntity(
                    title = "Santunan Anak Yatim & Dhuafa Lingkungan",
                    amount = 2500000,
                    type = "EXPENSE",
                    category = "Santunan Sosial",
                    date = "Kamis",
                    notes = "10 Penerima santunan"
                )
            )
            for (f in initialFinances) {
                db.financeDao().insert(f)
            }

            // 4. Initial Mosque Activities & Kajian
            val activities = listOf(
                MosqueActivityEntity(
                    title = "Kajian Subuh Berjamaah: Riyadhus Shalihin",
                    speaker = "Buya H. Yahya Zainul Ma'arif",
                    date = "Setiap Ahad Pagi",
                    time = "05.15 WIB (Bada Subuh)",
                    location = "Ruang Utama Masjid",
                    description = "Bedah Kitab Hadits Keutamaan Akhlak & Niat",
                    category = "Kajian Rutin"
                ),
                MosqueActivityEntity(
                    title = "Kajian Fiqih Ibadah & Muamalah Kontemporer",
                    speaker = "Ustadz Dr. Syafiq Riza Basalamah, M.A.",
                    date = "Sabtu Malam Ahad",
                    time = "18.30 WIB (Bada Maghrib)",
                    location = "Ruang Utama Masjid",
                    description = "Tanya Jawab Seputar Muamalah dan Syariah",
                    category = "Tabligh Akbar"
                ),
                MosqueActivityEntity(
                    title = "Tahsin & Tahfidz Al-Qur'an Santri TPQ Baiturrohman",
                    speaker = "Ustadzah Nurul Hidayah, S.Pd.I",
                    date = "Senin s/d Kamis",
                    time = "16.00 - 17.30 WIB",
                    location = "Gedung TPQ Lantai 2",
                    description = "Bimbingan Tartil Tajwid dan Hafalan Juz 30",
                    category = "TPQ Anak"
                )
            )
            for (act in activities) {
                db.mosqueActivityDao().insert(act)
            }

            // 5. Initial Running Texts
            val runningTexts = listOf(
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
            for (rt in runningTexts) {
                db.runningTextDao().insert(rt)
            }

            // 6. Initial 30-Night Tarawih Schedules
            val tarawihSchedules = generateDefaultTarawihSchedules()
            db.tarawihScheduleDao().insertAll(tarawihSchedules)
        }

        fun generateDefaultTarawihSchedules(): List<TarawihScheduleEntity> {
            val samplePenceramah = listOf(
                Pair("Ust. Dr. H. Fathurrahman, M.Ag", "Meraih Keberkahan dan Ampunan di Bulan Ramadhan"),
                Pair("Buya H. Yahya Zainul Ma'arif", "Adab dan Fiqih Berpuasa Sesuai Sunnah"),
                Pair("Ust. H. Abdul Somad, Lc., D.E.S.A", "Menghidupkan Malam Lailatul Qadar"),
                Pair("Ust. Adi Hidayat, Lc., M.A.", "Tadabbur Al-Qur'an dan Pembersihan Jiwa"),
                Pair("Ust. Dr. Syafiq Riza Basalamah", "Keutamaan Sedekah dan Kepedulian Sosial"),
                Pair("Prof. Dr. KH. Nasaruddin Umar", "Membangun Kedamaian Hati dengan Dzikir"),
                Pair("Ust. Hanan Attaki, Lc.", "Istiqomah Menjaga Semangat Ibadah"),
                Pair("K.H. Ahmad Bahauddin Nursalim", "Hakikat Syukur dan Ikhlas Beribadah")
            )
            val sampleImam = listOf(
                "Ust. H. Ahmad Dahlan, Lc.",
                "Ust. Farhan Al-Hafizh",
                "Ust. Ridwan Kamil, S.Q.",
                "Ust. H. Syarifuddin Mustofa",
                "Ust. Dr. Muhammad Iqbal",
                "Ust. M. Firdaus Al-Hafidz"
            )
            val sampleBilal = listOf(
                "Akhi Muhammad Syahril",
                "Ust. Bilal Ramadhan",
                "Akhi Rizky Kurniawan",
                "Akhi Fauzan Azhima"
            )

            return (1..30).map { night ->
                val p = samplePenceramah[(night - 1) % samplePenceramah.size]
                val imamT = sampleImam[(night - 1) % sampleImam.size]
                val imamW = sampleImam[night % sampleImam.size]
                val bilal = sampleBilal[(night - 1) % sampleBilal.size]
                TarawihScheduleEntity(
                    night = night,
                    date = "Malam ke-$night Ramadhan",
                    penceramah = p.first,
                    penceramahPhone = "",
                    judulKultum = p.second,
                    imamTarawih = imamT,
                    imamTarawihPhone = "",
                    imamWitir = imamW,
                    imamWitirPhone = "",
                    bilalTarawih = bilal,
                    bilalTarawihPhone = "",
                    notes = "Jadwal Resmi Tarawih Masjid"
                )
            }
        }
    }
}
