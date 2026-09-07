package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File



private sealed interface CarouselSlideItem {
    object FinancialReport : CarouselSlideItem
    object FridayOfficers : CarouselSlideItem
    object MosqueActivities : CarouselSlideItem
    object DailyMaklumat : CarouselSlideItem
    object QrisDonation : CarouselSlideItem
    object DailyHadithSlide : CarouselSlideItem
    data class MediaImage(val slide: MediaSlide) : CarouselSlideItem
    data class YouTubeLive(val url: String, val title: String) : CarouselSlideItem
    data class LiveCctv(val url: String, val title: String) : CarouselSlideItem
}

@Composable
fun CarouselContainer(
    slideIndex: Int,
    onSelectSlide: (Int) -> Unit,
    nextPrayerInfo: NextPrayerInfo?,
    config: MosqueConfig,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    activities: List<MosqueActivity>,
    mediaSlides: List<MediaSlide> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (config.youtubeLiveEnabled && config.youtubeLiveUrl.isNotBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.dp, Color(0x4010B981), RoundedCornerShape(20.dp))
        ) {
            SlideYouTubeLive(
                url = config.youtubeLiveUrl,
                title = config.youtubeLiveTitle,
                config = config,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val isFridayToday = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }

    val activeSlides = remember(
        config.showFinancialReport,
        config.showFridayOfficers,
        config.showActivities,
        config.showDailyMaklumat,
        config.showQrisCard,
        config.showDailyHadith,
        config.showYoutubeLiveSlide,
        config.youtubeLiveUrl,
        config.youtubeLiveTitle,
        config.showCctvSlide,
        config.cctvStreamUrl,
        config.cctvStreamTitle,
        mediaSlides,
        isFridayToday
    ) {
        buildList<CarouselSlideItem> {
            if (config.showFinancialReport) add(CarouselSlideItem.FinancialReport)
            // Khusus Petugas Jum'at: Jika tidak dicentang, otomatis hanya aktif tampil pada hari Jum'at saja
            if (config.showFridayOfficers || isFridayToday) {
                add(CarouselSlideItem.FridayOfficers)
            }
            if (config.showActivities) add(CarouselSlideItem.MosqueActivities)
            if (config.showDailyMaklumat) add(CarouselSlideItem.DailyMaklumat)
            if (config.showQrisCard) add(CarouselSlideItem.QrisDonation)
            if (config.showDailyHadith) add(CarouselSlideItem.DailyHadithSlide)
            if (config.showYoutubeLiveSlide && config.youtubeLiveUrl.isNotBlank()) {
                add(CarouselSlideItem.YouTubeLive(config.youtubeLiveUrl, config.youtubeLiveTitle))
            }
            if (config.showCctvSlide && config.cctvStreamUrl.isNotBlank()) {
                add(CarouselSlideItem.LiveCctv(config.cctvStreamUrl, config.cctvStreamTitle))
            }
            mediaSlides.forEach { add(CarouselSlideItem.MediaImage(it)) }
        }
    }

    // Jika semua toggle dimatikan (activeSlides kosong), tampilkan MURNI background TV saja tanpa card apa pun!
    if (activeSlides.isEmpty()) {
        Spacer(modifier = modifier.fillMaxSize())
        return
    }

    val safeIndex = slideIndex % activeSlides.size
    val currentSlide = activeSlides[safeIndex]

    val cardAlpha = (1f - config.centerCardTransparency).coerceIn(0.15f, 0.92f)
    val cardBgColor = Color(0x02, 0x2C, 0x22, (cardAlpha * 255).toInt())

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBgColor)
            .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(22.dp))
            .padding(10.dp)
    ) {
        // Active Slide Content - Full Space
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = {
                fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() + slideOutHorizontally { -it / 4 }
            },
            label = "sleek_carousel_anim",
            modifier = Modifier.fillMaxSize()
        ) { target ->
            when (target) {
                is CarouselSlideItem.FinancialReport -> SlideFinancialReport(
                    finances = finances,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = balance,
                    config = config
                )
                is CarouselSlideItem.FridayOfficers -> SlideFridayOfficers(schedule = fridaySchedule, config = config)
                is CarouselSlideItem.MosqueActivities -> SlideMosqueActivities(activities = activities, config = config)
                is CarouselSlideItem.DailyMaklumat -> SlideDailyMaklumat(config = config)
                is CarouselSlideItem.QrisDonation -> SlideQrisDonation(config = config)
                is CarouselSlideItem.DailyHadithSlide -> SlideDailyHadith(config = config)
                is CarouselSlideItem.MediaImage -> SlideMediaImage(slide = target.slide)
                is CarouselSlideItem.YouTubeLive -> SlideYouTubeLive(url = target.url, title = target.title, config = config)
                is CarouselSlideItem.LiveCctv -> SlideLiveCctv(url = target.url, title = target.title, config = config)
            }
        }
    }
}

