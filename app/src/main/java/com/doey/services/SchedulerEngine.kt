package com.doey.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private const val TAG           = "SchedulerEngine"
const val PREFS_SCHEDULER       = "doey_scheduler"
const val KEY_SCHEDULES         = "schedules"
const val KEY_AGENT_CONFIG      = "agent_config"

// ── SchedulerEngine ───────────────────────────────────────────────────────────

object SchedulerEngine {

    fun setSchedule(context: Context, json: JSONObject): JSONObject {
        val id       = json.getString("id")
        var enabled  = json.optBoolean("enabled", true)
        var trigger  = json.optLong("triggerAtMs", 0)
        val now      = System.currentTimeMillis()

        if (enabled && trigger <= now) {
            val next = calculateNextTrigger(json, now)
            if (next != null) { trigger = next; json.put("triggerAtMs", trigger) }
            else              { json.put("enabled", false); enabled = false }
        }
        saveSchedule(context, json)
        if (enabled && trigger > now) setAlarm(context, id, trigger) else cancelAlarm(context, id)
        return json
    }

    fun removeSchedule(context: Context, id: String) { cancelAlarm(context, id); deleteSchedule(context, id) }

    fun getSchedule(context: Context, id: String): JSONObject? {
        val arr = loadAll(context)
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            if (s.getString("id") == id) return s
        }
        return null
    }

    fun getAllSchedules(context: Context): JSONArray = loadAll(context)

    fun updateTrigger(context: Context, id: String, newMs: Long) {
        val s = getSchedule(context, id) ?: return
        s.put("triggerAtMs", newMs)
        saveSchedule(context, s)
        if (s.optBoolean("enabled", true) && newMs > System.currentTimeMillis()) setAlarm(context, id, newMs)
    }

    fun onAlarmFired(context: Context, id: String) {
        Log.d(TAG, "Alarm fired: $id")
        val s   = getSchedule(context, id) ?: return
        val now = System.currentTimeMillis()
        s.put("lastExecutedAt", now)
        val recType = s.optJSONObject("recurrence")?.optString("type", "once") ?: "once"
        if (recType != "once") {
            val next = calculateNextTrigger(s, now)
            if (next != null) { s.put("triggerAtMs", next); saveSchedule(context, s); setAlarm(context, id, next) }
            else              { s.put("enabled", false); saveSchedule(context, s) }
        } else {
            s.put("enabled", false); saveSchedule(context, s)
        }
        val instruction = s.optString("instruction", "")
        if (instruction.isNotBlank()) executeInstruction(context, id, instruction)
    }

    fun reScheduleAll(context: Context) {
        val arr = loadAll(context)
        val now = System.currentTimeMillis()
        for (i in 0 until arr.length()) {
            val s       = arr.getJSONObject(i)
            val id      = s.getString("id")
            val enabled = s.optBoolean("enabled", true)
            var trigger = s.optLong("triggerAtMs", 0)
            if (!enabled) continue
            if (trigger <= now) {
                val next = calculateNextTrigger(s, now) ?: continue
                trigger = next; s.put("triggerAtMs", trigger); saveSchedule(context, s)
            }
            setAlarm(context, id, trigger)
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
        val i = Intent(context, SchedulerReceiver::class.java).apply { putExtra("schedule_id", id) }
        return PendingIntent.getBroadcast(context, id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadAll(context: Context): JSONArray {
        val json = context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE)
            .getString(KEY_SCHEDULES, "[]") ?: "[]"
        return try { JSONArray(json) } catch (_: Exception) { JSONArray() }
    }

    private fun saveSchedule(context: Context, s: JSONObject) {
        val id  = s.getString("id")
        val arr = loadAll(context)
        var found = false
        for (i in 0 until arr.length()) { if (arr.getJSONObject(i).getString("id") == id) { arr.put(i, s); found = true; break } }
        if (!found) arr.put(s)
        context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE).edit()
            .putString(KEY_SCHEDULES, arr.toString()).apply()
    }

    private fun deleteSchedule(context: Context, id: String) {
        val arr    = loadAll(context)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) { val s = arr.getJSONObject(i); if (s.getString("id") != id) newArr.put(s) }
        context.getSharedPreferences(PREFS_SCHEDULER, Context.MODE_PRIVATE).edit()
            .putString(KEY_SCHEDULES, newArr.toString()).apply()
    }

    // ── Recurrence ────────────────────────────────────────────────────────────

    fun calculateNextTrigger(schedule: JSONObject, now: Long): Long? {
        val rec = schedule.optJSONObject("recurrence") ?: return null
        return when (rec.optString("type", "once")) {
            "once"     -> null
            "interval" -> now + rec.optLong("intervalMs", 60_000)
            "daily"    -> {
                val time = rec.optString("time", "").takeIf { it.isNotEmpty() } ?: return null
                getNextDaily(time, now)
            }
            "weekly"   -> {
                val time     = rec.optString("time", "").takeIf { it.isNotEmpty() } ?: return null
                val daysArr  = rec.optJSONArray("daysOfWeek") ?: return null
                val days     = (0 until daysArr.length()).map { daysArr.getInt(it) }
                getNextWeekly(time, days, now)
            }
            else -> null
        }
    }

    fun getNextDaily(time: String, after: Long): Long {
        val (h, m) = time.split(":").map { it.toInt() }
        val cal    = Calendar.getInstance().apply {
            timeInMillis = after; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    fun getNextWeekly(time: String, daysOfWeek: List<Int>, after: Long): Long {
        val (h, m) = time.split(":").map { it.toInt() }
        for (ahead in 0..7) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = after; add(Calendar.DAY_OF_MONTH, ahead)
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val calDay = cal.get(Calendar.DAY_OF_WEEK)
            val ourDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
            if (daysOfWeek.contains(ourDay) && cal.timeInMillis > after) return cal.timeInMillis
        }
        return after + 7 * 24 * 60 * 60 * 1000
    }

    // ── Execute instruction ───────────────────────────────────────────────────

    private fun executeInstruction(context: Context, scheduleId: String, instruction: String) {
        try {
            context.startService(Intent(context, SchedulerJobService::class.java).apply {
                putExtra("schedule_id", scheduleId)
                putExtra("instruction", instruction)
            })
        } catch (e: Exception) { Log.e(TAG, "Failed to start SchedulerJobService: ${e.message}") }
    }
}
