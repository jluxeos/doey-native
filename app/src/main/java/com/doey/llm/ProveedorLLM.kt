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
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String?        = null
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>
)

data class LLMResponse(
    val content: String,
    val toolCalls: List<ToolCall>,
    val finishReason: String
)

data class LLMOptions(
    val maxTokens: Int      = 4096,
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

// ── OpenAI-compatible (Groq / OpenRouter / custom) ────────────────────────────

class OpenAIProvider(
    private val apiKey: String,
    private val model: String   = "llama-3.3-70b-versatile",
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
            val err  = if (!ok) parseError(resp.code, resp.body?.string() ?: "") else null
            resp.close()
            Pair(ok, err)
        } catch (e: Exception) { Pair(false, e.message) }
    }

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply { messages.forEach { put(msgToJson(it)) } })
            put("max_tokens", options.maxTokens)
            put("temperature", options.temperature)
            if (tools.isNotEmpty()) {
                put("tools", JSONArray().apply { tools.forEach { put(toolToJson(it)) } })
                put("tool_choice", "auto")
                put("top_p", 1.0)
                put("frequency_penalty", 0.0)
                put("presence_penalty", 0.0)
            }
        }
        val resp = client.newCall(buildRequest(body)).execute()
        val rb   = resp.body?.string() ?: throw Exception("Cuerpo vacío")
        if (!resp.isSuccessful) throw Exception(parseError(resp.code, rb))
        parseResponse(JSONObject(rb))
    }

    private fun buildRequest(body: JSONObject) =
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
                            put("id", tc.id); put("type", "function")
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

    private fun toolToJson(tool: ToolDefinition) = JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", tool.name)
            put("description", tool.description)
            put("strict", false)
            put("parameters", JSONObject(tool.parameters))
        })
    }

    private fun parseResponse(json: JSONObject): LLMResponse {
        val choice  = json.getJSONArray("choices").getJSONObject(0)
        val message = choice.getJSONObject("message")
        val content = message.optString("content", "")
        val finish  = choice.optString("finish_reason", "stop")
        val calls   = mutableListOf<ToolCall>()
        message.optJSONArray("tool_calls")?.let { arr ->
            for (i in 0 until arr.length()) {
                val tc  = arr.getJSONObject(i)
                val fn  = tc.getJSONObject("function")
                val raw = try { JSONObject(fn.optString("arguments", "{}")) } catch (_: Exception) { JSONObject() }
                val map = mutableMapOf<String, Any?>()
                raw.keys().forEach { k -> map[k] = raw.opt(k) }
                calls.add(ToolCall(tc.getString("id"), fn.getString("name"), map))
            }
        }
        return LLMResponse(content, calls, if (finish == "tool_calls") "tool_calls" else "stop")
    }

    private fun parseError(code: Int, body: String): String = try {
        val json = JSONObject(body)
        val msg  = json.optJSONObject("error")?.optString("message", "") ?: json.optString("message", "")
        when {
            msg.isNotBlank() -> "Error $code: $msg"
            code == 401 -> "Error 401: API key inválida"
            code == 429 -> "Error 429: Límite alcanzado. Espera un momento."
            code == 404 -> "Error 404: Modelo '$model' no encontrado."
            code == 500 -> "Error 500: Error interno del servidor."
            else -> "Error HTTP $code"
        }
    } catch (_: Exception) { "Error HTTP $code" }
}

// ── Gemini ────────────────────────────────────────────────────────────────────

