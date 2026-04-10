package com.doey.herramientas.comun

import com.doey.AplicacionDoey
import com.doey.servicios.basico.NotificationAccessManager
import org.json.JSONArray
import org.json.JSONObject

class NotificationListenerTool : Tool {
    override fun name() = "notifications"
    override fun description() = "Read or clear system notifications. Requires notification access."
    override fun parameters() = mapOf("type" to "object", "properties" to mapOf("action" to mapOf("type" to "string", "enum" to listOf("read", "clear"))), "required" to listOf("action"))
    override fun systemHint() = "Check if notification access is granted before using this tool."

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")
        val context = AplicacionDoey.instance

        if (!NotificationAccessManager.isAccessGranted(context)) {
            return errorResult("Notification access not granted. Please enable it in Android Settings.")
        }

        return when (action) {
            "read" -> {
                val notifications = NotificationAccessManager.getRecentNotifications(context)
                if (notifications.isEmpty()) {
                    successResult("No new notifications.")
                } else {
                    val sb = StringBuilder("Buffered Notifications:\n")
                    notifications.forEach { notif ->
                        sb.append("- App: ${notif.optString("packageName")}, Title: ${notif.optString("title")}, Text: ${notif.optString("text")}\n")
                    }
                    successResult(sb.toString())
                }
            }
            "clear" -> {
                NotificationAccessManager.clearBuffer(context)
                successResult("Notifications buffer cleared.")
            }
            else -> errorResult("Unknown action: $action")
        }
    }
}
