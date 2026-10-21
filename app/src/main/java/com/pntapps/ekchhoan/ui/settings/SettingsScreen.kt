package com.pntapps.ekchhoan.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pntapps.ekchhoan.BuildConfig
import com.pntapps.ekchhoan.APP_LANGUAGES
import com.pntapps.ekchhoan.DEFAULT_LANGUAGE
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.data.IconSize
import com.pntapps.ekchhoan.data.MAX_FAMILY_MEMBERS
import com.pntapps.ekchhoan.data.TextSize
import com.pntapps.ekchhoan.system.LaunchResult
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.pntapps.ekchhoan.data.ContactGroup
import com.pntapps.ekchhoan.data.FAMILY_GROUP_ID
import com.pntapps.ekchhoan.data.MAX_GROUPS
import androidx.compose.ui.Alignment
import com.pntapps.ekchhoan.data.HomeItem
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Description

/** Family-only settings (§4.7). Normal Material sizing; the elder never sees this screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditMember: (memberId: String?, groupId: String) -> Unit,
    onChangeLock: () -> Unit,
    onArrangeHome: () -> Unit,
    onClose: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val loaded by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isDefaultHome by remember { mutableStateOf(vm.isDefaultHome()) }
    var confirmDelete by remember { mutableStateOf<FamilyMember?>(null) }
    var confirmDeleteGroup by remember { mutableStateOf<ContactGroup?>(null) }
    /** Group being renamed, or [NEW_GROUP] while creating one. */
    var groupDialog by remember { mutableStateOf<ContactGroup?>(null) }
    var showCallDenied by remember { mutableStateOf(false) }

    val couldNotOpen = stringResource(R.string.set_msg_could_not_open)
    fun report(result: LaunchResult) {
        if (result != LaunchResult.Ok) scope.launch { snackbar.showSnackbar(couldNotOpen) }
    }
    fun startSafely(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            report(LaunchResult.NotInstalled)
        } catch (e: SecurityException) {
            report(LaunchResult.Failed)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isDefaultHome = vm.isDefaultHome() }
    BackHandler(onBack = onClose)

    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setOneTapCall(granted)
        if (!granted) showCallDenied = true
    }
    val homeRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = vm.isDefaultHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.set_action_close))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s: AppSettings = loaded ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            // 1. Contacts: the built-in Family group, then the family's own groups (Friends, Office…)
            section(R.string.settings_family)
            contactGroup(
                group = null,
                members = s.membersOf(FAMILY_GROUP_ID),
                onEditMember = onEditMember,
                vm = vm,
                onRenameGroup = {},
                onDeleteGroup = {},
                onDeleteMember = { confirmDelete = it },
            )
            s.groups.forEach { group ->
                contactGroup(
                    group = group,
                    members = s.membersOf(group.id),
                    onEditMember = onEditMember,
                    vm = vm,
                    onRenameGroup = { groupDialog = it },
                    onDeleteGroup = { confirmDeleteGroup = it },
                    onDeleteMember = { confirmDelete = it },
                )
            }
            item(key = "new-group") {
                Block(top = 12.dp) {
                    val full = s.groups.size >= MAX_GROUPS
                    OutlinedButton(onClick = { groupDialog = NEW_GROUP }, enabled = !full) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.group_new))
                    }
                    Text(
                        stringResource(if (full) R.string.groups_full else R.string.group_new_hint, MAX_GROUPS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 2. One-tap call
            section(R.string.settings_calls)
            item {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
                SwitchRow(
                    title = stringResource(R.string.one_tap_call),
                    subtitle = stringResource(
                        if (s.oneTapCall && !granted) R.string.one_tap_call_no_permission else R.string.one_tap_call_desc,
                    ),
                    checked = s.oneTapCall,
                    onChange = { on ->
                        when {
                            !on -> vm.setOneTapCall(false)
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                                PackageManager.PERMISSION_GRANTED -> vm.setOneTapCall(true)
                            else -> callPermission.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                )
            }

            // 3. Text size
            section(R.string.settings_text_size)
            item {
                val options = listOf(
                    TextSize.Normal to R.string.text_size_normal,
                    TextSize.Large to R.string.text_size_large,
                    TextSize.ExtraLarge to R.string.text_size_extra_large,
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    options.forEachIndexed { i, (size, label) ->
                        SegmentedButton(
                            selected = s.textSize == size,
                            onClick = { vm.setTextSize(size) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                        ) { Text(stringResource(label), maxLines = 2) }
                    }
                }
            }

            // Icons: with or without names, and how big every Home icon and family photo is.
            section(R.string.settings_icons)
            item {
                ChoiceRow(
                    options = listOf(true to R.string.icons_with_text, false to R.string.icons_only),
                    selected = s.showLabels,
                    onSelect = vm::setShowLabels,
                )
            }
            item {
                Text(
                    stringResource(R.string.icon_size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                )
                ChoiceRow(
                    options = listOf(
                        IconSize.Normal to R.string.text_size_normal,
                        IconSize.Large to R.string.text_size_large,
                        IconSize.ExtraLarge to R.string.text_size_extra_large,
                    ),
                    selected = s.iconSize,
                    onSelect = vm::setIconSize,
                )
            }

            // Theme: Classic by default; previews show each theme before choosing.
            section(R.string.settings_theme)
            item { ThemePicker(selected = s.theme, onSelect = vm::setTheme) }

            // 4. Show / hide tiles
            section(R.string.settings_tiles)
            item { SwitchRow(stringResource(R.string.set_tile_youtube), null, s.showYouTube, vm::setShowYouTube) }
            item { SwitchRow(stringResource(R.string.set_tile_camera), null, s.showCamera, vm::setShowCamera) }
            item { SwitchRow(stringResource(R.string.set_tile_gallery), null, s.showGallery, vm::setShowGallery) }
            item { SwitchRow(stringResource(R.string.set_tile_torch), null, s.showTorch, vm::setShowTorch) }
            item { SwitchRow(stringResource(R.string.set_tile_internet), null, s.showInternetBar, vm::setShowInternetBar) }

            // Favourite apps pinned to Home as extra big tiles.
            section(R.string.settings_favorite_apps)
            item { FavoriteAppsBlock(s.favoriteApps, vm) }

            // Home screen order: opens Home as it looks, to rearrange by drag and drop.
            section(R.string.settings_home_order)
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.DragIndicator, contentDescription = null) },
                    title = stringResource(R.string.arrange_open),
                    subtitle = stringResource(R.string.home_order_desc),
                    onClick = onArrangeHome,
                )
            }

            // 5. Language
            section(R.string.settings_language)
            item { LanguagePicker() }

            // 6. Read aloud
            section(R.string.settings_read_aloud)
            item {
                SwitchRow(
                    title = stringResource(R.string.read_aloud),
                    subtitle = stringResource(R.string.read_aloud_desc),
                    checked = s.speakAloud,
                    onChange = { vm.setSpeakAloud(it) },
                )
            }
            if (s.speakAloud) {
                item {
                    val missing by vm.voiceMissing.collectAsState()
                    if (missing) {
                        Block {
                            Text(stringResource(R.string.bengali_voice_missing), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = {
                                startSafely(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                            }) { Text(stringResource(R.string.bengali_voice_install)) }
                        }
                    }
                }
            }

            // 7. Picture lock
            section(R.string.settings_security)
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    title = stringResource(R.string.change_lock),
                    subtitle = stringResource(if (s.lockHash == null) R.string.lock_not_set else R.string.lock_is_set),
                    onClick = onChangeLock,
                )
            }

            // 8. Default home
            section(R.string.settings_home)
            item {
                Block {
                    Text(
                        stringResource(if (isDefaultHome) R.string.set_home_is_default else R.string.set_home_not_default),
                        fontWeight = FontWeight.Medium,
                    )
                    if (isDefaultHome) {
                        OutlinedButton(onClick = { startSafely(vm.homeSettingsIntent()) }) {
                            Icon(Icons.Rounded.Home, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.set_home_switch_back))
                        }
                    } else {
                        Button(onClick = {
                            try {
                                homeRequest.launch(vm.homeRequestIntent())
                            } catch (e: ActivityNotFoundException) {
                                report(LaunchResult.NotInstalled)
                            }
                        }) {
                            Icon(Icons.Rounded.Home, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.set_home_make_default))
                        }
                    }
                }
            }

            // 9. Phone settings, 10. Privacy policy + version
            section(R.string.settings_more)
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    title = stringResource(R.string.open_phone_settings),
                    subtitle = stringResource(R.string.open_phone_settings_desc),
                    onClick = { report(vm.openPhoneSettings()) },
                )
            }

            // About: developer credit and PNT Apps links.
            section(R.string.settings_about)
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Business, contentDescription = null) },
                    title = stringResource(R.string.about_developed_by),
                    subtitle = "pntapps.com",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = { report(vm.openLink(PntAppsLinks.WEBSITE)) },
                )
            }
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    title = stringResource(R.string.about_pnt_apps),
                    subtitle = null,
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = { report(vm.openLink(PntAppsLinks.ABOUT)) },
                )
            }
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Policy, contentDescription = null) },
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.privacy_policy_desc),
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = { report(vm.openLink(PntAppsLinks.PRIVACY)) },
                )
            }
            item {
                ActionRow(
                    icon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                    title = stringResource(R.string.terms_of_use),
                    subtitle = null,
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = { report(vm.openLink(PntAppsLinks.TERMS)) },
                )
            }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.app_version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                )
            }
        }
    }

    confirmDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, member.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(member.id)
                    confirmDelete = null
                }) { Text(stringResource(R.string.set_action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.set_action_cancel)) }
            },
        )
    }

    groupDialog?.let { group ->
        GroupNameDialog(
            initial = if (group === NEW_GROUP) "" else group.name,
            isNew = group === NEW_GROUP,
            onDismiss = { groupDialog = null },
            onSave = { name ->
                if (group === NEW_GROUP) vm.addGroup(name) else vm.renameGroup(group.id, name)
                groupDialog = null
            },
        )
    }

    confirmDeleteGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { confirmDeleteGroup = null },
            title = { Text(stringResource(R.string.group_delete_title)) },
            text = { Text(stringResource(R.string.group_delete_message, group.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGroup(group.id)
                    confirmDeleteGroup = null
                }) { Text(stringResource(R.string.set_action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteGroup = null }) { Text(stringResource(R.string.set_action_cancel)) }
            },
        )
    }

    if (showCallDenied) {
        AlertDialog(
            onDismissRequest = { showCallDenied = false },
            title = { Text(stringResource(R.string.one_tap_call)) },
            text = { Text(stringResource(R.string.one_tap_call_denied)) },
            confirmButton = {
                TextButton(onClick = { showCallDenied = false }) { Text(stringResource(R.string.set_action_ok)) }
            },
        )
    }
}

