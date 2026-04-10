package com.doey.ui.comun

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
import com.doey.ui.core.*
import com.doey.MainViewModel

// ── Modelo de datos ───────────────────────────────────────────────────────────

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

// ── Serialización ─────────────────────────────────────────────────────────────

fun List<MemoryEntry>.toMarkdown(): String {
    if (isEmpty()) return ""
    return joinToString("\n") { entry ->
        "- [${entry.category}] ${entry.variable}: ${entry.definition}"
    }
}

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
        // Fallback: parsear formato Markdown legacy "- [cat] var: def"
        raw.lines()
            .filter { it.trimStart().startsWith("-") }
            .mapNotNull { line ->
                val trimmed = line.trimStart().removePrefix("-").trim()
                val catMatch = Regex("^\\[([^]]+)]\\s*(.+)").find(trimmed)
                if (catMatch != null) {
                    val cat  = catMatch.groupValues[1]
                    val rest = catMatch.groupValues[2]
                    val colonIdx = rest.indexOf(':')
                    if (colonIdx != -1) {
                        MemoryEntry(
                            category   = cat,
                            variable   = rest.substring(0, colonIdx).trim(),
                            definition = rest.substring(colonIdx + 1).trim()
                        )
                    } else {
                        MemoryEntry(category = cat, variable = rest.trim(), definition = "")
                    }
                } else if (trimmed.isNotBlank()) {
                    MemoryEntry(category = "otro", variable = trimmed, definition = "")
                } else null
            }
    }
}

// ── Pantalla principal ────────────────────────────────────────────────────────

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

    // Cargar desde settings al iniciar
    LaunchedEffect(Unit) {
        val raw = settings.getPersonalMemory()
        entries = parseMemoryEntries(raw)
    }

    // Guardar automáticamente cuando cambian las entradas
    LaunchedEffect(entries) {
        scope.launch {
            settings.setPersonalMemory(entries.toJson())
            isSaved = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Memorias", color = Label1Light, fontWeight = FontWeight.Bold)
                        Text("${entries.size} datos guardados", color = Label3Light, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1Light),
                actions = {
                    if (isSaved) {
                        Icon(
                            Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32),
                            modifier = Modifier.padding(end = 8.dp).size(20.dp)
                        )
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
                        Icon(Icons.Default.Share, "Compartir", tint = Label3Light)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Agregar", tint = Purple)
                    }
                }
            )
        },
        containerColor = Surface0Light,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddDialog = true },
                containerColor = Purple,
                contentColor   = OnPurple
            ) {
                Icon(Icons.Default.Add, "Agregar memoria")
            }
        }
    ) { pad ->
        if (entries.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.Psychology, null, tint = Label3Light,
                        modifier = Modifier.size(64.dp))
                    Text("Sin memorias guardadas", fontWeight = FontWeight.Bold,
                        color = Label1Light, fontSize = 18.sp)
                    Text(
                        "Toca el botón + para agregar información sobre ti. Doey la usará para conocerte mejor y ayudarte de forma personalizada.",
                        color = Label3Light, fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors  = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Icon(Icons.Default.Add, null, tint = OnPurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar mi primera memoria", color = OnPurple)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(pad),
                contentPadding  = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = entries.groupBy { it.category }
                val knownCats = MEMORY_CATEGORIES.filter { grouped.containsKey(it.id) }
                val unknownCats = grouped.keys
                    .filter { key -> MEMORY_CATEGORIES.none { it.id == key } }
                    .map { key ->
                        MemoryCategory(
                            id = key, label = key,
                            icon = Icons.Default.MoreHoriz,
                            color = Color(0xFF546E7A),
                            variableHint = "", definitionHint = ""
                        )
                    }
                val allCats = knownCats + unknownCats

                items(allCats, key = { it.id }) { cat ->
                    val catEntries = grouped[cat.id] ?: return@items
                    val isExpanded = cat.id in expandedCats

                    CategorySection(
                        category   = cat,
                        entries    = catEntries,
                        isExpanded = isExpanded,
                        onToggle   = {
                            expandedCats = if (isExpanded) expandedCats - cat.id
                            else expandedCats + cat.id
                        },
                        onEdit     = { entry ->
                            editingEntry  = entry
                            selectedCatId = entry.category
                            showAddDialog = true
                        },
                        onDelete   = { entry ->
                            entries = entries.filter { it.id != entry.id }
                        }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddEditMemoryDialog(
            initial      = editingEntry,
            initialCatId = selectedCatId,
            onDismiss    = {
                showAddDialog = false
                editingEntry  = null
            },
            onSave = { newEntry ->
                entries = if (editingEntry != null) {
                    entries.map { if (it.id == editingEntry!!.id) newEntry else it }
                } else {
                    entries + newEntry
                }
                showAddDialog = false
                editingEntry  = null
                isSaved = false
            }
        )
    }
}

// ── Sección de categoría ──────────────────────────────────────────────────────

@Composable
private fun CategorySection(
    category: MemoryCategory,
    entries: List<MemoryEntry>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEdit: (MemoryEntry) -> Unit,
    onDelete: (MemoryEntry) -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = Surface1Light,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = category.color.copy(alpha = 0.15f)
                ) {
                    Icon(
                        category.icon, null,
                        tint     = category.color,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.label, fontWeight = FontWeight.Bold,
                        color = Label1Light, fontSize = 14.sp)
                    Text(
                        "${entries.size} ${if (entries.size == 1) "elemento" else "elementos"}",
                        color = Label3Light, fontSize = 11.sp
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = Label3Light
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Surface2Light)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text("Variable", color = Label3Light, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Definición", color = Label3Light, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                        Spacer(Modifier.width(56.dp))
                    }
                    entries.forEach { entry ->
                        MemoryEntryRow(
                            entry    = entry,
                            catColor = category.color,
                            onEdit   = { onEdit(entry) },
                            onDelete = { onDelete(entry) }
                        )
                    }
                }
            }
        }
    }
}

