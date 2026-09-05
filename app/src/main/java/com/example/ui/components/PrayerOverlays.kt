package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PrayerName
import com.example.ui.theme.*

import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.BackgroundPresetHelper
import com.example.data.model.MosqueConfig
import java.io.File

// ----------------------------------------------------
// ----------------------------------------------------
// 0. PRE-ADHAN COUNTDOWN OVERLAY (JELANG WAKTU SHOLAT)
// ----------------------------------------------------
@Composable
fun PreAdhanCountdownOverlay(
    prayerName: PrayerName,
    remainingSeconds: Int,
    imamName: String = "",
    muadzinName: String = "",
    khotibName: String = "",
    bilalName: String = "",
    isFriday: Boolean = false,
    config: MosqueConfig? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timeFormatted = if (mins > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d", secs)
    }

    val isUrgent = remainingSeconds in 1..10
    val isFridayDzuhur = isFriday && prayerName == PrayerName.DZUHUR
    val prayerTitle = if (isFridayDzuhur) "JUM'AT" else prayerName.displayName.uppercase()

    val customCountdownBitmap = remember(config?.customCountdownBgPath) {
        val path = config?.customCountdownBgPath.orEmpty()
        if (path.isNotBlank()) {
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) { null }
        } else null
    }

    val countdownResId = remember(config?.countdownBgPreset) {
        val preset = config?.countdownBgPreset ?: BackgroundPresetHelper.PRESET_COUNTDOWN_PLAQUE
        BackgroundPresetHelper.getDrawableResId(preset) ?: R.drawable.bg_preset_countdown_plaque
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High quality Islamic Stage / Plaque Background
        if (customCountdownBitmap != null) {
            Image(
                bitmap = customCountdownBitmap,
                contentDescription = "Countdown Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = countdownResId),
                contentDescription = "Countdown Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Center-translucent contrast scrim: lets corners & lanterns shine, keeping center texts razor sharp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isUrgent) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xD97F1D1D), Color(0x99000000), Color(0x40000000)),
                            radius = 950f
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color(0xB8000000), Color(0x73000000), Color(0x26000000)),
                            radius = 950f
                        )
                    }
                )
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33D4AF37))
                    .border(1.2.dp, SleekAmber400.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isFridayDzuhur) "⏳ HITUNG MUNDUR MENJELANG SHOLAT JUM'AT" else "⏳ HITUNG MUNDUR MENJELANG WAKTU SHOLAT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekAmber300,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PERSIAPAN SHOLAT $prayerTitle",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Big Digital Countdown Box (Bolder, Larger Islamic Digits)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF0000000))
                    .border(
                        3.5.dp,
                        if (isUrgent) Brush.linearGradient(listOf(ExpenseRed, Color(0xFFF87171)))
                        else Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFF59E0B), Color(0xFFFFD54F))),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 64.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 135.sp,
                        lineHeight = 125.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUrgent) ExpenseRed else SleekAmber400,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    )
                    if (mins == 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "detik",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isUrgent) Color(0xFFFCA5A5) else SleekAmber300,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Imam, Khotib, Muadzin, Bilal Badges (Lebar Maksimal & Font Jelas)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFridayDzuhur && khotibName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33000000))
                            .border(1.5.dp, SleekAmber400.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎙️", fontSize = 24.sp)
                        Column {
                            Text("KHOTIB JUM'AT:", fontSize = 12.sp, color = SleekAmber300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(khotibName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        }
                    }
                }

                if (imamName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33000000))
                            .border(1.5.dp, SleekEmerald400.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("👳", fontSize = 24.sp)
                        Column {
                            Text("IMAM SHOLAT:", fontSize = 12.sp, color = SleekEmerald300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(imamName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        }
                    }
                }

                if (muadzinName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33000000))
                            .border(1.5.dp, Color(0x6634D399), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📢", fontSize = 24.sp)
                        Column {
                            Text("MUADZIN:", fontSize = 12.sp, color = SleekEmerald300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(muadzinName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        }
                    }
                }

                if (isFridayDzuhur && bilalName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33000000))
                            .border(1.5.dp, Color(0x6634D399), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📜", fontSize = 24.sp)
                        Column {
                            Text("BILAL:", fontSize = 12.sp, color = SleekEmerald300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(bilalName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder message
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x3334D399), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 9.dp)
            ) {
                Text(
                    text = if (isFridayDzuhur) "💧 Silakan mandi sunnah, berwudhu, memakai wewangian & mengisi shaf terdepan" else "💧 Silakan mengambil air wudhu & bersiap mengisi shaf terdepan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekEmerald200
                )
            }
        }
    }
}

