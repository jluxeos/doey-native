package com.doey.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.doey.DoeyApplication
import java.util.Calendar

// ── Datos de alarma ──────────────────────────────────────────────────────────

data class ScheduledAlarm(
    val id: Int,
    val title: String,
    val description: String,
    val triggerTimeMs: Long,
    val isRecurring: Boolean = false,
    val recurringIntervalMs: Long = 0L
)

// ── Scheduler de alarmas ─────────────────────────────────────────────────────

object AlarmScheduler {
    private const val ALARM_ACTION = "com.doey.ALARM_TRIGGER"
    private const val ALARM_ID_EXTRA = "alarm_id"
    private const val ALARM_TITLE_EXTRA = "alarm_title"
    private const val ALARM_DESC_EXTRA = "alarm_desc"
    
    private val alarmManager: AlarmManager?
        get() = DoeyApplication.instance.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    
    /**
     * Programa una alarma para sonar a una hora específica.
     * Funciona incluso con la app cerrada.
     */
    fun scheduleAlarm(alarm: ScheduledAlarm) {
        val ctx = DoeyApplication.instance
        val alarmMgr = alarmManager ?: return
        
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(ALARM_ID_EXTRA, alarm.id)
            putExtra(ALARM_TITLE_EXTRA, alarm.title)
            putExtra(ALARM_DESC_EXTRA, alarm.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            ctx, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: requiere SCHEDULE_EXACT_ALARM
                if (alarmMgr.canScheduleExactAlarms()) {
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmMgr.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmMgr.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
    }
    
    /**
     * Programa una alarma recurrente (diaria, semanal, etc.).
     */
    fun scheduleRecurringAlarm(alarm: ScheduledAlarm) {
        val ctx = DoeyApplication.instance
        val alarmMgr = alarmManager ?: return
        
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(ALARM_ID_EXTRA, alarm.id)
            putExtra(ALARM_TITLE_EXTRA, alarm.title)
            putExtra(ALARM_DESC_EXTRA, alarm.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            ctx, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmMgr.canScheduleExactAlarms()) {
                    alarmMgr.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerTimeMs,
                        alarm.recurringIntervalMs,
                        pendingIntent
                    )
                } else {
                    alarmMgr.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerTimeMs,
                        alarm.recurringIntervalMs,
                        pendingIntent
                    )
                }
            } else {
                alarmMgr.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerTimeMs,
                    alarm.recurringIntervalMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Error scheduling recurring alarm: ${e.message}")
        }
    }
    
    /**
     * Cancela una alarma programada.
     */
    fun cancelAlarm(alarmId: Int) {
        val ctx = DoeyApplication.instance
        val alarmMgr = alarmManager ?: return
        
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            ctx, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmMgr.cancel(pendingIntent)
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Error canceling alarm: ${e.message}")
        }
    }
    
    /**
     * Programa una alarma para dentro de X minutos.
     */
    fun scheduleAlarmInMinutes(
        id: Int,
        title: String,
        description: String,
        delayMinutes: Int
    ) {
        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        scheduleAlarm(
            ScheduledAlarm(
                id = id,
                title = title,
                description = description,
                triggerTimeMs = triggerTime
            )
        )
    }
    
    /**
     * Programa una alarma para una hora específica del día (HH:mm).
     */
    fun scheduleAlarmAtTime(
        id: Int,
        title: String,
        description: String,
        hour: Int,
        minute: Int,
        isRecurringDaily: Boolean = false
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val alarm = ScheduledAlarm(
            id = id,
            title = title,
            description = description,
            triggerTimeMs = calendar.timeInMillis,
            isRecurring = isRecurringDaily,
            recurringIntervalMs = if (isRecurringDaily) AlarmManager.INTERVAL_DAY else 0L
        )
        
        if (isRecurringDaily) {
            scheduleRecurringAlarm(alarm)
        } else {
            scheduleAlarm(alarm)
        }
    }
}
