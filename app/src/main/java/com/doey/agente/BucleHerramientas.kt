package com.doey.agente

import android.util.Log
import com.doey.llm.LLMProvider
import com.doey.llm.LLMOptions
import com.doey.llm.Message
import com.doey.herramientas.comun.ToolRegistry
import com.doey.herramientas.comun.errorResult

private const val TAG = "ToolLoop"

data class ToolLoopResult(
    val content: String,
    val newMessages: List<Message>,
    val iterations: Int
)

suspend fun runToolLoop(
    provider:      LLMProvider,
    tools:         ToolRegistry,
    messages:      List<Message>,
    maxIterations: Int              = 8,
    options:       LLMOptions       = LLMOptions(),
    shouldExit:    (() -> Boolean)?  = null,
    onIteration:   (() -> Unit)?     = null
): ToolLoopResult {
    val newMessages  = mutableListOf<Message>()
    val allMessages  = messages.toMutableList()
    var iterations   = 0
    var finalContent = ""

    val toolDefs = tools.definitions()

    DoeyLogger.aiRequest(
        model           = provider.getCurrentModel(),
        messageCount    = messages.size,
        systemPromptLen = messages.firstOrNull { it.role == "system" }?.content?.length ?: 0
    )

    while (iterations < maxIterations) {
        if (shouldExit?.invoke() == true) break
        iterations++
        Log.d(TAG, "Iteración $iterations/$maxIterations")

        val response = try {
            provider.chat(allMessages, toolDefs, options)
        } catch (e: Exception) {
            Log.e(TAG, "LLM error: ${e.message}")
            DoeyLogger.error("IA", e.message ?: "Error desconocido")
            throw e
        }

        DoeyLogger.aiResponse(
            content       = response.content,
            toolCallCount = response.toolCalls.size,
            finishReason  = response.finishReason
        )

        // ── Detección de tool calls alucinadas (modelos débiles escriben JSON como texto)
        val hallucinatedCall = response.finishReason == "stop"
            && response.toolCalls.isEmpty()
            && (response.content.contains("TOOLCALL>") ||
                (response.content.contains("tool_call") && response.content.contains(""""name""")))
        if (hallucinatedCall) {
            DoeyLogger.info("Tool call alucinada detectada — solicitando reintento estructurado")
            val retryMsg = Message(
                role    = "user",
                content = "Usa la herramienta de forma estructurada, no como texto."
            )
            allMessages.add(retryMsg)
            newMessages.add(retryMsg)
            onIteration?.invoke()
            continue
        }

        val assistantMsg = Message(
            role      = "assistant",
            content   = response.content,
            toolCalls = response.toolCalls.takeIf { it.isNotEmpty() }
        )
        allMessages.add(assistantMsg)
        newMessages.add(assistantMsg)
        finalContent = response.content

        // Sin más herramientas que llamar — fin del bucle
        if (response.finishReason != "tool_calls" || response.toolCalls.isEmpty()) {
            onIteration?.invoke()
            break
        }

        // ── Ejecutar herramientas ─────────────────────────────────────────────
        for (tc in response.toolCalls) {
            Log.d(TAG, "Herramienta: ${tc.name}")
            DoeyLogger.toolCall(tc.name, tc.arguments)

            if (tc.name == "skill_detail") {
                DoeyLogger.skillLoad(tc.arguments["skill_name"] as? String ?: "?")
            }
            if (tc.name == "accessibility") {
                val action = tc.arguments["action"] as? String ?: "?"
                DoeyLogger.accessibilitySend(action, tc.arguments["node_id"] as? String,
                    (tc.arguments["text"] ?: tc.arguments["package_name"]) as? String)
            }

            val result = try {
                tools.execute(tc.name, tc.arguments)
            } catch (e: Exception) {
                errorResult("${tc.name}: ${e.message}")
            }

            DoeyLogger.toolResult(tc.name, result.forLLM, result.isError)

            if (tc.name == "accessibility") {
                val action = tc.arguments["action"] as? String ?: "?"
                DoeyLogger.accessibilityRecv(action, result.forLLM)
            }

            val toolMsg = Message(
                role       = "tool",
                content    = result.forLLM,
                toolCallId = tc.id
            )
            allMessages.add(toolMsg)
            newMessages.add(toolMsg)
            Log.d(TAG, "Tool ${tc.name} → ${result.forLLM.take(100)}")
        }

        onIteration?.invoke()
        if (shouldExit?.invoke() == true) break
    }

    return ToolLoopResult(finalContent, newMessages, iterations)
}
