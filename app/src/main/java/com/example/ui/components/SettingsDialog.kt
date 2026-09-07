package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.prayer.IndonesiaCityData
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    config: MosqueConfig,
    serverUrl: String,
    localIp: String,
    qrBitmap: ImageBitmap?,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSaveConfig: (MosqueConfig) -> Unit,
    onAddFinance: (String, Long, TransactionType, String, String, String) -> Unit,
    onDeleteFinance: (Long) -> Unit,
    finances: List<FinanceTransaction>,
    onSaveFriday: (FridaySchedule) -> Unit,
    fridaySchedule: FridaySchedule,
    allFridaySchedules: List<FridaySchedule> = emptyList(),
    upcomingFridayIndex: Int = 1,
    onAddActivity: (String, String, String, String, String, String, String) -> Unit,
    onDeleteActivity: (Long) -> Unit,
    activities: List<MosqueActivity>,
    onAddRunningText: (String) -> Unit,
    onUpdateRunningText: (Long, String, Boolean, Int) -> Unit = { _, _, _, _ -> },
    onDeleteRunningText: (Long) -> Unit,
    runningTexts: List<RunningTextItem>,
    onTriggerAdhan: () -> Unit,
    onTriggerIqomah: () -> Unit,
    onTriggerSholat: () -> Unit,
    onTestChime: () -> Unit,
    onPlayMurottalPreset: (String) -> Unit = {},
    onStopMurottal: () -> Unit = {}
) {
    var mosqueName by remember(config) { mutableStateOf(config.mosqueName) }
    var tagline by remember(config) { mutableStateOf(config.tagline) }
    var address by remember(config) { mutableStateOf(config.address) }
    var city by remember(config) { mutableStateOf(config.city) }
    var phone by remember(config) { mutableStateOf(config.phone) }
    var bankName by remember(config) { mutableStateOf(config.bankName) }
    var bankAccount by remember(config) { mutableStateOf(config.bankAccount) }
    var bankAccountName by remember(config) { mutableStateOf(config.bankAccountName) }
    var latitude by remember(config) { mutableStateOf(config.latitude) }
    var longitude by remember(config) { mutableStateOf(config.longitude) }
    var timezone by remember(config) { mutableStateOf(config.timezone) }

    var showImsak by remember(config) { mutableStateOf(config.showImsak) }
    var showSubuh by remember(config) { mutableStateOf(config.showSubuh) }
    var showTerbit by remember(config) { mutableStateOf(config.showTerbit) }
    var showDhuha by remember(config) { mutableStateOf(config.showDhuha) }
    var showDzuhur by remember(config) { mutableStateOf(config.showDzuhur) }
    var showAshar by remember(config) { mutableStateOf(config.showAshar) }
    var showMaghrib by remember(config) { mutableStateOf(config.showMaghrib) }
    var showIsya by remember(config) { mutableStateOf(config.showIsya) }

    var offSubuh by remember(config) { mutableStateOf(config.offsetSubuh.toString()) }
    var offDzuhur by remember(config) { mutableStateOf(config.offsetDzuhur.toString()) }
    var offAshar by remember(config) { mutableStateOf(config.offsetAshar.toString()) }
    var offMaghrib by remember(config) { mutableStateOf(config.offsetMaghrib.toString()) }
    var offIsya by remember(config) { mutableStateOf(config.offsetIsya.toString()) }

    var iqSubuh by remember(config) { mutableStateOf(config.iqomahSubuh.toString()) }
    var iqDzuhur by remember(config) { mutableStateOf(config.iqomahDzuhur.toString()) }
    var iqJumat by remember(config) { mutableStateOf(config.iqomahJumat.toString()) }
    var iqAshar by remember(config) { mutableStateOf(config.iqomahAshar.toString()) }
    var iqMaghrib by remember(config) { mutableStateOf(config.iqomahMaghrib.toString()) }
    var iqIsya by remember(config) { mutableStateOf(config.iqomahIsya.toString()) }

    var enablePreAdhan by remember(config) { mutableStateOf(config.enablePreAdhanCountdown) }
    var preAdhanSecs by remember(config) { mutableStateOf(config.preAdhanCountdownSeconds.toString()) }
    var imamSubuh by remember(config) { mutableStateOf(config.imamSubuh) }
    var imamDzuhur by remember(config) { mutableStateOf(config.imamDzuhur) }
    var imamAshar by remember(config) { mutableStateOf(config.imamAshar) }
    var imamMaghrib by remember(config) { mutableStateOf(config.imamMaghrib) }
    var imamIsya by remember(config) { mutableStateOf(config.imamIsya) }

    var murottalEnabled by remember(config) { mutableStateOf(config.murottalEnabled) }
    var murottalSubuh by remember(config) { mutableStateOf(config.murottalSubuh) }
    var murottalDzuhur by remember(config) { mutableStateOf(config.murottalDzuhur) }
    var murottalAshar by remember(config) { mutableStateOf(config.murottalAshar) }
    var murottalMaghrib by remember(config) { mutableStateOf(config.murottalMaghrib) }
    var murottalIsya by remember(config) { mutableStateOf(config.murottalIsya) }
    var murottalJumat by remember(config) { mutableStateOf(config.murottalJumat) }
    var murottalDuration by remember(config) { mutableStateOf(config.murottalDurationMinutes.toString()) }
    var murottalVolume by remember(config) { mutableStateOf(config.murottalVolume.toString()) }
    var murottalPresetId by remember(config) { mutableStateOf(config.murottalSelectedPresetId) }
    var murottalSourceType by remember(config) { mutableStateOf(config.murottalSourceType) }

    var youtubeUrl by remember(config) { mutableStateOf(config.youtubeLiveUrl) }
    var youtubeTitle by remember(config) { mutableStateOf(config.youtubeLiveTitle) }
    var youtubeEnabled by remember(config) { mutableStateOf(config.youtubeLiveEnabled) }
    var youtubeShowSlide by remember(config) { mutableStateOf(config.showYoutubeLiveSlide) }
    var youtubeMuted by remember(config) { mutableStateOf(config.youtubeLiveMuted) }

    var tarawihEnabled by remember(config) { mutableStateOf(config.tarawihEnabled) }
    var tarawihAutoDetectNight by remember(config) { mutableStateOf(config.tarawihAutoDetectNight) }
    var tarawihManualNight by remember(config) { mutableStateOf(config.tarawihManualNight.toString()) }
    var tarawihShowSlide by remember(config) { mutableStateOf(config.tarawihShowSlide) }
    var tarawihKultumMinutes by remember(config) { mutableStateOf(config.tarawihKultumMinutes.toString()) }
    var tarawihSholatMinutes by remember(config) { mutableStateOf(config.tarawihSholatMinutes.toString()) }
    var tarawihBgPreset by remember(config) { mutableStateOf(config.tarawihBgPreset) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, IslamicGoldPrimary, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06281D))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IslamicEmeraldDark)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🕌", fontSize = 16.sp)
                            Text(
                                "PENGATURAN TV MASJIDKU",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = IslamicGoldBright,
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Content Area: Sidebar + Active Tab Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left Sidebar Nav
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF031911))
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val navItems = listOf(
                                "📲 Sambung HP" to 0,
                                "🎧 Murottal & Tarhim" to 8,
                                "🔴 YouTube Live" to 9,
                                "🌙 Tarawih Ramadhan" to 10,
                                "🕌 Profil & Lokasi" to 1,
                                "🎨 Tema & Jam" to 2,
                                "⏰ Jadwal & Imam" to 3,
                                "💰 Kas Keuangan" to 4,
                                "📋 Petugas Jum'at" to 5,
                                "📅 Agenda Kegiatan" to 6,
                                "📜 Running Text" to 7
                            )

                            navItems.forEach { (label, idx) ->
                                val isSel = selectedTab == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) IslamicGoldPrimary else Color.Transparent)
                                        .clickable { onSelectTab(idx) }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) IslamicEmeraldDark else Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_close_settings"),
                                contentPadding = PaddingValues(vertical = 5.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x26FFFFFF))
                            ) {
                                Text("✕ Tutup", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        // Right Content Area (Scrollable & Responsive)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(10.dp)
                        ) {
                            when (selectedTab) {
                                0 -> TabMobileConnect(serverUrl, localIp, qrBitmap, config.webAdminPassword)
                                1 -> TabMosqueProfile(
                                    mosqueName = mosqueName, onNameChange = { mosqueName = it },
                                    tagline = tagline, onTaglineChange = { tagline = it },
                                    address = address, onAddressChange = { address = it },
                                    city = city, onCityChange = { city = it },
                                    phone = phone, onPhoneChange = { phone = it },
                                    bankName = bankName, onBankNameChange = { bankName = it },
                                    bankAccount = bankAccount, onBankAccountChange = { bankAccount = it },
                                    bankAccountName = bankAccountName, onBankAccountNameChange = { bankAccountName = it },
                                    latitude = latitude, onLatChange = { latitude = it },
                                    longitude = longitude, onLngChange = { longitude = it },
                                    timezone = timezone, onTzChange = { timezone = it },
                                    onSave = {
                                        onSaveConfig(
                                            config.copy(
                                                mosqueName = mosqueName,
                                                tagline = tagline,
                                                address = address,
                                                city = city,
                                                phone = phone,
                                                bankName = bankName,
                                                bankAccount = bankAccount,
                                                bankAccountName = bankAccountName,
                                                latitude = latitude,
                                                longitude = longitude,
                                                timezone = timezone
                                            )
                                        )
                                    }
                                )
                                2 -> TabThemeAndSim(
                                    config = config,
                                    onSaveConfig = onSaveConfig,
                                    onTriggerAdhan = onTriggerAdhan,
                                    onTriggerIqomah = onTriggerIqomah,
                                    onTriggerSholat = onTriggerSholat,
                                    onTestChime = onTestChime
                                )
                                3 -> TabPrayerSettings(
                                    showImsak = showImsak, onShowImsak = { showImsak = it },
                                    showSubuh = showSubuh, onShowSubuh = { showSubuh = it },
                                    showTerbit = showTerbit, onShowTerbit = { showTerbit = it },
                                    showDhuha = showDhuha, onShowDhuha = { showDhuha = it },
                                    showDzuhur = showDzuhur, onShowDzuhur = { showDzuhur = it },
                                    showAshar = showAshar, onShowAshar = { showAshar = it },
                                    showMaghrib = showMaghrib, onShowMaghrib = { showMaghrib = it },
                                    showIsya = showIsya, onShowIsya = { showIsya = it },
                                    offSubuh = offSubuh, onOffSubuh = { offSubuh = it },
                                    offDzuhur = offDzuhur, onOffDzuhur = { offDzuhur = it },
                                    offAshar = offAshar, onOffAshar = { offAshar = it },
                                    offMaghrib = offMaghrib, onOffMaghrib = { offMaghrib = it },
                                    offIsya = offIsya, onOffIsya = { offIsya = it },
                                    iqSubuh = iqSubuh, onIqSubuh = { iqSubuh = it },
                                    iqDzuhur = iqDzuhur, onIqDzuhur = { iqDzuhur = it },
                                    iqJumat = iqJumat, onIqJumat = { iqJumat = it },
                                    iqAshar = iqAshar, onIqAshar = { iqAshar = it },
                                    iqMaghrib = iqMaghrib, onIqMaghrib = { iqMaghrib = it },
                                    iqIsya = iqIsya, onIqIsya = { iqIsya = it },
                                    enablePreAdhan = enablePreAdhan, onEnablePreAdhan = { enablePreAdhan = it },
                                    preAdhanSecs = preAdhanSecs, onPreAdhanSecs = { preAdhanSecs = it },
                                    imamSubuh = imamSubuh, onImamSubuh = { imamSubuh = it },
                                    imamDzuhur = imamDzuhur, onImamDzuhur = { imamDzuhur = it },
                                    imamAshar = imamAshar, onImamAshar = { imamAshar = it },
                                    imamMaghrib = imamMaghrib, onImamMaghrib = { imamMaghrib = it },
                                    imamIsya = imamIsya, onImamIsya = { imamIsya = it },
                                    onSave = {
                                        onSaveConfig(
                                            config.copy(
                                                mosqueName = mosqueName,
                                                tagline = tagline,
                                                address = address,
                                                city = city,
                                                phone = phone,
                                                bankName = bankName,
                                                bankAccount = bankAccount,
                                                bankAccountName = bankAccountName,
                                                latitude = latitude,
                                                longitude = longitude,
                                                timezone = timezone,
                                                showImsak = showImsak,
                                                showSubuh = showSubuh,
                                                showTerbit = showTerbit,
                                                showDhuha = showDhuha,
                                                showDzuhur = showDzuhur,
                                                showAshar = showAshar,
                                                showMaghrib = showMaghrib,
                                                showIsya = showIsya,
                                                offsetSubuh = offSubuh.toIntOrNull() ?: config.offsetSubuh,
                                                offsetDzuhur = offDzuhur.toIntOrNull() ?: config.offsetDzuhur,
                                                offsetAshar = offAshar.toIntOrNull() ?: config.offsetAshar,
                                                offsetMaghrib = offMaghrib.toIntOrNull() ?: config.offsetMaghrib,
                                                offsetIsya = offIsya.toIntOrNull() ?: config.offsetIsya,
                                                iqomahSubuh = iqSubuh.toIntOrNull() ?: config.iqomahSubuh,
                                                iqomahDzuhur = iqDzuhur.toIntOrNull() ?: config.iqomahDzuhur,
                                                iqomahJumat = iqJumat.toIntOrNull() ?: config.iqomahJumat,
                                                iqomahAshar = iqAshar.toIntOrNull() ?: config.iqomahAshar,
                                                iqomahMaghrib = iqMaghrib.toIntOrNull() ?: config.iqomahMaghrib,
                                                iqomahIsya = iqIsya.toIntOrNull() ?: config.iqomahIsya,
                                                enablePreAdhanCountdown = enablePreAdhan,
                                                preAdhanCountdownSeconds = preAdhanSecs.toIntOrNull() ?: config.preAdhanCountdownSeconds,
                                                imamSubuh = imamSubuh,
                                                imamDzuhur = imamDzuhur,
                                                imamAshar = imamAshar,
                                                imamMaghrib = imamMaghrib,
                                                imamIsya = imamIsya
                                            )
                                        )
                                    }
                                )
                                4 -> TabFinanceSettings(finances, onAddFinance, onDeleteFinance)
                                5 -> TabMultiFridaySettings(
                                    allSchedules = allFridaySchedules,
                                    upcomingIndex = upcomingFridayIndex,
                                    config = config,
                                    onSaveConfig = onSaveConfig,
                                    onSaveFriday = onSaveFriday
                                )
                                6 -> TabActivitiesSettings(activities, onAddActivity, onDeleteActivity)
                                7 -> TabRunningTextSettings(runningTexts, onAddRunningText, onUpdateRunningText, onDeleteRunningText)
                                8 -> TabMurottalSettings(
                                    enabled = murottalEnabled, onEnabledChange = { murottalEnabled = it },
                                    subuh = murottalSubuh, onSubuhChange = { murottalSubuh = it },
                                    dzuhur = murottalDzuhur, onDzuhurChange = { murottalDzuhur = it },
                                    ashar = murottalAshar, onAsharChange = { murottalAshar = it },
                                    maghrib = murottalMaghrib, onMaghribChange = { murottalMaghrib = it },
                                    isya = murottalIsya, onIsyaChange = { murottalIsya = it },
                                    jumat = murottalJumat, onJumatChange = { murottalJumat = it },
                                    durationStr = murottalDuration, onDurationChange = { murottalDuration = it },
                                    volumeStr = murottalVolume, onVolumeChange = { murottalVolume = it },
                                    selectedPresetId = murottalPresetId, onPresetChange = { murottalPresetId = it },
                                    sourceType = murottalSourceType, onSourceTypeChange = { murottalSourceType = it },
                                    onSave = {
                                        onSaveConfig(
                                            config.copy(
                                                murottalEnabled = murottalEnabled,
                                                murottalSubuh = murottalSubuh,
                                                murottalDzuhur = murottalDzuhur,
                                                murottalAshar = murottalAshar,
                                                murottalMaghrib = murottalMaghrib,
                                                murottalIsya = murottalIsya,
                                                murottalJumat = murottalJumat,
                                                murottalDurationMinutes = murottalDuration.toIntOrNull() ?: 10,
                                                murottalVolume = murottalVolume.toIntOrNull() ?: 80,
                                                murottalSelectedPresetId = murottalPresetId,
                                                murottalSourceType = murottalSourceType
                                            )
                                        )
                                    },
                                    onPlayPreset = onPlayMurottalPreset,
                                    onStopPlayer = onStopMurottal
                                )
                                10 -> TabTarawihSettings(
                                    config = config,
                                    tarawihEnabled = tarawihEnabled,
                                    onTarawihEnabledChange = { tarawihEnabled = it },
                                    tarawihAutoDetectNight = tarawihAutoDetectNight,
                                    onTarawihAutoDetectChange = { tarawihAutoDetectNight = it },
                                    tarawihManualNight = tarawihManualNight,
                                    onTarawihManualNightChange = { tarawihManualNight = it },
                                    tarawihShowSlide = tarawihShowSlide,
                                    onTarawihShowSlideChange = { tarawihShowSlide = it },
                                    tarawihKultumMinutes = tarawihKultumMinutes,
                                    onTarawihKultumMinutesChange = { tarawihKultumMinutes = it },
                                    tarawihSholatMinutes = tarawihSholatMinutes,
                                    onTarawihSholatMinutesChange = { tarawihSholatMinutes = it },
                                    tarawihBgPreset = tarawihBgPreset,
                                    onTarawihBgPresetChange = { tarawihBgPreset = it },
                                    onSave = {
                                        onSaveConfig(
                                            config.copy(
                                                tarawihEnabled = tarawihEnabled,
                                                tarawihAutoDetectNight = tarawihAutoDetectNight,
                                                tarawihManualNight = tarawihManualNight.toIntOrNull() ?: config.tarawihManualNight,
                                                tarawihShowSlide = tarawihShowSlide,
                                                tarawihKultumMinutes = tarawihKultumMinutes.toIntOrNull() ?: config.tarawihKultumMinutes,
                                                tarawihSholatMinutes = tarawihSholatMinutes.toIntOrNull() ?: config.tarawihSholatMinutes,
                                                tarawihBgPreset = tarawihBgPreset
                                            )
                                        )
                                    }
                                )
                                9 -> TabYouTubeLiveSettings(
                                    url = youtubeUrl, onUrlChange = { youtubeUrl = it },
                                    title = youtubeTitle, onTitleChange = { youtubeTitle = it },
                                    enabled = youtubeEnabled, onEnabledChange = { youtubeEnabled = it },
                                    showSlide = youtubeShowSlide, onShowSlideChange = { youtubeShowSlide = it },
                                    muted = youtubeMuted, onMutedChange = { youtubeMuted = it },
                                    onSave = {
                                        onSaveConfig(
                                            config.copy(
                                                youtubeLiveUrl = youtubeUrl,
                                                youtubeLiveTitle = youtubeTitle,
                                                youtubeLiveEnabled = youtubeEnabled,
                                                showYoutubeLiveSlide = youtubeShowSlide,
                                                youtubeLiveMuted = youtubeMuted
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB IMPLEMENTATIONS ----------------

@Composable
private fun TabMobileConnect(serverUrl: String, localIp: String, qrBitmap: ImageBitmap?, webAdminPassword: String = "123456") {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (qrBitmap != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR Code Dashboard HP",
                    modifier = Modifier.size(160.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "📱 Atur TV Lewat HP (Jaringan Wi-Fi Sama)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = IslamicGoldBright
            )
            Text(
                text = "1. Hubungkan HP ke Wi-Fi yang sama dengan Android TV ini.\n" +
                        "2. Scan QR Code atau buka alamat berikut di Google Chrome / Safari HP Anda:",
                fontSize = 11.sp,
                color = Color.White,
                lineHeight = 16.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF031911))
                    .border(1.dp, IslamicGoldPrimary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = "🌐 Alamat Browser HP:",
                        fontSize = 9.sp,
                        color = Color(0xFFA5D6A7)
                    )
                    Text(
                        text = serverUrl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = IslamicGoldBright,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔐 PIN / Password Login:",
                        fontSize = 9.sp,
                        color = Color(0xFFA5D6A7)
                    )
                    Text(
                        text = "🔑 $webAdminPassword",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "✨ Dilengkapi proteksi login! Masukkan PIN di atas saat membuka alamat web remote di HP.",
                fontSize = 10.sp,
                color = Color(0xFFA5D6A7)
            )
        }
    }
}

@Composable
private fun TabMosqueProfile(
    mosqueName: String, onNameChange: (String) -> Unit,
    tagline: String, onTaglineChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    bankName: String, onBankNameChange: (String) -> Unit,
    bankAccount: String, onBankAccountChange: (String) -> Unit,
    bankAccountName: String, onBankAccountNameChange: (String) -> Unit,
    latitude: Double, onLatChange: (Double) -> Unit,
    longitude: Double, onLngChange: (Double) -> Unit,
    timezone: Double, onTzChange: (Double) -> Unit,
    onSave: () -> Unit
) {
    var savedToast by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }
    var showCityDropdown by remember { mutableStateOf(false) }

    val filteredCities = remember(citySearchQuery) {
        IndonesiaCityData.findCity(citySearchQuery).take(15)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Identitas & Lokasi Masjid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

        OutlinedTextField(
            value = mosqueName,
            onValueChange = onNameChange,
            label = { Text("Nama Masjid", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = tagline,
            onValueChange = onTaglineChange,
            label = { Text("Tagline / Visi", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Alamat Lengkap", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Kabupaten / Kota Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1A000000))
                .border(1.dp, Color(0x3334D399), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍 Pilihan Kabupaten / Kota & Zona Waktu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
                Text("Zona: ${if (timezone == 7.0) "WIB" else if (timezone == 8.0) "WITA" else "WIT"}", fontSize = 10.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = citySearchQuery,
                onValueChange = {
                    citySearchQuery = it
                    showCityDropdown = true
                },
                label = { Text("Cari Kabupaten / Kota di Indonesia...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showCityDropdown = !showCityDropdown }) {
                        Icon(if (showCityDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = "Pilih Kota")
                    }
                }
            )

            if (showCityDropdown) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF031911))
                        .border(1.dp, IslamicGoldPrimary, RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    filteredCities.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onCityChange(item.displayName)
                                    onLatChange(item.latitude)
                                    onLngChange(item.longitude)
                                    onTzChange(item.timezone)
                                    citySearchQuery = item.name
                                    showCityDropdown = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(item.province, fontSize = 9.sp, color = TextWhiteDim)
                            }
                            Text(item.getTimezoneName(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SleekAmber400)
                        }
                        HorizontalDivider(color = Color(0x14FFFFFF))
                    }
                }
            }

            Text(
                text = "Terpilih: $city | Lat $latitude, Long $longitude (UTC+$timezone)",
                fontSize = 10.sp,
                color = SleekEmerald300
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("No. Kontak DKM", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = bankName,
                onValueChange = onBankNameChange,
                label = { Text("Nama Bank", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bankAccount,
                onValueChange = onBankAccountChange,
                label = { Text("No. Rekening", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = bankAccountName,
                onValueChange = onBankAccountNameChange,
                label = { Text("Atas Nama Rekening", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(
            onClick = {
                onSave()
                savedToast = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Text("💾 Simpan Perubahan Profil & Lokasi", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (savedToast) {
            Text("✅ Profil & Lokasi Masjid Tersimpan!", color = Color(0xFF81C784), fontSize = 11.sp)
        }
    }
}

@Composable
private fun TabThemeAndSim(
    config: MosqueConfig,
    onSaveConfig: (MosqueConfig) -> Unit,
    onTriggerAdhan: () -> Unit,
    onTriggerIqomah: () -> Unit,
    onTriggerSholat: () -> Unit,
    onTestChime: () -> Unit
) {
    val layoutModels = listOf(
        Triple("MIHRAB_GRAND_ROYAL", "🕌 Model 7: Grand Mihrab Nabawi (Portal Jam Kubah + Kartu Berkubah + Card Tengah Emas)", "Layar megah nuansa Islami: portal kubah jam tengah atas, 6 kartu jadwal sholat berkubah lancip emas, dan card tengah petugas & kas."),
        Triple("CORDOBA_ANDALUSIA", "🏛️ Model 8: Cordoba & Alhambra Moorish (Lengkung Tapal Kuda + QRIS Infaq)", "Arsitektur Andalusia Spanyol: portal & kartu kubah tapal kuda bermotif belang merah-emas khas Masjid Cordoba."),
        Triple("CAROUSEL_BOTTOM", "📺 Model 1: Layar Informasi Berputar (Carousel)", "Jadwal sholat horizontal di bawah. Layar tengah memutar Laporan Kas, Petugas Jum'at, Agenda, & Maklumat."),
        Triple("SPLIT_DASHBOARD", "📊 Model 2: Split Dashboard Modern (Jam Samping)", "Jadwal sholat vertikal samping. Layar utama menampilkan Kas Keuangan & Petugas Jum'at berdampingan!"),
        Triple("DUAL_INFO_COMPACT", "🗂️ Model 3: Dual Card Berdampingan (Kas & Petugas)", "Card Kas Keuangan & Petugas Jum'at berdampingan di tengah dengan bar waktu sholat ringkas di bawah."),
        Triple("SIDEBAR_ANALOG_NEO", "🕌 Model 4: Al-Amin Cyber (Jadwal Kiri + Jam Analog + Foto)", "Jadwal sholat samping kiri kotak neon cyber, jam analog elegan di tengah, dan foto/poster utama."),
        Triple("RIGHT_WAVE_ANALOG", "🌊 Model 5: Al-Ikhsan Wave (Panel Wave Kanan + Jam Analog)", "Foto masjid luas di kiri dan panel gelombang biru di kanan dengan Jam Analog & ikon sholat."),
        Triple("WAKTIHA_CLASSIC", "⏱️ Model 6: Waktiha Klaten (Jam Digital Besar Kiri + Petugas)", "Jam digital besar '10:10' di kiri dengan countdown & petugas, foto di kanan, dan 7 bar hijau di bawah.")
    )

    val themes = listOf(
        Triple("DYNAMIC_SKY", "🌅 Langit Dinamis 5 Waktu (Otomatis)", "Warna latar berganti otomatis mengikuti fajar, terbit, dzuhur, ashar, maghrib, & malam"),
        Triple("NABAWI_EMERALD", "🌿 Mihrab Nabawi (Hijau & Emas)", "Nuansa Kubah Masjid Nabawi Madinah & Ornamen Emas"),
        Triple("KISWAH_GOLD", "🖤 Kiswah Ka'bah (Hitam & Emas 24K)", "Keagungan kain Kiswah Ka'bah Makkah kontras tinggi"),
        Triple("OTTOMAN_BLUE", "🌌 Blue Ottoman (Safir & Pirus)", "Keindahan Blue Mosque Istanbul & Samarkand"),
        Triple("EMERALD_GOLD", "🍃 Emerald Green & Gold", "Hijau Zamrud & Aksen Emas Berkilau"),
        Triple("ROYAL_NAVY", "🌊 Royal Navy & Star", "Biru Malam Elegan & Cyan Bintang"),
        Triple("SUNSET_AMBER", "🌅 Sunset Amber & Gold", "Senja Hangat Jingga Keemasan"),
        Triple("MIDNIGHT_CHARCOAL", "🌑 Midnight Dark Luxury", "Abu Gelap Minimalis Mewah"),
        Triple("MIHRAB_CLASSIC", "🕌 Mihrab Klasik Kubah", "Nuansa Mihrab Kubah Hijau Emas")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. KONTROL TAMPILAN KONTEN & SLIDE TV (PALING ATAS)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33064E3B))
                .border(1.2.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎛️ Kontrol Tampilan Informasi & Slide TV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
                Text("Centang konten yang ingin diaktifkan di layar TV. Jika tidak dicentang, konten tidak akan pernah tampil:", fontSize = 10.5.sp, color = Color(0xFFA7F3D0))

                val contentToggles = listOf(
                    Triple("💳 Laporan Kas", config.showFinancialReport) { v: Boolean -> onSaveConfig(config.copy(showFinancialReport = v)) },
                    Triple("👥 Petugas Jum'at", config.showFridayOfficers) { v: Boolean -> onSaveConfig(config.copy(showFridayOfficers = v)) },
                    Triple("📱 Infaq QRIS", config.showQrisCard) { v: Boolean -> onSaveConfig(config.copy(showQrisCard = v)) },
                    Triple("📖 Mutiara Hadits", config.showDailyHadith) { v: Boolean -> onSaveConfig(config.copy(showDailyHadith = v)) },
                    Triple("📅 Agenda / Kajian", config.showActivities) { v: Boolean -> onSaveConfig(config.copy(showActivities = v)) },
                    Triple("📢 Maklumat Harian", config.showDailyMaklumat) { v: Boolean -> onSaveConfig(config.copy(showDailyMaklumat = v)) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    contentToggles.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { (label, isChecked, onToggle) ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isChecked) Color(0x4D10B981) else Color(0x1A000000))
                                        .border(1.dp, if (isChecked) Color(0xFF34D399) else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                        .clickable { onToggle(!isChecked) }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = onToggle,
                                        colors = CheckboxDefaults.colors(checkedColor = IslamicGoldPrimary, checkmarkColor = IslamicEmeraldDark),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) Color.White else TextWhiteDim
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text("Pilihan Model Tata Letak Layar TV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            layoutModels.forEach { (code, title, desc) ->
                val isSel = config.displayLayoutModel == code
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0x3310B981) else Color(0x14FFFFFF))
                        .border(1.2.dp, if (isSel) IslamicGoldPrimary else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onSaveConfig(config.copy(displayLayoutModel = code)) }
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) IslamicGoldBright else Color.White)
                        if (isSel) {
                            Text("AKTIF ✓", fontSize = 10.sp, fontWeight = FontWeight.Black, color = IslamicGoldBright)
                        }
                    }
                    Text(desc, fontSize = 10.sp, color = if (isSel) Color(0xFFE2E8F0) else TextWhiteDim)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Pilihan Tema Warna Layar TV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            themes.forEach { (code, title, desc) ->
                val isSel = config.activeTheme == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0x33F59E0B) else Color(0x14FFFFFF))
                        .border(1.dp, if (isSel) IslamicGoldPrimary else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onSaveConfig(config.copy(activeTheme = code)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) IslamicGoldBright else Color.White)
                        Text(desc, fontSize = 9.sp, color = TextWhiteDim)
                    }
                    if (isSel) {
                        Text("Terpilih ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Simulasi Layar & Pengujian Audio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onTestChime, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)), contentPadding = PaddingValues(vertical = 4.dp)) {
                Text("🔔 Chime", fontSize = 10.sp)
            }
            Button(onClick = onTriggerAdhan, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C3A)), contentPadding = PaddingValues(vertical = 4.dp)) {
                Text("🕌 Adzan", fontSize = 10.sp)
            }
            Button(onClick = onTriggerIqomah, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)), contentPadding = PaddingValues(vertical = 4.dp)) {
                Text("⏱️ Iqomah", fontSize = 10.sp)
            }
            Button(onClick = onTriggerSholat, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)), contentPadding = PaddingValues(vertical = 4.dp)) {
                Text("📿 Hening", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TabPrayerSettings(
    showImsak: Boolean, onShowImsak: (Boolean) -> Unit,
    showSubuh: Boolean, onShowSubuh: (Boolean) -> Unit,
    showTerbit: Boolean, onShowTerbit: (Boolean) -> Unit,
    showDhuha: Boolean, onShowDhuha: (Boolean) -> Unit,
    showDzuhur: Boolean, onShowDzuhur: (Boolean) -> Unit,
    showAshar: Boolean, onShowAshar: (Boolean) -> Unit,
    showMaghrib: Boolean, onShowMaghrib: (Boolean) -> Unit,
    showIsya: Boolean, onShowIsya: (Boolean) -> Unit,
    offSubuh: String, onOffSubuh: (String) -> Unit,
    offDzuhur: String, onOffDzuhur: (String) -> Unit,
    offAshar: String, onOffAshar: (String) -> Unit,
    offMaghrib: String, onOffMaghrib: (String) -> Unit,
    offIsya: String, onOffIsya: (String) -> Unit,
    iqSubuh: String, onIqSubuh: (String) -> Unit,
    iqDzuhur: String, onIqDzuhur: (String) -> Unit,
    iqJumat: String, onIqJumat: (String) -> Unit,
    iqAshar: String, onIqAshar: (String) -> Unit,
    iqMaghrib: String, onIqMaghrib: (String) -> Unit,
    iqIsya: String, onIqIsya: (String) -> Unit,
    enablePreAdhan: Boolean, onEnablePreAdhan: (Boolean) -> Unit,
    preAdhanSecs: String, onPreAdhanSecs: (String) -> Unit,
    imamSubuh: String, onImamSubuh: (String) -> Unit,
    imamDzuhur: String, onImamDzuhur: (String) -> Unit,
    imamAshar: String, onImamAshar: (String) -> Unit,
    imamMaghrib: String, onImamMaghrib: (String) -> Unit,
    imamIsya: String, onImamIsya: (String) -> Unit,
    onSave: () -> Unit
) {
    var savedToast by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Hitung Mundur Pra-Adzan
        Text("⏳ Hitung Mundur Jelang Adzan (Pra-Adzan)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1A000000))
                .border(1.dp, Color(0x3334D399), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onEnablePreAdhan(!enablePreAdhan) }
                    .padding(4.dp)
            ) {
                Checkbox(
                    checked = enablePreAdhan,
                    onCheckedChange = onEnablePreAdhan,
                    colors = CheckboxDefaults.colors(checkedColor = IslamicGoldPrimary, checkmarkColor = IslamicEmeraldDark)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("Aktifkan Layar Hitung Mundur Jelang Adzan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Menampilkan timer detik, ajakan wudhu & nama imam sebelum adzan", fontSize = 9.sp, color = TextWhiteDim)
                }
            }

            OutlinedTextField(
                value = preAdhanSecs,
                onValueChange = onPreAdhanSecs,
                label = { Text("Durasi (Detik)", fontSize = 10.sp) },
                modifier = Modifier.width(110.dp),
                singleLine = true
            )
        }

        // 2. Imam Sholat Harian 5 Waktu
        Text("👳 Nama Imam Sholat Harian (5 Waktu)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
        Text("Nama imam akan ditampilkan di layar TV saat pra-adzan, adzan, iqomah, dan sholat:", fontSize = 10.sp, color = TextWhiteDim)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = imamSubuh, onValueChange = onImamSubuh, label = { Text("Imam Subuh", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = imamDzuhur, onValueChange = onImamDzuhur, label = { Text("Imam Dzuhur", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = imamAshar, onValueChange = onImamAshar, label = { Text("Imam Ashar", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = imamMaghrib, onValueChange = onImamMaghrib, label = { Text("Imam Maghrib", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = imamIsya, onValueChange = onImamIsya, label = { Text("Imam Isya", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
        }

        // 3. Waktu Sholat & Koreksi
        Text("Pilihan Waktu Sholat & Koreksi Menit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1A000000))
                .border(1.dp, Color(0x3334D399), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Pilihan Waktu Sholat yang Ditampilkan di Layar TV:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

            val prayerToggles = listOf(
                Triple("Imsak", showImsak, onShowImsak),
                Triple("Subuh", showSubuh, onShowSubuh),
                Triple("Terbit", showTerbit, onShowTerbit),
                Triple("Dhuha", showDhuha, onShowDhuha),
                Triple("Dzuhur", showDzuhur, onShowDzuhur),
                Triple("Ashar", showAshar, onShowAshar),
                Triple("Maghrib", showMaghrib, onShowMaghrib),
                Triple("Isya'", showIsya, onShowIsya)
            )

            for (chunk in prayerToggles.chunked(4)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chunk.forEach { (label, isChecked, onToggle) ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChecked) Color(0x3310B981) else Color(0x14FFFFFF))
                                .border(1.dp, if (isChecked) Color(0xFF10B981) else Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                            .clickable { onToggle(!isChecked) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = onToggle,
                                colors = CheckboxDefaults.colors(checkedColor = IslamicGoldPrimary, checkmarkColor = IslamicEmeraldDark),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isChecked) Color.White else TextWhiteDim)
                        }
                    }
                }
            }
        }

        Text("Koreksi Waktu Sholat (+/- Menit):", fontSize = 11.sp, color = IslamicGoldLight)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = offSubuh, onValueChange = onOffSubuh, label = { Text("Subuh", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = offDzuhur, onValueChange = onOffDzuhur, label = { Text("Dzuhur", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = offAshar, onValueChange = onOffAshar, label = { Text("Ashar", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = offMaghrib, onValueChange = onOffMaghrib, label = { Text("Maghrib", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = offIsya, onValueChange = onOffIsya, label = { Text("Isya", fontSize = 10.sp) }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Text("Durasi Hitung Mundur Iqomah (Menit):", fontSize = 11.sp, color = IslamicGoldLight)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = iqSubuh, onValueChange = onIqSubuh, label = { Text("Subuh", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = iqDzuhur, onValueChange = onIqDzuhur, label = { Text("Dzuhur", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = iqJumat, onValueChange = onIqJumat, label = { Text("Jum'at", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = iqAshar, onValueChange = onIqAshar, label = { Text("Ashar", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = iqMaghrib, onValueChange = onIqMaghrib, label = { Text("Maghrib", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = iqIsya, onValueChange = onIqIsya, label = { Text("Isya", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Button(
            onClick = {
                onSave()
                savedToast = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Text("💾 Simpan Pengaturan Sholat & Imam", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (savedToast) {
            Text("✅ Pengaturan Sholat & Imam Berhasil Tersimpan!", color = Color(0xFF81C784), fontSize = 11.sp)
        }
    }
}

// ---------------- TAB PETUGAS JUMAT (JUMAT 1 S/D 5 OTOMATIS) ----------------

@Composable
private fun TabMultiFridaySettings(
    allSchedules: List<FridaySchedule>,
    upcomingIndex: Int,
    config: MosqueConfig? = null,
    onSaveConfig: ((MosqueConfig) -> Unit)? = null,
    onSaveFriday: (FridaySchedule) -> Unit
) {
    var selectedWeek by remember { mutableStateOf(upcomingIndex.coerceIn(1, 7)) }
    var savedToast by remember { mutableStateOf(false) }

    val defaultDate = when (selectedWeek) {
        6 -> "1 Syawal (Idul Fitri)"
        7 -> "10 Dzulhijjah (Idul Adha)"
        else -> "Jum'at Ke-$selectedWeek"
    }
    val defaultHijri = when (selectedWeek) {
        6 -> "1 Syawal"
        7 -> "10 Dzulhijjah"
        else -> "Jum'at Barakah"
    }

    // Find current schedule for selected week or fallback
    val currentSchedule = remember(allSchedules, selectedWeek) {
        allSchedules.find { it.id == selectedWeek.toLong() }
            ?: FridaySchedule(
                id = selectedWeek.toLong(),
                date = defaultDate,
                hijriDate = defaultHijri,
                khotib = "",
                imam = "",
                muadzin = "",
                bilal = "",
                khutbahTopic = ""
            )
    }

    var khotib by remember(currentSchedule) { mutableStateOf(currentSchedule.khotib) }
    var imam by remember(currentSchedule) { mutableStateOf(currentSchedule.imam) }
    var muadzin by remember(currentSchedule) { mutableStateOf(currentSchedule.muadzin) }
    var bilal by remember(currentSchedule) { mutableStateOf(currentSchedule.bilal) }
    var khutbahTopic by remember(currentSchedule) { mutableStateOf(currentSchedule.khutbahTopic) }

    val isIdulFitri = selectedWeek == 6
    val isIdulAdha = selectedWeek == 7
    val isHariRaya = isIdulFitri || isIdulAdha

    // Hari Raya Execution Settings State
    var hrEnabled by remember(config, selectedWeek) {
        mutableStateOf(if (isIdulFitri) config?.idulFitriEnabled ?: true else config?.idulAdhaEnabled ?: true)
    }
    var hrDate by remember(config, selectedWeek) {
        mutableStateOf(if (isIdulFitri) config?.idulFitriDate.orEmpty() else config?.idulAdhaDate.orEmpty())
    }
    var hrTime by remember(config, selectedWeek) {
        mutableStateOf(if (isIdulFitri) (config?.idulFitriTime?.takeIf { it.isNotBlank() } ?: "06:30") else (config?.idulAdhaTime?.takeIf { it.isNotBlank() } ?: "06:30"))
    }
    var hrIqomah by remember(config, selectedWeek) {
        mutableStateOf(if (isIdulFitri) (config?.idulFitriIqomahMinutes ?: 15).toString() else (config?.idulAdhaIqomahMinutes ?: 15).toString())
    }
    var hrSholat by remember(config, selectedWeek) {
        mutableStateOf(if (isIdulFitri) (config?.idulFitriSholatMinutes ?: 20).toString() else (config?.idulAdhaSholatMinutes ?: 20).toString())
    }

    val eventName = when (selectedWeek) {
        6 -> "Sholat Idul Fitri"
        7 -> "Sholat Idul Adha"
        else -> "Sholat Jum'at Ke-$selectedWeek"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jadwal Petugas Sholat Jum'at & Hari Raya", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x3310B981))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("🌟 Jum'at Aktif: Pekan $upcomingIndex", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
            }
        }

        Text(
            text = "💡 Sistem TV otomatis memilih & menampilkan petugas sesuai pekan Jum'at yang akan datang, serta menyediakan jadwal khusus untuk Sholat Idul Fitri dan Idul Adha.",
            fontSize = 10.sp,
            color = SleekEmerald300,
            lineHeight = 14.sp
        )

        // Switcher Tabs (Jum'at 1..5, Idul Fitri, Idul Adha)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..7).forEach { weekNum ->
                val isSelected = selectedWeek == weekNum
                val isUpcoming = upcomingIndex == weekNum && weekNum <= 5
                val btnLabel = when (weekNum) {
                    6 -> "🎉 Idul Fitri"
                    7 -> "🐑 Idul Adha"
                    else -> "Jum'at $weekNum"
                }

                Button(
                    onClick = {
                        selectedWeek = weekNum
                        savedToast = false
                    },
                    modifier = Modifier.weight(if (weekNum >= 6) 1.25f else 1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) IslamicGoldPrimary else if (isUpcoming) Color(0x4010B981) else Color(0x26FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = btnLabel,
                            fontSize = if (weekNum >= 6) 9.5.sp else 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) IslamicEmeraldDark else Color.White,
                            maxLines = 1
                        )
                        if (isUpcoming) {
                            Text("Akan Datang", fontSize = 7.5.sp, color = if (isSelected) IslamicEmeraldDark else SleekAmber400, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A000000)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (selectedWeek) {
                        6 -> "Form Petugas Sholat Idul Fitri (1 Syawal)"
                        7 -> "Form Petugas Sholat Idul Adha (10 Dzulhijjah)"
                        else -> "Form Petugas Sholat Jum'at Ke-$selectedWeek"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGoldLight
                )

                OutlinedTextField(
                    value = khotib,
                    onValueChange = { khotib = it },
                    label = { Text(if (isHariRaya) "Khotib $eventName" else "Khotib Jum'at Ke-$selectedWeek", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = imam,
                    onValueChange = { imam = it },
                    label = { Text(if (isHariRaya) "Imam $eventName" else "Imam Sholat Jum'at", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = muadzin,
                        onValueChange = { muadzin = it },
                        label = { Text(if (isHariRaya) "Muadzin / Pemandu Takbir" else "Muadzin", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bilal,
                        onValueChange = { bilal = it },
                        label = { Text(if (isHariRaya) "Bilal / Protokol" else "Bilal", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (isHariRaya) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x2610B981)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚙️ Aktifkan Otomatisasi Waktu Sholat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekAmber300)
                                Switch(
                                    checked = hrEnabled,
                                    onCheckedChange = { hrEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = hrDate,
                                    onValueChange = { hrDate = it },
                                    label = { Text("Tgl Pelaksanaan (YYYY-MM-DD)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = hrTime,
                                    onValueChange = { hrTime = it },
                                    label = { Text("Jam Sholat (HH:mm)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = hrIqomah,
                                    onValueChange = { hrIqomah = it },
                                    label = { Text("Countdown Pra-Sholat (Mnt)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = hrSholat,
                                    onValueChange = { hrSholat = it },
                                    label = { Text("Durasi Sholat/Khutbah (Mnt)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val defaultNotes = when (selectedWeek) {
                            6 -> "Jadwal Petugas Sholat Idul Fitri"
                            7 -> "Jadwal Petugas Sholat Idul Adha"
                            else -> "Jadwal Petugas Sholat Jum'at Pekan Ke-$selectedWeek"
                        }
                        val updated = currentSchedule.copy(
                            id = selectedWeek.toLong(),
                            date = if (currentSchedule.date.isNotBlank() && !currentSchedule.date.startsWith("Jum'at Ke-")) currentSchedule.date else defaultDate,
                            hijriDate = if (currentSchedule.hijriDate.isNotBlank() && !currentSchedule.hijriDate.startsWith("Jum'at Ke-")) currentSchedule.hijriDate else defaultHijri,
                            khotib = khotib,
                            imam = imam,
                            muadzin = muadzin,
                            bilal = bilal,
                            khutbahTopic = khutbahTopic,
                            notes = if (currentSchedule.notes.isNotBlank()) currentSchedule.notes else defaultNotes
                        )
                        onSaveFriday(updated)

                        if (config != null && onSaveConfig != null) {
                            if (isIdulFitri) {
                                onSaveConfig(
                                    config.copy(
                                        idulFitriEnabled = hrEnabled,
                                        idulFitriDate = hrDate,
                                        idulFitriTime = hrTime,
                                        idulFitriIqomahMinutes = hrIqomah.toIntOrNull() ?: config.idulFitriIqomahMinutes,
                                        idulFitriSholatMinutes = hrSholat.toIntOrNull() ?: config.idulFitriSholatMinutes
                                    )
                                )
                            } else if (isIdulAdha) {
                                onSaveConfig(
                                    config.copy(
                                        idulAdhaEnabled = hrEnabled,
                                        idulAdhaDate = hrDate,
                                        idulAdhaTime = hrTime,
                                        idulAdhaIqomahMinutes = hrIqomah.toIntOrNull() ?: config.idulAdhaIqomahMinutes,
                                        idulAdhaSholatMinutes = hrSholat.toIntOrNull() ?: config.idulAdhaSholatMinutes
                                    )
                                )
                            }
                        }

                        savedToast = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "💾 Simpan Jadwal Petugas $eventName",
                        color = IslamicEmeraldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (savedToast) {
                    Text("✅ Jadwal Petugas $eventName Tersimpan!", color = Color(0xFF81C784), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TabFinanceSettings(
    finances: List<FinanceTransaction>,
    onAdd: (String, Long, TransactionType, String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Catat Kas Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Keterangan", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Nominal Rp", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { isExpense = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isExpense) SuccessGreen else Color(0x33FFFFFF)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) { Text("Pemasukan", fontSize = 10.sp) }
                Button(
                    onClick = { isExpense = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isExpense) ExpenseRed else Color(0x33FFFFFF)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) { Text("Pengeluaran", fontSize = 10.sp) }
            }
            Button(
                onClick = {
                    val amt = amount.toLongOrNull() ?: 0L
                    if (title.isNotBlank() && amt > 0) {
                        onAdd(title, amt, if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME, "Kas", "Hari Ini", "")
                        title = ""
                        amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("➕ Tambahkan", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(finances) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33000000))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(f.title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Rp ${f.amount} • ${f.type.name}", fontSize = 9.sp, color = if (f.type == TransactionType.INCOME) Color(0xFF81C784) else Color(0xFFEF9A9A))
                    }
                    IconButton(onClick = { onDelete(f.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabActivitiesSettings(
    activities: List<MosqueActivity>,
    onAdd: (String, String, String, String, String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Tambah Agenda Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Acara", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = speaker, onValueChange = { speaker = it }, label = { Text("Penceramah / Ustadz", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Waktu (Contoh: Ahad 05.30)", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title, speaker, "Rutin", time, "Masjid", "", "Kajian")
                        title = ""
                        speaker = ""
                        time = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("➕ Tambah Agenda", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(activities) { a ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33000000))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(a.title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${a.speaker} • ${a.time}", fontSize = 9.sp, color = IslamicGoldLight)
                    }
                    IconButton(onClick = { onDelete(a.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRunningTextSettings(
    runningTexts: List<RunningTextItem>,
    onAdd: (String) -> Unit,
    onUpdate: (Long, String, Boolean, Int) -> Unit,
    onDelete: (Long) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<RunningTextItem?>(null) }
    var savedToast by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pengumuman / Teks Berjalan (Running Text)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
            if (editingItem != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, SleekAmber400, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("✏️ Mode Edit (ID: ${editingItem?.id})", fontSize = 10.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Input Form
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(if (editingItem != null) "Edit isi teks pengumuman" else "Teks pengumuman / hadits baru", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            if (editingItem != null) {
                Button(
                    onClick = {
                        val current = editingItem
                        if (current != null && textInput.isNotBlank()) {
                            onUpdate(current.id, textInput, current.isActive, current.order)
                            editingItem = null
                            textInput = ""
                            savedToast = "Perubahan Teks Berhasil Disimpan!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("💾 Simpan", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        editingItem = null
                        textInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("✕ Batal", color = Color.White, fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAdd(textInput)
                            textInput = ""
                            savedToast = "Teks Berjalan Berhasil Ditambahkan!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("➕ Tambah", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (savedToast != null) {
            Text("✅ $savedToast", color = Color(0xFF81C784), fontSize = 11.sp)
        }

        Text("Daftar Teks Berjalan di Layar TV:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldLight)

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(runningTexts) { r ->
                val isBeingEdited = editingItem?.id == r.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBeingEdited) Color(0x33F59E0B) else Color(0x33000000))
                        .border(1.dp, if (isBeingEdited) SleekAmber400 else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = r.text,
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Edit Button (Pencil Icon)
                        IconButton(
                            onClick = {
                                editingItem = r
                                textInput = r.text
                                savedToast = null
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3310B981))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Teks", tint = Color(0xFFA7F3D0), modifier = Modifier.size(15.dp))
                        }

                        // Delete Button (Trash Icon)
                        IconButton(
                            onClick = {
                                onDelete(r.id)
                                if (editingItem?.id == r.id) {
                                    editingItem = null
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33EF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Teks", tint = ExpenseRed, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabMurottalSettings(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    subuh: Boolean,
    onSubuhChange: (Boolean) -> Unit,
    dzuhur: Boolean,
    onDzuhurChange: (Boolean) -> Unit,
    ashar: Boolean,
    onAsharChange: (Boolean) -> Unit,
    maghrib: Boolean,
    onMaghribChange: (Boolean) -> Unit,
    isya: Boolean,
    onIsyaChange: (Boolean) -> Unit,
    jumat: Boolean,
    onJumatChange: (Boolean) -> Unit,
    durationStr: String,
    onDurationChange: (String) -> Unit,
    volumeStr: String,
    onVolumeChange: (String) -> Unit,
    selectedPresetId: String,
    onPresetChange: (String) -> Unit,
    sourceType: String,
    onSourceTypeChange: (String) -> Unit,
    onSave: () -> Unit,
    onPlayPreset: (String) -> Unit,
    onStopPlayer: () -> Unit
) {
    var savedToast by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎧 Pengaturan Murottal & Tarhim Otomatis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldLight)
            Button(
                onClick = {
                    onSave()
                    savedToast = "Pengaturan Murottal Berhasil Disimpan!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("💾 Simpan", color = IslamicEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (savedToast != null) {
            Text("✅ $savedToast", color = Color(0xFF81C784), fontSize = 11.sp)
        }

        // Master Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEnabledChange(!enabled) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = CheckboxDefaults.colors(checkedColor = IslamicGoldPrimary, checkmarkColor = IslamicEmeraldDark)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Aktifkan Murottal Otomatis Sebelum Sholat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Audio akan otomatis berhenti saat masuk adzan", color = Color(0xFFA7F3D0), fontSize = 10.sp)
                }
            }
        }

        // Prayer Time Checkboxes
        Text("Pilih Waktu Sholat yang Memutar Murottal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrayerToggleBox("🌅 Subuh", subuh, onSubuhChange, modifier = Modifier.weight(1f))
            PrayerToggleBox("☀️ Dzuhur", dzuhur, onDzuhurChange, modifier = Modifier.weight(1f))
            PrayerToggleBox("⛅ Ashar", ashar, onAsharChange, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrayerToggleBox("🌇 Maghrib", maghrib, onMaghribChange, modifier = Modifier.weight(1f))
            PrayerToggleBox("🌌 Isya'", isya, onIsyaChange, modifier = Modifier.weight(1f))
            PrayerToggleBox("🕌 Jum'at", jumat, onJumatChange, modifier = Modifier.weight(1f))
        }

        // Duration & Volume Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("⏱️ Menit Sebelum Adzan:", color = Color.White, fontSize = 11.sp)
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = onDurationChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                    placeholder = { Text("Contoh: 10", color = Color.Gray, fontSize = 11.sp) },
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("🔊 Volume Suara (%):", color = Color.White, fontSize = 11.sp)
                OutlinedTextField(
                    value = volumeStr,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                    placeholder = { Text("Contoh: 80", color = Color.Gray, fontSize = 11.sp) },
                    singleLine = true
                )
            }
        }

        // Preset Qari Selection
        Text("Pilihan Qari / Surah Preset:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
        val presets = MurottalPresetCatalogue.list
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.forEach { preset ->
                val isSelected = selectedPresetId == preset.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0x3310B981) else Color(0x1AFFFFFF))
                        .border(1.dp, if (isSelected) Color(0xFF10B981) else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { onPresetChange(preset.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${preset.surahName} - ${preset.qariName}",
                            color = if (isSelected) Color(0xFF6EE7B7) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${preset.durationText} • ${preset.description}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 9.5.sp
                        )
                    }
                    if (isSelected) {
                        Text("✅ TERPILIH", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        // Test Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onPlayPreset(selectedPresetId) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("▶️ Tes Putar di TV", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStopPlayer,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text("⏹️ Hentikan Suara", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrayerToggleBox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) Color(0x3310B981) else Color(0x1A000000))
            .border(1.dp, if (checked) Color(0xFF10B981) else Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981), checkmarkColor = Color.White)
        )
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TabYouTubeLiveSettings(
    url: String,
    onUrlChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    showSlide: Boolean,
    onShowSlideChange: (Boolean) -> Unit,
    muted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var savedToast by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔴 Live Streaming YouTube", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldLight)
            Button(
                onClick = {
                    onSave()
                    savedToast = "Pengaturan YouTube Live Berhasil Disimpan!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("💾 Simpan", color = IslamicEmeraldDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (savedToast != null) {
            Text(savedToast!!, color = IslamicGoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (enabled) Color(0x33DC2626) else Color(0x22FFFFFF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) Color(0xFFEF4444) else Color(0x33FFFFFF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (enabled) Color(0xFFEF4444) else Color.Gray)
                    )
                    Text(
                        text = if (enabled) "🔴 LIVE STREAMING AKTIF DI TV" else "⚪ STANDBY (NONAKTIF)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = if (title.isNotBlank()) title else "Live Streaming Masjid",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Quick Presets
        Text("Pilihan Preset Cepat (1-Klik):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val p = YouTubeLiveHelper.PRESETS.find { it.id == "makkah_live" }
                    if (p != null) {
                        onUrlChange(p.url)
                        onTitleChange(p.name)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3310B981)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🕋 Makkah Live", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val p = YouTubeLiveHelper.PRESETS.find { it.id == "madinah_live" }
                    if (p != null) {
                        onUrlChange(p.url)
                        onTitleChange(p.name)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3310B981)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🕌 Madinah Live", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // URL Input
        Text("Tautan / URL YouTube Live:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.Gray, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IslamicGoldPrimary,
                unfocusedBorderColor = Color(0x44FFFFFF)
            )
        )

        // Title Input
        Text("Judul Siaran / Nama Kajian:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Kajian Rutin / Sholat Tarawih Live", color = Color.Gray, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IslamicGoldPrimary,
                unfocusedBorderColor = Color(0x44FFFFFF)
            )
        )

        // Toggle Fullscreen TV
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) Color(0x33DC2626) else Color(0x1AFFFFFF))
                .clickable { onEnabledChange(!enabled) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🔴 Tampilkan Siaran di TV Sekarang (Layar Utama)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFDC2626))
            )
        }

        // Toggle Mute Audio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (muted) Color(0x33F59E0B) else Color(0x1AFFFFFF))
                .clickable { onMutedChange(!muted) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (muted) "🔇 Audio Dibisukan (Muted / Hening)" else "🔊 Audio Aktif (Bersuara di TV)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = if (muted) "Siaran live diputar tanpa suara di speaker TV" else "Suara video live terdengar di speaker TV",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp
                )
            }
            Switch(
                checked = muted,
                onCheckedChange = onMutedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
            )
        }

        // Toggle Carousel Slide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (showSlide) Color(0x3310B981) else Color(0x1AFFFFFF))
                .clickable { onShowSlideChange(!showSlide) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎞️ Tampilkan Juga Sebagai Slide Carousel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Switch(
                checked = showSlide,
                onCheckedChange = onShowSlideChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
            )
        }
    }
}

@Composable
private fun TabTarawihSettings(
    config: MosqueConfig,
    tarawihEnabled: Boolean,
    onTarawihEnabledChange: (Boolean) -> Unit,
    tarawihAutoDetectNight: Boolean,
    onTarawihAutoDetectChange: (Boolean) -> Unit,
    tarawihManualNight: String,
    onTarawihManualNightChange: (String) -> Unit,
    tarawihShowSlide: Boolean,
    onTarawihShowSlideChange: (Boolean) -> Unit,
    tarawihKultumMinutes: String,
    onTarawihKultumMinutesChange: (String) -> Unit,
    tarawihSholatMinutes: String,
    onTarawihSholatMinutesChange: (String) -> Unit,
    tarawihBgPreset: String,
    onTarawihBgPresetChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var savedToast by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌙 Tarawih Ramadhan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IslamicGoldLight)
            Button(
                onClick = {
                    onSave()
                    savedToast = "Pengaturan Tarawih Ramadhan Berhasil Disimpan!"
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("💾 Simpan", color = IslamicEmeraldDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (savedToast != null) {
            Text(savedToast!!, color = IslamicGoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (tarawihEnabled) Color(0x3310B981) else Color(0x22FFFFFF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (tarawihEnabled) Color(0xFF10B981) else Color(0x33FFFFFF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (tarawihEnabled) Color(0xFF10B981) else Color.Gray)
                    )
                    Text(
                        text = if (tarawihEnabled) "🟢 FITUR TARAWIH RAMADHAN AKTIF" else "⚪ FITUR TARAWIH NONAKTIF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "Mode: " + if (tarawihAutoDetectNight) "Otomatis Kalender Hijriyah" else "Manual Malam ke-$tarawihManualNight",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifkan Fitur Tarawih Ramadhan", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = tarawihEnabled,
                        onCheckedChange = onTarawihEnabledChange,
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary, checkedTrackColor = IslamicEmeraldDark)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Otomatis Deteksi Malam Tarawih (Hijriyah)", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = tarawihAutoDetectNight,
                        onCheckedChange = onTarawihAutoDetectChange,
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary, checkedTrackColor = IslamicEmeraldDark)
                    )
                }

                if (!tarawihAutoDetectNight) {
                    OutlinedTextField(
                        value = tarawihManualNight,
                        onValueChange = onTarawihManualNightChange,
                        label = { Text("Malam Tarawih Manual (1 - 30)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IslamicGoldPrimary,
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Slide Petugas Tarawih di TV", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = tarawihShowSlide,
                        onCheckedChange = onTarawihShowSlideChange,
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary, checkedTrackColor = IslamicEmeraldDark)
                    )
                }
            }
        }

        // Durations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Durasi Tampilan Layar Tarawih:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IslamicGoldBright)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tarawihKultumMinutes,
                        onValueChange = onTarawihKultumMinutesChange,
                        label = { Text("Durasi Kultum (Menit)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IslamicGoldPrimary,
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tarawihSholatMinutes,
                        onValueChange = onTarawihSholatMinutesChange,
                        label = { Text("Durasi Sholat (Menit)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IslamicGoldPrimary,
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }
        }

        Text(
            "💡 Info: Untuk mengatur nama penceramah, imam, dan bilal per malam dari 30 malam Ramadhan, silakan gunakan Web Remote Admin melalui browser HP / Laptop Anda.",
            fontSize = 11.sp,
            color = IslamicGoldLight.copy(alpha = 0.8f),
            lineHeight = 16.sp
        )
    }
}
