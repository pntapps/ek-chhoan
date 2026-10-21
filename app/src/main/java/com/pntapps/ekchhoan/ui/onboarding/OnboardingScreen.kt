package com.pntapps.ekchhoan.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.container
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.data.FAMILY_GROUP_ID
import com.pntapps.ekchhoan.data.MAX_FAMILY_MEMBERS
import com.pntapps.ekchhoan.ui.lock.SetLockContent
import com.pntapps.ekchhoan.ui.settings.LanguagePicker
import com.pntapps.ekchhoan.ui.settings.PickPhoneContact
import com.pntapps.ekchhoan.ui.settings.launchContactPicker
import com.pntapps.ekchhoan.ui.settings.readPickedContact
import com.pntapps.ekchhoan.ui.theme.Background
import com.pntapps.ekchhoan.ui.theme.CallGreen
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InkSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.pntapps.ekchhoan.data.MAX_FAVORITE_APPS
import com.pntapps.ekchhoan.ui.settings.AppIcon
import com.pntapps.ekchhoan.ui.settings.AppPickerDialog
import com.pntapps.ekchhoan.ui.theme.CameraPurple
import com.pntapps.ekchhoan.ui.theme.GalleryOrange
import com.pntapps.ekchhoan.ui.theme.YouTubeRed

private enum class Step { Welcome, Family, Call, Apps, Lock, Home, Done }

/** First-run setup done by a family member (§4.8): one big button per step. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val container = LocalContext.current.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var restored by rememberSaveable { mutableStateOf(false) }
    // Kept here, not inside the step, so going Back to the apps step keeps the family's ticks.
    var appsChosen by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val step = Step.entries[stepIndex]
    // Advances only from the step this composition showed, so a double tap can never skip a step (e.g. the lock).
    val next = { if (stepIndex == step.ordinal) stepIndex = (step.ordinal + 1).coerceAtMost(Step.entries.lastIndex) }

    // Becoming the home app starts a fresh activity in the home task; resume where setup left off.
    LaunchedEffect(Unit) {
        if (!restored) {
            val saved = container.settings.settings.first().onboardingStep
            stepIndex = maxOf(stepIndex, saved.coerceIn(0, Step.entries.lastIndex))
            restored = true
        }
    }
    LaunchedEffect(stepIndex, restored) {
        if (restored) container.settings.setOnboardingStep(stepIndex)
    }

    BackHandler(enabled = stepIndex > 0) { stepIndex-- }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (stepIndex + 1f) / Step.entries.size },
            modifier = Modifier.fillMaxWidth(),
            color = CallGreen,
        )
        Text(
            stringResource(R.string.onboarding_step, stepIndex + 1, Step.entries.size),
            style = MaterialTheme.typography.labelLarge,
            color = InkSoft,
        )
        when (step) {
            Step.Welcome -> WelcomeStep(onNext = next)
            Step.Family -> FamilyStep(settings, onNext = next)
            Step.Call -> CallStep(oneTapCall = settings?.oneTapCall == true, onNext = next)
            Step.Apps -> settings?.let { AppsStep(it, appsChosen, { appsChosen = it }, onNext = next) }
            Step.Lock -> {
                Title(stringResource(R.string.onboarding_lock_title))
                Body(stringResource(R.string.onboarding_lock_body))
                SetLockContent(onSet = { hash, salt ->
                    scope.launch {
                        container.settings.setLock(hash, salt)
                        next()
                    }
                })
                if (settings?.lockHash != null) {
                    BigButton(stringResource(R.string.onboarding_keep_lock), onClick = next)
                } else {
                    SkipButton(onClick = next)
                }
            }
            Step.Home -> HomeStep(onNext = next)
            Step.Done -> {
                Title(stringResource(R.string.onboarding_done_title))
                Body(stringResource(R.string.onboarding_done_body))
                            BigButton(stringResource(R.string.onboarding_start), onClick = {
                    scope.launch {
                        container.settings.setOnboardingDone(true)
                        onFinished()
                    }
                })
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    // Language first, so the rest of setup is in the family's language (English by default).
    LanguagePicker(modifier = Modifier)
    Title(stringResource(R.string.onboarding_welcome_title))
    Body(stringResource(R.string.onboarding_welcome_body))
    BigButton(stringResource(R.string.onboarding_begin), onClick = onNext)
}

/** Inline mini-form; photos can be added later in Settings. */
@Composable
private fun FamilyStep(settings: AppSettings?, onNext: () -> Unit) {
    val container = LocalContext.current.container
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var noContacts by remember { mutableStateOf(false) }
    val family = settings?.membersOf(FAMILY_GROUP_ID).orEmpty()
    val full = family.size >= MAX_FAMILY_MEMBERS

    val pickContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedContact(context.contentResolver, uri) }
            picked?.second?.let { phone = it }
            if (name.isBlank()) picked?.first?.let { name = it; nameError = false }
        }
    }

    Title(stringResource(R.string.onboarding_family_title))
    Body(stringResource(R.string.onboarding_family_body))

    family.forEach { m ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = CallGreen)
            Spacer(Modifier.size(8.dp))
            Text("${m.name}  ${m.phone}", style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
    }

    if (!full) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
            label = { Text(stringResource(R.string.edit_name)) },
            placeholder = { Text(stringResource(R.string.edit_name_hint)) },
            isError = nameError,
            supportingText = if (nameError) {
                { Text(stringResource(R.string.edit_name_required)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.edit_phone)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { launchContactPicker(pickContact) { noContacts = true } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Contacts, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.edit_pick_contact))
        }
        if (noContacts) Body(stringResource(R.string.set_msg_no_contacts_app))
        OutlinedButton(
            onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    val member = FamilyMember(UUID.randomUUID().toString(), name.trim(), phone.trim(), null, family.size)
                    name = ""
                    phone = ""
                    scope.launch {
                        val current = container.settings.settings.first().family
                        container.settings.setFamily(current + member)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.onboarding_add_member)) }
    } else {
        Body(stringResource(R.string.family_full, MAX_FAMILY_MEMBERS))
    }

    BigButton(
        stringResource(if (family.isEmpty()) R.string.onboarding_skip else R.string.onboarding_next),
        onClick = onNext,
    )
}

