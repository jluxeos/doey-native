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

    while (iterations < maxIterations) {
        if (shouldExit?.invoke() == true) break
        iterations++
        Log.d(TAG, "Iteration $iterations/$maxIterations")

        val response = try {
            provider.chat(allMessages, toolDefs)
        } catch (e: Exception) {
            Log.e(TAG, "LLM error: ${e.message}")
            throw e
        }

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
            onIteration?.invoke()
            break
        }

        for (tc in response.toolCalls) {
            Log.d(TAG, "Calling tool: ${tc.name}")
            val result = try {
                tools.execute(tc.name, tc.arguments)
            } catch (e: Exception) {
                errorResult("${tc.name} threw: ${e.message}")
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
