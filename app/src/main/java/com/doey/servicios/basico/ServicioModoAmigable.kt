package com.doey.servicios.basico

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.doey.AplicacionDoey
import com.doey.agente.ConversationPipeline
import com.doey.agente.LocalIntentProcessor
import com.doey.agente.ProfileStore
import com.doey.agente.SettingsStore
import android.net.Uri
import com.doey.agente.SystemPromptBuilder
import com.doey.agente.SkillLoader
import com.doey.herramientas.comun.*
import com.doey.ui.MainActivity
import com.doey.ui.core.*
import com.doey.servicios.comun.DoeySpeechRecognizer
import com.doey.servicios.comun.DoeyTTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * FriendlyModeService — Doey (Refactored)
 *
 * Correcciones aplicadas:
 *  - Teclado: el overlay alterna FLAG_NOT_FOCUSABLE <> FLAG_FOCUSABLE dinámicamente
 *    para que el SoftKeyboard se abra al tocar el TextField.
 *  - STT: se inicializa correctamente en el Main thread; se gestiona el ciclo de vida
 *    del reconocedor con stop/destroy al pausar o cerrar.
 *  - TTS: se inicializa en onCreate y se detiene al pausar.
 *  - Botones: rediseñados sin Surface anidado en IconButton (causa deformación).
 *    Ahora son Box circulares con clip(CircleShape).
 *  - Pausa: implementa pausa real — detiene TTS, STT y cancela el job activo.
 *  - Translucidez: fondo sólido (~97% opacidad).
 *  - UI: sombra, divisor, colores más consistentes.
 */