@Composable
private fun SlideMediaImage(slide: MediaSlide) {
    val bitmap = remember(slide.filePath) {
        try {
            val file = File(slide.filePath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = slide.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = TextWhiteDim,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (slide.title.isNotBlank()) slide.title else "Poster Slideshow",
                    fontSize = 13.sp,
                    color = TextWhiteDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ====================================================
// REUSABLE SLIDE CANVAS BACKGROUND (PNG/JPG PRESET OR UPLOAD)
// ====================================================
@Composable
private fun SlideCanvasBackground(
    customPath: String = "",
    defaultPreset: String = BackgroundPresetHelper.PRESET_EMERALD_MIHRAB,
    contentDescription: String = "Canvas"
) {
    val customBitmap = remember(customPath) {
        if (customPath.isNotBlank()) {
            try {
                val f = File(customPath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) { null }
        } else null
    }

    val resId = remember(defaultPreset) {
        BackgroundPresetHelper.getDrawableResId(defaultPreset) ?: R.drawable.bg_preset_emerald_mihrab
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }

    // Soft center contrast scrim: ensures text readability on any custom canvas/image
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    radius = 950f
                )
            )
    )
}

// ----------------------------------------------------
// SLIDE 1: PRAYER OVERVIEW & HERO COUNTDOWN (CANVAS OVERLAY)
// ----------------------------------------------------
@Composable
private fun SlidePrayerOverview(
    nextPrayerInfo: NextPrayerInfo?,
    config: MosqueConfig
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFinanceBgPath.ifBlank { config.customFridayOfficerBgPath },
            defaultPreset = BackgroundPresetHelper.PRESET_COUNTDOWN_PLAQUE,
            contentDescription = "Prayer Overview Canvas"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Column: Sleek Hero Countdown Card
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x26000000))
                    .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                // Top Right Pill "MENDATANG"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MENDATANG",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekAmber300,
                        letterSpacing = 1.5.sp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val prayerName = nextPrayerInfo?.prayer?.displayName ?: "SHOLAT"
                    val timeStr = nextPrayerInfo?.timeStr ?: "--:--"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "SHOLAT $prayerName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekEmerald300,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeStr,
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontFamily = RajdhaniFont,
                            letterSpacing = 1.sp
                        )
                    }

                    // Sleek Amber Countdown Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SleekAmber500)
                            .border(1.dp, SleekAmber100.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "●",
                            color = SleekEmerald950.copy(alpha = dotAlpha),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "MASUK WAKTU DALAM ${nextPrayerInfo?.formattedCountdown ?: "--:--:--"}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekEmerald950,
                            fontFamily = OrbitronFont,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Progress Bar Line
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x26FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.68f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SleekAmber500)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mari segerakan berwudhu dan mengisi shaf terdepan",
                            fontSize = 10.sp,
                            color = SleekEmerald200.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Right Column: Islamic Wisdom Quote & Infaq / QRIS Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sleek Wisdom Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x26000000))
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Hadits",
                            tint = SleekEmerald400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MUTIARA SUNNAH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekEmerald400,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "“Sholat berjamaah lebih utama dua puluh tujuh derajat dibandingkan sholat sendirian.”",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "HR. Bukhari & Muslim",
                        fontSize = 10.sp,
                        color = SleekEmerald300,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sleek Infaq Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x26000000))
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "INFAQ & WAKAF MASJID",
                            fontSize = 10.sp,
                            color = SleekEmerald400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = config.bankName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = config.bankAccount,
                            fontSize = 12.sp,
                            color = SleekAmber400,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = "Infaq",
                        tint = SleekAmber400,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

fun cleanOfficerName(name: String?, defaultName: String): String {
    if (name.isNullOrBlank()) return defaultName
    val trimmed = name.trim()
    val lower = trimmed.lowercase()
    if (trimmed == "-" || trimmed == "--" ||
        lower == "khotib" || lower == "khotib sholat" || lower == "khotib sholat jum'at" || lower == "khotib sholat jumat" ||
        lower == "khatib" || lower == "khatib sholat" || lower == "khatib sholat jum'at" || lower == "khatib sholat jumat" ||
        lower == "ustadz khotib" || lower == "ustadz khatib" || lower == "nama khotib" || lower == "nama khatib" ||
        lower == "imam" || lower == "imam sholat" || lower == "imam sholat jum'at" || lower == "ustadz imam" || lower == "nama imam" ||
        lower == "muadzin" || lower == "muadzin sholat" || lower == "ustadz muadzin" || lower == "nama muadzin" ||
        lower == "bilal" || lower == "bilal / muraqqi" || lower == "bilal muraqqi" || lower == "ustadz bilal" || lower == "nama bilal"
    ) {
        return defaultName
    }
    return trimmed
}

