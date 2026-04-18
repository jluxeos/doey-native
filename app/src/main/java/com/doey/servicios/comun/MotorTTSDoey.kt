package com.doey.servicios.comun

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.doey.AplicacionDoey
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

/** Removes emoji and other non-speakable Unicode symbols from TTS input. */
private fun stripEmojis(text: String): String {
    // Remove emoji using Unicode block ranges
    val emojiRegex = Regex(
        "[\u00A9\u00AE\u203C\u2049\u20E3\u2122\u2139\u2194-\u2199\u21A9\u21AA" +
        "\u231A\u231B\u23CF\u23E9-\u23F3\u23F8-\u23FA\u24C2\u25AA\u25AB\u25B6" +
        "\u25C0\u25FB-\u25FE\u2600-\u2604\u260E\u2611\u2614\u2615\u2618\u261D" +
        "\u2620\u2622\u2623\u2626\u262A\u262E\u262F\u2638-\u263A\u2640\u2642" +
        "\u2648-\u2653\u265F\u2660\u2663\u2665\u2666\u2668\u267B\u267E\u267F" +
        "\u2692-\u2697\u2699\u269B\u269C\u26A0\u26A1\u26AA\u26AB\u26B0\u26B1" +
        "\u26BD\u26BE\u26C4\u26C5\u26CE\u26CF\u26D1\u26D3\u26D4\u26E9\u26EA" +
        "\u26F0-\u26F5\u26F7-\u26FA\u26FD\u2702\u2705\u2708-\u270D\u270F\u2712" +
        "\u2714\u2716\u271D\u2721\u2728\u2733\u2734\u2744\u2747\u274C\u274E" +
        "\u2753-\u2755\u2757\u2763\u2764\u2795-\u2797\u27A1\u27B0\u27BF\u2934" +
        "\u2935\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D\u3297\u3299" +
        "]|[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u200D\uFE0F\u20E3]"
    )
    return emojiRegex.replace(text, "").replace(Regex("\\s{2,}"), " ").trim()
}

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
        val clean = stripEmojis(text)
        if (clean.isBlank()) return
        val uid = "tts_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        suspendCancellableCoroutine<Unit> { cont ->
            val doSpeak: () -> Unit = {   // ✅ FIX AQUÍ
                setLang(language)
                pendingCallbacks[uid] = { if (cont.isActive) cont.resume(Unit) }
                val result = tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, uid)
                if (result != TextToSpeech.SUCCESS) {
                    pendingCallbacks.remove(uid)
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            if (isReady) doSpeak() else { pendingInit = doSpeak }
        }
    }

    fun speakAsync(text: String, language: String = "en-US") {
        val clean = stripEmojis(text)
        if (clean.isBlank()) return
        val doSpeak: () -> Unit = {
            setLang(language)
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "async_${System.currentTimeMillis()}")
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