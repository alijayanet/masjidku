package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import java.io.File
import android.graphics.BitmapFactory
import java.util.Calendar
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.DrawScope

// =========================================================================
// MODEL 4: AL-AMIN CYBER NEO (Sidebar Kiri + Jam Analog + Foto Utama)
// =========================================================================
@Composable
fun LayoutSidebarAnalogNeo(
    config: MosqueConfig,
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    calendar: Calendar,
    currentTime: String,
    currentSeconds: String,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    activities: List<MosqueActivity>,
    mediaSlides: List<MediaSlide>,
    slideIndex: Int,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // LEFT COLUMN: Vertical Prayer Times Box (Dark Cyber Blue with Cyan Glow)
        Box(
            modifier = Modifier
                .width(185.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xE60A192F),
                            Color(0xF00D2238),
                            Color(0xE60A192F)
                        )
                    )
                )
                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isFriday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                val prayerList = buildList {
                    if (config.showImsak) add(Triple("IMSAK", schedule?.imsak ?: "--:--", PrayerName.IMSAK))
                    if (config.showSubuh) add(Triple("SUBUH", schedule?.subuh ?: "--:--", PrayerName.SUBUH))
                    if (config.showTerbit) add(Triple("TERBIT", schedule?.terbit ?: "--:--", PrayerName.TERBIT))
                    if (config.showDhuha) add(Triple("DHUHA", schedule?.dhuha ?: "--:--", PrayerName.DHUHA))
                    if (config.showDzuhur) add(Triple(if (isFriday) "JUM'AT" else "DZUHUR", schedule?.dzuhur ?: "--:--", PrayerName.DZUHUR))
                    if (config.showAshar) add(Triple("ASHAR", schedule?.ashar ?: "--:--", PrayerName.ASHAR))
                    if (config.showMaghrib) add(Triple("MAGHRIB", schedule?.maghrib ?: "--:--", PrayerName.MAGHRIB))
                    if (config.showIsya) add(Triple("ISYA'", schedule?.isya ?: "--:--", PrayerName.ISYA))
                }

                prayerList.forEach { (name, time, pName) ->
                    val isNext = nextPrayerInfo?.prayer == pName
                    val cardBg = if (isNext) {
                        Brush.horizontalGradient(listOf(Color(0xFF00B4D8), Color(0xFF0077B6)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0x33002845), Color(0x22002845)))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .border(
                                1.dp,
                                if (isNext) Color(0xFF90E0EF) else Color(0x2200E5FF),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNext) Color.White else Color(0xFF90E0EF),
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = time,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RajdhaniFont,
                            color = if (isNext) Color(0xFFFFF176) else Color(0xFFFFD54F),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // CENTER COLUMN: Analog Clock + Next Prayer Countdown
        Column(
            modifier = Modifier
                .width(185.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Analog Clock Card
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .border(2.dp, Color(0x4000E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnalogClock(
                    calendar = calendar,
                    modifier = Modifier.fillMaxSize(),
                    clockTheme = AnalogClockTheme.ROYAL_BLUE
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Next Prayer Countdown Card (Sleek Cyber Gold & Cyan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE60A192F), Color(0xF003182B))
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFF00E5FF))),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("●", fontSize = 7.sp, color = SleekAmber400)
                        Text(
                            text = "MENUJU ${(nextPrayerInfo?.prayer?.displayName ?: "SHOLAT").uppercase()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekAmber300,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = nextPrayerInfo?.formattedCountdown ?: "00:00:00",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFont,
                        color = Color(0xFFFFE082),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Waktu: ${nextPrayerInfo?.timeStr ?: "--:--"} WIB",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF90E0EF)
                    )
                }
            }
        }

        // RIGHT COLUMN: Main Photo & Slideshow Carousel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CarouselContainer(
                slideIndex = slideIndex,
                onSelectSlide = onSelectSlide,
                nextPrayerInfo = nextPrayerInfo,
                config = config,
                finances = finances,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                fridaySchedule = fridaySchedule,
                activities = activities,
                mediaSlides = mediaSlides
            )
        }
    }
}

// =========================================================================
// MODEL 5: AL-IKHSAN WAVE ELEGAN (Panel Wave Biru Kanan + Jam Analog)
// =========================================================================
@Composable
fun LayoutRightWaveAnalog(
    config: MosqueConfig,
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    calendar: Calendar,
    currentTime: String,
    currentSeconds: String,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    activities: List<MosqueActivity>,
    mediaSlides: List<MediaSlide>,
    slideIndex: Int,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // LEFT AREA: Big Visual Carousel / Slideshow & Mosque Brand Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CarouselContainer(
                slideIndex = slideIndex,
                onSelectSlide = onSelectSlide,
                nextPrayerInfo = nextPrayerInfo,
                config = config,
                finances = finances,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                fridaySchedule = fridaySchedule,
                activities = activities,
                mediaSlides = mediaSlides
            )
        }

        // RIGHT AREA: Wave Blue Curved Panel containing Analog Clock & Vertical Prayer Times
        Box(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF20B4870),
                            Color(0xF5042A42),
                            Color(0xF20B4870)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0x3348CAE4),
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val prayerItems = buildList {
                if (config.showImsak) add(Tuple4("Imsak", schedule?.imsak ?: "--:--", Icons.Default.HourglassBottom, PrayerName.IMSAK))
                if (config.showSubuh) add(Tuple4("Shubuh", schedule?.subuh ?: "--:--", Icons.Default.WbTwilight, PrayerName.SUBUH))
                if (config.showTerbit) add(Tuple4("Syuruq", schedule?.terbit ?: "--:--", Icons.Default.WbSunny, PrayerName.TERBIT))
                if (config.showDhuha) add(Tuple4("Dhuha", schedule?.dhuha ?: "--:--", Icons.Default.Brightness5, PrayerName.DHUHA))
                if (config.showDzuhur) add(Tuple4("Dzuhur", schedule?.dzuhur ?: "--:--", Icons.Default.LightMode, PrayerName.DZUHUR))
                if (config.showAshar) add(Tuple4("Ashar", schedule?.ashar ?: "--:--", Icons.Default.Cloud, PrayerName.ASHAR))
                if (config.showMaghrib) add(Tuple4("Maghrib", schedule?.maghrib ?: "--:--", Icons.Default.NightsStay, PrayerName.MAGHRIB))
                if (config.showIsya) add(Tuple4("Isya'", schedule?.isya ?: "--:--", Icons.Default.Bedtime, PrayerName.ISYA))
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Analog Clock on Blue Face (Compact 72dp so prayer list has full room)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x26000000)),
                    contentAlignment = Alignment.Center
                ) {
                    AnalogClock(
                        calendar = calendar,
                        modifier = Modifier.fillMaxSize(),
                        clockTheme = AnalogClockTheme.ROYAL_BLUE
                    )
                }

                // Vertical Prayer Times List with Icons (Clear & Never Clipped)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    prayerItems.forEach { (name, time, icon, pName) ->
                        val isNext = nextPrayerInfo?.prayer == pName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNext) Color(0x4000E5FF) else Color(0x14FFFFFF))
                                .border(0.5.dp, if (isNext) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isNext) Color(0xFFFFD54F) else Color(0xFF90CAF9),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isNext) Color.White else Color(0xFFE3F2FD)
                                )
                            }
                            Text(
                                text = time,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isNext) Color(0xFFFFEE58) else Color.White
                            )
                        }
                    }
                }

                // Mini Countdown Pill at Bottom of Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE6001B2E))
                        .padding(vertical = 5.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏱️ ${nextPrayerInfo?.prayer?.displayName ?: "Sholat"} -${nextPrayerInfo?.formattedCountdown ?: "00:00:00"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80),
                        fontFamily = OrbitronFont
                    )
                }
            }
        }
    }
}

