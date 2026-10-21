package com.pntapps.ekchhoan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pntapps.ekchhoan.data.AppTheme

/** Internet bar palette: background, border + icon circle, text. */
data class InternetColors(val background: Color, val accent: Color, val text: Color)

/**
 * How cards and tiles are drawn. Classic uses flat white cards with a 2dp outline; the modern themes use
 * borderless cards that float on a soft tinted shadow, rounder corners and gradient icon chips.
 */
data class CardStyle(
    val borderWidth: Dp,
    val elevation: Dp,
    val shadowTint: Color,
    /** Added to every card/tile corner radius. */
    val cornerExtra: Dp,
    val gradientChips: Boolean,
)

/** Every color the UI uses, so a theme swaps as a whole. Text pairs keep ≥4.5:1 contrast in every theme. */
data class Palette(
    val background: Color,
    /** Screen backdrop on Home; a soft gradient in the modern themes. */
    val backgroundBrush: Brush,
    val surface: Color,
    val border: Color,
    val ink: Color,
    val inkSoft: Color,
    val callGreen: Color,
    val youTubeRed: Color,
    val cameraPurple: Color,
    val galleryOrange: Color,
    val torchOnBg: Color,
    val torchOnBorder: Color,
    val torchOnInk: Color,
    val lockGrey: Color,
    val avatarTints: List<Pair<Color, Color>>,
    val internetOk: InternetColors,
    val internetWarn: InternetColors,
    val internetOff: InternetColors,
    val card: CardStyle,
    val isDark: Boolean,
)

private val ClassicCard = CardStyle(borderWidth = 2.dp, elevation = 0.dp, shadowTint = Color.Black, cornerExtra = 0.dp, gradientChips = false)

private val LightInternet = Triple(
    InternetColors(Color(0xFFE3F5EC), Color(0xFF1B7F3B), Color(0xFF0F5132)),
    InternetColors(Color(0xFFFFF0DC), Color(0xFFB4530A), Color(0xFF7A3300)),
    InternetColors(Color(0xFFFDE4E4), Color(0xFFC62828), Color(0xFF8E1616)),
)
private val DarkInternet = Triple(
    InternetColors(Color(0xFF12301F), Color(0xFF34A35F), Color(0xFFB4EBCB)),
    InternetColors(Color(0xFF3A2508), Color(0xFFE08A1E), Color(0xFFFCD9A8)),
    InternetColors(Color(0xFF3D1515), Color(0xFFE05252), Color(0xFFF8C0C0)),
)

private fun vertical(top: Color, bottom: Color) = Brush.verticalGradient(listOf(top, bottom))

/** The original design. */
val ClassicPalette = Palette(
    background = Color(0xFFF2F4F7),
    backgroundBrush = vertical(Color(0xFFF2F4F7), Color(0xFFF2F4F7)),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE2E6EC),
    ink = Color(0xFF111827),
    inkSoft = Color(0xFF374151),
    callGreen = Color(0xFF1B7F3B),
    youTubeRed = Color(0xFFC62828),
    cameraPurple = Color(0xFF5B3CC4),
    galleryOrange = Color(0xFFB4530A),
    torchOnBg = Color(0xFFFFE08A),
    torchOnBorder = Color(0xFFE0A800),
    torchOnInk = Color(0xFF111827),
    lockGrey = Color(0xFFE5E7EB),
    avatarTints = listOf(
        Color(0xFFDCE7FF) to Color(0xFF1E3A8A),
        Color(0xFFFFE4CC) to Color(0xFF8A3A00),
        Color(0xFFE6DDFF) to Color(0xFF4527A0),
        Color(0xFFD4F0E4) to Color(0xFF0F5132),
    ),
    internetOk = LightInternet.first,
    internetWarn = LightInternet.second,
    internetOff = LightInternet.third,
    card = ClassicCard,
    isDark = false,
)