fun cleanFridayDate(dateStr: String?): String {
    val clean = dateStr?.trim().orEmpty()
    if (clean.isBlank() || clean.contains("Ke-", ignoreCase = true) || clean.contains("Ke ", ignoreCase = true) || clean.equals("Jum'at", ignoreCase = true) || clean.equals("Jumat", ignoreCase = true) || clean.equals("Jum'at Ini", ignoreCase = true) || clean.equals("Jum'at Barakah", ignoreCase = true)) {
        val targetCal = Calendar.getInstance()
        val currentDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK)
        val currentHour = targetCal.get(Calendar.HOUR_OF_DAY)
        var daysUntilFriday = (Calendar.FRIDAY - currentDayOfWeek + 7) % 7
        if (daysUntilFriday == 0 && currentHour >= 14) {
            daysUntilFriday = 7
        }
        targetCal.add(Calendar.DAY_OF_MONTH, daysUntilFriday)
        val monthNames = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        return "Jum'at, ${targetCal.get(Calendar.DAY_OF_MONTH)} ${monthNames[targetCal.get(Calendar.MONTH)]} ${targetCal.get(Calendar.YEAR)}"
    }
    return clean
}

// ----------------------------------------------------
// SLIDE 2: LAPORAN KEUANGAN KAS MASJID (MEWAH & LENGKAP)
// ----------------------------------------------------
@Composable
private fun SlideFinancialReport(
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    config: MosqueConfig
) {
    fun formatRupiah(amount: Long): String {
        return "Rp " + NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount)
    }

    val effectiveIncome = if (totalIncome > 0L) totalIncome else finances.filter { it.type == TransactionType.INCOME && it.amount > 0 }.sumOf { it.amount }
    val effectiveExpense = if (totalExpense > 0L) totalExpense else finances.filter { it.type == TransactionType.EXPENSE && it.amount > 0 }.sumOf { it.amount }
    val effectiveBalance = if (balance != 0L) balance else effectiveIncome - effectiveExpense

    val bankName = if (config.bankName.isNotBlank() && config.bankName != "-") config.bankName else "Bank Syariah Indonesia (BSI)"
    val bankAccount = if (config.bankAccount.isNotBlank() && config.bankAccount != "-") config.bankAccount else "7123-4567-8901"
    val bankAccountName = if (config.bankAccountName.isNotBlank() && config.bankAccountName != "-") config.bankAccountName else "DKM Baitul Muttaqin"

    val displayFinances = finances.filter { it.amount > 0 }

    val financePreset = when (config.financeBgPreset) {
        BackgroundPresetHelper.SAME_AS_MAIN -> config.mainScreenBgPreset
        else -> config.financeBgPreset
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFinanceBgPath,
            defaultPreset = financePreset.ifBlank { BackgroundPresetHelper.PRESET_WHITE_PEARL_GOLD },
            contentDescription = "Finance Canvas"
        )

        val finTitleColor = parseHexColor(config.financeTitleColor, SleekAmber400)
        val finBalanceColor = parseHexColor(config.financeBalanceColor, Color(0xFFFFE082))
        val finIncomeColor = parseHexColor(config.financeIncomeColor, Color(0xFF4ADE80))
        val finExpenseColor = parseHexColor(config.financeExpenseColor, Color(0xFFFB7185))
        val finFooterColor = parseHexColor(config.financeFooterColor, SleekAmber400)
        val finTitleText = config.financeTitleText.ifBlank { "LAPORAN KAS & KEUANGAN MASJID" }

        // Text directly on canvas (frameless, elegant, readable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER KANVAS
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("۞", fontSize = 18.sp, color = finTitleColor, fontWeight = FontWeight.Bold)
                    Text(
                        text = finTitleText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = finTitleColor,
                        letterSpacing = 1.5.sp
                    )
                    Text("۞", fontSize = 18.sp, color = finTitleColor, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Amanah Infaq, Shodaqoh & Donasi Ummat • ${config.mosqueName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, finTitleColor, Color.Transparent)
                            )
                        )
                )
            }

            // 2. HERO: SALDO KAS TERSEDIA (Frameless, Bold, Gold Shimmer)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TOTAL SALDO KAS SAAT INI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = SleekEmerald300,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiah(effectiveBalance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = finBalanceColor,
                    fontFamily = RajdhaniFont,
                    letterSpacing = 0.5.sp
                )
            }

            // 3. DUA KOLOM: PEMASUKAN & PENGELUARAN (Frameless with thin dividers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom Pemasukan
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🟢", fontSize = 13.sp)
                        Text(
                            text = "TOTAL PEMASUKAN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = finIncomeColor.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = formatRupiah(effectiveIncome),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = finIncomeColor,
                        fontFamily = RajdhaniFont,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Infaq Kotak, QRIS & Donatur",
                        fontSize = 10.5.sp,
                        color = Color(0xCCE2E8F0)
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(55.dp)
                        .background(Color(0x40FFFFFF))
                )

                // Kolom Pengeluaran
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔴", fontSize = 13.sp)
                        Text(
                            text = "TOTAL PENGELUARAN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = finExpenseColor.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = formatRupiah(effectiveExpense),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = finExpenseColor,
                        fontFamily = RajdhaniFont,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Operasional, Listrik & Perawatan",
                        fontSize = 10.5.sp,
                        color = Color(0xCCE2E8F0)
                    )
                }
            }

            // 4. FOOTER: REKENING BANK & DONASI QRIS (Frameless Bottom Bar)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(1.dp)
                        .background(Color(0x33FFFFFF))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏦", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "$bankName: $bankAccount",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = finFooterColor,
                                fontFamily = RajdhaniFont,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "a.n $bankAccountName",
                                fontSize = 10.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QRIS",
                            tint = SleekAmber400,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Infaq QRIS Tersedia",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekAmber300
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SLIDE 3: PETUGAS SHOLAT JUM'AT (LENGKAP & MAKSIMAL)
// ----------------------------------------------------
@Composable
private fun SlideFridayOfficers(
    schedule: FridaySchedule,
    config: MosqueConfig
) {
    val khotibName = cleanOfficerName(schedule.khotib, "Prof. Dr. KH. Ahmad Syakir, M.A.")
    val imamName = cleanOfficerName(schedule.imam, "Ustadz H. M. Firdaus Al-Hafidz")
    val muadzinName = cleanOfficerName(schedule.muadzin, "Ustadz Bilal Ramadhan")
    val bilalName = cleanOfficerName(schedule.bilal, "Akhi Muhammad Syahril")

    val fridayPreset = when (config.fridayOfficerBgPreset) {
        BackgroundPresetHelper.SAME_AS_MAIN -> config.mainScreenBgPreset
        else -> config.fridayOfficerBgPreset
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFridayOfficerBgPath,
            defaultPreset = fridayPreset.ifBlank { BackgroundPresetHelper.PRESET_EMERALD_MIHRAB },
            contentDescription = "Friday Officers Canvas"
        )

        val friTitleColor = parseHexColor(config.fridayTitleColor, SleekAmber400)
        val friTitleText = config.fridayTitleText.ifBlank { "JADWAL PETUGAS SHOLAT JUM'AT" }
        val friOfficerNameColor = parseHexColor(config.fridayOfficerNameColor, Color.White)
        val friOfficerLabelColor = parseHexColor(config.fridayOfficerLabelColor, Color(0xFF90E0EF))

        // Text directly on canvas (frameless, grand, high readability)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER KANVAS
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("۞", fontSize = 18.sp, color = friTitleColor, fontWeight = FontWeight.Bold)
                    Text(
                        text = friTitleText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = friTitleColor,
                        letterSpacing = 1.5.sp
                    )
                    Text("۞", fontSize = 18.sp, color = friTitleColor, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = schedule.date.ifBlank { "Jum'at Pekan Ini" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, friTitleColor, Color.Transparent)
                            )
                        )
                )
            }

            // 2. EMPAT PETUGAS UTAMA (2 Kolom Frameless Langsung di Atas Kanvas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom Kiri: Khatib & Imam
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Khatib
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🎙️", fontSize = 16.sp)
                            Text(
                                text = "KHATIB JUM'AT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = friOfficerLabelColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = khotibName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = friOfficerNameColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x3300E5FF))
                    )

                    // Imam
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🕌", fontSize = 16.sp)
                            Text(
                                text = "IMAM SHOLAT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = friOfficerLabelColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = imamName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = friOfficerNameColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Vertical Center Divider
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight(0.85f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x4DF59E0B), Color.Transparent)
                            )
                        )
                )

                // Kolom Kanan: Muadzin & Bilal
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Muadzin
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📢", fontSize = 16.sp)
                            Text(
                                text = "MUADZIN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = friOfficerLabelColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = muadzinName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = friOfficerNameColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x33FBBF24))
                    )

                    // Bilal
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📿", fontSize = 16.sp)
                            Text(
                                text = "BILAL / MURAQQI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = friOfficerLabelColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = bilalName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = friOfficerNameColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. FOOTER
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(1.dp)
                    .background(Color(0x26FFFFFF))
            )
            Text(
                text = "Mari luruskan dan rapatkan shaf demi kesempurnaan sholat berjamaah.",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xCCFDE68A)
            )
        }
    }
}