// ── Fila de entrada ───────────────────────────────────────────────────────────

@Composable
private fun MemoryEntryRow(
    entry: MemoryEntry,
    catColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = catColor.copy(alpha = 0.05f),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, catColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.variable,
                color      = Label1Light,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f)
            )
            Text(
                entry.definition,
                color    = Label2Light,
                fontSize = 12.sp,
                modifier = Modifier.weight(1.5f)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Editar", tint = catColor,
                    modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFC62828),
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Diálogo de agregar/editar ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditMemoryDialog(
    initial: MemoryEntry?,
    initialCatId: String,
    onDismiss: () -> Unit,
    onSave: (MemoryEntry) -> Unit
) {
    val isEditing = initial != null
    var selectedCat by remember {
        mutableStateOf(
            MEMORY_CATEGORIES.find { it.id == (initial?.category ?: initialCatId) }
                ?: MEMORY_CATEGORIES.first()
        )
    }
    var variable    by remember { mutableStateOf(initial?.variable ?: "") }
    var definition  by remember { mutableStateOf(initial?.definition ?: "") }
    var catExpanded by remember { mutableStateOf(false) }
    var varExpanded by remember { mutableStateOf(false) }
    var defExpanded by remember { mutableStateOf(false) }
    var varSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Cargar sugerencias según la categoría
    LaunchedEffect(selectedCat.id) {
        varSuggestions = when (selectedCat.id) {
            "contacto" -> ContactsSuggestionsHelper.getContactsSuggestions().map { it.name }
            "lugar" -> ContactsSuggestionsHelper.getLocationsSuggestions().map { it.name }
            "app_favorita" -> ContactsSuggestionsHelper.getAppsSuggestions()
            else -> emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = Surface0Light,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (isEditing) "Editar memoria" else "Nueva memoria",
                    fontWeight = FontWeight.Bold,
                    color      = Label1Light,
                    fontSize   = 18.sp
                )

                // Paso 1: Categoría
                Text("1. ¿De qué categoría es?",
                    color = Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ExposedDropdownMenuBox(
                    expanded         = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value         = selectedCat.label,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Categoría") },
                        leadingIcon   = {
                            Icon(selectedCat.icon, null, tint = selectedCat.color,
                                modifier = Modifier.size(20.dp))
                        },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        colors        = doeyFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded         = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        MEMORY_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(cat.icon, null, tint = cat.color,
                                            modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(cat.label)
                                    }
                                },
                                onClick = {
                                    selectedCat = cat
                                    catExpanded = false
                                    if (!isEditing) { variable = ""; definition = "" }
                                }
                            )
                        }
                    }
                }

                // Paso 2: Variable
                Text(
                    "2. ${selectedCat.variableHint.ifBlank { "Variable / nombre" }}",
                    color = Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                if (selectedCat.variableOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded         = varExpanded,
                        onExpandedChange = { varExpanded = !varExpanded }
                    ) {
                        OutlinedTextField(
                            value         = variable,
                            onValueChange = { variable = it },
                            label         = { Text(selectedCat.variableHint) },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(varExpanded) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            colors        = doeyFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded         = varExpanded,
                            onDismissRequest = { varExpanded = false }
                        ) {
                            selectedCat.variableOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text    = { Text(opt) },
                                    onClick = { variable = opt; varExpanded = false }
                                )
                            }
                        }
                    }
                } else if (selectedCat.id in listOf("contacto", "lugar", "app_favorita")) {
                    // Autocompletado inteligente para contactos, lugares y apps
                    val filteredSuggestions = when {
                        variable.isBlank() -> emptyList()
                        selectedCat.id == "contacto" -> ContactsSuggestionsHelper.filterContacts(variable, ContactsSuggestionsHelper.getContactsSuggestions()).map { it.name }
                        selectedCat.id == "lugar" -> ContactsSuggestionsHelper.filterLocations(variable, ContactsSuggestionsHelper.getLocationsSuggestions()).map { it.name }
                        selectedCat.id == "app_favorita" -> ContactsSuggestionsHelper.filterApps(variable, varSuggestions)
                        else -> emptyList()
                    }
                    ExposedDropdownMenuBox(
                        expanded         = varExpanded && filteredSuggestions.isNotEmpty(),
                        onExpandedChange = { varExpanded = it && filteredSuggestions.isNotEmpty() }
                    ) {
                        OutlinedTextField(
                            value         = variable,
                            onValueChange = { variable = it; varExpanded = true },
                            label         = { Text(selectedCat.variableHint) },
                            trailingIcon  = if (filteredSuggestions.isNotEmpty()) { { ExposedDropdownMenuDefaults.TrailingIcon(varExpanded) } } else null,
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            colors        = doeyFieldColors()
                        )
                        if (filteredSuggestions.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded         = varExpanded,
                                onDismissRequest = { varExpanded = false }
                            ) {
                                filteredSuggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text    = { Text(suggestion) },
                                        onClick = { variable = suggestion; varExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value         = variable,
                        onValueChange = { variable = it },
                        label         = { Text(selectedCat.variableHint.ifBlank { "Nombre / clave" }) },
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = doeyFieldColors()
                    )
                }

                // Paso 3: Definición
                Text(
                    "3. ${selectedCat.definitionHint.ifBlank { "Definición / valor" }}",
                    color = Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                if (selectedCat.definitionOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded         = defExpanded,
                        onExpandedChange = { defExpanded = !defExpanded }
                    ) {
                        OutlinedTextField(
                            value         = definition,
                            onValueChange = { definition = it },
                            label         = { Text(selectedCat.definitionHint) },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(defExpanded) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            colors        = doeyFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded         = defExpanded,
                            onDismissRequest = { defExpanded = false }
                        ) {
                            selectedCat.definitionOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text    = { Text(opt) },
                                    onClick = { definition = opt; defExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value         = definition,
                        onValueChange = { definition = it },
                        label         = { Text(selectedCat.definitionHint.ifBlank { "Valor / descripción" }) },
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = doeyFieldColors()
                    )
                }

                // Preview
                if (variable.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = selectedCat.color.copy(alpha = 0.08f)
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(selectedCat.icon, null, tint = selectedCat.color,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "- [${selectedCat.id}] $variable${if (definition.isNotBlank()) ": $definition" else ""}",
                                color    = Label2Light,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Botones
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Label2Light)
                    ) { Text("Cancelar") }

                    Button(
                        onClick  = {
                            if (variable.isNotBlank()) {
                                onSave(
                                    MemoryEntry(
                                        id         = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                        category   = selectedCat.id,
                                        variable   = variable.trim(),
                                        definition = definition.trim()
                                    )
                                )
                            }
                        },
                        enabled  = variable.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Icon(Icons.Default.Save, null, tint = OnPurple)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEditing) "Actualizar" else "Guardar", color = OnPurple)
                    }
                }
            }
        }
    }
}