// =========================================================================
// MODEL 6: WAKTIHA KLATEN (Jam Digital Besar Kiri + Petugas + 7 Bar Bawah)
// =========================================================================
@Composable
fun LayoutWaktihaClassic(
    config: MosqueConfig,
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    currentTime: String,
    currentSeconds: String,
    fridaySchedule: FridaySchedule,
    activities: List<MosqueActivity>,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    mediaSlides: List<MediaSlide>,
    slideIndex: Int,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP AREA: Left Information Column + Right Visual Card
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEFT SIDE: Countdown + Petugas Sholat Jumat (Jam Digital dihapus agar tidak redundan & data petugas maksimal)
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Countdown Badge Box (Waktu Menuju Sholat)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF065F46), Color(0xFF047857))
                            )
                        )
                        .border(1.dp, Color(0x4D34D399), RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "MENUJU ${(nextPrayerInfo?.prayer?.displayName ?: "SHOLAT").uppercase()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F3D0),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = nextPrayerInfo?.formattedCountdown ?: "00:00:00",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OrbitronFont,
                            color = Color(0xFFFFE082),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 2. Petugas Sholat Jum'at / Maklumat Card
                val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                val showFriday = config.showFridayOfficers || isFriday
                val showMaklumat = config.showDailyMaklumat

                if (showFriday || showMaklumat) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xF5FFFFFF))
                            .border(1.5.dp, Color(0x330D5C3A), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        if (showFriday) {
                        val khotibName = cleanOfficerName(fridaySchedule.khotib, "Prof. Dr. KH. Ahmad Syakir, M.A.")
                        val imamName = cleanOfficerName(fridaySchedule.imam, "Ustadz H. M. Firdaus Al-Hafidz")
                        val muadzinName = cleanOfficerName(fridaySchedule.muadzin, "Ustadz Bilal Ramadhan")
                        val bilalName = cleanOfficerName(fridaySchedule.bilal, "Akhi Muhammad Syahril")

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color(0xFF045435),
                                    modifier = Modifier.size(19.dp)
                                )
                                Text(
                                    text = "PETUGAS SHOLAT JUM'AT",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF045435),
                                    letterSpacing = 0.6.sp
                                )
                            }

                            // Khatib
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18045435))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🎙️ KHATIB", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF045435), letterSpacing = 0.4.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = khotibName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Imam
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18045435))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🕌 IMAM", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF045435), letterSpacing = 0.4.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = imamName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Muadzin & Bilal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x18045435))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text("📢 MUADZIN", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF045435), letterSpacing = 0.3.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = muadzinName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x18045435))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text("📜 BILAL", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF045435), letterSpacing = 0.3.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bilalName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        // Maklumat Masjid Harian
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF045435),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "MAKLUMAT MASJID",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF045435),
                                    letterSpacing = 0.6.sp
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18045435))
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = config.mosqueName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = config.tagline,
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 15.sp
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x10045435))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📌 Informasi",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF045435)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Jadwal Petugas Sholat Jum'at otomatis tampil setiap hari Jum'at.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            }

            // RIGHT SIDE: Main Slideshow & Visual Photo Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CarouselContainer(
                    slideIndex = slideIndex,
                    onSelectSlide = onSelectSlide,
                    nextPrayerInfo = nextPrayerInfo,
                    config = config,
                    finances = finances,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = balance,
                    fridaySchedule = fridaySchedule,
                    activities = activities,
                    mediaSlides = mediaSlides
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // BOTTOM AREA: 7 Rounded Green Cards for Prayer Times
        PrayerTimesBar(
            schedule = schedule,
            nextPrayerInfo = nextPrayerInfo,
            config = config
        )
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// =========================================================================
// MODEL 7: GRAND MIHRAB NABAWI (Kubah Kurung Kurawa Ganda + Royal Aesthetic 1:1)
// =========================================================================

