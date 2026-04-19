package com.doey.DELTA.core

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Proveedor centralizado del icono de Doey
 * Usa el sistema de iconos personalizados lineales minimalistas.
 */

@Composable
fun DoeyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified
) {
    // Usar el icono SmartToy personalizado como identidad de marca
    Icon(
        imageVector = CustomIcons.SmartToy,
        contentDescription = "Doey",
        modifier = modifier.size(size),
        tint = tint
    )
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
