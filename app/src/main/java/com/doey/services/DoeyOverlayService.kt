package com.doey.services

import android.app.Notification
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
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
import com.doey.ui.MainActivity

/**
 * Servicio de overlay flotante para Doey.
 * Muestra una burbuja arrastrable sobre cualquier app con el estado del asistente.
 */
class DoeyOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "doey_overlay"
        const val NOTIF_ID = 2001

        const val ACTION_SHOW = "com.doey.overlay.SHOW"
        const val ACTION_HIDE = "com.doey.overlay.HIDE"
        const val ACTION_UPDATE_STATUS = "com.doey.overlay.UPDATE_STATUS"
        const val ACTION_UPDATE_MESSAGE = "com.doey.overlay.UPDATE_MESSAGE"

        const val EXTRA_STATUS = "status"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_NEXT_ACTION = "next_action"

        @Volatile var instance: DoeyOverlayService? = null
        @Volatile var currentStatus: OverlayStatus = OverlayStatus.IDLE
        @Volatile var currentMessage: String = "Doey listo"
        @Volatile var nextAction: String = ""

        fun updateStatus(context: Context, status: OverlayStatus, message: String, nextAction: String = "") {
            currentStatus = status
            currentMessage = message
            DoeyOverlayService.nextAction = nextAction
            instance?.updateOverlayContent(status, message, nextAction)
        }
    }

    enum class OverlayStatus {
        IDLE, LISTENING, THINKING, ACTING, SUCCESS, ERROR
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isExpanded      = mutableStateOf(false)
    private var isBubbleVisible = mutableStateOf(true)   // FIX BUG-2: controla visibilidad sin destruir la vista
    private var statusState     = mutableStateOf(OverlayStatus.IDLE)
    private var messageState    = mutableStateOf("Doey listo")
    private var nextActionState = mutableStateOf("")

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        instance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
            ACTION_UPDATE_STATUS -> {
                val status = intent.getStringExtra(EXTRA_STATUS)?.let {
                    try { OverlayStatus.valueOf(it) } catch (_: Exception) { OverlayStatus.IDLE }
                } ?: OverlayStatus.IDLE
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                val next = intent.getStringExtra(EXTRA_NEXT_ACTION) ?: ""
                updateOverlayContent(status, message, next)
            }
            else -> showOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        // En onDestroy sí removemos la vista completamente
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        instance = null
        super.onDestroy()
    }

    fun updateOverlayContent(status: OverlayStatus, message: String, nextAction: String = "") {
        statusState.value = status
        messageState.value = message
        nextActionState.value = nextAction
        updateNotification(message)
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (!android.provider.Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DoeyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DoeyOverlayService)
            setContent {
                MaterialTheme {
                    // FIX BUG-2: AnimatedVisibility controla la burbuja sin destruir el servicio
                    AnimatedVisibility(
                        visible = isBubbleVisible.value,
                        enter   = scaleIn() + fadeIn(),
                        exit    = scaleOut() + fadeOut()
                    ) {
                        DoeyBubble(
                            isExpanded = isExpanded.value,
                            status     = statusState.value,
                            message    = messageState.value,
                            nextAction = nextActionState.value,
                            onToggleExpand = { isExpanded.value = !isExpanded.value },
                            onOpenApp = {
                                val i = Intent(this@DoeyOverlayService, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                }
                                startActivity(i)
                            },
                            // FIX BUG-2: cerrar solo colapsa, no destruye
                            onClose    = { isBubbleVisible.value = false },
                            onDismiss  = { dismissOverlay() }
                        )
                    }
                }
            }
        }

        // Drag listener
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        params.x = initialX - dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(composeView, params)
                        true
                    } else false
                }
                else -> false
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    // FIX BUG-2: hideOverlay ahora solo colapsa la burbuja (no destruye el servicio)
    private fun hideOverlay() {
        isBubbleVisible.value = false
        isExpanded.value = false
    }

    // Muestra la burbuja nuevamente (para el switch de ajustes)
    fun showBubble() {
        isBubbleVisible.value = true
    }

    // Cierre completo: remueve la vista y detiene el servicio
    private fun dismissOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Doey Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Burbuja flotante del asistente Doey"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Doey activo")
            .setContentText(currentMessage)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification())
    }
}

