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
    private var maxIterations: Int = 8,           // Reducido de 10: 8 es más que suficiente
    private var maxHistoryMessages: Int = 16,      // Reducido de 20
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

    // ── Cache de system prompt (evita reconstruirlo en cada mensaje) ──────────
    private var cachedSystemPrompt: String = ""
    private var systemPromptDirty: Boolean = true
    private var cachedMinimalPrompt: String = ""
    private var minimalPromptDirty: Boolean = true

    fun setEnabledSkills(names: List<String>) { enabledSkillNames = names; systemPromptDirty = true; minimalPromptDirty = true }
    fun setDrivingMode(v: Boolean)            { drivingMode = v; systemPromptDirty = true; minimalPromptDirty = true }
    fun setLanguage(v: String)                { language = v; systemPromptDirty = true; minimalPromptDirty = true }
    fun setSoul(v: String)                    { soul = v; systemPromptDirty = true; minimalPromptDirty = true }
    fun setPersonalMemory(v: String)          { personalMemory = v; systemPromptDirty = true }
    fun setUserName(v: String)                { userName = v; systemPromptDirty = true }
    fun setExpertMode(v: Boolean)             { expertMode = v; systemPromptDirty = true }
    fun setProvider(p: LLMProvider)           { provider = p }
    fun clearHistory()                        { history.clear() }
    fun exportHistory(): List<Message>        = history.toList()
    fun importHistory(msgs: List<Message>)    { history.clear(); history.addAll(msgs) }
    fun appendToHistory(msgs: List<Message>)  { history.addAll(msgs); trimHistory() }
    fun setIdle()                             { _state.value = PipelineState.IDLE }
    fun setTokenOptimizerEnabled(v: Boolean)  { tokenOptimizerEnabled = v }
    fun setPromptCacheEnabled(v: Boolean)     { promptCacheEnabled = v; if (!v) { systemPromptDirty = true; minimalPromptDirty = true } }
    fun setHistoryCompressionEnabled(v: Boolean) { historyCompressionEnabled = v }

    fun startListening() {
        if (_state.value == PipelineState.IDLE || _state.value == PipelineState.SPEAKING) {
            _state.value = PipelineState.LISTENING
        }
    }

    fun stopListening() {
        if (_state.value == PipelineState.LISTENING) {
            _state.value = PipelineState.IDLE
        }
    }

    suspend fun processUtterance(
        userText: String,
        silent: Boolean = false,
        onSpeak: (suspend (text: String, lang: String) -> Unit)? = null
    ): String {
        when (_state.value) {
            PipelineState.LISTENING -> _state.value = PipelineState.PROCESSING
            PipelineState.IDLE      -> _state.value = PipelineState.PROCESSING
            else                    -> return ""
        }

        return try {
            if (!silent) onTranscript?.invoke("user", userText)
            Log.d(TAG, "Processing: ${userText.take(80)}")

            // ── Clasificar complejidad para elegir prompt ─────────────────────
            val complexity = if (tokenOptimizerEnabled)
                TokenOptimizer.classifyComplexity(userText)
            else
                TokenOptimizer.CommandComplexity.COMPLEX

            val strategy = TokenOptimizer.getStrategy(complexity, maxIterations)

            // ── Construir system prompt (mínimo o completo según complejidad) ──
            val systemPrompt = when {
                !tokenOptimizerEnabled -> getOrBuildFullPrompt()
                strategy.useMinimalPrompt -> getOrBuildMinimalPrompt()
                else -> getOrBuildFullPrompt()
            }

            val systemMsg = Message(role = "system", content = systemPrompt)
            val userMsg   = Message(role = "user",   content = userText)

            // ── Historial comprimido ──────────────────────────────────────────
            val workingHistory = when {
                !historyCompressionEnabled -> history.takeLast(maxHistoryMessages)
                strategy.compressHistory && history.size > strategy.maxHistoryMessages ->
                    TokenOptimizer.compressHistory(history, keepLast = strategy.maxHistoryMessages)
                else -> history.takeLast(strategy.maxHistoryMessages)
            }
            val cleanHistory = if (tokenOptimizerEnabled)
                TokenOptimizer.deduplicateToolResults(workingHistory)
            else workingHistory

            val messages = listOf(systemMsg) + cleanHistory + listOf(userMsg)
            history.add(userMsg)

            // ── Opciones LLM: maxTokens bajo porque la IA no genera texto largo ─
            val llmOptions = LLMOptions(
                maxTokens   = if (strategy.useMinimalPrompt) 512 else 1024,
                temperature = 0.1  // Más bajo = más determinista
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

            if (isSilent) {
                _state.value = PipelineState.IDLE
                return ""
            }

            if (assistantText.isNotBlank()) {
                onTranscript?.invoke("assistant", assistantText)
            }

            if (drivingMode && onSpeak != null && assistantText.isNotBlank()) {
                _state.value = PipelineState.SPEAKING
                try { onSpeak(assistantText, language.ifBlank { "es-MX" }) }
                catch (e: Exception) { Log.e(TAG, "TTS error: ${e.message}") }
                if (_state.value != PipelineState.LISTENING) _state.value = PipelineState.IDLE
            } else {
                if (_state.value != PipelineState.LISTENING) _state.value = PipelineState.IDLE
            }

            assistantText

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error: ${e.message}")
            _state.value = PipelineState.ERROR

            // ── Respuestas locales para errores comunes (cero tokens) ─────────
            val friendlyError = friendlyErrorMessage(e.message ?: "")
            onTranscript?.invoke("assistant", friendlyError)
            onError?.invoke(e.message ?: "Error desconocido")
            history.add(Message(role = "assistant", content = "[Error: ${e.message}]"))
            _state.value = PipelineState.IDLE
            friendlyError
        }
    }

    // ── Mensajes de error amigables sin gastar tokens ─────────────────────────
    private fun friendlyErrorMessage(errorMsg: String): String = when {
        errorMsg.contains("401") || errorMsg.contains("API key") ->
            "Hay un problema con mi configuración. Ve a Ajustes y revisa la API key."
        errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("quota") ->
            "Estoy muy ocupado ahora mismo. Espera un momento e intenta de nuevo."
        errorMsg.contains("timeout") || errorMsg.contains("connect") || errorMsg.contains("network") ->
            "No tengo conexión a internet. Revisa tu WiFi o datos."
        errorMsg.contains("model") && errorMsg.contains("not found") ->
            "El modelo configurado no existe. Ve a Ajustes y verifica el nombre."
        else -> "Algo salió mal. Intenta de nuevo."
    }

    // ── Cache de prompts ──────────────────────────────────────────────────────

    private fun getOrBuildFullPrompt(): String {
        if (promptCacheEnabled && !systemPromptDirty && cachedSystemPrompt.isNotBlank())
            return cachedSystemPrompt
        cachedSystemPrompt = SystemPromptBuilder.build(
            skillLoader        = skillLoader,
            toolRegistry       = tools,
            enabledSkillNames  = enabledSkillNames,
            drivingMode        = drivingMode,
            language           = language,
            soul               = soul,
            personalMemory     = personalMemory,
            expertMode         = expertMode,
            userName           = userName
        )
        systemPromptDirty = false
        return cachedSystemPrompt
    }

    private fun getOrBuildMinimalPrompt(): String {
        if (promptCacheEnabled && !minimalPromptDirty && cachedMinimalPrompt.isNotBlank())
            return cachedMinimalPrompt
        cachedMinimalPrompt = SystemPromptBuilder.buildMinimal(
            language     = language,
            soul         = soul,
            toolSummaries = tools.getSummaries()
        )
        minimalPromptDirty = false
        return cachedMinimalPrompt
    }

    private fun trimHistory() {
        if (history.size <= maxHistoryMessages) return
        var start = history.size - maxHistoryMessages
        // No cortar en medio de un bloque tool_call/tool
        while (start < history.size) {
            val msg = history[start]
            if (msg.role == "tool") { start++; continue }
            if (msg.role == "assistant" && !msg.toolCalls.isNullOrEmpty()) { start++; continue }
            break
        }
        repeat(start) { if (history.isNotEmpty()) history.removeAt(0) }
    }
}
