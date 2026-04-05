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
            "instruction"  to mapOf("type" to "string", "description" to "Natural language instruction for the sub-agent"),
            "trigger_at_ms" to mapOf("type" to "number", "description" to "Unix timestamp in ms for first trigger"),
            "recurrence"   to mapOf("type" to "object", "properties" to mapOf(
                "type"        to mapOf("type" to "string", "enum" to listOf("once","interval","daily","weekly")),
                "interval_ms" to mapOf("type" to "number"),
                "time"        to mapOf("type" to "string", "description" to "HH:mm 24h format"),
                "days_of_week" to mapOf("type" to "array", "items" to mapOf("type" to "integer"), "description" to "1=Mon..7=Sun")
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
        @Suppress("UNCHECKED_CAST")
        val recMap  = args["recurrence"] as? Map<String, Any?> ?: mapOf("type" to "once")
        val rec     = JSONObject().apply {
            put("type", recMap["type"] as? String ?: "once")
            (recMap["interval_ms"] as? Number)?.let { put("intervalMs", it.toLong()) }
            (recMap["time"] as? String)?.let { put("time", it) }
            @Suppress("UNCHECKED_CAST")
            (recMap["days_of_week"] as? List<Number>)?.let { days -> put("daysOfWeek", JSONArray(days.map { it.toInt() })) }
        }
        val id  = "sched_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val s   = JSONObject().apply {
            put("id", id); put("label", args["label"] as? String ?: instruction.take(50))
            put("instruction", instruction); put("triggerAtMs", triggerMs)
            put("enabled", true); put("recurrence", rec); put("createdAt", now)
        }
        SchedulerEngine.setSchedule(ctx, s)
        return successResult("Schedule created: ID=$id, next: ${fmtMs(triggerMs)}", "Schedule set")
    }

    private fun listSchedules(): ToolResult {
        val arr = SchedulerEngine.getAllSchedules(ctx)
        if (arr.length() == 0) return successResult("No schedules.")
        val sb = StringBuilder("Schedules:\n")
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            val status = if (s.optBoolean("enabled")) "[active]" else "[paused]"
            val label  = s.optString("label").ifBlank { s.optString("instruction").take(40) }
            sb.appendLine("$status [${s.getString("id")}] \"$label\" → ${fmtMs(s.optLong("triggerAtMs"))}")
        }
        return successResult(sb.toString())
    }

    private fun getSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")
        return successResult(s.toString(2))
    }

    private fun updateSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")
        (args["instruction"] as? String)?.let { s.put("instruction", it) }
        (args["label"] as? String)?.let { s.put("label", it) }
        (args["trigger_at_ms"] as? Number)?.let { s.put("triggerAtMs", it.toLong()) }
        SchedulerEngine.setSchedule(ctx, s)
        return successResult("Updated schedule $id")
    }

    private fun deleteSchedule(args: Map<String, Any?>): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        SchedulerEngine.removeSchedule(ctx, id); return successResult("Deleted $id")
    }

    private fun toggleSchedule(args: Map<String, Any?>, enabled: Boolean): ToolResult {
        val id = args["schedule_id"] as? String ?: return errorResult("schedule_id required")
        val s  = SchedulerEngine.getSchedule(ctx, id) ?: return errorResult("Not found: $id")
        s.put("enabled", enabled); SchedulerEngine.setSchedule(ctx, s)
        return successResult("Schedule $id ${if (enabled) "enabled" else "disabled"}")
    }

    private fun fmtMs(ms: Long) = if (ms == 0L) "?" else SimpleDateFormat("EEE d MMM HH:mm", Locale.ENGLISH).format(Date(ms))
}

// ── TimerTool ─────────────────────────────────────────────────────────────────

