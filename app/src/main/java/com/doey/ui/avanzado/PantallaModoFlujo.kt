package com.doey.ui.avanzado

// FlowModeScreen eliminado.
// El Modo Flujo ahora vive como carrusel inline en HomeScreen.
// Este archivo se mantiene vacío para no romper la ruta en DoeyApp.kt.

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlowModeScreen(vm: MainViewModel) {
    // Esta pantalla ya no se utiliza.
    // El Modo Flujo se activa desde el ícono del árbol en la barra superior de Inicio.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌿", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "El Modo Flujo ahora está en Inicio",
                fontWeight = FontWeight.Bold,
                color = PurpleDark,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Toca el ícono  ⚡  en la barra superior\npara activar el carrusel de acciones.",
                color = Label2Light,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
