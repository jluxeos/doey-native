package com.doey.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

private const val TAG         = "TimerEngine"
private const val PREFS_TIMERS = "doey_timers"
private const val KEY_TIMERS  = "timers"

object TimerEngine {

    fun setTimer(context: Context, timer: JSONObject): JSONObject {
        val id       = timer.getString("id")
        val type     = timer.getString("type")
        val enabled  = timer.optBoolean("enabled", true)
        val duration = timer.optLong("durationMs", 0)
        saveTimer(context, timer)
        if (type == "timer" && enabled && duration > 0) {
            val start   = timer.optLong("startTimeMs", System.currentTimeMillis())
            val trigger = start + duration
            if (trigger > System.currentTimeMillis()) setAlarm(context, id, trigger)
        }
        return timer
    }

    fun removeTimer(context: Context, id: String) { cancelAlarm(context, id); deleteTimer(context, id) }

    fun getTimer(context: Context, id: String): JSONObject? {
        val arr = loadAll(context)
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i); if (t.getString("id") == id) return t
        }
        return null
    }

    fun getAllTimers(context: Context): JSONArray = loadAll(context)

    fun onAlarmFired(context: Context, timerId: String) {
        Log.d(TAG, "Timer expired: $timerId")
        val timer = getTimer(context, timerId)
        val label = timer?.optString("label", "Timer") ?: "Timer"
        deleteTimer(context, timerId)
        DoeyTTSEngine.playBeep(3)
        val msg = if (label.isNotBlank()) "$label expired!" else "Timer expired!"
        DoeyTTSEngine.speakAsync(msg)
    }

    fun reScheduleAll(context: Context) {
        val arr = loadAll(context)
        val now = System.currentTimeMillis()
        for (i in 0 until arr.length()) {
            val t       = arr.getJSONObject(i)
            if (t.getString("type") != "timer") continue
            if (!t.optBoolean("enabled", true)) continue
            val start   = t.optLong("startTimeMs", 0)
            val dur     = t.optLong("durationMs", 0)
            val trigger = start + dur
            if (trigger > now) setAlarm(context, t.getString("id"), trigger)
            else onAlarmFired(context, t.getString("id"))
        }
    }

    // ── AlarmManager ─────────────────────────────────────────────────────────

    private fun setAlarm(context: Context, id: String, triggerMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, buildPI(context, id))
            else
                am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, buildPI(context, id))
        } catch (e: Exception) { Log.e(TAG, "setAlarm failed: ${e.message}") }
    }

    private fun cancelAlarm(context: Context, id: String) =
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(buildPI(context, id))

    private fun buildPI(context: Context, id: String): PendingIntent {
        val i = Intent(context, TimerReceiver::class.java).apply { putExtra("timer_id", id) }
        return PendingIntent.getBroadcast(context, id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadAll(context: Context): JSONArray {
        val json = context.getSharedPreferences(PREFS_TIMERS, Context.MODE_PRIVATE)
            .getString(KEY_TIMERS, "[]") ?: "[]"
        return try { JSONArray(json) } catch (_: Exception) { JSONArray() }
    }

    private fun saveTimer(context: Context, timer: JSONObject) {
        val id  = timer.getString("id")
        val arr = loadAll(context)
        var found = false
        for (i in 0 until arr.length()) { if (arr.getJSONObject(i).getString("id") == id) { arr.put(i, timer); found = true; break } }
        if (!found) arr.put(timer)
        context.getSharedPreferences(PREFS_TIMERS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TIMERS, arr.toString()).apply()
    }

    private fun deleteTimer(context: Context, id: String) {
        val arr    = loadAll(context)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) { val t = arr.getJSONObject(i); if (t.getString("id") != id) newArr.put(t) }
        context.getSharedPreferences(PREFS_TIMERS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TIMERS, newArr.toString()).apply()
    }
}