class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val baseUrl get() =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    override fun getCurrentModel() = model

    override suspend fun testConnection(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply { put("text", "hola") }))
                }))
                put("generationConfig", JSONObject().apply { put("maxOutputTokens", 5) })
            }
            val resp = client.newCall(buildRequest(body)).execute()
            val ok   = resp.isSuccessful
            val err  = if (!ok) parseGeminiError(resp.code, resp.body?.string() ?: "") else null
            resp.close()
            Pair(ok, err)
        } catch (e: Exception) { Pair(false, e.message) }
    }

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val systemMsg = messages.firstOrNull { it.role == "system" }
        val convMsgs  = messages.filter { it.role != "system" }

        val contents = JSONArray().apply {
            convMsgs.forEach { msg ->
                when (msg.role) {
                    "user" -> put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply { put("text", msg.content) }))
                    })
                    "assistant" -> {
                        if (!msg.toolCalls.isNullOrEmpty()) {
                            put(JSONObject().apply {
                                put("role", "model")
                                put("parts", JSONArray().apply {
                                    msg.toolCalls.forEach { tc ->
                                        put(JSONObject().apply {
                                            put("functionCall", JSONObject().apply {
                                                put("name", tc.name)
                                                put("args", JSONObject(tc.arguments))
                                            })
                                        })
                                    }
                                })
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "model")
                                put("parts", JSONArray().put(JSONObject().apply { put("text", msg.content) }))
                            })
                        }
                    }
                    "tool" -> put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", msg.toolCallId ?: "tool")
                                put("response", JSONObject().apply { put("output", msg.content) })
                            })
                        }))
                    })
                }
            }
        }

        if (contents.length() == 0) {
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", "Hola") }))
            })
        }

        val body = JSONObject().apply {
            if (systemMsg != null) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemMsg.content) }))
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", options.maxTokens)
                put("temperature", options.temperature)
            })
            if (tools.isNotEmpty()) {
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        tools.forEach { t ->
                            put(JSONObject().apply {
                                put("name", t.name)
                                put("description", t.description)
                                put("parameters", cleanSchema(JSONObject(t.parameters)))
                            })
                        }
                    })
                }))
            }
        }

        val resp = client.newCall(buildRequest(body)).execute()
        val rb   = resp.body?.string() ?: throw Exception("Cuerpo vacío")
        if (!resp.isSuccessful) throw Exception(parseGeminiError(resp.code, rb))
        parseGeminiResponse(JSONObject(rb))
    }

    private fun cleanSchema(obj: JSONObject): JSONObject {
        val result    = JSONObject()
        val blacklist = setOf("additionalProperties", "\$schema", "default")
        obj.keys().forEach { key ->
            if (key in blacklist) return@forEach
            when (val v = obj.get(key)) {
                is JSONObject -> result.put(key, cleanSchema(v))
                is JSONArray  -> result.put(key, cleanArray(v))
                else          -> result.put(key, v)
            }
        }
        return result
    }

    private fun cleanArray(arr: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until arr.length()) {
            when (val v = arr.get(i)) {
                is JSONObject -> result.put(cleanSchema(v))
                is JSONArray  -> result.put(cleanArray(v))
                else          -> result.put(v)
            }
        }
        return result
    }

    private fun buildRequest(body: JSONObject) =
        Request.Builder()
            .url(baseUrl)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

    private fun parseGeminiResponse(json: JSONObject): LLMResponse {
        if (json.has("error")) {
            val e = json.getJSONObject("error")
            throw Exception(parseGeminiError(e.optInt("code", 0), json.toString()))
        }
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val reason = json.optJSONObject("promptFeedback")?.optString("blockReason", "")
            if (!reason.isNullOrBlank()) throw Exception("Gemini bloqueó: $reason")
            throw Exception("Gemini sin candidatos en respuesta")
        }
        val candidate = candidates.getJSONObject(0)
        val finishRaw = candidate.optString("finishReason", "STOP")
        if (finishRaw == "SAFETY")    throw Exception("Gemini bloqueó por seguridad. Reformula.")
        if (finishRaw == "RECITATION") throw Exception("Gemini detectó plagio. Cambia la pregunta.")

        val content = candidate.optJSONObject("content") ?: return LLMResponse("", emptyList(), "stop")
        val parts   = content.optJSONArray("parts")       ?: return LLMResponse("", emptyList(), "stop")

        val calls     = mutableListOf<ToolCall>()
        val textParts = mutableListOf<String>()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            when {
                part.has("functionCall") -> {
                    val fc  = part.getJSONObject("functionCall")
                    val args = fc.optJSONObject("args") ?: JSONObject()
                    val map  = mutableMapOf<String, Any?>()
                    args.keys().forEach { k -> map[k] = args.opt(k) }
                    calls.add(ToolCall("gemini_${fc.getString("name")}_$i", fc.getString("name"), map))
                }
                part.has("text") -> textParts.add(part.getString("text"))
            }
        }
        return LLMResponse(textParts.joinToString(""), calls, if (calls.isNotEmpty()) "tool_calls" else "stop")
    }

    private fun parseGeminiError(code: Int, body: String): String = try {
        val msg = JSONObject(body).optJSONObject("error")?.optString("message", "") ?: ""
        when {
            msg.isNotBlank() -> "Gemini $code: $msg"
            code == 401 -> "Gemini 401: API key inválida."
            code == 403 -> "Gemini 403: Sin permisos para este modelo."
            code == 404 -> "Gemini 404: Modelo '$model' no encontrado. Prueba 'gemini-2.5-flash'."
            code == 429 -> "Gemini 429: Límite alcanzado. Espera un momento."
            else -> "Gemini Error HTTP $code"
        }
    } catch (_: Exception) { "Gemini Error HTTP $code" }
}

// ── Pollinations (GRATIS, sin API key) ────────────────────────────────────────

class PollinationsProvider(
    private val model: String = "openai"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL   = "https://text.pollinations.ai/openai"

    override fun getCurrentModel() = model

    override suspend fun testConnection(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user"); put("content", "hola")
                }))
                put("max_tokens", 5)
            }
            val resp = client.newCall(buildRequest(body)).execute()
            val ok   = resp.isSuccessful
            val err  = if (!ok) "Pollinations Error ${resp.code}" else null
            resp.close()
            Pair(ok, err)
        } catch (e: Exception) { Pair(false, e.message) }
    }

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
            put("max_tokens", options.maxTokens)
            put("temperature", options.temperature)
        }
        val resp = client.newCall(buildRequest(body)).execute()
        val rb   = resp.body?.string() ?: throw Exception("Cuerpo vacío")
        if (!resp.isSuccessful) throw Exception("Pollinations Error ${resp.code}: $rb")
        val json    = JSONObject(rb)
        val choice  = json.getJSONArray("choices").getJSONObject(0)
        val content = choice.getJSONObject("message").optString("content", "")
        LLMResponse(content, emptyList(), "stop")
    }

    private fun buildRequest(body: JSONObject) =
        Request.Builder()
            .url(BASE_URL)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
}

// ── Factory ────────────────────────────────────────────────────────────────────

object LLMProviderFactory {
    fun create(provider: String, apiKey: String, model: String, customUrl: String = ""): LLMProvider =
        when (provider) {
            "gemini"       -> GeminiProvider(apiKey, model.ifBlank { "gemini-2.5-flash" })
            "groq"         -> OpenAIProvider(apiKey, model.ifBlank { "llama-3.3-70b-versatile" },
                                             "https://api.groq.com/openai/v1/chat/completions")
            "openrouter"   -> OpenAIProvider(apiKey, model.ifBlank { "openrouter/auto" },
                                             "https://openrouter.ai/api/v1/chat/completions")
            "pollinations" -> PollinationsProvider(model.ifBlank { "openai" })
            "custom"       -> OpenAIProvider(apiKey, model,
                                             customUrl.ifBlank { "https://api.openai.com/v1/chat/completions" })
            else           -> GeminiProvider(apiKey, model.ifBlank { "gemini-2.5-flash" })
        }
}
