package com.doey.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colores del Onboarding ────────────────────────────────────────────────────
private val OBGradientStart = Color(0xFF0D0D1A)
private val OBGradientEnd   = Color(0xFF1A0A2E)
private val OBAccent        = Color(0xFF7C4DFF)
private val OBAccentLight   = Color(0xFFBB86FC)
private val OBText          = Color(0xFFFFFFFF)
private val OBTextSub       = Color(0xFFB0B0C0)
private val OBCard          = Color(0xFF1E1E2E)
private val OBCardBorder    = Color(0xFF3A3A5C)

enum class OnboardingStep {
    WELCOME,
    USER_DATA, // Nombre y Edad
    USER_PROFILE, // Básico / Experto
    PERFORMANCE_MODE,
    PERMISSIONS,
    API_KEY,
    DONE
}

enum class UserProfile(val title: String, val subtitle: String, val icon: ImageVector) {
    BASIC(
        "Modo Básico",
        "Para personas que prefieren una experiencia simple y guiada. Doey hará todo por ti con solo hablarle.",
        Icons.Default.AutoAwesome
    ),
    ADVANCED(
        "Modo Experto",
        "Para usuarios que quieren control total: skills, logs, configuración avanzada y automatizaciones complejas.",
        Icons.Default.Psychology
    )
}

enum class PerformanceMode(val title: String, val subtitle: String, val icon: ImageVector) {
    LOW_POWER(
        "Bajo Consumo",
        "Optimizado para dispositivos de gama baja. Sin animaciones, menos procesos en segundo plano.",
        Icons.Default.BatteryAlert
    ),
    HIGH_PERFORMANCE(
        "Alto Rendimiento",
        "Experiencia completa con animaciones, funciones avanzadas y mayor capacidad de respuesta.",
        Icons.Default.Speed
    )
}

@Composable
fun OnboardingFlow(
    onComplete: (name: String, profile: UserProfile, performance: PerformanceMode, provider: String, apiKey: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var usageLevel by remember { mutableStateOf("Bajo") } // Bajo, Medio, Alto
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedPerformance by remember { mutableStateOf<PerformanceMode?>(null) }
    var selectedProvider by remember { mutableStateOf("openrouter") }
    var apiKey by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(OBGradientStart, OBGradientEnd))
            )
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onNext = { currentStep = OnboardingStep.USER_DATA }
                )
                OnboardingStep.USER_DATA -> UserDataStep(
                    name = userName,
                    age = userAge,
                    usageLevel = usageLevel,
                    onNameChange = { userName = it },
                    onAgeChange = { userAge = it },
                    onUsageLevelChange = { usageLevel = it },
                    onNext = { 
                        // Lógica de sugerencia de modo según nivel de uso
                        selectedProfile = if (usageLevel == "Alto") UserProfile.ADVANCED else UserProfile.BASIC
                        currentStep = OnboardingStep.USER_PROFILE 
                    },
                    onBack = { currentStep = OnboardingStep.WELCOME }
                ))
                OnboardingStep.USER_PROFILE -> UserProfileStep(
                    selected = selectedProfile,
                    onSelect = { selectedProfile = it },
                    onNext = { currentStep = OnboardingStep.PERFORMANCE_MODE },
                    onBack = { currentStep = OnboardingStep.USER_NAME }
                )
                OnboardingStep.PERFORMANCE_MODE -> PerformanceModeStep(
                    selected = selectedPerformance,
                    onSelect = { selectedPerformance = it },
                    onNext = { currentStep = OnboardingStep.PERMISSIONS },
                    onBack = { currentStep = OnboardingStep.USER_PROFILE }
                )
                OnboardingStep.PERMISSIONS -> PermissionsOnboardingStep(
                    onNext = { currentStep = OnboardingStep.API_KEY },
                    onBack = { currentStep = OnboardingStep.PERFORMANCE_MODE }
                )
                OnboardingStep.API_KEY -> ApiKeyStep(
                    provider = selectedProvider,
                    apiKey = apiKey,
                    onProviderChange = { selectedProvider = it },
                    onApiKeyChange = { apiKey = it },
                    onNext = {
                        onComplete(
                            userName,
                            selectedProfile ?: UserProfile.BASIC,
                            selectedPerformance ?: PerformanceMode.LOW_POWER,
                            selectedProvider,
                            apiKey
                        )
                    },
                    onBack = { currentStep = OnboardingStep.PERMISSIONS }
                )
                OnboardingStep.DONE -> {}
            }
        }

        // Indicador de progreso
        if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.DONE) {
            OnboardingProgressIndicator(
                currentStep = currentStep,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep.USER_PROFILE,
        OnboardingStep.PERFORMANCE_MODE,
        OnboardingStep.PERMISSIONS,
        OnboardingStep.API_KEY
    )
    val currentIndex = steps.indexOf(currentStep)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val isActive = index == currentIndex
            val isPast = index < currentIndex
            Box(
                Modifier
                    .height(4.dp)
                    .width(if (isActive) 24.dp else 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isActive -> OBAccentLight
                            isPast -> OBAccent.copy(alpha = 0.6f)
                            else -> OBCardBorder
                        }
                    )
            )
        }
    }
}