class FriendlyModeService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "doey_friendly"
        const val NOTIF_ID   = 3001
        const val ACTION_SHOW        = "com.doey.friendly.SHOW"
        const val ACTION_HIDE        = "com.doey.friendly.HIDE"
        const val ACTION_SEND_TEXT   = "com.doey.friendly.SEND_TEXT"
        const val EXTRA_TEXT         = "text"
        const val EXTRA_CONTEXT_APP  = "context_app"

        @Volatile var instance: FriendlyModeService? = null
        @Volatile var isRunning: Boolean = false

        val FriendlyGreen       = Color(0xFF4CAF50)
        val FriendlyGreenLight  = Color(0xFF81C784)
        val FriendlyGreenDark   = Color(0xFF2E7D32)
        // alpha 97% para fondo prácticamente sólido
        val FriendlyBg          = Color(0xF71B3A1F)
        val FriendlyBubble      = Color(0xFF1E4620)
        val FriendlyText        = Color(0xFFE8F5E9)
        val FriendlySurface     = Color(0xFF243B27)
    }

    enum class FriendlyStatus {
        IDLE, LISTENING, PROCESSING, ACTING, WAITING_CONFIRM, SUCCESS, ERROR
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var barView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val statusState      = mutableStateOf(FriendlyStatus.IDLE)
    private val messageState     = mutableStateOf("")
    private val responseState    = mutableStateOf("")
    private val contextAppState  = mutableStateOf("")
    private val showConfirmState = mutableStateOf(false)
    private val confirmTextState = mutableStateOf("")
    private val isPausedState    = mutableStateOf(false)
    private val inputTextState   = mutableStateOf("")
    private val isExpandedState  = mutableStateOf(true)

    private var pipeline: ConversationPipeline? = null
    private var pendingConfirmAction: (() -> Unit)? = null
    private var speechRecognizer: DoeySpeechRecognizer? = null
    private var activeProcessingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        instance = this
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        DoeyTTSEngine.init(this)
        initPipeline()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        when (intent?.action) {
            ACTION_SHOW -> {
                val contextApp = intent.getStringExtra(EXTRA_CONTEXT_APP) ?: ""
                showBar(contextApp)
            }
            ACTION_HIDE -> hideBar()
            ACTION_SEND_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return START_STICKY
                processUserInput(text)
            }
            else -> showBar("")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        hideBar()
        serviceScope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        DoeyTTSEngine.stop()
        instance = null
        isRunning = false
        super.onDestroy()
    }

    private fun initPipeline() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val app = AplicacionDoey.instance
                val settings = SettingsStore(app)
                val provider = com.doey.llm.LLMProviderFactory.create(
                    settings.getProvider(),
                    settings.getApiKey(settings.getProvider()),
                    settings.getModel(),
                    settings.getCustomModelUrl()
                )
                val skillLoader = SkillLoader(app)
                val enabledSkills = settings.getEnabledSkillsList()
                val tools = ToolRegistry().apply {
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
                val profileStore = ProfileStore(app)
                val isLowPower   = profileStore.isLowPowerMode()
                val maxIter      = if (isLowPower) minOf(settings.getMaxIterations(), 5)
                                   else minOf(settings.getMaxIterations(), 8)
                val maxHistory   = if (isLowPower) 10 else settings.getMaxHistoryMessages()

                pipeline = ConversationPipeline(
                    ctx                       = app,
                    provider                  = provider,
                    tools                     = tools,
                    skillLoader               = skillLoader,
                    drivingMode               = false,
                    language                  = settings.getLanguage(),
                    soul                      = settings.getSoul(),
                    personalMemory            = settings.getPersonalMemory(),
                    userName                  = profileStore.getUserName(),
                    maxIterations             = maxIter,
                    maxHistoryMessages        = maxHistory,
                    expertMode                = settings.getExpertMode(),
                    tokenOptimizerEnabled     = settings.getTokenOptimizerEnabled(),
                    promptCacheEnabled        = settings.getSystemPromptCacheEnabled(),
                    historyCompressionEnabled = settings.getHistoryCompressionEnabled()
                ).apply { setEnabledSkills(enabledSkills) }
            } catch (e: Exception) {
                android.util.Log.e("FriendlyMode", "Error init pipeline: ${e.message}")
            }
        }
    }

    // ── Mostrar / Ocultar barra ────────────────────────────────────────────────

    private fun showBar(contextApp: String) {
        if (barView != null) {
            contextAppState.value = contextApp
            isExpandedState.value = true
            return
        }
        if (!android.provider.Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        contextAppState.value = contextApp

        // iniciar con FLAG_NOT_FOCUSABLE; se cambia dinámicamente al enfocar el campo
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0; y = 0
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FriendlyModeService)
            setViewTreeSavedStateRegistryOwner(this@FriendlyModeService)
            setContent {
                FriendlyBar(
                    status             = statusState.value,
                    message            = messageState.value,
                    response           = responseState.value,
                    contextApp         = contextAppState.value,
                    showConfirm        = showConfirmState.value,
                    confirmText        = confirmTextState.value,
                    isPaused           = isPausedState.value,
                    isExpanded         = isExpandedState.value,
                    inputText          = inputTextState.value,
                    onInputChange      = { inputTextState.value = it },
                    onSend             = { processUserInput(inputTextState.value); inputTextState.value = "" },
                    onMic              = { startVoiceInput() },
                    onPause            = { togglePause() },
                    onClose            = { hideBar() },
                    onOpenApp          = { openMainApp() },
                    onConfirm          = { pendingConfirmAction?.invoke(); showConfirmState.value = false },
                    onDeny             = { showConfirmState.value = false; responseState.value = "Entendido, cancelado." },
                    onExpand           = { isExpandedState.value = !isExpandedState.value },
                    onTextFieldFocused = { focused -> if (focused) enableKeyboard() else disableKeyboard() }
                )
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        windowManager?.addView(composeView, params)
        barView = composeView
    }

    /** Habilita el teclado quitando FLAG_NOT_FOCUSABLE del overlay */
    private fun enableKeyboard() {
        val lp   = layoutParams ?: return
        val view = barView ?: return
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        windowManager?.updateViewLayout(view, lp)
    }

    /** Restaura FLAG_NOT_FOCUSABLE cuando el TextField pierde foco */
    private fun disableKeyboard() {
        val lp   = layoutParams ?: return
        val view = barView ?: return
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager?.updateViewLayout(view, lp)
    }

    private fun hideBar() {
        barView?.let { windowManager?.removeView(it); barView = null }
        stopSelf()
    }

    private fun openMainApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_friendly", true)
        })
    }

    // ── Pausa / Reanudar ──────────────────────────────────────────────────────

    /**
     * Pausa real: detiene TTS, cancela STT en curso y el job de procesamiento.
     * Al reanudar restaura el estado IDLE.
     */
    private fun togglePause() {
        val nowPaused = !isPausedState.value
        isPausedState.value = nowPaused
        if (nowPaused) {
            DoeyTTSEngine.stop()
            speechRecognizer?.stop()
            activeProcessingJob?.cancel()
            activeProcessingJob = null
            if (statusState.value != FriendlyStatus.WAITING_CONFIRM) {
                statusState.value = FriendlyStatus.IDLE
            }
            responseState.value = "En pausa. Toca ▶ para continuar."
        } else {
            statusState.value   = FriendlyStatus.IDLE
            responseState.value = ""
        }
    }

    // ── Procesamiento ──────────────────────────────────────────────────────────

    fun processUserInput(text: String) {
        if (text.isBlank() || isPausedState.value) return
        messageState.value     = text
        statusState.value      = FriendlyStatus.PROCESSING
        responseState.value    = ""
        showConfirmState.value = false

        activeProcessingJob = serviceScope.launch {
            try {
                val intentClass = LocalIntentProcessor.classify(text)
                when (intentClass) {
                    is LocalIntentProcessor.IntentClass.Local -> {
                        statusState.value   = FriendlyStatus.ACTING
                        val result          = executeLocalAction(intentClass.action)
                        statusState.value   = FriendlyStatus.SUCCESS
                        responseState.value = result
                    }
                    is LocalIntentProcessor.IntentClass.Complex -> {
                        val screenCtx   = DoeyAccessibilityService.instance?.buildAccessibilityTree() ?: ""
                        val ctxPrefix   = if (screenCtx.isNotBlank() && contextAppState.value.isNotBlank())
                            "[Contexto de pantalla — ${contextAppState.value}]:\n${screenCtx.take(1500)}\n\n" else ""
                        val optimized   = LocalIntentProcessor.buildOptimizedPrompt(intentClass.subtasks, text)
                        val response    = pipeline?.processUtterance(ctxPrefix + optimized) ?: "Pipeline no disponible."
                        statusState.value   = FriendlyStatus.SUCCESS
                        responseState.value = response
                    }
                    is LocalIntentProcessor.IntentClass.Delegate -> {
                        val screenCtx   = DoeyAccessibilityService.instance?.buildAccessibilityTree() ?: ""
                        val ctxPrefix   = if (screenCtx.isNotBlank() && contextAppState.value.isNotBlank())
                            "[Contexto de pantalla — ${contextAppState.value}]:\n${screenCtx.take(2000)}\n\n" else ""
                        val response    = pipeline?.processUtterance(ctxPrefix + text) ?: "Pipeline no disponible."
                        statusState.value   = FriendlyStatus.SUCCESS
                        responseState.value = response
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                statusState.value   = FriendlyStatus.ERROR
                responseState.value = "Error: ${e.message}"
            }
        }
    }

    fun startVoiceInput() {
        if (isPausedState.value) return
        serviceScope.launch(Dispatchers.Main) {
            try {
                statusState.value = FriendlyStatus.LISTENING
                if (speechRecognizer == null) speechRecognizer = DoeySpeechRecognizer(this@FriendlyModeService)
                val settings = SettingsStore(AplicacionDoey.instance)
                val lang     = settings.getLanguage().let { l ->
                    if (l == "system") java.util.Locale.getDefault().toLanguageTag() else l
                }
                val text = speechRecognizer!!.listen(lang, "auto")
                statusState.value = FriendlyStatus.IDLE
                if (text.isNotBlank()) {
                    inputTextState.value = text
                    processUserInput(text)
                    inputTextState.value = ""
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                statusState.value   = FriendlyStatus.ERROR
                responseState.value = "Error de voz: ${e.message}"
            }
        }
    }

    fun requestConfirmation(text: String, onConfirm: () -> Unit) {
        confirmTextState.value  = text
        pendingConfirmAction    = onConfirm
        showConfirmState.value  = true
        statusState.value       = FriendlyStatus.WAITING_CONFIRM
    }

    private suspend fun executeLocalAction(action: LocalIntentProcessor.LocalAction): String {
        return when (action) {
            is LocalIntentProcessor.LocalAction.OpenApp -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pm    = packageManager
                val apps  = pm.queryIntentActivities(intent, 0)
                val match = apps.firstOrNull { info ->
                    pm.getApplicationLabel(info.activityInfo.applicationInfo)
                        .toString().contains(action.query, ignoreCase = true)
                }
                if (match != null) {
                    val li = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    if (li != null) { startActivity(li); "Abriendo ${action.query}" }
                    else "No pude abrir ${action.query}"
                } else "No encontré la app '${action.query}'"
            }
            is LocalIntentProcessor.LocalAction.Call -> {
                val number = LocalIntentProcessor.resolveContactNumber(this, action.contact) ?: action.contact
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                "Llamando a ${action.contact}"
            }
            is LocalIntentProcessor.LocalAction.Navigate -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(action.destination)}"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                "Navegando a ${action.destination}"
            }
            is LocalIntentProcessor.LocalAction.QueryInfo -> when (action.type) {
                LocalIntentProcessor.InfoType.TIME -> {
                    val cal = java.util.Calendar.getInstance()
                    "Son las ${cal.get(java.util.Calendar.HOUR_OF_DAY)}:${
                        cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')}"
                }
                LocalIntentProcessor.InfoType.DATE -> {
                    val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", java.util.Locale("es", "MX"))
                    "Hoy es ${sdf.format(java.util.Date())}"
                }
                LocalIntentProcessor.InfoType.BATTERY -> {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                    "Batería al ${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
                }
                else -> "No puedo obtener esa información localmente."
            }
            else -> pipeline?.processUtterance(action.toString()) ?: "No disponible"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doey Modo Friendly", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Barra de asistente Doey en modo Friendly"; setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Doey — Modo Friendly")
        .setContentText("Asistente activo en barra inferior")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setOngoing(true).setSilent(true)
        .setContentIntent(PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )).build()
}

