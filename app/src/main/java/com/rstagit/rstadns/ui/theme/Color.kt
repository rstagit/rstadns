package com.rstagit.rstadns.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val BackgroundDeep = Color(0xFF0D0D1A)
val BackgroundCard = Color(0xFF141428)
val BackgroundElevated = Color(0xFF1C1C35)
val SurfaceVariant = Color(0xFF21213D)

val AccentPrimary = Color(0xFF7C3AED)
val AccentSecondary = Color(0xFF8B5CF6)
val AccentGlow = Color(0xFF7C3AED).copy(alpha = 0.35f)
val AccentConnect = Color(0xFF6D28D9)

val PingExcellent = Color(0xFF22C55E)
val PingGood = Color(0xFF84CC16)
val PingModerate = Color(0xFFF59E0B)
val PingPoor = Color(0xFFEF4444)
val PingUnknown = Color(0xFF64748B)
val PingFailed = Color(0xFF991B1B)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF475569)
val TextAccent = Color(0xFFA78BFA)

val BorderSubtle = Color(0xFF252545)
val BorderActive = Color(0xFF7C3AED).copy(alpha = 0.5f)

val ConnectedGreen = Color(0xFF10B981)
val DisconnectedRed = Color(0xFFEF4444)
val ConnectingYellow = Color(0xFFF59E0B)

val GlowRed = Color(0xFFEF4444).copy(alpha = 0.3f)
val GlowGreen = Color(0xFF10B981).copy(alpha = 0.3f)
val GlowPurple = Color(0xFF7C3AED).copy(alpha = 0.25f)

val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2D1B69),
    onPrimaryContainer = AccentSecondary,
    secondary = AccentSecondary,
    onSecondary = Color(0xFF1A0A3D),
    secondaryContainer = Color(0xFF2D1B69),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = PingExcellent,
    background = BackgroundDeep,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderActive,
    error = PingPoor,
    onError = Color.White,
    errorContainer = Color(0xFF3B0A0A),
    onErrorContainer = Color(0xFFFFB4AB)
)
