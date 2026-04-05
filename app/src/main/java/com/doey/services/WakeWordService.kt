package com.doey.services

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.doey.DoeyApplication
import com.doey.ui.MainActivity

private const val TAG = "WakeWordService"
private const val CHANNEL_ID = "doey_wake_word"
private const val NOTIF_ID = 9001

class WakeWordService : Service() {

    companion object {
        var onWakeWord: ((String) -> Unit)? = null
        var isRunning = false
            private set

        const val EXTRA_ACCESS_KEY = "ACCESS_KEY"
        const val EXTRA_KEYWORD_PATH = "KEYWORD_PATH"
    }

    private var porcupine: PorcupineManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val accessKey = intent?.getStringExtra(EXTRA_ACCESS_KEY) ?: ""
        val keywordPath = intent?.getStringExtra(EXTRA_KEYWORD_PATH) ?: "PORCUPINE"

        startForeground(NOTIF_ID, buildNotification())
        startPorcupine(accessKey, keywordPath)

        return START_STICKY
    }

    private fun startPorcupine(accessKey: String, keywordPath: String) {
        try {
            porcupine?.stop()
            porcupine?.delete()

            val wakeWordCallback = PorcupineManager.WakeWordCallback { idx ->
                val keyword = when {
                    keywordPath.contains(".ppn") -> "custom"
                    else -> keywordPath
                }
                Log.i(TAG, "Wake word detected: $keyword (index=$idx)")
                onWakeWord?.invoke(keyword)
            }

            val errorCallback = PorcupineManager.ErrorCallback { e ->
                Log.e(TAG, "Porcupine error: ${e.message}")
            }

            // Check for custom .ppn in assets
            val assetKeyword = findAssetKeyword()

            porcupine = if (assetKeyword != null) {
                Log.i(TAG, "Using asset keyword: $assetKeyword")
                PorcupineManager.Builder()
                    .setAccessKey(accessKey)
                    .setKeywordPath(assetKeyword)
                    .setSensitivity(0.8f)
                    .setErrorCallback(errorCallback)
                    .build(this, wakeWordCallback)
            } else {
                val builtIn = resolveBuiltIn(keywordPath)
                if (builtIn != null) {
                    Log.i(TAG, "Using built-in keyword: $builtIn")
                    PorcupineManager.Builder()
                        .setAccessKey(accessKey)
                        .setKeyword(builtIn)
                        .setSensitivity(0.85f)
                        .setErrorCallback(errorCallback)
                        .build(this, wakeWordCallback)
                } else {
                    Log.i(TAG, "Using keyword path: $keywordPath")
                    PorcupineManager.Builder()
                        .setAccessKey(accessKey)
                        .setKeywordPath(keywordPath)
                        .setSensitivity(0.85f)
                        .setErrorCallback(errorCallback)
                        .build(this, wakeWordCallback)
                }
            }

            porcupine?.start()
            Log.i(TAG, "Porcupine started")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine init failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "WakeWordService error: ${e.message}")
        }
    }

    private fun findAssetKeyword(): String? {
        return try {
            val assets = assets.list("") ?: return null
            assets.firstOrNull { it.endsWith(".ppn") }?.let { "assets/$it" }
        } catch (e: Exception) { null }
    }

    private fun resolveBuiltIn(name: String): Porcupine.BuiltInKeyword? {
        return try { Porcupine.BuiltInKeyword.valueOf(name.uppercase()) }
        catch (e: IllegalArgumentException) { null }
    }

    override fun onDestroy() {
        try {
            porcupine?.stop()
            porcupine?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }
        porcupine = null
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doey Wake Word", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for the wake word"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Doey")
            .setContentText("Listening for wake word…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