class TimerTool : Tool {
    private val ctx get() = DoeyApplication.instance
    override fun name()        = "timer"
    override fun description() = "Manage countdown timers and stopwatches. Timers beep on expiry. Multiple can run in parallel."
    override fun systemHint()  = "Use for simple alarms (egg timer, reminders). For complex AI tasks use scheduler."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"      to mapOf("type" to "string",
                "enum" to listOf("start_timer","start_stopwatch","list","get_status","cancel")),
            "duration_ms" to mapOf("type" to "number", "description" to "Duration in milliseconds"),
            "label"       to mapOf("type" to "string"),
            "timer_id"    to mapOf("type" to "string")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = when (args["action"] as? String) {
        "start_timer"     -> startTimer(args)
        "start_stopwatch" -> startStopwatch(args)
        "list"            -> listTimers()
        "get_status"      -> getStatus(args)
        "cancel"          -> cancel(args)
        else              -> errorResult("Unknown action: ${args["action"]}")
    }

    private fun startTimer(args: Map<String, Any?>): ToolResult {
        val durMs = (args["duration_ms"] as? Number)?.toLong() ?: return errorResult("duration_ms required")
        if (durMs <= 0) return errorResult("duration_ms must be > 0")
        val now = System.currentTimeMillis()
        val id  = "timer_$now"
        val label = args["label"] as? String ?: fmtDur(durMs)
        TimerEngine.setTimer(ctx, JSONObject().apply {
            put("id", id); put("label", label); put("type", "timer")
            put("startTimeMs", now); put("durationMs", durMs); put("enabled", true)
        })
        return successResult("Timer started: \"$label\", expires in ${fmtDur(durMs)}", "$label started")
    }

    private fun startStopwatch(args: Map<String, Any?>): ToolResult {
        val now = System.currentTimeMillis()
        val id  = "sw_$now"
        val label = args["label"] as? String ?: "Stopwatch"
        TimerEngine.setTimer(ctx, JSONObject().apply {
            put("id", id); put("label", label); put("type", "stopwatch")
            put("startTimeMs", now); put("enabled", true)
        })
        return successResult("Stopwatch started: \"$label\"", "$label started")
    }

    private fun listTimers(): ToolResult {
        val arr = TimerEngine.getAllTimers(ctx)
        if (arr.length() == 0) return successResult("No active timers.")
        val now = System.currentTimeMillis()
        val sb  = StringBuilder()
        for (i in 0 until arr.length()) {
            val t     = arr.getJSONObject(i)
            val label = t.optString("label", t.getString("id"))
            if (t.getString("type") == "timer") {
                val rem = (t.optLong("startTimeMs") + t.optLong("durationMs")) - now
                sb.appendLine("[${t.getString("id")}] Timer \"$label\": ${if (rem > 0) "${fmtDur(rem)} remaining" else "expired"}")
            } else {
                sb.appendLine("[${t.getString("id")}] Stopwatch \"$label\": ${fmtDur(now - t.optLong("startTimeMs"))} elapsed")
            }
        }
        return successResult(sb.toString())
    }

    private fun getStatus(args: Map<String, Any?>): ToolResult {
        val id = args["timer_id"] as? String ?: return errorResult("timer_id required")
        val t  = TimerEngine.getTimer(ctx, id) ?: return errorResult("Not found: $id")
        val now = System.currentTimeMillis()
        val label = t.optString("label")
        return if (t.getString("type") == "timer") {
            val rem = (t.optLong("startTimeMs") + t.optLong("durationMs")) - now
            successResult("\"$label\": ${if (rem > 0) "${fmtDur(rem)} remaining" else "expired"}")
        } else {
            successResult("\"$label\": ${fmtDur(now - t.optLong("startTimeMs"))} elapsed")
        }
    }

    private fun cancel(args: Map<String, Any?>): ToolResult {
        val id = args["timer_id"] as? String ?: return errorResult("timer_id required")
        TimerEngine.removeTimer(ctx, id)
        return successResult("Timer $id cancelled")
    }

    private fun fmtDur(ms: Long): String {
        val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return when { h > 0 -> "${h}h ${m}m"; m > 0 -> "${m}m ${sec}s"; else -> "${sec}s" }
    }
}

