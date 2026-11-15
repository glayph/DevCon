package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist Zinc / Monolithic Base Palette
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF121215)
val Zinc850 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc600 = Color(0xFF52525B)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)
val PureWhite = Color(0xFFFFFFFF)

// Refined Accent Colors (Selectable by user)
val AccentCyan = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentViolet = Color(0xFF8B5CF6)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)
val AccentMono = Color(0xFFE4E4E7)

// Legacy aliases for backward compatibility
val CyberCyan = AccentCyan
val CyberCyanDark = Color(0xFF0891B2)
val ElectricIndigo = AccentViolet
val ElectricIndigoDark = Color(0xFF7C3AED)
val NeonEmerald = AccentEmerald
val NeonAmber = AccentAmber
val NeonCrimson = AccentRose

val Slate900 = Zinc950
val Slate800 = Zinc900
val Slate700 = Zinc800
val Slate600 = Zinc700
val Slate400 = Zinc400
val Slate200 = Zinc200

val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceVariant = Color(0xFFF4F4F5)
val LightOnSurface = Color(0xFF18181B)

enum class AppAccent(val label: String, val color: Color) {
    CYAN("Cyan", AccentCyan),
    EMERALD("Emerald", AccentEmerald),
    VIOLET("Violet", AccentViolet),
    AMBER("Amber", AccentAmber),
    MONO("Mono", AccentMono)
}

enum class UiDensity(val label: String, val cardPadding: Int, val spacing: Int) {
    COMPACT("Compact", 12, 8),
    COMFORTABLE("Comfortable", 18, 14)
}
