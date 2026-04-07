package com.doey.tools

import com.doey.services.DoeyNotificationListenerService
import com.doey.services.NotificationAccessManager
import org.json.JSONObject

class NotificationListenerTool : Tool {
    override fun name() = "notifications"
    override fun description() = "Read or clear system notifications. Requires notification access."
    override fun parameters() = mapOf("type" to "object", "properties" to mapOf("action" to mapOf("type" to "string", "enum" to listOf("read", "clear"))), "required" to listOf("action"))
    override fun systemHint() = "Check if notification access is granted before using this tool."

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")

        if (!NotificationAccessManager.isAccessGranted()) {
            return errorResult("Notification access not granted. Please enable it in Android Settings.")
        }

        val service = DoeyNotificationListenerService.instance
            ?: return errorResult("Notification listener service not running.")

        return when (action) {
            "read" -> {
                val notifications = service.getBufferedNotifications()
                if (notifications.length() == 0) {
                    successResult("No new notifications.")
                } else {
                    val sb = StringBuilder("Buffered Notifications:\n")
                    for (i in 0 until notifications.length()) {
                        val notif = notifications.getJSONObject(i)
                        sb.append("- App: ${notif.optString("packageName")}, Title: ${notif.optString("title")}, Text: ${notif.optString("text")}\n")
                    }
                    successResult(sb.toString())
                }
            }
            "clear" -> {
                service.clearBufferedNotifications()
                successResult("Notifications buffer cleared.")
            }
            else -> errorResult("Unknown action: $action")
        }
    }
}
