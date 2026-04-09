package com.doey.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doey.agent.ConversationPipeline
import com.doey.agent.LocalIntentProcessor
import com.doey.agent.PipelineState
import com.doey.agent.SettingsStore
import com.doey.agent.SkillLoader
import com.doey.agent.ProfileStore
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

    private val settings    = SettingsStore(app)
    private val profileStore = ProfileStore(app)
    private var pipeline: ConversationPipeline? = null
    private var speechRecognizer: DoeySpeechRecognizer? = null
    private var driveListenJob: Job? = null
    private var pipelineStateJob: Job? = null

    init {
        viewModelScope.launch {
            initPipeline()
        }
        observeSpeechEvents()
        observeNowPlaying()
    }

    private suspend fun initPipeline() {
        val provider            = settings.getProvider()
        val model               = settings.getModel()
        val language            = settings.getLanguage()
        val drivingMode         = settings.getDrivingMode()
        val expertMode          = settings.getExpertMode()
        val enabledSkills       = settings.getEnabledSkillsList()
        val maxHistory          = settings.getMaxHistoryMessages()
        val tokenOptimizer      = settings.getTokenOptimizerEnabled()
        val promptCache         = settings.getSystemPromptCacheEnabled()
        val historyCompression  = settings.getHistoryCompressionEnabled()
        val isLowPower          = profileStore.isLowPowerMode()
        val effectiveMaxIter    = if (isLowPower) minOf(settings.getMaxIterations(), 6) else settings.getMaxIterations()
        val effectiveMaxHistory = if (isLowPower) minOf(maxHistory, 12) else maxHistory

        val skillLoader = SkillLoader(app)
        val tools       = buildTools(skillLoader, enabledSkills)

        val p = ConversationPipeline(
            ctx                      = app,
            provider                 = com.doey.llm.LLMProviderFactory.create(
                                           provider,
                                           settings.getApiKey(provider),
                                           model,
                                           settings.getCustomModelUrl()
                                       ),
            tools                    = tools,
            skillLoader              = skillLoader,
            drivingMode              = drivingMode,
            language                 = resolveLanguage(language),
            soul                     = settings.getSoul(),
            personalMemory           = settings.getPersonalMemory(),
            userName                 = ProfileStore(app).getUserName(),
            maxIterations            = effectiveMaxIter,
            maxHistoryMessages       = effectiveMaxHistory,
            expertMode               = expertMode,
            tokenOptimizerEnabled    = tokenOptimizer,
            promptCacheEnabled       = promptCache,
            historyCompressionEnabled = historyCompression
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

        if (settings.getWakeWordEnabled()) {
            startWakeWord()
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

    private fun observeNowPlaying() {
        viewModelScope.launch {
            NowPlayingRepository.nowPlaying.collect { info ->
                _uiState.update { it.copy(nowPlaying = info) }
            }
        }
    }

    fun sendMessage(text: String, voiceEnabled: Boolean = true) {
        if (text.isBlank()) return
        val p = pipeline ?: return
        viewModelScope.launch {
            val intent = LocalIntentProcessor.classify(text)
            when (intent) {
                is LocalIntentProcessor.IntentClass.Local -> {
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
                }
                is LocalIntentProcessor.IntentClass.Complex -> {
                    // Optimized prompt building
                    val optimizedText = "El usuario tiene varias peticiones: ${intent.subtasks.joinToString(". ")}. Petición original: $text"
                    p.processUtterance(
                        userText = optimizedText,
                        onSpeak  = if (voiceEnabled) { t, lang -> DoeyTTSEngine.speakAndWait(t, lang) } else null
                    )
                    if (_uiState.value.isDrivingMode && voiceEnabled) { delay(500); startDrivingListen() }
                    return@launch
                }
                is LocalIntentProcessor.IntentClass.Delegate -> {}
            }
            p.processUtterance(
                userText = text,
                onSpeak  = if (voiceEnabled) { t, lang ->
                    DoeyTTSEngine.speakAndWait(t, lang)
                } else null
            )
            if (_uiState.value.isDrivingMode && voiceEnabled) {
                delay(500)
                startDrivingListen()
            }
        }
    }

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
                            val months = listOf("enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre")
                            "Hoy es ${now.get(java.util.Calendar.DAY_OF_MONTH)} de ${months[now.get(java.util.Calendar.MONTH)]} de ${now.get(java.util.Calendar.YEAR)}"
                        }
                        LocalIntentProcessor.InfoType.BATTERY -> {
                            val bm = app.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                            "Tienes un ${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}% de batería"
                        }
                        LocalIntentProcessor.InfoType.WEATHER -> "Consultando el clima..."
                    }
                }
                is LocalIntentProcessor.LocalAction.ToggleFlashlight -> {
                    DeviceTool().toggleFlashlight(app, action.enable)
                    if (action.enable) "Linterna encendida" else "Linterna apagada"
                }
                is LocalIntentProcessor.LocalAction.SetVolume -> "Volumen ajustado al ${action.level}%"
                is LocalIntentProcessor.LocalAction.Navigate -> "Abriendo navegación a ${action.destination}"
                is LocalIntentProcessor.LocalAction.OpenApp -> "Abriendo ${action.query}"
                is LocalIntentProcessor.LocalAction.Call -> "Llamando a ${action.contact}"
                is LocalIntentProcessor.LocalAction.SendSms -> "Enviando SMS a ${action.contact}"
                is LocalIntentProcessor.LocalAction.SendWhatsApp -> "Enviando WhatsApp a ${action.contact}"
                is LocalIntentProcessor.LocalAction.PlayMusic -> "Reproduciendo ${action.query} en ${action.app}"
                is LocalIntentProcessor.LocalAction.ToggleWifi -> if (action.enable) "Activando Wi-Fi" else "Desactivando Wi-Fi"
                is LocalIntentProcessor.LocalAction.ToggleBluetooth -> if (action.enable) "Activando Bluetooth" else "Desactivando Bluetooth"
                is LocalIntentProcessor.LocalAction.SetAlarm -> "Alarma programada a las ${action.hour}:${action.minute}"
                is LocalIntentProcessor.LocalAction.SetTimer -> "Temporizador iniciado por ${action.seconds} segundos"
            }
        } catch (e: Exception) { "" }
    }

    fun startListening() {
        pipeline?.startListening()
    }

    fun stopListening() {
        pipeline?.stopListening()
    }

    fun stopSpeaking() {
        DoeyTTSEngine.stop()
    }

    fun startDrivingListen() {
        driveListenJob?.cancel()
        driveListenJob = viewModelScope.launch {
            delay(500)
            pipeline?.startListening()
        }
    }

    fun stopDrivingListen() {
        driveListenJob?.cancel()
        pipeline?.stopListening()
    }

    fun toggleDrivingMode() {
        val next = !_uiState.value.isDrivingMode
        pipeline?.setDrivingMode(next)
        _uiState.update { it.copy(isDrivingMode = next) }
        viewModelScope.launch { 
            settings.setDrivingMode(next)
            if (_uiState.value.isWakeWordActive) {
                startWakeWord()
            }
        }
        if (!next) stopDrivingListen()
    }

    private suspend fun startWakeWord() {
        val phrase   = settings.getWakePhrase()
        val language = resolveLanguage(settings.getLanguage())
        WakeWordService.onWakeWord = { _ ->
            viewModelScope.launch(Dispatchers.Main) { 
                if (_uiState.value.isDrivingMode) startDrivingListen() else startListening()
            }
        }
        val intent = Intent(app, WakeWordService::class.java).apply {
            putExtra(WakeWordService.EXTRA_WAKE_PHRASE, phrase)
            putExtra(WakeWordService.EXTRA_LANGUAGE,    language)
        }
        app.startForegroundService(intent)
        _uiState.update { it.copy(isWakeWordActive = true) }
    }

    private fun stopWakeWord() {
        app.stopService(Intent(app, WakeWordService::class.java))
        WakeWordService.onWakeWord = null
        _uiState.update { it.copy(isWakeWordActive = false) }
    }

    fun toggleWakeWord() = viewModelScope.launch {
        if (_uiState.value.isWakeWordActive) {
            settings.setWakeWordEnabled(false)
            stopWakeWord()
        } else {
            settings.setWakeWordEnabled(true)
            startWakeWord()
        }
    }

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
        pipeline = null
        initPipeline()
    }

    fun getSettings() = settings

    fun clearHistory() = viewModelScope.launch {
        pipeline?.clearHistory()
        _uiState.update { it.copy(messages = emptyList()) }
    }

    fun toggleOverlay(enabled: Boolean) = viewModelScope.launch {
        settings.setOverlayEnabled(enabled)
        val ctx = app.applicationContext
        if (enabled) {
            if (android.provider.Settings.canDrawOverlays(ctx)) {
                val overlayInstance = com.doey.services.DoeyOverlayService.instance
                if (overlayInstance != null) {
                    overlayInstance.showBubble()
                } else {
                    val intent = Intent(ctx, com.doey.services.DoeyOverlayService::class.java)
                    ctx.startForegroundService(intent)
                }
            }
        } else {
            val intent = Intent(ctx, com.doey.services.DoeyOverlayService::class.java)
            ctx.stopService(intent)
        }
    }

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
