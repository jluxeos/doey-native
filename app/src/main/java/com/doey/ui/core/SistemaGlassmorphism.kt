package com.doey.ui.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

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

// ── PALETA DE COLORES — TONOS SOBRIOS ─────────────────────────────────────────
// Reemplazamos los colores saturados por versiones apagadas y elegantes

object GlassThemes {
    // Versiones sobrias / desaturadas de los temas
    val NebulaPurple = Color(0xFF7B5EA7)   // Lila apagado
    val AuroraGreen  = Color(0xFF4A7C59)   // Verde bosque
    val SolarOrange  = Color(0xFFB5722A)   // Ámbar oscuro
    val DeepSeaBlue  = Color(0xFF3B6EA5)   // Azul pizarra
    val CrimsonVoid  = Color(0xFFB04A4A)   // Rojo ladrillo
    val CyberWhite   = Color(0xFFFFFFFF)
    val SoftGray     = Color(0xFFF5F5F5)
}

// ── VARIABLES DE TEMA GLOBAL ──────────────────────────────────────────────────

var TauBg          by mutableStateOf(Color(0xFFF0F2F7))   // Gris azulado muy suave
var TauAccent      by mutableStateOf(GlassThemes.DeepSeaBlue)
var TauAccentGlow  by mutableStateOf(Color(0x223B6EA5))
var TauAccentLight by mutableStateOf(Color(0xFFD0E4F7))

var TauText1    by mutableStateOf(Color(0xFF1C1C1E))   // Negro iOS suave
var TauText2    by mutableStateOf(Color(0xFF3C3C43))
var TauText3    by mutableStateOf(Color(0xFF8E8E93))   // Gris sistema

// Superficies sólidas (sin efecto vidrio)
// Blanco puro con leve opacidad para que se note el fondo
var TauSurface1  by mutableStateOf(Color(0xFFFBFCFF))   // Blanco ligeramente azulado
var TauSurface2  by mutableStateOf(Color(0xFFEEF2FA))   // Hover / activo suave
var TauSurface3  by mutableStateOf(Color(0xFFDDE3EF))   // Placeholder / deshabilitado
var TauSeparator by mutableStateOf(Color(0xFFE2E6F0))   // Borde muy suave

val TauGreen  = GlassThemes.AuroraGreen
val TauRed    = GlassThemes.CrimsonVoid
val TauBlue   = GlassThemes.DeepSeaBlue
val TauOrange = GlassThemes.SolarOrange
val TauPurple = GlassThemes.NebulaPurple

// Glass params — mantenidos por compatibilidad, ya no generan blur real
var GlassOpacity by mutableStateOf(1.0f)
var GlassBlur    by mutableStateOf(0f)

// ── TIPOGRAFÍA ────────────────────────────────────────────────────────────────

val ProductSans = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold,    FontWeight.Bold),
    Font(R.font.poppins_bold,    FontWeight.SemiBold),
)

val Poppins = ProductSans

object DoeyTypography {
    val displayLarge   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 57.sp, color = Color(0xFF1C1C1E))
    val displayMedium  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 45.sp, color = Color(0xFF1C1C1E))
    val displaySmall   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 36.sp, color = Color(0xFF1C1C1E))
    val headlineLarge  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 32.sp, color = Color(0xFF1C1C1E))
    val headlineMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 28.sp, color = Color(0xFF1C1C1E))
    val headlineSmall  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 24.sp, color = Color(0xFF1C1C1E))
    val titleLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 22.sp, color = Color(0xFF1C1C1E))
    val titleMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 16.sp, color = Color(0xFF1C1C1E))
    val titleSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp, color = Color(0xFF1C1C1E))
    val bodyLarge      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Color(0xFF1C1C1E))
    val bodyMedium     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color(0xFF1C1C1E))
    val bodySmall      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color(0xFF1C1C1E))
    val labelLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp, color = Color(0xFF1C1C1E))
    val labelMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color(0xFF1C1C1E))
    val labelSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = Color(0xFF1C1C1E))
}

// ── FUNCIÓN DE TEMA ───────────────────────────────────────────────────────────

fun updateGlassTheme(themeName: String) {
    val color = when (themeName) {
        "NebulaPurple" -> GlassThemes.NebulaPurple
        "AuroraGreen"  -> GlassThemes.AuroraGreen
        "SolarOrange"  -> GlassThemes.SolarOrange
        "CrimsonVoid"  -> GlassThemes.CrimsonVoid
        else           -> GlassThemes.DeepSeaBlue
    }
    TauAccent      = color
    TauAccentGlow  = color.copy(alpha = 0.14f)
    TauAccentLight = color.copy(alpha = 0.18f)
    TauBg          = Color(0xFFF0F2F7)
    TauText1       = Color(0xFF1C1C1E)
    TauText2       = Color(0xFF3C3C43)
    TauText3       = Color(0xFF8E8E93)
    TauSurface1    = Color(0xFFFBFCFF)
    TauSurface2    = Color(0xFFEEF2FA)
    TauSurface3    = Color(0xFFDDE3EF)
    TauSeparator   = Color(0xFFE2E6F0)
}

