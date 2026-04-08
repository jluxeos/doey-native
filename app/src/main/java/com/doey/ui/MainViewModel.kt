package com.doey.ui

import android.app.Application
import android.content.Intent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doey.agent.ConversationPipeline
import com.doey.agent.LocalIntentProcessor
import com.doey.agent.PipelineState
import com.doey.agent.SettingsStore
import com.doey.agent.SkillLoader
import com.doey.agent.ProfileStore
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
    private var pipelineStateJob: Job? = null

    init {
        initPipeline()
        observeSpeechEvents()
        observeNowPlaying()
    }

    private fun initPipeline() = viewModelScope.launch {
        val provider      = settings.getProvider()
        val model         = settings.getModel()
        val language      = settings.getLanguage()
        val drivingMode   = settings.getDrivingMode().let { it } // force collect
        val expertMode    = settings.getExpertMode().let { it }
        val enabledSkills = settings.getEnabledSkillsList()

        val skillLoader = SkillLoader(app)
        val tools       = buildTools(skillLoader, enabledSkills)

        val p = ConversationPipeline(
            ctx                = app,
            provider           = com.doey.llm.LLMProviderFactory.create(provider, settings.getApiKey(provider), model, settings.getCustomModelUrl()),
            tools              = tools,
            skillLoader        = skillLoader,
            drivingMode        = drivingMode,
            language           = resolveLanguage(language),
            soul               = settings.getSoul(),
            personalMemory     = settings.getPersonalMemory(),
            userName           = ProfileStore(app).getUserName(),
            maxIterations      = settings.getMaxIterations(),
            expertMode         = expertMode
        ).apply {
            setEnabledSkills(enabledSkills)
        }

        p.onTranscript = { role, text ->
            _uiState.update { s ->
                val newList = s.messages + ChatMessage(role = role, text = text)
                s.copy(messages = newList.takeLast(50))
            }
        }
        p.onError = { error -> _uiState.update { it.copy(errorMessage = error) } }

        pipelineStateJob?.cancel()
        pipelineStateJob = viewModelScope.launch {
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

    private fun buildTools(skillLoader: SkillLoader, enabledSkills: List<String>) = ToolRegistry().apply {
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

        removeDisabledSkillTools(skillLoader.getDisabledExclusiveTools(enabledSkills))
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
            // ── Procesador local de intenciones (ahorra tokens de IA) ─────────────
            val intent = LocalIntentProcessor.classify(text)
            when (intent) {
                is LocalIntentProcessor.IntentClass.Local -> {
                    // Ejecutar localmente sin gastar tokens
                    val result = executeLocalAction(intent.action)
                    if (result.isNotBlank()) {
                        _uiState.update { s ->
                            val newList = s.messages +
                                ChatMessage(role = "user",      text = text) +
                                ChatMessage(role = "assistant", text = result)
                            s.copy(messages = newList.takeLast(50))
                        }
                        if (voiceEnabled) DoeyTTSEngine.speakAndWait(result, resolveLanguage(settings.getLanguage()))
                        return@launch
                    }
                    // Si falla la ejecución local, delegar a IA
                }
                is LocalIntentProcessor.IntentClass.Complex -> {
                    // Comando complejo: optimizar el prompt antes de enviar a IA
                    val optimizedText = LocalIntentProcessor.buildOptimizedPrompt(intent.subtasks, text)
                    p.processUtterance(
                        userText = optimizedText,
                        onSpeak  = if (voiceEnabled) { t, lang -> DoeyTTSEngine.speakAndWait(t, lang) } else null
                    )
                    if (_uiState.value.isDrivingMode && voiceEnabled) { delay(500); startDrivingListen() }
                    return@launch
                }
                is LocalIntentProcessor.IntentClass.Delegate -> { /* Delegar a IA normalmente */ }
            }
            // ── Delegar a IA ─────────────────────────────────────────────────────
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

    /**
     * Ejecuta una acción local sin consumir tokens de IA.
     * Retorna un texto de respuesta o blank si no se pudo ejecutar.
     */
    private suspend fun executeLocalAction(action: LocalIntentProcessor.LocalAction): String {
        return try {
            when (action) {
                is LocalIntentProcessor.LocalAction.QueryInfo -> {
                    when (action.type) {
                        LocalIntentProcessor.InfoType.TIME -> {
                            val now = java.util.Calendar.getInstance()
                            "Son las ${now.get(java.util.Calendar.HOUR_OF_DAY)}:${String.format("%02d", now.get(java.util.Calendar.MINUTE))}"
                        }
                        LocalIntentProcessor.InfoType.DATE -> {
                            val now = java.util.Calendar.getInstance()
                            val months = listOf("enero","febrero","marzo","abril","mayo","junio",
                                "julio","agosto","septiembre","octubre","noviembre","diciembre")
                            "Hoy es ${now.get(java.util.Calendar.DAY_OF_MONTH)} de ${months[now.get(java.util.Calendar.MONTH)]} de ${now.get(java.util.Calendar.YEAR)}"
                        }
                        LocalIntentProcessor.InfoType.BATTERY -> {
                            val bm = app.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                            val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            "La batería está al $level%"
                        }
                        LocalIntentProcessor.InfoType.WEATHER -> "" // Delegar a IA para clima
                    }
                }
                is LocalIntentProcessor.LocalAction.SetVolume -> {
                    val am = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val newVol = (maxVol * action.level / 100.0).toInt().coerceIn(0, maxVol)
                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                    "Volumen ajustado al ${action.level}%"
                }
                is LocalIntentProcessor.LocalAction.ToggleFlashlight -> {
                    // Usar intent para la linterna
                    ""
                }
                else -> "" // Para otras acciones, delegar a IA
            }
        } catch (e: Exception) {
            ""
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

    fun togglePlayback() = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun skipToNext() = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun skipToPrevious() = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

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

    // ── FIX BUG-2: toggleOverlay mejorado ───────────────────────────────────────────────────────────
    fun toggleOverlay(enabled: Boolean) = viewModelScope.launch {
        settings.setOverlayEnabled(enabled)
        val ctx = app.applicationContext
        if (enabled) {
            if (android.provider.Settings.canDrawOverlays(ctx)) {
                val overlayInstance = com.doey.services.DoeyOverlayService.instance
                if (overlayInstance != null) {
                    // FIX BUG-2: servicio ya corre, solo mostrar la burbuja
                    overlayInstance.showBubble()
                } else {
                    // Iniciar el servicio con foreground
                    val intent = Intent(ctx, com.doey.services.DoeyOverlayService::class.java)
                    ctx.startForegroundService(intent)
                }
            }
        } else {
            // Detener completamente el servicio
            val intent = Intent(ctx, com.doey.services.DoeyOverlayService::class.java)
            ctx.stopService(intent)
        }
    }

    // ── Speech events ─────────────────────────────────────────────────────────────────
    private fun observeSpeechEvents() {
        DoeySpeechEvents.onPartialResult = { partial -> _uiState.update { it.copy(partialSpeech = partial) } }
    }

    private fun resolveLanguage(lang: String): String {
        return if (lang == "system") java.util.Locale.getDefault().toLanguageTag() else lang
    }

    override fun onCleared() {
        driveListenJob?.cancel()
        pipelineStateJob?.cancel()
        speechRecognizer?.destroy()
        super.onCleared()
    }
}