@Composable
private fun UserDataStep(
    name: String,
    age: String,
    usageLevel: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onUsageLevelChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text(
            "Datos personales",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Nombre
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre", color = OBTextSub) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OBText, unfocusedTextColor = OBText,
                focusedBorderColor = OBAccent, unfocusedBorderColor = OBCardBorder
            )
        )

        Spacer(Modifier.height(16.dp))

        // Edad
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad", color = OBTextSub) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OBText, unfocusedTextColor = OBText,
                focusedBorderColor = OBAccent, unfocusedBorderColor = OBCardBorder
            )
        )

        Spacer(Modifier.height(24.dp))

        // Nivel de uso
        Text("Nivel de uso", color = OBText, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Bajo", "Medio", "Alto").forEach { level ->
                val sel = usageLevel == level
                Surface(
                    onClick = { onUsageLevelChange(level) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (sel) OBAccent else OBCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sel) OBAccentLight else OBCardBorder),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(level, color = if (sel) Color.White else OBTextSub, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Atrás")
            }
            Button(onClick = onNext, enabled = name.isNotBlank() && age.isNotBlank(), modifier = Modifier.weight(2f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo animado
        Box(
            Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(OBAccent, OBAccentLight.copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            "Hola, soy Doey",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Tu asistente personal inteligente.\nPuedo hacer casi cualquier cosa en tu teléfono por ti.",
            fontSize = 16.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(16.dp))

        // Chips de características
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            FeatureChip("🗣️ Voz")
            FeatureChip("🤖 IA")
            FeatureChip("📱 Acciones")
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Comenzar configuración",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null, tint = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Solo tomará 2 minutos",
            fontSize = 12.sp,
            color = OBTextSub
        )
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = OBAccent.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent.copy(alpha = 0.4f))
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = OBAccentLight,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun UserProfileStep(
    selected: UserProfile?,
    onSelect: (UserProfile) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            "¿Cómo quieres usar Doey?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Elige el perfil que mejor te describe",
            fontSize = 14.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        UserProfile.values().forEach { profile ->
            Column {
                ProfileCard(
                    profile = profile,
                    isSelected = selected == profile,
                    onClick = { onSelect(profile) }
                )
                // Sugerencia de modo
                if (selected == profile) {
                    Text(
                        text = "✨ Modo sugerido para ti",
                        fontSize = 11.sp,
                        color = OBAccentLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Advertencia si cambia el modo sugerido (simplificado)
        Surface(
            color = Color(0x22FF9800),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Cambiar el modo puede afectar la experiencia. Continúas bajo tu propio riesgo.",
                    fontSize = 11.sp, color = Color(0xFFFFB74D)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccentLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = OBAccentLight)
                Spacer(Modifier.width(8.dp))
                Text("Atrás", color = OBAccentLight)
            }

            Button(
                onClick = onNext,
                enabled = selected != null,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) OBAccent else OBCardBorder
    val bgColor = if (isSelected) OBAccent.copy(alpha = 0.15f) else OBCard

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) OBAccent else OBCardBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    profile.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    profile.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OBAccentLight else OBText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.subtitle,
                    fontSize = 13.sp,
                    color = OBTextSub,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OBAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PerformanceModeStep(
    selected: PerformanceMode?,
    onSelect: (PerformanceMode) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            "¿Cómo es tu dispositivo?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Esto optimizará Doey para tu teléfono",
            fontSize = 14.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        PerformanceMode.values().forEach { mode ->
            PerformanceModeCard(
                mode = mode,
                isSelected = selected == mode,
                onClick = { onSelect(mode) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // Nota informativa
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A2A1A)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Puedes cambiar esto en cualquier momento desde Ajustes → Rendimiento.",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccentLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = OBAccentLight)
                Spacer(Modifier.width(8.dp))
                Text("Atrás", color = OBAccentLight)
            }

            Button(
                onClick = onNext,
                enabled = selected != null,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PerformanceModeCard(
    mode: PerformanceMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) OBAccent else OBCardBorder
    val bgColor = if (isSelected) OBAccent.copy(alpha = 0.15f) else OBCard

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (mode) {
                            PerformanceMode.LOW_POWER -> Color(0xFF2A1A00)
                            PerformanceMode.HIGH_PERFORMANCE -> Color(0xFF001A2A)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    mode.icon,
                    contentDescription = null,
                    tint = when (mode) {
                        PerformanceMode.LOW_POWER -> Color(0xFFFFB300)
                        PerformanceMode.HIGH_PERFORMANCE -> Color(0xFF00B0FF)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    mode.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OBAccentLight else OBText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    mode.subtitle,
                    fontSize = 13.sp,
                    color = OBTextSub,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OBAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionsOnboardingStep(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val permissionItems = listOf(
        PermissionOnboardingItem(
            icon = Icons.Default.Accessibility,
            title = "Accesibilidad",
            description = "Permite a Doey tocar la pantalla y controlar apps por ti",
            isSpecial = true,
            action = {
                try {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
            }
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.Notifications,
            title = "Notificaciones",
            description = "Para leer y gestionar tus notificaciones",
            isSpecial = true,
            action = {
                try {
                    ctx.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
            }
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.Mic,
            title = "Micrófono",
            description = "Para escucharte y entender tus comandos de voz",
            isSpecial = false,
            action = null
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.Contacts,
            title = "Contactos",
            description = "Para llamar y enviar mensajes a tus contactos",
            isSpecial = false,
            action = null
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.Sms,
            title = "Mensajes",
            description = "Para leer y enviar SMS por ti",
            isSpecial = false,
            action = null
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.LocationOn,
            title = "Ubicación",
            description = "Para darte información basada en tu ubicación",
            isSpecial = false,
            action = null
        ),
        PermissionOnboardingItem(
            icon = Icons.Default.Layers,
            title = "Superposición de pantalla",
            description = "Para mostrar la burbuja flotante de Doey sobre otras apps",
            isSpecial = true,
            action = {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${ctx.packageName}")
                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ctx.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            "Permisos necesarios",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Doey necesita estos permisos para funcionar correctamente",
            fontSize = 14.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        permissionItems.forEach { item ->
            PermissionOnboardingCard(item = item)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A2A)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = OBAccentLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Todos los permisos son usados únicamente para ejecutar tus comandos. Doey nunca envía tus datos a terceros sin tu autorización.",
                    fontSize = 12.sp,
                    color = OBTextSub,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccentLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = OBAccentLight)
                Spacer(Modifier.width(8.dp))
                Text("Atrás", color = OBAccentLight)
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private data class PermissionOnboardingItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isSpecial: Boolean,
    val action: (() -> Unit)?
)

@Composable
private fun PermissionOnboardingCard(item: PermissionOnboardingItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OBCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OBCardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OBAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = OBAccentLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OBText
                    )
                    if (item.isSpecial) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3A1A00)
                        ) {
                            Text(
                                "Manual",
                                fontSize = 9.sp,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    item.description,
                    fontSize = 12.sp,
                    color = OBTextSub,
                    lineHeight = 16.sp
                )
            }

            if (item.action != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = item.action,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Activar",
                        fontSize = 12.sp,
                        color = OBAccentLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyStep(
    provider: String,
    apiKey: String,
    onProviderChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val providers = listOf(
        "openrouter" to "OpenRouter (Recomendado)",
        "gemini" to "Google Gemini",
        "groq" to "Groq (Rápido y gratuito)",
        "openai" to "OpenAI (GPT-4)"
    )

    val providerLinks = mapOf(
        "openrouter" to "https://openrouter.ai/keys",
        "gemini" to "https://aistudio.google.com/apikey",
        "groq" to "https://console.groq.com/keys",
        "openai" to "https://platform.openai.com/api-keys"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Icon(
            Icons.Default.Key,
            contentDescription = null,
            tint = OBAccentLight,
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Configura tu IA",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Doey necesita una clave API para conectarse a la inteligencia artificial",
            fontSize = 14.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Selector de proveedor
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OBCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OBCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Proveedor de IA",
                    fontSize = 13.sp,
                    color = OBTextSub,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                providers.forEach { (id, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onProviderChange(id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == id,
                            onClick = { onProviderChange(id) },
                            colors = RadioButtonDefaults.colors(selectedColor = OBAccent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name, fontSize = 15.sp, color = OBText)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Campo de API key
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OBCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OBCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Clave API",
                    fontSize = 13.sp,
                    color = OBTextSub,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...", color = OBTextSub) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = OBTextSub
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OBAccent,
                        unfocusedBorderColor = OBCardBorder,
                        focusedTextColor = OBText,
                        unfocusedTextColor = OBText,
                        cursorColor = OBAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Botón para obtener clave
                val link = providerLinks[provider] ?: ""
                TextButton(
                    onClick = {
                        try {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(link)
                                ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        } catch (_: Exception) {}
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, null, tint = OBAccentLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Obtener clave gratis en ${providers.find { it.first == provider }?.second ?: provider}",
                        fontSize = 13.sp,
                        color = OBAccentLight
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Nota de seguridad
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A2A)
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tu clave se almacena de forma cifrada en tu dispositivo y nunca se comparte.",
                    fontSize = 12.sp,
                    color = OBTextSub,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccentLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = OBAccentLight)
                Spacer(Modifier.width(8.dp))
                Text("Atrás", color = OBAccentLight)
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (apiKey.isNotBlank()) OBAccent else OBAccent.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (apiKey.isBlank()) "Omitir por ahora" else "¡Listo!",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (apiKey.isBlank()) Icons.Default.SkipNext else Icons.Default.Check,
                    null,
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
