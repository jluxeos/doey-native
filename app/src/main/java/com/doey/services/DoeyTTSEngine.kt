package com.doey.services

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.doey.DoeyApplication
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

private const val TAG = "DoeyTTSEngine"

private val LOCALE_MAP = mapOf(
    "de_AT" to "de_DE", "de_CH" to "de_DE",
    "en_GB" to "en_US", "en_AU" to "en_US", "en_CA" to "en_US",
    "fr_BE" to "fr_FR", "fr_CA" to "fr_FR",
    "es_AR" to "es_ES", "es_MX" to "es_ES", "es_CO" to "es_ES",
    "pt_BR" to "pt_PT"
)

object DoeyTTSEngine {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pendingCallbacks = ConcurrentHashMap<String, () -> Unit>()
    private var pendingInit: (() -> Unit)? = null

    val onDone = mutableListOf<(String) -> Unit>()
    val onError = mutableListOf<(String) -> Unit>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                setLang("en-US")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(uid: String?) {}
                    override fun onDone(uid: String?) {
                        uid ?: return
                        pendingCallbacks.remove(uid)?.invoke()
                        onDone.forEach { it(uid) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(uid: String?) {
                        uid ?: return
                        pendingCallbacks.remove(uid)?.invoke()
                        onError.forEach { it(uid) }
                    }
                    override fun onError(uid: String?, code: Int) {
                        uid ?: return
                        pendingCallbacks.remove(uid)?.invoke()
                        onError.forEach { it(uid) }
                    }
                })
                pendingInit?.invoke()
                pendingInit = null
                Log.i(TAG, "TTS engine ready")
            }
        }
    }

    suspend fun speakAndWait(text: String, language: String = "en-US") {
        val uid = "tts_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        suspendCancellableCoroutine<Unit> { cont ->
            val doSpeak = {
                setLang(language)
                pendingCallbacks[uid] = { if (cont.isActive) cont.resume(Unit) }
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, uid)
                if (result != TextToSpeech.SUCCESS) {
                    pendingCallbacks.remove(uid)
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            if (isReady) doSpeak() else { pendingInit = doSpeak }
        }
    }

    fun speakAsync(text: String, language: String = "en-US") {
        val doSpeak = {
            setLang(language)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "async_${System.currentTimeMillis()}")
        }
        if (isReady) doSpeak() else { pendingInit = doSpeak }
    }

    fun stop() {
        val callbacks = pendingCallbacks.values.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it() }
        tts?.stop()
    }

    fun isSpeaking() = tts?.isSpeaking ?: false

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private fun setLang(language: String) {
        val norm = language.replace("-", "_")
        val candidates = listOfNotNull(norm, LOCALE_MAP[norm], norm.substringBefore("_").takeIf { it != norm }).distinct()
        for (tag in candidates) {
            val locale = Locale.forLanguageTag(tag.replace("_", "-"))
            val r = tts?.setLanguage(locale)
            if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
                selectBestVoice(locale)
                return
            }
        }
    }

    private fun selectBestVoice(locale: Locale) {
        val best = tts?.voices
            ?.filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            ?.maxByOrNull { it.quality }
            ?: return
        tts?.voice = best
    }

    fun playBeep(count: Int = 1) {
        Thread {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                repeat(count.coerceIn(1, 5)) {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
                    Thread.sleep(600)
                }
                tg.release()
            } catch (e: Exception) {
                Log.e(TAG, "Beep error: ${e.message}")
            }
        }.start()
    }
}
