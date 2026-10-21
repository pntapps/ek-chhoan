package com.pntapps.ekchhoan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Single source of truth for family members and all family-controlled settings. Data never leaves the device. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val onboardingStep = intPreferencesKey("onboarding_step")
        val family = stringPreferencesKey("family_json")
        val groups = stringPreferencesKey("groups_json")
        val oneTapCall = booleanPreferencesKey("one_tap_call")
        val textSize = stringPreferencesKey("text_size")
        val iconSize = stringPreferencesKey("icon_size")
        val showLabels = booleanPreferencesKey("show_labels")
        val themeMode = stringPreferencesKey("theme_mode")
        val favoriteApps = stringPreferencesKey("favorite_apps")
        val homeOrder = stringPreferencesKey("home_order")
        val showYouTube = booleanPreferencesKey("show_youtube")
        val showCamera = booleanPreferencesKey("show_camera")
        val showGallery = booleanPreferencesKey("show_gallery")
        val showTorch = booleanPreferencesKey("show_torch")
        val showInternetBar = booleanPreferencesKey("show_internet_bar")
        val speakAloud = booleanPreferencesKey("speak_aloud")
        val lockHash = stringPreferencesKey("lock_hash")
        val lockSalt = stringPreferencesKey("lock_salt")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val d = AppSettings()
            AppSettings(
                onboardingDone = p[Keys.onboardingDone] ?: d.onboardingDone,
                onboardingStep = p[Keys.onboardingStep] ?: d.onboardingStep,
                family = p[Keys.family]?.let(::decodeFamily) ?: d.family,
                groups = p[Keys.groups]?.let(::decodeGroups) ?: d.groups,
                oneTapCall = p[Keys.oneTapCall] ?: d.oneTapCall,
                textSize = p[Keys.textSize]?.let { runCatching { TextSize.valueOf(it) }.getOrNull() } ?: d.textSize,
                theme = AppTheme.parse(p[Keys.themeMode]),
                iconSize = p[Keys.iconSize]?.let { runCatching { IconSize.valueOf(it) }.getOrNull() } ?: d.iconSize,
                showLabels = p[Keys.showLabels] ?: d.showLabels,
                favoriteApps = p[Keys.favoriteApps]?.split(',')?.filter { it.isNotBlank() } ?: d.favoriteApps,
                homeOrder = p[Keys.homeOrder]?.split(',')?.filter { it.isNotBlank() } ?: d.homeOrder,
                showYouTube = p[Keys.showYouTube] ?: d.showYouTube,
                showCamera = p[Keys.showCamera] ?: d.showCamera,
                showGallery = p[Keys.showGallery] ?: d.showGallery,
                showTorch = p[Keys.showTorch] ?: d.showTorch,
                showInternetBar = p[Keys.showInternetBar] ?: d.showInternetBar,
                speakAloud = p[Keys.speakAloud] ?: d.speakAloud,
                lockHash = p[Keys.lockHash],
                lockSalt = p[Keys.lockSalt],
            )
        }

    fun decodeFamily(raw: String): List<FamilyMember> =
        runCatching { json.decodeFromString<List<FamilyMember>>(raw) }.getOrDefault(emptyList()).sortedBy { it.order }

    fun encodeFamily(list: List<FamilyMember>): String = json.encodeToString(list)

    fun decodeGroups(raw: String): List<ContactGroup> =
        runCatching { json.decodeFromString<List<ContactGroup>>(raw) }.getOrDefault(emptyList())

    /**
     * Replaces every contact; re-numbers `order` to match list position and keeps at most
     * [MAX_FAMILY_MEMBERS] per group.
     */
    suspend fun setFamily(list: List<FamilyMember>) {
        store.edit { it[Keys.family] = encodeFamily(normalize(list)) }
    }

    /** Replaces the custom groups and contacts in one write (a deleted group takes its contacts with it). */
    suspend fun setGroupsAndFamily(groups: List<ContactGroup>, family: List<FamilyMember>) {
        store.edit {
            it[Keys.groups] = json.encodeToString(groups.take(MAX_GROUPS))
            it[Keys.family] = encodeFamily(normalize(family))
        }
    }

    private fun normalize(list: List<FamilyMember>): List<FamilyMember> {
        val perGroup = mutableMapOf<String, Int>()
        return list
            .filter { m -> perGroup.merge(m.groupId, 1, Int::plus)!! <= MAX_FAMILY_MEMBERS }
            .mapIndexed { i, m -> m.copy(order = i) }
    }

    suspend fun setOnboardingDone(done: Boolean) = store.edit { it[Keys.onboardingDone] = done }
    suspend fun setOnboardingStep(step: Int) = store.edit { it[Keys.onboardingStep] = step }
    suspend fun setOneTapCall(enabled: Boolean) = store.edit { it[Keys.oneTapCall] = enabled }
    suspend fun setTextSize(size: TextSize) = store.edit { it[Keys.textSize] = size.name }
    suspend fun setIconSize(size: IconSize) = store.edit { it[Keys.iconSize] = size.name }
    suspend fun setShowLabels(show: Boolean) = store.edit { it[Keys.showLabels] = show }
    suspend fun setTheme(theme: AppTheme) = store.edit { it[Keys.themeMode] = theme.name }

    /** Package names never contain commas, so a comma-joined string is enough. Caps at [MAX_FAVORITE_APPS]. */
    suspend fun setFavoriteApps(packages: List<String>) =
        store.edit { it[Keys.favoriteApps] = packages.distinct().take(MAX_FAVORITE_APPS).joinToString(",") }

    /** Keys never contain commas (package names can't), so a comma-joined string is enough. */
    suspend fun setHomeOrder(keys: List<String>) = store.edit { it[Keys.homeOrder] = keys.joinToString(",") }

    suspend fun setShowYouTube(show: Boolean) = store.edit { it[Keys.showYouTube] = show }
    suspend fun setShowCamera(show: Boolean) = store.edit { it[Keys.showCamera] = show }
    suspend fun setShowGallery(show: Boolean) = store.edit { it[Keys.showGallery] = show }
    suspend fun setShowTorch(show: Boolean) = store.edit { it[Keys.showTorch] = show }
    suspend fun setShowInternetBar(show: Boolean) = store.edit { it[Keys.showInternetBar] = show }
    suspend fun setSpeakAloud(enabled: Boolean) = store.edit { it[Keys.speakAloud] = enabled }

    suspend fun setLock(hash: String, salt: String) = store.edit {
        it[Keys.lockHash] = hash
        it[Keys.lockSalt] = salt
    }
}
