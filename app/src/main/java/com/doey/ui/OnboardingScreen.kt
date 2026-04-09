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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    PERSONAL_DATA,
    MODE_SUGGESTION,
    IA_CONFIG,
    PERMISSIONS,
    DONE
}

enum class UsageLevel(val label: String, val icon: ImageVector) {
    LOW("Bajo", Icons.Default.SignalCellularAlt1Bar),
    MEDIUM("Medio", Icons.Default.SignalCellularAlt2Bar),
    HIGH("Alto", Icons.Default.SignalCellularAlt)
}

@Composable
fun OnboardingFlow(
    onComplete: (name: String, age: String, usageLevel: String, profile: String, performance: String, provider: String, apiKey: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    
    // Datos Personales
    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var usageLevel by remember { mutableStateOf(UsageLevel.MEDIUM) }
    
    // Sugerencia de modo
    var selectedProfile by remember { mutableStateOf("basic") }
    var isManualModeSelection by remember { mutableStateOf(false) }
    
    // IA
    var selectedProvider by remember { mutableStateOf("gemini") }
    var apiKey by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OBGradientStart, OBGradientEnd)))
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
                OnboardingStep.WELCOME -> WelcomeStep(onNext = { currentStep = OnboardingStep.PERSONAL_DATA })
                
                OnboardingStep.PERSONAL_DATA -> PersonalDataStep(
                    name = userName, onNameChange = { userName = it },
                    age = userAge, onAgeChange = { userAge = it },
                    level = usageLevel, onLevelChange = { usageLevel = it },
                    onNext = { 
                        // Lógica de sugerencia: Si el nivel es Alto, sugerir Experto.
                        selectedProfile = if (usageLevel == UsageLevel.HIGH) "advanced" else "basic"
                        currentStep = OnboardingStep.MODE_SUGGESTION 
                    },
                    onBack = { currentStep = OnboardingStep.WELCOME }
                )
                
                OnboardingStep.MODE_SUGGESTION -> ModeSuggestionStep(
                    suggestedProfile = selectedProfile,
                    isManual = isManualModeSelection,
                    onModeChange = { selectedProfile = it; isManualModeSelection = true },
                    onNext = { currentStep = OnboardingStep.IA_CONFIG },
                    onBack = { currentStep = OnboardingStep.PERSONAL_DATA }
                )
                
                OnboardingStep.IA_CONFIG -> IAConfigStep(
                    provider = selectedProvider,
                    apiKey = apiKey,
                    onProviderChange = { selectedProvider = it },
                    onApiKeyChange = { apiKey = it },
                    onNext = { currentStep = OnboardingStep.PERMISSIONS },
                    onBack = { currentStep = OnboardingStep.MODE_SUGGESTION }
                )
                
                OnboardingStep.PERMISSIONS -> PermissionsStep(
                    onNext = {
                        onComplete(
                            userName,
                            userAge,
                            usageLevel.name.lowercase(),
                            selectedProfile,
                            if (selectedProfile == "advanced") "high" else "low",
                            selectedProvider,
                            apiKey
                        )
                    },
                    onBack = { currentStep = OnboardingStep.IA_CONFIG }
                )
                OnboardingStep.DONE -> {}
            }
        }

        if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.DONE) {
            OnboardingProgressIndicator(currentStep, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SmartToy, null, tint = OBAccent, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(24.dp))
        Text("¿Qué es Doey?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = OBText)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tu nuevo asistente personal inteligente. Doey te ayuda a controlar tu dispositivo, gestionar tu tiempo y mucho más de forma sencilla.",
            textAlign = TextAlign.Center, color = OBTextSub, fontSize = 16.sp
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OBAccent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun PersonalDataStep(
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    level: UsageLevel, onLevelChange: (UsageLevel) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Datos personales", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OBText)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OBText, unfocusedTextColor = OBText, focusedBorderColor = OBAccent)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = age, onValueChange = onAgeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Edad") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OBText, unfocusedTextColor = OBText, focusedBorderColor = OBAccent)
        )
        Spacer(Modifier.height(24.dp))
        
        Text("Nivel de uso tecnológico", color = OBText, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageLevel.values().forEach { item ->
                val sel = level == item
                Surface(
                    onClick = { onLevelChange(item) },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (sel) OBAccent else OBCard,
                    border = if (sel) null else androidx.compose.foundation.BorderStroke(1.dp, OBCardBorder)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(item.icon, null, tint = if (sel) Color.White else OBTextSub)
                        Text(item.label, color = if (sel) Color.White else OBTextSub, fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Atrás", color = OBTextSub) }
            Button(
                onClick = onNext,
                enabled = name.isNotBlank() && age.isNotBlank(),
                modifier = Modifier.weight(2f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent)
            ) { Text("Continuar") }
        }
    }
}

@Composable
private fun ModeSuggestionStep(
    suggestedProfile: String,
    isManual: Boolean,
    onModeChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Sugerencia de modo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OBText)
        Spacer(Modifier.height(16.dp))
        
        val isBasic = suggestedProfile == "basic"
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = OBAccent.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, OBAccent.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (isManual) "Modo seleccionado:" else "Según tus respuestas, te sugerimos:",
                    color = OBAccentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (isBasic) "Modo Básico" else "Modo Experto",
                    color = OBText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Explicación
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = OBCard) {
            Column(Modifier.padding(16.dp)) {
                Text(if (isBasic) "Qué hace el modo básico:" else "Qué hace el modo experto:", color = OBText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isBasic) "Interfaz simplificada, comandos de voz directos y optimización automática para una experiencia sin complicaciones."
                    else "Control total sobre la IA, acceso a logs técnicos, configuración detallada de tokens y herramientas avanzadas.",
                    color = OBTextSub, fontSize = 14.sp
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        TextButton(onClick = { onModeChange(if (isBasic) "advanced" else "basic") }) {
            Text(if (isBasic) "Quiero cambiar a Modo Experto" else "Quiero cambiar a Modo Básico", color = OBAccentLight)
        }
        
        if (isManual) {
            Spacer(Modifier.height(8.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF5252).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
            ) {
                Text(
                    "⚠️ Cambiar el modo puede afectar la experiencia. Continúas bajo tu propio riesgo.",
                    color = Color(0xFFFF5252), fontSize = 11.sp, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Atrás", color = OBTextSub) }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent)
            ) { Text(if (isManual) "Aceptar selección" else "Aceptar sugerencia") }
        }
    }
}

@Composable
private fun IAConfigStep(
    provider: String, apiKey: String,
    onProviderChange: (String) -> Unit, onApiKeyChange: (String) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Configuración de IA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OBText)
        Spacer(Modifier.height(32.dp))
        
        Text("Proveedor de IA", color = OBText, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val providers = listOf("gemini" to "Gemini", "openai" to "ChatGPT", "groq" to "Groq", "openrouter" to "OpenRouter")
        providers.forEach { (id, label) ->
            val sel = provider == id
            Surface(
                onClick = { onProviderChange(id) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (sel) OBAccent else OBCard,
                border = if (sel) null else androidx.compose.foundation.BorderStroke(1.dp, OBCardBorder)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = sel, onClick = { onProviderChange(id) }, colors = RadioButtonDefaults.colors(selectedColor = Color.White))
                    Text(label, color = if (sel) Color.White else OBText)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = apiKey, onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OBText, unfocusedTextColor = OBText, focusedBorderColor = OBAccent)
        )
        
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Atrás", color = OBTextSub) }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent)
            ) { Text("Probar y continuar") }
        }
    }
}

@Composable
private fun PermissionsStep(onNext: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text("Permisos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OBText)
        Spacer(Modifier.height(16.dp))
        Text("Para funcionar correctamente, Doey necesita algunos accesos:", color = OBTextSub, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(32.dp))
        listOf(
            Icons.Default.Mic to "Micrófono",
            Icons.Default.Notifications to "Notificaciones",
            Icons.Default.Accessibility to "Accesibilidad"
        ).forEach { (icon, label) ->
            Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = OBCard) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = OBAccentLight)
                    Spacer(Modifier.width(16.dp))
                    Text(label, color = OBText)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.CheckCircle, null, tint = OBAccent.copy(alpha = 0.3f))
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Atrás", color = OBTextSub) }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(2f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OBAccent)
            ) { Text("Finalizar") }
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(step: OnboardingStep, modifier: Modifier) {
    val steps = OnboardingStep.values().filter { it != OnboardingStep.WELCOME && it != OnboardingStep.DONE }
    val currentIdx = steps.indexOf(step)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { idx, _ ->
            Box(Modifier.size(height = 4.dp, width = 20.dp).clip(CircleShape).background(if (idx <= currentIdx) OBAccent else OBCardBorder))
        }
    }
}
