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

    // Accurately measure the full single-line text width in pixels without any bounding constraints
    val measuredWidthPx = remember(fullText, fontSize, density) {
        val layoutResult = textMeasurer.measure(
            text = AnnotatedString(fullText),
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            maxLines = 1,
            softWrap = false
        )
        layoutResult.size.width
    }

    var containerWidthPx by remember { mutableStateOf(0) }
    var positionedWidthPx by remember { mutableStateOf(0) }

    val effectiveTextWidthPx = maxOf(measuredWidthPx, positionedWidthPx)

    val totalDistance = remember(containerWidthPx, effectiveTextWidthPx) {
        if (containerWidthPx > 0 && effectiveTextWidthPx > 0) {
            containerWidthPx + effectiveTextWidthPx
        } else 0
    }

    // Animation duration proportional to text length (constant smooth speed ~70 px/s for TV viewing)
    val durationMs = remember(totalDistance) {
        val speedFactor = 70f // pixels per second
        if (totalDistance > 0) {
            ((totalDistance / speedFactor) * 1000).toInt().coerceAtLeast(8000)
        } else 20000
    }

    val transition = rememberInfiniteTransition(label = "ticker_trans")
    val animatedOffsetPx by transition.animateFloat(
        initialValue = containerWidthPx.toFloat(),
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

        // Running Text Content Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .onSizeChanged { containerWidthPx = it.width },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = fullText,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = SleekEmerald200,
                maxLines = 1,
                softWrap = false,
                letterSpacing = 0.3.sp,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        // Force unconstrained infinite width during measurement
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
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .onGloballyPositioned { positionedWidthPx = it.size.width }
                    .offset {
                        IntOffset(x = animatedOffsetPx.toInt(), y = 0)
                    }
            )
        }
    }
}