/** Classic in soft dark (not pure black, so large white text doesn't glare). */
val ClassicDarkPalette = Palette(
    background = Color(0xFF0F141A),
    backgroundBrush = vertical(Color(0xFF0F141A), Color(0xFF0F141A)),
    surface = Color(0xFF1B222B),
    border = Color(0xFF323C48),
    ink = Color(0xFFF3F4F6),
    inkSoft = Color(0xFFC9CED6),
    callGreen = Color(0xFF1E8A42),
    youTubeRed = Color(0xFFD32F2F),
    cameraPurple = Color(0xFF6A4DD6),
    galleryOrange = Color(0xFFC25E0E),
    torchOnBg = Color(0xFF4A3B00),
    torchOnBorder = Color(0xFFE0A800),
    torchOnInk = Color(0xFFFFF4CC),
    lockGrey = Color(0xFF2B3440),
    avatarTints = listOf(
        Color(0xFF233A6B) to Color(0xFFDCE7FF),
        Color(0xFF5A2E0C) to Color(0xFFFFE4CC),
        Color(0xFF3B2A78) to Color(0xFFE6DDFF),
        Color(0xFF173F2C) to Color(0xFFD4F0E4),
    ),
    internetOk = DarkInternet.first,
    internetWarn = DarkInternet.second,
    internetOff = DarkInternet.third,
    card = ClassicCard,
    isDark = true,
)

/** Warm peach morning light; cards float on a soft coral shadow. */
val SunrisePalette = Palette(
    background = Color(0xFFFFF5EE),
    backgroundBrush = vertical(Color(0xFFFFF7F0), Color(0xFFFFE3D3)),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFF6DCCB),
    ink = Color(0xFF2D1608),
    inkSoft = Color(0xFF5C3B2B),
    callGreen = Color(0xFF15803D),
    youTubeRed = Color(0xFFE0362F),
    cameraPurple = Color(0xFF7C3AED),
    galleryOrange = Color(0xFFC2410C),
    torchOnBg = Color(0xFFFFE08A),
    torchOnBorder = Color(0xFFE0A800),
    torchOnInk = Color(0xFF2D1608),
    lockGrey = Color(0xFFFCE3D3),
    avatarTints = listOf(
        Color(0xFFFFE1CC) to Color(0xFF8A3A00),
        Color(0xFFFDE2E4) to Color(0xFF9F1239),
        Color(0xFFFFEDD5) to Color(0xFF7C2D12),
        Color(0xFFFCE7F3) to Color(0xFF831843),
    ),
    internetOk = LightInternet.first,
    internetWarn = LightInternet.second,
    internetOff = LightInternet.third,
    card = CardStyle(borderWidth = 0.dp, elevation = 8.dp, shadowTint = Color(0xFFC2410C), cornerExtra = 6.dp, gradientChips = true),
    isDark = false,
)

/** Cool sky-to-periwinkle; calm and airy. */
val OceanPalette = Palette(
    background = Color(0xFFEAF4FF),
    backgroundBrush = vertical(Color(0xFFE3F2FF), Color(0xFFEDEBFF)),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFD5E3F5),
    ink = Color(0xFF0B1B33),
    inkSoft = Color(0xFF334766),
    callGreen = Color(0xFF15803D),
    youTubeRed = Color(0xFFDC2626),
    cameraPurple = Color(0xFF4F46E5),
    galleryOrange = Color(0xFF0E7490),
    torchOnBg = Color(0xFFFFE08A),
    torchOnBorder = Color(0xFFE0A800),
    torchOnInk = Color(0xFF0B1B33),
    lockGrey = Color(0xFFDCE9F8),
    avatarTints = listOf(
        Color(0xFFDBEAFE) to Color(0xFF1E3A8A),
        Color(0xFFCFFAFE) to Color(0xFF155E75),
        Color(0xFFE0E7FF) to Color(0xFF3730A3),
        Color(0xFFD1FAE5) to Color(0xFF065F46),
    ),
    internetOk = LightInternet.first,
    internetWarn = LightInternet.second,
    internetOff = LightInternet.third,
    card = CardStyle(borderWidth = 0.dp, elevation = 8.dp, shadowTint = Color(0xFF1D4ED8), cornerExtra = 6.dp, gradientChips = true),
    isDark = false,
)

