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
    private var language: String = "es",
    private var soul: String = "",
    private var personalMemory: String = "",
    private var userName: String = "",
    private var maxIterations: Int = 7,
    private var maxHistoryMessages: Int = 12,
    private var expertMode: Boolean = false,
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

    // Cache de prompts — se invalidan solo cuando cambia configuración
    private var cachedNano: String = ""
    private var cachedMini: String = ""
    private var cachedFull: String = ""
    private var promptsDirty = true

    fun setEnabledSkills(names: List<String>) { enabledSkillNames = names }
    fun setDrivingMode(v: Boolean)            { drivingMode = v; promptsDirty = true }
    fun setLanguage(v: String)                { language = v; promptsDirty = true }
    fun setSoul(v: String)                    { soul = v; promptsDirty = true }
    fun setPersonalMemory(v: String)          { personalMemory = v; promptsDirty = true }
    fun setUserName(v: String)                { userName = v; promptsDirty = true }
    fun setExpertMode(v: Boolean)             { expertMode = v; promptsDirty = true }
    fun setProvider(p: LLMProvider)           { provider = p }
    fun clearHistory()                        { history.clear() }
    fun exportHistory(): List<Message>        = history.toList()
    fun importHistory(msgs: List<Message>)    { history.clear(); history.addAll(msgs) }
    fun appendToHistory(msgs: List<Message>)  { history.addAll(msgs); trimHistory() }
    fun setIdle()                             { _state.value = PipelineState.IDLE }
    fun setTokenOptimizerEnabled(v: Boolean)  { tokenOptimizerEnabled = v }
    fun setPromptCacheEnabled(v: Boolean)     { promptCacheEnabled = v; if (!v) promptsDirty = true }
    fun setHistoryCompressionEnabled(v: Boolean) { historyCompressionEnabled = v }

    fun startListening() {
        if (_state.value == PipelineState.IDLE || _state.value == PipelineState.SPEAKING)
            _state.value = PipelineState.LISTENING
    }
    fun stopListening() {
        if (_state.value == PipelineState.LISTENING)
            _state.value = PipelineState.IDLE
    }

    suspend fun processUtterance(
        userText: String,
        silent: Boolean = false,
        onSpeak: (suspend (text: String, lang: String) -> Unit)? = null
    ): String {
        when (_state.value) {
            PipelineState.LISTENING -> _state.value = PipelineState.PROCESSING
            PipelineState.IDLE      -> _state.value = PipelineState.PROCESSING
            else -> return ""
        }

        return try {
            if (!silent) onTranscript?.invoke("user", userText)
            Log.d(TAG, "Processing: ${userText.take(80)}")

            // ── Clasificar complejidad → elegir prompt y estrategia ──────────
            val complexity = if (tokenOptimizerEnabled)
                TokenOptimizer.classifyComplexity(userText)
            else
                TokenOptimizer.CommandComplexity.COMPLEX

            val strategy = TokenOptimizer.getStrategy(complexity, maxIterations)

            // ── System prompt según complejidad ───────────────────────────────
            // TRIVIAL/SIMPLE  → nano (~30 tokens) o mini (~80 tokens)
            // MODERATE/COMPLEX → full (~200-300 tokens)
            val systemPrompt = when {
                !tokenOptimizerEnabled -> getOrBuildFull()
                complexity == TokenOptimizer.CommandComplexity.TRIVIAL -> getOrBuildNano()
                strategy.useMinimalPrompt -> getOrBuildMini()
                else -> getOrBuildFull()
            }

            val userMsg = Message(role = "user", content = userText)

            // ── Historial comprimido ──────────────────────────────────────────
            val workingHistory = when {
                strategy.maxHistoryMessages == 0 -> emptyList()
                !historyCompressionEnabled -> history.takeLast(maxHistoryMessages)
                strategy.compressHistory && history.size > strategy.maxHistoryMessages ->
                    TokenOptimizer.compressHistory(history, keepLast = strategy.maxHistoryMessages)
                else -> history.takeLast(strategy.maxHistoryMessages)
            }
            val cleanHistory = if (tokenOptimizerEnabled)
                TokenOptimizer.deduplicateToolResults(workingHistory) else workingHistory

            val messages = listOf(Message(role = "system", content = systemPrompt)) +
                           cleanHistory + listOf(userMsg)

            history.add(userMsg)

            // ── maxTokens según complejidad ───────────────────────────────────
            val llmOptions = LLMOptions(
                maxTokens   = when (complexity) {
                    TokenOptimizer.CommandComplexity.TRIVIAL  -> 256
                    TokenOptimizer.CommandComplexity.SIMPLE   -> 256
                    TokenOptimizer.CommandComplexity.MODERATE -> 512
                    TokenOptimizer.CommandComplexity.COMPLEX  -> 768
                },
                // Acciones (SIMPLE/MODERATE/COMPLEX): temperatura baja = determinista, menos alucinaciones
                // Conocimiento/recomendaciones (TRIVIAL sin herramienta): temperatura media = más honesto
                temperature = when (complexity) {
                    TokenOptimizer.CommandComplexity.TRIVIAL  -> 0.7  // preguntas/recomendaciones
                    TokenOptimizer.CommandComplexity.SIMPLE   -> 0.2  // 1 acción directa
                    TokenOptimizer.CommandComplexity.MODERATE -> 0.1  // acción con contexto
                    TokenOptimizer.CommandComplexity.COMPLEX  -> 0.1  // encadenado — máximo determinismo
                }
            )

            val result = runToolLoop(
                provider      = provider,
                tools         = tools,
                messages      = messages,
                maxIterations = strategy.maxIterations,
                options       = llmOptions
            )

            val assistantText = result.content.ifBlank { "" }
            val isSilent = assistantText.contains("__SILENT__")

            history.addAll(result.newMessages)
            trimHistory()

            if (isSilent) { _state.value = PipelineState.IDLE; return "" }

            if (assistantText.isNotBlank()) onTranscript?.invoke("assistant", assistantText)

            if (drivingMode && onSpeak != null && assistantText.isNotBlank()) {
                _state.value = PipelineState.SPEAKING
                try { onSpeak(assistantText, language.ifBlank { "es-MX" }) }
                catch (e: Exception) { Log.e(TAG, "TTS: ${e.message}") }
                if (_state.value != PipelineState.LISTENING) _state.value = PipelineState.IDLE
            } else {
                if (_state.value != PipelineState.LISTENING) _state.value = PipelineState.IDLE
            }

            assistantText

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error: ${e.message}")
            _state.value = PipelineState.IDLE
            val msg = friendlyError(e.message ?: "")
            onTranscript?.invoke("assistant", msg)
            onError?.invoke(e.message ?: "Error")
            history.add(Message(role = "assistant", content = "[Error]"))
            msg
        }
    }

    private fun friendlyError(e: String) = when {
        e.contains("401") || e.contains("API key") ->
            "Hay un problema con la API key. Ve a Ajustes y verifícala."
        e.contains("429") || e.contains("limit") ->
            "Demasiadas peticiones seguidas. Espera un momento e intenta de nuevo."
        e.contains("timeout") || e.contains("connect") ->
            "Sin conexión. Revisa tu internet."
        e.contains("model") || e.contains("404") ->
            "Modelo no encontrado. Ve a Ajustes."
        else -> "Algo salió mal. Intenta de nuevo."
    }

    // ── Cache de prompts ──────────────────────────────────────────────────────

    private fun getOrBuildNano(): String {
        if (promptCacheEnabled && !promptsDirty && cachedNano.isNotBlank()) return cachedNano
        cachedNano = SystemPromptBuilder.buildNano(language)
        return cachedNano
    }

    private fun getOrBuildMini(): String {
        if (promptCacheEnabled && !promptsDirty && cachedMini.isNotBlank()) return cachedMini
        cachedMini = SystemPromptBuilder.buildMini(language, soul)
        return cachedMini
    }

    private fun getOrBuildFull(): String {
        if (promptCacheEnabled && !promptsDirty && cachedFull.isNotBlank()) return cachedFull
        cachedFull = SystemPromptBuilder.build(
            toolRegistry       = tools,
            drivingMode        = drivingMode,
            language           = language,
            soul               = soul,
            personalMemory     = personalMemory,
            expertMode         = expertMode,
            userName           = userName
        )
        promptsDirty = false
        return cachedFull
    }

    private fun trimHistory() {
        if (history.size <= maxHistoryMessages) return
        var start = history.size - maxHistoryMessages
        while (start < history.size) {
            val msg = history[start]
            if (msg.role == "tool") { start++; continue }
            if (msg.role == "assistant" && !msg.toolCalls.isNullOrEmpty()) { start++; continue }
            break
        }
        repeat(start) { if (history.isNotEmpty()) history.removeAt(0) }
    }
}
