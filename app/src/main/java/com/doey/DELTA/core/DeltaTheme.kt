package com.doey.DELTA.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.R

// ═══════════════════════════════════════════════════════════════
// DOEY DELTA — SISTEMA DE DISEÑO
// Tema: Deep Night (oscuro, morado profundo, sin glassmorphism)
// ═══════════════════════════════════════════════════════════════

// ── Paleta principal ─────────────────────────────────────────────
object DeltaColors {
    // Acentos
    val VioletPrimary  = Color(0xFF7C5DFA)   // morado vibrante
    val VioletSoft     = Color(0xFF9B7FFF)   // variante clara
    val VioletDim      = Color(0xFF2D2260)   // sombra profunda
    val VioletGlow     = Color(0x267C5DFA)   // brillo sutil

    // Temas alternativos
    val EmeraldAccent  = Color(0xFF10B981)
    val SunsetAccent   = Color(0xFFF59E0B)
    val RoseAccent     = Color(0xFFF43F5E)
    val OceanAccent    = Color(0xFF0EA5E9)

    // Fondos (modo oscuro)
    val Bg0            = Color(0xFF0D0D14)   // fondo base ultra oscuro
    val Bg1            = Color(0xFF13131F)   // superficie 1
    val Bg2            = Color(0xFF1A1A2E)   // superficie 2
    val Bg3            = Color(0xFF22223A)   // superficie elevada

    // Texto
    val Text1          = Color(0xFFF0F0FF)   // blanco suave
    val Text2          = Color(0xFFB0B0CC)   // gris claro
    val Text3          = Color(0xFF606080)   // gris apagado

    // Semánticos
    val Success        = Color(0xFF10B981)
    val Error          = Color(0xFFF43F5E)
    val Warning        = Color(0xFFF59E0B)
    val Separator      = Color(0xFF252540)
}

// ── Variables de tema mutables ────────────────────────────────────
var DeltaBg          by mutableStateOf(DeltaColors.Bg0)
var DeltaSurface1    by mutableStateOf(DeltaColors.Bg1)
var DeltaSurface2    by mutableStateOf(DeltaColors.Bg2)
var DeltaSurface3    by mutableStateOf(DeltaColors.Bg3)
var DeltaAccent      by mutableStateOf(DeltaColors.VioletPrimary)
var DeltaAccentSoft  by mutableStateOf(DeltaColors.VioletSoft)
var DeltaAccentGlow  by mutableStateOf(DeltaColors.VioletGlow)
var DeltaText1       by mutableStateOf(DeltaColors.Text1)
var DeltaText2       by mutableStateOf(DeltaColors.Text2)
var DeltaText3       by mutableStateOf(DeltaColors.Text3)
var DeltaSeparator   by mutableStateOf(DeltaColors.Separator)

// Alias para compatibilidad con archivos que usen Tau*
val TauBg          get() = DeltaBg
val TauSurface1    get() = DeltaSurface1
val TauSurface2    get() = DeltaSurface2
val TauSurface3    get() = DeltaSurface3
val TauAccent      get() = DeltaAccent
val TauAccentGlow  get() = DeltaAccentGlow
val TauAccentLight get() = DeltaAccentGlow
val TauText1       get() = DeltaText1
val TauText2       get() = DeltaText2
val TauText3       get() = DeltaText3
val TauSeparator   get() = DeltaSeparator
val TauGreen       get() = DeltaColors.Success
val TauRed         get() = DeltaColors.Error
val TauBlue        get() = DeltaColors.OceanAccent
val TauOrange      get() = DeltaColors.Warning
val TauPurple      get() = DeltaColors.VioletPrimary

// Estos parámetros ya no generan efecto visual — mantenidos solo
// para que los call-sites que los pasan no necesiten modificarse.
var GlassOpacity by mutableStateOf(1.0f)
var GlassBlur    by mutableStateOf(0f)

// ── Aplicar tema por nombre ───────────────────────────────────────
fun updateGlassTheme(name: String) {
    DeltaAccent = when (name) {
        "NebulaPurple" -> DeltaColors.VioletPrimary
        "AuroraGreen"  -> DeltaColors.EmeraldAccent
        "SolarOrange"  -> DeltaColors.SunsetAccent
        "CrimsonVoid"  -> DeltaColors.RoseAccent
        "OceanBlue"    -> DeltaColors.OceanAccent
        else           -> DeltaColors.VioletPrimary
    }
    DeltaAccentSoft = DeltaAccent.copy(alpha = 0.8f)
    DeltaAccentGlow = DeltaAccent.copy(alpha = 0.15f)
}

// ── Tipografía ────────────────────────────────────────────────────
val ProductSans = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold,    FontWeight.Bold),
    Font(R.font.poppins_bold,    FontWeight.SemiBold),
)
val Poppins = ProductSans

