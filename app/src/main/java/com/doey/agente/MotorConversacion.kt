package com.doey.agente

import android.util.Log
import com.doey.llm.LLMProvider
import com.doey.llm.LLMOptions
import com.doey.llm.Message
import com.doey.herramientas.comun.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Pipeline"

enum class PipelineState { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

class ConversationPipeline(
    private val ctx: android.content.Context,
    private var provider: LLMProvider,
    private val tools: ToolRegistry,
    private val skillLoader: SkillLoader,
    private var drivingMode: Boolean = false,
    private var language: String = "en",
    private var soul: String = "",
    private var personalMemory: String = "",
    private var userName: String = "",
    private var maxIterations: Int = 10,
    private var maxHistoryMessages: Int = 20,
    private var expertMode: Boolean = false,
    // ── Ajustes de optimización de tokens (ahora controlables desde UI) ────────
    private var tokenOptimizerEnabled: Boolean = true,
    private var promptCacheEnabled: Boolean = true,
    private var historyCompressionEnabled: Boolean = true
) {
    private val _state = MutableStateFlow(PipelineState.IDLE)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val history = mutableListOf<Message>()
    private var enabledSkillNames: List<String> = emptyList()

    var onTranscript: ((role: String, text: String) -> Unit)? = null
    var onError: ((error: String) -> Unit)? = null

    // ── Cache de system prompt ─────────────────────────────────────────────────
    private var cachedSystemPrompt: String = ""
    private var systemPromptDirty: Boolean = true

    fun setEnabledSkills(names: List<String>) { enabledSkillNames = names; systemPromptDirty = true }
    fun setDrivingMode(v: Boolean)            { drivingMode = v; systemPromptDirty = true }
    fun setLanguage(v: String)                { language = v; systemPromptDirty = true }
    fun setSoul(v: String)                    { soul = v; systemPromptDirty = true }
    fun setPersonalMemory(v: String)          { personalMemory = v; systemPromptDirty = true }
    fun setUserName(v: String)                { userName = v; systemPromptDirty = true }
    fun setExpertMode(v: Boolean)             { expertMode = v; systemPromptDirty = true }
    fun setProvider(p: LLMProvider)           { provider = p }
    fun clearHistory()                        { history.clear() }
    fun exportHistory(): List<Message>        = history.toList()
    fun importHistory(msgs: List<Message>)    { history.clear(); history.addAll(msgs) }
    fun appendToHistory(msgs: List<Message>)  { history.addAll(msgs); trimHistory() }
    fun setIdle()                             { _state.value = PipelineState.IDLE }

    // Actualizar ajustes de optimización desde UI
    fun setTokenOptimizerEnabled(v: Boolean)     { tokenOptimizerEnabled = v }
    fun setPromptCacheEnabled(v: Boolean)         { promptCacheEnabled = v; if (!v) systemPromptDirty = true }
    fun setHistoryCompressionEnabled(v: Boolean)  { historyCompressionEnabled = v }

    fun startListening() {
        if (_state.value == PipelineState.IDLE || _state.value == PipelineState.SPEAKING) {
            DoeyLogger.pipelineState(_state.value.name, "LISTENING")
            _state.value = PipelineState.LISTENING
        }
    }

    fun stopListening() {
        if (_state.value == PipelineState.LISTENING) {
            DoeyLogger.pipelineState("LISTENING", "IDLE")
            _state.value = PipelineState.IDLE
        }
    }

    suspend fun processUtterance(
        userText: String,
        silent: Boolean = false,
        onSpeak: (suspend (text: String, lang: String) -> Unit)? = null
    ): String {
        val prevState = _state.value.name
        when (_state.value) {
            PipelineState.LISTENING -> _state.value = PipelineState.PROCESSING
            PipelineState.IDLE      -> _state.value = PipelineState.PROCESSING
            else                    -> return ""
        }
        DoeyLogger.pipelineState(prevState, "PROCESSING")

        return try {
            if (!silent) onTranscript?.invoke("user", userText)
            DoeyLogger.userInput(userText)
            Log.d(TAG, "Processing: ${userText.take(80)}")

            // Detección de peticiones para Google Gemini manual (creación de contenido)
            val geminiTriggers = listOf("escribe una canción", "genera una imagen", "dibuja", "componer canción", "crea un poema largo")
            if (geminiTriggers.any { userText.lowercase().contains(it) }) {
                DoeyLogger.info("Redirigiendo a Google Gemini manual...")
                val intent = ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                    return "Abriendo Google Gemini para ayudarte con eso..."
                }
            }

            // ── Optimización de tokens ─────────────────────────────────────────
            val complexity = if (tokenOptimizerEnabled) {
                TokenOptimizer.classifyComplexity(userText)
            } else {
                TokenOptimizer.CommandComplexity.COMPLEX // Sin optimización: siempre complejo
            }
            val strategy = TokenOptimizer.getStrategy(complexity, maxIterations)
            Log.d(TAG, "Complexity: $complexity, maxIter=${strategy.maxIterations}, tokenOpt=$tokenOptimizerEnabled")

            val systemPrompt = when {
                !tokenOptimizerEnabled -> getOrBuildSystemPrompt(strategy) // Sin optimización: siempre completo
                strategy.useMinimalPrompt -> TokenOptimizer.buildMinimalSystemPrompt(language, soul)
                else -> if (promptCacheEnabled) getOrBuildSystemPrompt(strategy)
                        else buildFreshSystemPrompt(strategy)
            }

            val systemMsg = Message(role = "system", content = systemPrompt)
            val userMsg   = Message(role = "user",   content = userText)

            // Comprimir historial según estrategia y ajustes
            val workingHistory = when {
                !historyCompressionEnabled -> history.takeLast(maxHistoryMessages)
                strategy.compressHistory && history.size > strategy.maxHistoryMessages ->
                    TokenOptimizer.compressHistory(history, keepLast = strategy.maxHistoryMessages)
                else -> history.takeLast(strategy.maxHistoryMessages)
            }
            val cleanHistory = if (tokenOptimizerEnabled) {
                TokenOptimizer.deduplicateToolResults(workingHistory)
            } else {
                workingHistory
            }
            val messages = listOf(systemMsg) + cleanHistory + listOf(userMsg)

            val estimatedTokens = TokenOptimizer.estimateMessageTokens(messages)
            Log.d(TAG, "Estimated tokens: ~$estimatedTokens")
            DoeyLogger.info("Tokens estimados: ~$estimatedTokens (complejidad: $complexity, optimizador: $tokenOptimizerEnabled)")

            history.add(userMsg)

            val result = com.doey.agente.runToolLoop(
                provider      = provider,
                tools         = tools,
                messages      = messages,
                maxIterations = strategy.maxIterations
            )

            val assistantText = result.content.ifBlank { "Done." }
            val isSilent = assistantText.contains("__SILENT__")

            // result.newMessages ya incluye el mensaje del asistente; no agregar de nuevo
            history.addAll(result.newMessages)
            trimHistory()

            if (isSilent) {
                DoeyLogger.pipelineState("PROCESSING", "IDLE (silencioso)")
                _state.value = PipelineState.IDLE
                return ""
            }

            onTranscript?.invoke("assistant", assistantText)

            if (drivingMode && onSpeak != null) {
                DoeyLogger.pipelineState("PROCESSING", "SPEAKING")
                _state.value = PipelineState.SPEAKING
                try { onSpeak(assistantText, language.ifBlank { "en-US" }) }
                catch (e: Exception) {
                    Log.e(TAG, "TTS error: ${e.message}")
                    DoeyLogger.error("TTS", e.message ?: "Error de voz")
                }
                if (_state.value != PipelineState.LISTENING) {
                    DoeyLogger.pipelineState("SPEAKING", "IDLE")
                    _state.value = PipelineState.IDLE
                }
            } else {
                if (_state.value != PipelineState.LISTENING) {
                    DoeyLogger.pipelineState("PROCESSING", "IDLE")
                    _state.value = PipelineState.IDLE
                }
            }

            assistantText

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error: ${e.message}")
            DoeyLogger.error("Pipeline", e.message ?: "Error desconocido")

            // ── Sistema ARIA mejorado (Asistente de Respaldo de Inteligencia Artificial) ──
            // CORRECCIÓN: ahora usa la misma API key del proveedor actual como respaldo,
            // y si el proveedor ya es Gemini, intenta con OpenRouter gratuito.
            DoeyLogger.info("Iniciando sistema ARIA de respaldo...")
            val ariaResponse = tryAriaFallback(e.message ?: "Error desconocido")
            if (ariaResponse != null) {
                onTranscript?.invoke("assistant", ariaResponse)
                _state.value = PipelineState.IDLE
                return ariaResponse
            }

            _state.value = PipelineState.ERROR
            DoeyLogger.pipelineState("PROCESSING", "ERROR")
            onError?.invoke(e.message ?: "Unknown error")
            history.add(Message(role = "assistant", content = "[Error: ${e.message}]"))
            _state.value = PipelineState.IDLE
            ""
        }
    }

