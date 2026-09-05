package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Sleek Interface Emerald & Amber Theme Palette
val SleekEmerald950 = Color(0xFF022C22)
val SleekEmerald900 = Color(0xFF064E3B)
val SleekEmerald800 = Color(0xFF065F46)
val SleekEmerald700 = Color(0xFF047857)
val SleekEmerald500 = Color(0xFF10B981)
val SleekEmerald400 = Color(0xFF34D399)
val SleekEmerald300 = Color(0xFF6EE7B7)
val SleekEmerald200 = Color(0xFFA7F3D0)

val SleekAmber500 = Color(0xFFF59E0B)
val SleekAmber400 = Color(0xFFFBBF24)
val SleekAmber300 = Color(0xFFFCD34D)
val SleekAmber100 = Color(0xFFFEF3C7)

// Backward compatible Islamic Emerald & Gold references mapped to Sleek Interface
val IslamicEmeraldDark = SleekEmerald950
val IslamicEmeraldPrimary = SleekEmerald900
val IslamicEmeraldLight = SleekEmerald700
val IslamicGoldPrimary = SleekAmber500
val IslamicGoldBright = SleekAmber400
val IslamicGoldLight = SleekAmber100
val IslamicGoldDark = Color(0xFFD97706)

// Alternative Theme Palettes
val RoyalNavyDark = Color(0xFF021226)
val RoyalNavyPrimary = Color(0xFF0B2447)
val RoyalNavyLight = Color(0xFF19376D)
val RoyalCyanAccent = Color(0xFF38BDF8)

val SunsetAmberDark = Color(0xFF1C0D03)
val SunsetAmberPrimary = Color(0xFF422108)
val SunsetAmberLight = Color(0xFF7A4011)
val SunsetOrangeAccent = Color(0xFFF97316)
val SunsetGoldLight = Color(0xFFFDE047)

val MidnightCharcoalDark = Color(0xFF0B0F17)
val MidnightCharcoalPrimary = Color(0xFF1E293B)
val MidnightCharcoalLight = Color(0xFF334155)

// Premium Islamic Themes Palettes
// 1. Mihrab Nabawi (Royal Emerald & Madinah Gold)
val NabawiEmeraldDark = Color(0xFF012018)
val NabawiEmeraldPrimary = Color(0xFF043828)
val NabawiEmeraldLight = Color(0xFF0A5C43)
val NabawiGoldBright = Color(0xFFFBBF24)
val NabawiGoldPrimary = Color(0xFFF59E0B)
val NabawiGoldDark = Color(0xFFD97706)

// 2. Kiswah Ka'bah (Midnight Obsidian & 24K Pure Gold)
val KiswahObsidianDark = Color(0xFF070A0F)
val KiswahObsidianPrimary = Color(0xFF101622)
val KiswahObsidianLight = Color(0xFF1E293B)
val KiswahGoldBright = Color(0xFFFCD34D)
val KiswahGoldPrimary = Color(0xFFF59E0B)
val KiswahGoldDark = Color(0xFFB45309)

// 3. Blue Ottoman & Samarkand (Sapphire & Turquoise)
val OttomanSapphireDark = Color(0xFF041021)
val OttomanSapphirePrimary = Color(0xFF0A2240)
val OttomanSapphireLight = Color(0xFF143B66)
val OttomanTurquoise = Color(0xFF38BDF8)
val OttomanGold = Color(0xFFFBBF24)

// Sleek Glass & Card Utilities
val GlassCardBackground = Color(0x66064E3B)
val GlassCardBorder = Color(0x26FFFFFF)
val GlassCardBorderEmerald = Color(0x3334D399)
val GlassCardBorderAmber = Color(0x66F59E0B)
val GlassCardHighlight = Color(0x1AFFFFFF)

// Text Colors
val TextGold = SleekAmber400
val TextWhite = Color(0xFFF8FAFC)
val TextWhiteMuted = Color(0xFFCBD5E1)
val TextWhiteDim = Color(0xFF94A3B8)
val TextEmeraldMuted = SleekEmerald300

// Status & Indicator Colors
val SuccessGreen = Color(0xFF10B981)
val ExpenseRed = Color(0xFFF43F5E)
val BadgeBlue = Color(0xFF38BDF8)
val BadgePurple = Color(0xFFA855F7)

/**
 * Safe Hex Color Parser with fallback
 */
fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
