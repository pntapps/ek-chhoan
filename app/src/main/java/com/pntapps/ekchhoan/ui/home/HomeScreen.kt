package com.pntapps.ekchhoan.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.icu.text.DateFormat as IcuDateFormat
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.FAMILY_GROUP_ID
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.data.HomeItem
import com.pntapps.ekchhoan.data.homeItems
import com.pntapps.ekchhoan.system.TorchState
import com.pntapps.ekchhoan.ui.common.FriendlyMessage
import com.pntapps.ekchhoan.ui.theme.Background
import com.pntapps.ekchhoan.ui.theme.CallGreen
import com.pntapps.ekchhoan.ui.theme.CameraPurple
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.ElderSize
import com.pntapps.ekchhoan.ui.theme.GalleryOrange
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InkSoft
import com.pntapps.ekchhoan.ui.theme.HomeIcons
import com.pntapps.ekchhoan.ui.theme.LocalHomeIcons
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.LocalTextScale
import com.pntapps.ekchhoan.ui.theme.LockGrey
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.YouTubeRed
import com.pntapps.ekchhoan.ui.theme.elderStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Holding the lock button this long opens the picture lock (§4.7). */
private const val LOCK_HOLD_MS = 5_000L

/** The one elder-facing screen (§3 "Layout of Home"). Back is swallowed; it never navigates away by itself. */
@Composable
fun HomeScreen(
    onOpenLock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val youTubeIcon by viewModel.youTubeIcon.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    BackHandler {}
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    // Draw nothing until settings, calling apps and favourites are all known, so the layout never shifts.
    val apps = favorites
    if (!state.loaded || apps == null) {
        Box(modifier.fillMaxSize().background(Background))
        return
    }

    val icons = HomeIcons(scale = state.settings.iconSize.multiplier, showLabels = state.settings.showLabels)
    CompositionLocalProvider(LocalHomeIcons provides icons) {
        HomeContent(state, youTubeIcon, apps, viewModel, onOpenLock, modifier)
    }

    message?.let { res ->
        val text = stringResource(res)
        LaunchedEffect(res) { viewModel.say(text) }
        FriendlyMessage(text = text, onDismiss = viewModel::dismissMessage)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    youTubeIcon: ImageBitmap?,
    favorites: List<FavoriteApp>,
    vm: HomeViewModel,
    onOpenLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fits a 6" phone without scrolling; small screens or very large text scroll instead of clipping (§7).
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalPalette.current.backgroundBrush)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
    ) {
        Header(onOpenLock)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // At Extra large text (with or without system font scale) two narrow columns would clip names.
            val effectiveScale = LocalDensity.current.fontScale * LocalTextScale.current
            val columns = if (effectiveScale >= 1.3f && (maxWidth - Dimens.Gap) / 2 < 180.dp) 1 else 2

            val items = state.settings.homeItems(state.communicationApps, torchAvailable = state.torch != TorchState.Unavailable)
            val apps = favorites.associateBy { it.packageName }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                // Consecutive tiles share one two-column grid; contacts and the internet bar span the width.
                splitRuns(items).forEach { run ->
                    when (run.singleOrNull()) {
                        HomeItem.CONTACTS -> ContactSections(state, columns, vm, onOpenLock)
                        HomeItem.INTERNET -> {
                            val look = state.internet.look()
                            val spoken = stringResource(look.title) + ". " + stringResource(look.subtitle)
                            InternetBar(state.internet, onClick = { vm.onInternetTap(spoken) })
                        }
                        else -> TileGrid(run, columns, state, youTubeIcon, apps, vm)
                    }
                }
            }
        }
    }
}

/** Groups [items] into runs: each full-width item alone, consecutive tiles together. */
private fun splitRuns(items: List<String>): List<List<String>> = buildList {
    var tiles = mutableListOf<String>()
    items.forEach { key ->
        if (HomeItem.isFullWidth(key)) {
            if (tiles.isNotEmpty()) add(tiles).also { tiles = mutableListOf() }
            add(listOf(key))
        } else {
            tiles.add(key)
        }
    }
    if (tiles.isNotEmpty()) add(tiles)
}