/** Fresh sage and spring green. */
val MeadowPalette = Palette(
    background = Color(0xFFF1FAF2),
    backgroundBrush = vertical(Color(0xFFF2FBF3), Color(0xFFE6F4DC)),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFD3E8D6),
    ink = Color(0xFF10291A),
    inkSoft = Color(0xFF36513F),
    callGreen = Color(0xFF15803D),
    youTubeRed = Color(0xFFD32F2F),
    cameraPurple = Color(0xFF6D28D9),
    galleryOrange = Color(0xFFB45309),
    torchOnBg = Color(0xFFFFE08A),
    torchOnBorder = Color(0xFFE0A800),
    torchOnInk = Color(0xFF10291A),
    lockGrey = Color(0xFFDDEFE0),
    avatarTints = listOf(
        Color(0xFFDCFCE7) to Color(0xFF14532D),
        Color(0xFFFEF3C7) to Color(0xFF78350F),
        Color(0xFFE0F2FE) to Color(0xFF075985),
        Color(0xFFFCE7F3) to Color(0xFF9D174D),
    ),
    internetOk = LightInternet.first,
    internetWarn = LightInternet.second,
    internetOff = LightInternet.third,
    card = CardStyle(borderWidth = 0.dp, elevation = 8.dp, shadowTint = Color(0xFF166534), cornerExtra = 6.dp, gradientChips = true),
    isDark = false,
)

/** Deep indigo night with vivid accents; dark, with a fine glowing outline instead of shadows. */
val MidnightPalette = Palette(
    background = Color(0xFF0D1230),
    backgroundBrush = vertical(Color(0xFF0B1026), Color(0xFF221A4D)),
    surface = Color(0xFF1A2046),
    border = Color(0xFF39437E),
    ink = Color(0xFFF4F6FF),
    inkSoft = Color(0xFFC5CAEB),
    callGreen = Color(0xFF1E9A52),
    youTubeRed = Color(0xFFE11D48),
    cameraPurple = Color(0xFF7C3AED),
    galleryOrange = Color(0xFFD9480F),
    torchOnBg = Color(0xFF4A3B00),
    torchOnBorder = Color(0xFFF5B700),
    torchOnInk = Color(0xFFFFF4CC),
    lockGrey = Color(0xFF2A3263),
    avatarTints = listOf(
        Color(0xFF2B3A7A) to Color(0xFFE0E7FF),
        Color(0xFF4A2A5E) to Color(0xFFF5D0FE),
        Color(0xFF163E4F) to Color(0xFFCFFAFE),
        Color(0xFF4A2E18) to Color(0xFFFFE4CC),
    ),
    internetOk = DarkInternet.first,
    internetWarn = DarkInternet.second,
    internetOff = DarkInternet.third,
    card = CardStyle(borderWidth = 1.dp, elevation = 0.dp, shadowTint = Color.Black, cornerExtra = 6.dp, gradientChips = true),
    isDark = true,
)

fun AppTheme.palette(): Palette = when (this) {
    AppTheme.Classic -> ClassicPalette
    AppTheme.ClassicDark -> ClassicDarkPalette
    AppTheme.Sunrise -> SunrisePalette
    AppTheme.Ocean -> OceanPalette
    AppTheme.Meadow -> MeadowPalette
    AppTheme.Midnight -> MidnightPalette
}

val LocalPalette = staticCompositionLocalOf { ClassicPalette }

// Token accessors: same names everywhere in the UI, resolved from the active palette.
val Background: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val Border: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.border
val Ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val InkSoft: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkSoft
val CallGreen: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.callGreen
val YouTubeRed: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.youTubeRed
val CameraPurple: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.cameraPurple
val GalleryOrange: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.galleryOrange
val TorchOnBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.torchOnBg
val TorchOnBorder: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.torchOnBorder
val TorchOnInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.torchOnInk
val LockGrey: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.lockGrey

/** Family avatar tints, cycled by position: background to initial-letter color. */
val AvatarTints: List<Pair<Color, Color>> @Composable @ReadOnlyComposable get() = LocalPalette.current.avatarTints

val InternetOk: InternetColors @Composable @ReadOnlyComposable get() = LocalPalette.current.internetOk
val InternetWarn: InternetColors @Composable @ReadOnlyComposable get() = LocalPalette.current.internetWarn
val InternetOff: InternetColors @Composable @ReadOnlyComposable get() = LocalPalette.current.internetOff
