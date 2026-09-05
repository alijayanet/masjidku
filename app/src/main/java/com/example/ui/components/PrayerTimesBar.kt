package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MosqueConfig
import com.example.data.model.NextPrayerInfo
import com.example.data.model.PrayerName
import com.example.data.model.PrayerSchedule
import com.example.ui.theme.*

data class PrayerItemUi(
    val name: PrayerName,
    val displayName: String,
    val arabicName: String,
    val time: String,
    val isNext: Boolean,
    val countdown: String? = null
)

@Composable
fun PrayerTimesBar(
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    config: MosqueConfig = MosqueConfig(),
    modifier: Modifier = Modifier
) {
    if (schedule == null) return

    val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY

    val items = buildList {
        if (config.showImsak) {
            add(PrayerItemUi(PrayerName.IMSAK, "IMSAK", "الإمساك", schedule.imsak, nextPrayerInfo?.prayer == PrayerName.IMSAK))
        }
        if (config.showSubuh) {
            add(PrayerItemUi(PrayerName.SUBUH, "SUBUH", "الفجر", schedule.subuh, nextPrayerInfo?.prayer == PrayerName.SUBUH, nextPrayerInfo?.takeIf { it.prayer == PrayerName.SUBUH }?.formattedCountdown))
        }
        if (config.showTerbit) {
            add(PrayerItemUi(PrayerName.TERBIT, "TERBIT", "الشروق", schedule.terbit, nextPrayerInfo?.prayer == PrayerName.TERBIT))
        }
        if (config.showDhuha) {
            add(PrayerItemUi(PrayerName.DHUHA, "DHUHA", "الضحى", schedule.dhuha, nextPrayerInfo?.prayer == PrayerName.DHUHA))
        }
        if (config.showDzuhur) {
            val dName = if (isFriday) "JUM'AT" else "DZUHUR"
            val aName = if (isFriday) "الجمعة" else "الظهر"
            add(PrayerItemUi(PrayerName.DZUHUR, dName, aName, schedule.dzuhur, nextPrayerInfo?.prayer == PrayerName.DZUHUR, nextPrayerInfo?.takeIf { it.prayer == PrayerName.DZUHUR }?.formattedCountdown))
        }
        if (config.showAshar) {
            add(PrayerItemUi(PrayerName.ASHAR, "ASHAR", "العصر", schedule.ashar, nextPrayerInfo?.prayer == PrayerName.ASHAR, nextPrayerInfo?.takeIf { it.prayer == PrayerName.ASHAR }?.formattedCountdown))
        }
        if (config.showMaghrib) {
            add(PrayerItemUi(PrayerName.MAGHRIB, "MAGHRIB", "المغرب", schedule.maghrib, nextPrayerInfo?.prayer == PrayerName.MAGHRIB, nextPrayerInfo?.takeIf { it.prayer == PrayerName.MAGHRIB }?.formattedCountdown))
        }
        if (config.showIsya) {
            add(PrayerItemUi(PrayerName.ISYA, "ISYA'", "العشاء", schedule.isya, nextPrayerInfo?.prayer == PrayerName.ISYA, nextPrayerInfo?.takeIf { it.prayer == PrayerName.ISYA }?.formattedCountdown))
        }
    }

    if (items.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            PrayerCard(
                item = item,
                activeTheme = config.activeTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PrayerTimesVerticalColumn(
    schedule: PrayerSchedule?,
    nextPrayerInfo: NextPrayerInfo?,
    config: MosqueConfig = MosqueConfig(),
    modifier: Modifier = Modifier
) {
    if (schedule == null) return

    val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY

    val items = buildList {
        if (config.showImsak) {
            add(PrayerItemUi(PrayerName.IMSAK, "IMSAK", "الإمساك", schedule.imsak, nextPrayerInfo?.prayer == PrayerName.IMSAK))
        }
        if (config.showSubuh) {
            add(PrayerItemUi(PrayerName.SUBUH, "SUBUH", "الفجر", schedule.subuh, nextPrayerInfo?.prayer == PrayerName.SUBUH))
        }
        if (config.showTerbit) {
            add(PrayerItemUi(PrayerName.TERBIT, "TERBIT", "الشروق", schedule.terbit, nextPrayerInfo?.prayer == PrayerName.TERBIT))
        }
        if (config.showDhuha) {
            add(PrayerItemUi(PrayerName.DHUHA, "DHUHA", "الضحى", schedule.dhuha, nextPrayerInfo?.prayer == PrayerName.DHUHA))
        }
        if (config.showDzuhur) {
            val dName = if (isFriday) "JUM'AT" else "DZUHUR"
            val aName = if (isFriday) "الجمعة" else "الظهر"
            add(PrayerItemUi(PrayerName.DZUHUR, dName, aName, schedule.dzuhur, nextPrayerInfo?.prayer == PrayerName.DZUHUR))
        }
        if (config.showAshar) {
            add(PrayerItemUi(PrayerName.ASHAR, "ASHAR", "العصر", schedule.ashar, nextPrayerInfo?.prayer == PrayerName.ASHAR))
        }
        if (config.showMaghrib) {
            add(PrayerItemUi(PrayerName.MAGHRIB, "MAGHRIB", "المغرب", schedule.maghrib, nextPrayerInfo?.prayer == PrayerName.MAGHRIB))
        }
        if (config.showIsya) {
            add(PrayerItemUi(PrayerName.ISYA, "ISYA'", "العشاء", schedule.isya, nextPrayerInfo?.prayer == PrayerName.ISYA))
        }
    }

    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Dedicated Sleek Countdown Card for Next Prayer
        if (nextPrayerInfo != null) {
            val nextName = if (isFriday && nextPrayerInfo.prayer == PrayerName.DZUHUR) "JUM'AT" else nextPrayerInfo.prayer.displayName.uppercase()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE6064E3B), Color(0xF5022C22))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFFF59E0B))),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "●",
                            color = SleekAmber400,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "MENUJU $nextName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekAmber300,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = nextPrayerInfo.formattedCountdown,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE082),
                        fontFamily = OrbitronFont,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Masuk: ${nextPrayerInfo.timeStr} WIB",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
        }

        // Clean & Uniform Prayer Rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.5.dp)
        ) {
            items.forEach { item ->
                PrayerVerticalCard(
                    item = item,
                    activeTheme = config.activeTheme,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrayerCard(
    item: PrayerItemUi,
    activeTheme: String = "EMERALD_GOLD",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (activeGradient, normalGradient, activeTextColor, normalTitleColor) = when (activeTheme) {
        "NABAWI_EMERALD" -> Quad(
            Brush.verticalGradient(listOf(NabawiGoldBright, NabawiGoldPrimary)),
            Brush.verticalGradient(listOf(Color(0xCC043828), Color(0x99012018))),
            NabawiEmeraldDark,
            NabawiGoldBright
        )
        "KISWAH_GOLD" -> Quad(
            Brush.verticalGradient(listOf(KiswahGoldBright, KiswahGoldPrimary)),
            Brush.verticalGradient(listOf(Color(0xDD101622), Color(0xAA070A0F))),
            Color(0xFF070A0F),
            KiswahGoldBright
        )
        "OTTOMAN_BLUE" -> Quad(
            Brush.verticalGradient(listOf(OttomanTurquoise, Color(0xFF0284C7))),
            Brush.verticalGradient(listOf(Color(0xCC0A2240), Color(0x99041021))),
            Color(0xFF041021),
            OttomanTurquoise
        )
        "ROYAL_NAVY" -> Quad(
            Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))),
            Brush.verticalGradient(listOf(Color(0x990A192F), Color(0x66020C1B))),
            Color(0xFF020C1B),
            RoyalCyanAccent
        )
        "SUNSET_AMBER" -> Quad(
            Brush.verticalGradient(listOf(SunsetOrangeAccent, SunsetGoldLight)),
            Brush.verticalGradient(listOf(Color(0x993E1A00), Color(0x661F0A00))),
            Color(0xFF1F0A00),
            SunsetGoldLight
        )
        "MIDNIGHT_CHARCOAL" -> Quad(
            Brush.verticalGradient(listOf(IslamicGoldBright, IslamicGoldPrimary)),
            Brush.verticalGradient(listOf(Color(0x991E293B), Color(0x660F172A))),
            Color(0xFF0F172A),
            Color(0xFF94A3B8)
        )
        "MIHRAB_CLASSIC" -> Quad(
            Brush.verticalGradient(listOf(Color(0xFFF9A825), Color(0xFFF57F17))),
            Brush.verticalGradient(listOf(Color(0x991B3A2B), Color(0x660D1E16))),
            Color(0xFF0D1E16),
            Color(0xFFFFD54F)
        )
        else -> Quad(
            Brush.verticalGradient(listOf(SleekAmber400, SleekAmber500)),
            Brush.verticalGradient(listOf(Color(0x99064E3B), Color(0x66022C22))),
            SleekEmerald950,
            SleekEmerald400
        )
    }

    val cardBg = if (item.isNext) activeGradient else normalGradient
    val borderColor = if (item.isNext) Color.White.copy(alpha = 0.4f * pulseBorder) else Color(0x1AFFFFFF)
    val borderWidth = if (item.isNext) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Prayer Name
            Text(
                text = item.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (item.isNext) activeTextColor else normalTitleColor,
                letterSpacing = 0.5.sp
            )

            // Prayer Time (Bolder, Larger, Islamic Contrast)
            Text(
                text = item.time,
                fontSize = if (item.isNext) 25.sp else 23.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isNext) activeTextColor else TextWhite,
                fontFamily = RajdhaniFont,
                letterSpacing = 0.5.sp
            )

            // Bottom Subtext (Arabic or Live Countdown)
            if (item.isNext && item.countdown != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(activeTextColor.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "-${item.countdown}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor,
                        fontFamily = OrbitronFont
                    )
                }
            } else {
                Text(
                    text = item.arabicName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isNext) activeTextColor.copy(alpha = 0.85f) else TextWhiteDim
                )
            }
        }
    }
}

