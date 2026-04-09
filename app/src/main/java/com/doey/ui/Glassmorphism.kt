package com.doey.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── PALETA DE COLORES "CHIDA" (Glass Themes) ──────────────────────────────────
// Nombres inspirados en elementos naturales y futuristas

object GlassThemes {
    val NebulaPurple = Color(0xFF8E24AA)
    val AuroraGreen  = Color(0xFF00C853)
    val SolarOrange  = Color(0xFFFF6D00)
    val DeepSeaBlue  = Color(0xFF0091EA)
    val CrimsonVoid  = Color(0xFFD50000)
    val CyberWhite   = Color(0xFFF5F5F5)
    
    // Colores de fondo base (Oscuro profundo para que el cristal resalte)
    val AbyssBlack   = Color(0xFF050505)
    val GlassSurface = Color(0x1AFFFFFF) // Blanco muy traslúcido
    val GlassBorder  = Color(0x33FFFFFF) // Borde sutil
}

// Alias para compatibilidad con el código existente pero con el nuevo estilo
val TauBg       = GlassThemes.AbyssBlack
val TauSurface1 = Color(0x0DFFFFFF)
val TauSurface2 = Color(0x1AFFFFFF)
val TauSurface3 = Color(0x26FFFFFF)
val TauText1    = Color(0xFFFFFFFF)
val TauText2    = Color(0xB3FFFFFF)
val TauText3    = Color(0x80FFFFFF)
val TauAccent   = GlassThemes.DeepSeaBlue
val TauAccentLight = Color(0xFF40C4FF)
val TauAccentGlow  = Color(0x4D0091EA)
val TauSeparator   = Color(0x1AFFFFFF)
val TauGreen       = GlassThemes.AuroraGreen
val TauRed         = GlassThemes.CrimsonVoid
val TauBlue        = GlassThemes.DeepSeaBlue
val TauOrange      = GlassThemes.SolarOrange

// ── COMPONENTES MAESTROS GLASSMORPHISM ────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column { content() }
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
        shape = RoundedCornerShape(16.dp),
        color = containerColor.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, containerColor.copy(alpha = 0.4f))
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
        Text(label, color = TauText2, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TauText3) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
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
            Text(title.uppercase(), color = TauText2, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
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
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) TauAccent else TauText3, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TauText1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TauText3, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TauAccent,
                uncheckedThumbColor = TauText3,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TauText3,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        letterSpacing = 1.5.sp
    )
}

@Composable
fun DrawerSubHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TauAccentLight.copy(alpha = 0.6f), 
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp)
    )
}

// ── EFECTO DE FONDO DINÁMICO ──────────────────────────────────────────────────

@Composable
fun GlassBackground(accentColor: Color = TauAccent) {
    Box(modifier = Modifier.fillMaxSize().background(GlassThemes.AbyssBlack)) {
        // Orbe de luz traslúcido 1
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .blur(100.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
        )
        // Orbe de luz traslúcido 2
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .blur(80.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape)
        )
    }
}