/**
 * Family first, then each custom group (Friends, Office…) that has contacts, each under its own heading.
 * With no contacts at all, shows the big "add family members" card.
 */
@Composable
private fun ContactSections(state: HomeUiState, columns: Int, vm: HomeViewModel, onOpenLock: () -> Unit) {
    val s = state.settings
    if (s.family.isEmpty()) {
        SectionTitle(stringResource(R.string.home_family_title))
        AddFamilyCard(onClick = onOpenLock, modifier = Modifier.fillMaxWidth())
        return
    }
    val familyTitle = stringResource(R.string.home_family_title)
    val sections = listOf(familyTitle to s.membersOf(FAMILY_GROUP_ID)) +
        s.groups.map { it.name to s.membersOf(it.id) }
    sections.filter { it.second.isNotEmpty() }.forEach { (title, members) ->
        SectionTitle(title)
        ContactGrid(members, columns, vm)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = elderStyle(ElderSize.Section, bold = true),
        color = Ink,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ContactGrid(family: List<FamilyMember>, columns: Int, vm: HomeViewModel) {
    GridRows(family.withIndex().toList(), columns) { (index, member), cellModifier ->
        val spoken = stringResource(R.string.home_call_spoken, member.name)
        FamilyCard(member, index, onClick = { vm.onFamilyTap(member, spoken) }, modifier = cellModifier)
    }
}

@Composable
private fun TileGrid(
    keys: List<String>,
    columns: Int,
    state: HomeUiState,
    youTubeIcon: ImageBitmap?,
    apps: Map<String, FavoriteApp>,
    vm: HomeViewModel,
) {
    // A favourite whose app was uninstalled has no entry in [apps] and is skipped.
    val shown = keys.filter { key -> HomeItem.packageOf(key)?.let { it in apps } ?: true }
    GridRows(shown, columns) { key, cellModifier ->
        when (key) {
            HomeItem.YOUTUBE -> {
                val label = stringResource(R.string.home_youtube)
                AppTile(label, Icons.Rounded.SmartDisplay, YouTubeRed, { vm.onYouTube(label) }, cellModifier, youTubeIcon)
            }
            HomeItem.CAMERA -> {
                val label = stringResource(R.string.home_camera)
                AppTile(label, Icons.Rounded.PhotoCamera, CameraPurple, { vm.onCamera(label) }, cellModifier)
            }
            HomeItem.GALLERY -> {
                val label = stringResource(R.string.home_gallery)
                AppTile(label, Icons.Rounded.Image, GalleryOrange, { vm.onGallery(label) }, cellModifier)
            }
            HomeItem.TORCH -> {
                val on = state.torch == TorchState.On
                val spoken = stringResource(if (on) R.string.home_torch_off_spoken else R.string.home_torch_on_spoken)
                TorchTile(on = on, onClick = { vm.onTorch(spoken) }, modifier = cellModifier)
            }
            else -> {
                val app = apps.getValue(HomeItem.packageOf(key)!!)
                AppTile(
                    label = app.label,
                    icon = Icons.Rounded.Apps,
                    chipColor = CameraPurple,
                    onClick = { vm.onFavorite(app) },
                    modifier = cellModifier,
                    appIcon = app.icon,
                )
            }
        }
    }
}

/** Lays [items] out in rows of [columns]; cells in a row share the tallest cell's height. */
@Composable
private fun <T> GridRows(items: List<T>, columns: Int, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
        items.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
            ) {
                row.forEach { item -> cell(item, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Header(onOpenLock: () -> Unit) {
    val now = rememberNow()
    val locale = LocalConfiguration.current.locales[0]
    val hour = remember(now) { Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY) }
    val date = remember(now, locale) { formatDate(now, locale) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = remember(now, locale, is24Hour) { formatTime(now, locale, is24Hour) }

    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Text(stringResource(greetingFor(hour)), style = elderStyle(ElderSize.Greeting, bold = true), color = Ink)
            Text(time, style = elderStyle(ElderSize.Time, bold = true), color = Ink)
            Text(date, style = elderStyle(ElderSize.Date), color = InkSoft)
        }
        LockButton(onOpenLock)
    }
}

/** Bengali uses bn-BD so weekday, month and digits are Bengali. */
private fun displayLocale(locale: Locale): ULocale =
    ULocale.forLocale(if (locale.language == "bn") Locale.forLanguageTag("bn-BD") else locale)

/** "EEEE, d MMMM", e.g. "মঙ্গলবার, ২১ এপ্রিল". */
private fun formatDate(millis: Long, locale: Locale): String =
    SimpleDateFormat("EEEE, d MMMM", displayLocale(locale)).format(Date(millis))

/** Hours and minutes in the phone's 12/24-hour style, with the locale's own AM/PM words and digits. */
private fun formatTime(millis: Long, locale: Locale, is24Hour: Boolean): String =
    IcuDateFormat.getInstanceForSkeleton(if (is24Hour) "Hm" else "hm", displayLocale(locale)).format(Date(millis))

/** Wall-clock time, refreshed on each minute boundary and when the time, date or timezone changes (§4.2). */
@Composable
private fun rememberNow(): Long {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L + 50L)
        }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

/**
 * 48dp grey lock (§4.7). A tap only shows a tiny "পরিবারের জন্য" hint; holding it 5 seconds fills a ring
 * and then calls [onOpenLock]. Releasing early or sliding off cancels.
 */
@Composable
private fun LockButton(onOpenLock: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ringColor = CallGreen
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var showHint by remember { mutableStateOf(false) }
    val openLock by rememberUpdatedState(onOpenLock)
    val description = stringResource(R.string.home_lock_cd)
    val openLabel = stringResource(R.string.home_lock_open_action)

    Box(
        modifier = Modifier
            .size(Dimens.MinTouch)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { showHint = true; true }
                // TalkBack users can't time a 5 s hold; the picture lock still protects Settings.
                onLongClick(openLabel) { openLock(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fill = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(LOCK_HOLD_MS.toInt(), easing = LinearEasing))
                    }
                    // null = still held when time ran out; true = lifted inside; false = cancelled / slid off.
                    val released = withTimeoutOrNull(LOCK_HOLD_MS) { waitForUpOrCancellation() != null }
                    fill.cancel()
                    when (released) {
                        null -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { progress.snapTo(0f) }
                            openLock()
                            waitForUpOrCancellation()
                        }
                        true -> {
                            showHint = true
                            scope.launch { progress.animateTo(0f, tween(150)) }
                        }
                        false -> scope.launch { progress.animateTo(0f, tween(150)) }
                    }
                }
            }
            .drawBehind {
                val p = progress.value
                if (p > 0f) {
                    val stroke = 5.dp.toPx()
                    val diameter = Dimens.LockButton.toPx() + stroke * 2
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * p,
                        useCenter = false,
                        topLeft = topLeft + Offset(stroke / 2, stroke / 2),
                        size = Size(diameter - stroke, diameter - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(Dimens.LockButton).background(LockGrey, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = InkSoft, modifier = Modifier.size(26.dp))
        }

        if (showHint) {
            val offsetY = with(LocalDensity.current) { Dimens.MinTouch.roundToPx() }
            Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, offsetY), onDismissRequest = { showHint = false }) {
                Text(
                    text = stringResource(R.string.home_lock_hint),
                    style = elderStyle(ElderSize.Min, bold = true),
                    color = Surface,
                    modifier = Modifier
                        .background(Ink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            LaunchedEffect(Unit) {
                delay(2_000)
                showHint = false
            }
        }
    }
}
