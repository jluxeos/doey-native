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
    val maxTokens: Int     = 4096,
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

// ── OpenAI-compatible provider (Groq / OpenAI / OpenRouter / custom) ──────────

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
            val errorMsg = if (!ok) {
                val bodyStr = resp.body?.string() ?: ""
                parseErrorMessage(resp.code, bodyStr)
            } else null
            resp.close()
            Pair(ok, errorMsg)
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
                put("top_p", 1.0)
                put("frequency_penalty", 0.0)
                put("presence_penalty", 0.0)
            }
        }

        val resp     = client.newCall(buildRequest(body)).execute()
        val respBody = resp.body?.string() ?: throw Exception("Cuerpo de respuesta vacío")
        if (!resp.isSuccessful) {
            throw Exception(parseErrorMessage(resp.code, respBody))
        }

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
        val choice    = json.getJSONArray("choices").getJSONObject(0)
        val message   = choice.getJSONObject("message")
        val content   = message.optString("content", "")
        val finish    = choice.optString("finish_reason", "stop")
        val toolCalls = mutableListOf<ToolCall>()
        val tcArray   = message.optJSONArray("tool_calls")
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
            content      = content,
            toolCalls    = toolCalls,
            finishReason = if (finish == "tool_calls") "tool_calls" else "stop"
        )
    }

    /** Extrae un mensaje de error legible del cuerpo de respuesta HTTP */
    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val msg = errorObj?.optString("message") ?: json.optString("message", "")
            when {
                msg.isNotBlank() -> "Error $code: $msg"
                code == 401 -> "Error 401: API key inválida o sin autorización"
                code == 429 -> "Error 429: Límite de solicitudes alcanzado. Espera un momento."
                code == 404 -> "Error 404: Modelo '$model' no encontrado. Verifica el nombre en Ajustes."
                code == 500 -> "Error 500: Error interno del servidor. Intenta de nuevo."
                code == 503 -> "Error 503: Servicio no disponible temporalmente."
                else -> "Error HTTP $code"
            }
        } catch (_: Exception) {
            "Error HTTP $code"
        }
    }
}

// ── Gemini provider (Generative Language API con function calling) ─────────────

