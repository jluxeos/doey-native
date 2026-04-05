package com.doey.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ── Data classes ──────────────────────────────────────────────────────────────

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>
)

data class Message(
    val role: String,          // system | user | assistant | tool
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String?    = null
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
)

data class LLMResponse(
    val content: String,
    val toolCalls: List<ToolCall>,
    val finishReason: String   // stop | tool_calls
)

data class LLMOptions(
    val maxTokens: Int    = 4096,
    val temperature: Double = 0.7
)

// ── Provider interface ────────────────────────────────────────────────────────

interface LLMProvider {
    suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions = LLMOptions()
    ): LLMResponse

    fun getCurrentModel(): String

    suspend fun testConnection(): Pair<Boolean, String?>
}

// ── OpenAI-compatible provider (Groq / OpenAI / custom) ──────────────────────

class OpenAIProvider(
    private val apiKey: String,
    private val model: String  = "llama-3.3-70b-versatile",
    private val baseUrl: String = "https://api.groq.com/openai/v1/chat/completions"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    override fun getCurrentModel() = model

    override suspend fun testConnection(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user"); put("content", "hello")
                }))
                put("max_tokens", 5)
            }
            val resp = client.newCall(buildRequest(body)).execute()
            val ok   = resp.isSuccessful
            resp.close()
            Pair(ok, if (!ok) "HTTP ${resp.code}" else null)
        } catch (e: Exception) {
            Pair(false, e.message)
        }
    }

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val msgsJson = JSONArray().apply { messages.forEach { put(msgToJson(it)) } }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", msgsJson)
            put("max_tokens", options.maxTokens)
            put("temperature", options.temperature)
            if (tools.isNotEmpty()) {
                put("tools", JSONArray().apply { tools.forEach { put(toolToJson(it)) } })
                put("tool_choice", "auto")
            }
        }

        val resp     = client.newCall(buildRequest(body)).execute()
        val respBody = resp.body?.string() ?: throw Exception("Empty response body")
        if (!resp.isSuccessful) throw Exception("LLM Error ${resp.code}: $respBody")

        parseResponse(JSONObject(respBody))
    }

    private fun buildRequest(body: JSONObject): Request =
        Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

    private fun msgToJson(msg: Message): JSONObject = JSONObject().apply {
        put("role", msg.role)
        when {
            msg.role == "tool" -> {
                put("content", msg.content)
                put("tool_call_id", msg.toolCallId ?: "")
            }
            msg.role == "assistant" && !msg.toolCalls.isNullOrEmpty() -> {
                put("content", if (msg.content.isBlank()) JSONObject.NULL else msg.content)
                put("tool_calls", JSONArray().apply {
                    msg.toolCalls.forEach { tc ->
                        put(JSONObject().apply {
                            put("id", tc.id)
                            put("type", "function")
                            put("function", JSONObject().apply {
                                put("name", tc.name)
                                put("arguments", JSONObject(tc.arguments).toString())
                            })
                        })
                    }
                })
            }
            else -> put("content", msg.content)
        }
    }

    private fun toolToJson(tool: ToolDefinition): JSONObject = JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", tool.name)
            put("description", tool.description)
            put("strict", false)
            put("parameters", JSONObject(tool.parameters))
        })
    }

    private fun parseResponse(json: JSONObject): LLMResponse {
        val choice     = json.getJSONArray("choices").getJSONObject(0)
        val message    = choice.getJSONObject("message")
        val content    = message.optString("content", "")
        val finish     = choice.optString("finish_reason", "stop")
        val toolCalls  = mutableListOf<ToolCall>()
        val tcArray    = message.optJSONArray("tool_calls")
        if (tcArray != null) {
            for (i in 0 until tcArray.length()) {
                val tc   = tcArray.getJSONObject(i)
                val fn   = tc.getJSONObject("function")
                val args = try { JSONObject(fn.optString("arguments", "{}")) } catch (_: Exception) { JSONObject() }
                val map  = mutableMapOf<String, Any?>()
                args.keys().forEach { k -> map[k] = args.opt(k) }
                toolCalls.add(ToolCall(id = tc.getString("id"), name = fn.getString("name"), arguments = map))
            }
        }
        return LLMResponse(
            content     = content,
            toolCalls   = toolCalls,
            finishReason = if (finish == "tool_calls") "tool_calls" else "stop"
        )
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

object LLMProviderFactory {
    fun create(provider: String, apiKey: String, model: String, customUrl: String = ""): LLMProvider =
        when (provider) {
            "openai" -> OpenAIProvider(apiKey, model.ifBlank { "gpt-4o" },
                "https://api.openai.com/v1/chat/completions")
            "custom" -> OpenAIProvider(apiKey, model, customUrl)
            else     -> OpenAIProvider(apiKey, model.ifBlank { "llama-3.3-70b-versatile" },
                "https://api.groq.com/openai/v1/chat/completions")
        }
}
