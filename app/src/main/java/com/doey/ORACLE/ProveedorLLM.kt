package com.doey.ORACLE

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
    val maxTokens: Int      = 512,
    val temperature: Double = 0.1
)

// ── Interfaz ──────────────────────────────────────────────────────────────────

interface LLMProvider {
    suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions = LLMOptions()
    ): LLMResponse
    fun getCurrentModel(): String
    suspend fun testConnection(): Pair<Boolean, String?>
}

// ── Gemini ────────────────────────────────────────────────────────────────────

class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
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
                        } else if (msg.content.isNotBlank()) {
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
        val rb   = resp.body?.string() ?: throw Exception("Sin respuesta del servidor")
        if (!resp.isSuccessful) throw Exception(parseError(resp.code, rb))
        parseResponse(JSONObject(rb))
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

    private fun parseResponse(json: JSONObject): LLMResponse {
        if (json.has("error")) {
            val e = json.getJSONObject("error")
            throw Exception(parseError(e.optInt("code", 0), json.toString()))
        }
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val reason = json.optJSONObject("promptFeedback")?.optString("blockReason", "")
            if (!reason.isNullOrBlank()) throw Exception("Bloqueado: $reason")
            throw Exception("Sin respuesta")
        }
        val candidate = candidates.getJSONObject(0)
        if (candidate.optString("finishReason") == "SAFETY")
            throw Exception("Bloqueado por seguridad. Reformula.")

        val content = candidate.optJSONObject("content") ?: return LLMResponse("", emptyList(), "stop")
        val parts   = content.optJSONArray("parts")       ?: return LLMResponse("", emptyList(), "stop")

        val calls     = mutableListOf<ToolCall>()
        val textParts = mutableListOf<String>()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            when {
                part.has("functionCall") -> {
                    val fc   = part.getJSONObject("functionCall")
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

    private fun parseError(code: Int, body: String): String = try {
        val msg = JSONObject(body).optJSONObject("error")?.optString("message", "") ?: ""
        when {
            msg.isNotBlank() -> msg
            code == 401 -> "API key inválida o expirada"
            code == 403 -> "Sin permisos para este modelo"
            code == 404 -> "Modelo '$model' no encontrado. Prueba gemini-2.5-flash"
            code == 429 -> "Límite de peticiones alcanzado. Espera un momento"
            code == 500 -> "Error interno de Gemini. Reintenta"
            else -> "Error $code"
        }
    } catch (_: Exception) { "Error $code" }

    /** Devuelve true si el error es recuperable y merece fallback a Groq */
    fun isTransientError(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("high demand") ||
               lower.contains("overloaded") ||
               lower.contains("503") ||
               lower.contains("502") ||
               lower.contains("temporarily") ||
               lower.contains("try again") ||
               lower.contains("currently unavailable") ||
               lower.contains("quota") ||
               lower.contains("rate limit") ||
               lower.contains("429")
    }
}

// ── Groq ──────────────────────────────────────────────────────────────────────
// Fallback gratuito cuando Gemini está saturado.
// Modelos recomendados: llama-3.3-70b-versatile, llama3-8b-8192 (más rápido)

class GroqProvider(
    private val apiKey: String,
    private val model: String = "llama-3.3-70b-versatile"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL   = "https://api.groq.com/openai/v1/chat/completions"

    override fun getCurrentModel() = model

    override suspend fun testConnection(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 5)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user"); put("content", "hola")
                }))
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
        val msgs = JSONArray().apply {
            messages.forEach { msg ->
                when (msg.role) {
                    "system", "user" -> put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                    "assistant" -> {
                        if (!msg.toolCalls.isNullOrEmpty()) {
                            put(JSONObject().apply {
                                put("role", "assistant")
                                put("content", JSONObject.NULL)
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
                            })
                        } else {
                            put(JSONObject().apply {
                                put("role", "assistant")
                                put("content", msg.content)
                            })
                        }
                    }
                    "tool" -> put(JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", msg.toolCallId ?: "")
                        put("content", msg.content)
                    })
                }
            }
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", options.maxTokens)
            put("temperature", options.temperature)
            put("messages", msgs)
            if (tools.isNotEmpty()) {
                put("tools", JSONArray().apply {
                    tools.forEach { t ->
                        put(JSONObject().apply {
                            put("type", "function")
                            put("function", JSONObject().apply {
                                put("name", t.name)
                                put("description", t.description)
                                put("parameters", JSONObject(t.parameters))
                            })
                        })
                    }
                })
                put("tool_choice", "auto")
            }
        }

        val resp = client.newCall(buildRequest(body)).execute()
        val rb   = resp.body?.string() ?: throw Exception("Sin respuesta de Groq")
        if (!resp.isSuccessful) throw Exception(parseError(resp.code, rb))
        parseResponse(JSONObject(rb))
    }

    private fun buildRequest(body: JSONObject) =
        Request.Builder()
            .url(BASE_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

    private fun parseResponse(json: JSONObject): LLMResponse {
        if (json.has("error")) {
            val e = json.getJSONObject("error")
            throw Exception(e.optString("message", "Error desconocido de Groq"))
        }
        val choices = json.optJSONArray("choices") ?: throw Exception("Sin respuesta de Groq")
        if (choices.length() == 0) throw Exception("Sin respuesta de Groq")
        val choice  = choices.getJSONObject(0)
        val message = choice.optJSONObject("message") ?: return LLMResponse("", emptyList(), "stop")

        val toolCallsJson = message.optJSONArray("tool_calls")
        val calls = mutableListOf<ToolCall>()
        if (toolCallsJson != null) {
            for (i in 0 until toolCallsJson.length()) {
                val tc   = toolCallsJson.getJSONObject(i)
                val fn   = tc.getJSONObject("function")
                val args = try { JSONObject(fn.getString("arguments")) } catch (_: Exception) { JSONObject() }
                val map  = mutableMapOf<String, Any?>()
                args.keys().forEach { k -> map[k] = args.opt(k) }
                calls.add(ToolCall(tc.optString("id", "groq_$i"), fn.getString("name"), map))
            }
        }
        val text = message.optString("content", "")
        return LLMResponse(text, calls, if (calls.isNotEmpty()) "tool_calls" else "stop")
    }

    private fun parseError(code: Int, body: String): String = try {
        val msg = JSONObject(body).optJSONObject("error")?.optString("message", "") ?: ""
        when {
            msg.isNotBlank() -> msg
            code == 401 -> "API key de Groq inválida o expirada"
            code == 429 -> "Límite de Groq alcanzado. Espera un momento"
            code == 503 -> "Groq no disponible temporalmente"
            else -> "Error Groq $code"
        }
    } catch (_: Exception) { "Error Groq $code" }
}

