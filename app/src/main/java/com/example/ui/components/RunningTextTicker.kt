package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RunningTextItem
import com.example.ui.theme.*

@Composable
fun RunningTextTicker(
    runningTexts: List<RunningTextItem>,
    fontSize: Int = 20,
    modifier: Modifier = Modifier
) {
    val activeMessages = runningTexts.filter { it.isActive }
    val fullText = remember(activeMessages) {
        val raw = if (activeMessages.isEmpty()) {
            "Selamat Datang di Rumah Allah SWT • Luruskan dan rapatkan shaf sholat • Matikan atau senyapkan nada dering ponsel • Jagalah kesucian dan kebersihan masjid"
        } else {
            activeMessages.joinToString("        ❖        ") { it.text.trim() }
        }
        "$raw        ❖        "
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val textStyle = remember(fontSize) {
        TextStyle(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = SleekEmerald200
        )
    }

    // Accurately measure the single-cycle text width in pixels
    val measuredWidthPx = remember(fullText, textStyle, density) {
        val layoutResult = textMeasurer.measure(
            text = AnnotatedString(fullText),
            style = textStyle,
            maxLines = 1,
            softWrap = false
        )
        layoutResult.size.width
    }

    var containerWidthPx by remember { mutableStateOf(0) }
    val effectiveTextWidthPx = measuredWidthPx.coerceAtLeast(100)

    // Calculate how many copies are needed to guarantee a seamless, infinite, zero-gap flow
    val copyCount = remember(containerWidthPx, effectiveTextWidthPx) {
        if (containerWidthPx > 0 && effectiveTextWidthPx > 0) {
            (containerWidthPx / effectiveTextWidthPx) + 2
        } else {
            2
        }
    }

    // Animation duration for exactly 1 full cycle of effectiveTextWidthPx (smooth speed ~65 px/s)
    val durationMs = remember(effectiveTextWidthPx) {
        val speedFactor = 65f // pixels per second
        ((effectiveTextWidthPx / speedFactor) * 1000).toInt().coerceAtLeast(3000)
    }

    val transition = rememberInfiniteTransition(label = "ticker_trans")
    val animatedOffsetPx by transition.animateFloat(
        initialValue = 0f,
        targetValue = -effectiveTextWidthPx.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ticker_offset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6022C22))
            .border(1.2.dp, Color(0x4D34D399), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sleek Emerald INFO Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(SleekAmber400, SleekAmber500)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "INFO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = SleekEmerald950,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Running Text Content Container (Seamless Infinite Zero-Gap Marquee)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .onSizeChanged { containerWidthPx = it.width },
            contentAlignment = Alignment.CenterStart
        ) {
            if (containerWidthPx > 0 && effectiveTextWidthPx > 0) {
                for (i in 0 until copyCount) {
                    val offsetX = animatedOffsetPx + (i * effectiveTextWidthPx)
                    Text(
                        text = fullText,
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = 0,
                                        maxWidth = Constraints.Infinity
                                    )
                                )
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .offset {
                                IntOffset(x = offsetX.toInt(), y = 0)
                            }
                    )
                }
            } else {
                Text(
                    text = fullText,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
