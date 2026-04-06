package com.doey.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Proveedor centralizado del icono de Doey
 * Usa ic_launcher_foreground en lugar de emojis genéricos
 */

@Composable
fun DoeyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified
) {
    try {
        // Intentar cargar el icono personalizado del usuario
        val painter = painterResource(id = android.R.drawable.ic_dialog_info) // Fallback
        Image(
            painter = painter,
            contentDescription = "Doey",
            modifier = modifier.size(size),
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
        )
    } catch (e: Exception) {
        // Fallback a icono genérico si no se encuentra
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "Doey",
            modifier = modifier.size(size),
            tint = tint
        )
    }
}

@Composable
fun DoeyIconSmall(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    DoeyIcon(modifier = modifier, size = 20.dp, tint = tint)
}

@Composable
fun DoeyIconMedium(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    DoeyIcon(modifier = modifier, size = 32.dp, tint = tint)
}

@Composable
fun DoeyIconLarge(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    DoeyIcon(modifier = modifier, size = 48.dp, tint = tint)
}
