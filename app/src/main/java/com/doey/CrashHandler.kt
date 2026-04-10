package com.doey

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.doey.ui.MainActivity

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val CHANNEL_ID = "doey_crash"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var appContext: Context

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        createChannel()
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val log = buildString {
                appendLine("💥 CRASH en hilo: ${thread.name}")
                appendLine("Excepción: ${throwable::class.simpleName}")
                appendLine("Mensaje: ${throwable.message}")
                appendLine("─────────────────────────")
                appendLine(throwable.stackTraceToString().take(1500))
            }

            // Guardar en SharedPreferences para leer después
            appContext.getSharedPreferences("doey_crash_log", Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash", log)
                .putLong("crash_time", System.currentTimeMillis())
                .apply()

            // Notificación inmediata
            val openIntent = PendingIntent.getActivity(
                appContext, 0,
                Intent(appContext, MainActivity::class.java).apply {
                    putExtra("show_crash_log", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("💥 Doey crasheó")
                .setContentText("${throwable::class.simpleName}: ${throwable.message?.take(80)}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(log.take(400)))
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build()

            nm.notify(9999, notif)

        } catch (_: Exception) {
            // No hacer nada si el propio handler falla
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Errores de Doey",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificaciones de crash para debug" }
            (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