class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash-preview-04-17"
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
            val errorMsg = if (!ok) {
                val bodyStr = resp.body?.string() ?: ""
                parseGeminiError(resp.code, bodyStr)
            } else null
            resp.close()
            Pair(ok, errorMsg)
        } catch (e: Exception) {
            Pair(false, e.message)
        }
    }

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse = withContext(Dispatchers.IO) {

        // Separar system prompt de los mensajes de conversación
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

        // Si no hay mensajes de conversación, agregar un mensaje vacío para evitar error
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
                        tools.forEach { tool ->
                            put(JSONObject().apply {
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", cleanSchema(JSONObject(tool.parameters)))
                            })
                        }
                    })
                }))
            }
        }

        val resp     = client.newCall(buildRequest(body)).execute()
        val respBody = resp.body?.string() ?: throw Exception("Cuerpo de respuesta vacío")
        if (!resp.isSuccessful) {
            throw Exception(parseGeminiError(resp.code, respBody))
        }

        parseGeminiResponse(JSONObject(respBody))
    }

    /** Limpia el schema JSON para compatibilidad con Gemini */
    private fun cleanSchema(obj: JSONObject): JSONObject {
        val result = JSONObject()
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

    private fun buildRequest(body: JSONObject): Request =
        Request.Builder()
            .url(baseUrl)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

    private fun parseGeminiResponse(json: JSONObject): LLMResponse {
        // Verificar si hay error en la respuesta
        if (json.has("error")) {
            val error = json.getJSONObject("error")
            val msg   = error.optString("message", "Error desconocido de Gemini")
            val code  = error.optInt("code", 0)
            throw Exception(parseGeminiError(code, json.toString()))
        }

        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            // Verificar si hay promptFeedback con bloqueo
            val feedback = json.optJSONObject("promptFeedback")
            val blockReason = feedback?.optString("blockReason", "")
            if (!blockReason.isNullOrBlank()) {
                throw Exception("Gemini bloqueó la solicitud: $blockReason. Intenta reformular tu pregunta.")
            }
            throw Exception("Gemini no devolvió candidatos en la respuesta")
        }

        val candidate = candidates.getJSONObject(0)

        // Verificar finish reason
        val finishRaw = candidate.optString("finishReason", "STOP")
        if (finishRaw == "SAFETY") {
            throw Exception("Gemini bloqueó la respuesta por políticas de seguridad. Intenta reformular.")
        }
        if (finishRaw == "RECITATION") {
            throw Exception("Gemini detectó posible plagio. Intenta con una pregunta diferente.")
        }

        val content   = candidate.optJSONObject("content")
            ?: return LLMResponse("", emptyList(), "stop")
        val parts     = content.optJSONArray("parts")
            ?: return LLMResponse("", emptyList(), "stop")

        val toolCalls  = mutableListOf<ToolCall>()
        val textParts  = mutableListOf<String>()

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            when {
                part.has("functionCall") -> {
                    val fc   = part.getJSONObject("functionCall")
                    val args = fc.optJSONObject("args") ?: JSONObject()
                    val map  = mutableMapOf<String, Any?>()
                    args.keys().forEach { k -> map[k] = args.opt(k) }
                    toolCalls.add(
                        ToolCall(
                            id        = "gemini_${fc.getString("name")}_$i",
                            name      = fc.getString("name"),
                            arguments = map
                        )
                    )
                }
                part.has("text") -> textParts.add(part.getString("text"))
            }
        }

        return LLMResponse(
            content      = textParts.joinToString(""),
            toolCalls    = toolCalls,
            finishReason = if (toolCalls.isNotEmpty()) "tool_calls" else "stop"
        )
    }

    /** Extrae un mensaje de error legible de la respuesta de Gemini */
    private fun parseGeminiError(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val msg   = error?.optString("message", "") ?: ""
            when {
                msg.isNotBlank() -> "Gemini Error $code: $msg"
                code == 400 -> "Gemini Error 400: Solicitud inválida. Verifica el modelo y los parámetros."
                code == 401 -> "Gemini Error 401: API key de Gemini inválida. Ve a Ajustes para actualizarla."
                code == 403 -> "Gemini Error 403: Sin acceso. Verifica que tu API key tenga permisos para este modelo."
                code == 404 -> "Gemini Error 404: Modelo '$model' no encontrado. Prueba con 'gemini-2.5-flash-preview-04-17'."
                code == 429 -> "Gemini Error 429: Límite de solicitudes alcanzado. Espera un momento."
                code == 500 -> "Gemini Error 500: Error interno. Intenta de nuevo en unos segundos."
                code == 503 -> "Gemini Error 503: Servicio no disponible. Intenta más tarde."
                else -> "Gemini Error HTTP $code"
            }
        } catch (_: Exception) {
            "Gemini Error HTTP $code"
        }
    }
}

// ── Factory ────────────────────────────────────────────────────────────────────

object LLMProviderFactory {
    /**
     * Modelos por defecto actualizados y verificados (Abril 2025):
     * - Gemini: gemini-2.5-flash-preview-04-17 (más reciente y estable)
     * - Groq: llama-3.3-70b-versatile (mejor relación calidad/velocidad)
     * - OpenRouter: meta-llama/llama-3.2-3b-instruct:free (gratuito y funcional)
     * - OpenAI: gpt-4o-mini (más económico que gpt-4o)
     */
    fun create(provider: String, apiKey: String, model: String, customUrl: String = ""): LLMProvider =
        when (provider) {
            "openrouter" -> OpenAIProvider(
                apiKey  = apiKey,
                model   = when {
                    model.isNotBlank() -> model
                    else -> "meta-llama/llama-3.2-3b-instruct:free" // Gratuito y funcional
                },
                baseUrl = "https://openrouter.ai/api/v1/chat/completions"
            )
            "gemini" -> GeminiProvider(
                apiKey = apiKey,
                model  = when {
                    model.isNotBlank() -> model
                    else -> "gemini-2.5-flash-preview-04-17" // Modelo más reciente y estable
                }
            )
            "openai" -> OpenAIProvider(
                apiKey  = apiKey,
                model   = model.ifBlank { "gpt-4o-mini" }, // Más económico por defecto
                baseUrl = "https://api.openai.com/v1/chat/completions"
            )
            "groq" -> OpenAIProvider(
                apiKey  = apiKey,
                model   = model.ifBlank { "llama-3.3-70b-versatile" },
                baseUrl = "https://api.groq.com/openai/v1/chat/completions"
            )
            "custom" -> OpenAIProvider(
                apiKey  = apiKey,
                model   = model,
                baseUrl = customUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
            )
            else -> OpenAIProvider(
                apiKey  = apiKey,
                model   = model.ifBlank { "meta-llama/llama-3.2-3b-instruct:free" },
                baseUrl = "https://openrouter.ai/api/v1/chat/completions"
            )
        }
}