// ── FONDO GRADIENTE OPTIMIZADO ────────────────────────────────────────────────
// Gradiente lineal diagonal conservado pero con alpha reducido para no saturar.
// Un solo Box con Brush.linearGradient: cero capas extra, cero blur, cero RAM.

@Composable
fun GlassBackground(accentColor: Color = TauAccent) {
    val diagonalGradient = Brush.linearGradient(
        0.0f to accentColor.copy(alpha = 0.13f),
        0.45f to Color(0xFFF0F2F7),
        0.55f to Color(0xFFF0F2F7),
        1.0f to accentColor.copy(alpha = 0.13f),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    Box(Modifier.fillMaxSize().background(diagonalGradient))
}

// ── CARD SÓLIDA (reemplaza GlassCard) ────────────────────────────────────────
// Sin blur, sin capas radiales, sin gradiente de textura.
// Solo: sombra suave + fondo blanco + borde sutil. Rendimiento máximo.

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    opacity: Float = 1.0f,       // Parámetro conservado para compatibilidad
    blur: Float = 0f,            // Ya no se aplica blur
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation    = 2.dp,
                shape        = shape,
                spotColor    = Color(0x14000000),
                ambientColor = Color(0x0A000000)
            )
            .clip(shape)
            .background(TauSurface1)
            .border(1.dp, TauSeparator, shape)
    ) {
        Column(Modifier.padding(20.dp)) {
            content()
        }
    }
}

// ── BOTÓN PRIMARIO ────────────────────────────────────────────────────────────

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TauAccent,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick         = onClick,
        modifier        = modifier.height(52.dp),
        shape           = CircleShape,
        color           = containerColor,
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

// ── BOTÓN SECUNDARIO OUTLINE ──────────────────────────────────────────────────

@Composable
fun GlassOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = TauAccent,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.5.dp, accentColor.copy(alpha = 0.35f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

// ── TEXT FIELD ────────────────────────────────────────────────────────────────

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
        Text(
            label,
            style    = DoeyTypography.labelMedium,
            color    = TauText2,
            modifier = Modifier.padding(start = 4.dp)
        )
        TextField(
            value           = value,
            onValueChange   = onValueChange,
            placeholder     = {
                Text(placeholder, style = DoeyTypography.bodyMedium, color = TauText3)
            },
            modifier        = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TauSurface2)
                .border(1.dp, TauSeparator, RoundedCornerShape(14.dp)),
            colors          = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = TauText1,
                unfocusedTextColor      = TauText1,
            ),
            visualTransformation = visualTransformation,
            trailingIcon         = trailingIcon
        )
    }
}

// ── SETTINGS SECTION ──────────────────────────────────────────────────────────

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
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                style         = DoeyTypography.labelSmall,
                color         = TauText3,
                letterSpacing = 1.2.sp
            )
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

// ── SWITCH ROW ────────────────────────────────────────────────────────────────

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
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (checked) TauAccent.copy(alpha = 0.12f) else TauSurface3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint     = if (checked) TauAccent else TauText3,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = DoeyTypography.bodyMedium,  color = TauText1)
            Text(subtitle, style = DoeyTypography.labelSmall,  color = TauText3)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = TauAccent,
                uncheckedThumbColor  = TauText3,
                uncheckedTrackColor  = TauSurface3,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ── DRAWER ────────────────────────────────────────────────────────────────────

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
            .background(
                if (isSelected) TauAccent.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .border(
                width  = if (isSelected) 1.dp else 0.dp,
                color  = if (isSelected) TauAccent.copy(alpha = 0.20f) else Color.Transparent,
                shape  = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint     = if (isSelected) TauAccent else TauText2,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            color = if (isSelected) TauAccent else TauText1,
            style = if (isSelected) DoeyTypography.titleMedium
                    else DoeyTypography.bodyLarge
        )
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text          = title.uppercase(),
        style         = DoeyTypography.labelSmall,
        color         = TauText3,
        modifier      = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
        letterSpacing = 1.2.sp
    )
}

// ── FIELD COLORS ──────────────────────────────────────────────────────────────

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = TauAccent,
    unfocusedBorderColor      = TauSeparator,
    focusedTextColor          = TauText1,
    unfocusedTextColor        = TauText1,
    focusedLabelColor         = TauAccent,
    unfocusedLabelColor       = TauText3,
    cursorColor               = TauAccent,
    focusedPlaceholderColor   = TauText3,
    unfocusedPlaceholderColor = TauText3,
    focusedContainerColor     = TauSurface2,
    unfocusedContainerColor   = TauSurface2
)
