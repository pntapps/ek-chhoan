package com.pntapps.ekchhoan.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.AppTheme
import com.pntapps.ekchhoan.ui.common.cardShadow
import com.pntapps.ekchhoan.ui.common.cardSurface
import com.pntapps.ekchhoan.ui.common.chipBrush
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.palette

@StringRes
private fun AppTheme.label(): Int = when (this) {
    AppTheme.Classic -> R.string.theme_classic
    AppTheme.ClassicDark -> R.string.theme_classic_dark
    AppTheme.Sunrise -> R.string.theme_sunrise
    AppTheme.Ocean -> R.string.theme_ocean
    AppTheme.Meadow -> R.string.theme_meadow
    AppTheme.Midnight -> R.string.theme_midnight
}

/** Two-column grid of live previews; each preview is drawn with that theme's own palette and card style. */
@Composable
internal fun ThemePicker(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppTheme.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        selected = theme == selected,
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeOption(theme: AppTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(if (selected) 3.dp else 1.dp, outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            // The preview renders inside the theme it shows.
            CompositionLocalProvider(LocalPalette provides theme.palette()) { MiniHome() }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                        .size(24.dp),
                )
            }
        }
        Text(
            stringResource(theme.label()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** A tiny Home: backdrop, a family card and two tiles with chips, in the current [LocalPalette]. */
@Composable
private fun MiniHome() {
    val p = LocalPalette.current
    val cardShape = RoundedCornerShape(8.dp + p.card.cornerExtra / 2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.backgroundBrush)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(width = 54.dp, height = 8.dp).background(p.ink, RoundedCornerShape(4.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .cardShadow(cardShape)
                    .cardSurface(cardShape, p.surface, p.border, minOf(p.card.borderWidth, 1.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(20.dp).background(p.avatarTints[0].first, CircleShape))
            }
            Box(Modifier.weight(1f).height(38.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(p.youTubeRed, p.cameraPurple).forEach { chip ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .cardShadow(cardShape)
                        .cardSurface(cardShape, p.surface, p.border, minOf(p.card.borderWidth, 1.dp))
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(18.dp).background(chipBrush(chip), RoundedCornerShape(5.dp)))
                    Box(Modifier.size(width = 22.dp, height = 5.dp).background(p.inkSoft, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
