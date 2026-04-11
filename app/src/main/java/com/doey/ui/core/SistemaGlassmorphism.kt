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
import androidx.compose.ui.draw.blur
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

// ── PALETA DE COLORES ─────────────────────────────────────────────────────────

object GlassThemes {
    val NebulaPurple = Color(0xFF9C27B0)
    val AuroraGreen  = Color(0xFF4CAF50)
    val SolarOrange  = Color(0xFFFF9800)
    val DeepSeaBlue  = Color(0xFF2196F3)
    val CrimsonVoid  = Color(0xFFF44336)
    val CyberWhite   = Color(0xFFFFFFFF)
    val SoftGray     = Color(0xFFF8F9FA)
}

// ── VARIABLES DE TEMA GLOBAL ──────────────────────────────────────────────────

var TauBg          by mutableStateOf(Color(0xFFF4F6FB))
var TauAccent      by mutableStateOf(GlassThemes.DeepSeaBlue)    // #2196F3
var TauAccentGlow  by mutableStateOf(Color(0x332196F3))
var TauAccentLight by mutableStateOf(Color(0xFFBBDEFB))

var TauText1    by mutableStateOf(Color(0xFF1A1A1A))
var TauText2    by mutableStateOf(Color(0xFF4A4A4A))
var TauText3    by mutableStateOf(Color(0xFF7A7A7A))

// Glassmorphism surfaces
// Blanco 55% opacidad = 0x8CFFFFFF
var TauSurface1  by mutableStateOf(Color(0x8CFFFFFF))   // card bg rgba(255,255,255,0.55)
var TauSurface2  by mutableStateOf(Color(0xAAFFFFFF))   // hover/activo
var TauSurface3  by mutableStateOf(Color(0x4D9E9E9E))   // placeholder / disabled
var TauSeparator by mutableStateOf(Color(0xD9FFFFFF))   // border rgba(255,255,255,0.85)

val TauGreen  = GlassThemes.AuroraGreen   // #4CAF50
val TauRed    = GlassThemes.CrimsonVoid   // #F44336
val TauBlue   = GlassThemes.DeepSeaBlue  // #2196F3
val TauOrange = GlassThemes.SolarOrange  // #FF9800
val TauPurple = GlassThemes.NebulaPurple // #9C27B0

// Glass params
var GlassOpacity by mutableStateOf(0.55f)
var GlassBlur    by mutableStateOf(20f)

// ── TIPOGRAFÍA: PRODUCT SANS (vía Downloadable Fonts) / POPPINS FALLBACK ─────
//
// Product Sans no se distribuye como TTF libre. Usamos Poppins como fuente
// instalada en el APK, que es visualmente muy cercana, mientras Android
// gestiona la descarga de "Google Sans" en dispositivos con GMS.
// Para cambiar al font descargado: reemplazar R.font.poppins_* por
// R.font.product_sans_* una vez que la fuente esté disponible en el device.

val ProductSans = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold,    FontWeight.Bold),
    Font(R.font.poppins_bold,    FontWeight.SemiBold),
)

// Alias para compatibilidad con código existente
val Poppins = ProductSans

object DoeyTypography {
    // Títulos — Bold
    val displayLarge   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 57.sp, color = Color(0xFF1A1A1A))
    val displayMedium  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 45.sp, color = Color(0xFF1A1A1A))
    val displaySmall   = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 36.sp, color = Color(0xFF1A1A1A))
    val headlineLarge  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 32.sp, color = Color(0xFF1A1A1A))
    val headlineMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 28.sp, color = Color(0xFF1A1A1A))
    val headlineSmall  = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 24.sp, color = Color(0xFF1A1A1A))
    val titleLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 22.sp, color = Color(0xFF1A1A1A))
    val titleMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 16.sp, color = Color(0xFF1A1A1A))
    val titleSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp, color = Color(0xFF1A1A1A))
    // Cuerpo — Regular
    val bodyLarge      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Color(0xFF1A1A1A))
    val bodyMedium     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color(0xFF1A1A1A))
    val bodySmall      = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color(0xFF1A1A1A))
    val labelLarge     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold,   fontSize = 14.sp, color = Color(0xFF1A1A1A))
    val labelMedium    = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color(0xFF1A1A1A))
    val labelSmall     = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = Color(0xFF1A1A1A))
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
    TauAccentGlow  = color.copy(alpha = 0.20f)
    TauAccentLight = color.copy(alpha = 0.40f)
    TauBg          = Color(0xFFF4F6FB)
    TauText1       = Color(0xFF1A1A1A)
    TauText2       = Color(0xFF4A4A4A)
    TauText3       = Color(0xFF7A7A7A)
    TauSurface1    = Color(0x8CFFFFFF)
    TauSeparator   = Color(0xD9FFFFFF)
}

