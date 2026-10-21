package com.pntapps.ekchhoan.data

import kotlinx.serialization.Serializable

/** Most contacts in one group (Family or a custom group). */
const val MAX_FAMILY_MEMBERS = 6

/** Most custom groups besides the built-in Family group. */
const val MAX_GROUPS = 5

/** Id of the built-in, undeletable Family group. Contacts saved before groups existed belong to it. */
const val FAMILY_GROUP_ID = "family"

/** A custom contact group such as "Friends" or "Office", shown as its own section on Home. */
@Serializable
data class ContactGroup(val id: String, val name: String)

@Serializable
data class FamilyMember(
    val id: String,          // UUID
    val name: String,        // e.g. "ছেলে" or "রহিম"
    val phone: String,       // e.g. "+8801712345678"
    val photoPath: String?,  // absolute path inside filesDir, or null
    val order: Int,
    val groupId: String = FAMILY_GROUP_ID,
) {
    /** True when the number has at least 3 dialable digits. */
    val hasValidPhone: Boolean get() = phone.count { it.isDigit() } >= 3
}

enum class TextSize(val multiplier: Float) { Normal(1.0f), Large(1.15f), ExtraLarge(1.3f) }

/** Size of every Home icon: app chips, app icons and family photos. */
enum class IconSize(val multiplier: Float) { Normal(1.0f), Large(1.25f), ExtraLarge(1.5f) }

/** Look of the whole app. Classic (the original light design) is the default; the rest are opt-in. */
enum class AppTheme {
    Classic, ClassicDark, Sunrise, Ocean, Meadow, Midnight;

    companion object {
        /** Parses a stored name; also maps the earlier Light/Dark/System setting. */
        fun parse(name: String?): AppTheme = when (name) {
            null, "Light", "System" -> Classic
            "Dark" -> ClassicDark
            else -> entries.firstOrNull { it.name == name } ?: Classic
        }
    }
}

/** Most favourite apps the family can pin to Home. */
const val MAX_FAVORITE_APPS = 8

/** Everything the family configures. Persisted by [SettingsRepository]. */
data class AppSettings(
    val onboardingDone: Boolean = false,
    /** Onboarding step reached so far; survives the activity being re-created in the home task. */
    val onboardingStep: Int = 0,
    /** Every contact in every group, in display order. */
    val family: List<FamilyMember> = emptyList(),
    /** Custom groups in display order; Family is built in and not listed here. */
    val groups: List<ContactGroup> = emptyList(),
    val oneTapCall: Boolean = false,
    val textSize: TextSize = TextSize.Normal,
    val iconSize: IconSize = IconSize.Normal,
    /** False = icons only on Home (names stay available to TalkBack). */
    val showLabels: Boolean = true,
    val theme: AppTheme = AppTheme.Classic,
    /** Package names of apps pinned to Home, in display order. */
    val favoriteApps: List<String> = emptyList(),
    /** Home item keys ([HomeItem]) in the order the family arranged; empty means the default order. */
    val homeOrder: List<String> = emptyList(),
    val showYouTube: Boolean = true,
    val showCamera: Boolean = true,
    val showGallery: Boolean = true,
    val showTorch: Boolean = true,
    val showInternetBar: Boolean = true,
    val speakAloud: Boolean = false,
    /** Hex SHA-256 of salt + picture sequence, or null when no lock is set yet. */
    val lockHash: String? = null,
    /** Hex random salt for [lockHash]. */
    val lockSalt: String? = null,
) {
    fun membersOf(groupId: String): List<FamilyMember> = family.filter { it.groupId == groupId }
}
