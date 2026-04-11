package com.doey.servicios.basico

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * FriendlyModeService — Doey 23.4.9 Ultra (Tau Version)
 *
 * Servicio de overlay que implementa el "Modo Friendly": una barra inferior
 * flotante que aparece cuando Doey es invocado como asistente del sistema.
 * Lee el contexto de la pantalla activa via AccessibilityService y permite
 * al usuario interactuar con Doey sin abandonar la app actual.
 *
 * Diseño basado en los dibujos del usuario:
 *  - Barra verde/lima en la parte inferior
 *  - Indicador de contexto "Usando contexto en pantalla"
 *  - Botones Pausa y Cerrar
 *  - Campo de texto / burbuja de respuesta
 *  - Botones de confirmación [Continuar] / [No, gracias]
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

        // Colores del modo Friendly (verde lima como en los dibujos)
        val FriendlyGreen       = Color(0xFF4CAF50)
        val FriendlyGreenLight  = Color(0xFF81C784)
        val FriendlyGreenDark   = Color(0xFF388E3C)
        val FriendlyBg          = Color(0xF02E7D32)   // Verde oscuro semitransparente
        val FriendlyBubble      = Color(0xFFE8F5E9)   // Verde muy claro para burbujas
        val FriendlyText        = Color(0xFF1B5E20)   // Verde oscuro para texto
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Estados reactivos
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
    private var speechRecognizer: com.doey.servicios.comun.DoeySpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        instance = this
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
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
        instance = null
        isRunning = false
        super.onDestroy()
    }

    // ── Inicialización del pipeline ────────────────────────────────────────────

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

                val profileStore = com.doey.agente.ProfileStore(app)
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
                ).apply {
                    setEnabledSkills(enabledSkills)
                }
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
            x = 0
            y = 0
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FriendlyModeService)
            setViewTreeSavedStateRegistryOwner(this@FriendlyModeService)
            setContent {
                FriendlyBar(
                    status       = statusState.value,
                    message      = messageState.value,
                    response     = responseState.value,
                    contextApp   = contextAppState.value,
                    showConfirm  = showConfirmState.value,
                    confirmText  = confirmTextState.value,
                    isPaused     = isPausedState.value,
                    isExpanded   = isExpandedState.value,
                    inputText    = inputTextState.value,
                    onInputChange = { inputTextState.value = it },
                    onSend       = { processUserInput(inputTextState.value); inputTextState.value = "" },
                    onMic        = { startVoiceInput() },
                    onPause      = { isPausedState.value = !isPausedState.value },
                    onClose      = { hideBar() },
                    onOpenApp    = { openMainApp() },
                    onConfirm    = { pendingConfirmAction?.invoke(); showConfirmState.value = false },
                    onDeny       = { showConfirmState.value = false; responseState.value = "Entendido, cancelado." },
                    onExpand     = { isExpandedState.value = !isExpandedState.value }
                )
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        windowManager?.addView(composeView, params)
        barView = composeView
    }

    private fun hideBar() {
        barView?.let {
            windowManager?.removeView(it)
            barView = null
        }
        stopSelf()
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_friendly", true)
        }
        startActivity(intent)
    }

    // ── Procesamiento de entrada del usuario ───────────────────────────────────

    fun processUserInput(text: String) {
        if (text.isBlank() || isPausedState.value) return

        messageState.value = text
        statusState.value  = FriendlyStatus.PROCESSING
        responseState.value = ""
        showConfirmState.value = false

        serviceScope.launch {
            try {
                // 1. Intentar resolver localmente primero
                val intentClass = LocalIntentProcessor.classify(text)

                when (intentClass) {
                    is LocalIntentProcessor.IntentClass.Local -> {
                        // Comando simple: ejecutar sin IA
                        statusState.value = FriendlyStatus.ACTING
                        val result = executeLocalAction(intentClass.action)
                        statusState.value = FriendlyStatus.SUCCESS
                        responseState.value = result
                    }

                    is LocalIntentProcessor.IntentClass.Complex -> {
                        // Comando complejo: usar IA con contexto de pantalla
                        val screenContext = DoeyAccessibilityService.instance?.buildAccessibilityTree() ?: ""
                        val contextPrefix = if (screenContext.isNotBlank() && contextAppState.value.isNotBlank()) {
                            "[Contexto de pantalla — ${contextAppState.value}]:\n${screenContext.take(1500)}\n\n"
                        } else ""

                        val optimizedPrompt = LocalIntentProcessor.buildOptimizedPrompt(
                            intentClass.subtasks,
                            text
                        )
                        val fullPrompt = contextPrefix + optimizedPrompt

                        val response = pipeline?.processUtterance(fullPrompt) ?: "Pipeline no disponible."
                        statusState.value  = FriendlyStatus.SUCCESS
                        responseState.value = response
                    }

                    is LocalIntentProcessor.IntentClass.Delegate -> {
                        // Delegar a IA con contexto de pantalla si está disponible
                        val screenContext = DoeyAccessibilityService.instance?.buildAccessibilityTree() ?: ""
                        val contextPrefix = if (screenContext.isNotBlank() && contextAppState.value.isNotBlank()) {
                            "[Contexto de pantalla — ${contextAppState.value}]:\n${screenContext.take(2000)}\n\n"
                        } else ""

                        val response = pipeline?.processUtterance(contextPrefix + text) ?: "Pipeline no disponible."
                        statusState.value  = FriendlyStatus.SUCCESS
                        responseState.value = response
                    }
                }
            } catch (e: Exception) {
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
                statusState.value   = FriendlyStatus.ERROR
                responseState.value = "Error de voz: ${e.message}"
            }
        }
    }

    fun requestConfirmation(text: String, onConfirm: () -> Unit) {
        confirmTextState.value    = text
        pendingConfirmAction      = onConfirm
        showConfirmState.value    = true
        statusState.value         = FriendlyStatus.WAITING_CONFIRM
    }

    private suspend fun executeLocalAction(action: LocalIntentProcessor.LocalAction): String {
        return when (action) {
            is LocalIntentProcessor.LocalAction.OpenApp -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                // Buscar la app por nombre
                val pm = packageManager
                val apps = pm.queryIntentActivities(intent, 0)
                val match = apps.firstOrNull { info ->
                    pm.getApplicationLabel(info.activityInfo.applicationInfo)
                        .toString().contains(action.query, ignoreCase = true)
                }
                if (match != null) {
                    val launchIntent = pm.getLaunchIntentForPackage(
                        match.activityInfo.packageName
                    )?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                        "Abriendo ${action.query}"
                    } else "No pude abrir ${action.query}"
                } else "No encontré la app '${action.query}'"
            }

            is LocalIntentProcessor.LocalAction.Call -> {
                val number = LocalIntentProcessor.resolveContactNumber(this, action.contact)
                    ?: action.contact
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(callIntent)
                "Llamando a ${action.contact}"
            }

            is LocalIntentProcessor.LocalAction.Navigate -> {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(action.destination)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(mapIntent)
                "Navegando a ${action.destination}"
            }

            is LocalIntentProcessor.LocalAction.QueryInfo -> {
                when (action.type) {
                    LocalIntentProcessor.InfoType.TIME -> {
                        val cal = java.util.Calendar.getInstance()
                        "Son las ${cal.get(java.util.Calendar.HOUR_OF_DAY)}:${
                            cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
                        }"
                    }
                    LocalIntentProcessor.InfoType.DATE -> {
                        val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", java.util.Locale("es", "MX"))
                        "Hoy es ${sdf.format(java.util.Date())}"
                    }
                    LocalIntentProcessor.InfoType.BATTERY -> {
                        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        "Batería al $level%"
                    }
                    else -> "No puedo obtener esa información localmente."
                }
            }

            else -> {
                // Para acciones que requieren herramientas del pipeline, delegar
                val response = pipeline?.processUtterance(action.toString()) ?: "No disponible"
                response
            }
        }
    }

    // ── Notificación ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Doey Modo Friendly",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Barra de asistente Doey en modo Friendly"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Doey — Modo Friendly")
        .setContentText("Asistente activo en barra inferior")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
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
    onExpand: () -> Unit
) {
    val accentGreen     = FriendlyModeService.FriendlyGreen
    val accentGreenDark = FriendlyModeService.FriendlyGreenDark
    val bgColor         = FriendlyModeService.FriendlyBg
    val bubbleColor     = FriendlyModeService.FriendlyBubble
    val textColor       = FriendlyModeService.FriendlyText

    // Animación de pulso para el indicador de estado
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val isAnimating = status == FriendlyModeService.FriendlyStatus.PROCESSING ||
            status == FriendlyModeService.FriendlyStatus.ACTING


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, bgColor.copy(alpha = 0.95f))),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── Barra de control superior ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de estado
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                FriendlyModeService.FriendlyStatus.IDLE             -> Color(0xFF9E9E9E)
                                FriendlyModeService.FriendlyStatus.LISTENING        -> Color(0xFF2196F3)
                                FriendlyModeService.FriendlyStatus.PROCESSING,
                                FriendlyModeService.FriendlyStatus.ACTING           -> accentGreen.copy(alpha = if (isAnimating) pulseAlpha else 1f)
                                FriendlyModeService.FriendlyStatus.WAITING_CONFIRM  -> Color(0xFFFF9800)
                                FriendlyModeService.FriendlyStatus.SUCCESS          -> accentGreen
                                FriendlyModeService.FriendlyStatus.ERROR            -> Color(0xFFFF3B30)
                            }
                        )
                )
                Spacer(Modifier.width(6.dp))

                // Etiqueta "Modo Friendly"
                Text(
                    "Modo Friendly",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentGreen
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    when (status) {
                        FriendlyModeService.FriendlyStatus.IDLE             -> ""
                        FriendlyModeService.FriendlyStatus.LISTENING        -> "Escuchando..."
                        FriendlyModeService.FriendlyStatus.PROCESSING       -> "Procesando..."
                        FriendlyModeService.FriendlyStatus.ACTING           -> "Actuando..."
                        FriendlyModeService.FriendlyStatus.WAITING_CONFIRM  -> "Esperando confirmación..."
                        FriendlyModeService.FriendlyStatus.SUCCESS          -> ""
                        FriendlyModeService.FriendlyStatus.ERROR            -> "Error"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFB2DFDB)
                )

                Spacer(Modifier.weight(1f))

                // Indicador de contexto
                if (contextApp.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1565C0).copy(alpha = 0.8f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Usando contexto en pantalla",
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }

                // Botón expandir/colapsar
                IconButton(onClick = onExpand, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        null,
                        tint = Color(0xFFB2DFDB),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Botón pausa
                IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPaused) Color(0xFFFF9800) else Color(0xFFFF9800).copy(alpha = 0.7f)
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))

                // Botón cerrar
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF3B30)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // ── Contenido expandible ───────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))

                    // Burbuja de respuesta de Doey
                    if (response.isNotBlank() || isAnimating) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            color = bubbleColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Avatar Doey
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accentGreenDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("D", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                if (isAnimating && response.isBlank()) {
                                    // Indicador de carga animado
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(3) { i ->
                                            val dotAlpha by infiniteTransition.animateFloat(
                                                initialValue = 0.2f, targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(600, delayMillis = i * 200),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot$i"
                                            )
                                            Box(
                                                Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(accentGreenDark.copy(alpha = dotAlpha))
                                            )
                                            if (i < 2) Spacer(Modifier.width(4.dp))
                                        }
                                    }
                                } else {
                                    Text(
                                        response,
                                        fontSize = 13.sp,
                                        color = textColor,
                                        lineHeight = 18.sp,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Botones de confirmación
                    AnimatedVisibility(visible = showConfirm) {
                        Column {
                            if (confirmText.isNotBlank()) {
                                Text(
                                    confirmText,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onConfirm,
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Continuar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onDeny,
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("No, gracias", fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Campo de entrada de texto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón de micrófono
                        IconButton(
                            onClick  = { onMic() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                null,
                                tint = accentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Campo de texto
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            placeholder = { Text("Escribe un comando...", fontSize = 12.sp, color = Color(0xFFB2DFDB)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = accentGreen,
                                unfocusedBorderColor = accentGreen.copy(alpha = 0.5f),
                                focusedTextColor     = Color.White,
                                unfocusedTextColor   = Color.White,
                                cursorColor          = accentGreen
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )

                        Spacer(Modifier.width(4.dp))

                        // Botón enviar / cohete
                        IconButton(
                            onClick = onSend,
                            modifier = Modifier.size(40.dp),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                null,
                                tint = if (inputText.isNotBlank()) accentGreen else Color(0xFF546E7A),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Botón abrir app completa
                        IconButton(
                            onClick = onOpenApp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                null,
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
}