// ── FONDO CON ORBS (AMBIENT BACKGROUND) ──────────────────────────────────────
// Implementa las "esferas de color" de la spec con blur(60dp) real.

@Composable
fun GlassBackground(accentColor: Color = TauAccent) {
    // Gradiente diagonal: color del tema en esquinas (sup-izq e inf-der), blanco en el centro
    val diagonalGradient = Brush.linearGradient(
        0.0f to accentColor.copy(alpha = 0.15f),
        0.5f to Color.White,
        1.0f to accentColor.copy(alpha = 0.15f),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    
    Box(Modifier.fillMaxSize().background(diagonalGradient))
}

// ── GLASS CARD — IMPLEMENTACIÓN DE CAPAS ─────────────────────────────────────
// Capa 1: sombra de elevación  0 8px 32px rgba(0,0,0,0.08)
// Capa 2: fondo blanco 55% opacidad + blur(20dp)
// Capa 3: borde de luz 1.5dp rgba(255,255,255,0.85)
// Capa 4: contenido

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    opacity: Float = 0.70f, // Un poco más opaco para mejor legibilidad
    blur: Float = 25f,      // Un poco más de blur para suavizar el fondo
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation     = 12.dp,
                shape         = shape,
                spotColor     = Color(0x1A000000),   // rgba(0,0,0,0.10)
                ambientColor  = Color(0x1A000000)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = opacity),
                        Color.White.copy(alpha = opacity * 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.3f)
                    )
                ),
                shape = shape
            )
    ) {
        // Contenido (encima, sin blur)
        Column(Modifier.padding(20.dp)) { // Más padding para airear el diseño
            content()
        }
    }
}

// ── GLASS BUTTON (PILL) ───────────────────────────────────────────────────────

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TauAccent,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick      = onClick,
        modifier     = modifier.height(52.dp),
        shape        = CircleShape,
        color        = containerColor,
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.padding(horizontal = 28.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

// ── PILL BUTTON SECUNDARIO (OUTLINE GLASS) ────────────────────────────────────

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
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.5.dp, accentColor.copy(alpha = 0.40f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x8CFFFFFF))
                .border(1.5.dp, Color(0xD9FFFFFF), RoundedCornerShape(24.dp)),
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(start = 4.dp)
        ) {
            Icon(icon, null, tint = TauAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                style         = DoeyTypography.labelSmall,
                color         = TauText2,
                letterSpacing = 1.5.sp
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
                .size(38.dp)
                .clip(CircleShape)
                .background(if (checked) TauAccent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.03f)),
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
                checkedThumbColor   = Color.White,
                checkedTrackColor   = TauAccent,
                uncheckedThumbColor = TauText3,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.06f),
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
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) TauAccent.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .border(
                width  = if (isSelected) 1.5.dp else 0.dp,
                color  = if (isSelected) TauAccent.copy(alpha = 0.25f) else Color.Transparent,
                shape  = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint     = if (isSelected) TauAccent else TauText2,
            modifier = Modifier.size(22.dp)
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
        modifier      = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        letterSpacing = 1.5.sp
    )
}

// ── FIELD COLORS ──────────────────────────────────────────────────────────────

@Composable
fun doeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor        = TauAccent,
    unfocusedBorderColor      = Color(0xD9FFFFFF),
    focusedTextColor          = TauText1,
    unfocusedTextColor        = TauText1,
    focusedLabelColor         = TauAccent,
    unfocusedLabelColor       = TauText3,
    cursorColor               = TauAccent,
    focusedPlaceholderColor   = TauText3,
    unfocusedPlaceholderColor = TauText3,
    focusedContainerColor     = Color(0x8CFFFFFF),
    unfocusedContainerColor   = Color(0x8CFFFFFF)
)
