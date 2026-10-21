package com.pntapps.ekchhoan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.ui.theme.Border
import com.pntapps.ekchhoan.ui.theme.ElderSize
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.TorchOnBg
import com.pntapps.ekchhoan.ui.theme.TorchOnInk
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.TorchOnBorder
import com.pntapps.ekchhoan.ui.theme.elderStyle

/**
 * Torch toggle (§4.5): yellow tile with a dark "চালু" pill when on, white tile with a grey "বন্ধ" pill when off.
 * Toggles in place; never opens another screen.
 */
@Composable
fun TorchTile(on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stateText = stringResource(if (on) R.string.home_torch_on else R.string.home_torch_off)
    val label = stringResource(R.string.home_torch)
    TileFrame(
        onClick = onClick,
        // Named for TalkBack even when labels are hidden (icons-only mode).
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
            stateDescription = stateText
        },
        background = if (on) TorchOnBg else Surface,
        borderColor = if (on) TorchOnBorder else Border,
        borderWidth = if (on) 3.dp else LocalPalette.current.card.borderWidth,
        icon = {
            // A dark chip stays dark in dark mode so the white torch icon keeps its contrast.
            IconChip(Icons.Rounded.FlashlightOn, if (LocalPalette.current.isDark) Color(0xFF4B5563) else Ink)
        },
        text = { stacked ->
            Column(
                horizontalAlignment = if (stacked) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.home_torch),
                    style = elderStyle(ElderSize.TileLabel, bold = true),
                    color = if (on) TorchOnInk else Ink,
                )
                Text(
                    text = stateText,
                    style = elderStyle(ElderSize.Pill, bold = true),
                    color = if (on) Surface else Ink,
                    modifier = Modifier
                        .background(if (on) Ink else Border, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        },
    )
}
