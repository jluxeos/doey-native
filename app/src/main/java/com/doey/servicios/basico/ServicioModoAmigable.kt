package com.doey.servicios.basico

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
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
        val FriendlyGreenDark   = Color(0xFF388E3C)
        val FriendlyBg          = Color(0xF02E7D32)
        val FriendlyBubble      = Color(0xFFE8F5E9)
        val FriendlyText        = Color(0xFF1B5E20)
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
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showBar(contextApp: String) {
        if (barView != null) {
            contextAppState.value = contextApp
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        contextAppState.value = contextApp

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FriendlyModeService)
            setViewTreeSavedStateRegistryOwner(this@FriendlyModeService)
            setContent {
                FriendlyBar(
                    status      = statusState.value,
                    message     = messageState.value,
                    response    = responseState.value,
                    contextApp  = contextAppState.value,
                    showConfirm = showConfirmState.value,
                    confirmText = confirmTextState.value,
                    isPaused    = isPausedState.value,
                    isExpanded  = isExpandedState.value,
                    inputText   = inputTextState.value,
                    onInputChange = { inputTextState.value = it },
                    onSend      = { processUserInput(inputTextState.value); inputTextState.value = "" },
                    onMic       = { startVoiceInput() },
                    onPause     = { isPausedState.value = !isPausedState.value },
                    onClose     = { hideBar(); stopSelf() },
                    onOpenApp   = { openMainApp() },
                    onConfirm   = { pendingConfirmAction?.invoke(); showConfirmState.value = false },
                    onDeny      = { showConfirmState.value = false },
                    onExpand    = { isExpandedState.value = !isExpandedState.value }
                )
            }
        }

        barView = composeView
        windowManager?.addView(barView, params)
    }

    private fun hideBar() {
        barView?.let {
            windowManager?.removeView(it)
            barView = null
        }
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        hideBar()
        stopSelf()
    }

    private fun processUserInput(text: String) {
        if (text.isBlank() || isPausedState.value) return
        
        serviceScope.launch {
            statusState.value = FriendlyStatus.PROCESSING
            responseState.value = ""
            
            val intent = LocalIntentProcessor.classify(text)
            if (intent is LocalIntentProcessor.IntentClass.Local) {
                statusState.value = FriendlyStatus.ACTING
                val result = LocalIntentProcessor.execute(AplicacionDoey.instance, intent.action)
                responseState.value = result
                statusState.value = FriendlyStatus.SUCCESS
                return@launch
            }

            val p = pipeline ?: return@launch
            p.onTranscript = { role, content ->
                if (role == "assistant") responseState.value = content
            }
            
            try {
                p.processMessage(text)
                statusState.value = FriendlyStatus.SUCCESS
            } catch (e: Exception) {
                statusState.value = FriendlyStatus.ERROR
                responseState.value = "Error: ${e.message}"
            }
        }
    }

    private fun startVoiceInput() {
        if (speechRecognizer == null) {
            speechRecognizer = DoeySpeechRecognizer(this)
        }
        
        statusState.value = FriendlyStatus.LISTENING
        speechRecognizer?.startListening(object : DoeySpeechRecognizer.SpeechCallback {
            override fun onResult(text: String) {
                inputTextState.value = text
                processUserInput(text)
            }
            override fun onError(error: String) {
                statusState.value = FriendlyStatus.ERROR
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doey Friendly Mode",
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            "Contexto: $contextApp",
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }

            IconButton(onClick = onExpand, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    null,
                    tint = Color(0xFFB2DFDB),
                    modifier = Modifier.size(16.dp)
                )
            }

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

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit  = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))

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
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (showConfirm) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                        ) {
                            Text("Continuar", color = Color.White)
                        }
                        Button(
                            onClick = onDeny,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("No, gracias", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Pregunta algo...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (inputText.isNotBlank()) onSend() else onMic() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) accentGreen else Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            if (inputText.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenApp,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