// ----------------------------------------------------
// SLIDE 4: AGENDA & KEGIATAN MASJID
// ----------------------------------------------------
@Composable
private fun SlideMosqueActivities(
    activities: List<MosqueActivity>,
    config: MosqueConfig
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFinanceBgPath.ifBlank { config.customFridayOfficerBgPath },
            defaultPreset = BackgroundPresetHelper.PRESET_MIDNIGHT_LANTERNS,
            contentDescription = "Activities Canvas"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("۞", fontSize = 18.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
                    Text(
                        text = "AGENDA & KAJIAN ILMU MASJID",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekAmber400,
                        letterSpacing = 1.5.sp
                    )
                    Text("۞", fontSize = 18.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Terbuka Untuk Seluruh Jama'ah Kaum Muslimin & Muslimat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, SleekAmber400, Color.Transparent)))
                )
            }

            if (activities.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada agenda kegiatan terdaftar pekan ini.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    activities.take(3).forEach { act ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x26000000))
                                .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SleekAmber500)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(act.category, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF022018))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = act.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = act.speaker,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🗓️ ${act.date}", fontSize = 11.sp, color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold)
                                Text("⏰ ${act.time}", fontSize = 12.sp, color = SleekAmber400, fontWeight = FontWeight.Black)
                                Text("📍 ${act.location}", fontSize = 10.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Footer
            Text(
                text = "${config.mosqueName} • Menuntut ilmu adalah kewajiban bagi setiap muslim.",
                fontSize = 11.5.sp,
                color = Color(0xCCFDE68A)
            )
        }
    }
}

