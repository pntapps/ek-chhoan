package com.pntapps.ekchhoan.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.pntapps.ekchhoan.ui.theme.LocalPalette

/**
 * Soft tinted shadow for the modern themes (none in Classic). Chain it *before* [elderClickable],
 * which clips, so the shadow can spread outside the card.
 */
@Composable
fun Modifier.cardShadow(shape: Shape): Modifier {
    val card = LocalPalette.current.card
    if (card.elevation.value <= 0f) return this
    val tint = card.shadowTint.copy(alpha = 0.28f)
    return shadow(card.elevation, shape, clip = false, ambientColor = tint, spotColor = tint)
}

/** Card fill plus the theme's outline (2dp in Classic, hairline in Midnight, none in the light modern themes). */
@Composable
fun Modifier.cardSurface(
    shape: Shape,
    color: Color,
    borderColor: Color,
    borderWidth: Dp = LocalPalette.current.card.borderWidth,
): Modifier {
    val filled = background(color, shape)
    return if (borderWidth.value > 0f) filled.border(borderWidth, borderColor, shape) else filled
}

/** Icon chip fill: flat in Classic, a gentle top-left highlight gradient in the modern themes. */
@Composable
fun chipBrush(color: Color): Brush =
    if (LocalPalette.current.card.gradientChips) {
        Brush.linearGradient(listOf(lerp(color, Color.White, 0.22f), color))
    } else {
        Brush.linearGradient(listOf(color, color))
    }
