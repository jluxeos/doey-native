package com.doey.agent

import android.util.Log
import com.doey.llm.LLMProvider
import com.doey.llm.LLMOptions
import com.doey.llm.Message
import com.doey.tools.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Pipeline"

enum class PipelineState { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

class ConversationPipeline(
    private var provider: LLMProvider,
    private val tools: ToolRegistry,
    private val skillLoader: SkillLoader,
    private var drivingMode: Boolean = false,
    private var language: String = "en",
    private var soul: String = "",
    private var personalMemory: String = "",
    private var maxIterations: Int = 10,
    private var maxHistoryMessages: Int = 20,
    private var expertMode: Boolean = false
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
    fun setDrivingMode(v: Boolean)           { drivingMode = v; systemPromptDirty = true }
    fun setLanguage(v: String)               { language = v; systemPromptDirty = true }
    fun setSoul(v: String)                   { soul = v; systemPromptDirty = true }
    fun setPersonalMemory(v: String)         { personalMemory = v; systemPromptDirty = true }
    fun setExpertMode(v: Boolean)            { expertMode = v; systemPromptDirty = true }
    fun setProvider(p: LLMProvider)          { provider = p }
    fun clearHistory()                       { history.clear() }
    fun exportHistory(): List<Message>       = history.toList()
    fun importHistory(msgs: List<Message>)   { history.clear(); history.addAll(msgs) }
    fun appendToHistory(msgs: List<Message>) { history.addAll(msgs); trimHistory() }
    fun setIdle()                            { _state.value = PipelineState.IDLE }

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

            // ── Optimización de tokens ─────────────────────────────────────
            val complexity = TokenOptimizer.classifyComplexity(userText)
            val strategy   = TokenOptimizer.getStrategy(complexity, maxIterations)
            Log.d(TAG, "Complexity: $complexity, maxIter=${strategy.maxIterations}")

            val systemPrompt = if (strategy.useMinimalPrompt) {
                TokenOptimizer.buildMinimalSystemPrompt(language, soul)
            } else {
                getOrBuildSystemPrompt(strategy)
            }

            val systemMsg = Message(role = "system", content = systemPrompt)
            val userMsg   = Message(role = "user",   content = userText)

            // Comprimir historial según estrategia
            val workingHistory = if (strategy.compressHistory && history.size > strategy.maxHistoryMessages) {
                TokenOptimizer.compressHistory(history, keepLast = strategy.maxHistoryMessages)
            } else {
                history.takeLast(strategy.maxHistoryMessages)
            }
            val cleanHistory = TokenOptimizer.deduplicateToolResults(workingHistory)
            val messages = listOf(systemMsg) + cleanHistory + listOf(userMsg)

            val estimatedTokens = TokenOptimizer.estimateMessageTokens(messages)
            Log.d(TAG, "Estimated tokens: ~$estimatedTokens")
            DoeyLogger.info("Tokens estimados: ~$estimatedTokens (complejidad: $complexity)")

            history.add(userMsg)

            val result = com.doey.agent.runToolLoop(
                provider      = provider,
                tools         = tools,
                messages      = messages,
                maxIterations = strategy.maxIterations
            )

            val assistantText = result.content.ifBlank { "Done." }
            val isSilent = assistantText.contains("__SILENT__")

            history.addAll(result.newMessages)
            history.add(Message(role = "assistant", content = assistantText))
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
            _state.value = PipelineState.ERROR
            DoeyLogger.pipelineState("PROCESSING", "ERROR")
            onError?.invoke(e.message ?: "Unknown error")
            history.add(Message(role = "assistant", content = "[Error: ${e.message}]"))
            _state.value = PipelineState.IDLE
            ""
        }
    }

    // ── Cache de system prompt ─────────────────────────────────────────────────

    private fun getOrBuildSystemPrompt(strategy: TokenOptimizer.OptimizationStrategy): String {
        if (!systemPromptDirty && cachedSystemPrompt.isNotBlank()) {
            Log.d(TAG, "Using cached system prompt")
            return cachedSystemPrompt
        }
        val prompt = SystemPromptBuilder.build(
            skillLoader        = skillLoader,
            toolRegistry       = tools,
            enabledSkillNames  = if (strategy.includeSkills) enabledSkillNames else emptyList(),
            drivingMode        = drivingMode,
            language           = language,
            soul               = soul,
            personalMemory     = personalMemory,
            expertMode         = expertMode
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
            if (msg.role == "tool")                                   { start++; continue }
            if (msg.role == "assistant" && !msg.toolCalls.isNullOrEmpty()) { start++; continue }
            break
        }
        repeat(start) { if (history.isNotEmpty()) history.removeAt(0) }
    }
}