// ── FallbackProvider — Gemini primero, Groq si Gemini falla ──────────────────
//
// Lógica:
//  1. Intenta con Gemini (primary)
//  2. Si el error es transitorio (alta demanda, 429, 503…) intenta con Groq
//  3. Si Groq también falla, relanza el error original de Gemini
//  4. getCurrentModel() muestra cuál respondió en último turno

class FallbackProvider(
    private val primary: GeminiProvider,
    private val fallback: GroqProvider?          // null si no hay API key de Groq
) : LLMProvider {

    @Volatile private var lastUsed: LLMProvider = primary

    override fun getCurrentModel() = lastUsed.getCurrentModel()

    override suspend fun testConnection() = primary.testConnection()

    override suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDefinition>,
        options: LLMOptions
    ): LLMResponse {
        return try {
            val result = primary.chat(messages, tools, options)
            lastUsed = primary
            result
        } catch (primaryError: Exception) {
            val shouldFallback = fallback != null && primary.isTransientError(primaryError.message ?: "")
            if (shouldFallback) {
                try {
                    android.util.Log.w("FallbackProvider",
                        "Gemini no disponible (${primaryError.message}), usando Groq como fallback")
                    com.doey.NEXUS.DoeyLogger.info(
                        "Fallback → Groq",
                        "Gemini falló: ${primaryError.message?.take(120)}"
                    )
                    val result = fallback!!.chat(messages, tools, options)
                    lastUsed = fallback
                    result
                } catch (fallbackError: Exception) {
                    lastUsed = primary
                    android.util.Log.e("FallbackProvider",
                        "Groq también falló: ${fallbackError.message}")
                    com.doey.NEXUS.DoeyLogger.error(
                        "FallbackProvider",
                        "Groq también falló: ${fallbackError.message?.take(120)}"
                    )
                    throw primaryError  // relanzar el error original
                }
            } else {
                lastUsed = primary
                throw primaryError
            }
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

object LLMProviderFactory {
    fun create(
        provider: String = "gemini",
        apiKey: String,
        model: String = "",
        customUrl: String = "",
        groqApiKey: String = ""                  // opcional: activa el fallback
    ): LLMProvider {
        return when (provider) {
            "groq" -> GroqProvider(
                apiKey = apiKey,
                model  = model.ifBlank { "llama-3.3-70b-versatile" }
            )
            else -> {
                // Gemini con fallback automático a Groq si hay API key
                val gemini = GeminiProvider(
                    apiKey = apiKey,
                    model  = model.ifBlank { "gemini-2.5-flash" }
                )
                val groq = if (groqApiKey.isNotBlank()) {
                    GroqProvider(apiKey = groqApiKey)
                } else null
                FallbackProvider(primary = gemini, fallback = groq)
            }
        }
    }
}

// ── RotatingProvider — mantenido por compatibilidad ───────────────────────────
class RotatingProvider(private val providers: List<LLMProvider>) : LLMProvider {
    override fun getCurrentModel() = providers.firstOrNull()?.getCurrentModel() ?: "gemini-2.5-flash"
    override suspend fun testConnection() = providers.firstOrNull()?.testConnection() ?: Pair(false, "Sin proveedor")
    override suspend fun chat(messages: List<Message>, tools: List<ToolDefinition>, options: LLMOptions) =
        providers.first().chat(messages, tools, options)
}