/** Sentinel for "creating a new group" in the group-name dialog. */
private val NEW_GROUP = ContactGroup(id = "", name = "")

/** One contact group in Settings: its header (rename/delete for custom groups), its contacts and an add button. */
private fun LazyListScope.contactGroup(
    group: ContactGroup?,
    members: List<FamilyMember>,
    onEditMember: (memberId: String?, groupId: String) -> Unit,
    vm: SettingsViewModel,
    onRenameGroup: (ContactGroup) -> Unit,
    onDeleteGroup: (ContactGroup) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
) {
    val groupId = group?.id ?: FAMILY_GROUP_ID
    item(key = "group-$groupId") {
        val name = group?.name ?: stringResource(R.string.home_family_title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (group != null) {
                IconButton(onClick = { onRenameGroup(group) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.group_rename_cd, name))
                }
                IconButton(onClick = { onDeleteGroup(group) }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.group_delete_cd, name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    itemsIndexed(members, key = { _, m -> m.id }) { index, member ->
        FamilyRow(
            member = member,
            index = index,
            isFirst = index == 0,
            isLast = index == members.lastIndex,
            onEdit = { onEditMember(member.id, groupId) },
            onUp = { vm.move(member.id, -1) },
            onDown = { vm.move(member.id, +1) },
            onDelete = { onDeleteMember(member) },
        )
    }
    item(key = "add-$groupId") {
        val full = members.size >= MAX_FAMILY_MEMBERS
        Block {
            if (members.isEmpty()) {
                Text(stringResource(R.string.group_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { onEditMember(null, groupId) }, enabled = !full) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (group == null) stringResource(R.string.family_add) else stringResource(R.string.group_add_contact, group.name),
                )
            }
            if (full) {
                Text(
                    stringResource(R.string.family_full, MAX_FAMILY_MEMBERS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GroupNameDialog(initial: String, isNew: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.group_new else R.string.group_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text(stringResource(R.string.group_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.set_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.set_action_cancel)) } },
    )
}

/** Segmented single choice, full width; labels may wrap to two lines. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
            ) { Text(stringResource(label), maxLines = 2) }
        }
    }
}

private fun LazyListScope.section(@StringRes title: Int) {
    item(contentType = "header") {
        Block(top = 20.dp) {
            HorizontalDivider()
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Block(top: Dp = 4.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = top, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun FamilyRow(
    member: FamilyMember,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onEdit),
        leadingContent = { MemberAvatar(member.name, member.photoPath, index, 48.dp) },
        headlineContent = { Text(member.name, maxLines = 1) },
        supportingContent = {
            Text(
                member.phone.ifBlank { stringResource(R.string.family_no_number) },
                color = if (member.hasValidPhone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onUp, enabled = !isFirst) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.family_move_up, member.name))
                }
                IconButton(onClick = onDown, enabled = !isLast) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.family_move_down, member.name))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.family_delete, member.name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else {
            null
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else {
            null
        },
        trailingContent = trailing,
    )
}

/** English / বাংলা / हिन्दी per-app language; AppCompat recreates the activity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePicker(modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    val current = remember {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        APP_LANGUAGES.firstOrNull { (tag, _) -> tags.startsWith(tag) }?.first ?: DEFAULT_LANGUAGE
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        APP_LANGUAGES.forEachIndexed { i, (tag, label) ->
            SegmentedButton(
                selected = tag == current,
                onClick = {
                    if (tag != current) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = APP_LANGUAGES.size),
            ) { Text(label) }
        }
    }
}
