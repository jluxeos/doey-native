package com.doey.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import com.doey.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── PALETA DE COLORES "CHIDA" (Glass Themes - Base Blanca) ──────────────────────

object GlassThemes {
    val NebulaPurple = Color(0xFF9C27B0)
    val AuroraGreen  = Color(0xFF4CAF50)
    val SolarOrange  = Color(0xFFFF9800)
    val DeepSeaBlue  = Color(0xFF2196F3)
    val CrimsonVoid  = Color(0xFFF44336)
    val CyberWhite   = Color(0xFFFFFFFF)
    val SoftGray     = Color(0xFFF8F9FA)
}

// Variables globales que se actualizan según el tema seleccionado
var TauBg       by mutableStateOf(GlassThemes.CyberWhite)
var TauAccent   by mutableStateOf(GlassThemes.DeepSeaBlue)
var TauAccentGlow by mutableStateOf(Color(0x332196F3))
var TauAccentLight by mutableStateOf(Color(0xFFBBDEFB))

// Colores estáticos de soporte (Base Blanca)
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

var TauText1 by mutableStateOf(Color(0xFF1A1A1A))
var TauText2 by mutableStateOf(Color(0xFF4A4A4A))
var TauText3 by mutableStateOf(Color(0xFF7A7A7A))

val DoeyTypography = Typography(
    displayLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 57.sp, color = TauText1),
    displayMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 45.sp, color = TauText1),
    displaySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = TauText1),
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = TauText1),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TauText1),
    headlineSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TauText1),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TauText1),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TauText1),
    titleSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TauText1),
    bodyLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TauText1),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TauText1),
    bodySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TauText1),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TauText1),
    labelMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = TauText1),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = TauText1)
)
var TauSurface1 by mutableStateOf(Color(0x1A000000))
var TauSurface2 by mutableStateOf(Color(0x33000000))
var TauSurface3 by mutableStateOf(Color(0x4D000000))
var TauSeparator by mutableStateOf(Color(0x1A000000))

val TauGreen    = GlassThemes.AuroraGreen
val TauRed      = GlassThemes.CrimsonVoid
val TauBlue     = GlassThemes.DeepSeaBlue
val TauOrange   = GlassThemes.SolarOrange

// Estado global de opacidad y desenfoque
    var GlassOpacity by mutableStateOf(0.55f) // Opacidad del fondo de tarjeta
    var GlassBlur    by mutableStateOf(20f)  // Desenfoque de la tarjeta

// Función para actualizar el tema globalmente
fun updateGlassTheme(themeName: String) {
    val color = when(themeName) {
        "NebulaPurple" -> GlassThemes.NebulaPurple
        "AuroraGreen"  -> GlassThemes.AuroraGreen
        "SolarOrange"  -> GlassThemes.SolarOrange
        "CrimsonVoid"  -> GlassThemes.CrimsonVoid
        else           -> GlassThemes.DeepSeaBlue
    }
    TauAccent = color
    TauAccentGlow = color.copy(alpha = 0.2f)
    TauAccentLight = color.copy(alpha = 0.4f)
    
    // Asegurar que el fondo sea blanco
    TauBg = GlassThemes.CyberWhite
    TauText1 = Color(0xFF1A1A1A)
    TauText2 = Color(0xFF4A4A4A)
    TauText3 = Color(0xFF7A7A7A)
}

// ── COMPONENTES MAESTROS GLASSMORPHISM ────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    opacity: Float = GlassOpacity,
    blur: Float = GlassBlur,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .shadow(elevation = 8.dp, spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Black.copy(alpha = 0.08f), shape = RoundedCornerShape(24.dp)) // Sombra de Elevación
    ) {
        // Capa de fondo con desenfoque y opacidad
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.55f)) // Fondo de Tarjeta: Blanco con 55% de opacidad
                .blur(if (blur > 0) blur.dp else 0.dp) // Efecto de Desenfoque
        )
        
        // Borde de Luz
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.5.dp, // Borde de Luz: 1.5px
                    color = Color.White.copy(alpha = 0.85f), // Borde de Luz: rgba(255, 255, 255, 0.85)
                    shape = RoundedCornerShape(24.dp)
                )
        )
        
        // Contenido (sin desenfoque)
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TauAccent,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = CircleShape, // Botones totalmente redondeados (pills)
        color = containerColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, containerColor.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TauText2, modifier = Modifier.padding(start = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TauText3) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)) // Radio de curvatura de 24px
                .background(Color.Black.copy(alpha = 0.03f))
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp)), // Radio de curvatura de 24px
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TauText1,
                unfocusedTextColor = TauText1
            ),
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon
        )
    }
}

@Composable
fun TauSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = TauText2, letterSpacing = 1.5.sp)
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun TauSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.03f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) TauAccent else TauText3, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TauText1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TauText3)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TauAccent,
                uncheckedThumbColor = TauText3,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.05f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ── COMPONENTES DEL DRAWER (MENÚ LATERAL) ─────────────────────────────────────

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) TauAccent.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            null, 
            tint = if (isSelected) TauAccent else TauText2, 
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
            Text(
                label, 
                color = if (isSelected) TauAccent else TauText1, 
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
            )
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TauText3,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        letterSpacing = 1.5.sp
    )
}

// ── EFECTO DE FONDO DINÁMICO ──────────────────────────────────────────────────

@Composable
fun GlassBackground(accentColor: Color = TauAccent) {
    Box(modifier = Modifier.fillMaxSize().background(TauBg)) {
        // Orbe de luz traslúcido 1
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .blur(60.dp) // Desenfoque de 60px
                .background(accentColor.copy(alpha = 0.1f), CircleShape)
        )
        // Orbe de luz traslúcido 2
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .blur(60.dp) // Desenfoque de 60px
                .background(accentColor.copy(alpha = 0.08f), CircleShape)
        )
    }
}

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = TauAccent,
    unfocusedBorderColor      = TauText3,
    focusedTextColor          = TauText1,
    unfocusedTextColor        = TauText1,
    focusedLabelColor         = TauAccent,
    unfocusedLabelColor       = TauText3,
    cursorColor               = TauAccent,
    focusedPlaceholderColor   = TauText3,
    unfocusedPlaceholderColor = TauText3,
    focusedContainerColor     = Color.Black.copy(alpha = 0.02f),
    unfocusedContainerColor   = Color.Black.copy(alpha = 0.02f)
)
