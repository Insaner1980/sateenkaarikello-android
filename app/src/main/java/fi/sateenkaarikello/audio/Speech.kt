package fi.sateenkaarikello.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Puhe suomeksi. [available] on true vain, kun laitteessa on suomenkielinen ääni
 * (setLanguage palauttaa LANG_AVAILABLE tai paremman). Muuten Kuuntele-napit piilotetaan.
 */
class Speech(
    context: Context,
) {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts =
            TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) configure() else Log.i(TAG, "Puhe ei käytössä: $status")
            }
    }

    private fun configure() {
        val engine = tts ?: return
        val result = runCatching { engine.setLanguage(FINNISH) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val ok = result >= TextToSpeech.LANG_AVAILABLE
        if (ok) engine.setSpeechRate(RATE)
        _available.value = ok
    }

    fun speak(text: String) {
        if (!_available.value) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    private companion object {
        const val TAG = "Speech"
        const val RATE = 0.9f
        const val UTTERANCE_ID = "sateenkaarikello"
        val FINNISH: Locale = Locale.forLanguageTag("fi-FI")
    }
}
