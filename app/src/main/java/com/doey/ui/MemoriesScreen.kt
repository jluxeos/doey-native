package com.doey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class MemoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val variable: String,
    val definition: String
)

data class MemoryCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val variableHint: String,
    val definitionHint: String,
    val variableOptions: List<String> = emptyList(),
    val definitionOptions: List<String> = emptyList()
)

val MEMORY_CATEGORIES = listOf(
    MemoryCategory(
        id = "contacto",
        label = "Contactos",
        icon = Icons.Default.Person,
        color = Color(0xFF1565C0),
        variableHint = "Nombre del contacto",
        definitionHint = "Relación o descripción",
        variableOptions = emptyList(),
        definitionOptions = listOf(
            "esposa", "esposo", "mamá", "papá", "hermano", "hermana",
            "hijo", "hija", "amigo", "amiga", "jefe", "compañero de trabajo", "médico"
        )
    ),
    MemoryCategory(
        id = "lugar",
        label = "Lugares",
        icon = Icons.Default.Place,
        color = Color(0xFF2E7D32),
        variableHint = "Nombre del lugar",
        definitionHint = "Para qué sirve / qué es",
        variableOptions = emptyList(),
        definitionOptions = listOf(
            "casa", "trabajo", "gimnasio", "escuela", "supermercado",
            "restaurante favorito", "médico", "farmacia", "banco"
        )
    ),
    MemoryCategory(
        id = "app_favorita",
        label = "Apps favoritas",
        icon = Icons.Default.Apps,
        color = Color(0xFF6A1B9A),
        variableHint = "Nombre de la app",
        definitionHint = "Para qué la uso",
        variableOptions = listOf(
            "WhatsApp", "Instagram", "TikTok", "YouTube", "Spotify",
            "Netflix", "Gmail", "Maps", "Chrome", "Twitter/X", "Facebook", "Telegram"
        ),
        definitionOptions = listOf(
            "mensajes", "redes sociales", "música", "videos",
            "correo", "navegación", "noticias", "trabajo", "entretenimiento"
        )
    ),
    MemoryCategory(
        id = "utilidad_favorita",
        label = "Utilidades favoritas",
        icon = Icons.Default.Build,
        color = Color(0xFFE65100),
        variableHint = "Nombre de la herramienta",
        definitionHint = "Para qué la uso",
        variableOptions = listOf(
            "Calculadora", "Notas", "Calendario", "Reloj", "Cámara",
            "Grabadora", "Traductor", "Escáner", "Linterna"
        ),
        definitionOptions = listOf("trabajo", "organización", "recordatorios", "productividad")
    ),
    MemoryCategory(
        id = "comida_favorita",
        label = "Comidas favoritas",
        icon = Icons.Default.Restaurant,
        color = Color(0xFFC62828),
        variableHint = "Nombre del platillo o comida",
        definitionHint = "Descripción o contexto",
        variableOptions = emptyList(),
        definitionOptions = listOf(
            "desayuno favorito", "comida favorita", "cena favorita",
            "snack favorito", "bebida favorita", "postre favorito", "no me gusta", "alergia"
        )
    ),
    MemoryCategory(
        id = "dato_personal",
        label = "Datos personales",
        icon = Icons.Default.Badge,
        color = Color(0xFF00838F),
        variableHint = "Tipo de dato",
        definitionHint = "Valor",
        variableOptions = listOf(
            "nombre", "apodo", "edad", "cumpleaños", "profesión",
            "ciudad", "idioma nativo", "hobby", "deporte favorito", "mascota"
        ),
        definitionOptions = emptyList()
    ),
    MemoryCategory(
        id = "preferencia",
        label = "Preferencias",
        icon = Icons.Default.Tune,
        color = Color(0xFF558B2F),
        variableHint = "Tipo de preferencia",
        definitionHint = "Mi preferencia",
        variableOptions = listOf(
            "música favorita", "género de películas", "color favorito",
            "deporte favorito", "horario preferido", "idioma preferido"
        ),
        definitionOptions = emptyList()
    ),
    MemoryCategory(
        id = "otro",
        label = "Otro",
        icon = Icons.Default.MoreHoriz,
        color = Color(0xFF546E7A),
        variableHint = "Nombre / clave",
        definitionHint = "Valor / descripción",
        variableOptions = emptyList(),
        definitionOptions = emptyList()
    )
)

fun List<MemoryEntry>.toJson(): String {
    val arr = JSONArray()
    forEach { e ->
        arr.put(JSONObject().apply {
            put("id", e.id)
            put("category", e.category)
            put("variable", e.variable)
            put("definition", e.definition)
        })
    }
    return arr.toString()
}

fun parseMemoryEntries(raw: String): List<MemoryEntry> {
    if (raw.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw.trim())
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            MemoryEntry(
                id         = obj.optString("id", java.util.UUID.randomUUID().toString()),
                category   = obj.optString("category", "otro"),
                variable   = obj.optString("variable", ""),
                definition = obj.optString("definition", "")
            )
        }.filter { it.variable.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(vm: MainViewModel) {
    val settings = vm.getSettings()
    val scope    = rememberCoroutineScope()
    var entries       by remember { mutableStateOf(listOf<MemoryEntry>()) }
    var expandedCats  by remember { mutableStateOf(setOf<String>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry  by remember { mutableStateOf<MemoryEntry?>(null) }
    var selectedCatId by remember { mutableStateOf("contacto") }
    var isSaved       by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        entries = parseMemoryEntries(settings.getPersonalMemory())
    }

    LaunchedEffect(entries) {
        settings.setPersonalMemory(entries.toJson())
        isSaved = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Memorias", color = TauText1, fontWeight = FontWeight.Bold)
                        Text("${entries.size} datos guardados", color = TauText3, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1),
                actions = {
                    if (isSaved) {
                        Icon(Icons.Default.CheckCircle, null, tint = TauGreen, modifier = Modifier.padding(end = 8.dp).size(20.dp))
                    }
                    val context = LocalContext.current
                    IconButton(onClick = { 
                        val json = entries.toJson()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "Exportar Memorias"))
                    }) {
                        Icon(Icons.Default.Share, null, tint = TauText3)
                    }
                }
            )
        },
        containerColor = TauBg,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = TauAccent, contentColor = Color.White) {
                Icon(Icons.Default.Add, "Añadir")
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MEMORY_CATEGORIES.forEach { cat ->
                val catEntries = entries.filter { it.category == cat.id }
                if (catEntries.isNotEmpty()) {
                    item {
                        MemoryCategoryCard(cat, catEntries, expandedCats.contains(cat.id), onToggle = {
                            expandedCats = if (expandedCats.contains(cat.id)) expandedCats - cat.id else expandedCats + cat.id
                        }, onEdit = { editingEntry = it }, onDelete = { entries = entries - it })
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryCategoryCard(cat: MemoryCategory, entries: List<MemoryEntry>, isExpanded: Boolean, onToggle: () -> Unit, onEdit: (MemoryEntry) -> Unit, onDelete: (MemoryEntry) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = TauSurface1, modifier = Modifier.fillMaxWidth().clickable { onToggle() }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = cat.color.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(cat.icon, null, tint = cat.color) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(cat.label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${entries.size} entradas", color = TauText3, fontSize = 12.sp)
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TauText3)
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    entries.forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.variable, color = TauAccentLight, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(entry.definition, color = TauText2, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Default.Edit, null, tint = TauText3, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, null, tint = TauRed, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}
