package com.pntapps.ekchhoan.system

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.pntapps.ekchhoan.data.FamilyMember

private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
private const val PHOTOS_PACKAGE = "com.google.android.apps.photos"
private const val YOUTUBE_WEB = "https://www.youtube.com"
private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

/** Opens other apps (§4.3, §4.4, §4.6). Never throws; every call reports a [LaunchResult]. */
class AppLauncher(private val context: Context) {

    /** Opens the dialer with the number filled in, or calls directly when one-tap call is on and allowed. */
    fun call(member: FamilyMember, oneTap: Boolean): LaunchResult {
        if (!member.hasValidPhone) return LaunchResult.NoPhoneNumber
        val uri = Uri.fromParts("tel", dialable(member.phone), null)
        if (oneTap && hasCallPermission()) {
            val direct = start(Intent(Intent.ACTION_CALL, uri))
            if (direct == LaunchResult.Ok) return direct
        }
        return start(Intent(Intent.ACTION_DIAL, uri))
    }

    fun openYouTube(): LaunchResult {
        context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE)?.let { app ->
            if (start(app) == LaunchResult.Ok) return LaunchResult.Ok
        }
        return start(Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_WEB)))
    }

    fun openCamera(): LaunchResult = start(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    /** Gallery app, then any image viewer, then Google Photos. */
    fun openGallery(): LaunchResult {
        val selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
        if (start(selector) == LaunchResult.Ok) return LaunchResult.Ok
        if (start(Intent(Intent.ACTION_VIEW).setType("image/*")) == LaunchResult.Ok) return LaunchResult.Ok
        val photos = context.packageManager.getLaunchIntentForPackage(PHOTOS_PACKAGE)
            ?: return LaunchResult.NotInstalled
        return start(photos)
    }

    /** System Internet panel (bottom sheet) on Android 10+, wireless settings below. */
    fun openInternetPanel(): LaunchResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (start(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) == LaunchResult.Ok) return LaunchResult.Ok
        }
        if (start(Intent(Settings.ACTION_WIRELESS_SETTINGS)) == LaunchResult.Ok) return LaunchResult.Ok
        return start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openUrl(url: String): LaunchResult = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun openPhoneSettings(): LaunchResult = start(Intent(Settings.ACTION_SETTINGS))

    /** The installed YouTube app's own icon (never bundled, §5), or null when it is not installed. */
    fun youTubeIcon(): Drawable? = try {
        context.packageManager.getApplicationIcon(YOUTUBE_PACKAGE)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * Apps that appear in the phone's app drawer (via the LAUNCHER `<queries>` entry, not QUERY_ALL_PACKAGES),
     * excluding this app, sorted by name. Call off the main thread.
     */
    fun launchableApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (pkg, _) -> pkg != context.packageName }
            .distinctBy { it.first }
            .map { (pkg, label) -> InstalledApp(pkg, label) }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * The phone's own calling and messaging apps, in suggestion order: phone (dialer), contacts, messages
     * (the default SMS app) and WhatsApp, when installed. Found by role, not by package name, so they work
     * on every brand. Call off the main thread.
     */
    fun communicationApps(): List<String> {
        val pm = context.packageManager
        fun launchable(pkg: String?) = pkg != null && pkg != "android" && pm.getLaunchIntentForPackage(pkg) != null
        fun handler(intent: Intent): String? {
            // resolveActivity returns the system chooser ("android") when there is no default; then take the first app.
            val preferred = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            if (launchable(preferred)) return preferred
            return pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.firstOrNull(::launchable)
        }
        val phone = handler(Intent(Intent.ACTION_DIAL))
        val contacts = handler(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        val messages = Telephony.Sms.getDefaultSmsPackage(context)?.takeIf(::launchable)
            ?: handler(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
        val whatsApp = WHATSAPP_PACKAGES.firstOrNull(::launchable)
        return listOfNotNull(phone, contacts, messages, whatsApp).distinct()
    }

    /** Name and icon of an installed, launchable app, or null when it was uninstalled or disabled. */
    fun appInfo(packageName: String): Pair<String, Drawable>? {
        val pm = context.packageManager
        if (pm.getLaunchIntentForPackage(packageName) == null) return null
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString() to pm.getApplicationIcon(info)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun openApp(packageName: String): LaunchResult {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return LaunchResult.NotInstalled
        return start(intent)
    }

    /** True when YouTube is installed and enabled (a disabled app has no launch intent). */
    fun isYouTubeInstalled(): Boolean = context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE) != null

    private fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun start(intent: Intent): LaunchResult = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        LaunchResult.Ok
    } catch (e: ActivityNotFoundException) {
        LaunchResult.NotInstalled
    } catch (e: SecurityException) {
        LaunchResult.Failed
    }
}

data class InstalledApp(val packageName: String, val label: String)

/** Keeps only what a dialer understands; Bengali digits (০–৯) become ASCII. */
internal fun dialable(phone: String): String = buildString {
    for (c in phone) {
        when {
            c.isDigit() -> append(c.digitToInt())
            c == '+' || c == '*' || c == '#' -> append(c)
        }
    }
}

/** Ignores a second tap within [windowMs] of the last accepted one (elders often double-tap). */
class TapDebouncer(
    private val windowMs: Long = 800,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var lastAccepted: Long? = null

    @Synchronized
    fun tryTap(): Boolean {
        val now = clock()
        val last = lastAccepted
        if (last != null && now - last < windowMs) return false
        lastAccepted = now
        return true
    }
}
