package com.doey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val settings = vm.getSettings()
    val scope = rememberCoroutineScope()

    var personalMemory by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        personalMemory = settings.getPersonalMemory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memorias Personales", color = Label1Light, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            settings.setPersonalMemory(personalMemory)
                            isEditing = false
                        }
                    }) {
                        Icon(Icons.Default.Save, "Guardar", tint = Purple)
                    }
                }
            )
        },
        containerColor = Surface0Light
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Estas son las cosas que Doey sabe sobre ti. Puedes editarlas manualmente o dejar que Doey aprenda de tus conversaciones.",
                color = Label2Light,
                fontSize = 14.sp
            )

            // ── Editor de Memoria ─────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Surface1Light,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, null, tint = Purple)
                        Spacer(Modifier.width(8.dp))
                        Text("Archivo de Contexto (MEMORY.md)", fontWeight = FontWeight.Bold, color = Label1Light)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = personalMemory,
                        onValueChange = { personalMemory = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("- [nombre] Me llamo Juan\n- [casa] Vivo en Calle Mayor 123...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // ── Consejos ──────────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Surface2Light,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("💡 Consejo:", fontWeight = FontWeight.Bold, color = Purple, fontSize = 12.sp)
                    Text(
                        "Usa etiquetas como [casa], [familia] o [trabajo] para que Doey sepa clasificar mejor la información.",
                        color = Label2Light,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}