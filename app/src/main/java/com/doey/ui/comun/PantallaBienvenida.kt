package com.doey.ui.comun

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
import com.doey.ui.core.*

// ── Colores del Onboarding (Base Blanca) ──────────────────────────────────────
private val OBGradientStart = Color(0xFFFFFFFF)
private val OBGradientEnd   = Color(0xFFF5F5F5)
private val OBAccent        = Color(0xFF2196F3)
private val OBAccentLight   = Color(0xFFBBDEFB)
private val OBText          = Color(0xFF1A1A1A)
private val OBTextSub       = Color(0xFF7A7A7A)
private val OBCard          = Color(0x1A000000)
private val OBCardBorder    = Color(0x1A000000)

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
        // Fondo dinámico con orbes (estilo Glassmorphism)
        GlassBackground(accentColor = OBAccent)

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
                        selectedProfile = if (usageLevel == "Alto") UserProfile.ADVANCED else UserProfile.BASIC
                        currentStep = OnboardingStep.USER_PROFILE 
                    },
                    onBack = { currentStep = OnboardingStep.WELCOME }
                )
                OnboardingStep.USER_PROFILE -> UserProfileStep(
                    selected = selectedProfile,
                    onSelect = { selectedProfile = it },
                    onNext = { currentStep = OnboardingStep.PERFORMANCE_MODE },
                    onBack = { currentStep = OnboardingStep.USER_DATA }
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
                    .padding(top = 48.dp) // Ajustado para edge-to-edge
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
                            isActive -> OBAccent
                            isPast -> OBAccent.copy(alpha = 0.6f)
                            else -> OBCardBorder
                        }
                    )
            )
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
        // Logo animado con Glassmorphism
        Box(
            Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(OBAccent.copy(alpha = 0.2f), OBAccent.copy(alpha = 0.05f))
                    )
                )
                .border(1.dp, OBAccent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = OBAccent,
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
        Spacer(Modifier.height(100.dp))

        Text(
            "Datos personales",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            DoeyTextField(value = name, onValueChange = onNameChange, label = "Nombre", placeholder = "¿Cómo te llamas?")
            Spacer(Modifier.height(16.dp))
            DoeyTextField(value = age, onValueChange = onAgeChange, label = "Edad", placeholder = "¿Cuántos años tienes?")
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Text("Atrás")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
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
        Spacer(Modifier.height(100.dp))

        Text(
            "¿Cómo quieres usar Doey?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        UserProfile.values().forEach { profile ->
            ProfileCard(
                profile = profile,
                isSelected = selected == profile,
                onClick = { onSelect(profile) }
            )
            Spacer(Modifier.height(16.dp))
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Text("Atrás")
            }

            Button(
                onClick = onNext,
                enabled = selected != null,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) OBAccent else Color.Black.copy(alpha = 0.05f)
    val bgColor = if (isSelected) OBAccent.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.6f)

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
                    .background(if (isSelected) OBAccent else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    profile.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else OBTextSub,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    profile.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OBText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.subtitle,
                    fontSize = 13.sp,
                    color = OBTextSub,
                    lineHeight = 18.sp
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
        Spacer(Modifier.height(100.dp))

        Text(
            "Rendimiento",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        PerformanceMode.values().forEach { mode ->
            PerformanceCard(
                mode = mode,
                isSelected = selected == mode,
                onClick = { onSelect(mode) }
            )
            Spacer(Modifier.height(16.dp))
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Text("Atrás")
            }

            Button(
                onClick = onNext,
                enabled = selected != null,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    mode: PerformanceMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) OBAccent else Color.Black.copy(alpha = 0.05f)
    val bgColor = if (isSelected) OBAccent.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.6f)

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
                    .background(if (isSelected) OBAccent else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    mode.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else OBTextSub,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    mode.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OBText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    mode.subtitle,
                    fontSize = 13.sp,
                    color = OBTextSub,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionsOnboardingStep(onNext: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(100.dp))

        Text(
            "Permisos",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Doey necesita algunos permisos para funcionar correctamente.",
            fontSize = 14.sp,
            color = OBTextSub,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Puedes configurar los permisos ahora o más tarde desde los ajustes del sistema.", color = OBText, fontSize = 14.sp)
        }

        Spacer(Modifier.height(48.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Text("Atrás")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, color = Color.White)
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(100.dp))

        Text(
            "Configuración de IA",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OBText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            DoeyTextField(value = apiKey, onValueChange = onApiKeyChange, label = "API Key", placeholder = "Pega tu clave aquí", visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(16.dp))
            Text("Proveedor: $provider", color = OBTextSub, fontSize = 12.sp)
        }

        Spacer(Modifier.height(48.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OBAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent)
            ) {
                Text("Atrás")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Finalizar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
