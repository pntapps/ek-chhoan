package com.pntapps.ekchhoan.system

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Voices to try for each app language, in order of preference (§4.9). English is the last resort. */
private val VOICES = mapOf(
    "bn" to listOf(Locale.forLanguageTag("bn-BD"), Locale.forLanguageTag("bn-IN")),
    "hi" to listOf(Locale.forLanguageTag("hi-IN")),
    "en" to listOf(Locale.forLanguageTag("en-IN"), Locale.ENGLISH),
)

/** The per-app UI language ("en", "bn" or "hi"); English when none is set. */
private fun appLanguage(): String =
    AppCompatDelegate.getApplicationLocales()[0]?.language?.takeIf { it in VOICES } ?: "en"

/**
 * Optional readout via [TextToSpeech] in the app's UI language. The engine is created only when readout
 * is enabled and released when it is disabled. Call from the main thread.
 */
class TtsSpeaker(context: Context) : Speaker {
    private val app = context.applicationContext

    private val _voiceMissing = MutableStateFlow(false)
    override val voiceMissing: StateFlow<Boolean> = _voiceMissing.asStateFlow()

    private var enabled = false
    private var tts: TextToSpeech? = null
    private var ready = false
    /** App language the engine's voice was last chosen for. */
    private var voiceLanguage: String? = null
    /** Latest text asked for before the engine finished starting. */
    private var pending: String? = null
    private var utteranceCounter = 0

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            if (tts == null) start()
        } else {
            shutdown()
        }
    }

    override fun speak(text: String) {
        if (!enabled || text.isBlank()) return
        val engine = tts
        if (engine == null || !ready) {
            pending = text
            return
        }
        // The family may have switched the UI language since the voice was picked.
        if (voiceLanguage != appLanguage()) chooseVoice(engine)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ekchhoan-${utteranceCounter++}")
    }

    override fun shutdown() {
        pending = null
        ready = false
        voiceLanguage = null
        tts?.let {
            it.stop()
            it.shutdown()
        }
        tts = null
    }

    private fun chooseVoice(engine: TextToSpeech) {
        val language = appLanguage()
        val voice = VOICES.getValue(language).firstOrNull { locale ->
            runCatching { engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
        }
        engine.setLanguage(voice ?: Locale.ENGLISH)
        voiceLanguage = language
        _voiceMissing.value = voice == null && language != "en"
    }

    private fun start() {
        ready = false
        var created: TextToSpeech? = null
        created = TextToSpeech(app) { status ->
            // Null when no engine exists and init failed inside the constructor: stay silent.
            val engine = created ?: return@TextToSpeech
            // Ignore a late callback from an engine that was shut down in the meantime.
            if (tts !== engine) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                shutdown()
                return@TextToSpeech
            }
            chooseVoice(engine)
            ready = true
            pending?.let { text ->
                pending = null
                speak(text)
            }
        }
        tts = created
    }
}