object DoeyTypography {
    val displayLarge   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 57.sp)
    val displayMedium  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 45.sp)
    val displaySmall   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 36.sp)
    val headlineLarge  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 32.sp)
    val headlineMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 28.sp)
    val headlineSmall  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 24.sp)
    val titleLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 22.sp)
    val titleMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 16.sp)
    val titleSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp)
    val bodyLarge      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 16.sp)
    val bodyMedium     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    val bodySmall      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val labelLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp)
    val labelMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val labelSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 11.sp)
}

// ════════════════════════════════════════════════════════════════
// COMPONENTES DELTA
// ════════════════════════════════════════════════════════════════

// ── Fondo degradado sutil ────────────────────────────────────────
@Composable
fun DeltaBackground() {
    val gradient = Brush.radialGradient(
        0.0f to DeltaAccent.copy(alpha = 0.08f),
        0.6f to DeltaBg,
        1.0f to DeltaBg,
        radius = 900f
    )
    Box(Modifier.fillMaxSize().background(DeltaBg)) {
        Box(Modifier.fillMaxSize().background(gradient))
    }
}

// Alias para compatibilidad
@Composable
fun GlassBackground(accentColor: Color = DeltaAccent) {
    DeltaBackground()
}

// ── Card elevada ─────────────────────────────────────────────────
@Composable
fun DeltaCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(DeltaSurface1)
            .border(1.dp, DeltaSeparator, shape)
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

// Alias GlassCard → DeltaCard para compatibilidad total
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    opacity: Float = 1.0f,
    blur: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) = DeltaCard(modifier = modifier, radius = radius, content = content)

// ── Botón primario ───────────────────────────────────────────────
@Composable
fun DeltaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DeltaAccent,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick         = onClick,
        modifier        = modifier.height(52.dp),
        shape           = CircleShape,
        color           = color,
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DeltaAccent,
    content: @Composable RowScope.() -> Unit
) = DeltaButton(onClick = onClick, modifier = modifier, color = containerColor, content = content)

// ── Botón outline ────────────────────────────────────────────────
@Composable
fun GlassOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = DeltaAccent,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.5.dp, accentColor.copy(alpha = 0.40f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }
}

// ── TextField ────────────────────────────────────────────────────
@Composable
fun DoeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = DoeyTypography.labelMedium, color = DeltaText2,
            modifier = Modifier.padding(start = 4.dp))
        TextField(
            value           = value,
            onValueChange   = onValueChange,
            placeholder     = { Text(placeholder, style = DoeyTypography.bodyMedium, color = DeltaText3) },
            modifier        = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeltaSurface2)
                .border(1.dp, DeltaSeparator, RoundedCornerShape(14.dp)),
            colors          = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = DeltaText1,
                unfocusedTextColor      = DeltaText1,
            ),
            visualTransformation = visualTransformation,
            trailingIcon         = trailingIcon
        )
    }
}

// ── Settings Section ─────────────────────────────────────────────
@Composable
fun TauSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp)
        ) {
            Icon(icon, null, tint = DeltaAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), style = DoeyTypography.labelSmall,
                color = DeltaText3, letterSpacing = 1.2.sp)
        }
        DeltaCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

// ── Switch Row ───────────────────────────────────────────────────
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
                .background(if (checked) DeltaAccent.copy(alpha = 0.15f) else DeltaSurface3),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = if (checked) DeltaAccent else DeltaText3,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = DoeyTypography.bodyMedium, color = DeltaText1)
            Text(subtitle, style = DoeyTypography.labelSmall, color = DeltaText3)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = DeltaAccent,
                uncheckedThumbColor  = DeltaText3,
                uncheckedTrackColor  = DeltaSurface3,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ── Drawer items ─────────────────────────────────────────────────
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
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) DeltaAccent.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width  = if (isSelected) 1.dp else 0.dp,
                color  = if (isSelected) DeltaAccent.copy(alpha = 0.25f) else Color.Transparent,
                shape  = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint     = if (isSelected) DeltaAccent else DeltaText2,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label,
            color = if (isSelected) DeltaAccent else DeltaText1,
            style = if (isSelected) DoeyTypography.titleMedium else DoeyTypography.bodyLarge)
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text          = title.uppercase(),
        style         = DoeyTypography.labelSmall,
        color         = DeltaText3,
        modifier      = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
        letterSpacing = 1.2.sp
    )
}

// ── OutlinedTextField colors ─────────────────────────────────────
@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = DeltaAccent,
    unfocusedBorderColor      = DeltaSeparator,
    focusedTextColor          = DeltaText1,
    unfocusedTextColor        = DeltaText1,
    focusedLabelColor         = DeltaAccent,
    unfocusedLabelColor       = DeltaText3,
    cursorColor               = DeltaAccent,
    focusedPlaceholderColor   = DeltaText3,
    unfocusedPlaceholderColor = DeltaText3,
    focusedContainerColor     = DeltaSurface2,
    unfocusedContainerColor   = DeltaSurface2
)
