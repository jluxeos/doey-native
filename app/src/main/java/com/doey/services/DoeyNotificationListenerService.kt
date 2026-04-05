package com.doey.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.doey.DoeyApplication
import com.doey.agent.ConversationPipeline
import com.doey.llm.LLMProviderFactory
import com.doey.tools.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

private const val TAG          = "NotifListener"
private const val PREFS_NOTIF  = "doey_notifications"
private const val KEY_SUBS     = "subscribed_apps"
private const val KEY_BUFFER   = "notifications_buffer"
private const val MAX_BUFFER   = 50

// ── NotificationRule store ────────────────────────────────────────────────────

object NotificationRulesStore {

    data class Rule(
        val id: String,
        val app: String,
        val instruction: String,
        val condition: String? = null,
        val enabled: Boolean   = true
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("doey_notif_rules", Context.MODE_PRIVATE)

    fun getRules(ctx: Context): List<Rule> {
        val arr = JSONArray(prefs(ctx).getString("rules", "[]") ?: "[]")
        return (0 until arr.length()).mapNotNull { i ->
            try {
                val o = arr.getJSONObject(i)
                Rule(o.getString("id"), o.getString("app"), o.getString("instruction"),
                    o.optString("condition").takeIf { it.isNotEmpty() },
                    o.optBoolean("enabled", true))
            } catch (_: Exception) { null }
        }
    }

    fun saveRules(ctx: Context, rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { r -> arr.put(JSONObject().apply {
            put("id", r.id); put("app", r.app); put("instruction", r.instruction)
            put("condition", r.condition ?: ""); put("enabled", r.enabled)
        }) }
        prefs(ctx).edit().putString("rules", arr.toString()).apply()
    }

    fun getRulesForApp(ctx: Context, pkg: String): List<Rule> =
        getRules(ctx).filter { it.app == pkg && it.enabled }
}

// ── Notification access helpers ───────────────────────────────────────────────

object NotificationAccessManager {

    fun getSubscribedApps(ctx: Context): List<String> {
        val json = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            .getString(KEY_SUBS, "[]") ?: "[]"
        return try { val a = JSONArray(json); (0 until a.length()).map { a.getString(it) } }
        catch (_: Exception) { emptyList() }
    }

    fun setSubscribedApps(ctx: Context, apps: List<String>) {
        val arr = JSONArray(); apps.forEach { arr.put(it) }
        ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE).edit()
            .putString(KEY_SUBS, arr.toString()).apply()
    }

    fun getRecentNotifications(ctx: Context, limit: Int = 20): List<JSONObject> {
        val json = ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            .getString(KEY_BUFFER, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val from = maxOf(0, arr.length() - limit)
            (from until arr.length()).map { arr.getJSONObject(it) }.reversed()
        } catch (_: Exception) { emptyList() }
    }

    fun clearBuffer(ctx: Context) {
        ctx.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE).edit()
            .putString(KEY_BUFFER, "[]").apply()
    }

    fun isAccessGranted(ctx: Context): Boolean {
        val cn   = ComponentName(ctx, DoeyNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(cn.flattenToString())
    }
}

// ── NotificationListenerService ───────────────────────────────────────────────

class DoeyNotificationListenerService : NotificationListenerService() {

    private val scope        = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedKeys = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val key = sbn.key
        if (key in processedKeys) return
        processedKeys.add(key); if (processedKeys.size > 200) processedKeys.iterator().let { it.next(); it.remove() }

        val subs = NotificationAccessManager.getSubscribedApps(this).toSet()
        if (sbn.packageName !in subs || sbn.isOngoing || sbn.packageName == "com.doey") return

        val extras = sbn.notification?.extras ?: return
        val title  = extras.getCharSequence("android.title")?.toString() ?: ""
        val text   = extras.getCharSequence("android.text") ?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return

        bufferNotification(sbn.packageName, title, text, sbn.postTime)
        scope.launch { processNotification(sbn.packageName, title, text) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun bufferNotification(pkg: String, title: String, text: String, ts: Long) {
        val prefs  = getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
        val arr    = try { JSONArray(prefs.getString(KEY_BUFFER, "[]")) } catch (_: Exception) { JSONArray() }
        arr.put(JSONObject().apply { put("packageName",pkg); put("title",title); put("text",text); put("timestamp",ts) })
        val trimmed = JSONArray()
        val from    = maxOf(0, arr.length() - MAX_BUFFER)
        for (i in from until arr.length()) trimmed.put(arr.get(i))
        prefs.edit().putString(KEY_BUFFER, trimmed.toString()).apply()
    }

    private suspend fun processNotification(pkg: String, title: String, text: String) {
        val rules = NotificationRulesStore.getRulesForApp(this, pkg)
        if (rules.isEmpty()) return

        val app      = DoeyApplication.instance
        val settings = app.settingsStore
        val apiKey   = settings.getApiKey(settings.getProvider())
        if (apiKey.isBlank()) return

        val provider  = settings.getProvider()
        val model     = settings.getModel()
        val customUrl = settings.getCustomModelUrl()
        val language  = settings.getLanguage()
        val llm       = LLMProviderFactory.create(provider, apiKey, model, customUrl)
        val tools     = ToolRegistry().apply {
            register(IntentTool()); register(SmsTool()); register(BeepTool())
            register(DateTimeTool()); register(DeviceTool()); register(QueryContactsTool())
            register(HttpTool()); register(TTSTool()); register(AppSearchTool())
            register(FileStorageTool()); register(SkillDetailTool(app.skillLoader))
            register(PersonalMemoryTool()); register(JournalTool())
        }

        val pipeline = ConversationPipeline(
            provider    = llm, tools = tools, skillLoader = app.skillLoader,
            language    = language, soul = settings.getSoul(),
            personalMemory = settings.getPersonalMemory(), maxIterations = 8
        )
        pipeline.setEnabledSkills(settings.getEnabledSkillsList())

        for (rule in rules) {
            val msg = buildString {
                appendLine("Notification from app: $pkg")
                appendLine("From/Title: $title")
                if (text.isNotBlank()) appendLine("Text: ${text.take(300)}")
                if (rule.condition != null) {
                    appendLine()
                    appendLine("CONDITION: ${rule.condition}")
                    appendLine("Only execute the instruction if this condition is TRUE.")
                }
                appendLine()
                appendLine("INSTRUCTION: ${rule.instruction}")
            }
            try {
                val result = pipeline.processUtterance(msg, silent = true)
                if (result.isNotBlank() && !result.contains("__SILENT__")) {
                    withContext(Dispatchers.Main) { DoeyTTSEngine.speakAsync(result, language.ifBlank { "en-US" }) }
                }
            } catch (e: Exception) { Log.e(TAG, "Rule failed: ${e.message}") }
        }
    }
}