// ── JournalTool ───────────────────────────────────────────────────────────────

class JournalTool : Tool {
    private val prefs get() = DoeyApplication.instance.getSharedPreferences("doey_journal", Context.MODE_PRIVATE)
    override fun name()        = "journal"
    override fun description() = "Create, read and manage journal entries with categories, dates and details."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"   to mapOf("type" to "string", "enum" to listOf("create","list","get","delete","list_by_category")),
            "title"    to mapOf("type" to "string"),
            "category" to mapOf("type" to "string"),
            "details"  to mapOf("type" to "string"),
            "date_from" to mapOf("type" to "number"),
            "date_to"   to mapOf("type" to "number"),
            "entry_id"  to mapOf("type" to "string")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = when (args["action"] as? String) {
        "create"           -> createEntry(args)
        "list"             -> listEntries()
        "get"              -> getEntry(args)
        "delete"           -> deleteEntry(args)
        "list_by_category" -> listByCategory(args)
        else               -> errorResult("Unknown: ${args["action"]}")
    }

    private fun load(): JSONArray = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
    private fun save(arr: JSONArray) = prefs.edit().putString("entries", arr.toString()).apply()

    private fun createEntry(args: Map<String, Any?>): ToolResult {
        val title    = args["title"]    as? String ?: return errorResult("title required")
        val category = args["category"] as? String ?: return errorResult("category required")
        val details  = args["details"]  as? String ?: return errorResult("details required")
        val now = System.currentTimeMillis()
        val id  = "entry_$now"
        val entry = JSONObject().apply {
            put("id", id); put("title", title); put("category", category)
            put("details", details); put("createdAt", now)
            (args["date_from"] as? Number)?.let { put("dateFrom", it.toLong()) }
            (args["date_to"]   as? Number)?.let { put("dateTo",   it.toLong()) }
        }
        val arr = load(); arr.put(entry); save(arr)
        return successResult("Journal entry created: $id", "Entry saved")
    }

    private fun listEntries(): ToolResult {
        val arr = load()
        if (arr.length() == 0) return successResult("No journal entries.")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(e.optLong("createdAt")))
            sb.appendLine("[${e.getString("id")}] ${e.optString("title")} (${e.optString("category")}) – $d")
        }
        return successResult(sb.toString())
    }

    private fun getEntry(args: Map<String, Any?>): ToolResult {
        val id = args["entry_id"] as? String ?: return errorResult("entry_id required")
        val arr = load()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            if (e.getString("id") == id) return successResult(
                "${e.optString("title")}\n[${e.optString("category")}]\n${e.optString("details")}")
        }
        return errorResult("Entry not found: $id")
    }

    private fun deleteEntry(args: Map<String, Any?>): ToolResult {
        val id  = args["entry_id"] as? String ?: return errorResult("entry_id required")
        val arr = load()
        val new = JSONArray()
        for (i in 0 until arr.length()) { val e = arr.getJSONObject(i); if (e.getString("id") != id) new.put(e) }
        save(new)
        return successResult("Entry deleted: $id")
    }

    private fun listByCategory(args: Map<String, Any?>): ToolResult {
        val cat = args["category"] as? String ?: return errorResult("category required")
        val arr = load()
        val sb  = StringBuilder()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            if (e.optString("category").equals(cat, ignoreCase = true)) {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(e.optLong("createdAt")))
                sb.appendLine("[${e.getString("id")}] ${e.optString("title")} – $d")
            }
        }
        return if (sb.isEmpty()) successResult("No entries in \"$cat\"") else successResult(sb.toString())
    }
}

// ── NotificationListenerTool ──────────────────────────────────────────────────

class NotificationListenerTool : Tool {
    private val ctx get() = DoeyApplication.instance
    private val APP_ALIASES = mapOf(
        "whatsapp" to "com.whatsapp", "gmail" to "com.google.android.gm",
        "telegram" to "org.telegram.messenger", "signal" to "org.thoughtcrime.securesms",
        "instagram" to "com.instagram.android", "slack" to "com.Slack", "discord" to "com.discord"
    )
    private fun pkg(app: String) = APP_ALIASES[app.lowercase().trim()] ?: app