// ----------------------------------------------------
// SLIDE 4: MAKLUMAT & ADAB MASJID (PENGGANTI COUNTDOWN)
// ----------------------------------------------------
@Composable
private fun SlideDailyMaklumat(
    config: MosqueConfig
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFridayOfficerBgPath.ifBlank { config.customFinanceBgPath },
            defaultPreset = BackgroundPresetHelper.PRESET_OTTOMAN_SAPPHIRE,
            contentDescription = "Maklumat Canvas"
        )

        val makLeftTitleColor = parseHexColor(config.maklumatTitleLeftColor, Color(0xFF90E0EF))
        val makLeftTitle = config.maklumatTitleLeft.ifBlank { "MUTIARA HIKMAH" }
        val makLeftTextColor = parseHexColor(config.maklumatTextLeftColor, Color.White)
        val makLeftText = config.maklumatTextLeft.ifBlank { "“Barangsiapa membangun masjid karena Allah walaupun hanya sebesar sarang burung, niscaya Allah akan membangunkan untuknya sebuah istana di surga.”" }
        val makRightTitleColor = parseHexColor(config.maklumatTitleRightColor, Color(0xFFFDE68A))
        val makRightTitle = config.maklumatTitleRight.ifBlank { "HIMBAUAN & ADAB JAMA'AH" }
        val makPointsColor = parseHexColor(config.maklumatPointsColor, Color.White)
        val point1 = config.maklumatPoint1.ifBlank { "Mohon nonaktifkan nada dering handphone (HP) di dalam masjid." }
        val point2 = config.maklumatPoint2.ifBlank { "Luruskan dan rapatkan shaf sebelum memulai sholat." }
        val point3 = config.maklumatPoint3.ifBlank { "Jagalah kebersihan dan kesucian masjid kita bersama." }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("۞", fontSize = 18.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
                    Text(
                        text = "MAKLUMAT & ADAB MASJID",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekAmber400,
                        letterSpacing = 1.5.sp
                    )
                    Text("۞", fontSize = 18.sp, color = SleekAmber400, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${config.mosqueName} • Kenyamanan & Ketertiban Beribadah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, SleekAmber400, Color.Transparent)))
                )
            }

            // Two Frameless Columns: Hadits Pilihan (Kiri) & Tata Tertib Jama'ah (Kanan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolom Kiri: Mutiara Hadits
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📖", fontSize = 16.sp)
                        Text(
                            text = makLeftTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = makLeftTitleColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = makLeftText,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = makLeftTextColor,
                        lineHeight = 22.sp
                    )

                    Text(
                        text = "— HR. Ibnu Majah & Ahmad",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekAmber400
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight(0.85f)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x4DF59E0B), Color.Transparent)))
                )

                // Kolom Kanan: Adab Masjid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📢", fontSize = 16.sp)
                        Text(
                            text = makRightTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = makRightTitleColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔇", fontSize = 16.sp)
                            Text(point1, fontSize = 13.sp, color = makPointsColor, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🚶‍♂️", fontSize = 16.sp)
                            Text(point2, fontSize = 13.sp, color = makPointsColor, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🧼", fontSize = 16.sp)
                            Text(point3, fontSize = 13.sp, color = makPointsColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Footer
            Text(
                text = "Terima kasih atas kepedulian Anda dalam menjaga ketenangan ibadah jama'ah.",
                fontSize = 11.5.sp,
                color = Color(0xCCFDE68A)
            )
        }
    }
}

// ----------------------------------------------------
// MODEL 2: SPLIT DASHBOARD VIEW (KEUANGAN & JUM'AT BERSAMAAN)
// ----------------------------------------------------
@Composable
fun SplitDashboardView(
    nextPrayerInfo: NextPrayerInfo?,
    config: MosqueConfig,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    modifier: Modifier = Modifier
) {
    fun formatRupiah(amount: Long): String {
        return "Rp " + NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount)
    }

    val effectiveIncome = if (totalIncome > 0L) totalIncome else finances.filter { it.type == TransactionType.INCOME && it.amount > 0 }.sumOf { it.amount }
    val effectiveExpense = if (totalExpense > 0L) totalExpense else finances.filter { it.type == TransactionType.EXPENSE && it.amount > 0 }.sumOf { it.amount }
    val effectiveBalance = if (balance != 0L) balance else effectiveIncome - effectiveExpense

    val khotibName = cleanOfficerName(fridaySchedule.khotib, "Prof. Dr. KH. Ahmad Syakir, M.A.")
    val imamName = cleanOfficerName(fridaySchedule.imam, "Ustadz H. M. Firdaus Al-Hafidz")
    val muadzinName = cleanOfficerName(fridaySchedule.muadzin, "Ustadz Bilal Ramadhan")
    val bilalName = cleanOfficerName(fridaySchedule.bilal, "Akhi Muhammad Syahril")
    
    val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val showFriday = config.showFridayOfficers || isFriday
    val showFinance = config.showFinancialReport

    if (!showFinance && !showFriday) {
        SlideDailyMaklumat(config = config)
        return
    }

    val finTitleColor = parseHexColor(config.financeTitleColor, SleekAmber400)
    val finBalanceColor = parseHexColor(config.financeBalanceColor, Color(0xFFFFE082))
    val finIncomeColor = parseHexColor(config.financeIncomeColor, Color(0xFF4ADE80))
    val finExpenseColor = parseHexColor(config.financeExpenseColor, Color(0xFFFB7185))
    val finFooterColor = parseHexColor(config.financeFooterColor, SleekAmber300)
    val finTitleText = config.financeTitleText.ifBlank { "KAS KEUANGAN MASJID" }

    val friTitleColor = parseHexColor(config.fridayTitleColor, SleekAmber400)
    val friTitleText = config.fridayTitleText.ifBlank { "PETUGAS SHOLAT JUM'AT" }
    val friOfficerNameColor = parseHexColor(config.fridayOfficerNameColor, Color.White)
    val friOfficerLabelColor = parseHexColor(config.fridayOfficerLabelColor, Color(0xFF90E0EF))

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showFinance) {
            // Left Card: Kas Keuangan Masjid on Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                SlideCanvasBackground(
                    customPath = config.customFinanceBgPath,
                    defaultPreset = BackgroundPresetHelper.PRESET_WHITE_PEARL_GOLD,
                    contentDescription = "Split Finance Canvas"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Kas", tint = finTitleColor, modifier = Modifier.size(18.dp))
                        Text(finTitleText, fontSize = 12.sp, fontWeight = FontWeight.Black, color = finTitleColor, letterSpacing = 0.5.sp)
                    }

                    // Saldo Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x26000000))
                            .border(1.dp, Color(0x4DFBBF24), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Saldo Kas Tersedia", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                        Text(
                            text = formatRupiah(effectiveBalance),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = finBalanceColor,
                            fontFamily = RajdhaniFont,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // In & Out stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22000000))
                                .border(1.dp, Color(0x3334D399), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Pemasukan", fontSize = 9.5.sp, color = finIncomeColor.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                            Text(
                                text = formatRupiah(effectiveIncome),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = finIncomeColor,
                                fontFamily = RajdhaniFont,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22000000))
                                .border(1.dp, Color(0x33FB7185), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Pengeluaran", fontSize = 9.5.sp, color = finExpenseColor.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                            Text(
                                text = formatRupiah(effectiveExpense),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = finExpenseColor,
                                fontFamily = RajdhaniFont,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Recent Transactions List (3 items)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Transaksi Terakhir:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFDE68A))
                        val recent = finances.take(3)
                        if (recent.isEmpty()) {
                            Text("Belum ada mutasi kas terbaru", fontSize = 8.5.sp, color = TextWhiteDim)
                        } else {
                            recent.forEach { item ->
                                val isInc = item.type == TransactionType.INCOME
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x1F000000))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${if (isInc) "+" else "-"} ${formatRupiah(item.amount)}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInc) finIncomeColor else finExpenseColor,
                                        fontFamily = RajdhaniFont
                                    )
                                }
                            }
                        }
                    }

                    // Rekening Info Mini Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${config.bankName.ifBlank { "BSI" }} ${config.bankAccount.ifBlank { "7123-4567-8901" }}", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = finFooterColor
                            )
                            Text(
                                text = "a.n ${config.bankAccountName.ifBlank { "DKM Masjid" }}", 
                                fontSize = 8.5.sp, 
                                color = Color.White
                            )
                        }
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QRIS", tint = SleekAmber400, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        if (showFriday) {
            // Right Card: Petugas Sholat Jum'at on Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                SlideCanvasBackground(
                    customPath = config.customFridayOfficerBgPath,
                    defaultPreset = BackgroundPresetHelper.PRESET_EMERALD_MIHRAB,
                    contentDescription = "Split Friday Canvas"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = "Jumat", tint = friTitleColor, modifier = Modifier.size(18.dp))
                            Text(
                                text = friTitleText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = friTitleColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = fridaySchedule.date.ifBlank { "Jum'at Ini" },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A)
                        )
                    }

                    // 2 Cards Layout - Khatib+Imam dan Muadzin+Bilal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Khatib & Imam
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x26000000))
                                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Khatib
                            Column {
                                Text("🎙️ KHATIB", fontSize = 10.sp, fontWeight = FontWeight.Black, color = friOfficerLabelColor, letterSpacing = 0.3.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = khotibName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = friOfficerNameColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // Imam
                            Column {
                                Text("🕌 IMAM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = friOfficerLabelColor, letterSpacing = 0.3.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = imamName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = friOfficerNameColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Card 2: Muadzin & Bilal
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x26000000))
                                .border(1.dp, Color(0x33FBBF24), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Muadzin
                            Column {
                                Text("📢 MUADZIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = friOfficerLabelColor, letterSpacing = 0.3.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = muadzinName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = friOfficerNameColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // Bilal
                            Column {
                                Text("📜 BILAL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = friOfficerLabelColor, letterSpacing = 0.3.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bilalName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = friOfficerNameColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SLIDE: MUTIARA HIKMAH & HADITS HARI INI
// =========================================================================
@Composable
private fun SlideDailyHadith(config: MosqueConfig) {
    val hadiths = DefaultHadiths.list
    val currentHadith = remember {
        val index = ((System.currentTimeMillis() / (1000 * 60 * 15)) % hadiths.size).toInt()
        hadiths[index.coerceIn(0, hadiths.size - 1)]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFridayOfficerBgPath.ifBlank { config.customFinanceBgPath },
            defaultPreset = BackgroundPresetHelper.PRESET_MIDNIGHT_KISWAH,
            contentDescription = "Hadith Canvas"
        )

        val hadithTitleColor = parseHexColor(config.hadithTitleColor, SleekAmber400)
        val hadithTitleText = config.hadithTitleText.ifBlank { "MUTIARA HADITS SHAHIH" }
        val hadithThemeColor = parseHexColor(config.hadithThemeColor, Color(0xFFFDE68A))
        val hadithThemeText = config.hadithThemeText.ifBlank { currentHadith.topic }
        val hadithArabicColor = parseHexColor(config.hadithArabicColor, Color(0xFFFFE082))
        val hadithArabicText = config.hadithArabicText.ifBlank { currentHadith.arabic }
        val hadithTranslationColor = parseHexColor(config.hadithTranslationColor, Color.White)
        val hadithTranslationText = config.hadithTranslationText.ifBlank { currentHadith.translation }
        val hadithNarratorColor = parseHexColor(config.hadithNarratorColor, SleekAmber300)
        val hadithNarratorText = config.hadithNarratorText.ifBlank { currentHadith.narrator }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("۞", fontSize = 18.sp, color = hadithTitleColor, fontWeight = FontWeight.Bold)
                    Text(
                        text = hadithTitleText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = hadithTitleColor,
                        letterSpacing = 1.5.sp
                    )
                    Text("۞", fontSize = 18.sp, color = hadithTitleColor, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Tema: ${hadithThemeText.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = hadithThemeColor
                )
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, hadithTitleColor, Color.Transparent)))
                )
            }

            // Teks Arab Berharakat
            Text(
                text = hadithArabicText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = hadithArabicColor,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 4.dp)
            )

            // Terjemahan Bahasa Indonesia
            Text(
                text = "\"$hadithTranslationText\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = hadithTranslationColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth(0.90f)
            )

            // Perawi Hadits
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("📖", fontSize = 14.sp)
                Text(
                    text = "Hadits Riwayat: $hadithNarratorText",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = hadithNarratorColor
                )
            }
        }
    }
}

