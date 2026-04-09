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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.agent.DoeyLogger
import com.doey.agent.LogEntry
import com.doey.agent.LogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen() {
    val entries by DoeyLogger.entries.collectAsState()
    val listState = rememberLazyListState()
    var filterType by remember { mutableStateOf<LogType?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }
    var autoScroll by remember { mutableStateOf(true) }

    val filtered = remember(entries, filterType) {
        if (filterType == null) entries else entries.filter { it.type == filterType }
    }

    LaunchedEffect(filtered.size) {
        if (autoScroll && filtered.isNotEmpty()) {
            listState.animateScrollToItem(filtered.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registro de Actividad", color = TauText1, fontWeight = FontWeight.Bold)
                        Text("${filtered.size} entradas", color = TauText3, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1),
                actions = {
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(
                            if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignCenter,
                            contentDescription = "Auto-scroll",
                            tint = if (autoScroll) TauAccent else TauText3
                        )
                    }
                    val context = LocalContext.current
                    IconButton(onClick = { 
                        val logText = entries.joinToString("\n") { 
                            "[${it.type.name}] ${it.formattedTime}: ${it.title} - ${it.detail}" 
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, logText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Exportar Logs"))
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportar", tint = TauText3)
                    }
                    IconButton(onClick = { DoeyLogger.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar", tint = TauText3)
                    }
                }
            )
        },
        containerColor = TauBg
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            FilterChipsRow(
                selected = filterType,
                onSelect = { filterType = if (filterType == it) null else it }
            )

            HorizontalDivider(color = TauSurface2)

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Terminal, null, tint = TauText3,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin registros aún", color = TauText3, fontSize = 14.sp)
                        Text("Los eventos aparecerán aquí cuando uses Doey",
                            color = TauText3, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        val isExpanded = entry.id in expandedIds
                        LogEntryCard(
                            entry = entry,
                            isExpanded = isExpanded,
                            onToggle = {
                                expandedIds = if (isExpanded) {
                                    expandedIds - entry.id
                                } else {
                                    expandedIds + entry.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: LogType?,
    onSelect: (LogType) -> Unit
) {
    val filters = listOf(
        LogType.USER_INPUT to "Usuario",
        LogType.AI_REQUEST to "→ IA",
        LogType.AI_RESPONSE to "← IA",
        LogType.TOOL_CALL to "Tools",
        LogType.TOOL_RESULT to "Resultados",
        LogType.SKILL_LOAD to "Skills",
        LogType.ACCESSIBILITY_SEND to "→ Pantalla",
        LogType.ACCESSIBILITY_RECV to "← Pantalla",
        LogType.ERROR to "Errores"
    )
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(filters) { (type, label) ->
            val isSelected = selected == type
            val chipColor = Color(type.toColor())
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(type) },
                label    = { Text(label, fontSize = 11.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                    selectedLabelColor     = chipColor,
                    selectedLeadingIconColor = chipColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = chipColor,
                    borderColor = TauText3.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val typeColor = Color(entry.typeColor)
    val hasDetail = entry.detail.isNotBlank()

    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = TauSurface1,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasDetail) Modifier.clickable { onToggle() } else Modifier)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = typeColor.copy(alpha = 0.15f),
                    modifier = Modifier.border(0.5.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        entry.typeLabel,
                        color    = typeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.title,
                    color    = TauText1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    entry.formattedTime,
                    color    = TauText3,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (hasDetail) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TauText3,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp)
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded && hasDetail,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = TauSurface2, thickness = 0.5.dp)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1A1A2E).copy(alpha = 0.05f)
                    ) {
                        Text(
                            entry.detail,
                            color      = TauText2,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier   = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            if (!isExpanded && hasDetail) {
                Text(
                    entry.detail,
                    color    = TauText3,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun LogType.toColor(): Long = when (this) {
    LogType.USER_INPUT          -> 0xFF1565C0L
    LogType.AI_REQUEST          -> 0xFF6A1B9AL
    LogType.AI_RESPONSE         -> 0xFF2E7D32L
    LogType.TOOL_CALL           -> 0xFFE65100L
    LogType.TOOL_RESULT         -> 0xFF00838FL
    LogType.SKILL_LOAD          -> 0xFF558B2FL
    LogType.ACCESSIBILITY_SEND  -> 0xFFC62828L
    LogType.ACCESSIBILITY_RECV  -> 0xFF4527A0L
    LogType.PIPELINE_STATE      -> 0xFF37474FL
    LogType.ERROR               -> 0xFFB71C1CL
    LogType.INFO                -> 0xFF546E7AL
}
