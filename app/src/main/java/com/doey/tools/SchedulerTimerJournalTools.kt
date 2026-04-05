package com.doey.tools

import android.content.Context
import com.doey.DoeyApplication
import com.doey.services.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── SchedulerTool ─────────────────────────────────────────────────────────────

class SchedulerTool : Tool {
    private val ctx get() = DoeyApplication.instance
    override fun name()        = "scheduler"
    override fun description() = "Schedule AI tasks for the future. Supports one-time, interval, daily, weekly recurrence. Use for complex tasks needing AI reasoning."
    override fun systemHint()  = "For simple alarms use timer tool. Use scheduler for tasks that need AI execution."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"       to mapOf("type" to "string",
                "enum" to listOf("create","list","get","update","delete","enable","disable")),
            "schedule_id"  to mapOf("type" to "string"),
            "label"        to mapOf("type" to "string"),
            "instruction"  to mapOf("type" to "string"),
            "trigger_at_ms" to mapOf("type" to "number"),
            "recurrence"   to mapOf("type" to "object", "properties" to mapOf(
                "type"        to mapOf("type" to "string", "enum" to listOf("once","interval","daily","weekly")),
                "interval_ms" to mapOf("type" to "number"),
                "time"        to mapOf("type" to "string"),
                "days_of_week" to mapOf("type" to "array", "items" to mapOf("type" to "integer"))
            ))
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = when (args["action"] as? String) {
        "create"  -> createSchedule(args)
        "list"    -> listSchedules()
        "get"     -> getSchedule(args)
        "update"  -> updateSchedule(args)
        "delete"  -> deleteSchedule(args)
        "enable"  -> toggleSchedule(args, true)
        "disable" -> toggleSchedule(args, false)
        else      -> errorResult("Unknown action: ${args["action"]}")
    }

    private fun createSchedule(args: Map<String, Any?>): ToolResult {
        val instruction = args["instruction"] as? String ?: return errorResult("instruction required")
        val triggerMs   = (args["trigger_at_ms"] as? Number)?.toLong() ?: return errorResult("trigger_at_ms required")

        val id  = "sched_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        val s = JSONObject().apply {
            put("id", id)
            put("label", args["label"] as? String ?: instruction.take(50))
            put("instruction", instruction)
            put("triggerAtMs", triggerMs)
            put("enabled", true)
            put("createdAt", now)
        }

        SchedulerEngine.setSchedule(ctx, s)
        return successResult("Schedule created: ID=$id")
    }

    private fun listSchedules(): ToolResult {
        val arr = SchedulerEngine.getAllSchedules(ctx)
        if (arr.length() == 0) return successResult("No schedules.")

        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            sb.appendLine("[${s.getString("id")}] ${s.optString("label")}")
        }
        return successResult(sb.toString())
    }

    private fun getSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")
        return successResult(s.toString())
    }

    private fun updateSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")

        (args["instruction"] as? String)?.let { s.put("instruction", it) }
        SchedulerEngine.setSchedule(ctx, s)

        return successResult("Updated schedule $id")
    }

    private fun deleteSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        SchedulerEngine.removeSchedule(ctx, id)
        return successResult("Deleted $id")
    }

    private fun toggleSchedule(args: Map<String, Any?>, enabled: Boolean): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")

        s.put("enabled", enabled)
        SchedulerEngine.setSchedule(ctx, s)

        return successResult("Schedule $id updated")
    }
}

// ── TimerTool ─────────────────────────────────────────────────────────────────

class TimerTool : Tool {
    private val ctx get() = DoeyApplication.instance
    override fun name() = "timer"
    override fun description() = "Timers and stopwatches"

    override fun parameters() = mapOf<String, Any?>()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("Not implemented")
    }
}

// ── JournalTool ───────────────────────────────────────────────────────────────

class JournalTool : Tool {
    private val prefs get() = DoeyApplication.instance.getSharedPreferences("doey_journal", Context.MODE_PRIVATE)

    override fun name() = "journal"
    override fun description() = "Journal system"

    override fun parameters() = mapOf<String, Any?>()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("Not implemented")
    }

    private fun load(): JSONArray {
        return try {
            JSONArray(prefs.getString("entries", "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun save(arr: JSONArray) {
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}

// ── NotificationListenerTool ──────────────────────────────────────────────────

class NotificationListenerTool : Tool {
    private val ctx get() = DoeyApplication.instance

    private val APP_ALIASES = mapOf(
        "whatsapp" to "com.whatsapp"
    )

    private fun pkg(app: String): String {
        return APP_ALIASES[app.lowercase().trim()] ?: app
    }

    override fun name() = "notification_listener"
    override fun description() = "Notifications"

    override fun parameters() = mapOf<String, Any?>()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("Not implemented")
    }
}