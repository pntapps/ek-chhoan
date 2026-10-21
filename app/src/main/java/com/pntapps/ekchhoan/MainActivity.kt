package com.pntapps.ekchhoan

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pntapps.ekchhoan.data.AppSettings
import com.pntapps.ekchhoan.data.FAMILY_GROUP_ID
import com.pntapps.ekchhoan.ui.home.HomeScreen
import com.pntapps.ekchhoan.ui.lock.PictureLockScreen
import com.pntapps.ekchhoan.ui.lock.SetLockScreen
import com.pntapps.ekchhoan.ui.onboarding.OnboardingScreen
import com.pntapps.ekchhoan.ui.settings.ArrangeHomeScreen
import com.pntapps.ekchhoan.ui.settings.EditMemberScreen
import com.pntapps.ekchhoan.ui.settings.SettingsScreen
import com.pntapps.ekchhoan.ui.theme.Background
import com.pntapps.ekchhoan.ui.theme.EkChhoanTheme
import com.pntapps.ekchhoan.ui.theme.palette
import com.pntapps.ekchhoan.data.AppTheme

object Routes {
    const val HOME = "home"
    const val UNLOCK = "unlock"
    const val SETTINGS = "settings"
    const val EDIT = "edit/{id}?group={group}"
    const val SET_LOCK = "setlock"
    const val ONBOARDING = "onboarding"
    const val ARRANGE = "arrange"
    fun edit(id: String?, group: String) = "edit/${id ?: "new"}?group=$group"
}

/** UI languages the app ships, as (language tag, native name). The first is the default. */
val APP_LANGUAGES = listOf("en" to "English", "bn" to "বাংলা", "hi" to "हिन्दी")
const val DEFAULT_LANGUAGE = "en"

/** Settings stay unlocked this long after a successful picture-lock unlock. */
private const val UNLOCK_GRACE_MS = 60_000L

class MainActivity : AppCompatActivity() {

    /** Bumped whenever the Home button is pressed while we are the home app; Compose pops back to Home. */
    private val homePresses = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // English is the default UI language; the family can switch to বাংলা or हिन्दी in onboarding or Settings.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEFAULT_LANGUAGE))
        }
        enableEdgeToEdge()
        setContent { EkChhoanRoot(homePresses.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }
}

@Composable
private fun EkChhoanRoot(homePresses: Int) {
    val container = androidx.compose.ui.platform.LocalContext.current.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val loaded: AppSettings? = settings

    val palette = (loaded?.theme ?: AppTheme.Classic).palette()
    val dark = palette.isDark
    val activity = LocalActivity.current as? ComponentActivity
    // Status/navigation bar icons follow the app's theme choice, not only the phone's.
    LaunchedEffect(dark, activity) {
        val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    }

    EkChhoanTheme(textScale = loaded?.textSize?.multiplier ?: 1.0f, palette = palette) {
        if (loaded == null) {
            Box(Modifier.fillMaxSize().background(Background))
            return@EkChhoanTheme
        }
        val nav = rememberNavController()
        val start = remember { if (loaded.onboardingDone) Routes.HOME else Routes.ONBOARDING }

        LaunchedEffect(homePresses) {
            if (homePresses > 0 && loaded.onboardingDone) nav.popToHome()
        }

        NavHost(navController = nav, startDestination = start, modifier = Modifier.fillMaxSize().background(Background)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenLock = {
                        val fresh = SystemClock.elapsedRealtime() - container.lastUnlockAt < UNLOCK_GRACE_MS &&
                            container.lastUnlockAt != 0L
                        nav.navigate(if (fresh) Routes.SETTINGS else Routes.UNLOCK) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.UNLOCK) {
                PictureLockScreen(
                    onUnlocked = {
                        container.lastUnlockAt = SystemClock.elapsedRealtime()
                        nav.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME) }
                    },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onEditMember = { id, group -> nav.navigate(Routes.edit(id, group)) },
                    onChangeLock = { nav.navigate(Routes.SET_LOCK) },
                    onArrangeHome = { nav.navigate(Routes.ARRANGE) },
                    onClose = { nav.popToHome() },
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("group") { type = NavType.StringType; defaultValue = FAMILY_GROUP_ID }),
            ) { entry ->
                val id = entry.arguments?.getString("id")?.takeIf { it != "new" }
                val group = entry.arguments?.getString("group") ?: FAMILY_GROUP_ID
                EditMemberScreen(memberId = id, groupId = group, onDone = { nav.popBackStack() })
            }
            composable(Routes.ARRANGE) {
                ArrangeHomeScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.SET_LOCK) {
                SetLockScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                )
            }
        }
    }
}

private fun NavHostController.popToHome() {
    if (!popBackStack(Routes.HOME, inclusive = false)) {
        navigate(Routes.HOME) { popUpTo(0) }
    }
}