@Composable
private fun DoeyBubble(
    isExpanded: Boolean,
    status: DoeyOverlayService.OverlayStatus,
    message: String,
    nextAction: String,
    onToggleExpand: () -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit = onClose  // FIX BUG-2: onDismiss para cierre completo
) {
    val bubbleColor = when (status) {
        DoeyOverlayService.OverlayStatus.IDLE -> Color(0xFF1E1E2E)
        DoeyOverlayService.OverlayStatus.LISTENING -> Color(0xFF1A2A1A)
        DoeyOverlayService.OverlayStatus.THINKING -> Color(0xFF1A1A2A)
        DoeyOverlayService.OverlayStatus.ACTING -> Color(0xFF2A1A00)
        DoeyOverlayService.OverlayStatus.SUCCESS -> Color(0xFF1A2A1A)
        DoeyOverlayService.OverlayStatus.ERROR -> Color(0xFF2A1A1A)
    }

    val accentColor = when (status) {
        DoeyOverlayService.OverlayStatus.IDLE -> Color(0xFF7C4DFF)
        DoeyOverlayService.OverlayStatus.LISTENING -> Color(0xFF4CAF50)
        DoeyOverlayService.OverlayStatus.THINKING -> Color(0xFF2196F3)
        DoeyOverlayService.OverlayStatus.ACTING -> Color(0xFFFF9800)
        DoeyOverlayService.OverlayStatus.SUCCESS -> Color(0xFF4CAF50)
        DoeyOverlayService.OverlayStatus.ERROR -> Color(0xFFF44336)
    }

    val statusIcon = when (status) {
        DoeyOverlayService.OverlayStatus.IDLE -> Icons.Default.SmartToy
        DoeyOverlayService.OverlayStatus.LISTENING -> Icons.Default.Mic
        DoeyOverlayService.OverlayStatus.THINKING -> Icons.Default.Psychology
        DoeyOverlayService.OverlayStatus.ACTING -> Icons.Default.TouchApp
        DoeyOverlayService.OverlayStatus.SUCCESS -> Icons.Default.CheckCircle
        DoeyOverlayService.OverlayStatus.ERROR -> Icons.Default.Error
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubble_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isAnimating = status in listOf(
        DoeyOverlayService.OverlayStatus.LISTENING,
        DoeyOverlayService.OverlayStatus.THINKING,
        DoeyOverlayService.OverlayStatus.ACTING
    )

    Column(horizontalAlignment = Alignment.End) {
        // Burbuja principal
        Box(
            Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = if (isAnimating) pulseAlpha else 1f),
                            bubbleColor
                        )
                    )
                )
                .border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                statusIcon,
                contentDescription = "Doey",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Panel expandido
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bubbleColor,
                modifier = Modifier
                    .width(240.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(14.dp)) {
                    // Header
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = if (isAnimating) pulseAlpha else 1f))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (status) {
                                    DoeyOverlayService.OverlayStatus.IDLE -> "Listo"
                                    DoeyOverlayService.OverlayStatus.LISTENING -> "Escuchando..."
                                    DoeyOverlayService.OverlayStatus.THINKING -> "Pensando..."
                                    DoeyOverlayService.OverlayStatus.ACTING -> "Ejecutando..."
                                    DoeyOverlayService.OverlayStatus.SUCCESS -> "Completado"
                                    DoeyOverlayService.OverlayStatus.ERROR -> "Error"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onOpenApp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.OpenInFull,
                                    null,
                                    tint = Color(0xFF9E9E9E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // FIX BUG-2: este botón minimiza (no destruye)
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    "Ocultar",
                                    tint = Color(0xFF9E9E9E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Botón de cierre completo (apaga el servicio)
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.PowerSettingsNew,
                                    "Apagar burbuja",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Mensaje actual
                    Text(
                        message,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Próxima acción
                    if (nextAction.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    nextAction,
                                    fontSize = 11.sp,
                                    color = accentColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Botón abrir app
                    Button(
                        onClick = onOpenApp,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.OpenInFull, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir Doey", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
