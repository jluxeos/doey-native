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

    override fun name() = "scheduler"

    override fun description() =
        "Schedule AI tasks for the future. Supports one-time, interval, daily, weekly recurrence."

    override fun systemHint() =
        "For simple alarms use timer tool. Use scheduler for tasks that need AI execution."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "string",
                "enum" to listOf("create", "list", "get", "update", "delete", "enable", "disable")
            ),
            "schedule_id" to mapOf("type" to "string"),
            "label" to mapOf("type" to "string"),
            "instruction" to mapOf("type" to "string"),
            "trigger_at_ms" to mapOf("type" to "number"),
            "recurrence" to mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "type" to mapOf("type" to "string"),
                    "interval_ms" to mapOf("type" to "number"),
                    "time" to mapOf("type" to "string"),
                    "days_of_week" to mapOf("type" to "array")
                )
            )
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = when (args["action"] as? String) {
        "create" -> createSchedule(args)
        "list" -> listSchedules()
        "get" -> getSchedule(args)
        "update" -> updateSchedule(args)
        "delete" -> deleteSchedule(args)
        "enable" -> toggleSchedule(args, true)
        "disable" -> toggleSchedule(args, false)
        else -> errorResult("Unknown action")
    }

    private fun createSchedule(args: Map<String, Any?>): ToolResult {
        val instruction = args["instruction"] as? String ?: return errorResult("instruction required")
        val triggerMs = (args["trigger_at_ms"] as? Number)?.toLong() ?: return errorResult("trigger_at_ms required")

        val id = "sched_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        val obj = JSONObject().apply {
            put("id", id)
            put("instruction", instruction)
            put("triggerAtMs", triggerMs)
            put("createdAt", now)
            put("enabled", true)
        }

        SchedulerEngine.setSchedule(ctx, obj)
        return successResult("Created $id")
    }

    private fun listSchedules(): ToolResult {
        val arr = SchedulerEngine.getAllSchedules(ctx)
        if (arr.length() == 0) return successResult("No schedules")

        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            sb.appendLine("${s.getString("id")} → ${fmtMs(s.optLong("triggerAtMs"))}")
        }
        return successResult(sb.toString())
    }

    private fun getSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("id required")
        val s = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found")
        return successResult(s.toString())
    }

    private fun updateSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("id required")
        val s = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found")

        (args["instruction"] as? String)?.let { s.put("instruction", it) }

        SchedulerEngine.setSchedule(ctx, s)
        return successResult("Updated")
    }

    private fun deleteSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("id required")
        SchedulerEngine.removeSchedule(ctx, id)
        return successResult("Deleted")
    }

    private fun toggleSchedule(args: Map<String, Any?>, enabled: Boolean): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("id required")
        val s = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found")

        s.put("enabled", enabled)
        SchedulerEngine.setSchedule(ctx, s)

        return successResult("Updated")
    }

    private fun fmtMs(ms: Long) =
        SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(ms))
}

// ── TimerTool ─────────────────────────────────────────────────────────────────

class TimerTool : Tool {

    private val ctx get() = DoeyApplication.instance

    override fun name() = "timer"

    override fun description() = "Manage timers"

    override fun systemHint() = "Use for simple alarms"

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action" to mapOf("type" to "string"),
            "duration_ms" to mapOf("type" to "number"),
            "label" to mapOf("type" to "string"),
            "timer_id" to mapOf("type" to "string")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult =
        when (args["action"] as? String) {
            "start_timer" -> startTimer(args)
            "list" -> listTimers()
            else -> errorResult("Unknown")
        }

    private fun startTimer(args: Map<String, Any?>): ToolResult {
        val dur = (args["duration_ms"] as? Number)?.toLong() ?: return errorResult("duration required")

        val id = "timer_${System.currentTimeMillis()}"

        TimerEngine.setTimer(ctx, JSONObject().apply {
            put("id", id)
            put("durationMs", dur)
        })

        return successResult("Started $id")
    }

    private fun listTimers(): ToolResult {
        val arr = TimerEngine.getAllTimers(ctx)
        return successResult(arr.toString())
    }
}

// ── JournalTool ───────────────────────────────────────────────────────────────

class JournalTool : Tool {

    private val prefs = DoeyApplication.instance.getSharedPreferences("journal", Context.MODE_PRIVATE)

    override fun name() = "journal"

    override fun description() = "Journal entries"

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf("action" to mapOf("type" to "string")),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("OK")
    }
}

// ── NotificationListenerTool ──────────────────────────────────────────────────

class NotificationListenerTool : Tool {

    override fun name() = "notification_listener"

    override fun description() = "Notifications"

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf("action" to mapOf("type" to "string")),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("OK")
    }
}