/**
 * Suggests the phone's own calling and messaging apps (phone, contacts, messages, WhatsApp) plus the built-in
 * YouTube, Camera, Photos and Torch tiles, ticked by default; "More apps…" opens the searchable app list.
 */
@Composable
private fun AppsStep(settings: AppSettings, chosen: List<String>?, onChosen: (List<String>) -> Unit, onNext: () -> Unit) {
    val container = LocalContext.current.container
    val launcher = container.launcher
    val scope = rememberCoroutineScope()
    val suggested by produceState<List<String>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { runCatching { launcher.communicationApps() }.getOrDefault(emptyList()) }
    }
    var saving by remember { mutableStateOf(false) }
    var youTube by rememberSaveable { mutableStateOf(settings.showYouTube) }
    var camera by rememberSaveable { mutableStateOf(settings.showCamera) }
    var gallery by rememberSaveable { mutableStateOf(settings.showGallery) }
    var torch by rememberSaveable { mutableStateOf(settings.showTorch) }
    var picking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(suggested) {
        val list = suggested ?: return@LaunchedEffect
        // Ticked apps start from what is already saved (resuming setup) or else every suggestion.
        if (chosen == null) onChosen(settings.favoriteApps.ifEmpty { list })
    }

    Title(stringResource(R.string.onboarding_apps_title))
    Body(stringResource(R.string.onboarding_apps_body))
    val picked = chosen
    val list = suggested
    if (picked == null || list == null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column {
        // Suggested apps first, then anything picked from "More apps…", each with its real icon and name.
        (list + picked.filterNot { it in list }).forEach { pkg ->
            val name by produceState<String?>(initialValue = null, pkg) {
                value = withContext(Dispatchers.Default) { launcher.appInfo(pkg)?.first }
            }
            CheckRow(
                checked = pkg in picked,
                title = name ?: pkg,
                leading = { AppIcon(pkg, launcher::appInfo) },
                onChange = { on -> onChosen(if (on) (picked + pkg).distinct().take(MAX_FAVORITE_APPS) else picked - pkg) },
            )
        }
        if (list.isEmpty() && picked.isEmpty()) Body(stringResource(R.string.onboarding_apps_none))
        CheckRow(youTube, stringResource(R.string.set_tile_youtube), { BuiltInIcon(Icons.Rounded.SmartDisplay, YouTubeRed) }) { youTube = it }
        CheckRow(camera, stringResource(R.string.set_tile_camera), { BuiltInIcon(Icons.Rounded.PhotoCamera, CameraPurple) }) { camera = it }
        CheckRow(gallery, stringResource(R.string.set_tile_gallery), { BuiltInIcon(Icons.Rounded.Image, GalleryOrange) }) { gallery = it }
        CheckRow(torch, stringResource(R.string.set_tile_torch), { BuiltInIcon(Icons.Rounded.FlashlightOn, Ink) }) { torch = it }
    }
    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Apps, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.onboarding_apps_more))
    }
    BigButton(stringResource(R.string.onboarding_next), onClick = {
        // One save at a time: a quick double tap must not run onNext twice and skip the lock step.
        if (saving) return@BigButton
        saving = true
        scope.launch {
            container.settings.setFavoriteApps(picked)
            container.settings.setShowYouTube(youTube)
            container.settings.setShowCamera(camera)
            container.settings.setShowGallery(gallery)
            container.settings.setShowTorch(torch)
            onNext()
        }
    })
    if (picking) {
        AppPickerDialog(
            initial = picked,
            loadApps = { withContext(Dispatchers.Default) { launcher.launchableApps() } },
            appInfo = launcher::appInfo,
            onDismiss = { picking = false },
            onDone = {
                onChosen(it)
                picking = false
            },
        )
    }
}

