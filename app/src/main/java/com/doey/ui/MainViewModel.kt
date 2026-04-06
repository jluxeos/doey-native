package com.doey.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doey.DoeyApplication
import com.doey.agent.ConversationPipeline
import com.doey.agent.PipelineState
import com.doey.llm.LLMProviderFactory
import com.doey.services.DoeySpeechEvents
import com.doey.services.DoeySpeechRecognizer
import com.doey.services.DoeyTTSEngine
import com.doey.services.WakeWordService
import com.doey.tools.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pipelineState: PipelineState = PipelineState.IDLE,
    val partialSpeech: String = "",
    val errorMessage: String? = null,
    val isDrivingMode: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val isListening: Boolean = false,
    val settingsSaved: Boolean = false,
    val isExpertMode: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app      = application as DoeyApplication
    private val settings = app.settingsStore

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pipeline: ConversationPipeline? = null
    private var speechRecognizer: DoeySpeechRecognizer? = null

    init {
        observeSpeechEvents()
        viewModelScope.launch { initPipeline() }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private suspend fun initPipeline() {
        val provider      = settings.getProvider()
        val model         = settings.getModel()
        val apiKey        = settings.getApiKey(provider)
        val customUrl     = settings.getCustomModelUrl()
        val language      = settings.getLanguage()
        val drivingMode   = settings.getDrivingMode()
        val soul          = settings.getSoul()
        val personalMem   = settings.getPersonalMemory()
        val enabledSkills = settings.getEnabledSkillsList()
        val maxIter       = settings.getMaxIterations()
        val expertMode    = settings.getExpertMode()

        if (apiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No API key configured. Go to Settings.") }
        }

        val llmProvider = LLMProviderFactory.create(provider, apiKey, model, customUrl)
        val skillLoader = app.skillLoader

        val tools = buildTools(skillLoader)

        val p = ConversationPipeline(
            provider       = llmProvider,
            tools          = tools,
            skillLoader    = skillLoader,
            drivingMode    = drivingMode,
            language       = language,
            soul           = soul,
            personalMemory = personalMem,
            maxIterations  = maxIter,
            expertMode     = expertMode
        )
        p.setEnabledSkills(enabledSkills)

        p.onTranscript = { role, text ->
            _uiState.update { s -> s.copy(messages = s.messages + ChatMessage(role = role, text = text)) }
        }
        p.onError = { error -> _uiState.update { it.copy(errorMessage = error) } }

        viewModelScope.launch {
            p.state.collect { state ->
                _uiState.update { it.copy(pipelineState = state, isListening = state == PipelineState.LISTENING) }
            }
        }

        pipeline = p
        _uiState.update { it.copy(isDrivingMode = drivingMode, isExpertMode = expertMode) }

        if (settings.getWakeWordEnabled()) startWakeWord()
    }

    private fun buildTools(skillLoader: com.doey.agent.SkillLoader) = ToolRegistry().apply {
        register(IntentTool())
        register(SmsTool())
        register(BeepTool())
        register(DateTimeTool())
        register(DeviceTool())
        register(QueryContactsTool())
        register(QuerySmsTool())
        register(QueryCallLogTool())
        register(HttpTool())
        register(TTSTool())
        register(AccessibilityTool())
        register(AppSearchTool())
        register(FileStorageTool())
        register(SkillDetailTool(skillLoader))
        register(PersonalMemoryTool())
        register(JournalTool())
        register(TimerTool())
        register(SchedulerTool())
        register(NotificationListenerTool())
        register(AlarmTool())
        register(AppSearchAndLaunchTool())
    }

    // ── User actions ──────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val p = pipeline ?: return
        viewModelScope.launch {
            p.processUtterance(
                userText = text,
                onSpeak  = if (_uiState.value.isDrivingMode) { t, lang ->
                    DoeyTTSEngine.speakAndWait(t, lang)
                } else null
            )
        }
    }

    fun startListening() {
        val p = pipeline ?: return
        if (_uiState.value.isListening) return
        p.startListening()
        _uiState.update { it.copy(partialSpeech = "") }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) speechRecognizer = DoeySpeechRecognizer(app)
                val lang = settings.getLanguage().let { if (it == "system") "en-US" else it }
                val mode = settings.getSttMode()
                val text = speechRecognizer!!.listen(lang, mode)
                _uiState.update { it.copy(partialSpeech = "") }
                if (text.isNotBlank()) sendMessage(text) else p.stopListening()
            } catch (e: Exception) {
                p.stopListening()
                _uiState.update { it.copy(errorMessage = "Speech error: ${e.message}") }
            }
        }
    }

    fun stopListening()  { speechRecognizer?.stop(); pipeline?.stopListening() }
    fun stopSpeaking()   { DoeyTTSEngine.stop(); pipeline?.setIdle() }
    fun clearHistory()   { pipeline?.clearHistory(); _uiState.update { it.copy(messages = emptyList()) } }
    fun clearError()     { _uiState.update { it.copy(errorMessage = null) } }

    fun toggleDrivingMode() {
        val next = !_uiState.value.isDrivingMode
        pipeline?.setDrivingMode(next)
        _uiState.update { it.copy(isDrivingMode = next) }
        viewModelScope.launch { settings.setDrivingMode(next) }
    }

    // ── Wake word ─────────────────────────────────────────────────────────────

    private suspend fun startWakeWord() {
        val key = settings.getPicovoiceKey()
        if (key.isBlank()) return
        WakeWordService.onWakeWord = { _ -> viewModelScope.launch(Dispatchers.Main) { startListening() } }
        val intent = Intent(app, WakeWordService::class.java).apply {
            putExtra(WakeWordService.EXTRA_ACCESS_KEY, key)
            putExtra(WakeWordService.EXTRA_KEYWORD_PATH, "PORCUPINE")
        }
        app.startForegroundService(intent)
        _uiState.update { it.copy(isWakeWordActive = true) }
    }

    fun toggleWakeWord() = viewModelScope.launch {
        if (_uiState.value.isWakeWordActive) {
            app.stopService(Intent(app, WakeWordService::class.java))
            WakeWordService.onWakeWord = null
            settings.setWakeWordEnabled(false)
            _uiState.update { it.copy(isWakeWordActive = false) }
        } else {
            settings.setWakeWordEnabled(true)
            startWakeWord()
        }
    }

    // ── Settings save ─────────────────────────────────────────────────────────

    fun saveSettings(
        provider: String, apiKey: String, model: String, customUrl: String,
        language: String, picovoiceKey: String, enabledSkills: List<String>,
        soul: String, personalMemory: String, maxIterations: Int, sttMode: String,
        expertMode: Boolean
    ) = viewModelScope.launch {
        settings.setProvider(provider)
        settings.setApiKey(provider, apiKey)
        settings.setModel(model)
        settings.setCustomModelUrl(customUrl)
        settings.setLanguage(language)
        settings.setPicovoiceKey(picovoiceKey)
        settings.setEnabledSkills(enabledSkills.joinToString(","))
        settings.setSoul(soul)
        settings.setPersonalMemory(personalMemory)
        settings.setMaxIterations(maxIterations)
        settings.setSttMode(sttMode)
        settings.setExpertMode(expertMode)
        _uiState.update { it.copy(settingsSaved = true) }
        pipeline = null
        initPipeline()
    }

    fun getSettings() = settings

    // ── Speech events ─────────────────────────────────────────────────────────

    private fun observeSpeechEvents() {
        DoeySpeechEvents.onPartialResult = { partial -> _uiState.update { it.copy(partialSpeech = partial) } }
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        super.onCleared()
    }
}