class DoubleEndedKurungKurawaShape(
    private val archRatio: Float = 0.15f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val aH = (h * archRatio).coerceIn(16f, 30f)

            // Start at left shoulder (0, aH)
            moveTo(0f, aH)

            // 1. TOP OGEE ARCH: from (0, aH) to (w/2, 0) to (w, aH)
            cubicTo(
                w * 0.08f, aH * 0.58f,
                w * 0.22f, aH * 0.48f,
                w * 0.32f, aH * 0.28f
            )
            cubicTo(
                w * 0.40f, aH * 0.12f,
                w * 0.46f, aH * 0.02f,
                w * 0.50f, 0f
            )
            cubicTo(
                w * 0.54f, aH * 0.02f,
                w * 0.60f, aH * 0.12f,
                w * 0.68f, aH * 0.28f
            )
            cubicTo(
                w * 0.78f, aH * 0.48f,
                w * 0.92f, aH * 0.58f,
                w, aH
            )

            // Right vertical wall
            lineTo(w, h - aH)

            // 2. BOTTOM INVERTED OGEE ARCH: from (w, h - aH) to (w/2, h) to (0, h - aH)
            cubicTo(
                w * 0.92f, h - aH * 0.58f,
                w * 0.78f, h - aH * 0.48f,
                w * 0.68f, h - aH * 0.28f
            )
            cubicTo(
                w * 0.60f, h - aH * 0.12f,
                w * 0.54f, h - aH * 0.02f,
                w * 0.50f, h
            )
            cubicTo(
                w * 0.46f, h - aH * 0.02f,
                w * 0.40f, h - aH * 0.12f,
                w * 0.32f, h - aH * 0.28f
            )
            cubicTo(
                w * 0.22f, h - aH * 0.48f,
                w * 0.08f, h - aH * 0.58f,
                0f, h - aH
            )

            // Left vertical wall back to top shoulder
            lineTo(0f, aH)
            close()
        }
        return Outline.Generic(path)
    }
}

class KurungKurawaArchShape(
    private val cornerRadiusRatio: Float = 0.08f,
    private val archHeightRatio: Float = 0.22f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = (w * cornerRadiusRatio).coerceAtMost(10f)
            val aH = (h * archHeightRatio).coerceIn(24f, 36f)

            moveTo(0f, aH)
            cubicTo(
                w * 0.06f, aH * 0.60f,
                w * 0.18f, aH * 0.52f,
                w * 0.28f, aH * 0.34f
            )
            cubicTo(
                w * 0.38f, aH * 0.16f,
                w * 0.45f, aH * 0.03f,
                w * 0.50f, 0f
            )
            cubicTo(
                w * 0.55f, aH * 0.03f,
                w * 0.62f, aH * 0.16f,
                w * 0.72f, aH * 0.34f
            )
            cubicTo(
                w * 0.82f, aH * 0.52f,
                w * 0.94f, aH * 0.60f,
                w, aH
            )
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        return Outline.Generic(path)
    }
}

typealias MihrabDomeCardShape = KurungKurawaArchShape

class GrandMihrabPortalShape(
    private val archPeakRatio: Float = 0.25f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val peakH = h * archPeakRatio
            val r = 16f

            moveTo(0f, peakH)
            cubicTo(
                w * 0.08f, peakH * 0.62f,
                w * 0.22f, peakH * 0.52f,
                w * 0.32f, peakH * 0.32f
            )
            cubicTo(
                w * 0.40f, peakH * 0.14f,
                w * 0.46f, peakH * 0.03f,
                w * 0.50f, 0f
            )
            cubicTo(
                w * 0.54f, peakH * 0.03f,
                w * 0.60f, peakH * 0.14f,
                w * 0.68f, peakH * 0.32f
            )
            cubicTo(
                w * 0.78f, peakH * 0.52f,
                w * 0.92f, peakH * 0.62f,
                w, peakH
            )
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun SweepingGoldenCanopy(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Latar Belakang Kanopi Lengkung Atas Kiri
        val leftCanopyPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h * 0.14f)
            cubicTo(
                w * 0.08f, h * 0.30f,
                w * 0.18f, h * 0.36f,
                w * 0.26f, h * 0.26f
            )
            cubicTo(
                w * 0.31f, h * 0.18f,
                w * 0.34f, h * 0.08f,
                w * 0.36f, 0f
            )
            close()
        }
        drawPath(
            path = leftCanopyPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xF001150F), Color(0xDD022018), Color(0x9902261C))
            )
        )

        // Latar Belakang Kanopi Lengkung Atas Kanan
        val rightCanopyPath = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h * 0.14f)
            cubicTo(
                w * (1f - 0.08f), h * 0.30f,
                w * (1f - 0.18f), h * 0.36f,
                w * (1f - 0.26f), h * 0.26f
            )
            cubicTo(
                w * (1f - 0.31f), h * 0.18f,
                w * (1f - 0.34f), h * 0.08f,
                w * (1f - 0.36f), 0f
            )
            close()
        }
        drawPath(
            path = rightCanopyPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xF001150F), Color(0xDD022018), Color(0x9902261C))
            )
        )

        // 2. Garis Pita Emas Berombak (Sweeping Golden Ribbon)
        val goldBrush = Brush.linearGradient(
            listOf(Color(0xFFFFE082), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFFE082))
        )

        val leftRibbonEdge = Path().apply {
            moveTo(0f, h * 0.14f)
            cubicTo(
                w * 0.08f, h * 0.30f,
                w * 0.18f, h * 0.36f,
                w * 0.26f, h * 0.26f
            )
            cubicTo(
                w * 0.31f, h * 0.18f,
                w * 0.34f, h * 0.08f,
                w * 0.36f, 0f
            )
        }
        drawPath(
            path = leftRibbonEdge,
            brush = goldBrush,
            style = Stroke(width = 3.5f * density)
        )

        val rightRibbonEdge = Path().apply {
            moveTo(w, h * 0.14f)
            cubicTo(
                w * (1f - 0.08f), h * 0.30f,
                w * (1f - 0.18f), h * 0.36f,
                w * (1f - 0.26f), h * 0.26f
            )
            cubicTo(
                w * (1f - 0.31f), h * 0.18f,
                w * (1f - 0.34f), h * 0.08f,
                w * (1f - 0.36f), 0f
            )
        }
        drawPath(
            path = rightRibbonEdge,
            brush = goldBrush,
            style = Stroke(width = 3.5f * density)
        )

        // 3. Pola Bintang 8 Geometris Islami (Arabesque Rosette Watermark)
        val starCenterLeft = Offset(w * 0.14f, h * 0.44f)
        val starCenterRight = Offset(w * 0.86f, h * 0.44f)
        drawIslamicStarWatermark(starCenterLeft, radius = 75f * density)
        drawIslamicStarWatermark(starCenterRight, radius = 75f * density)
    }
}