// =========================================================================
// SLIDE: QRIS INFAQ DIGITAL & PROGRAM MASJID
// =========================================================================
private fun formatRupiahCarousel(amount: Long): String {
    return try {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.maximumFractionDigits = 0
        format.format(amount).replace(",00", "").replace("Rp", "Rp ")
    } catch (_: Exception) {
        "Rp $amount"
    }
}

@Composable
private fun SlideQrisDonation(config: MosqueConfig) {
    val qrisBitmap = remember(config.qrisImagePath) {
        if (config.qrisImagePath.isNotBlank()) {
            try {
                val f = File(config.qrisImagePath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val collected = config.donationCollectedAmount
    val target = config.donationTargetAmount.coerceAtLeast(1L)
    val progress = (collected.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val percentInt = (progress * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        SlideCanvasBackground(
            customPath = config.customFinanceBgPath,
            defaultPreset = BackgroundPresetHelper.PRESET_WHITE_PEARL_GOLD,
            contentDescription = "QRIS Canvas"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SISI KIRI: QRIS Code Box Putih Bersih
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrisBitmap != null) {
                    Image(
                        bitmap = qrisBitmap,
                        contentDescription = "QRIS",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(95.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "QRIS INFAQ RESMI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "BCA, Mandiri, BSI, GoPay, OVO, Dana",
                            fontSize = 7.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // SISI KANAN: Program Donasi & Progress Bar Mengapung di Atas Kanvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "📱 ${config.qrisLabel.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekAmber300,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = config.donationProgramTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Salurkan infaq terbaik Anda secara langsung dan penuh keberkahan.",
                        fontSize = 11.sp,
                        color = Color(0xFFFDE68A)
                    )
                }

                // Progress Bar Donasi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26000000))
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERKUMPUL: ${formatRupiahCarousel(collected)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE082)
                        )
                        Text(
                            text = "$percentInt%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4ADE80)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFFF59E0B))))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target Program: ${formatRupiahCarousel(target)}",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }

                // Footer Norek
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏦", fontSize = 16.sp)
                    Text(
                        text = "${config.bankName}: ${config.bankAccount} a.n ${config.bankAccountName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekAmber400
                    )
                }
            }
        }
    }
}

@Composable
fun SlideLiveCctv(
    url: String,
    title: String,
    config: MosqueConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B0F19))
            .border(1.dp, Color(0x33DC2626), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar inside slide
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B))))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "🔴 LIVE CCTV",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = title.ifBlank { "Live Kamera Masjid" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // CCTV Player Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RtspCctvPlayerView(
                    rtspUrl = url,
                    modifier = Modifier.fillMaxSize(),
                    isMuted = config.cctvStreamMuted
                )
            }
        }
    }
}

