package com.pntapps.ekchhoan.ui.settings

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pntapps.ekchhoan.EkChhoanApp
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.data.MAX_FAMILY_MEMBERS
import com.pntapps.ekchhoan.data.MAX_GROUPS
import com.pntapps.ekchhoan.data.TextSize
import com.pntapps.ekchhoan.system.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.graphics.drawable.Drawable
import com.pntapps.ekchhoan.data.AppTheme
import kotlinx.coroutines.flow.map
import com.pntapps.ekchhoan.data.ContactGroup
import com.pntapps.ekchhoan.data.IconSize
import com.pntapps.ekchhoan.system.InstalledApp
import kotlinx.coroutines.flow.MutableStateFlow
import com.pntapps.ekchhoan.data.homeItems
import com.pntapps.ekchhoan.system.TorchState

/** Placeholder privacy policy page; the only place the URL lives. */
/** PNT Apps links shown in Settings → About. The developer credit is required in forks (see NOTICE). */
object PntAppsLinks {
    const val WEBSITE = "https://www.pntapps.com"
    const val ABOUT = "https://www.pntapps.com/about"
    const val PRIVACY = "https://www.pntapps.com/privacy"
    const val TERMS = "https://www.pntapps.com/terms"
}

/** Backs the family-only Settings screen. All writes go through [com.pntapps.ekchhoan.data.SettingsRepository]. */
class SettingsViewModel(app: EkChhoanApp) : ViewModel() {
    private val c = app.container

    val settings: StateFlow<AppSettings?> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Touches the TTS engine, so only read it while read-aloud is on. */
    val voiceMissing: StateFlow<Boolean> get() = c.speaker.voiceMissing

    /** Moves a contact one place up ([delta] = -1) or down (+1) within its own group. */
    fun move(id: String, delta: Int) = editFamily { list ->
        val i = list.indexOfFirst { it.id == id }
        if (i < 0) return@editFamily list
        val group = list[i].groupId
        val indices = if (delta < 0) (i - 1 downTo 0) else (i + 1..list.lastIndex)
        val j = indices.firstOrNull { list[it].groupId == group } ?: return@editFamily list
        list.toMutableList().apply { this[i] = list[j]; this[j] = list[i] }
    }

    fun addGroup(name: String) = viewModelScope.launch {
        val s = c.settings.settings.first()
        if (name.isBlank() || s.groups.size >= MAX_GROUPS) return@launch
        c.settings.setGroupsAndFamily(s.groups + ContactGroup(UUID.randomUUID().toString(), name.trim()), s.family)
    }

    fun renameGroup(id: String, name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val s = c.settings.settings.first()
        c.settings.setGroupsAndFamily(s.groups.map { if (it.id == id) it.copy(name = name.trim()) else it }, s.family)
    }

    /** Deletes a custom group together with its contacts and their photos. */
    fun deleteGroup(id: String) = viewModelScope.launch {
        val s = c.settings.settings.first()
        s.membersOf(id).forEach { c.photos.delete(it.photoPath) }
        c.settings.setGroupsAndFamily(s.groups.filterNot { it.id == id }, s.family.filterNot { it.groupId == id })
    }

    fun delete(id: String) = editFamily { list ->
        list.find { it.id == id }?.let { c.photos.delete(it.photoPath) }
        list.filterNot { it.id == id }
    }

    fun setOneTapCall(on: Boolean) {
        viewModelScope.launch { c.settings.setOneTapCall(on) }
    }
    fun setTextSize(size: TextSize) {
        viewModelScope.launch { c.settings.setTextSize(size) }
    }
    fun setIconSize(size: IconSize) {
        viewModelScope.launch { c.settings.setIconSize(size) }
    }
    fun setShowLabels(show: Boolean) {
        viewModelScope.launch { c.settings.setShowLabels(show) }
    }
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { c.settings.setTheme(theme) }
    }
    private val _communicationApps = MutableStateFlow<Set<String>?>(null)
    /** Phone, contacts, messages and WhatsApp packages (null until detected); they follow contacts by default. */
    val communicationApps: StateFlow<Set<String>?> = _communicationApps

    init {
        viewModelScope.launch {
            _communicationApps.value = withContext(Dispatchers.Default) {
                runCatching { c.launcher.communicationApps().toSet() }.getOrDefault(emptySet())
            }
        }
    }

    /** Home items in display order, as Home shows them. */
    fun homeItems(s: AppSettings, communication: Set<String>): List<String> =
        s.homeItems(communication, torchAvailable = c.torch.state.value != TorchState.Unavailable)

    /** Saves the order arranged by drag and drop. */
    suspend fun saveHomeOrder(keys: List<String>) = c.settings.setHomeOrder(keys)

    /** The default order for the current settings, ignoring any saved arrangement. */
    fun defaultHomeItems(): List<String>? {
        val s = settings.value ?: return null
        val talk = _communicationApps.value ?: return null
        return homeItems(s.copy(homeOrder = emptyList()), talk)
    }

    fun setFavoriteApps(packages: List<String>) {
        viewModelScope.launch { c.settings.setFavoriteApps(packages) }
    }
    /** Apps with a launcher icon, for the favourite-apps picker. */
    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.Default) { c.launcher.launchableApps() }
    /** Name and icon of an installed app, or null if it is gone. Call off the main thread. */
    fun appInfo(packageName: String): Pair<String, Drawable>? = c.launcher.appInfo(packageName)

    fun setShowYouTube(show: Boolean) {
        viewModelScope.launch { c.settings.setShowYouTube(show) }
    }
    fun setShowCamera(show: Boolean) {
        viewModelScope.launch { c.settings.setShowCamera(show) }
    }
    fun setShowGallery(show: Boolean) {
        viewModelScope.launch { c.settings.setShowGallery(show) }
    }
    fun setShowTorch(show: Boolean) {
        viewModelScope.launch { c.settings.setShowTorch(show) }
    }
    fun setShowInternetBar(show: Boolean) {
        viewModelScope.launch { c.settings.setShowInternetBar(show) }
    }

    fun setSpeakAloud(on: Boolean) {
        viewModelScope.launch {
            c.settings.setSpeakAloud(on)
            c.speaker.setEnabled(on)
        }
    }

    fun isDefaultHome(): Boolean = c.defaultLauncher.isDefaultHome()
    fun homeRequestIntent(): Intent = c.defaultLauncher.requestIntent()
    fun homeSettingsIntent(): Intent = c.defaultLauncher.homeSettingsIntent()
    fun openPhoneSettings(): LaunchResult = c.launcher.openPhoneSettings()
    fun openLink(url: String): LaunchResult = c.launcher.openUrl(url)

    private fun editFamily(change: (List<FamilyMember>) -> List<FamilyMember>) = viewModelScope.launch {
        val current = c.settings.settings.first().family
        c.settings.setFamily(change(current))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY] as EkChhoanApp) }
        }
    }
}

