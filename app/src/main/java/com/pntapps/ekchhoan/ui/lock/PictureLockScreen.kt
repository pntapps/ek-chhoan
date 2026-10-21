package com.pntapps.ekchhoan.ui.lock

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.container
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.PictureLock
import com.pntapps.ekchhoan.data.PictureLock.Picture
import com.pntapps.ekchhoan.ui.theme.Background
import com.pntapps.ekchhoan.ui.theme.Border
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InkSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_TRIES = 5
private const val LOCKOUT_MS = 30_000L

/** Wrong-try counter and lockout; process-wide so leaving and re-opening the lock doesn't reset it. */
private object LockAttempts {
    var failures = 0
    var lockedUntil = 0L
}

/** Unlock screen for family: tap the 3 secret pictures in order. Opens directly when no lock is set yet. */
@Composable
fun PictureLockScreen(onUnlocked: () -> Unit, onCancel: () -> Unit) {
    val container = LocalContext.current.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val loaded: AppSettings? = settings
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val entered = remember { mutableStateListOf<Picture>() }
    var lockedUntil by remember { mutableLongStateOf(LockAttempts.lockedUntil) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var checking by remember { mutableStateOf(false) }

    BackHandler(onBack = onCancel)

    LaunchedEffect(loaded) {
        if (loaded != null && loaded.lockHash == null) onUnlocked()
    }
    LaunchedEffect(lockedUntil) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            if (now >= lockedUntil) break
            delay(200)
        }
    }

    val locked = now < lockedUntil

    LockScaffold(
        title = stringResource(R.string.lock_title),
        subtitle = stringResource(R.string.lock_subtitle),
        onClose = onCancel,
    ) {
        if (locked) {
            LockoutRing(remainingMs = lockedUntil - now)
        } else {
            ProgressDots(count = entered.size)
            Spacer(Modifier.height(24.dp))
            PictureGrid(
                enabled = loaded?.lockHash != null && !checking,
                modifier = Modifier.shakeOffset(shake),
                onPick = { picture ->
                    val s = loaded ?: return@PictureGrid
                    if (entered.size >= PictureLock.SEQUENCE_LENGTH) return@PictureGrid
                    entered.add(picture)
                    if (entered.size < PictureLock.SEQUENCE_LENGTH) return@PictureGrid
                    if (PictureLock.verify(entered.toList(), s.lockSalt, s.lockHash)) {
                        LockAttempts.failures = 0
                        onUnlocked()
                        return@PictureGrid
                    }
                    checking = true
                    LockAttempts.failures++
                    scope.launch {
                        shake.shake()
                        entered.clear()
                        checking = false
                        if (LockAttempts.failures >= MAX_TRIES) {
                            LockAttempts.failures = 0
                            LockAttempts.lockedUntil = SystemClock.elapsedRealtime() + LOCKOUT_MS
                            lockedUntil = LockAttempts.lockedUntil
                        }
                    }
                },
            )
        }
    }
}

/** Full-screen "change picture lock" flow; saves the new lock and calls [onDone]. Back cancels. */
@Composable
fun SetLockScreen(onDone: () -> Unit) {
    val container = LocalContext.current.container
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onDone)
    LockScaffold(
        title = stringResource(R.string.setlock_title),
        subtitle = null,
        onClose = onDone,
    ) {
        SetLockContent(onSet = { hash, salt ->
            scope.launch {
                container.settings.setLock(hash, salt)
                onDone()
            }
        })
    }
}

/** Choose 3 pictures, then repeat them once. A mismatch shakes and starts over. Reused by onboarding. */
@Composable
fun SetLockContent(onSet: (hash: String, salt: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val entered = remember { mutableStateListOf<Picture>() }
    var first by remember { mutableStateOf<List<Picture>?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        val prompt = when {
            mismatch -> stringResource(R.string.setlock_mismatch)
            first == null -> stringResource(R.string.setlock_choose)
            else -> stringResource(R.string.setlock_confirm)
        }
        Text(
            text = prompt,
            style = MaterialTheme.typography.titleMedium,
            color = if (mismatch) MaterialTheme.colorScheme.error else Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(16.dp))
        ProgressDots(count = entered.size)
        Spacer(Modifier.height(24.dp))
        PictureGrid(
            enabled = !busy,
            modifier = Modifier.shakeOffset(shake),
            onPick = { picture ->
                if (entered.size >= PictureLock.SEQUENCE_LENGTH) return@PictureGrid
                mismatch = false
                entered.add(picture)
                if (entered.size < PictureLock.SEQUENCE_LENGTH) return@PictureGrid
                val chosen = entered.toList()
                val firstChoice = first
                busy = true
                scope.launch {
                    when {
                        firstChoice == null -> {
                            delay(250)
                            first = chosen
                        }
                        firstChoice == chosen -> {
                            val salt = PictureLock.newSalt()
                            onSet(PictureLock.hash(chosen, salt), salt)
                        }
                        else -> {
                            shake.shake()
                            first = null
                            mismatch = true
                        }
                    }
                    entered.clear()
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun LockScaffold(title: String, subtitle: String?, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.set_action_close), tint = Ink)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = InkSoft, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        content()
    }
}

/** 3×3 grid of large picture buttons. */
@Composable
internal fun PictureGrid(enabled: Boolean, onPick: (Picture) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Picture.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { picture ->
                    val label = stringResource(picture.label)
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPick(picture)
                        },
                        enabled = enabled,
                        shape = RoundedCornerShape(24.dp),
                        color = picture.color,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .semantics { contentDescription = label },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = picture.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Three dots that fill as pictures are tapped. */
@Composable
private fun ProgressDots(count: Int) {
    val description = stringResource(R.string.lock_progress, count, PictureLock.SEQUENCE_LENGTH)
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(PictureLock.SEQUENCE_LENGTH) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(22.dp)
                    .background(if (filled) Ink else Color.Transparent, CircleShape)
                    .border(3.dp, if (filled) Ink else Border, CircleShape),
            )
        }
    }
}

/** Countdown ring shown during the 30 s lockout after 5 wrong tries. */
@Composable
private fun LockoutRing(remainingMs: Long) {
    val seconds = ((remainingMs + 999) / 1000).toInt().coerceAtLeast(0)
    val fraction = (remainingMs.toFloat() / LOCKOUT_MS).coerceIn(0f, 1f)
    val wait = stringResource(R.string.lock_wait, seconds)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .semantics { contentDescription = wait },
        ) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = InkSoft,
                trackColor = Border,
            )
            Text(
                text = seconds.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(wait, style = MaterialTheme.typography.titleMedium, color = InkSoft, modifier = Modifier.alpha(0.9f))
    }
}

/** Horizontal wiggle used for a wrong sequence instead of an error message. */
private fun Modifier.shakeOffset(offset: Animatable<Float, AnimationVector1D>): Modifier = graphicsLayer {
    translationX = offset.value * density
}

private suspend fun Animatable<Float, AnimationVector1D>.shake() {
    for (x in listOf(18f, -18f, 14f, -14f, 8f, -8f, 0f)) animateTo(x, tween(durationMillis = 45))
}