@Composable
fun SlideYouTubeLive(
    url: String,
    title: String,
    config: MosqueConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B0F19))
            .border(1.dp, Color(0x33DC2626), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar inside slide
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B))))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "🔴 LIVE STREAMING",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = title.ifBlank { "Siaran Langsung Masjid" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Video Player Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                YouTubeLivePlayerView(
                    youtubeUrl = url,
                    title = title,
                    isMuted = config.youtubeLiveMuted,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLivePlayerView(
    youtubeUrl: String,
    title: String = "Live Streaming",
    modifier: Modifier = Modifier,
    isMuted: Boolean = false
) {
    if (youtubeUrl.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090D16)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔴 SIARAN LIVE STREAMING YOUTUBE",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title.ifBlank { "Tautan YouTube Live belum diatur" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Buka Web Admin di HP (ip:8080) menu YouTube Live untuk memasukkan tautan kajian.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val htmlData = remember(youtubeUrl, isMuted) {
        YouTubeLiveHelper.buildPlayerHtml(youtubeUrl, isMuted)
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                try {
                    val cookieMgr = android.webkit.CookieManager.getInstance()
                    cookieMgr.setAcceptCookie(true)
                    cookieMgr.setAcceptThirdPartyCookies(this, true)
                } catch (_: Exception) {}

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        val lowerUrl = url.lowercase()

                        // Intercept & block Google / YouTube ad requests
                        val isAdUrl = lowerUrl.contains("googleads") ||
                                lowerUrl.contains("doubleclick.net") ||
                                lowerUrl.contains("/pagead/") ||
                                lowerUrl.contains("/api/stats/ads") ||
                                lowerUrl.contains("adservice.google") ||
                                lowerUrl.contains("googlesyndication.com") ||
                                lowerUrl.contains("static.doubleclick.net")

                        if (isAdUrl) {
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        // Auto retry after 6 seconds if network drops or YouTube stream reconnects
                        view?.postDelayed({
                            try {
                                view.loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    htmlData,
                                    "text/html",
                                    "UTF-8",
                                    "https://www.youtube.com"
                                )
                            } catch (_: Exception) {}
                        }, 6000)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                        return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                    }
                }

                tag = htmlData
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    htmlData,
                    "text/html",
                    "UTF-8",
                    "https://www.youtube.com"
                )
            }
        },
        update = { webView ->
            // Only reload when the video/HTML data actually changes, avoiding 1-second recomposition crash
            if (webView.tag != htmlData) {
                webView.tag = htmlData
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    htmlData,
                    "text/html",
                    "UTF-8",
                    "https://www.youtube.com"
                )
            }
        },
        onRelease = { webView ->
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (_: Exception) {}
        }
    )
}