/**
 * Draft state for adding or editing one family member. A newly picked photo stays "pending" until
 * [save]; if the screen is left without saving, the pending file is deleted in [onCleared].
 */
class EditMemberViewModel(app: EkChhoanApp, private val memberId: String?, initialGroup: String) : ViewModel() {
    private val c = app.container

    /** Custom groups to choose from (Family is always offered first). */
    val groups: StateFlow<List<ContactGroup>> = c.settings.settings.map { it.groups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val id: String = memberId ?: UUID.randomUUID().toString()
    val isNew: Boolean get() = memberId == null

    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var groupId by mutableStateOf(initialGroup)
    var photoPath by mutableStateOf<String?>(null)
        private set
    var nameError by mutableStateOf(false)
        private set
    var photoBusy by mutableStateOf(false)
        private set
    var photoFailed by mutableStateOf(false)
        private set
    var familyFull by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    private var originalPhoto: String? = null

    init {
        if (memberId != null) viewModelScope.launch {
            c.settings.settings.first().family.find { it.id == memberId }?.let { m ->
                name = m.name
                phone = m.phone
                groupId = m.groupId
                photoPath = m.photoPath
                originalPhoto = m.photoPath
            }
        }
    }

    fun onNameChange(value: String) {
        name = value
        if (value.isNotBlank()) nameError = false
    }

    fun pickPhoto(uri: Uri) = viewModelScope.launch {
        photoBusy = true
        photoFailed = false
        val path = c.photos.importPhoto(uri, id)
        if (path != null) {
            discardPending()
            photoPath = path
        } else {
            photoFailed = true
        }
        photoBusy = false
    }

    fun removePhoto() {
        discardPending()
        photoPath = null
    }

    /** Fills phone (and name, if still empty) from a contact picked with the temporary-grant picker. */
    fun applyContact(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val picked = withContext(Dispatchers.IO) { readPickedContact(resolver, uri) } ?: return@launch
        picked.second?.let { phone = it }
        if (name.isBlank()) picked.first?.let { onNameChange(it) }
    }

    fun save() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        viewModelScope.launch {
            val list = c.settings.settings.first().family
            val index = list.indexOfFirst { it.id == id }
            val existing = list.getOrNull(index)
            // Adding, or moving into another group, needs room in the target group.
            if (existing?.groupId != groupId && list.count { it.groupId == groupId } >= MAX_FAMILY_MEMBERS) {
                familyFull = true
                return@launch
            }
            val updated = if (existing != null) {
                list.toMutableList().apply {
                    this[index] = existing.copy(name = name.trim(), phone = phone.trim(), photoPath = photoPath, groupId = groupId)
                }
            } else {
                list + FamilyMember(id, name.trim(), phone.trim(), photoPath, order = list.size, groupId = groupId)
            }
            c.settings.setFamily(updated)
            if (originalPhoto != photoPath) c.photos.delete(originalPhoto)
            originalPhoto = photoPath
            saved = true
        }
    }

    override fun onCleared() {
        if (!saved) discardPending()
    }

    private fun discardPending() {
        if (photoPath != originalPhoto) c.photos.delete(photoPath)
    }

    companion object {
        fun factory(memberId: String?, groupId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditMemberViewModel(this[APPLICATION_KEY] as EkChhoanApp, memberId, groupId) }
        }
    }
}

/** Reads (display name, number) from a picked Phone row. Never needs READ_CONTACTS: the picker grants this one row. */
internal fun readPickedContact(resolver: ContentResolver, uri: Uri): Pair<String?, String?>? = try {
    resolver.query(
        uri,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val number = cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }
        val name = cursor.getString(1)?.trim()?.takeIf { it.isNotEmpty() }
        name to number
    }
} catch (e: SecurityException) {
    null
} catch (e: IllegalArgumentException) {
    null
}
