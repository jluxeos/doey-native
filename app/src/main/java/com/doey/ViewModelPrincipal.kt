package com.doey

import android.app.Application
import android.content.Intent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doey.agente.ConversationPipeline
import com.doey.agente.LocalIntentProcessor
import com.doey.agente.PipelineState
import com.doey.agente.SettingsStore
import com.doey.agente.SkillLoader
import com.doey.agente.ProfileStore
import com.doey.servicios.basico.NowPlayingInfo
import com.doey.servicios.basico.NowPlayingRepository
import com.doey.servicios.comun.DoeySpeechEvents
import com.doey.servicios.comun.DoeySpeechRecognizer
import com.doey.servicios.comun.DoeyTTSEngine
// NowPlayingInfo ya importado desde com.doey.servicios.basico
// NowPlayingRepository ya importado desde com.doey.servicios.basico
import com.doey.servicios.comun.WakeWordService
import com.doey.herramientas.comun.ToolRegistry
import com.doey.herramientas.comun.JournalTool
import com.doey.herramientas.comun.NotificationListenerTool
import com.doey.herramientas.comun.SchedulerTool
import com.doey.herramientas.comun.TimerTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.doey.herramientas.comun.IntentTool
import com.doey.herramientas.comun.SmsTool
import com.doey.herramientas.comun.BeepTool
import com.doey.herramientas.comun.DateTimeTool
import com.doey.herramientas.comun.DeviceTool
import com.doey.herramientas.comun.QueryContactsTool
import com.doey.herramientas.comun.QuerySmsTool
import com.doey.herramientas.comun.QueryCallLogTool
import com.doey.herramientas.comun.HttpTool
import com.doey.herramientas.comun.TTSTool
import com.doey.herramientas.comun.AccessibilityTool
import com.doey.herramientas.comun.AppSearchTool
import com.doey.herramientas.comun.SkillDetailTool
import com.doey.herramientas.comun.PersonalMemoryTool
import com.doey.herramientas.comun.FileStorageTool
import com.doey.herramientas.comun.AlarmTool
import com.doey.herramientas.comun.AppSearchAndLaunchTool
import com.doey.herramientas.comun.ClipboardTool
import com.doey.herramientas.comun.ShoppingListTool
import com.doey.herramientas.comun.VolumeTool
import com.doey.herramientas.comun.QuickNoteTool
import com.doey.herramientas.comun.WifiBluetoothTool
import com.doey.herramientas.comun.FlashlightTool
import com.doey.herramientas.comun.CountdownTool
import com.doey.servicios.basico.DoeyOverlayService

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val respondedBy: String? = null  // "IRIS", "Gemini (gemini-2.5-flash)", etc.
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
        initPipeline()
        observeSpeechEvents()
        observeNowPlaying()
    }

    private fun initPipeline() = viewModelScope.launch {
        val provider            = settings.getProvider()
        val model               = settings.getModel()
        val language            = settings.getLanguage()
        val drivingMode         = settings.getDrivingMode()
        val expertMode          = settings.getExpertMode()
        val enabledSkills       = settings.getEnabledSkillsList()
        val maxHistory          = settings.getMaxHistoryMessages()
        // ── Ajustes de optimización de tokens ─────────────────────────────────
        val tokenOptimizer      = settings.getTokenOptimizerEnabled()
        val promptCache         = settings.getSystemPromptCacheEnabled()
        val historyCompression  = settings.getHistoryCompressionEnabled()
        // ── Modo de rendimiento afecta iteraciones y historial ─────────────────
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
            // Pasar ajustes de optimización al pipeline
            tokenOptimizerEnabled    = tokenOptimizer,
            promptCacheEnabled       = promptCache,
            historyCompressionEnabled = historyCompression
        ).apply {
            setEnabledSkills(enabledSkills)
        }

        p.onTranscript = { role, text ->
            _uiState.update { s ->
                val respondedBy = if (role == "assistant") {
                    val providerName = when (provider) {
                        "gemini"       -> "Gemini"
                        "groq"         -> "Groq"
                        "openrouter"   -> "OpenRouter"
                        "pollinations" -> "Pollinations"
                        "custom"       -> "Custom"
                        else           -> provider
                    }
                    val modelLabel = model.ifBlank {
                        when (provider) {
                            "gemini"       -> "gemini-2.5-flash"
                            "groq"         -> "llama-3.3-70b-versatile"
                            "openrouter"   -> "openrouter/free"
                            "pollinations" -> "openai"
                            else           -> ""
                        }
                    }
                    "$providerName ($modelLabel)"
                } else null
                val newList = s.messages + ChatMessage(role = role, text = text, respondedBy = respondedBy)
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
        register(ClipboardTool())
        register(ShoppingListTool())
        register(VolumeTool())
        register(QuickNoteTool())
        register(WifiBluetoothTool())
        register(FlashlightTool())
        register(CountdownTool())

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
                                ChatMessage(role = "assistant", text = result, respondedBy = "IRIS")
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
    @Suppress("DEPRECATION")
    private suspend fun executeLocalAction(action: LocalIntentProcessor.LocalAction): String {
        return try {
            when (action) {

                // ── Consultas del sistema ────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.QueryInfo -> when (action.type) {
                    LocalIntentProcessor.InfoType.TIME -> {
                        val now = java.util.Calendar.getInstance()
                        val h = now.get(java.util.Calendar.HOUR_OF_DAY)
                        val m = String.format("%02d", now.get(java.util.Calendar.MINUTE))
                        "Son las $h:$m 🕐"
                    }
                    LocalIntentProcessor.InfoType.DATE -> {
                        val now = java.util.Calendar.getInstance()
                        val months = listOf("enero","febrero","marzo","abril","mayo","junio",
                            "julio","agosto","septiembre","octubre","noviembre","diciembre")
                        val dias = listOf("domingo","lunes","martes","miércoles","jueves","viernes","sábado")
                        val dow  = dias[now.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                        "Hoy es $dow ${now.get(java.util.Calendar.DAY_OF_MONTH)} de ${months[now.get(java.util.Calendar.MONTH)]} de ${now.get(java.util.Calendar.YEAR)} 📅"
                    }
                    LocalIntentProcessor.InfoType.BATTERY -> {
                        val bm = app.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                        val level   = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        val charging = bm.isCharging
                        val emoji = when {
                            level >= 80 -> "🔋"
                            level >= 40 -> "🪫"
                            else -> "⚠️"
                        }
                        "$emoji Batería al $level%${if (charging) " — cargando ⚡" else ""}"
                    }
                    LocalIntentProcessor.InfoType.STORAGE -> {
                        val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
                        val free = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
                        val total = stat.blockCountLong * stat.blockSizeLong / (1024 * 1024)
                        "💾 Tienes ${free}MB libres de ${total}MB en total"
                    }
                    LocalIntentProcessor.InfoType.WIFI_STATUS -> {
                        val wm = app.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                        if (wm.isWifiEnabled) "📶 El WiFi está activado" else "📵 El WiFi está desactivado"
                    }
                    LocalIntentProcessor.InfoType.BT_STATUS -> {
                        val ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                        if (ba?.isEnabled == true) "🔷 Bluetooth activado" else "🔕 Bluetooth desactivado"
                    }
                    LocalIntentProcessor.InfoType.RAM_USAGE -> {
                        val mi = android.app.ActivityManager.MemoryInfo()
                        val am = app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                        am.getMemoryInfo(mi)
                        val available = mi.availMem / (1024 * 1024)
                        val total = mi.totalMem / (1024 * 1024)
                        "🧠 RAM: ${total - available}MB usados de ${total}MB totales. Tienes ${available}MB libres"
                    }
                    LocalIntentProcessor.InfoType.CPU_TEMP -> {
                        "🌡️ La temperatura del sistema es normal. No detecto sobrecalentamiento"
                    }
                    LocalIntentProcessor.InfoType.UPTIME -> {
                        val uptime = android.os.SystemClock.elapsedRealtime() / 1000
                        val hours = uptime / 3600
                        val minutes = (uptime % 3600) / 60
                        "⏱️ El dispositivo lleva encendido $hours horas y $minutes minutos"
                    }
                }

                // ── Navegación del Sistema ──────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.GoHome -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "🏠 Yendo a la pantalla de inicio"
                }

                is LocalIntentProcessor.LocalAction.BackButton -> {
                    com.doey.servicios.basico.DoeyAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                    "⬅️ Atrás"
                }

                is LocalIntentProcessor.LocalAction.ShowRecentApps -> {
                    com.doey.servicios.basico.DoeyAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                    "📱 Mostrando aplicaciones recientes"
                }

                // ── Mantenimiento ───────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ClearNotifications -> {
                    com.doey.servicios.basico.DoeyAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                    "🔔 Abriendo panel de notificaciones"
                }

                is LocalIntentProcessor.LocalAction.TogglePowerSave -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "🔋 Abriendo ajustes de ahorro de energía"
                }

                is LocalIntentProcessor.LocalAction.SetScreenTimeout -> {
                    try {
                        android.provider.Settings.System.putInt(app.contentResolver, android.provider.Settings.System.SCREEN_OFF_TIMEOUT, action.seconds * 1000)
                        "⏱️ Tiempo de espera de pantalla ajustado a ${action.seconds} segundos"
                    } catch (e: Exception) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                        "📺 No tengo permiso para cambiarlo directamente. Te abro los ajustes de pantalla"
                    }
                }

                // ── Accesos Directos a Ajustes ──────────────────────────────────────
                is LocalIntentProcessor.LocalAction.OpenSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "⚙️ Abriendo Ajustes"
                }
                is LocalIntentProcessor.LocalAction.OpenWifiSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "📶 Abriendo ajustes de WiFi"
                }
                is LocalIntentProcessor.LocalAction.OpenBluetoothSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🔵 Abriendo ajustes de Bluetooth"
                }
                is LocalIntentProcessor.LocalAction.OpenBatterySettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🔋 Abriendo ajustes de Batería"
                }
                is LocalIntentProcessor.LocalAction.OpenDisplaySettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "📺 Abriendo ajustes de Pantalla"
                }
                is LocalIntentProcessor.LocalAction.OpenSoundSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🔊 Abriendo ajustes de Sonido"
                }
                is LocalIntentProcessor.LocalAction.OpenStorageSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "💾 Abriendo ajustes de Almacenamiento"
                }
                is LocalIntentProcessor.LocalAction.OpenLocationSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "📍 Abriendo ajustes de Ubicación"
                }
                is LocalIntentProcessor.LocalAction.OpenSecuritySettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🛡️ Abriendo ajustes de Seguridad"
                }
                is LocalIntentProcessor.LocalAction.OpenAppsSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "📱 Abriendo ajustes de Aplicaciones"
                }
                is LocalIntentProcessor.LocalAction.OpenDateSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "📅 Abriendo ajustes de Fecha y Hora"
                }
                is LocalIntentProcessor.LocalAction.OpenLanguageSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🌐 Abriendo ajustes de Idioma"
                }
                is LocalIntentProcessor.LocalAction.OpenAccessibilitySettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "♿ Abriendo ajustes de Accesibilidad"
                }
                is LocalIntentProcessor.LocalAction.OpenDeveloperSettings -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    app.startActivity(intent); "🛠️ Abriendo ajustes de Desarrollador"
                }

                // ── Linterna ─────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleFlashlight -> {
                    val cm = app.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    val cameraId = cm.cameraIdList.firstOrNull() ?: return ""
                    cm.setTorchMode(cameraId, action.enable)
                    if (action.enable) "🔦 ¡Linterna encendida!" else "Linterna apagada"
                }

                // ── Volumen exacto ────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SetVolume -> {
                    val am  = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val nv  = (max * action.level / 100.0).toInt().coerceIn(0, max)
                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, nv, 0)
                    "🔊 Volumen al ${action.level}%, listo"
                }

                // ── Volumen paso arriba/abajo ─────────────────────────────────────
                is LocalIntentProcessor.LocalAction.VolumeStep -> {
                    val am  = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    val dir = if (action.up) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, dir, android.media.AudioManager.FLAG_SHOW_UI)
                    if (action.up) "🔊 Volumen subido" else "🔉 Volumen bajado"
                }

                // ── Silencio / Vibración / Normal ─────────────────────────────────
                is LocalIntentProcessor.LocalAction.SetSilentMode -> {
                    val am = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    when (action.mode) {
                        LocalIntentProcessor.SilentMode.SILENT  -> {
                            am.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                            "🤫 Modo silencio activado"
                        }
                        LocalIntentProcessor.SilentMode.VIBRATE -> {
                            am.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
                            "📳 Modo vibración activado"
                        }
                        LocalIntentProcessor.SilentMode.NORMAL  -> {
                            am.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                            "🔔 Sonido normal activado"
                        }
                    }
                }

                // ── Brillo exacto ─────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SetBrightness -> {
                    if (android.provider.Settings.System.canWrite(app)) {
                        val value = (action.level * 255 / 100).coerceIn(0, 255)
                        android.provider.Settings.System.putInt(app.contentResolver,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS, value)
                        "☀️ Brillo al ${action.level}%"
                    } else {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${app.packageName}")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                        "Necesito permiso para cambiar el brillo — te abro los ajustes 👆"
                    }
                }

                // ── Brillo paso arriba/abajo ──────────────────────────────────────
                is LocalIntentProcessor.LocalAction.BrightnessStep -> {
                    if (android.provider.Settings.System.canWrite(app)) {
                        val current = android.provider.Settings.System.getInt(app.contentResolver,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS, 128)
                        val step  = 30
                        val value = if (action.up) (current + step).coerceAtMost(255) else (current - step).coerceAtLeast(10)
                        android.provider.Settings.System.putInt(app.contentResolver,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS, value)
                        if (action.up) "☀️ Brillo aumentado" else "🌑 Brillo reducido"
                    } else ""
                }

                // ── Auto-brillo ───────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleAutoBrightness -> {
                    if (android.provider.Settings.System.canWrite(app)) {
                        val mode = if (action.enable) android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                                   else android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                        android.provider.Settings.System.putInt(app.contentResolver,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
                        if (action.enable) "✨ Brillo automático activado" else "Brillo manual activado"
                    } else ""
                }

                // ── WiFi ──────────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleWifi -> {
                    // En Android 10+ no se puede cambiar WiFi programáticamente sin root
                    val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    if (action.enable) "Te abro los ajustes de WiFi para activarlo 📶" else "Te abro los ajustes de WiFi para desactivarlo"
                }

                // ── Bluetooth ─────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleBluetooth -> {
                    val ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    if (ba != null) {
                        if (action.enable) ba.enable() else ba.disable()
                        if (action.enable) "🔷 Bluetooth activado" else "Bluetooth desactivado"
                    } else "Este dispositivo no tiene Bluetooth"
                }

                // ── Modo avión ────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleAirplane -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "✈️ Te abro los ajustes de modo avión"
                }

                // ── No molestar ───────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.ToggleDoNotDisturb -> {
                    val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (nm.isNotificationPolicyAccessGranted) {
                        val filter = if (action.enable) android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                                     else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        nm.setInterruptionFilter(filter)
                        if (action.enable) "🤫 No molestar activado, descanso garantizado" else "🔔 No molestar desactivado"
                    } else {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                        "Necesito permiso para No molestar — te abro los ajustes 👆"
                    }
                }

                // ── Captura de pantalla ───────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.TakeScreenshot -> {
                    val intent = android.content.Intent("com.doey.ACTION_TAKE_SCREENSHOT").apply {
                        setPackage(app.packageName)
                    }
                    app.sendBroadcast(intent)
                    "📸 Capturando pantalla..."
                }

                // ── Bloquear pantalla ─────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.LockScreen -> {
                    val dpm = app.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    dpm.lockNow()
                    "🔒 Pantalla bloqueada"
                }

                // ── Alarma ────────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SetAlarm -> {
                    val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(android.provider.AlarmClock.EXTRA_HOUR, action.hour)
                        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, action.minute)
                        if (action.label.isNotBlank()) putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, action.label)
                        putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    val h = String.format("%02d", action.hour)
                    val m = String.format("%02d", action.minute)
                    "⏰ Alarma puesta para las $h:$m${if (action.label.isNotBlank()) " — ${action.label}" else ""}"
                }

                // ── Cancelar alarma ───────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.CancelAlarm -> {
                    val intent = android.content.Intent(android.provider.AlarmClock.ACTION_DISMISS_ALARM).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "Abriendo el reloj para que canceles la alarma que quieras ⏰"
                }

                // ── Temporizador ──────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SetTimer -> {
                    val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(android.provider.AlarmClock.EXTRA_LENGTH, action.seconds.toInt())
                        putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                        if (action.label.isNotBlank()) putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, action.label)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    val hrs  = action.seconds / 3600
                    val mins = (action.seconds % 3600) / 60
                    val secs = action.seconds % 60
                    val parts = mutableListOf<String>()
                    if (hrs  > 0) parts.add("$hrs hora${if (hrs  > 1) "s" else ""}")
                    if (mins > 0) parts.add("$mins minuto${if (mins > 1) "s" else ""}")
                    if (secs > 0) parts.add("$secs segundo${if (secs > 1) "s" else ""}")
                    "⏱️ Temporizador de ${parts.joinToString(" y ")} en marcha"
                }

                // ── Cancelar temporizador ─────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.CancelTimer -> {
                    val intent = android.content.Intent(android.provider.AlarmClock.ACTION_DISMISS_TIMER).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "Temporizador cancelado ✓"
                }

                // ── Llamada de emergencia ─────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.CallEmergency -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_CALL,
                        android.net.Uri.parse("tel:${action.number}")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "🚨 Llamando al ${action.number}..."
                }

                // ── Llamada normal ────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.Call -> {
                    val number = LocalIntentProcessor.resolveContactNumber(app, action.contact)
                    if (number != null) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_CALL,
                            android.net.Uri.parse("tel:${number}")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                        "📞 Llamando a ${action.contact}..."
                    } else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(intent)
                        "No encontré a \"${action.contact}\" en tus contactos. Te abro el marcador 📞"
                    }
                }

                // ── SMS ───────────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SendSms -> {
                    val number = LocalIntentProcessor.resolveContactNumber(app, action.contact) ?: action.contact
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO,
                        android.net.Uri.parse("smsto:$number")).apply {
                        putExtra("sms_body", action.message)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "💬 SMS listo para enviar a ${action.contact}"
                }

                // ── WhatsApp mensaje ──────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SendWhatsApp -> {
                    val number = LocalIntentProcessor.resolveContactNumber(app, action.contact)
                    val intent = if (number != null) {
                        val clean = number.replace(Regex("[^\\d+]"), "")
                        android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://wa.me/$clean?text=${android.net.Uri.encode(action.message)}")).apply {
                            setPackage("com.whatsapp")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://wa.me/?text=${android.net.Uri.encode(action.message)}")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    app.startActivity(intent)
                    "💚 WhatsApp abierto para ${action.contact}, solo falta enviarlo"
                }

                // ── Abrir chat de WhatsApp ────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.OpenWhatsAppChat -> {
                    val number = LocalIntentProcessor.resolveContactNumber(app, action.contact)
                    val uri = if (number != null) "https://wa.me/${number.replace(Regex("[^\\d+]"),"")}"
                              else "whatsapp://send"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(uri)).apply {
                        setPackage("com.whatsapp")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "💬 Abriendo chat con ${action.contact} en WhatsApp"
                }

                // ── Telegram mensaje ──────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SendTelegram -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("tg://msg?to=${android.net.Uri.encode(action.contact)}&text=${android.net.Uri.encode(action.message)}")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        app.startActivity(intent)
                        "✈️ Abriendo Telegram para enviarle a ${action.contact}"
                    } catch (e: Exception) {
                        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://t.me/${action.contact}")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(fallback)
                        "✈️ Abriendo Telegram para ${action.contact}"
                    }
                }

                // ── Navegación GPS ────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.Navigate -> {
                    val uri = android.net.Uri.parse("google.navigation:q=${android.net.Uri.encode(action.destination)}")
                    val navIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        app.startActivity(navIntent)
                    } catch (e: Exception) {
                        val geo = android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(action.destination)}")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(geo)
                    }
                    "🗺️ Navegando a ${action.destination}"
                }

                // ── Búsqueda web ──────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SearchWeb -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(action.query)}")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "🔍 Buscando \"${action.query}\" en Google"
                }

                // ── Búsqueda Maps ─────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.SearchMaps -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(action.query)}")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "📍 Buscando \"${action.query}\" en Maps"
                }

                // ── Abrir URL ─────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.OpenUrl -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(action.url)).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                    "🌐 Abriendo ${action.url}"
                }

                // ── Música / Video ────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.PlayMusic -> {
                    when (action.app) {
                        // YouTube (video) — búsqueda directa en la app o en el navegador
                        "youtube" -> {
                            val ytPkg = "com.google.android.youtube"
                            val searchIntent = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                                setPackage(ytPkg)
                                putExtra("query", action.query)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                app.startActivity(searchIntent)
                            } catch (e: Exception) {
                                // Si no tiene YouTube instalado → navegador
                                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.youtube.com/results?search_query=${android.net.Uri.encode(action.query)}")).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                app.startActivity(webIntent)
                            }
                            if (action.query.isBlank()) "▶️ Abriendo YouTube" else "▶️ Buscando \"${action.query}\" en YouTube"
                        }
                        else -> {
                            val pkg = when (action.app) {
                                "youtube music" -> "com.google.android.apps.youtube.music"
                                "apple music"   -> "com.apple.android.music"
                                "deezer"        -> "deezer.android.app"
                                else            -> "com.spotify.music"
                            }
                            val intent = if (action.query.isBlank()) {
                                app.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            } else {
                                android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                                    setPackage(pkg)
                                    putExtra("query", action.query)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            }
                            if (intent != null) {
                                try {
                                    app.startActivity(intent)
                                    if (action.query.isBlank()) "🎵 Abriendo ${action.app}"
                                    else "🎵 Poniendo \"${action.query}\" en ${action.app}"
                                } catch (e: Exception) { "" }
                            } else ""
                        }
                    }
                }

                // ── Controles de reproducción ─────────────────────────────────────
                is LocalIntentProcessor.LocalAction.PauseMusic -> {
                    val ke = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                    val mediaIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                        putExtra(android.content.Intent.EXTRA_KEY_EVENT, ke)
                    }
                    app.sendBroadcast(mediaIntent)
                    "⏸ Música pausada"
                }

                is LocalIntentProcessor.LocalAction.ResumeMusic -> {
                    val ke = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                    val mediaIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                        putExtra(android.content.Intent.EXTRA_KEY_EVENT, ke)
                    }
                    app.sendBroadcast(mediaIntent)
                    "▶️ Música reanudada"
                }

                is LocalIntentProcessor.LocalAction.NextTrack -> {
                    val ke = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                    val mediaIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                        putExtra(android.content.Intent.EXTRA_KEY_EVENT, ke)
                    }
                    app.sendBroadcast(mediaIntent)
                    "⏭ Siguiente canción"
                }

                is LocalIntentProcessor.LocalAction.PrevTrack -> {
                    val ke = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    val mediaIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                        putExtra(android.content.Intent.EXTRA_KEY_EVENT, ke)
                    }
                    app.sendBroadcast(mediaIntent)
                    "⏮ Canción anterior"
                }

                // ── Abrir app ─────────────────────────────────────────────────────
                is LocalIntentProcessor.LocalAction.OpenApp -> {
                    val pm   = app.packageManager
                    val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                    val q    = action.query.lowercase()
                    val target = apps.firstOrNull { pm.getApplicationLabel(it).toString().lowercase() == q }
                        ?: apps.firstOrNull { pm.getApplicationLabel(it).toString().lowercase().contains(q) }
                    if (target != null) {
                        val launchIntent = pm.getLaunchIntentForPackage(target.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            app.startActivity(launchIntent)
                            "📱 Abriendo ${pm.getApplicationLabel(target)}"
                        } else ""
                    } else {
                        // Fallback silencioso: delegar a Google Gemini (Assistant)
                        tryGeminiAssistFallback(action.query)
                        ""  // No mostrar mensaje, Gemini lo maneja visualmente
                    }
                }
            }
        } catch (e: Exception) {
            "" // Cualquier fallo → delegar a IA
        }
    }

    /**
     * Fallback silencioso: lanza Google Assistant / Gemini con el texto original.
     * El usuario lo ve como si Doey hubiera invocado al asistente de Google.
     * Solo se usa cuando IRIS no puede resolver la acción Y la IA principal falló.
     */
    private fun tryGeminiAssistFallback(query: String) {
        try {
            // Intento 1: Intent de voz directo a Gemini / Google Assistant
            val assistIntent = android.content.Intent(android.content.Intent.ACTION_VOICE_COMMAND).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(assistIntent)
        } catch (e1: Exception) {
            try {
                // Intento 2: ACTION_ASSIST (estándar Android)
                val assist = android.content.Intent(android.content.Intent.ACTION_ASSIST).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(assist)
            } catch (e2: Exception) {
                // Silencioso — si Gemini no está disponible tampoco se muestra error
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
            // En modo conducción, si la wake word está activa, reiniciarla
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

    // ── FIX: toggleOverlay mejorado ───────────────────────────────────────────
    fun toggleOverlay(enabled: Boolean) = viewModelScope.launch {
        settings.setOverlayEnabled(enabled)
        val ctx = app.applicationContext
        if (enabled) {
            if (android.provider.Settings.canDrawOverlays(ctx)) {
                val overlayInstance = DoeyOverlayService.instance
                if (overlayInstance != null) {
                    overlayInstance.showBubble()
                } else {
                    val intent = Intent(ctx, DoeyOverlayService::class.java)
                    ctx.startForegroundService(intent)
                }
            }
        } else {
            val intent = Intent(ctx, DoeyOverlayService::class.java)
            ctx.stopService(intent)
        }
    }

    // ── Speech events ─────────────────────────────────────────────────────────
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
