package com.pntapps.ekchhoan.ui.home

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.pntapps.ekchhoan.ui.common.toAppIconBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pntapps.ekchhoan.AppContainer
import com.pntapps.ekchhoan.EkChhoanApp
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.system.InternetState
import com.pntapps.ekchhoan.system.LaunchResult
import com.pntapps.ekchhoan.system.TapDebouncer
import com.pntapps.ekchhoan.system.TorchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Greeting string for an hour of day 0–23 (§4.2): 5–11 morning, 12–16 noon, 17–19 evening, else night. */
@StringRes
fun greetingFor(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.home_greeting_morning
    in 12..16 -> R.string.home_greeting_noon
    in 17..19 -> R.string.home_greeting_evening
    else -> R.string.home_greeting_night
}

/** A pinned app as Home shows it; apps that were uninstalled are left out. */
data class FavoriteApp(val packageName: String, val label: String, val icon: ImageBitmap)

data class HomeUiState(
    /** False until settings and the calling apps are known; Home draws nothing before that so nothing shifts. */
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val internet: InternetState = InternetState.Offline,
    val torch: TorchState = TorchState.Off,
    /** Packages of the phone, contacts, messages and WhatsApp apps; placed right after contacts by default. */
    val communicationApps: Set<String> = emptySet(),
) {
    val family: List<FamilyMember> get() = settings.family
}

/**
 * Home screen state and elder actions. Every action is debounced (800 ms), speaks its label when readout
 * is on, and acts at the same time. Failures surface as a one-shot [message] (a string resource id).
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val debouncer = TapDebouncer(800)
    private var speakAloud = false

    /** Null until detected; Home waits for it so tiles never reshuffle under the elder's finger. */
    private val communication = MutableStateFlow<Set<String>?>(null)

    val state: StateFlow<HomeUiState> = combine(
        container.settings.settings,
        container.internet.state,
        container.torch.state,
        communication,
    ) { settings, internet, torch, talk -> HomeUiState(talk != null, settings, internet, torch, talk.orEmpty()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState(internet = container.internet.state.value, torch = container.torch.state.value),
        )

    private val _message = MutableStateFlow<Int?>(null)
    /** String resource of the friendly message to show, or null. Cleared by [dismissMessage]. */
    val message: StateFlow<Int?> = _message.asStateFlow()

    private val _youTubeIcon = MutableStateFlow<ImageBitmap?>(null)
    /** The installed YouTube app's icon, or null (tile then shows a placeholder icon). */
    val youTubeIcon: StateFlow<ImageBitmap?> = _youTubeIcon.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteApp>?>(null)
    /** The family's pinned apps with their current names and icons, in the chosen order; null until first loaded. */
    val favorites: StateFlow<List<FavoriteApp>?> = _favorites.asStateFlow()
    private var favoritePackages: List<String> = emptyList()

    init {
        viewModelScope.launch {
            container.settings.settings.map { it.favoriteApps }.distinctUntilChanged().collect { packages ->
                favoritePackages = packages
                loadFavorites()
            }
        }
        viewModelScope.launch {
            container.settings.settings.map { it.speakAloud }.distinctUntilChanged().collect { on ->
                // Only touch the (lazy) TTS engine once readout has been switched on.
                if (on || speakAloud) container.speaker.setEnabled(on)
                speakAloud = on
            }
        }
    }

    /** Called on every ON_RESUME: re-check the network and pick up YouTube being (un)installed. */
    fun onResume() {
        container.internet.refresh()
        viewModelScope.launch {
            _youTubeIcon.value = withContext(Dispatchers.Default) {
                runCatching { container.launcher.youTubeIcon()?.toAppIconBitmap(ICON_PX) }.getOrNull()
            }
        }
        loadFavorites()
        viewModelScope.launch {
            communication.value = withContext(Dispatchers.Default) {
                runCatching { container.launcher.communicationApps().toSet() }.getOrDefault(emptySet())
            }
        }
    }

    /** Re-reads names and icons (they change on app updates) and drops uninstalled apps. */
    private fun loadFavorites() {
        val packages = favoritePackages
        viewModelScope.launch {
            _favorites.value = withContext(Dispatchers.Default) {
                packages.mapNotNull { pkg ->
                    runCatching {
                        container.launcher.appInfo(pkg)?.let { (label, icon) ->
                            FavoriteApp(pkg, label, icon.toAppIconBitmap(ICON_PX))
                        }
                    }.getOrNull()
                }
            }
        }
    }

    /** Speaks [text] when readout is on. */
    fun say(text: String) {
        if (speakAloud) container.speaker.speak(text)
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun onFamilyTap(member: FamilyMember, spoken: String) {
        if (!debouncer.tryTap()) return
        if (!member.hasValidPhone) {
            show(R.string.home_msg_no_number) // the message itself is spoken
            return
        }
        say(spoken)
        when (container.launcher.call(member, state.value.settings.oneTapCall)) {
            LaunchResult.Ok -> Unit
            LaunchResult.NoPhoneNumber -> show(R.string.home_msg_no_number)
            LaunchResult.NotInstalled, LaunchResult.Failed -> show(R.string.home_msg_call_failed)
        }
    }

    fun onYouTube(spoken: String) = openApp(spoken, R.string.home_msg_youtube_missing) { container.launcher.openYouTube() }

    fun onCamera(spoken: String) = openApp(spoken, R.string.home_msg_camera_failed) { container.launcher.openCamera() }

    fun onGallery(spoken: String) = openApp(spoken, R.string.home_msg_gallery_failed) { container.launcher.openGallery() }

    fun onFavorite(app: FavoriteApp) =
        openApp(app.label, R.string.home_msg_app_missing) { container.launcher.openApp(app.packageName) }

    /** Toggles the torch directly; [spoken] is the label for the new state ("টর্চ চালু" / "টর্চ বন্ধ"). */
    fun onTorch(spoken: String) {
        if (!debouncer.tryTap()) return
        say(spoken)
        if (!container.torch.toggle()) show(R.string.home_msg_torch_busy)
    }

    /** Speaks the status; opens the system Internet panel only when something is wrong (§4.6). */
    fun onInternetTap(spoken: String) {
        if (!debouncer.tryTap()) return
        say(spoken)
        when (container.internet.state.value) {
            InternetState.WifiNoInternet, InternetState.Offline ->
                if (container.launcher.openInternetPanel() != LaunchResult.Ok) show(R.string.home_msg_panel_failed)
            InternetState.WifiOk, is InternetState.MobileOk -> Unit
        }
    }

    private inline fun openApp(spoken: String, @StringRes failure: Int, open: () -> LaunchResult) {
        if (!debouncer.tryTap()) return
        say(spoken)
        if (open() != LaunchResult.Ok) show(failure)
    }

    private fun show(@StringRes res: Int) {
        _message.value = res
    }

    companion object {
        /** Sharp at the Extra large icon size (75dp ≈ 200px on a 420dpi phone). */
        private const val ICON_PX = 256

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as EkChhoanApp).container) }
        }
    }
}