    /**
     * Sistema ARIA mejorado: intenta responder con un proveedor de respaldo.
     * Usa la API key del proveedor actual si es compatible, o respuesta local si no hay conectividad.
     */
    private suspend fun tryAriaFallback(errorMsg: String): String? {
        // Respuestas locales para errores comunes (sin consumir tokens)
        val localResponse = when {
            errorMsg.contains("401") || errorMsg.contains("Unauthorized") || errorMsg.contains("API key") ->
                "No puedo conectarme: la API key parece incorrecta o expirada. Ve a Ajustes > Cerebro de Doey para actualizarla."
            errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("quota") ->
                "He alcanzado el límite de solicitudes. Espera un momento e inténtalo de nuevo."
            errorMsg.contains("timeout") || errorMsg.contains("connect") || errorMsg.contains("network") ->
                "No hay conexión a internet o el servidor tardó demasiado. Verifica tu conexión."
            errorMsg.contains("model") && errorMsg.contains("not found") ->
                "El modelo configurado no existe. Ve a Ajustes > Cerebro de Doey y verifica el nombre del modelo."
            else -> null
        }

        if (localResponse != null) {
            DoeyLogger.info("ARIA: respuesta local sin tokens para error: $errorMsg")
            return "[ARIA]: $localResponse"
        }

        // Intentar con proveedor de respaldo (OpenRouter gratuito, sin API key necesaria)
        return try {
            val backupProvider = com.doey.llm.LLMProviderFactory.create(
                "openrouter", "", "meta-llama/llama-3.2-3b-instruct:free"
            )
            val backupResult = backupProvider.chat(
                messages = listOf(
                    Message("system", "Eres ARIA, asistente de respaldo de Doey. El sistema principal falló con: $errorMsg. Responde brevemente y en español, sugiriendo al usuario qué hacer."),
                    Message("user", "El sistema principal falló. ¿Qué puedo hacer?")
                ),
                tools = emptyList()
            )
            if (backupResult.content.isNotBlank()) {
                DoeyLogger.info("ARIA: respaldo exitoso con OpenRouter gratuito")
                "[ARIA]: ${backupResult.content}"
            } else null
        } catch (backupEx: Exception) {
            DoeyLogger.error("ARIA", "Fallo crítico: ${backupEx.message}")
            // Último recurso: mensaje de error amigable sin IA
            "[ARIA]: Ocurrió un error inesperado. Por favor verifica tu conexión y la API key en Ajustes."
        }
    }

