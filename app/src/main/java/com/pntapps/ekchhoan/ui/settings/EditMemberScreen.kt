package com.pntapps.ekchhoan.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.ui.theme.AvatarTints
import kotlinx.coroutines.launch
import com.pntapps.ekchhoan.container
import java.io.File
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pntapps.ekchhoan.data.ContactGroup
import com.pntapps.ekchhoan.data.FAMILY_GROUP_ID

/** Add (memberId == null) or edit one family member: name, phone (typed or picked), photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(memberId: String?, groupId: String, onDone: () -> Unit) {
    val vm: EditMemberViewModel = viewModel(
        key = "edit-${memberId ?: "new"}-$groupId",
        factory = EditMemberViewModel.factory(memberId, groupId),
    )
    val groups by vm.groups.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val noContactsApp = stringResource(R.string.set_msg_no_contacts_app)
    val noPhotoPicker = stringResource(R.string.set_msg_no_photo_picker)
    val photoFailed = stringResource(R.string.set_msg_photo_failed)
    val familyFull = stringResource(R.string.family_full, com.pntapps.ekchhoan.data.MAX_FAMILY_MEMBERS)

    val pickContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        if (uri != null) vm.applyContact(context.contentResolver, uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickPhoto(uri)
    }

    LaunchedEffect(vm.saved) { if (vm.saved) onDone() }
    LaunchedEffect(vm.photoFailed) { if (vm.photoFailed) snackbar.showSnackbar(photoFailed) }
    LaunchedEffect(vm.familyFull) { if (vm.familyFull) snackbar.showSnackbar(familyFull) }

    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.edit_title_add else R.string.edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.set_action_close))
                    }
                },
                actions = {
                    TextButton(onClick = vm::save, enabled = !vm.photoBusy) { Text(stringResource(R.string.set_action_save)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                MemberAvatar(name = vm.name, photoPath = vm.photoPath, tintIndex = 0, size = 120.dp)
                if (vm.photoBusy) CircularProgressIndicator()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !vm.photoBusy,
                    onClick = {
                        try {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (e: ActivityNotFoundException) {
                            scope.launch { snackbar.showSnackbar(noPhotoPicker) }
                        }
                    },
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(if (vm.photoPath == null) R.string.edit_add_photo else R.string.edit_change_photo))
                }
                if (vm.photoPath != null) {
                    TextButton(onClick = vm::removePhoto, enabled = !vm.photoBusy) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.edit_remove_photo))
                    }
                }
            }

            OutlinedTextField(
                value = vm.name,
                onValueChange = vm::onNameChange,
                label = { Text(stringResource(R.string.edit_name)) },
                placeholder = { Text(stringResource(R.string.edit_name_hint)) },
                isError = vm.nameError,
                supportingText = if (vm.nameError) {
                    { Text(stringResource(R.string.edit_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.phone,
                onValueChange = { vm.phone = it },
                label = { Text(stringResource(R.string.edit_phone)) },
                placeholder = { Text("+8801XXXXXXXXX") },
                supportingText = if (vm.phone.isNotBlank() && vm.phone.count { it.isDigit() } < 3) {
                    { Text(stringResource(R.string.edit_phone_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { launchContactPicker(pickContact) { scope.launch { snackbar.showSnackbar(noContactsApp) } } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Contacts, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.edit_pick_contact))
            }
            if (groups.isNotEmpty()) {
                GroupChooser(groups = groups, selected = vm.groupId, onSelect = { vm.groupId = it })
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = vm::save,
                enabled = !vm.photoBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.set_action_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Which group the contact belongs to: Family plus every custom group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChooser(groups: List<ContactGroup>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.edit_group), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf(FAMILY_GROUP_ID to stringResource(R.string.home_family_title)) + groups.map { it.id to it.name }
            options.forEach { (id, name) ->
                FilterChip(
                    selected = id == selected,
                    onClick = { onSelect(id) },
                    label = { Text(name) },
                    leadingIcon = if (id == selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** ACTION_PICK on phone numbers: the picker grants temporary read access to the one chosen row. */
internal class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/** Launches the contact picker, calling [onMissing] on devices without a contacts app. */
internal fun launchContactPicker(launcher: ActivityResultLauncher<Unit>, onMissing: () -> Unit) {
    try {
        launcher.launch(Unit)
    } catch (e: ActivityNotFoundException) {
        onMissing()
    }
}

/** Round photo, or a tinted circle with the first letter of the name. */
@Composable
internal fun MemberAvatar(name: String, photoPath: String?, tintIndex: Int, size: Dp) {
    val (bg, fg) = AvatarTints[Math.floorMod(tintIndex, AvatarTints.size)]
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = LocalContext.current.container.photos.resolve(photoPath) ?: File(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase(),
                color = fg,
                fontWeight = FontWeight.Bold,
                style = if (size >= 96.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            )
        }
    }
}
