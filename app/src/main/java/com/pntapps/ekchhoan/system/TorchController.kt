package com.pntapps.ekchhoan.system

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Torch via [CameraManager.setTorchMode]; needs no CAMERA permission.
 * State follows [CameraManager.TorchCallback], so the quick-settings torch stays in sync.
 * App-lifetime, so the callback is never unregistered.
 */
class CameraTorchController(context: Context) : TorchController {
    private val app = context.applicationContext
    private val cameraManager: CameraManager? = app.getSystemService(CameraManager::class.java)
    private val cameraId: String? = findFlashCamera()

    private val _state = MutableStateFlow(if (cameraId == null) TorchState.Unavailable else TorchState.Off)
    override val state: StateFlow<TorchState> = _state.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) _state.value = if (enabled) TorchState.On else TorchState.Off
        }

        // Camera is in use by another app: the torch is off for now, but the tile stays visible.
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) _state.value = TorchState.Off
        }
    }

    init {
        if (cameraId != null) {
            runCatching { cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper())) }
        }
    }

    override fun toggle(): Boolean {
        val id = cameraId ?: return false
        val manager = cameraManager ?: return false
        val turnOn = _state.value != TorchState.On
        return try {
            manager.setTorchMode(id, turnOn)
            _state.value = if (turnOn) TorchState.On else TorchState.Off
            true
        } catch (e: CameraAccessException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun findFlashCamera(): String? {
        if (!app.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return null
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
