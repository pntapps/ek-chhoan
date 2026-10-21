package com.pntapps.ekchhoan.ui.home

import androidx.compose.foundation.Image
import com.pntapps.ekchhoan.ui.theme.LocalHomeIcons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pntapps.ekchhoan.ui.common.cardShadow
import com.pntapps.ekchhoan.ui.common.cardSurface
import com.pntapps.ekchhoan.ui.common.chipBrush
import com.pntapps.ekchhoan.ui.common.elderClickable
import com.pntapps.ekchhoan.ui.common.FitText
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.Border
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.ElderSize
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.elderStyle

/**
 * Big app button (§3, §4.4): colored icon chip with a white icon and a bold label.
 * When [appIcon] is given (the real installed app icon) it replaces the chip.
 */
@Composable
fun AppTile(
    label: String,
    icon: ImageVector,
    chipColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appIcon: ImageBitmap? = null,
) {
    TileFrame(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
        icon = {
            if (appIcon != null) {
                // Same size and shape as the colored chips; the hairline keeps white icons visible on white tiles.
                val scale = LocalHomeIcons.current.scale
                val shape = RoundedCornerShape(Dimens.IconChipCorner * scale)
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimens.IconChip * scale)
                        .clip(shape)
                        .border(1.dp, Border, shape),
                )
            } else {
                IconChip(icon, chipColor)
            }
        },
        text = { stacked ->
            FitText(
                label,
                style = elderStyle(ElderSize.TileLabel, bold = true),
                color = Ink,
                textAlign = if (stacked) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.clearAndSetSemantics {},
            )
        },
    )
}

/**
 * Shared tile shell: min 84dp tall, themed corners, pressed scale + haptic, role Button.
 * Icon beside the label normally; icon above the label (or alone) when [HomeIcons.stacked].
 */
@Composable
internal fun TileFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Surface,
    borderColor: Color = Border,
    borderWidth: Dp = LocalPalette.current.card.borderWidth,
    icon: @Composable () -> Unit,
    text: @Composable (stacked: Boolean) -> Unit,
) {
    val icons = LocalHomeIcons.current
    val shape = RoundedCornerShape(Dimens.TileCorner + LocalPalette.current.card.cornerExtra)
    val frame = modifier
        .heightIn(min = Dimens.TileHeight)
        .cardShadow(shape)
        .elderClickable(shape, onClick = onClick)
        .cardSurface(shape, background, borderColor, borderWidth)
        .padding(horizontal = 12.dp, vertical = 10.dp)
    if (icons.stacked) {
        Column(
            modifier = frame,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            icon()
            if (icons.showLabels) text(true)
        }
    } else {
        Row(
            modifier = frame,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Box(Modifier.weight(1f)) { text(false) }
        }
    }
}

/** Colored rounded square with a white icon, sized by the Icon size setting. */
@Composable
internal fun IconChip(icon: ImageVector, color: Color) {
    val scale = LocalHomeIcons.current.scale
    Box(
        modifier = Modifier
            .size(Dimens.IconChip * scale)
            .background(chipBrush(color), RoundedCornerShape(Dimens.IconChipCorner * scale)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp * scale))
    }
}