@Composable
private fun PrayerVerticalCard(
    item: PrayerItemUi,
    activeTheme: String = "EMERALD_GOLD",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vert_glow")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_v"
    )

    val (activeGradient, normalGradient, activeTextColor, normalTitleColor) = when (activeTheme) {
        "NABAWI_EMERALD" -> Quad(
            Brush.horizontalGradient(listOf(NabawiGoldBright, NabawiGoldPrimary)),
            Brush.horizontalGradient(listOf(Color(0xCC043828), Color(0x99012018))),
            NabawiEmeraldDark,
            NabawiGoldBright
        )
        "KISWAH_GOLD" -> Quad(
            Brush.horizontalGradient(listOf(KiswahGoldBright, KiswahGoldPrimary)),
            Brush.horizontalGradient(listOf(Color(0xDD101622), Color(0xAA070A0F))),
            Color(0xFF070A0F),
            KiswahGoldBright
        )
        "OTTOMAN_BLUE" -> Quad(
            Brush.horizontalGradient(listOf(OttomanTurquoise, Color(0xFF0284C7))),
            Brush.horizontalGradient(listOf(Color(0xCC0A2240), Color(0x99041021))),
            Color(0xFF041021),
            OttomanTurquoise
        )
        "ROYAL_NAVY" -> Quad(
            Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))),
            Brush.horizontalGradient(listOf(Color(0x990A192F), Color(0x66020C1B))),
            Color(0xFF020C1B),
            RoyalCyanAccent
        )
        "SUNSET_AMBER" -> Quad(
            Brush.horizontalGradient(listOf(SunsetOrangeAccent, SunsetGoldLight)),
            Brush.horizontalGradient(listOf(Color(0x993E1A00), Color(0x661F0A00))),
            Color(0xFF1F0A00),
            SunsetGoldLight
        )
        "MIDNIGHT_CHARCOAL" -> Quad(
            Brush.horizontalGradient(listOf(IslamicGoldBright, IslamicGoldPrimary)),
            Brush.horizontalGradient(listOf(Color(0x991E293B), Color(0x660F172A))),
            Color(0xFF0F172A),
            Color(0xFF94A3B8)
        )
        "MIHRAB_CLASSIC" -> Quad(
            Brush.horizontalGradient(listOf(Color(0xFFF9A825), Color(0xFFF57F17))),
            Brush.horizontalGradient(listOf(Color(0x991B3A2B), Color(0x660D1E16))),
            Color(0xFF0D1E16),
            Color(0xFFFFD54F)
        )
        else -> Quad(
            Brush.horizontalGradient(listOf(SleekAmber400, SleekAmber500)),
            Brush.horizontalGradient(listOf(Color(0x99064E3B), Color(0x66022C22))),
            SleekEmerald950,
            SleekEmerald400
        )
    }

    val cardBg = if (item.isNext) activeGradient else normalGradient
    val borderColor = if (item.isNext) Color.White.copy(alpha = 0.4f * pulseBorder) else Color(0x1AFFFFFF)
    val borderWidth = if (item.isNext) 1.5.dp else 1.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.isNext) {
                    Text("●", fontSize = 7.sp, color = activeTextColor)
                }
                Text(
                    text = item.displayName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isNext) activeTextColor else normalTitleColor,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = item.arabicName,
                fontSize = 10.sp,
                color = if (item.isNext) activeTextColor.copy(alpha = 0.85f) else TextWhiteDim
            )
        }

        Text(
            text = item.time,
            fontSize = if (item.isNext) 22.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.isNext) activeTextColor else TextWhite,
            fontFamily = RajdhaniFont,
            letterSpacing = 0.5.sp
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
