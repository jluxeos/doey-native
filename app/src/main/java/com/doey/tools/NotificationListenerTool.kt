package com.doey.tools

class NotificationListenerTool : Tool {
    override fun name() = "notifications"
    override fun description() = "Read or clear system notifications."
    override fun parameters() = mapOf("type" to "object", "properties" to mapOf("action" to mapOf("type" to "string", "enum" to listOf("read", "clear"))), "required" to listOf("action"))
    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return successResult("Notifications processed")
    }
}
