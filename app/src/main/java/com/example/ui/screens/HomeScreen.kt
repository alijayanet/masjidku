package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PrayerName
import com.example.data.model.TVDisplayMode
import com.example.ui.components.*
import com.example.ui.theme.IslamicGoldBright
import com.example.viewmodel.MasjidTVViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: MasjidTVViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main TV Screen Display
            IslamicBackground(
                activeTheme = uiState.config.activeTheme,
                bgPreset = uiState.config.mainScreenBgPreset,
                backgroundType = uiState.config.backgroundType,
                backgroundVideoPath = uiState.config.backgroundVideoPath,
                customBackgroundImagePath = uiState.config.customBackgroundImagePath,
                customBackgroundDim = uiState.config.customBackgroundDim,
                backgroundCctvUrl = uiState.config.getBackgroundCctvUrl()
            ) {
                if (uiState.config.displayLayoutModel == "MIHRAB_GRAND_ROYAL") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LayoutMihrabGrandRoyal(
                                config = uiState.config,
                                schedule = uiState.prayerSchedule,
                                nextPrayerInfo = uiState.nextPrayerInfo,
                                currentTime = uiState.currentTimeString,
                                currentSeconds = uiState.currentSecondsString,
                                isBlinkColon = uiState.isBlinkColon,
                                gregorianDate = uiState.prayerSchedule?.dateStr ?: "Memuat...",
                                hijriDate = uiState.prayerSchedule?.hijriDateStr ?: "Memuat...",
                                finances = uiState.finances,
                                totalIncome = uiState.totalIncome,
                                totalExpense = uiState.totalExpense,
                                balance = uiState.balance,
                                fridaySchedule = uiState.fridaySchedule,
                                slideIndex = uiState.currentSlideIndex,
                                localIp = uiState.localIpAddress,
                                onOpenSettings = { viewModel.toggleSettingsDialog(true) }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RunningTextTicker(
                            runningTexts = uiState.activeRunningTexts,
                            fontSize = uiState.config.runningTextFontSize
                        )
                    }
                } else if (uiState.config.displayLayoutModel == "CORDOBA_ANDALUSIA") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LayoutCordobaAndalusia(
                                config = uiState.config,
                                schedule = uiState.prayerSchedule,
                                nextPrayerInfo = uiState.nextPrayerInfo,
                                currentTime = uiState.currentTimeString,
                                currentSeconds = uiState.currentSecondsString,
                                isBlinkColon = uiState.isBlinkColon,
                                gregorianDate = uiState.prayerSchedule?.dateStr ?: "Memuat...",
                                hijriDate = uiState.prayerSchedule?.hijriDateStr ?: "Memuat...",
                                finances = uiState.finances,
                                totalIncome = uiState.totalIncome,
                                totalExpense = uiState.totalExpense,
                                balance = uiState.balance,
                                fridaySchedule = uiState.fridaySchedule,
                                slideIndex = uiState.currentSlideIndex,
                                localIp = uiState.localIpAddress,
                                onOpenSettings = { viewModel.toggleSettingsDialog(true) }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RunningTextTicker(
                            runningTexts = uiState.activeRunningTexts,
                            fontSize = uiState.config.runningTextFontSize
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Top Area: Separated Header Bar & Standalone Digital Clock Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeaderBar(
                                config = uiState.config,
                                gregorianDate = uiState.prayerSchedule?.dateStr ?: "Memuat...",
                                hijriDate = uiState.prayerSchedule?.hijriDateStr ?: "Memuat...",
                                localIp = uiState.localIpAddress,
                                modifier = Modifier.weight(1f)
                            )

                            DigitalClockCard(
                                currentTime = uiState.currentTimeString,
                                currentSeconds = uiState.currentSecondsString,
                                isBlinkColon = uiState.isBlinkColon,
                                onOpenSettings = { viewModel.toggleSettingsDialog(true) },
                                config = uiState.config
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Center Area Based on Display Layout Model
                        when (uiState.config.displayLayoutModel) {
                        "SPLIT_DASHBOARD" -> {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Side: Vertical Prayer Times Column & Countdown
                                Box(
                                    modifier = Modifier
                                        .width(215.dp)
                                        .fillMaxHeight()
                                ) {
                                    PrayerTimesVerticalColumn(
                                        schedule = uiState.prayerSchedule,
                                        nextPrayerInfo = uiState.nextPrayerInfo,
                                        config = uiState.config
                                    )
                                }

                                // Right Side: Carousel Slides (strictly respecting toggles)
                                Box(modifier = Modifier.weight(1f)) {
                                    CarouselContainer(
                                        slideIndex = uiState.currentSlideIndex,
                                        onSelectSlide = { viewModel.setCarouselSlide(it) },
                                        nextPrayerInfo = uiState.nextPrayerInfo,
                                        config = uiState.config,
                                        finances = uiState.finances,
                                        totalIncome = uiState.totalIncome,
                                        totalExpense = uiState.totalExpense,
                                        balance = uiState.balance,
                                        fridaySchedule = uiState.fridaySchedule,
                                        activities = uiState.activities,
                                        mediaSlides = uiState.activeMediaSlides,
                                        tarawihSchedule = uiState.activeTarawihSchedule,
                                        activeTarawihNight = uiState.activeTarawihNight
                                    )
                                }
                            }
                        }

                        "DUAL_INFO_COMPACT" -> {
                            Box(modifier = Modifier.weight(1f)) {
                                CarouselContainer(
                                    slideIndex = uiState.currentSlideIndex,
                                    onSelectSlide = { viewModel.setCarouselSlide(it) },
                                    nextPrayerInfo = uiState.nextPrayerInfo,
                                    config = uiState.config,
                                    finances = uiState.finances,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    fridaySchedule = uiState.fridaySchedule,
                                    activities = uiState.activities,
                                    mediaSlides = uiState.activeMediaSlides,
                                    tarawihSchedule = uiState.activeTarawihSchedule,
                                    activeTarawihNight = uiState.activeTarawihNight
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            PrayerTimesBar(
                                schedule = uiState.prayerSchedule,
                                nextPrayerInfo = uiState.nextPrayerInfo,
                                config = uiState.config
                            )
                        }

                        "SIDEBAR_ANALOG_NEO" -> {
                            Box(modifier = Modifier.weight(1f)) {
                                LayoutSidebarAnalogNeo(
                                    config = uiState.config,
                                    schedule = uiState.prayerSchedule,
                                    nextPrayerInfo = uiState.nextPrayerInfo,
                                    calendar = uiState.currentCalendar,
                                    currentTime = uiState.currentTimeString,
                                    currentSeconds = uiState.currentSecondsString,
                                    finances = uiState.finances,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    fridaySchedule = uiState.fridaySchedule,
                                    activities = uiState.activities,
                                    mediaSlides = uiState.activeMediaSlides,
                                    slideIndex = uiState.currentSlideIndex,
                                    onSelectSlide = { viewModel.setCarouselSlide(it) }
                                )
                            }
                        }

                        "RIGHT_WAVE_ANALOG" -> {
                            Box(modifier = Modifier.weight(1f)) {
                                LayoutRightWaveAnalog(
                                    config = uiState.config,
                                    schedule = uiState.prayerSchedule,
                                    nextPrayerInfo = uiState.nextPrayerInfo,
                                    calendar = uiState.currentCalendar,
                                    currentTime = uiState.currentTimeString,
                                    currentSeconds = uiState.currentSecondsString,
                                    finances = uiState.finances,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    fridaySchedule = uiState.fridaySchedule,
                                    activities = uiState.activities,
                                    mediaSlides = uiState.activeMediaSlides,
                                    slideIndex = uiState.currentSlideIndex,
                                    onSelectSlide = { viewModel.setCarouselSlide(it) }
                                )
                            }
                        }

                        "WAKTIHA_KLATEN", "WAKTIHA_CLASSIC" -> {
                            Box(modifier = Modifier.weight(1f)) {
                                LayoutWaktihaClassic(
                                    config = uiState.config,
                                    schedule = uiState.prayerSchedule,
                                    nextPrayerInfo = uiState.nextPrayerInfo,
                                    currentTime = uiState.currentTimeString,
                                    currentSeconds = uiState.currentSecondsString,
                                    fridaySchedule = uiState.fridaySchedule,
                                    activities = uiState.activities,
                                    finances = uiState.finances,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    mediaSlides = uiState.activeMediaSlides,
                                    slideIndex = uiState.currentSlideIndex,
                                    onSelectSlide = { viewModel.setCarouselSlide(it) }
                                )
                            }
                        }

                        else -> {
                            // Default CAROUSEL_BOTTOM
                            Box(modifier = Modifier.weight(1f)) {
                                CarouselContainer(
                                    slideIndex = uiState.currentSlideIndex,
                                    onSelectSlide = { viewModel.setCarouselSlide(it) },
                                    nextPrayerInfo = uiState.nextPrayerInfo,
                                    config = uiState.config,
                                    finances = uiState.finances,
                                    totalIncome = uiState.totalIncome,
                                    totalExpense = uiState.totalExpense,
                                    balance = uiState.balance,
                                    fridaySchedule = uiState.fridaySchedule,
                                    activities = uiState.activities,
                                    mediaSlides = uiState.activeMediaSlides,
                                    tarawihSchedule = uiState.activeTarawihSchedule,
                                    activeTarawihNight = uiState.activeTarawihNight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            PrayerTimesBar(
                                schedule = uiState.prayerSchedule,
                                nextPrayerInfo = uiState.nextPrayerInfo,
                                config = uiState.config
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Bottom Running Text / Marquee Ticker
                    RunningTextTicker(
                        runningTexts = uiState.activeRunningTexts,
                        fontSize = uiState.config.runningTextFontSize
                    )
                }
            }
        }

            val isFridayNow = (uiState.isFridayPrayer || (uiState.currentCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && uiState.adhanPrayerName == PrayerName.DZUHUR)) && uiState.config.showFridayOfficers

            // Fullscreen Dynamic Overlays
            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.PRE_ADHAN_COUNTDOWN,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 1.1f)
            ) {
                PreAdhanCountdownOverlay(
                    prayerName = uiState.adhanPrayerName,
                    remainingSeconds = uiState.preAdhanRemainingSeconds,
                    imamName = uiState.activeImamName,
                    muadzinName = uiState.activeMuadzinName,
                    khotibName = uiState.activeKhotibName,
                    bilalName = uiState.activeBilalName,
                    isFriday = isFridayNow,
                    config = uiState.config,
                    onDismiss = { viewModel.resetToNormalMode() }
                )
            }

            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.ADHAN,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 1.1f)
            ) {
                AdhanOverlay(
                    prayerName = uiState.adhanPrayerName,
                    imamName = uiState.activeImamName,
                    muadzinName = uiState.activeMuadzinName,
                    khotibName = uiState.activeKhotibName,
                    bilalName = uiState.activeBilalName,
                    isFriday = isFridayNow,
                    onDismiss = { viewModel.resetToNormalMode() }
                )
            }

            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.IQOMAH_COUNTDOWN,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 1.1f)
            ) {
                IqomahOverlay(
                    prayerName = uiState.adhanPrayerName,
                    remainingSeconds = uiState.iqomahRemainingSeconds,
                    imamName = uiState.activeImamName,
                    isFriday = isFridayNow,
                    config = uiState.config,
                    onDismiss = { viewModel.resetToNormalMode() }
                )
            }

            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.SHOLAT_SILENT,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SholatSilentOverlay(
                    prayerName = uiState.adhanPrayerName,
                    remainingSeconds = uiState.sholatSilentRemainingSeconds,
                    imamName = uiState.activeImamName,
                    isFriday = isFridayNow,
                    onDismiss = { viewModel.resetToNormalMode() }
                )
            }

            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.FULLSCREEN_VIDEO,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 1.05f)
            ) {
                FullscreenVideoOverlay(
                    videoPath = uiState.config.fullscreenVideoPath,
                    title = uiState.config.fullscreenVideoTitle,
                    isMuted = uiState.config.fullscreenVideoMuted,
                    onDismiss = { viewModel.stopFullscreenVideo() }
                )
            }

            AnimatedVisibility(
                visible = uiState.displayMode == TVDisplayMode.FULLSCREEN_CCTV,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 1.05f)
            ) {
                FullscreenCctvOverlay(
                    cctvUrl = uiState.config.cctvStreamUrl,
                    title = uiState.config.cctvStreamTitle,
                    isMuted = uiState.config.cctvStreamMuted,
                    cameras = uiState.config.getCctvCameras(),
                    activeCameraId = uiState.config.cctvActiveCameraId,
                    displayLayout = uiState.config.cctvDisplayLayout,
                    onDismiss = { viewModel.stopFullscreenCctv() }
                )
            }

            // In-App Settings Dialog
            if (uiState.showSettingsDialog) {
                SettingsDialog(
                    config = uiState.config,
                    serverUrl = uiState.serverUrl,
                    localIp = uiState.localIpAddress,
                    qrBitmap = uiState.qrCodeBitmap,
                    selectedTab = uiState.settingsSelectedTab,
                    onSelectTab = { viewModel.setSettingsTab(it) },
                    onDismiss = { viewModel.toggleSettingsDialog(false) },
                    onSaveConfig = { viewModel.saveConfig(it) },
                    onAddFinance = { title, amt, type, cat, date, notes ->
                        viewModel.addFinance(title, amt, type, cat, date, notes)
                    },
                    onDeleteFinance = { viewModel.deleteFinance(it) },
                    finances = uiState.finances,
                    onSaveFriday = { viewModel.saveFridaySchedule(it) },
                    fridaySchedule = uiState.fridaySchedule,
                    allFridaySchedules = uiState.allFridaySchedules,
                    upcomingFridayIndex = uiState.upcomingFridayWeekIndex,
                    onAddActivity = { title, spk, date, time, loc, desc, cat ->
                        viewModel.addActivity(title, spk, date, time, loc, desc, cat)
                    },
                    onDeleteActivity = { viewModel.deleteActivity(it) },
                    activities = uiState.activities,
                    onAddRunningText = { viewModel.addRunningText(it) },
                    onUpdateRunningText = { id, text, isActive, order -> viewModel.updateRunningText(id, text, isActive, order) },
                    onDeleteRunningText = { viewModel.deleteRunningText(it) },
                    runningTexts = uiState.activeRunningTexts,
                    onTriggerAdhan = { viewModel.triggerAdhanManual() },
                    onTriggerIqomah = { viewModel.triggerIqomahManual() },
                    onTriggerSholat = { viewModel.triggerSholatSilentManual() },
                    onTestChime = { viewModel.playChimeTest() },
                    onPlayMurottalPreset = { viewModel.playMurottalManual(presetId = it) },
                    onStopMurottal = { viewModel.stopMurottalManual() }
                )
            }
        }
    }
}