// ----------------------------------------------------
// 1. ADHAN FULLSCREEN OVERLAY (SLEEK MODERN)
// ----------------------------------------------------
@Composable
fun AdhanOverlay(
    prayerName: PrayerName,
    imamName: String = "",
    muadzinName: String = "",
    khotibName: String = "",
    bilalName: String = "",
    isFriday: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "adhan_pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val isFridayDzuhur = isFriday && prayerName == PrayerName.DZUHUR
    val prayerTitle = if (isFridayDzuhur) "SHOLAT JUM'AT" else prayerName.displayName.uppercase()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF065F46),
                        Color(0xFF022C22),
                        Color(0xFF011611)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Right Close button for TV remote/click
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sleek Illuminated Mosque Logo Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(SleekAmber400, SleekAmber500)
                        )
                    )
                    .border(2.5.dp, SleekAmber100.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_masjid_logo),
                    contentDescription = "Logo Adzan",
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isFridayDzuhur) "WAKTU SHOLAT JUM'AT TELAH TIBA" else "WAKTU SHOLAT TELAH TIBA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SleekEmerald300,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ADZAN $prayerTitle BERKUMANDANG",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Call to Prayer
            Text(
                text = "حَيَّ عَلَى الصَّلَاةِ • حَيَّ عَلَى الْفَلَاحِ",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SleekAmber400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Imam & Muadzin & Khotib Badges (Lebar & Jelas di TV)
            if (imamName.isNotBlank() || muadzinName.isNotBlank() || (isFridayDzuhur && khotibName.isNotBlank())) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFridayDzuhur && khotibName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x33000000))
                                .border(1.5.dp, SleekAmber400.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🎙️", fontSize = 24.sp)
                            Column {
                                Text("KHOTIB JUM'AT:", fontSize = 12.sp, color = SleekAmber300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(khotibName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                            }
                        }
                    }

                    if (imamName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x33000000))
                                .border(1.5.dp, SleekEmerald400.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("👳", fontSize = 24.sp)
                            Column {
                                Text("IMAM SHOLAT:", fontSize = 12.sp, color = SleekEmerald300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(imamName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                            }
                        }
                    }

                    if (muadzinName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x33000000))
                                .border(1.5.dp, Color(0x6634D399), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📢", fontSize = 24.sp)
                            Column {
                                Text("MUADZIN:", fontSize = 12.sp, color = SleekEmerald300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(muadzinName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x26FFFFFF))
                    .border(1.dp, Color(0x3334D399), RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 9.dp)
            ) {
                Text(
                    text = if (isFridayDzuhur) "Mari mendengarkan adzan & khutbah Jum'at dengan khusyuk serta seksama" else "Mari menjawab seruan adzan & bersiap mendirikan sholat berjamaah",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekEmerald200
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. IQOMAH COUNTDOWN OVERLAY (SLEEK MODERN)
// ----------------------------------------------------
@Composable
fun IqomahOverlay(
    prayerName: PrayerName,
    remainingSeconds: Int,
    imamName: String = "",
    isFriday: Boolean = false,
    config: MosqueConfig? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)

    val isLastSeconds = remainingSeconds in 1..10
    val isFridayDzuhur = isFriday && prayerName == PrayerName.DZUHUR
    val prayerTitle = if (isFridayDzuhur) "JUM'AT" else prayerName.displayName.uppercase()

    val customCountdownBitmap = remember(config?.customCountdownBgPath) {
        val path = config?.customCountdownBgPath.orEmpty()
        if (path.isNotBlank()) {
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) { null }
        } else null
    }

    val countdownResId = remember(config?.countdownBgPreset) {
        val preset = config?.countdownBgPreset ?: BackgroundPresetHelper.PRESET_COUNTDOWN_PLAQUE
        BackgroundPresetHelper.getDrawableResId(preset) ?: R.drawable.bg_preset_countdown_plaque
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High quality Islamic Stage / Plaque Background
        if (customCountdownBitmap != null) {
            Image(
                bitmap = customCountdownBitmap,
                contentDescription = "Iqomah Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = countdownResId),
                contentDescription = "Iqomah Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Center-translucent contrast scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isLastSeconds) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xD9881337), Color(0x99000000), Color(0x40000000)),
                            radius = 950f
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color(0xB8000000), Color(0x73000000), Color(0x26000000)),
                            radius = 950f
                        )
                    }
                )
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "HITUNG MUNDUR MENUJU IQOMAH",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SleekEmerald300,
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SHOLAT $prayerTitle BERJAMAAH",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                letterSpacing = (-0.5).sp
            )

            if (imamName.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33000000))
                        .border(1.5.dp, SleekAmber400.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👳", fontSize = 24.sp)
                    Column {
                        Text("IMAM SHOLAT:", fontSize = 11.5.sp, color = SleekAmber300, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(imamName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Massive Digital Countdown Box (Bolder, Larger Islamic Digits)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF0000000))
                    .border(
                        3.5.dp,
                        if (isLastSeconds) Brush.linearGradient(listOf(ExpenseRed, Color(0xFFF87171)))
                        else Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFF59E0B), Color(0xFFFFD54F))),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 64.dp, vertical = 8.dp)
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 135.sp,
                    lineHeight = 125.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isLastSeconds) ExpenseRed else SleekAmber400,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Guidance & Reminder Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x3334D399), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Silent HP", tint = SleekAmber400, modifier = Modifier.size(24.dp))
                    Text("Matikan / Senyapkan Nada Dering HP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x3334D399), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Shaf", tint = SleekAmber400, modifier = Modifier.size(24.dp))
                    Text("Luruskan dan Rapatkan Shaf Sholat", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. SHOLAT SILENT (DIMMED / HENING) OVERLAY
// ----------------------------------------------------
@Composable
fun SholatSilentOverlay(
    prayerName: PrayerName = PrayerName.DZUHUR,
    remainingSeconds: Int,
    imamName: String = "",
    isFriday: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)

    val isFridayDzuhur = isFriday && prayerName == PrayerName.DZUHUR
    val prayerTitle = if (isFridayDzuhur) "JUM'AT" else prayerName.displayName.uppercase()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF02120E))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0x66FFFFFF), modifier = Modifier.size(24.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SHOLAT $prayerTitle BERJAMAAH SEDANG BERLANGSUNG",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = SleekEmerald400,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            if (imamName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Imam: $imamName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = SleekAmber300
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "سَوُّوا صُفُوفَكُمْ فَإِنَّ تَسْوِيَةَ الصُّفُوفِ مِنْ إِقَامَةِ الصَّلَاةِ",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = SleekAmber300,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "“Luruskan shaf-shaf kalian, karena meluruskan shaf adalah bagian dari kesempurnaan sholat.”",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Mode Layar Hening ($timeFormatted)",
                    fontSize = 14.sp,
                    color = SleekEmerald300.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ----------------------------------------------------
// 5. FULLSCREEN VIDEO OVERLAY (PENAYANGAN VIDEO LOKAL)
// ----------------------------------------------------
@Composable
fun FullscreenVideoOverlay(
    videoPath: String,
    title: String,
    isMuted: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LocalVideoPlayerView(
            videoPath = videoPath,
            modifier = Modifier.fillMaxSize(),
            isMuted = isMuted,
            isLooping = false,
            volume = 1.0f,
            onCompletion = onDismiss,
            onError = onDismiss
        )

        // Subtle Top Bar Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "▶ TAYANGAN VIDEO",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = title.ifBlank { "Video Dokumentasi Masjid" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Video",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 8. FULLSCREEN CCTV / IP CAMERA LIVE OVERLAY
// ----------------------------------------------------
@Composable
fun FullscreenCctvOverlay(
    cctvUrl: String,
    title: String = "Live Kamera Masjid",
    isMuted: Boolean = true,
    cameras: List<com.example.data.model.CctvCameraItem> = emptyList(),
    activeCameraId: String = "",
    displayLayout: String = "SINGLE",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameras.isNotEmpty()) {
            MultiCctvPlayerGrid(
                cameras = cameras,
                activeCameraId = activeCameraId,
                displayLayout = displayLayout,
                modifier = Modifier.fillMaxSize(),
                isMuted = isMuted
            )
        } else {
            RtspCctvPlayerView(
                rtspUrl = cctvUrl,
                modifier = Modifier.fillMaxSize(),
                isMuted = isMuted
            )
        }

        // Top sleek title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "LIVE CCTV MASJID",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val currentCamName = cameras.firstOrNull { it.id == activeCameraId }?.name ?: title
                Text(
                    text = if (displayLayout != "SINGLE" && cameras.size > 1) "Multi-Kamera CCTV ($displayLayout)" else currentCamName.ifBlank { "Siaran Langsung CCTV Masjid" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isMuted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔇 Audio Senyap",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Siaran CCTV",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