    override fun name()        = "notification_listener"
    override fun description() = "Manage notification rules. Subscribe to apps and define AI reactions to incoming notifications."
    override fun systemHint()  = "Use to auto-respond to WhatsApp messages, Gmail, etc."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"      to mapOf("type" to "string",
                "enum" to listOf("subscribe","unsubscribe","list_subscriptions","add_rule","list_rules","delete_rule","get_recent","clear_buffer","check_access")),
            "app"         to mapOf("type" to "string"),
            "instruction" to mapOf("type" to "string"),
            "condition"   to mapOf("type" to "string"),
            "rule_id"     to mapOf("type" to "string"),
            "limit"       to mapOf("type" to "integer")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = when (args["action"] as? String) {
        "subscribe" -> {
            val p = pkg(args["app"] as? String ?: return errorResult("app required"))
            val subs = NotificationAccessManager.getSubscribedApps(ctx).toMutableList()
            if (p !in subs) { subs.add(p); NotificationAccessManager.setSubscribedApps(ctx, subs) }
            successResult("Subscribed to $p")
        }
        "unsubscribe" -> {
            val p    = pkg(args["app"] as? String ?: return errorResult("app required"))
            val subs = NotificationAccessManager.getSubscribedApps(ctx).filter { it != p }
            NotificationAccessManager.setSubscribedApps(ctx, subs)
            successResult("Unsubscribed from $p")
        }
        "list_subscriptions" -> {
            val subs = NotificationAccessManager.getSubscribedApps(ctx)
            if (subs.isEmpty()) successResult("No subscriptions.") else successResult(subs.joinToString("\n"))
        }
        "add_rule" -> {
            val app  = args["app"]         as? String ?: return errorResult("app required")
            val inst = args["instruction"] as? String ?: return errorResult("instruction required")
            val p    = pkg(app)
            val rules = NotificationRulesStore.getRules(ctx).toMutableList()
            val id    = "rule_${System.currentTimeMillis()}"
            rules.add(NotificationRulesStore.Rule(id = id, app = p, instruction = inst,
                condition = args["condition"] as? String))
            NotificationRulesStore.saveRules(ctx, rules)
            val subs = NotificationAccessManager.getSubscribedApps(ctx).toMutableList()
            if (p !in subs) { subs.add(p); NotificationAccessManager.setSubscribedApps(ctx, subs) }
            successResult("Rule added (ID=$id): $inst")
        }
        "list_rules" -> {
            val rules = NotificationRulesStore.getRules(ctx)
            if (rules.isEmpty()) successResult("No rules.") else successResult(rules.joinToString("\n") { "[${it.id}] ${it.app}: ${it.instruction}" })
        }
        "delete_rule" -> {
            val rid   = args["rule_id"] as? String ?: return errorResult("rule_id required")
            val rules = NotificationRulesStore.getRules(ctx).filter { it.id != rid }
            NotificationRulesStore.saveRules(ctx, rules)
            successResult("Rule $rid deleted")
        }
        "get_recent" -> {
            val limit  = (args["limit"] as? Number)?.toInt() ?: 20
            val recent = NotificationAccessManager.getRecentNotifications(ctx, limit)
            if (recent.isEmpty()) successResult("No notifications.") else successResult(
                recent.joinToString("\n") { n -> "[${n.optString("packageName")}] ${n.optString("title")}: ${n.optString("text").take(80)}" })
        }
        "clear_buffer" -> { NotificationAccessManager.clearBuffer(ctx); successResult("Buffer cleared") }
        "check_access" -> {
            val granted = NotificationAccessManager.isAccessGranted(ctx)
            successResult("Notification access: ${if (granted) "granted" else "NOT granted – enable in Android Settings → Notification Access"}")
        }
        else -> errorResult("Unknown action: ${args["action"]}")
    }
}
