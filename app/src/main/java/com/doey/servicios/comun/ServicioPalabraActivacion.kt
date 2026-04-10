package com.doey.servicios.comun

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.doey.ui.MainActivity
import java.util.Locale

private const val TAG        = "WakeWordService"
private const val CHANNEL_ID = "doey_wake_word"
private const val NOTIF_ID   = 9001

// Tiempo de espera antes de reiniciar la escucha tras silencio o error (ms)
private const val RESTART_DELAY_MS = 1_000L

class WakeWordService : Service() {

    companion object {
        var onWakeWord: ((String) -> Unit)? = null
        var isRunning = false
            private set

        const val EXTRA_WAKE_PHRASE = "WAKE_PHRASE"
        const val EXTRA_LANGUAGE    = "LANGUAGE"

        private const val DEFAULT_PHRASE   = "hey doey"
        private const val DEFAULT_LANGUAGE = "es-ES"
    }

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var wakePhrase = DEFAULT_PHRASE
    private var language   = DEFAULT_LANGUAGE
    private var isListening = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakePhrase = (intent?.getStringExtra(EXTRA_WAKE_PHRASE) ?: DEFAULT_PHRASE).lowercase().trim()
        language   = intent?.getStringExtra(EXTRA_LANGUAGE) ?: DEFAULT_LANGUAGE

        startForeground(NOTIF_ID, buildNotification())
        mainHandler.post { startListening() }

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        isListening = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Escucha continua ──────────────────────────────────────────────────────

    private fun startListening() {
        if (!isRunning) return
        destroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer no disponible en este dispositivo")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Escuchando wake word…")
                isListening = true
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val results = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return
                checkForWakeWord(results)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val texts = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList()
                val triggered = checkForWakeWord(texts)
                // Si no se detectó la wake word, reiniciar escucha
                if (!triggered) scheduleRestart()
            }

            override fun onError(error: Int) {
                isListening = false
                val msg = errorString(error)
                Log.w(TAG, "STT error: $msg ($error)")
                
                // Si el reconocedor está ocupado, esperar un poco más antes de reintentar
                val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 2000L else RESTART_DELAY_MS
                
                // Reiniciar siempre salvo que el servicio esté siendo destruido
                if (isRunning) scheduleRestart(delay)
            }

            override fun onEndOfSpeech()                              { isListening = false }
            override fun onBeginningOfSpeech()                        {}
            override fun onRmsChanged(rmsdB: Float)                   {}
            override fun onBufferReceived(buffer: ByteArray?)         {}
            override fun onEvent(eventType: Int, params: Bundle?)     {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,        language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,     3)
            // Ajustes para escucha continua más robusta
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escucha: ${e.message}")
            scheduleRestart()
        }
    }

    /**
     * Revisa si alguno de los textos contiene la wake phrase.
     * Devuelve true si se disparó el callback.
     */
    private fun checkForWakeWord(texts: List<String>): Boolean {
        for (text in texts) {
            val normalized = text.lowercase().trim()
            if (normalized.contains(wakePhrase)) {
                Log.i(TAG, "¡Wake word detectada! → \"$normalized\"")
                
                // Notificar al sistema
                onWakeWord?.invoke(wakePhrase)
                
                // Detener la escucha propia para dejar el micrófono libre a Doey
                isListening = false
                destroyRecognizer()
                
                // Esperar a que Doey termine antes de volver a escuchar (se maneja desde el ViewModel usualmente,
                // pero aquí ponemos un delay de seguridad largo)
                scheduleRestart(delay = 8_000L)
                return true
            }
        }
        return false
    }

    private fun scheduleRestart(delay: Long = RESTART_DELAY_MS) {
        if (!isRunning) return
        destroyRecognizer()
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startListening() }, delay)
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
        isListening = false
    }

    // ── Notificación foreground ───────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doey Wake Word", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Escuchando la palabra de activación"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Doey")
            .setContentText("Escuchando: \"$wakePhrase\"…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun errorString(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO               -> "Error de audio"
        SpeechRecognizer.ERROR_CLIENT              -> "Error de cliente"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
        SpeechRecognizer.ERROR_NETWORK             -> "Error de red"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT     -> "Timeout de red"
        SpeechRecognizer.ERROR_NO_MATCH            -> "Sin coincidencia"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY     -> "Reconocedor ocupado"
        SpeechRecognizer.ERROR_SERVER              -> "Error del servidor"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT      -> "Timeout de voz"
        else                                       -> "Error desconocido ($code)"
    }
}