private fun DrawScope.drawIslamicStarWatermark(center: Offset, radius: Float) {
    val goldSubtle = Color(0x14F59E0B)
    drawCircle(
        color = goldSubtle,
        radius = radius,
        center = center,
        style = Stroke(width = 1.2f)
    )
    drawCircle(
        color = goldSubtle,
        radius = radius * 0.65f,
        center = center,
        style = Stroke(width = 1f)
    )
    val rSquare = radius * 0.72f
    rotate(0f, center) {
        drawRect(
            color = goldSubtle,
            topLeft = Offset(center.x - rSquare / 2, center.y - rSquare / 2),
            size = Size(rSquare, rSquare),
            style = Stroke(width = 1f)
        )
    }
    rotate(45f, center) {
        drawRect(
            color = goldSubtle,
            topLeft = Offset(center.x - rSquare / 2, center.y - rSquare / 2),
            size = Size(rSquare, rSquare),
            style = Stroke(width = 1f)
        )
    }
}

@Composable
private fun RoyalPrayerBadge(
    displayName: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    countdown: String? = null,
    isCountdownOnly: Boolean = false,
    goldGradientBrush: Brush
) {
    val doubleKurawaShape = remember { DoubleEndedKurungKurawaShape(0.15f) }
    val innerDoubleKurawaShape = remember { DoubleEndedKurungKurawaShape(0.15f) }

    val amPm = remember(time) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        if (hour < 12) "AM" else "PM"
    }

    Box(
        modifier = modifier
            .clip(doubleKurawaShape)
            .background(
                if (isNext) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF044835), Color(0xFF012C20), Color(0xFF011C14))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xF002221A), Color(0xFA021A14), Color(0xFA01140E))
                    )
                }
            )
            .border(
                width = if (isNext) 2.5.dp else 1.8.dp,
                brush = if (isNext) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFF59E0B), Color(0xFFD97706))
                    )
                } else {
                    goldGradientBrush
                },
                shape = doubleKurawaShape
            )
            .padding(3.dp)
            .clip(innerDoubleKurawaShape)
            .border(
                width = 1.dp,
                color = if (isNext) Color(0x88FFD54F) else Color(0x33F59E0B),
                shape = innerDoubleKurawaShape
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Ikon Emas dalam Lingkaran
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x22F59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = displayName,
                    tint = if (isNext) Color(0xFFFFE082) else Color(0xFFFDE68A),
                    modifier = Modifier.size(15.dp)
                )
            }

            // 2. Nama Sholat
            if (isCountdownOnly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEXT PRAYER:",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekEmerald300,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = displayName.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE082),
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Text(
                    text = displayName.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isNext) Color.White else Color(0xFFE2E8F0),
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }

            // 3. Jam Sholat Besar
            if (isCountdownOnly) {
                Text(
                    text = countdown ?: time,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE082),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 12.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = time,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE082),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = amPm,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A),
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // 4. Highlight Indikator jika isNext
            if (isNext && !isCountdownOnly) {
                Text(
                    text = countdown?.let { "-$it" } ?: "● AKTIF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFE082),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

private fun formatRupiahMihrab(amount: Long): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        format.format(amount).replace(",00", "").replace("Rp", "Rp ")
    } catch (_: Exception) {
        "Rp $amount"
    }
}

private fun getMihrabCardTitle(type: String): String {
    return when (type) {
        "FRIDAY" -> "۞ PETUGAS SHOLAT JUM'AT"
        "FINANCE" -> "LAPORAN KAS MASJID ۞"
        "QRIS" -> "INFAQ DIGITAL QRIS ۞"
        "HADITH" -> "MUTIARA HADITS SHAHIH ۞"
        else -> "۞ MAKLUMAT MASJID ۞"
    }
}