// ── Composable: Barra Friendly ─────────────────────────────────────────────────

@Composable
private fun FriendlyBar(
    status: FriendlyModeService.FriendlyStatus,
    message: String,
    response: String,
    contextApp: String,
    showConfirm: Boolean,
    confirmText: String,
    isPaused: Boolean,
    isExpanded: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    onPause: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
    onExpand: () -> Unit,
    onTextFieldFocused: (Boolean) -> Unit
) {
    val accentGreen     = FriendlyModeService.FriendlyGreen
    val accentGreenDark = FriendlyModeService.FriendlyGreenDark
    val bgColor         = FriendlyModeService.FriendlyBg
    val bubbleColor     = FriendlyModeService.FriendlyBubble
    val textColor       = FriendlyModeService.FriendlyText
    val surfaceColor    = FriendlyModeService.FriendlySurface

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val isAnimating = status == FriendlyModeService.FriendlyStatus.PROCESSING ||
            status == FriendlyModeService.FriendlyStatus.ACTING

    val statusDotColor = when (status) {
        FriendlyModeService.FriendlyStatus.IDLE            -> Color(0xFF78909C)
        FriendlyModeService.FriendlyStatus.LISTENING       -> Color(0xFF42A5F5)
        FriendlyModeService.FriendlyStatus.PROCESSING,
        FriendlyModeService.FriendlyStatus.ACTING          -> accentGreen.copy(alpha = if (isAnimating) pulseAlpha else 1f)
        FriendlyModeService.FriendlyStatus.WAITING_CONFIRM -> Color(0xFFFFB74D)
        FriendlyModeService.FriendlyStatus.SUCCESS         -> accentGreen
        FriendlyModeService.FriendlyStatus.ERROR           -> Color(0xFFEF5350)
    }

    val statusLabel = when (status) {
        FriendlyModeService.FriendlyStatus.IDLE            -> if (isPaused) "En pausa" else ""
        FriendlyModeService.FriendlyStatus.LISTENING       -> "Escuchando..."
        FriendlyModeService.FriendlyStatus.PROCESSING      -> "Pensando..."
        FriendlyModeService.FriendlyStatus.ACTING          -> "Ejecutando..."
        FriendlyModeService.FriendlyStatus.WAITING_CONFIRM -> "Confirmar accion"
        FriendlyModeService.FriendlyStatus.SUCCESS         -> ""
        FriendlyModeService.FriendlyStatus.ERROR           -> "Error"
    }

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(color = bgColor, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {

        // Fila de control superior
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            Box(Modifier.size(8.dp).clip(CircleShape).background(statusDotColor))
            Spacer(Modifier.width(6.dp))

            Text("Modo Friendly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentGreen)

            if (statusLabel.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(statusLabel, fontSize = 11.sp, color = FriendlyModeService.FriendlyGreenLight)
            }

            Spacer(Modifier.weight(1f))

            if (contextApp.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1565C0).copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("En pantalla", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            // ControlButton usa Box+clip(CircleShape) —
            // IconButton{ Surface{ Icon } } que causaba deformaciones hexagonales.

            ControlButton(onClick = onExpand, backgroundColor = surfaceColor, size = 26) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = FriendlyModeService.FriendlyGreenLight,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(6.dp))

            // color naranja cuando en pausa, verde cuando activo
            ControlButton(
                onClick = onPause,
                backgroundColor = if (isPaused) Color(0xFFE65100) else Color(0xFFBF360C).copy(alpha = 0.85f),
                size = 30
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Reanudar" else "Pausar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(6.dp))

            // Box circular — no se deforma en mini-barra
            ControlButton(onClick = onClose, backgroundColor = Color(0xFFB71C1C), size = 30) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = accentGreen.copy(alpha = 0.2f), thickness = 0.5.dp)

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))

                // Burbuja de respuesta
                if (response.isNotBlank() || isAnimating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bubbleColor)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(accentGreen, accentGreenDark))),
                                contentAlignment = Alignment.Center
                            ) { Text("D", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                            Spacer(Modifier.width(10.dp))
                            if (isAnimating && response.isBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 7.dp)) {
                                    repeat(3) { i ->
                                        val dotAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.2f, targetValue = 1f,
                                            animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                                            label = "dot$i"
                                        )
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(accentGreen.copy(alpha = dotAlpha)))
                                        if (i < 2) Spacer(Modifier.width(5.dp))
                                    }
                                }
                            } else {
                                Text(
                                    response, fontSize = 13.sp, color = textColor,
                                    lineHeight = 19.sp, maxLines = 6, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Confirmacion
                AnimatedVisibility(visible = showConfirm) {
                    Column {
                        if (confirmText.isNotBlank()) {
                            Text(confirmText, fontSize = 12.sp, color = textColor, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Continuar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = onDeny,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF78909C)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("No, gracias", fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Fila de entrada
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                    // Microfono
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(
                                if (status == FriendlyModeService.FriendlyStatus.LISTENING) Color(0xFF1565C0)
                                else surfaceColor
                            )
                            .clickable(enabled = !isPaused) { onMic() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic, contentDescription = "Microfono",
                            tint = if (status == FriendlyModeService.FriendlyStatus.LISTENING) Color.White else accentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // onFocusChanged notifica al servicio para cambiar flags del overlay
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text(
                                if (isPaused) "En pausa..." else "Escribe un comando...",
                                fontSize = 12.sp, color = Color(0xFF546E7A)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState -> onTextFieldFocused(focusState.isFocused) },
                        shape = RoundedCornerShape(25.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = accentGreen,
                            unfocusedBorderColor    = accentGreen.copy(alpha = 0.35f),
                            focusedTextColor        = textColor,
                            unfocusedTextColor      = textColor,
                            cursorColor             = accentGreen,
                            focusedContainerColor   = surfaceColor,
                            unfocusedContainerColor = surfaceColor
                        ),
                        singleLine = true,
                        enabled = !isPaused,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    Spacer(Modifier.width(6.dp))

                    // Enviar
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isPaused) accentGreenDark else surfaceColor)
                            .clickable(enabled = inputText.isNotBlank() && !isPaused) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send, contentDescription = "Enviar",
                            tint = if (inputText.isNotBlank() && !isPaused) Color.White else Color(0xFF37474F),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Abrir app completa
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(surfaceColor).clickable { onOpenApp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = "Abrir Doey",
                            tint = Color(0xFF78909C), modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Botón de control circular reutilizable.
 *
 * FIX CRITICO: Reemplaza el patrón defectuoso [IconButton { Surface { Icon } }]
 * que generaba formas hexagonales y el boton de cerrar se aplastaba en barra horizontal.
 * Ahora es un [Box] con [clip(CircleShape)] directo — forma perfectamente circular.
 */
@Composable
private fun ControlButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    size: Int = 30,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
