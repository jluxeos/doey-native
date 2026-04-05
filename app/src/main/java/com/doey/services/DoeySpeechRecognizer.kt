package com.doey.services

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "DoeySpeech"

private val LOCALE_MAP = mapOf(
    "de_AT" to "de_DE", "de_CH" to "de_DE",
    "en_GB" to "en_US", "en_AU" to "en_US", "en_CA" to "en_US",
    "fr_BE" to "fr_FR", "fr_CA" to "fr_FR",
    "es_AR" to "es_ES", "es_MX" to "es_ES", "es_CO" to "es_ES",
    "pt_BR" to "pt_PT"
)

/** Callbacks for partial results and volume changes (used by UI) */
object DoeySpeechEvents {
    var onPartialResult: ((String) -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null
    var onListeningStarted: (() -> Unit)? = null
    var onListeningStopped: (() -> Unit)? = null
}

/**
 * Standalone speech recognizer using Android's built-in SpeechRecognizer.
 * Must be called from the main thread (SpeechRecognizer requirement).
 */
class DoeySpeechRecognizer(private val context: android.content.Context) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * Listen for speech and return the final transcript.
     * Tries on-device first (API 33+), falls back to cloud.
     * MUST be called from the Main thread.
     */
    suspend fun listen(
        language: String = "en-US",
        mode: String = "auto" // auto | offline | online
    ): String = suspendCancellableCoroutine { cont ->

        recognizer?.destroy()
        recognizer = createRecognizer(mode)

        val intent = buildIntent(language, mode)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                DoeySpeechEvents.onListeningStarted?.invoke()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                DoeySpeechEvents.onVolumeChanged?.invoke(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                DoeySpeechEvents.onListeningStopped?.invoke()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val results = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                results?.firstOrNull()?.let { DoeySpeechEvents.onPartialResult?.invoke(it) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = texts?.firstOrNull() ?: ""
                Log.d(TAG, "STT result: $text")
                if (cont.isActive) cont.resume(text)
            }
            override fun onError(error: Int) {
                val msg = speechErrorToString(error)
                Log.w(TAG, "STT error: $msg (code=$error)")
                // On language/availability errors in auto mode, try to resolve gracefully
                if (cont.isActive) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> cont.resume("")
                        else -> cont.resumeWithException(Exception("STT error: $msg"))
                    }
                }
            }
        })

        recognizer?.startListening(intent)
        Log.d(TAG, "Started listening (lang=$language, mode=$mode)")

        cont.invokeOnCancellation {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer(mode: String): SpeechRecognizer {
        return if (mode == "offline" &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun buildIntent(language: String, mode: String): Intent {
        val norm = language.replace("-", "_")
        val resolvedLang = LOCALE_MAP[norm] ?: norm

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLang.replace("_", "-"))
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLang.replace("_", "-"))
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            if (mode == "offline" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }

    private fun speechErrorToString(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
        else -> "Unknown error ($code)"
    }
}
