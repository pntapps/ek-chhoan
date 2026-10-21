package com.pntapps.ekchhoan.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.system.InternetState
import com.pntapps.ekchhoan.ui.common.cardShadow
import com.pntapps.ekchhoan.ui.common.elderClickable
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.ElderSize
import com.pntapps.ekchhoan.ui.theme.InternetColors
import com.pntapps.ekchhoan.ui.theme.InternetOff
import com.pntapps.ekchhoan.ui.theme.InternetOk
import com.pntapps.ekchhoan.ui.theme.InternetWarn
import com.pntapps.ekchhoan.ui.theme.elderStyle

/** Everything the bar shows for one [InternetState]: color + icon + words (§4.6), never color alone. */
data class InternetLook(
    val colors: InternetColors,
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
)

@Composable
@ReadOnlyComposable
fun InternetState.look(): InternetLook = when (this) {
    InternetState.WifiOk ->
        InternetLook(InternetOk, Icons.Rounded.Wifi, R.string.home_internet_ok_title, R.string.home_internet_wifi_sub)
    is InternetState.MobileOk -> InternetLook(
        InternetOk,
        Icons.Rounded.SignalCellularAlt,
        R.string.home_internet_ok_title,
        if (wifiEnabled) R.string.home_internet_mobile_sub else R.string.home_internet_mobile_wifi_off_sub,
    )
    InternetState.WifiNoInternet ->
        InternetLook(InternetWarn, Icons.Rounded.Warning, R.string.home_internet_no_net_title, R.string.home_internet_no_net_sub)
    InternetState.Offline ->
        InternetLook(InternetOff, Icons.Rounded.WifiOff, R.string.home_internet_off_title, R.string.home_internet_off_sub)
}

/** Read-only internet status. Tapping opens the system Internet panel only when something is wrong. */
@Composable
fun InternetBar(state: InternetState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val look = state.look()
    val shape = RoundedCornerShape(Dimens.InternetCorner + LocalPalette.current.card.cornerExtra)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.InternetHeight)
            .cardShadow(shape)
            .elderClickable(shape, onClick = onClick)
            .background(look.colors.background, shape)
            .border(Dimens.InternetBorder, look.colors.accent, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(Dimens.InternetIcon).background(look.colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(look.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(look.title), style = elderStyle(ElderSize.InternetTitle, bold = true), color = look.colors.text)
            Text(stringResource(look.subtitle), style = elderStyle(ElderSize.InternetSubtitle), color = look.colors.text)
        }
    }
}
