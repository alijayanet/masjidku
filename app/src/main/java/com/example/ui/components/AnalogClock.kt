package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AnalogClockTheme {
    CLASSIC_WHITE, // Model Al-Amin (White face, black numerals, metallic/green border)
    ROYAL_BLUE,    // Model Al-Ikhsan (Deep blue face, white numerals, chrome rim)
    EMERALD_GOLD   // Emerald face, gold numerals & hands
}

@Composable
fun AnalogClock(
    calendar: Calendar,
    modifier: Modifier = Modifier,
    clockTheme: AnalogClockTheme = AnalogClockTheme.CLASSIC_WHITE
) {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 6.dp.toPx()

            // 1. Dial Colors based on theme
            val (dialBackground, rimGradient, numeralColor, tickColor, hourHandColor, minHandColor, secHandColor) = when (clockTheme) {
                AnalogClockTheme.ROYAL_BLUE -> Tuple7(
                    Color(0xFF0F3A66),
                    listOf(Color(0xFF81D4FA), Color(0xFF0288D1), Color(0xFF01579B), Color(0xFF81D4FA)),
                    Color.White,
                    Color(0x99FFFFFF),
                    Color.White,
                    Color(0xFFE1F5FE),
                    Color(0xFFFF5252) // Red second hand
                )
                AnalogClockTheme.EMERALD_GOLD -> Tuple7(
                    Color(0xFF063524),
                    listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFF8D6E63), Color(0xFFFFE082)),
                    Color(0xFFFFE082),
                    Color(0x80FFE082),
                    Color(0xFFFFD54F),
                    Color(0xFFFFF8E1),
                    Color(0xFF4ADE80)
                )
                else -> Tuple7( // CLASSIC_WHITE (Default)
                    Color(0xFFF9FBFA),
                    listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569), Color(0xFFE2E8F0)),
                    Color(0xFF0F172A),
                    Color(0xFF64748B),
                    Color(0xFF1E293B),
                    Color(0xFF334155),
                    Color(0xFFDC2626) // Vivid red
                )
            }

            // 2. Outer Bezel / Rim
            drawCircle(
                brush = Brush.sweepGradient(rimGradient, center),
                radius = radius + 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius + 5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner Shadow & Face Fill
            drawCircle(
                color = dialBackground,
                radius = radius,
                center = center
            )

            // Subtle 3D gradient overlay on dial face
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )

            // 3. Draw Minute / Hour Tick Marks
            for (i in 0 until 60) {
                val angleRad = (i * 6 - 90) * (PI / 180.0)
                val isHour = i % 5 == 0
                val tickLength = if (isHour) radius * 0.10f else radius * 0.04f
                val strokeWidth = if (isHour) 2.5.dp.toPx() else 1.dp.toPx()
                val startRadius = radius - 4.dp.toPx()
                val endRadius = startRadius - tickLength

                val start = Offset(
                    x = (center.x + startRadius * cos(angleRad)).toFloat(),
                    y = (center.y + startRadius * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = (center.x + endRadius * cos(angleRad)).toFloat(),
                    y = (center.y + endRadius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (isHour) numeralColor.copy(alpha = 0.8f) else tickColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 4. Draw Hour Numerals (1 to 12)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = numeralColor.hashCode()
                    textSize = radius * 0.22f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                for (i in 1..12) {
                    val angleRad = (i * 30 - 90) * (PI / 180.0)
                    val numRadius = radius * 0.72f
                    val numX = (center.x + numRadius * cos(angleRad)).toFloat()
                    val numY = (center.y + numRadius * sin(angleRad) + (paint.textSize / 3f)).toFloat()
                    canvas.nativeCanvas.drawText(i.toString(), numX, numY, paint)
                }
            }

            // 5. Calculate Hand Angles
            val secAngle = (second * 6f) - 90f
            val minAngle = (minute * 6f + second * 0.1f) - 90f
            val hourAngle = ((hour % 12) * 30f + minute * 0.5f) - 90f

            // Hour Hand
            drawHand(
                center = center,
                angleDeg = hourAngle,
                length = radius * 0.50f,
                width = 5.dp.toPx(),
                color = hourHandColor,
                tailLength = radius * 0.10f
            )

            // Minute Hand
            drawHand(
                center = center,
                angleDeg = minAngle,
                length = radius * 0.75f,
                width = 3.5.dp.toPx(),
                color = minHandColor,
                tailLength = radius * 0.12f
            )

            // Second Hand (Thin & Sharp)
            drawHand(
                center = center,
                angleDeg = secAngle,
                length = radius * 0.85f,
                width = 1.8.dp.toPx(),
                color = secHandColor,
                tailLength = radius * 0.20f
            )

            // Center Pin / Cap
            drawCircle(
                color = secHandColor,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = center
            )
        }
    }
}

private fun DrawScope.drawHand(
    center: Offset,
    angleDeg: Float,
    length: Float,
    width: Float,
    color: Color,
    tailLength: Float = 0f
) {
    val angleRad = angleDeg * (PI / 180.0)
    val tip = Offset(
        x = (center.x + length * cos(angleRad)).toFloat(),
        y = (center.y + length * sin(angleRad)).toFloat()
    )
    val tail = Offset(
        x = (center.x - tailLength * cos(angleRad)).toFloat(),
        y = (center.y - tailLength * sin(angleRad)).toFloat()
    )

    drawLine(
        color = color,
        start = tail,
        end = tip,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

private data class Tuple7<A, B, C, D, E, F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G
)
