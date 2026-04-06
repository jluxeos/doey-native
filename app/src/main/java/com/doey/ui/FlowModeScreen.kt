package com.doey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FlowModeScreen - Pantalla simplificada del Modo Flujo
 * Nota: Usa FlowModeAdvancedScreen como pantalla principal
 * Este archivo se mantiene para compatibilidad
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowModeScreen(vm: MainViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Surface0Light)
    ) {
        TopAppBar(
            title = {
                Text("Modo Flujo", color = Label1Light, fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light)
        )

        Box(
            Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = Label3Light,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Usa FlowModeAdvancedScreen",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Label1Light
                )
            }
        }
    }
}
