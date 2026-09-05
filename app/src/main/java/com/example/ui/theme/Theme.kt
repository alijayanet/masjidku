package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IslamicColorScheme = darkColorScheme(
    primary = IslamicGoldPrimary,
    onPrimary = IslamicEmeraldDark,
    primaryContainer = IslamicEmeraldPrimary,
    onPrimaryContainer = IslamicGoldLight,
    secondary = IslamicGoldBright,
    onSecondary = IslamicEmeraldDark,
    background = IslamicEmeraldDark,
    onBackground = TextWhite,
    surface = IslamicEmeraldPrimary,
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF073824),
    onSurfaceVariant = TextGold,
    error = ExpenseRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = IslamicColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IslamicEmeraldDark.toArgb()
            window.navigationBarColor = IslamicEmeraldDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
