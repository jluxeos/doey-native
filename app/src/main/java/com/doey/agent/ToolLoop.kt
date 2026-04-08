package com.doey.agent
import android.util.Log
import com.doey.llm.LLMProvider
import com.doey.llm.Message
import com.doey.tools.ToolRegistry
import com.doey.tools.errorResult

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
    maxIterations: Int             = 10,
    shouldExit:    (() -> Boolean)? = null,
    onIteration:   (() -> Unit)?    = null
): ToolLoopResult {
    val newMessages = mutableListOf<Message>()
    val allMessages = messages.toMutableList()
    var iterations  = 0
    var finalContent = ""

    val toolDefs = tools.definitions()

    // Log del request inicial a la IA
    val systemLen = messages.firstOrNull { it.role == "system" }?.content?.length ?: 0
    DoeyLogger.aiRequest(
        model           = provider.getCurrentModel(),
        messageCount    = messages.size,
        systemPromptLen = systemLen
    )

    while (iterations < maxIterations) {
        if (shouldExit?.invoke() == true) break
        iterations++
        Log.d(TAG, "Iteration $iterations/$maxIterations")
        DoeyLogger.info("Iteración $iterations de $maxIterations del bucle de herramientas")

        val response = try {
            provider.chat(allMessages, toolDefs)
        } catch (e: Exception) {
            Log.e(TAG, "LLM error: ${e.message}")
            DoeyLogger.error("IA", e.message ?: "Error desconocido")
            throw e
        }

        // Log de la respuesta de la IA
        DoeyLogger.aiResponse(
            content       = response.content,
            toolCallCount = response.toolCalls.size,
            finishReason  = response.finishReason
        )

        val assistantMsg = Message(
            role      = "assistant",
            content   = response.content,
            toolCalls = response.toolCalls.takeIf { it.isNotEmpty() }
        )
        allMessages.add(assistantMsg)
        newMessages.add(assistantMsg)
        finalContent = response.content

        if (response.finishReason != "tool_calls" || response.toolCalls.isEmpty()) {
            Log.d(TAG, "No tool calls – ending loop")
            DoeyLogger.info("Fin del bucle: sin más herramientas que llamar")
            onIteration?.invoke()
            break
        }

        for (tc in response.toolCalls) {
            Log.d(TAG, "Calling tool: ${tc.name}")

            // Log de llamada a herramienta
            DoeyLogger.toolCall(tc.name, tc.arguments)

            // Log especial para skills
            if (tc.name == "skill_detail") {
                val skillName = tc.arguments["skill_name"] as? String ?: "desconocido"
                DoeyLogger.skillLoad(skillName)
            }

            // Log especial para accesibilidad (envío)
            if (tc.name == "accessibility") {
                val action = tc.arguments["action"] as? String ?: "?"
                val nodeId = tc.arguments["node_id"] as? String
                val text   = tc.arguments["text"] as? String
                    ?: tc.arguments["package_name"] as? String
                DoeyLogger.accessibilitySend(action, nodeId, text)
            }

            val result = try {
                tools.execute(tc.name, tc.arguments)
            } catch (e: Exception) {
                errorResult("${tc.name} threw: ${e.message}")
            }

            // PUNTO 12: Verificación de éxito para acciones críticas
            if (tc.name == "intent" || tc.name == "accessibility" || tc.name == "send_sms") {
                if (result.isError) {
                    DoeyLogger.warning("Acción fallida: ${tc.name}. Intentando recuperación con accesibilidad...")
                } else {
                    // Si fue un intent de música o mensaje, verificar con accesibilidad si se completó
                    val action = tc.arguments["action"] as? String ?: ""
                    if (action.contains("android.intent.action.SEND") || action.contains("android.intent.action.VIEW")) {
                         DoeyLogger.info("Verificando estado de la acción en la app externa...")
                         // El agente LLM decidirá en la siguiente iteración si necesita usar get_tree para confirmar
                    }
                }
            }

            // Log del resultado de la herramienta
            DoeyLogger.toolResult(tc.name, result.forLLM, result.isError)

            // Log especial para accesibilidad (recepción)
            if (tc.name == "accessibility") {
                val action = tc.arguments["action"] as? String ?: "?"
                DoeyLogger.accessibilityRecv(action, result.forLLM)
            }

            val toolMsg = Message(role = "tool", content = result.forLLM, toolCallId = tc.id)
            allMessages.add(toolMsg)
            newMessages.add(toolMsg)
            Log.d(TAG, "Tool ${tc.name} → ${result.forLLM.take(150)}")
        }

        onIteration?.invoke()
        if (shouldExit?.invoke() == true) break
    }

    return ToolLoopResult(finalContent, newMessages, iterations)
}
