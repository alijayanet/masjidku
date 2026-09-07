package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BuzzerSoundPlayer
import com.example.audio.MurottalAudioPlayer
import com.example.data.local.MasjidDatabase
import com.example.data.model.*
import com.example.data.prayer.PrayerCalculator
import com.example.data.repository.MasjidRepository
import com.example.server.LocalHttpServer
import com.example.server.NetworkUtils
import com.example.server.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.TimeZone

data class MasjidTVUiState(
    val currentCalendar: Calendar = Calendar.getInstance(),
    val currentTimeString: String = "12:00",
    val currentSecondsString: String = "00",
    val isBlinkColon: Boolean = true,
    val prayerSchedule: PrayerSchedule? = null,
    val nextPrayerInfo: NextPrayerInfo? = null,
    val config: MosqueConfig = MosqueConfig(),
    val finances: List<FinanceTransaction> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val balance: Long = 0L,
    val allFridaySchedules: List<FridaySchedule> = emptyList(),
    val upcomingFridayWeekIndex: Int = 1,
    val fridaySchedule: FridaySchedule = FridaySchedule(
        id = 1,
        date = "Jum'at Ini",
        hijriDate = "Jum'at Barakah",
        khotib = "Khotib Sholat Jum'at",
        imam = "Imam Sholat Jum'at",
        muadzin = "Muadzin",
        bilal = "Bilal",
        khutbahTopic = "Tema Khutbah"
    ),
    val activities: List<MosqueActivity> = emptyList(),
    val activeRunningTexts: List<RunningTextItem> = emptyList(),
    val activeMediaSlides: List<MediaSlide> = emptyList(),
    val allMediaSlides: List<MediaSlide> = emptyList(),
    val currentSlideIndex: Int = 0,
    val displayMode: TVDisplayMode = TVDisplayMode.NORMAL,
    val adhanPrayerName: PrayerName = PrayerName.DZUHUR,
    val preAdhanRemainingSeconds: Int = 30,
    val activeImamName: String = "",
    val activeMuadzinName: String = "",
    val activeKhotibName: String = "",
    val activeBilalName: String = "",
    val isFridayPrayer: Boolean = false,
    val isHariRaya: Boolean = false,
    val hariRayaEventTitle: String = "",
    val iqomahRemainingSeconds: Int = 600,
    val sholatSilentRemainingSeconds: Int = 600,
    val dailyImamSchedules: List<DailyImamSchedule> = emptyList(),
    val isMurottalPlaying: Boolean = false,
    val murottalTrackTitle: String = "",
    val murottalQariName: String = "",
    val murottalSurahName: String = "",
    val allMurottalAudios: List<MurottalAudioItem> = emptyList(),
    val tarawihSchedules: List<TarawihSchedule> = emptyList(),
    val activeTarawihSchedule: TarawihSchedule? = null,
    val activeTarawihNight: Int = 1,
    val localIpAddress: String = "127.0.0.1",
    val serverUrl: String = "http://127.0.0.1:8080",
    val qrCodeBitmap: ImageBitmap? = null,
    val showSettingsDialog: Boolean = false,
    val settingsSelectedTab: Int = 0
)

class MasjidTVViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MasjidDatabase.getDatabase(application, viewModelScope)
    val repository = MasjidRepository(database, application)

    private val _uiState = MutableStateFlow(MasjidTVUiState())
    val uiState: StateFlow<MasjidTVUiState> = _uiState.asStateFlow()

    private var httpServer: LocalHttpServer? = null
    private var clockJob: Job? = null
    private var carouselJob: Job? = null
    private var countdownModeJob: Job? = null

    // Track triggered prayers to avoid duplicate alerts within the same minute
    private var lastTriggeredMinute = -1
    private var lastTriggeredPrayer: PrayerName? = null
    private var lastTriggeredMurottalMinute = -1
    private var lastTriggeredMurottalPrayer: PrayerName? = null

    init {
        val ip = NetworkUtils.getLocalIpAddress(application)
        val url = "http://$ip:8080"
        val qr = QrCodeUtils.generateQrBitmap(url, 220)
        _uiState.update { it.copy(localIpAddress = ip, serverUrl = url, qrCodeBitmap = qr) }

        startHttpServer()
        observeData()
        startClockAndPrayerEngine()
        startCarouselLoop()

        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
        }
    }

    private fun startHttpServer() {
        httpServer = LocalHttpServer(
            port = 8080,
            repository = repository,
            scope = viewModelScope,
            context = getApplication<Application>(),
            onTriggerAction = { action ->
                viewModelScope.launch {
                    when (action) {
                        "preadhan" -> triggerPreAdhanManual(PrayerName.DZUHUR, 30, forceFriday = false)
                        "preadhan_friday" -> triggerPreAdhanManual(PrayerName.DZUHUR, 30, forceFriday = true)
                        "adhan" -> triggerAdhanManual(PrayerName.DZUHUR, forceFriday = false)
                        "adhan_friday" -> triggerAdhanManual(PrayerName.DZUHUR, forceFriday = true)
                        "iqomah" -> triggerIqomahManual(PrayerName.DZUHUR, 10, forceFriday = false)
                        "sholat" -> triggerSholatSilentManual(10, forceFriday = false)
                        "play_murottal" -> playMurottalManual()
                        "stop_murottal" -> stopMurottalManual()
                        "idul_fitri_sim" -> triggerIdulFitriManual()
                        "idul_adha_sim" -> triggerIdulAdhaManual()
                        "tarawih_sim" -> triggerTarawihManual()
                        "stop_fullscreen_video" -> stopFullscreenVideo()
                        "stop_fullscreen_cctv" -> stopFullscreenCctv()
                        "reset" -> resetToNormalMode()
                    }
                }
            }
        )
        httpServer?.start()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.configFlow.collect { cfg ->
                val currentMode = _uiState.value.displayMode
                val updatedMode = when {
                    cfg.cctvStreamEnabled && cfg.cctvStreamUrl.isNotBlank() && currentMode == TVDisplayMode.NORMAL -> TVDisplayMode.FULLSCREEN_CCTV
                    !cfg.cctvStreamEnabled && currentMode == TVDisplayMode.FULLSCREEN_CCTV -> TVDisplayMode.NORMAL
                    cfg.fullscreenVideoPlaying && cfg.fullscreenVideoPath.isNotBlank() && currentMode == TVDisplayMode.NORMAL -> TVDisplayMode.FULLSCREEN_VIDEO
                    !cfg.fullscreenVideoPlaying && currentMode == TVDisplayMode.FULLSCREEN_VIDEO -> TVDisplayMode.NORMAL
                    else -> currentMode
                }
                _uiState.update { it.copy(config = cfg, displayMode = updatedMode) }
                updatePrayerCalculation()
            }
        }

        viewModelScope.launch {
            repository.dailyImamSchedulesFlow.collect { list ->
                _uiState.update { it.copy(dailyImamSchedules = list) }
            }
        }

        viewModelScope.launch {
            repository.financesFlow.collect { list ->
                var inc = 0L
                var exp = 0L
                for (f in list) {
                    if (f.type == TransactionType.INCOME) inc += f.amount else exp += f.amount
                }
                _uiState.update {
                    it.copy(
                        finances = list,
                        totalIncome = inc,
                        totalExpense = exp,
                        balance = inc - exp
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.allFridaySchedulesFlow.collect { list ->
                val upcoming = resolveUpcomingFridaySchedule(list, Calendar.getInstance())
                val weekIndex = getUpcomingFridayWeekIndex(Calendar.getInstance())
                _uiState.update {
                    it.copy(
                        allFridaySchedules = list,
                        upcomingFridayWeekIndex = weekIndex,
                        fridaySchedule = upcoming
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.activitiesFlow.collect { acts ->
                _uiState.update { it.copy(activities = acts) }
            }
        }

        viewModelScope.launch {
            repository.activeRunningTextsFlow.collect { texts ->
                _uiState.update { it.copy(activeRunningTexts = texts) }
            }
        }

        viewModelScope.launch {
            repository.activeMediaSlidesFlow.collect { slides ->
                _uiState.update { it.copy(activeMediaSlides = slides) }
            }
        }

        viewModelScope.launch {
            repository.allMediaSlidesFlow.collect { slides ->
                _uiState.update { it.copy(allMediaSlides = slides) }
            }
        }

        viewModelScope.launch {
            repository.allMurottalAudiosFlow.collect { audios ->
                _uiState.update { it.copy(allMurottalAudios = audios) }
            }
        }

        viewModelScope.launch {
            MurottalAudioPlayer.playbackInfo.collect { info ->
                _uiState.update {
                    it.copy(
                        isMurottalPlaying = info.isPlaying,
                        murottalTrackTitle = info.title,
                        murottalQariName = info.qari,
                        murottalSurahName = info.surah
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.tarawihSchedulesFlow.collect { list ->
                val night = resolveActiveTarawihNight(_uiState.value.config, _uiState.value.prayerSchedule?.hijriDateStr)
                val activeSchedule = list.find { it.night == night } ?: list.firstOrNull()
                _uiState.update {
                    it.copy(
                        tarawihSchedules = list,
                        activeTarawihNight = night,
                        activeTarawihSchedule = activeSchedule
                    )
                }
            }
        }
    }

    private fun startClockAndPrayerEngine() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                val cal = Calendar.getInstance(TimeZone.getDefault())
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                val sec = cal.get(Calendar.SECOND)
                val timeStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, min)
                val secStr = String.format(java.util.Locale.getDefault(), "%02d", sec)

                _uiState.update { current ->
                    current.copy(
                        currentCalendar = cal,
                        currentTimeString = timeStr,
                        currentSecondsString = secStr,
                        isBlinkColon = sec % 2 == 0
                    )
                }

                updatePrayerCalculation()

                // Check automatic Pre-Adhan Countdown & Adhan arrival
                val schedule = _uiState.value.prayerSchedule
                val config = _uiState.value.config
                val currentMinuteTotal = hour * 60 + min
                val currentSecsTotal = hour * 3600 + min * 60 + sec
                val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

                if (schedule != null) {
                    fun parseSeconds(timeStr: String): Int {
                        val parts = timeStr.split(":")
                        if (parts.size == 2) {
                            val h = parts[0].trim().toIntOrNull() ?: 0
                            val m = parts[1].trim().toIntOrNull() ?: 0
                            return h * 3600 + m * 60
                        }
                        return -1
                    }

                    val prayerCheckList = mutableListOf(
                        PrayerName.SUBUH to parseSeconds(schedule.subuh),
                        PrayerName.DZUHUR to parseSeconds(schedule.dzuhur),
                        PrayerName.ASHAR to parseSeconds(schedule.ashar),
                        PrayerName.MAGHRIB to parseSeconds(schedule.maghrib),
                        PrayerName.ISYA to parseSeconds(schedule.isya)
                    )
                    if (config.idulFitriEnabled && config.idulFitriDate.isNotBlank() && config.idulFitriDate == todayDateStr && config.idulFitriTime.isNotBlank()) {
                        prayerCheckList.add(0, PrayerName.IDUL_FITRI to parseSeconds(config.idulFitriTime))
                    }
                    if (config.idulAdhaEnabled && config.idulAdhaDate.isNotBlank() && config.idulAdhaDate == todayDateStr && config.idulAdhaTime.isNotBlank()) {
                        prayerCheckList.add(0, PrayerName.IDUL_ADHA to parseSeconds(config.idulAdhaTime))
                    }
                    val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

                    for ((pName, pSec) in prayerCheckList) {
                        if (pSec < 0) continue
                        val diffSecs = pSec - currentSecsTotal

                        // 0. Automatic Murottal / Tarhim Check (e.g. 10 minutes before prayer)
                        if (config.murottalEnabled) {
                            val isMurottalActiveForPrayer = when {
                                isFriday && pName == PrayerName.DZUHUR -> config.murottalJumat
                                pName == PrayerName.SUBUH -> config.murottalSubuh
                                pName == PrayerName.DZUHUR -> config.murottalDzuhur
                                pName == PrayerName.ASHAR -> config.murottalAshar
                                pName == PrayerName.MAGHRIB -> config.murottalMaghrib
                                pName == PrayerName.ISYA -> config.murottalIsya
                                else -> false
                            }
                            val murottalThreshold = (config.murottalDurationMinutes.coerceIn(3, 60)) * 60
                            if (isMurottalActiveForPrayer && diffSecs in 1..murottalThreshold) {
                                if (_uiState.value.displayMode == TVDisplayMode.NORMAL &&
                                    !MurottalAudioPlayer.playbackInfo.value.isPlaying &&
                                    lastTriggeredMurottalPrayer != pName
                                ) {
                                    lastTriggeredMurottalPrayer = pName
                                    lastTriggeredMurottalMinute = currentMinuteTotal
                                    playMurottalForPrayer(pName, config)
                                }
                            }
                        }

                        // 1. Pre-Adhan Countdown Check (e.g. 30 seconds before prayer)
                        val preAdhanThreshold = config.preAdhanCountdownSeconds.coerceIn(10, 600)
                        if (config.enablePreAdhanCountdown && diffSecs in 1..preAdhanThreshold) {
                            if (_uiState.value.displayMode == TVDisplayMode.NORMAL || _uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV) {
                                val officers = repository.getActivePrayerOfficers(todayDateStr, pName, isFriday, config, _uiState.value.fridaySchedule)
                                startPreAdhanCountdownFlow(pName, diffSecs, officers, config)
                            }
                            break
                        }

                        // 2. Exact Adhan Arrival Check
                        val pMinute = pSec / 60
                        if (pMinute == currentMinuteTotal && currentMinuteTotal != lastTriggeredMinute) {
                            lastTriggeredMinute = currentMinuteTotal
                            lastTriggeredPrayer = pName
                            if (_uiState.value.displayMode == TVDisplayMode.NORMAL || _uiState.value.displayMode == TVDisplayMode.PRE_ADHAN_COUNTDOWN || _uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV) {
                                val officers = repository.getActivePrayerOfficers(todayDateStr, pName, isFriday, config, _uiState.value.fridaySchedule)
                                startAutomaticAdhanFlow(pName, officers, config)
                            }
                            break
                        }
                    }
                }

                // 3. Auto-Rotate Multi-Camera CCTV (Auto-Tour)
                if (_uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV && config.cctvAutoRotateEnabled) {
                    val activeCameras = config.getCctvCameras().filter { it.isEnabled }
                    if (activeCameras.size > 1) {
                        val interval = config.cctvAutoRotateIntervalSeconds.coerceIn(5, 300)
                        if (sec % interval == 0) {
                            val currentIndex = activeCameras.indexOfFirst { it.id == config.cctvActiveCameraId }
                            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % activeCameras.size else 0
                            val nextCam = activeCameras[nextIndex]
                            repository.switchActiveCctvCamera(nextCam.id)
                        }
                    }
                }

                // 4. Manual Schedule by Time (HH:mm)
                if (config.cctvScheduleEnabled && config.cctvScheduleStartTime.isNotBlank() && config.cctvScheduleEndTime.isNotBlank()) {
                    val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> "AHAD"
                        Calendar.MONDAY -> "SENIN"
                        Calendar.TUESDAY -> "SELASA"
                        Calendar.WEDNESDAY -> "RABU"
                        Calendar.THURSDAY -> "KAMIS"
                        Calendar.FRIDAY -> "JUMAT"
                        Calendar.SATURDAY -> "SABTU"
                        else -> "ALL"
                    }
                    val daysList = config.cctvScheduleDays.uppercase().split(",").map { it.trim() }
                    val isDayMatched = daysList.contains("ALL") || daysList.contains(dayName) || (dayName == "AHAD" && daysList.contains("MINGGU"))

                    if (isDayMatched) {
                        val isInsideWindow = timeStr >= config.cctvScheduleStartTime && timeStr < config.cctvScheduleEndTime
                        if (isInsideWindow && _uiState.value.displayMode == TVDisplayMode.NORMAL) {
                            val targetCam = if (config.cctvScheduleCameraId.isNotBlank()) {
                                config.getCctvCameras().firstOrNull { it.id == config.cctvScheduleCameraId } ?: config.getActiveCctvCamera()
                            } else {
                                config.getActiveCctvCamera()
                            }
                            if (targetCam != null && targetCam.streamUrl.isNotBlank()) {
                                repository.playFullscreenCctv(targetCam.streamUrl, targetCam.name, targetCam.isMuted)
                            }
                        } else if (!isInsideWindow && _uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV && config.cctvStreamEnabled) {
                            if (timeStr >= config.cctvScheduleEndTime && sec == 0) {
                                repository.stopFullscreenCctv()
                            }
                        }
                    }
                }

                // 5. Automatic WhatsApp Gateway Reminders (Kamis & Jum'at pk 09:00 WIB)
                if (sec == 0 && config.waGatewayEnabled && config.waGatewayToken.isNotBlank()) {
                    checkAndTriggerWhatsAppReminders(cal, config, todayDateStr)
                }

                delay(1000)
            }
        }
    }

    private var isSendingWaReminder = false

    private fun checkAndTriggerWhatsAppReminders(cal: Calendar, config: MosqueConfig, todayDateStr: String) {
        if (isSendingWaReminder) return
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // 1. Thursday reminder at waGatewaySendThursdayHour (default 9:00 AM)
        if (dayOfWeek == Calendar.THURSDAY && hour >= config.waGatewaySendThursdayHour && config.waGatewayLastSentThursdayDate != todayDateStr) {
            isSendingWaReminder = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val schedule = _uiState.value.fridaySchedule
                    val prayerTimeStr = (_uiState.value.prayerSchedule?.dzuhur ?: "11:50") + " WIB"
                    val logList = mutableListOf<String>()

                    val officers = listOf(
                        Triple(schedule.khotib, schedule.khotibPhone, "Khotib Sholat Jum'at"),
                        Triple(schedule.imam, schedule.imamPhone, "Imam Sholat Jum'at"),
                        Triple(schedule.muadzin, schedule.muadzinPhone, "Muadzin Sholat Jum'at"),
                        Triple(schedule.bilal, schedule.bilalPhone, "Bilal Sholat Jum'at")
                    )

                    for ((name, phone, role) in officers) {
                        if (phone.isNotBlank()) {
                            val msg = com.example.notification.WhatsAppGatewayManager.formatFridayMessage(
                                template = config.waGatewayTemplateThursday,
                                config = config,
                                schedule = schedule,
                                officerName = name,
                                roleName = role,
                                prayerTimeStr = prayerTimeStr
                            )
                            val res = com.example.notification.WhatsAppGatewayManager.sendMessage(
                                token = config.waGatewayToken,
                                targetPhone = phone,
                                messageText = msg
                            )
                            logList.add("${role} ($name): ${if (res.success) "Sukses" else "Gagal (${res.message})"}")
                        }
                    }

                    val logSummary = "[Auto Kamis $todayDateStr] " + logList.joinToString(" | ")
                    repository.updateWaGatewayLastSent(thursdayDate = todayDateStr, logJson = logSummary)
                } catch (_: Throwable) {
                } finally {
                    isSendingWaReminder = false
                }
            }
        }

        // 2. Friday reminder at waGatewaySendFridayHour (default 9:00 AM)
        if (dayOfWeek == Calendar.FRIDAY && hour >= config.waGatewaySendFridayHour && config.waGatewayLastSentFridayDate != todayDateStr) {
            isSendingWaReminder = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val schedule = _uiState.value.fridaySchedule
                    val prayerTimeStr = (_uiState.value.prayerSchedule?.dzuhur ?: "11:50") + " WIB"
                    val logList = mutableListOf<String>()

                    val officers = listOf(
                        Triple(schedule.khotib, schedule.khotibPhone, "Khotib Sholat Jum'at"),
                        Triple(schedule.imam, schedule.imamPhone, "Imam Sholat Jum'at"),
                        Triple(schedule.muadzin, schedule.muadzinPhone, "Muadzin Sholat Jum'at"),
                        Triple(schedule.bilal, schedule.bilalPhone, "Bilal Sholat Jum'at")
                    )

                    for ((name, phone, role) in officers) {
                        if (phone.isNotBlank()) {
                            val msg = com.example.notification.WhatsAppGatewayManager.formatFridayMessage(
                                template = config.waGatewayTemplateFriday,
                                config = config,
                                schedule = schedule,
                                officerName = name,
                                roleName = role,
                                prayerTimeStr = prayerTimeStr
                            )
                            val res = com.example.notification.WhatsAppGatewayManager.sendMessage(
                                token = config.waGatewayToken,
                                targetPhone = phone,
                                messageText = msg
                            )
                            logList.add("${role} ($name): ${if (res.success) "Sukses" else "Gagal (${res.message})"}")
                        }
                    }

                    val logSummary = "[Auto Jum'at $todayDateStr] " + logList.joinToString(" | ")
                    repository.updateWaGatewayLastSent(fridayDate = todayDateStr, logJson = logSummary)
                } catch (_: Throwable) {
                } finally {
                    isSendingWaReminder = false
                }
            }
        }
    }

    private fun updatePrayerCalculation() {
        val cal = _uiState.value.currentCalendar
        val config = _uiState.value.config
        val schedule = PrayerCalculator.calculatePrayerTimes(cal, config)
        val nextInfo = PrayerCalculator.getNextPrayerInfo(cal, schedule, config)

        _uiState.update {
            it.copy(
                prayerSchedule = schedule,
                nextPrayerInfo = nextInfo
            )
        }
    }

    private fun getActiveSlideCount(): Int {
        val state = _uiState.value
        val config = state.config

        var count = 0
        if (config.showFinancialReport) count++
        if (config.showFridayOfficers) count++
        if (config.showActivities) count++
        if (config.showDailyMaklumat) count++
        if (config.showQrisCard) count++
        if (config.showDailyHadith) count++
        if (config.showYoutubeLiveSlide && config.youtubeLiveUrl.isNotBlank()) count++
        count += state.activeMediaSlides.size

        return count
    }

    private fun startCarouselLoop() {
        carouselJob?.cancel()
        carouselJob = viewModelScope.launch {
            while (isActive) {
                val interval = (_uiState.value.config.carouselIntervalSeconds.coerceAtLeast(8)) * 1000L
                delay(interval)
                if (_uiState.value.displayMode == TVDisplayMode.NORMAL && !_uiState.value.showSettingsDialog) {
                    val totalSlides = getActiveSlideCount()
                    if (totalSlides > 0) {
                        _uiState.update {
                            it.copy(currentSlideIndex = (it.currentSlideIndex + 1) % totalSlides)
                        }
                    }
                }
            }
        }
    }

    fun setCarouselSlide(index: Int) {
        val totalSlides = getActiveSlideCount()
        _uiState.update { it.copy(currentSlideIndex = index.coerceIn(0, totalSlides - 1)) }
    }

    private fun startPreAdhanCountdownFlow(
        prayer: PrayerName,
        initialSeconds: Int,
        officers: ActivePrayerOfficers,
        config: MosqueConfig
    ) {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stopWithFadeOut(1200L)
        countdownModeJob = viewModelScope.launch {
            var secs = initialSeconds
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.PRE_ADHAN_COUNTDOWN,
                    adhanPrayerName = prayer,
                    preAdhanRemainingSeconds = secs,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday,
                    isHariRaya = officers.isHariRaya,
                    hariRayaEventTitle = officers.eventTitle
                )
            }

            while (secs > 0) {
                delay(1000)
                secs--
                _uiState.update { it.copy(preAdhanRemainingSeconds = secs) }
                if (secs in 1..5 && config.soundAlertEnabled) {
                    BuzzerSoundPlayer.playCountdownBeep(false)
                }
            }

            // Move to Adhan
            startAutomaticAdhanFlow(prayer, officers, config)
        }
    }

    private fun startAutomaticAdhanFlow(
        prayer: PrayerName,
        officers: ActivePrayerOfficers,
        config: MosqueConfig
    ) {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stop()
        countdownModeJob = viewModelScope.launch {
            // 1. Play Adhan Arrival Chime & Show Adhan Screen for 30 seconds
            if (config.soundAlertEnabled) {
                BuzzerSoundPlayer.playAdhanArrivalChime(viewModelScope)
            }
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.ADHAN,
                    adhanPrayerName = prayer,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday,
                    isHariRaya = officers.isHariRaya,
                    hariRayaEventTitle = officers.eventTitle
                )
            }
            delay(30_000) // 30 seconds adhan splash

            // 2. Iqomah Countdown
            val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val iqomahMinutes = when (prayer) {
                PrayerName.SUBUH -> config.iqomahSubuh
                PrayerName.DZUHUR -> if (isFriday && config.showFridayOfficers) config.iqomahJumat else config.iqomahDzuhur
                PrayerName.ASHAR -> config.iqomahAshar
                PrayerName.MAGHRIB -> config.iqomahMaghrib
                PrayerName.ISYA -> config.iqomahIsya
                PrayerName.IDUL_FITRI -> config.idulFitriIqomahMinutes
                PrayerName.IDUL_ADHA -> config.idulAdhaIqomahMinutes
                else -> 10
            }
            var iqomahSeconds = iqomahMinutes * 60
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.IQOMAH_COUNTDOWN,
                    adhanPrayerName = prayer,
                    iqomahRemainingSeconds = iqomahSeconds,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday,
                    isHariRaya = officers.isHariRaya,
                    hariRayaEventTitle = officers.eventTitle
                )
            }

            while (iqomahSeconds > 0) {
                delay(1000)
                iqomahSeconds--
                _uiState.update { it.copy(iqomahRemainingSeconds = iqomahSeconds) }
                if (iqomahSeconds in 1..5 && config.soundAlertEnabled) {
                    BuzzerSoundPlayer.playCountdownBeep(false)
                }
            }

            // Final Iqomah Beep
            if (config.soundAlertEnabled) {
                BuzzerSoundPlayer.playCountdownBeep(true)
            }

            // 3. Sholat Silent / Dimmed Mode
            val sholatMinutes = when (prayer) {
                PrayerName.IDUL_FITRI -> config.idulFitriSholatMinutes
                PrayerName.IDUL_ADHA -> config.idulAdhaSholatMinutes
                else -> config.sholatDurationMinutes
            }
            var sholatSeconds = (sholatMinutes.coerceAtLeast(3)) * 60
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.SHOLAT_SILENT,
                    adhanPrayerName = prayer,
                    sholatSilentRemainingSeconds = sholatSeconds,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday,
                    isHariRaya = officers.isHariRaya,
                    hariRayaEventTitle = officers.eventTitle
                )
            }

            while (sholatSeconds > 0) {
                delay(1000)
                sholatSeconds--
                _uiState.update { it.copy(sholatSilentRemainingSeconds = sholatSeconds) }
            }

            // 4. Return to normal or Auto-Play CCTV after prayer
            if (config.cctvAutoPlayAfterPrayer) {
                val prayerStr = prayer.name.uppercase()
                val isFridayDzhur = officers.isFriday && prayer == PrayerName.DZUHUR
                val waktuList = config.cctvAfterPrayerWaktu.uppercase().split(",").map { it.trim() }
                val isWaktuMatched = waktuList.contains(prayerStr) || (isFridayDzhur && (waktuList.contains("JUMAT") || waktuList.contains("JUM'AT")))
                if (isWaktuMatched) {
                    val cameras = config.getCctvCameras()
                    val targetCam = if (config.cctvAfterPrayerCameraId.isNotBlank()) {
                        cameras.firstOrNull { it.id == config.cctvAfterPrayerCameraId } ?: config.getActiveCctvCamera()
                    } else {
                        config.getActiveCctvCamera()
                    }
                    if (targetCam != null && targetCam.streamUrl.isNotBlank()) {
                        startCctvAutoPlayAfterPrayer(targetCam, config.cctvAfterPrayerDurationMinutes)
                        return@launch
                    }
                }
            }

            resetToNormalMode()
        }
    }

    fun triggerIdulFitriManual() {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stop()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val config = _uiState.value.config
            val officers = repository.getActivePrayerOfficers(todayDateStr, PrayerName.IDUL_FITRI, false, config, _uiState.value.fridaySchedule)
            startPreAdhanCountdownFlow(PrayerName.IDUL_FITRI, 15, officers, config)
        }
    }

    fun triggerIdulAdhaManual() {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stop()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val config = _uiState.value.config
            val officers = repository.getActivePrayerOfficers(todayDateStr, PrayerName.IDUL_ADHA, false, config, _uiState.value.fridaySchedule)
            startPreAdhanCountdownFlow(PrayerName.IDUL_ADHA, 15, officers, config)
        }
    }

    fun triggerTarawihManual() {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stop()
        countdownModeJob = viewModelScope.launch {
            val night = _uiState.value.activeTarawihNight
            val schedule = _uiState.value.activeTarawihSchedule 
                ?: repository.getTarawihScheduleForNight(night)
                ?: repository.getTarawihSchedules().firstOrNull()
            val kultumTitle = if (schedule?.judulKultum?.isNotBlank() == true) " • ${schedule.judulKultum}" else ""
            val officers = ActivePrayerOfficers(
                imam = schedule?.imamTarawih?.takeIf { it.isNotBlank() } ?: "Ust. H. Ahmad Dahlan, Lc.",
                muadzin = schedule?.bilalTarawih?.takeIf { it.isNotBlank() } ?: "Akhi Muhammad Syahril",
                khotib = (schedule?.penceramah?.takeIf { it.isNotBlank() } ?: "Ust. Dr. H. Fathurrahman, M.Ag") + kultumTitle,
                bilal = schedule?.imamWitir?.takeIf { it.isNotBlank() } ?: "Ust. Farhan Al-Hafizh",
                isFriday = false,
                isHariRaya = true,
                eventTitle = "SHOLAT TARAWIH & KULTUM (MALAM KE-$night RAMADHAN)"
            )
            startPreAdhanCountdownFlow(PrayerName.ISYA, 15, officers, _uiState.value.config)
        }
    }



    fun triggerPreAdhanManual(prayer: PrayerName = PrayerName.DZUHUR, durationSeconds: Int = 30, forceFriday: Boolean = false) {
        countdownModeJob?.cancel()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val isFriday = forceFriday || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val officers = repository.getActivePrayerOfficers(todayDateStr, prayer, isFriday, _uiState.value.config, _uiState.value.fridaySchedule)
            startPreAdhanCountdownFlow(prayer, durationSeconds, officers, _uiState.value.config)
        }
    }

    fun triggerAdhanManual(prayer: PrayerName = PrayerName.DZUHUR, forceFriday: Boolean = false) {
        countdownModeJob?.cancel()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val isFriday = forceFriday || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val officers = repository.getActivePrayerOfficers(todayDateStr, prayer, isFriday, _uiState.value.config, _uiState.value.fridaySchedule)
            if (_uiState.value.config.soundAlertEnabled) {
                BuzzerSoundPlayer.playAdhanArrivalChime(viewModelScope)
            }
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.ADHAN,
                    adhanPrayerName = prayer,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday
                )
            }
        }
    }

    fun triggerIqomahManual(prayer: PrayerName = PrayerName.DZUHUR, durationMinutes: Int = 1, forceFriday: Boolean = false) {
        countdownModeJob?.cancel()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val isFriday = forceFriday || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val officers = repository.getActivePrayerOfficers(todayDateStr, prayer, isFriday, _uiState.value.config, _uiState.value.fridaySchedule)
            var secs = durationMinutes * 60
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.IQOMAH_COUNTDOWN,
                    adhanPrayerName = prayer,
                    iqomahRemainingSeconds = secs,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday
                )
            }
            while (secs > 0) {
                delay(1000)
                secs--
                _uiState.update { it.copy(iqomahRemainingSeconds = secs) }
                if (secs in 1..5) {
                    BuzzerSoundPlayer.playCountdownBeep(false)
                }
            }
            BuzzerSoundPlayer.playCountdownBeep(true)
            triggerSholatSilentManual(1, forceFriday)
        }
    }

    fun triggerSholatSilentManual(durationMinutes: Int = 1, forceFriday: Boolean = false) {
        countdownModeJob?.cancel()
        countdownModeJob = viewModelScope.launch {
            val cal = _uiState.value.currentCalendar
            val isFriday = forceFriday || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
            val officers = repository.getActivePrayerOfficers(todayDateStr, _uiState.value.adhanPrayerName, isFriday, _uiState.value.config, _uiState.value.fridaySchedule)
            var secs = durationMinutes * 60
            _uiState.update {
                it.copy(
                    displayMode = TVDisplayMode.SHOLAT_SILENT,
                    sholatSilentRemainingSeconds = secs,
                    activeImamName = officers.imam,
                    activeMuadzinName = officers.muadzin,
                    activeKhotibName = officers.khotib,
                    activeBilalName = officers.bilal,
                    isFridayPrayer = officers.isFriday
                )
            }
            while (secs > 0) {
                delay(1000)
                secs--
                _uiState.update { it.copy(sholatSilentRemainingSeconds = secs) }
            }
            resetToNormalMode()
        }
    }

    fun addDailyImamSchedule(date: String, prayerName: PrayerName, imamName: String, muadzinName: String = "", notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addDailyImamSchedule(date, prayerName, imamName, muadzinName, notes)
        }
    }

    fun deleteDailyImamSchedule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDailyImamSchedule(id)
        }
    }

    fun saveTarawihSchedule(schedule: TarawihSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTarawihSchedule(schedule)
        }
    }

    fun saveAllTarawihSchedules(schedules: List<TarawihSchedule>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAllTarawihSchedules(schedules)
        }
    }

    fun populateDefaultTarawihSchedules() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.populateDefaultTarawihSchedules()
        }
    }

    fun resetToNormalMode() {
        countdownModeJob?.cancel()
        MurottalAudioPlayer.stop()
        _uiState.update { it.copy(displayMode = TVDisplayMode.NORMAL) }
    }

    fun playMurottalForPrayer(prayer: PrayerName, config: MosqueConfig) {
        viewModelScope.launch {
            if (config.murottalSourceType == "MANUAL_UPLOAD") {
                val prayerAudios = repository.getMurottalAudiosForPrayer(prayer.name)
                val targetAudio = prayerAudios.find { it.isDefault } ?: prayerAudios.firstOrNull()
                if (targetAudio != null && File(targetAudio.filePath).exists()) {
                    MurottalAudioPlayer.playFile(
                        context = getApplication(),
                        filePath = targetAudio.filePath,
                        title = targetAudio.title,
                        qari = targetAudio.qari,
                        surah = targetAudio.surah,
                        volumePercent = config.murottalVolume
                    )
                    return@launch
                } else if (config.murottalCustomAudioPath.isNotBlank() && File(config.murottalCustomAudioPath).exists()) {
                    MurottalAudioPlayer.playFile(
                        context = getApplication(),
                        filePath = config.murottalCustomAudioPath,
                        title = "Murottal Kustom",
                        qari = "Qari Pilihan",
                        surah = "Tartil Al-Qur'an",
                        volumePercent = config.murottalVolume
                    )
                    return@launch
                }
            }

            // Default to Preset
            val presetId = if (prayer == PrayerName.SUBUH && config.murottalSelectedPresetId == "tarhim_subuh") {
                "tarhim_subuh"
            } else {
                config.murottalSelectedPresetId.ifBlank { "mishary_ar_rahman" }
            }
            MurottalAudioPlayer.playPresetById(presetId, config.murottalVolume)
        }
    }

    fun playMurottalManual(presetId: String? = null, filePath: String? = null, qari: String? = null, surah: String? = null) {
        val config = _uiState.value.config
        if (!filePath.isNullOrBlank()) {
            MurottalAudioPlayer.playFile(
                context = getApplication(),
                filePath = filePath,
                title = surah ?: "Murottal Al-Qur'an",
                qari = qari ?: "Qari Pilihan",
                surah = surah ?: "Tartil Al-Qur'an",
                volumePercent = config.murottalVolume
            )
        } else {
            val pId = presetId ?: config.murottalSelectedPresetId
            MurottalAudioPlayer.playPresetById(pId, config.murottalVolume)
        }
    }

    fun stopMurottalManual() {
        MurottalAudioPlayer.stop()
    }

    fun setMurottalPreset(presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setMurottalPreset(presetId)
        }
    }

    fun updateYoutubeLiveConfig(
        url: String,
        enabled: Boolean,
        title: String = "Live Streaming Masjid",
        showSlide: Boolean = false,
        isMuted: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateYoutubeLiveConfig(url, enabled, title, showSlide, isMuted)
        }
    }

    fun playFullscreenVideo(filePath: String, title: String = "", isMuted: Boolean = false) {
        viewModelScope.launch {
            repository.playFullscreenVideo(filePath, title, isMuted)
            _uiState.update { it.copy(displayMode = TVDisplayMode.FULLSCREEN_VIDEO) }
        }
    }

    fun stopFullscreenVideo() {
        viewModelScope.launch {
            repository.stopFullscreenVideo()
            _uiState.update { it.copy(displayMode = TVDisplayMode.NORMAL) }
        }
    }

    fun setBackgroundVideo(filePath: String, isEnabled: Boolean = true) {
        viewModelScope.launch {
            repository.setBackgroundVideo(filePath, isEnabled)
        }
    }


    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setSettingsTab(tabIndex: Int) {
        _uiState.update { it.copy(settingsSelectedTab = tabIndex) }
    }

    fun saveConfig(newConfig: MosqueConfig) {
        viewModelScope.launch {
            repository.saveConfig(newConfig)
        }
    }

    fun addFinance(title: String, amount: Long, type: TransactionType, category: String, date: String, notes: String) {
        viewModelScope.launch {
            repository.addFinance(title, amount, type, category, date, notes)
        }
    }

    fun updateFinance(id: Long, title: String, amount: Long, type: TransactionType, category: String, date: String, notes: String) {
        viewModelScope.launch {
            repository.updateFinance(id, title, amount, type, category, date, notes)
        }
    }

    fun deleteFinance(id: Long) {
        viewModelScope.launch {
            repository.deleteFinance(id)
        }
    }

    fun saveFridaySchedule(schedule: FridaySchedule) {
        viewModelScope.launch {
            repository.saveFridaySchedule(schedule)
        }
    }

    fun addActivity(title: String, speaker: String, date: String, time: String, location: String, description: String, category: String, speakerPhone: String = "") {
        viewModelScope.launch {
            repository.addActivity(title, speaker, speakerPhone, date, time, location, description, category)
        }
    }

    fun updateActivity(id: Long, title: String, speaker: String, date: String, time: String, location: String, description: String, category: String, speakerPhone: String = "") {
        viewModelScope.launch {
            repository.updateActivity(id, title, speaker, speakerPhone, date, time, location, description, category)
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            repository.deleteActivity(id)
        }
    }

    fun addRunningText(text: String) {
        viewModelScope.launch {
            repository.addRunningText(text)
        }
    }

    fun deleteRunningText(id: Long) {
        viewModelScope.launch {
            repository.deleteRunningText(id)
        }
    }

    fun updateRunningText(id: Long, text: String, isActive: Boolean = true, order: Int = 0) {
        viewModelScope.launch {
            repository.updateRunningText(id, text, isActive, order)
        }
    }

    fun addMediaSlide(filePath: String, title: String, durationSeconds: Int = 15, isActive: Boolean = true, order: Int = 0) {
        viewModelScope.launch {
            repository.addMediaSlide(filePath, title, durationSeconds, isActive, order)
        }
    }

    fun updateMediaSlide(id: Long, title: String, durationSeconds: Int, isActive: Boolean, order: Int) {
        viewModelScope.launch {
            repository.updateMediaSlide(id, title, durationSeconds, isActive, order)
        }
    }

    fun deleteMediaSlide(id: Long) {
        viewModelScope.launch {
            repository.deleteMediaSlide(id)
        }
    }

    fun playChimeTest() {
        BuzzerSoundPlayer.playAdhanArrivalChime(viewModelScope)
    }

    companion object {
        fun resolveActiveTarawihNight(config: MosqueConfig, hijriDateStr: String?): Int {
            if (!config.tarawihAutoDetectNight) {
                return config.tarawihManualNight.coerceIn(1, 30)
            }
            if (hijriDateStr != null && (hijriDateStr.contains("Ramadhan", ignoreCase = true) || hijriDateStr.contains("Ramadan", ignoreCase = true))) {
                val parts = hijriDateStr.trim().split(Regex("\\s+"))
                val dayNum = parts.firstOrNull()?.toIntOrNull()
                if (dayNum != null) {
                    return dayNum.coerceIn(1, 30)
                }
            }
            return config.tarawihManualNight.coerceIn(1, 30)
        }

        fun getUpcomingFridayWeekIndex(cal: Calendar): Int {
            val targetCal = cal.clone() as Calendar
            val currentDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK)
            val currentHour = targetCal.get(Calendar.HOUR_OF_DAY)

            var daysUntilFriday = (Calendar.FRIDAY - currentDayOfWeek + 7) % 7
            if (daysUntilFriday == 0 && currentHour >= 14) {
                daysUntilFriday = 7
            }
            targetCal.add(Calendar.DAY_OF_MONTH, daysUntilFriday)

            val dayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
            return ((dayOfMonth - 1) / 7 + 1).coerceIn(1, 5)
        }

        fun resolveUpcomingFridaySchedule(allSchedules: List<FridaySchedule>, cal: Calendar): FridaySchedule {
            val targetCal = cal.clone() as Calendar
            val currentDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK)
            val currentHour = targetCal.get(Calendar.HOUR_OF_DAY)

            var daysUntilFriday = (Calendar.FRIDAY - currentDayOfWeek + 7) % 7
            if (daysUntilFriday == 0 && currentHour >= 14) {
                daysUntilFriday = 7
            }
            targetCal.add(Calendar.DAY_OF_MONTH, daysUntilFriday)

            val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
            val autoDate = "Jum'at, ${targetCal.get(Calendar.DAY_OF_MONTH)} ${monthNames[targetCal.get(Calendar.MONTH)]} ${targetCal.get(Calendar.YEAR)}"

            if (allSchedules.isEmpty()) {
                return FridaySchedule(
                    id = 1,
                    date = autoDate,
                    hijriDate = "",
                    khotib = "Ust. Dr. H. Ahmad Dahlan",
                    imam = "Ust. Farhan Al-Hafizh",
                    muadzin = "Ust. Bilal Ramadhan",
                    bilal = "Akhi Muhammad Syahril",
                    khutbahTopic = "Menjaga Keistiqomahan Ibadah & Keikhlasan Hati"
                )
            }

            val dayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
            val fridayWeekNum = ((dayOfMonth - 1) / 7 + 1).coerceIn(1, 5)

            val base = allSchedules.find { it.id == fridayWeekNum.toLong() }
                ?: allSchedules.firstOrNull()
                ?: FridaySchedule(
                    id = 1,
                    date = autoDate,
                    hijriDate = "",
                    khotib = "Ust. Dr. H. Ahmad Dahlan",
                    imam = "Ust. Farhan Al-Hafizh",
                    muadzin = "Ust. Bilal Ramadhan",
                    bilal = "Akhi Muhammad Syahril",
                    khutbahTopic = "Menjaga Keistiqomahan Ibadah & Keikhlasan Hati"
                )

            val finalDate = if (base.date.isNotBlank() && !base.date.startsWith("Jum'at Ke-", ignoreCase = true) && !base.date.equals("Jum'at", ignoreCase = true)) {
                base.date
            } else {
                autoDate
            }

            return base.copy(
                date = finalDate,
                hijriDate = if (base.hijriDate.startsWith("Jum'at Ke-", ignoreCase = true)) "" else base.hijriDate
            )
        }
    }

    private var cctvAutoPlayJob: kotlinx.coroutines.Job? = null

    private fun startCctvAutoPlayAfterPrayer(camera: com.example.data.model.CctvCameraItem, durationMinutes: Int) {
        cctvAutoPlayJob?.cancel()
        _uiState.update {
            it.copy(
                displayMode = TVDisplayMode.FULLSCREEN_CCTV
            )
        }
        cctvAutoPlayJob = viewModelScope.launch {
            val totalSeconds = (durationMinutes.coerceIn(1, 120)) * 60
            var remaining = totalSeconds
            while (remaining > 0 && _uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV) {
                delay(1000)
                remaining--
            }
            if (_uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV) {
                resetToNormalMode()
            }
        }
    }

    fun stopFullscreenCctv() {
        cctvAutoPlayJob?.cancel()
        viewModelScope.launch {
            repository.stopFullscreenCctv()
            if (_uiState.value.displayMode == TVDisplayMode.FULLSCREEN_CCTV) {
                _uiState.update { it.copy(displayMode = TVDisplayMode.NORMAL) }
            }
        }
    }

    fun playFullscreenCctv(url: String = "", title: String = "", isMuted: Boolean = true) {
        viewModelScope.launch {
            repository.playFullscreenCctv(url, title, isMuted)
            _uiState.update { it.copy(displayMode = TVDisplayMode.FULLSCREEN_CCTV) }
        }
    }

    fun switchActiveCctvCamera(cameraId: String) {
        viewModelScope.launch {
            repository.switchActiveCctvCamera(cameraId)
        }
    }

    fun saveCctvCameras(
        cameras: List<com.example.data.model.CctvCameraItem>,
        activeCameraId: String? = null,
        autoRotate: Boolean? = null,
        autoRotateInterval: Int? = null,
        displayLayout: String? = null
    ) {
        viewModelScope.launch {
            repository.saveCctvCameras(cameras, activeCameraId, autoRotate, autoRotateInterval, displayLayout)
        }
    }

    fun saveCctvScheduleConfig(
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
        viewModelScope.launch {
            repository.saveCctvScheduleConfig(
                autoPlayAfterPrayer,
                afterPrayerDuration,
                afterPrayerWaktu,
                afterPrayerCameraId,
                scheduleEnabled,
                scheduleStartTime,
                scheduleEndTime,
                scheduleDays,
                scheduleCameraId
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpServer?.stop()
        clockJob?.cancel()
        carouselJob?.cancel()
        countdownModeJob?.cancel()
        cctvAutoPlayJob?.cancel()
    }
}
