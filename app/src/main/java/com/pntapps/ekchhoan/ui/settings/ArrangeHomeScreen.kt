package com.pntapps.ekchhoan.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.HomeItem
import com.pntapps.ekchhoan.data.moveHomeItem
import com.pntapps.ekchhoan.ui.common.FitText
import com.pntapps.ekchhoan.ui.common.cardShadow
import com.pntapps.ekchhoan.ui.common.cardSurface
import com.pntapps.ekchhoan.ui.common.chipBrush
import com.pntapps.ekchhoan.ui.theme.Border
import com.pntapps.ekchhoan.ui.theme.CallGreen
import com.pntapps.ekchhoan.ui.theme.CameraPurple
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.GalleryOrange
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InkSoft
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.YouTubeRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home laid out as it will appear, for the family to rearrange: long-press an item and drag it to a new place.
 * Contacts and the internet bar span the width; everything else is a tile in two columns.
 * Done saves the order; Back or ✕ leaves Home as it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrangeHomeScreen(onDone: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val loaded by vm.settings.collectAsStateWithLifecycle()
    val communication by vm.communicationApps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Working copy; nothing is saved until Done. Built only once the calling apps are known, so phone and
    // messages start right after contacts exactly as on Home.
    var items by rememberSaveable { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(loaded, communication) {
        val s = loaded ?: return@LaunchedEffect
        val talk = communication ?: return@LaunchedEffect
        if (items == null) items = vm.homeItems(s, talk)
    }

    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.arrange_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.set_action_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val order = items ?: return@TextButton
                        scope.launch {
                            vm.saveHomeOrder(order)
                            onDone()
                        }
                    }) { Text(stringResource(R.string.favorite_apps_done), fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        val list = items ?: return@Scaffold
        val grid = rememberLazyGridState()
        var dragging by remember { mutableStateOf<String?>(null) }
        // Where the dragged item started (grid coordinates) and how far the finger has moved since.
        var dragStart by remember { mutableStateOf(Offset.Zero) }
        var dragDelta by remember { mutableStateOf(Offset.Zero) }
        var touchInItem by remember { mutableStateOf(Offset.Zero) }
        // The item just swapped with; ignored until the finger leaves it, so a tile next to a full-width card
        // can't flip back and forth on every move.
        var lastTarget by remember { mutableStateOf<String?>(null) }
        val edge = with(density) { 56.dp.toPx() }

        // The item under [point]; in the gap between columns (where a full-width card's middle lands when dragged
        // straight down) the nearest item in that row counts, so drops are forgiving for unsteady hands.
        fun infoAt(point: Offset): LazyGridItemInfo? = grid.layoutInfo.visibleItemsInfo
            .filter { point.y >= it.offset.y && point.y < it.offset.y + it.size.height }
            .minByOrNull { info ->
                val left = info.offset.x
                val right = left + info.size.width
                when {
                    point.x < left -> left - point.x
                    point.x >= right -> point.x - right
                    else -> 0f
                }
            }

        Column(Modifier.fillMaxSize().background(LocalPalette.current.backgroundBrush).padding(padding)) {
            Text(
                stringResource(R.string.arrange_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = grid,
                // While a card is lifted the grid must not scroll by finger, or it steals the drag; edge
                // auto-scroll below still moves it from code.
                userScrollEnabled = dragging == null,
                // Side margins are outer padding (not contentPadding) so pointer and item coordinates share one origin.
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
                verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { point ->
                                val info = infoAt(point) ?: return@detectDragGesturesAfterLongPress
                                val key = info.key as? String ?: return@detectDragGesturesAfterLongPress
                                if (items?.contains(key) != true) return@detectDragGesturesAfterLongPress
                                dragging = key
                                dragStart = info.offset.toOffset()
                                touchInItem = point - dragStart
                                dragDelta = Offset.Zero
                                lastTarget = null
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                val key = dragging ?: return@detectDragGesturesAfterLongPress
                                dragDelta += amount
                                val finger = dragStart + touchInItem + dragDelta
                                // Near the top or bottom edge, scroll so a card can travel past the visible area.
                                val height = grid.layoutInfo.viewportSize.height
                                if (finger.y < edge) scope.launch { grid.scrollBy(-24f) }
                                if (finger.y > height - edge) scope.launch { grid.scrollBy(24f) }

                                val target = infoAt(finger)?.key as? String
                                if (target != lastTarget) lastTarget = null
                                val current = items ?: return@detectDragGesturesAfterLongPress
                                if (target == null || target == key || target == lastTarget || target !in current) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                val from = current.indexOf(key)
                                val to = current.indexOf(target)
                                // Keep the grid from re-anchoring on the first visible item while it moves.
                                if (from == grid.firstVisibleItemIndex || to == grid.firstVisibleItemIndex) {
                                    grid.requestScrollToItem(grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset)
                                }
                                items = current.toMutableList().apply { add(to, removeAt(from)) }
                                lastTarget = target
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = { dragging = null },
                            onDragCancel = { dragging = null },
                        )
                    },
            ) {
                items(
                    list,
                    key = { it },
                    span = { GridItemSpan(if (HomeItem.isFullWidth(it)) 2 else 1) },
                ) { key ->
                    val isDragged = key == dragging
                    val name = itemName(key, vm)
                    val moveUp = stringResource(R.string.family_move_up, name)
                    val moveDown = stringResource(R.string.family_move_down, name)
                    ArrangeCard(
                        key = key,
                        name = name,
                        vm = vm,
                        lifted = isDragged,
                        modifier = Modifier
                            .then(if (isDragged) Modifier else Modifier.animateItem())
                            .graphicsLayer {
                                if (!isDragged) return@graphicsLayer
                                // Read after layout: start + finger movement, minus where the grid put the card now.
                                val here = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.offset
                                    ?: return@graphicsLayer
                                val shift = dragStart + dragDelta - here.toOffset()
                                translationX = shift.x
                                translationY = shift.y
                                scaleX = 1.04f
                                scaleY = 1.04f
                            }
                            // TalkBack users can't drag: offer move up / move down instead.
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(moveUp) {
                                        items = moveHomeItem(items ?: list, key, -1)
                                        true
                                    },
                                    CustomAccessibilityAction(moveDown) {
                                        items = moveHomeItem(items ?: list, key, +1)
                                        true
                                    },
                                )
                            },
                    )
                }
                item(key = "reset", span = { GridItemSpan(2) }) {
                    TextButton(onClick = { vm.defaultHomeItems()?.let { items = it } }) {
                        Text(stringResource(R.string.home_order_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun itemName(key: String, vm: SettingsViewModel): String {
    val pkg = HomeItem.packageOf(key)
    val appName by produceState<String?>(initialValue = null, pkg) {
        value = pkg?.let { withContext(Dispatchers.Default) { vm.appInfo(it)?.first } }
    }
    return when (key) {
        HomeItem.CONTACTS -> stringResource(R.string.home_order_contacts)
        HomeItem.YOUTUBE -> stringResource(R.string.home_youtube)
        HomeItem.CAMERA -> stringResource(R.string.home_camera)
        HomeItem.GALLERY -> stringResource(R.string.home_gallery)
        HomeItem.TORCH -> stringResource(R.string.home_torch)
        HomeItem.INTERNET -> stringResource(R.string.set_tile_internet)
        else -> appName ?: pkg.orEmpty()
    }
}

/** A Home item drawn as on Home (same card style and icon), with a drag handle; not tappable here. */
@Composable
private fun ArrangeCard(key: String, name: String, vm: SettingsViewModel, lifted: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(Dimens.TileCorner + palette.card.cornerExtra)
    val fullWidth = HomeItem.isFullWidth(key)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (fullWidth) 72.dp else Dimens.TileHeight)
            .then(if (lifted) Modifier.shadow(12.dp, shape) else Modifier.cardShadow(shape))
            .cardSurface(shape, Surface, if (lifted) CallGreen else Border, if (lifted) 3.dp else palette.card.borderWidth)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (key) {
            HomeItem.CONTACTS -> Chip(Icons.Rounded.Group, CallGreen, round = true)
            HomeItem.INTERNET -> Chip(Icons.Rounded.Wifi, CallGreen, round = true)
            HomeItem.YOUTUBE -> Chip(Icons.Rounded.SmartDisplay, YouTubeRed)
            HomeItem.CAMERA -> Chip(Icons.Rounded.PhotoCamera, CameraPurple)
            HomeItem.GALLERY -> Chip(Icons.Rounded.Image, GalleryOrange)
            HomeItem.TORCH -> Chip(Icons.Rounded.FlashlightOn, if (palette.isDark) Color(0xFF4B5563) else Ink)
            else -> Box(Modifier.size(40.dp)) { AppIcon(HomeItem.packageOf(key)!!, vm::appInfo) }
        }
        FitText(
            name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        // The whole card drags; the handle is a hint on wide cards, where there is room for it.
        if (fullWidth) Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = InkSoft)
    }
}

@Composable
private fun Chip(icon: ImageVector, color: Color, round: Boolean = false) {
    val shape = if (round) CircleShape else RoundedCornerShape(10.dp)
    Box(
        Modifier.size(40.dp).background(chipBrush(color), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