@Composable
private fun OfficerDetailRow(label: String, name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFDE68A),
            letterSpacing = 0.3.sp
        )
        Text(
            text = name,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceAllocationRow(label: String, amount: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
        Text(text = amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
private fun MihrabCenterColumnContent(
    type: String,
    config: MosqueConfig,
    fridaySchedule: FridaySchedule,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long
) {
    when (type) {
        "FRIDAY" -> {
            val khotibName = cleanOfficerName(fridaySchedule.khotib, "Ust. Dr. H. Ahmad Dahlan")
            val imamName = cleanOfficerName(fridaySchedule.imam, "Ust. Farhan Al-Hafizh")
            val muadzinName = cleanOfficerName(fridaySchedule.muadzin, "Ust. Bilal Ramadhan")
            val bilalName = cleanOfficerName(fridaySchedule.bilal, "")

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕌 PETUGAS JUM'AT",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE082),
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = cleanFridayDate(fridaySchedule.date),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekEmerald300
                    )
                }

                OfficerDetailRow("KHATIB", khotibName)
                OfficerDetailRow("IMAM", imamName)
                OfficerDetailRow("MUADZIN", muadzinName)
                if (bilalName.isNotBlank()) {
                    OfficerDetailRow("BILAL", bilalName)
                } else if (fridaySchedule.khutbahTopic.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TEMA", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                        Text(fridaySchedule.khutbahTopic, fontSize = 10.5.sp, color = Color(0xFFCBD5E1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        "FINANCE" -> {
            val qrisBitmap = remember(config.qrisImagePath) {
                if (config.qrisImagePath.isNotBlank()) {
                    val f = File(config.qrisImagePath)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                } else null
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰 KAS & INFAQ MASJID",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE082),
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "SALDO: ${formatRupiahMihrab(balance)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF86EFAC)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FinanceAllocationRow("PEMASUKAN KAS:", formatRupiahMihrab(totalIncome), Color(0xFF86EFAC))
                    FinanceAllocationRow("PENGELUARAN KAS:", formatRupiahMihrab(totalExpense), Color(0xFFFCA5A5))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (config.bankAccount.isNotBlank()) "${config.bankName}: ${config.bankAccount}" else "Infaq & Shodaqoh Masjid",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (config.bankAccountName.isNotBlank()) "a.n. ${config.bankAccountName}" else "Mari Makmurkan Masjid",
                            fontSize = 8.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (qrisBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrisBitmap,
                                contentDescription = "QRIS",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
        "QRIS" -> {
            val qrisBitmap = remember(config.qrisImagePath) {
                if (config.qrisImagePath.isNotBlank()) {
                    val f = File(config.qrisImagePath)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                } else null
            }
            val collected = config.donationCollectedAmount
            val target = config.donationTargetAmount.coerceAtLeast(1L)
            val progress = (collected.toFloat() / target.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(2.dp),
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
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = config.donationProgramTitle,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Terkumpul: ${formatRupiahMihrab(collected)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE082)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                                    .fillMaxHeight()
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                    Text(
                        text = "Target: ${formatRupiahMihrab(target)}",
                        fontSize = 8.sp,
                        color = SleekEmerald300
                    )
                }
            }
        }
        "HADITH" -> {
            val hadith = remember {
                val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                DefaultHadiths.list[day % DefaultHadiths.list.size]
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hadith.arabic,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE082),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "\"${hadith.translation}\"",
                    fontSize = 8.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "• ${hadith.narrator} •",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekEmerald300
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("۞ MAKLUMAT MASJID ۞", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SleekAmber300)
                Spacer(modifier = Modifier.height(2.dp))
                Text(config.mosqueName, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                Text(config.tagline, fontSize = 9.5.sp, color = SleekEmerald200, maxLines = 1)
            }
        }
    }
}

@Composable
fun LayoutMihrabGrandRoyal(
    config: MosqueConfig,
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    currentTime: String,
    currentSeconds: String,
    isBlinkColon: Boolean,
    gregorianDate: String,
    hijriDate: String,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    slideIndex: Int = 0,
    localIp: String = "",
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val showFriday = config.showFridayOfficers || isFriday
    val showFinance = config.showFinancialReport
    val showQris = config.showQrisCard
    val showHadith = config.showDailyHadith
    val showActivities = config.showActivities
    val showMaklumat = config.showDailyMaklumat

    val goldGradientBrush = Brush.verticalGradient(
        listOf(Color(0xFFFFE082), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFF59E0B))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF043828), Color(0xFF012018), Color(0xFF00120D))
                )
            )
    ) {
        // 0. Sweeping Golden Ribbon Drapery Canopy & Islamic Star Watermarks Background
        SweepingGoldenCanopy(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP SECTION: Header Navigasi & Grand Mihrab Arch Portal (Jam Raksasa & Kaligrafi)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            ) {
                // Pill Indikator Kiri Atas (Logo & Identitas)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x44000000))
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_masjid_logo),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (config.tagline.isNotBlank()) config.tagline.uppercase() else "SMART MOSQUE TV",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekEmerald200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tombol Pengaturan Kanan Atas
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x44000000))
                        .border(1.dp, Color(0x33F59E0B), CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = SleekAmber400,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Portal Lengkung Kubah Mihrab Emas (Center Grand Arch)
                val portalShape = remember { GrandMihrabPortalShape(0.24f) }
                val innerPortalShape = remember { GrandMihrabPortalShape(0.24f) }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(580.dp)
                        .fillMaxHeight()
                        .clip(portalShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xF502281E), Color(0xFA03382A), Color(0xFA011D15))
                            )
                        )
                        .border(2.5.dp, goldGradientBrush, portalShape)
                        .padding(3.dp)
                        .clip(innerPortalShape)
                        .border(1.dp, Color(0x55F59E0B), innerPortalShape)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // 1. Kaligrafi Basmalah Emas
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE082),
                            letterSpacing = 0.5.sp
                        )

                        // 2. Baris Kaligrafi / Nama Masjid Gaya Thuluth
                        Text(
                            text = config.mosqueName.ifBlank { "المَسْجِدُ الْمُبَارَكُ لِتَقْوَى الْحِكْمَةِ وَالْيَقِينِ" }.uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A),
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 3. Jam Digital Raksasa Luminous Gold dengan Detik Berjalan
                        val mihrabClockFont = getClockFontFamily(config.clockFontFamily)
                        val mihrabClockColor = parseHexColor(config.clockColor, Color(0xFFFFE082))
                        val mihrabColonColor = parseHexColor(config.clockColonColor, Color(0xFFF59E0B))
                        val mihrabSecColor = parseHexColor(config.clockSecondsColor, Color(0xFFF59E0B))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val timeParts = currentTime.split(":")
                            val hours = timeParts.getOrElse(0) { "12" }
                            val mins = timeParts.getOrElse(1) { "00" }

                            Text(
                                text = hours,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = mihrabClockColor,
                                fontFamily = mihrabClockFont,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = ":",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlinkColon) mihrabColonColor else Color.Transparent,
                                fontFamily = mihrabClockFont,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            )
                            Text(
                                text = mins,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = mihrabClockColor,
                                fontFamily = mihrabClockFont,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = ":$currentSeconds",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = mihrabSecColor,
                                fontFamily = mihrabClockFont,
                                modifier = Modifier.padding(bottom = 6.dp, start = 3.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val amPm = remember(currentTime) {
                                val hour = hours.toIntOrNull() ?: 12
                                if (hour < 12) "AM" else "PM"
                            }
                            Text(
                                text = amPm,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(bottom = 6.dp, start = 3.dp)
                            )
                        }

                        // 4. Tanggal Masehi & Hijriah
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = gregorianDate,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = hijriDate,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }

            // BOTTOM SECTION: Simetris Sempurna 1:1 (3 Badge Kiri, Card Laporan Tengah, 3 Badge Kanan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 1. SISI KIRI: 3 Badge Waktu Sholat Kurung Kurawa Ganda
                val prayersLeft = listOf(
                    Triple("SUBUH", schedule?.subuh ?: "05:08", Icons.Default.WbTwilight),
                    Triple("TERBIT", schedule?.terbit ?: "06:34", Icons.Default.WbSunny),
                    Triple(if (isFriday) "JUM'AT" else "DZUHUR", schedule?.dzuhur ?: "12:21", Icons.Default.LightMode)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    prayersLeft.forEach { (name, time, icon) ->
                        val isNext = nextPrayerInfo?.prayer?.getDisplayName(isFriday).equals(name, ignoreCase = true)
                        RoyalPrayerBadge(
                            displayName = name,
                            time = time,
                            icon = icon,
                            isNext = isNext,
                            countdown = if (isNext) nextPrayerInfo?.formattedCountdown else null,
                            goldGradientBrush = goldGradientBrush,
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                        )
                    }
                }

                // 2. SISI TENGAH: Card Laporan Donasi & Petugas Jum'at dengan Mahkota Medallion Emas
                val centerItems = remember(showFriday, showFinance, showQris, showHadith, showActivities, showMaklumat) {
                    buildList {
                        if (showFinance) add("FINANCE")
                        if (showFriday) add("FRIDAY")
                        if (showHadith) add("HADITH")
                        if (showQris) add("QRIS")
                        if (showActivities) add("ACTIVITIES")
                        if (showMaklumat) add("MAKLUMAT")
                    }
                }

                if (centerItems.isEmpty()) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    val numItems = centerItems.size
                    val col1 = if (numItems == 1) centerItems[0] else centerItems[slideIndex % numItems]
                    val col2 = if (numItems <= 1) null else if (numItems == 2) centerItems[1] else centerItems[(slideIndex + 1) % numItems]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                    // Card Laporan Utama dengan Sudut Berlekuk & Double Gold Border
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp) // Ruang untuk medallion crest di atas
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xF502261C), Color(0xFA011D15), Color(0xFA01140E))
                                )
                            )
                            .border(1.8.dp, goldGradientBrush, RoundedCornerShape(14.dp))
                            .padding(2.5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                MihrabCenterColumnContent(
                                    type = col1,
                                    config = config,
                                    fridaySchedule = fridaySchedule,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    balance = balance
                                )
                            }

                            if (col2 != null) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(Color(0x33F59E0B))
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    MihrabCenterColumnContent(
                                        type = col2,
                                        config = config,
                                        fridaySchedule = fridaySchedule,
                                        totalIncome = totalIncome,
                                        totalExpense = totalExpense,
                                        balance = balance
                                    )
                                }
                            }
                        }
                    }

                    // Mahkota Medallion Emas Islami di Puncak Atas Card
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF032B20))
                            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFF59E0B))), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("۞", fontSize = 13.sp, color = Color(0xFFFFE082), fontWeight = FontWeight.Bold)
                    }
                }
            }

                // 3. SISI KANAN: 3 Badge Waktu Sholat Kurung Kurawa Ganda
                val prayersRight = listOf(
                    Triple("ASHAR", schedule?.ashar ?: "15:12", Icons.Default.WbSunny),
                    Triple("MAGHRIB", schedule?.maghrib ?: "18:07", Icons.Default.NightsStay),
                    Triple("ISYA", schedule?.isya ?: "19:22", Icons.Default.Bedtime)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    prayersRight.forEach { (name, time, icon) ->
                        val isNext = nextPrayerInfo?.prayer?.displayName.equals(name, ignoreCase = true)
                        RoyalPrayerBadge(
                            displayName = name,
                            time = time,
                            icon = icon,
                            isNext = isNext,
                            countdown = if (isNext) nextPrayerInfo?.formattedCountdown else null,
                            goldGradientBrush = goldGradientBrush,
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MihrabDomePrayerCard(
    displayName: String,
    arabicName: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNext: Boolean,
    goldGradientBrush: Brush,
    countdown: String? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isNext) {
        Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    } else {
        Brush.verticalGradient(listOf(Color(0xEE02281E), Color(0xF5011711)))
    }
    val textColor = if (isNext) Color(0xFF022018) else SleekAmber300
    val subTextColor = if (isNext) Color(0xFF022018).copy(alpha = 0.85f) else SleekEmerald300

    Column(
        modifier = modifier
            .clip(KurungKurawaArchShape())
            .background(cardBg)
            .border(if (isNext) 2.dp else 1.2.dp, if (isNext) Color.White else Color(0x66F59E0B), KurungKurawaArchShape())
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Ikon sholat di bawah lekuk kurung kurawa
        Spacer(modifier = Modifier.height(6.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = 0.4.sp
        )
        Text(
            text = time,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = if (isNext) Color(0xFF022018) else Color.White,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )
        if (isNext && countdown != null) {
            Text(
                text = "-$countdown",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF022018)
            )
        } else {
            Text(
                text = arabicName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = subTextColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun OfficerCompactTile(label: String, name: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x2E34D399))
            .padding(horizontal = 8.dp, vertical = 4.5.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = SleekEmerald300,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =========================================================================
// MODEL 8: CORDOBA & ALHAMBRA MOORISH (Lengkung Tapal Kuda Andalusia + QRIS)
// =========================================================================

class MoorishHorseshoeShape(
    private val cornerRadiusRatio: Float = 0.08f,
    private val archPeakRatio: Float = 0.28f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = (w * cornerRadiusRatio).coerceAtMost(14f)
            val springY = (h * archPeakRatio).coerceAtLeast(24f)

            moveTo(r, h)
            lineTo(w - r, h)
            quadraticTo(w, h, w, h - r)
            lineTo(w, springY * 1.10f)
            cubicTo(
                w * 1.05f, springY * 0.65f,
                w * 0.72f, 0f,
                w * 0.5f, 0f
            )
            cubicTo(
                w * 0.28f, 0f,
                -w * 0.05f, springY * 0.65f,
                0f, springY * 1.10f
            )
            lineTo(0f, h - r)
            quadraticTo(0f, h, r, h)
            close()
        }
        return Outline.Generic(path)
    }
}

class GrandCordobaPortalShape(
    private val archPeakRatio: Float = 0.30f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val peakH = h * archPeakRatio
            val r = 18f

            moveTo(r, h)
            lineTo(w - r, h)
            quadraticTo(w, h, w, h - r)
            lineTo(w, peakH * 1.10f)
            cubicTo(
                w * 1.04f, peakH * 0.60f,
                w * 0.70f, 0f,
                w * 0.5f, 0f
            )
            cubicTo(
                w * 0.30f, 0f,
                -w * 0.04f, peakH * 0.60f,
                0f, peakH * 1.10f
            )
            lineTo(0f, h - r)
            quadraticTo(0f, h, r, h)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun CordobaGrandPanel(
    type: String,
    config: MosqueConfig,
    fridaySchedule: FridaySchedule,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    showFinance: Boolean,
    showQris: Boolean,
    cordobaArchBrush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xF5180B0D))
            .border(1.5.dp, cordobaArchBrush, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        when (type) {
            "FRIDAY" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33B91C1C))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "۞ PETUGAS SHOLAT JUM'AT PEKAN INI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A)
                        )
                        Text(
                            text = cleanFridayDate(fridaySchedule.date),
                            fontSize = 10.sp,
                            color = Color(0xFFFECDD3)
                        )
                    }

                    val khotibName = cleanOfficerName(fridaySchedule.khotib, "Prof. Dr. KH. Ahmad Syakir, M.A.")
                    val imamName = cleanOfficerName(fridaySchedule.imam, "Ustadz H. M. Firdaus Al-Hafidz")
                    val muadzinName = cleanOfficerName(fridaySchedule.muadzin, "Ustadz Bilal Ramadhan")
                    val bilalName = cleanOfficerName(fridaySchedule.bilal, "Akhi Muhammad Syahril")

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OfficerCompactTile("🎙️ KHATIB JUM'AT", khotibName)
                        OfficerCompactTile("🕌 IMAM SHOLAT", imamName)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OfficerCompactTile("📢 MUADZIN", muadzinName)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OfficerCompactTile("📜 BILAL", bilalName)
                            }
                        }
                    }

                    if (fridaySchedule.khutbahTopic.isNotBlank()) {
                        Text(
                            text = "Khutbah: \"${fridaySchedule.khutbahTopic}\"",
                            fontSize = 9.5.sp,
                            color = Color(0xFFFDE68A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "• Mohon Lurus dan Rapatkan Shaf Sholat • Matikan / Senyapkan Ponsel",
                            fontSize = 9.sp,
                            color = Color(0xFFFECDD3),
                            maxLines = 1
                        )
                    }
                }
            }
            "FINANCE_QRIS" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val headerTitle = when {
                        showFinance && showQris -> "۞ KAS MASJID & DONASI DIGITAL QRIS"
                        showFinance -> "۞ LAPORAN KAS & KEUANGAN MASJID"
                        else -> "۞ INFAQ & DONASI DIGITAL QRIS"
                    }
                    val headerSub = when {
                        showFinance && showQris -> "BSI / E-Wallet / QRIS"
                        showFinance -> "Transparansi Dana Ummat"
                        else -> "Scan QRIS Barakah"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33B91C1C))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = headerTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A)
                        )
                        Text(
                            text = headerSub,
                            fontSize = 10.sp,
                            color = Color(0xFFFECDD3)
                        )
                    }

                    // Baris Saldo Kas (jika showFinance aktif)
                    if (showFinance) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33450A0A))
                                .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = if (!showQris) 8.dp else 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SALDO KAS TERKINI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                                Text(
                                    text = formatRupiahMihrab(balance),
                                    fontSize = if (!showQris) 23.sp else 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFE082),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("+ Masuk: ${formatRupiahMihrab(totalIncome)}", fontSize = 9.5.sp, color = Color(0xFF86EFAC))
                                Text("- Keluar: ${formatRupiahMihrab(totalExpense)}", fontSize = 9.5.sp, color = Color(0xFFFCA5A5))
                            }
                        }
                    }

                    // Progress Bar Donasi & QRIS (jika showQris aktif)
                    if (showQris) {
                        val collected = config.donationCollectedAmount
                        val target = config.donationTargetAmount.coerceAtLeast(1L)
                        val progress = (collected.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                        val percentInt = (progress * 100).toInt()

                        val qrisBitmap = remember(config.qrisImagePath) {
                            if (config.qrisImagePath.isNotBlank()) {
                                val f = File(config.qrisImagePath)
                                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                            } else null
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 8.dp, vertical = if (!showFinance) 8.dp else 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (!showFinance) 58.dp else 42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(2.dp),
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
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "QRIS",
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier.size(if (!showFinance) 46.dp else 34.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 ${config.donationProgramTitle}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$percentInt% Tercapai",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0x33FFFFFF))
                                ) {
                                    if (progress > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFDC2626), Color(0xFFFBBF24))
                                                    )
                                                )
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Terkumpul: ${formatRupiahMihrab(collected)}", fontSize = 8.5.sp, color = Color(0xFFFDE68A))
                                    Text("Target: ${formatRupiahMihrab(target)}", fontSize = 8.5.sp, color = Color(0xFFFECDD3))
                                }
                            }
                        }
                    }

                    // Rekening DKM
                    Text(
                        text = "🏛️ Rekening: ${config.bankName} ${config.bankAccount} a.n ${config.bankAccountName}",
                        fontSize = 9.sp,
                        color = Color(0xFFFECDD3),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            "HADITH" -> {
                val hadith = remember {
                    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    DefaultHadiths.list[day % DefaultHadiths.list.size]
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33B91C1C))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "۞ MUTIARA HADITS SHAHIH HARI INI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A)
                        )
                        Text(
                            text = hadith.topic,
                            fontSize = 9.5.sp,
                            color = Color(0xFFFECDD3)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hadith.arabic,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE082),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${hadith.translation}\"",
                            fontSize = 10.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "• ${hadith.narrator} •",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33B91C1C))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "۞ MAKLUMAT & INFORMASI JEMA'AH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(config.mosqueName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(config.tagline, fontSize = 10.5.sp, color = Color(0xFFFDE68A))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(config.address, fontSize = 9.5.sp, color = Color(0xFFFECDD3))
                    }

                    Text(
                        text = "• Mohon Lurus dan Rapatkan Shaf Sholat • Senyapkan Nada Dering Ponsel •",
                        fontSize = 8.5.sp,
                        color = Color(0xFFFDE68A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LayoutCordobaAndalusia(
    config: MosqueConfig,
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    currentTime: String,
    currentSeconds: String,
    isBlinkColon: Boolean,
    gregorianDate: String,
    hijriDate: String,
    finances: List<FinanceTransaction>,
    totalIncome: Long,
    totalExpense: Long,
    balance: Long,
    fridaySchedule: FridaySchedule,
    slideIndex: Int = 0,
    localIp: String = "",
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    val showFriday = config.showFridayOfficers
    val showFinance = config.showFinancialReport

    // Aksen Belang Dua Warna Khas Masjid Cordoba (Merah Terakota & Emas Marmer)
    val cordobaArchBrush = Brush.verticalGradient(
        listOf(Color(0xFFB91C1C), Color(0xFFFDE68A), Color(0xFF991B1B), Color(0xFFF59E0B))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. TOP SECTION: Grand Header Istana Cordoba (Lebar Penuh)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE62A0808), Color(0xF0180404), Color(0xE62A0808))
                    )
                )
                .border(1.5.dp, cordobaArchBrush, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sisi Kiri: Identitas Masjid & IP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_masjid_logo),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
                Column {
                    Text(
                        text = config.mosqueName.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (config.tagline.isNotBlank()) "${config.city} • ${config.tagline.uppercase()}" else "${config.address} ${config.city}",
                        fontSize = 11.5.sp,
                        color = Color(0xFFFDE68A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sisi Kanan: Jam Digital Istana Cordoba & Kalender
            val cordobaClockFont = getClockFontFamily(config.clockFontFamily)
            val cordobaClockColor = parseHexColor(config.clockColor, Color(0xFFFFE082))
            val cordobaSecColor = parseHexColor(config.clockSecondsColor, Color(0xFFF87171))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currentTime,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = cordobaClockColor,
                            fontFamily = cordobaClockFont,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = ":$currentSeconds",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = cordobaSecColor,
                            fontFamily = cordobaClockFont,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = "$gregorianDate • $hijriDate",
                        fontSize = 12.sp,
                        color = Color(0xFFFDE68A)
                    )
                }

                // Tombol Settings
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33B91C1C))
                        .border(1.dp, Color(0x66F59E0B), CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. MIDDLE SECTION: Panel Informasi Dinamis (Petugas Jum'at / Kas & QRIS / Hadits / Maklumat)
        val isFriday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val showFriday = config.showFridayOfficers || isFriday
        val showFinance = config.showFinancialReport
        val showQris = config.showQrisCard
        val showHadith = config.showDailyHadith
        val showActivities = config.showActivities
        val showMaklumat = config.showDailyMaklumat

        val activePanels = remember(showFriday, showFinance, showQris, showHadith, showActivities, showMaklumat) {
            buildList {
                if (showFinance || showQris) add("FINANCE_QRIS")
                if (showFriday) add("FRIDAY")
                if (showHadith) add("HADITH")
                if (showActivities) add("ACTIVITIES")
                if (showMaklumat) add("MAKLUMAT")
            }
        }

        if (activePanels.isEmpty()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )
        } else {
            val numPanels = activePanels.size
            val p1 = if (numPanels == 1) activePanels[0] else activePanels[slideIndex % numPanels]
            val p2 = if (numPanels <= 1) null else if (numPanels == 2) activePanels[1] else activePanels[(slideIndex + 1) % numPanels]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CordobaGrandPanel(
                    type = p1,
                    config = config,
                    fridaySchedule = fridaySchedule,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = balance,
                    showFinance = showFinance,
                    showQris = showQris,
                    cordobaArchBrush = cordobaArchBrush
                )
            }

            if (p2 != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CordobaGrandPanel(
                        type = p2,
                        config = config,
                        fridaySchedule = fridaySchedule,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        balance = balance,
                        showFinance = showFinance,
                        showQris = showQris,
                        cordobaArchBrush = cordobaArchBrush
                    )
                }
            }
        }
    }

        // 3. BOTTOM SECTION: 6 Kartu Tapal Kuda Cordoba Berjajar Horizontal Penuh (Tinggi Proporsional 120dp)
        val allPrayers = listOf(
            Tuple4("SUBUH", "الفجر", schedule?.subuh ?: "04:32", Icons.Default.WbTwilight),
            Tuple4("TERBIT", "الشروق", schedule?.terbit ?: "05:48", Icons.Default.WbSunny),
            Tuple4(if (isFriday) "JUM'AT" else "DZUHUR", if (isFriday) "الجمعة" else "الظهر", schedule?.dzuhur ?: "11:51", Icons.Default.LightMode),
            Tuple4("ASHAR", "العصر", schedule?.ashar ?: "15:12", Icons.Default.WbSunny),
            Tuple4("MAGHRIB", "المغرب", schedule?.maghrib ?: "17:53", Icons.Default.NightsStay),
            Tuple4("ISYA", "العشاء", schedule?.isya ?: "19:03", Icons.Default.Bedtime)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            allPrayers.forEach { (name, arName, time, icon) ->
                val isNext = nextPrayerInfo?.prayer?.getDisplayName(isFriday).equals(name, ignoreCase = true)
                CordobaMoorishPrayerCard(
                    displayName = name,
                    arabicName = arName,
                    time = time,
                    icon = icon,
                    isNext = isNext,
                    borderBrush = cordobaArchBrush,
                    countdown = if (isNext) nextPrayerInfo?.formattedCountdown else null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun CordobaMoorishPrayerCard(
    displayName: String,
    arabicName: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNext: Boolean,
    borderBrush: Brush,
    countdown: String? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isNext) {
        Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xEE1F0C0F), Color(0xF5140608)))
    }
    val textColor = if (isNext) Color(0xFFFDE68A) else Color(0xFFFFE082)
    val subTextColor = if (isNext) Color.White else Color(0xFFFCA5A5)

    Column(
        modifier = modifier
            .clip(MoorishHorseshoeShape())
            .background(cardBg)
            .border(if (isNext) 2.dp else 1.2.dp, if (isNext) Color.White else Color(0x66B91C1C), MoorishHorseshoeShape())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = 0.4.sp
        )
        Text(
            text = time,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            color = if (isNext) Color(0xFFFDE68A) else Color.White,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )
        if (isNext && countdown != null) {
            Text(
                text = "-$countdown",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        } else {
            Text(
                text = arabicName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = subTextColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