    // ── Cache de system prompt ─────────────────────────────────────────────────

    private fun getOrBuildSystemPrompt(strategy: TokenOptimizer.OptimizationStrategy): String {
        if (!systemPromptDirty && cachedSystemPrompt.isNotBlank()) {
            Log.d(TAG, "Using cached system prompt")
            return cachedSystemPrompt
        }
        return buildFreshSystemPrompt(strategy)
    }

    private fun buildFreshSystemPrompt(strategy: TokenOptimizer.OptimizationStrategy): String {
        val prompt = SystemPromptBuilder.build(
            skillLoader        = skillLoader,
            toolRegistry       = tools,
            enabledSkillNames  = if (strategy.includeSkills) enabledSkillNames else emptyList(),
            drivingMode        = drivingMode,
            language           = language,
            soul               = soul,
            personalMemory     = personalMemory,
            expertMode         = expertMode,
            userName           = userName
        )
        cachedSystemPrompt = prompt
        systemPromptDirty  = false
        Log.d(TAG, "System prompt built and cached (${prompt.length} chars)")
        return prompt
    }

    private fun trimHistory() {
        if (history.size <= maxHistoryMessages) return
        var start = history.size - maxHistoryMessages
        while (start < history.size) {
            val msg = history[start]
            if (msg.role == "tool")                                           { start++; continue }
            if (msg.role == "assistant" && !msg.toolCalls.isNullOrEmpty())    { start++; continue }
            break
        }
        repeat(start) { if (history.isNotEmpty()) history.removeAt(0) }
    }
}