@Composable
private fun CheckRow(checked: Boolean, title: String, leading: @Composable () -> Unit, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading()
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun BuiltInIcon(icon: ImageVector, color: Color) {
    Box(Modifier.size(40.dp).background(color, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** Direct calling needs CALL_PHONE, asked here while the family member is setting up (never on Home). */
@Composable
private fun CallStep(oneTapCall: Boolean, onNext: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    var denied by remember { mutableStateOf(false) }
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED
    val enable = {
        scope.launch {
            container.settings.setOneTapCall(true)
            onNext()
        }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) enable() else denied = true
    }

    Title(stringResource(R.string.onboarding_call_title))
    Body(stringResource(R.string.onboarding_call_body))
    if (oneTapCall && granted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CallGreen, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(8.dp))
            Body(stringResource(R.string.onboarding_call_on))
        }
        BigButton(stringResource(R.string.onboarding_next), onClick = onNext)
    } else {
        if (denied) Body(stringResource(R.string.one_tap_call_denied))
        BigButton(stringResource(R.string.onboarding_call_allow), onClick = {
            if (granted) enable() else request.launch(Manifest.permission.CALL_PHONE)
        })
        SkipButton(onClick = onNext)
    }
}

@Composable
private fun HomeStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val helper = context.container.defaultLauncher
    var isDefault by remember { mutableStateOf(helper.isDefaultHome()) }
    var failed by remember { mutableStateOf(false) }
    // Once we hold the home role, setup continues in the home-task instance; close this app-drawer task.
    val recheck = {
        isDefault = helper.isDefaultHome()
        val activity = context.findActivity()
        if (isDefault && activity != null && !activity.intent.hasCategory(Intent.CATEGORY_HOME)) {
            activity.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            activity.finishAndRemoveTask()
        }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { recheck() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { recheck() }

    Title(stringResource(R.string.onboarding_home_title))
    if (isDefault) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CallGreen, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(8.dp))
            Body(stringResource(R.string.onboarding_home_already))
        }
        BigButton(stringResource(R.string.onboarding_next), onClick = onNext)
    } else {
        Body(stringResource(R.string.onboarding_home_body))
        if (failed) Body(stringResource(R.string.set_msg_could_not_open))
        BigButton(stringResource(R.string.set_home_make_default), onClick = {
            try {
                request.launch(helper.requestIntent())
            } catch (e: ActivityNotFoundException) {
                failed = true
            }
        })
        SkipButton(onClick = onNext)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = InkSoft)
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_skip))
    }
}
