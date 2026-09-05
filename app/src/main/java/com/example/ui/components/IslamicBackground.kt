package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.model.BackgroundPresetHelper
import java.io.File
import java.util.Calendar

@Composable
fun IslamicBackground(
    activeTheme: String,
    bgPreset: String = "PRESET_EMERALD_MIHRAB",
    backgroundType: String = "PRESET",
    backgroundVideoPath: String = "",
    customBackgroundImagePath: String = "",
    customBackgroundDim: Float = 0.5f,
    backgroundCctvUrl: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "islamic_bg")
    val glowAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val customBitmap = remember(customBackgroundImagePath) {
        if (customBackgroundImagePath.isNotBlank()) {
            try {
                val file = File(customBackgroundImagePath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val presetDrawableRes = remember(bgPreset) {
        BackgroundPresetHelper.getDrawableResId(bgPreset)
    }

    val (bgGradient, accentColor, secondaryColor) = when (activeTheme) {
        "DYNAMIC_SKY" -> {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val timeMinutes = hour * 60 + minute

            when {
                // Fajar / Subuh (04:00 - 05:45 = 240..345)
                timeMinutes in 240..345 -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF080D21), Color(0xFF1B1033), Color(0xFF331B4D))),
                    Color(0xFFFFD166),
                    Color(0xFFF77F00)
                )
                // Terbit & Dhuha Pagi (05:46 - 10:45 = 346..645)
                timeMinutes in 346..645 -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF0B1E29), Color(0xFF1A3847), Color(0xFF254B5E))),
                    Color(0xFFFFE082),
                    Color(0xFF38BDF8)
                )
                // Dzuhur Siang Jernih (10:46 - 14:45 = 646..885)
                timeMinutes in 646..885 -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF041728), Color(0xFF0A2E4E), Color(0xFF104369))),
                    Color(0xFF7DD3FC),
                    Color(0xFFFBBF24)
                )
                // Ashar Sore Hangat (14:46 - 17:30 = 886..1050)
                timeMinutes in 886..1050 -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF18120C), Color(0xFF382012), Color(0xFF593415))),
                    Color(0xFFF59E0B),
                    Color(0xFFFCD34D)
                )
                // Maghrib Lembayung Senja (17:31 - 18:50 = 1051..1130)
                timeMinutes in 1051..1130 -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF160A26), Color(0xFF330E30), Color(0xFF590E2E))),
                    Color(0xFFFB7185),
                    Color(0xFFFBBF24)
                )
                // Isya & Malam Beludru Bintang (18:51 - 03:59)
                else -> Triple(
                    Brush.verticalGradient(listOf(Color(0xFF020512), Color(0xFF091024), Color(0xFF020512))),
                    Color(0xFFFCD34D),
                    Color(0xFF38BDF8)
                )
            }
        }
        "NABAWI_EMERALD" -> Triple(
            Brush.verticalGradient(listOf(NabawiEmeraldDark, NabawiEmeraldPrimary, NabawiEmeraldDark)),
            NabawiGoldBright,
            NabawiGoldPrimary
        )
        "KISWAH_GOLD" -> Triple(
            Brush.verticalGradient(listOf(KiswahObsidianDark, KiswahObsidianPrimary, KiswahObsidianDark)),
            KiswahGoldBright,
            KiswahGoldPrimary
        )
        "OTTOMAN_BLUE" -> Triple(
            Brush.verticalGradient(listOf(OttomanSapphireDark, OttomanSapphirePrimary, OttomanSapphireDark)),
            OttomanTurquoise,
            OttomanGold
        )
        "ROYAL_NAVY" -> Triple(
            Brush.verticalGradient(listOf(RoyalNavyDark, RoyalNavyPrimary, RoyalNavyDark)),
            RoyalCyanAccent,
            IslamicGoldPrimary
        )
        "SUNSET_AMBER" -> Triple(
            Brush.verticalGradient(listOf(SunsetAmberDark, SunsetAmberPrimary, SunsetAmberDark)),
            SunsetOrangeAccent,
            IslamicGoldBright
        )
        "MIDNIGHT_CHARCOAL" -> Triple(
            Brush.verticalGradient(listOf(MidnightCharcoalDark, MidnightCharcoalPrimary, MidnightCharcoalDark)),
            IslamicGoldPrimary,
            Color(0xFF81C784)
        )
        "MIHRAB_CLASSIC" -> Triple(
            Brush.verticalGradient(listOf(Color(0xFF0D1E16), Color(0xFF1B3A2B), Color(0xFF0D1E16))),
            Color(0xFFFFD54F),
            Color(0xFF81C784)
        )
        else -> Triple(
            Brush.verticalGradient(listOf(IslamicEmeraldDark, IslamicEmeraldPrimary, IslamicEmeraldDark)),
            IslamicGoldPrimary,
            IslamicGoldBright
        )
    }

    val isCctvBackground = backgroundType == "CCTV" && backgroundCctvUrl.isNotBlank()
    val isVideoBackground = backgroundType == "VIDEO" && backgroundVideoPath.isNotBlank() && try { File(backgroundVideoPath).exists() } catch (_: Exception) { false }
    val hasVisualBackground = isCctvBackground || isVideoBackground || customBitmap != null || presetDrawableRes != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (!hasVisualBackground) Modifier.background(bgGradient) else Modifier)
    ) {
        if (isCctvBackground) {
            RtspCctvPlayerView(
                rtspUrl = backgroundCctvUrl,
                modifier = Modifier.fillMaxSize(),
                isMuted = true
            )
            // Center-translucent soft contrast wash for CCTV background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (customBackgroundDim * 0.80f).coerceIn(0.20f, 0.85f)),
                                Color.Black.copy(alpha = (customBackgroundDim * 0.50f).coerceIn(0.10f, 0.60f)),
                                Color.Black.copy(alpha = 0.25f)
                            ),
                            radius = 1100f
                        )
                    )
            )
        } else if (isVideoBackground) {
            LocalVideoPlayerView(
                videoPath = backgroundVideoPath,
                modifier = Modifier.fillMaxSize(),
                isMuted = true,
                isLooping = true,
                volume = 0f
            )
            // Center-translucent soft contrast wash for video background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (customBackgroundDim * 0.80f).coerceIn(0.20f, 0.85f)),
                                Color.Black.copy(alpha = (customBackgroundDim * 0.50f).coerceIn(0.10f, 0.60f)),
                                Color.Black.copy(alpha = 0.25f)
                            ),
                            radius = 1100f
                        )
                    )
            )
        } else if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Custom Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Center-translucent soft contrast wash: keeps corners and golden arches vivid,
            // while giving the center area soft readability for texts
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (customBackgroundDim * 0.75f).coerceIn(0.10f, 0.70f)),
                                Color.Black.copy(alpha = (customBackgroundDim * 0.40f).coerceIn(0.05f, 0.45f)),
                                Color.Transparent
                            ),
                            radius = 1100f
                        )
                    )
            )
        } else if (presetDrawableRes != null) {
            Image(
                painter = painterResource(id = presetDrawableRes),
                contentDescription = "Preset Islamic Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Center-translucent soft contrast wash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = (customBackgroundDim * 0.65f).coerceIn(0.08f, 0.65f)),
                                Color.Black.copy(alpha = (customBackgroundDim * 0.30f).coerceIn(0.02f, 0.35f)),
                                Color.Transparent
                            ),
                            radius = 1100f
                        )
                    )
            )
        }

        if (!hasVisualBackground) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Subtle glowing ambient light at top-center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.15f * glowAnim), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.1f),
                    radius = width * 0.45f
                ),
                radius = width * 0.45f,
                center = Offset(width * 0.5f, height * 0.1f)
            )

            // 2. Ambient light at bottom corners
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(width * 0.1f, height * 0.85f),
                    radius = width * 0.35f
                ),
                radius = width * 0.35f,
                center = Offset(width * 0.1f, height * 0.85f)
            )

            // 3. Islamic Arch & Star Geometric Outline
            val archPath = Path().apply {
                moveTo(width * 0.05f, height)
                lineTo(width * 0.05f, height * 0.35f)
                cubicTo(
                    width * 0.05f, height * 0.12f,
                    width * 0.45f, height * 0.04f,
                    width * 0.5f, height * 0.02f
                )
                cubicTo(
                    width * 0.55f, height * 0.04f,
                    width * 0.95f, height * 0.12f,
                    width * 0.95f, height * 0.35f
                )
                lineTo(width * 0.95f, height)
            }
            drawPath(
                path = archPath,
                color = accentColor.copy(alpha = 0.07f),
                style = Stroke(width = 2f)
            )

            // 4. Subtle 8-Pointed Islamic Stars in background
            fun drawIslamicStar(cx: Float, cy: Float, r: Float, alpha: Float) {
                val p = Path()
                val points = 8
                val innerR = r * 0.45f
                for (i in 0 until points * 2) {
                    val currentR = if (i % 2 == 0) r else innerR
                    val angle = i * Math.PI / points - Math.PI / 2
                    val x = cx + currentR * cos(angle).toFloat()
                    val y = cy + currentR * sin(angle).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, color = accentColor.copy(alpha = alpha), style = Stroke(width = 1.5f))
            }

            drawIslamicStar(width * 0.12f, height * 0.22f, 32f, 0.08f)
            drawIslamicStar(width * 0.88f, height * 0.22f, 32f, 0.08f)
            drawIslamicStar(width * 0.5f, height * 0.06f, 24f, 0.12f)
            drawIslamicStar(width * 0.08f, height * 0.65f, 20f, 0.05f)
            drawIslamicStar(width * 0.92f, height * 0.65f, 20f, 0.05f)

            // 5. Subtle Mosque Skyline Silhouette at Bottom
            val skylinePath = Path().apply {
                moveTo(0f, height * 0.95f)
                // Left dome
                lineTo(width * 0.15f, height * 0.95f)
                cubicTo(width * 0.18f, height * 0.90f, width * 0.22f, height * 0.90f, width * 0.25f, height * 0.95f)
                // Central dome
                lineTo(width * 0.42f, height * 0.95f)
                cubicTo(width * 0.46f, height * 0.86f, width * 0.54f, height * 0.86f, width * 0.58f, height * 0.95f)
                // Right dome
                lineTo(width * 0.75f, height * 0.95f)
                cubicTo(width * 0.78f, height * 0.90f, width * 0.82f, height * 0.90f, width * 0.85f, height * 0.95f)
                lineTo(width, height * 0.95f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(path = skylinePath, color = Color.Black.copy(alpha = 0.25f))

            // 6. Sacred Ambient Shimmer (Partikel Titik Debu Emas Melayang Lembut)
            val shimmerCount = 20
            for (i in 0 until shimmerCount) {
                val seed = i * 41f
                val px = (width * ((seed * 17f) % 100f) / 100f)
                val basePy = height * ((seed * 23f) % 100f) / 100f
                val floatOffset = (glowAnim - 0.7f) * 35f * ((i % 3) + 1)
                val py = (basePy - floatOffset + height) % height
                val particleRadius = 1.5f + (i % 3) * 0.8f
                val particleAlpha = (0.05f + 0.08f * sin((glowAnim * 3.14159f + i).toDouble()).toFloat()).coerceIn(0.02f, 0.16f)

                drawCircle(
                    color = accentColor.copy(alpha = particleAlpha),
                    radius = particleRadius,
                    center = Offset(px, py)
                )
            }
        }
        }

        content()
    }
}
