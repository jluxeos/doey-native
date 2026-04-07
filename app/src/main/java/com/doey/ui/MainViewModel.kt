package com.doey.ui

import android.app.Application
import android.content.Intent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doey.agent.ConversationPipeline
import com.doey.agent.PipelineState
import com.doey.agent.SettingsStore
import com.doey.agent.SkillLoader
import com.doey.services.DoeyNotificationListenerService
import com.doey.services.DoeySpeechEvents
import com.doey.services.DoeySpeechRecognizer
import com.doey.services.DoeyTTSEngine
import com.doey.services.NowPlayingInfo
import com.doey.services.NowPlayingRepository
import com.doey.services.WakeWordService
import com.doey.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
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
    val isExpertMode: Boolean = false,
    val nowPlaying: NowPlayingInfo = NowPlayingInfo()
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val settings = SettingsStore(app)
    private var pipeline: ConversationPipeline? = null
    private var speechRecognizer: DoeySpeechRecognizer? = null
    private var driveListenJob: Job? = null

    init {
        initPipeline()
        observeSpeechEvents()
        observeNowPlaying()
    }

    private fun initPipeline() = viewModelScope.launch {
        val provider   = settings.getProvider()
        val model      = settings.getModel()
        val language   = settings.getLanguage()
        val drivingMode = settings.getDrivingMode().let { it } // force collect
        val expertMode  = settings.getExpertMode().let { it }

        val skillLoader = SkillLoader(app)
        val tools       = buildTools(skillLoader)
        
        val p = ConversationPipeline(
            provider           = com.doey.llm.LLMProviderFactory.create(provider, settings.getApiKey(provider), model, settings.getCustomModelUrl()),
            tools              = tools,
            skillLoader        = skillLoader,
            drivingMode        = drivingMode,
            language           = resolveLanguage(language),
            soul               = settings.getSoul(),
            personalMemory     = settings.getPersonalMemory(),
            maxIterations      = settings.getMaxIterations(),
            expertMode         = expertMode
        )

        p.onTranscript = { role, text ->
            _uiState.update { s ->
                val newList = s.messages + ChatMessage(role = role, text = text)
                s.copy(messages = newList.takeLast(50))
            }
        }
        p.onError = { error -> _uiState.update { it.copy(errorMessage = error) } }

        viewModelScope.launch {
            p.state.collect { state ->
                _uiState.update { it.copy(pipelineState = state, isListening = state == PipelineState.LISTENING) }
            }
        }

        pipeline = p
        _uiState.update { it.copy(isDrivingMode = drivingMode, isExpertMode = expertMode) }

        // Iniciar WakeWord si está habilitado en settings
        viewModelScope.launch {
            if (settings.getWakeWordEnabled()) {
                startWakeWord()
            }
        }
    }

    private fun buildTools(skillLoader: SkillLoader) = ToolRegistry().apply {
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

    // ── Observadores ─────────────────────────────────────────────────────────

    private fun observeNowPlaying() {
        viewModelScope.launch {
            NowPlayingRepository.nowPlaying.collect { info ->
                _uiState.update { it.copy(nowPlaying = info) }
            }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────────

    fun sendMessage(text: String, voiceEnabled: Boolean = true) {
        if (text.isBlank()) return
        val p = pipeline ?: return
        viewModelScope.launch {
            p.processUtterance(
                userText = text,
                onSpeak  = if (voiceEnabled) { t, lang ->
                    DoeyTTSEngine.speakAndWait(t, lang)
                } else null
            )
            // En modo conducción con voz, reanudar escucha automáticamente
            if (_uiState.value.isDrivingMode && voiceEnabled) {
                delay(500)
                startDrivingListen()
            }
        }
    }

    fun startListening() {
        val p = pipeline ?: return
        if (_uiState.value.isListening) return
        
        // Si la wake word está activa, detenerla temporalmente para no interferir
        val wasWakeWordActive = _uiState.value.isWakeWordActive
        if (wasWakeWordActive) {
            app.stopService(Intent(app, WakeWordService::class.java))
        }

        p.startListening()
        _uiState.update { it.copy(partialSpeech = "") }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) speechRecognizer = DoeySpeechRecognizer(app)
                val lang = resolveLanguage(settings.getLanguage())
                val mode = settings.getSttMode()
                val text = speechRecognizer!!.listen(lang, mode)
                _uiState.update { it.copy(partialSpeech = "") }
                
                if (text.isNotBlank()) {
                    sendMessage(text)
                } else {
                    p.stopListening()
                }
            } catch (e: Exception) {
                p.stopListening()
                _uiState.update { it.copy(errorMessage = "Speech error: ${e.message}") }
            } finally {
                // Reanudar WakeWord si estaba activa
                if (wasWakeWordActive && !_uiState.value.isDrivingMode) {
                    startWakeWord()
                }
            }
        }
    }

    /**
     * Inicia una sesión de escucha única para el modo conducción.
     */
    fun startDrivingListen() {
        val p = pipeline ?: return
        if (_uiState.value.isListening || _uiState.value.pipelineState == PipelineState.PROCESSING) return

        driveListenJob?.cancel()
        driveListenJob = viewModelScope.launch(Dispatchers.Main) {
            p.startListening()
            _uiState.update { it.copy(partialSpeech = "") }
            try {
                if (speechRecognizer == null) speechRecognizer = DoeySpeechRecognizer(app)
                val lang = resolveLanguage(settings.getLanguage())
                val text = speechRecognizer!!.listen(lang, "auto")
                _uiState.update { it.copy(partialSpeech = "") }
                if (text.isNotBlank()) {
                    sendMessage(text, voiceEnabled = true)
                } else {
                    p.stopListening()
                }
            } catch (e: Exception) {
                p.stopListening()
            }
        }
    }

    fun stopDrivingListen() {
        driveListenJob?.cancel()
        speechRecognizer?.stop()
        pipeline?.stopListening()
    }

    fun dispatchMediaKey(keyCode: Int) {
        NowPlayingRepository.dispatchMediaKey(app, keyCode)
    }

    fun stopListening()  { speechRecognizer?.stop(); pipeline?.stopListening() }
    fun stopSpeaking()   { DoeyTTSEngine.stop(); pipeline?.setIdle() }
    fun clearHistory()   { pipeline?.clearHistory(); _uiState.update { it.copy(messages = emptyList()) } }
    fun clearError()     { _uiState.update { it.copy(errorMessage = null) } }

    fun toggleDrivingMode() {
        val next = !_uiState.value.isDrivingMode
        pipeline?.setDrivingMode(next)
        _uiState.update { it.copy(isDrivingMode = next) }
        viewModelScope.launch { 
            settings.setDrivingMode(next)
            // En modo conducción, si la wake word está activa, reiniciarla para asegurar que use el modo correcto
            if (_uiState.value.isWakeWordActive) {
                startWakeWord()
            }
        }
        if (!next) stopDrivingListen()
    }

    // ── Wake word ─────────────────────────────────────────────────────────────

    private suspend fun startWakeWord() {
        val phrase   = settings.getWakePhrase()
        val language = resolveLanguage(settings.getLanguage())

        WakeWordService.onWakeWord = { _ ->
            viewModelScope.launch(Dispatchers.Main) { 
                if (_uiState.value.isDrivingMode) {
                    startDrivingListen()
                } else {
                    startListening()
                }
            }
        }

        val intent = Intent(app, WakeWordService::class.java).apply {
            putExtra(WakeWordService.EXTRA_WAKE_PHRASE, phrase)
            putExtra(WakeWordService.EXTRA_LANGUAGE,    language)
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
        language: String, wakePhrase: String, enabledSkills: List<String>,
        soul: String, personalMemory: String, maxIterations: Int, sttMode: String,
        expertMode: Boolean
    ) = viewModelScope.launch {
        settings.setProvider(provider)
        settings.setApiKey(provider, apiKey)
        settings.setModel(model)
        settings.setCustomModelUrl(customUrl)
        settings.setLanguage(language)
        settings.setWakePhrase(wakePhrase)
        settings.setEnabledSkills(enabledSkills.joinToString(","))
        settings.setSoul(soul)
        settings.setPersonalMemory(personalMemory)
        settings.setMaxIterations(maxIterations)
        settings.setSttMode(sttMode)
        settings.setExpertMode(expertMode)
        _uiState.update { it.copy(settingsSaved = true) }
        
        // Reiniciar pipeline con nuevos ajustes
        pipeline = null
        initPipeline()
    }

    fun getSettings() = settings

    // ── Speech events ─────────────────────────────────────────────────────────

    private fun observeSpeechEvents() {
        DoeySpeechEvents.onPartialResult = { partial -> _uiState.update { it.copy(partialSpeech = partial) } }
    }

    private fun resolveLanguage(lang: String): String {
        return if (lang == "system") java.util.Locale.getDefault().toLanguageTag() else lang
    }

    override fun onCleared() {
        driveListenJob?.cancel()
        speechRecognizer?.destroy()
        super.onCleared()
    }
}
