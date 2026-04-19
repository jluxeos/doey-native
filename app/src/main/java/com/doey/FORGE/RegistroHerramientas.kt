package com.doey.FORGE

import com.doey.ORACLE.ToolDefinition

// ── Result types ──────────────────────────────────────────────────────────────

data class ToolResult(
    val forLLM: String,
    val forUser: String? = null,
    val isError: Boolean = false
)

fun successResult(forLLM: String, forUser: String? = null) =
    ToolResult(forLLM = forLLM, forUser = forUser, isError = false)

fun errorResult(message: String) =
    ToolResult(forLLM = "Error: $message", isError = true)

// ── Tool interface ────────────────────────────────────────────────────────────

interface Tool {
    fun name(): String
    fun description(): String
    fun parameters(): Map<String, Any?>
    suspend fun execute(args: Map<String, Any?>): ToolResult
    fun systemHint(): String? = null
}

fun Tool.toDefinition() = ToolDefinition(
    name        = name(),
    description = description(),
    parameters  = parameters()
)

// ── Tool Registry ─────────────────────────────────────────────────────────────

class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()
    private var summaryCache: List<String>? = null

    fun register(tool: Tool) {
        tools[tool.name()] = tool
        summaryCache = null
    }

    fun unregister(name: String) {
        tools.remove(name)
        summaryCache = null
    }

    fun removeDisabledSkillTools(disabled: List<String>) {
        disabled.forEach { tools.remove(it) }
        if (disabled.isNotEmpty()) summaryCache = null
    }

    fun getTool(name: String): Tool? = tools[name]

    fun list(): List<String> = tools.keys.toList()

    fun definitions(): List<ToolDefinition> = tools.values.map { it.toDefinition() }

    fun getSummaries(): List<String> {
        summaryCache?.let { return it }
        val s = tools.values
            .sortedBy { it.name() }
            .map { t ->
                val hint = t.systemHint()
                val base = "- **${t.name()}**: ${t.description()}"
                if (hint != null) "$base — *Hint: $hint*" else base
            }
        summaryCache = s
        return s
    }

    suspend fun execute(name: String, args: Map<String, Any?>): ToolResult {
        val tool = tools[name] ?: return errorResult("Unknown tool: $name")
        return try {
            tool.execute(args)
        } catch (e: Exception) {
            errorResult("Tool execution failed: ${e.message}")
        }
    }
}
