package com.pntapps.ekchhoan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** In-app text size multiplier (Settings → Text size). Elder-facing text multiplies its sp by this. */
val LocalTextScale = staticCompositionLocalOf { 1.0f }

/** Home icon settings: [scale] multiplies every icon and photo; [showLabels] false means icons only. */
data class HomeIcons(val scale: Float = 1f, val showLabels: Boolean = true) {
    /** Big icons or icons only stack each tile vertically (icon above label) so nothing gets squeezed. */
    val stacked: Boolean get() = scale > 1f || !showLabels
}

val LocalHomeIcons = staticCompositionLocalOf { HomeIcons() }

private fun materialColors(p: Palette): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.callGreen,
        onPrimary = Color.White,
        background = p.background,
        onBackground = p.ink,
        surface = p.surface,
        onSurface = p.ink,
        onSurfaceVariant = p.inkSoft,
        // Switch/segment outlines need ≥3:1 contrast; the Border token is for card edges only.
        outline = if (p.isDark) Color(0xFF8B95A3) else Color(0xFF6B7280),
        outlineVariant = p.border,
        secondaryContainer = if (p.isDark) Color(0xFF173F2C) else Color(0xFFD4F0E4),
        onSecondaryContainer = if (p.isDark) Color(0xFFD4F0E4) else Color(0xFF0F5132),
        surfaceContainerHighest = p.lockGrey,
        surfaceContainer = p.surface,
        surfaceContainerLow = p.surface,
        error = p.youTubeRed,
    )
}

/** Classic by default; the family can pick another theme in Settings. */
@Composable
fun EkChhoanTheme(textScale: Float = 1.0f, palette: Palette = ClassicPalette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextScale provides textScale, LocalPalette provides palette) {
        MaterialTheme(colorScheme = materialColors(palette), typography = AppTypography, content = content)
    }
}
