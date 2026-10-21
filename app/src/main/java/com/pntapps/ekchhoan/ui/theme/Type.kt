@file:OptIn(ExperimentalTextApi::class)

package com.pntapps.ekchhoan.ui.theme

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.pntapps.ekchhoan.R

/**
 * One weight of Atkinson Hyperlegible (Latin) chained per-glyph to bundled Noto Sans Bengali.
 * Neither font covers the other's script and a Compose FontFamily can't merge per glyph, so on API 29+ we build
 * a platform [Typeface.CustomFallbackBuilder] chain; on API 26–28 Bengali falls back to the system Noto Bengali.
 */
private class HyperlegibleBengaliFont(override val weight: FontWeight) :
    AndroidFont(FontLoadingStrategy.Blocking, Loader, FontVariation.Settings()) {

    override val style: FontStyle = FontStyle.Normal

    override fun toString() = "HyperlegibleBengaliFont(${weight.weight})"
}

private object Loader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        val bold = font.weight.weight >= FontWeight.SemiBold.weight
        val latin = if (bold) R.font.atkinson_hyperlegible_bold else R.font.atkinson_hyperlegible_regular
        val bengali = if (bold) R.font.noto_sans_bengali_bold else R.font.noto_sans_bengali_regular
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { merged(context.resources, latin, bengali, if (bold) 700 else 400) }.getOrNull()?.let { return it }
        }
        return ResourcesCompat.getFont(context, latin)
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? = loadBlocking(context, font)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun merged(res: Resources, latin: Int, bengali: Int, weight: Int): Typeface {
    fun family(id: Int) = android.graphics.fonts.FontFamily.Builder(
        android.graphics.fonts.Font.Builder(res, id).setWeight(weight).build(),
    ).build()
    return Typeface.CustomFallbackBuilder(family(latin))
        .addCustomFallback(family(bengali))
        .setStyle(android.graphics.fonts.FontStyle(weight, android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT))
        .setSystemFallback("sans-serif")
        .build()
}

/** Atkinson Hyperlegible + Noto Sans Bengali, Regular (400) and Bold (700); mixed Bengali/English renders correctly. */
val AppFontFamily: FontFamily = FontFamily(
    HyperlegibleBengaliFont(FontWeight.Normal),
    HyperlegibleBengaliFont(FontWeight.Bold),
)

private val Default = Typography()

/** Material type scale with the app font. Elder-facing screens use [elderStyle] instead (spec §3 sizes). */
val AppTypography: Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = Default.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = Default.displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = Default.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = Default.headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = Default.headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = Default.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = Default.titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = Default.titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = Default.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = Default.bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = Default.bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = Default.labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = Default.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = Default.labelSmall.copy(fontFamily = AppFontFamily),
)

/** Base sizes (sp, before the in-app text multiplier) for elder-facing text, spec §3. Nothing below [Min]. */
object ElderSize {
    const val Greeting = 30f
    const val Time = 44f
    const val Date = 19f
    const val Section = 21f
    const val FamilyName = 22f
    const val TileLabel = 20f
    const val InternetTitle = 20f
    const val InternetSubtitle = 16f
    const val Pill = 15f
    const val Min = 15f
}

/** [base] sp multiplied by the in-app text size ([LocalTextScale]); system font scale applies on top. */
@Composable
@ReadOnlyComposable
fun scaledSp(base: Float): TextUnit = (base.coerceAtLeast(ElderSize.Min) * LocalTextScale.current).sp

/** Elder-facing text style at [base] sp, scaled by [LocalTextScale]. */
@Composable
@ReadOnlyComposable
fun elderStyle(base: Float, bold: Boolean = false): TextStyle = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontSize = scaledSp(base),
)
