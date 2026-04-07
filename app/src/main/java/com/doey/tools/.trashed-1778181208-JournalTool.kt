package com.doey.tools

class JournalTool : Tool {
    override fun name() = "journal"
    override fun description() = "Keep a personal journal or notes."
    override fun parameters() = mapOf("type" to "object", "properties" to mapOf("entry" to mapOf("type" to "string")), "required" to listOf("entry"))
    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val entry = args["entry"] as? String ?: return errorResult("entry required")
        return successResult("Logged: $entry")
    }
}
