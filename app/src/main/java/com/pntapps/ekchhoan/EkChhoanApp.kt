package com.pntapps.ekchhoan

import android.app.Application
import android.content.Context
import com.pntapps.ekchhoan.data.PhotoStore
import com.pntapps.ekchhoan.data.SettingsRepository
import com.pntapps.ekchhoan.system.AndroidInternetMonitor
import com.pntapps.ekchhoan.system.AppLauncher
import com.pntapps.ekchhoan.system.CameraTorchController
import com.pntapps.ekchhoan.system.DefaultLauncherHelper
import com.pntapps.ekchhoan.system.InternetMonitor
import com.pntapps.ekchhoan.system.Speaker
import com.pntapps.ekchhoan.system.TorchController
import com.pntapps.ekchhoan.system.TtsSpeaker

/** Manual dependency container; no DI framework. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val settings = SettingsRepository(app)
    val photos = PhotoStore(app)
    val internet: InternetMonitor by lazy { AndroidInternetMonitor(app) }
    val torch: TorchController by lazy { CameraTorchController(app) }
    val launcher = AppLauncher(app)
    val defaultLauncher = DefaultLauncherHelper(app)
    val speaker: Speaker by lazy { TtsSpeaker(app) }

    /** Elapsed-realtime of the last successful picture-lock unlock; settings stay open for 60 s after it. */
    @Volatile var lastUnlockAt: Long = 0L
}

class EkChhoanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as EkChhoanApp).container
