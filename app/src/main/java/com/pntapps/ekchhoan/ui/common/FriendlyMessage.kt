package com.pntapps.ekchhoan.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.ui.theme.CallGreen
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InternetWarn
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.elderStyle

/**
 * Big, calm message for when something can't be opened ("YouTube ইনস্টল নেই", "নম্বর নেই", …).
 * One large OK button; tapping outside or Back also closes it. Never a crash, never a tiny toast.
 */
@Composable
fun FriendlyMessage(
    text: String,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Rounded.Info,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier.size(Dimens.MinTouch).background(InternetWarn.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = InternetWarn.accent, modifier = Modifier.size(42.dp))
            }
            Text(
                text = text,
                style = elderStyle(24f, bold = true),
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            val shape = RoundedCornerShape(Dimens.TileCorner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouch)
                    .elderClickable(shape, onClick = onDismiss)
                    .background(CallGreen, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.home_msg_ok), style = elderStyle(22f, bold = true), color = Surface)
            }
        }
    }
}
