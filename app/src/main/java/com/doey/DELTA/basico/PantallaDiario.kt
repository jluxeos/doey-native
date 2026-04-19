package com.doey.DELTA.basico

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.doey.DELTA.core.*
import com.doey.MainViewModel
import com.doey.DELTA.comun.ItemCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun JournalScreen(vm: MainViewModel) {
    val ctx   = LocalContext.current
    val prefs = ctx.getSharedPreferences("doey_journal", 0)
    var entries   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var filterCat by remember { mutableStateOf("") }

    fun refresh() {
        val arr = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
        entries = (0 until arr.length()).map { arr.getJSONObject(it) }.reversed()
    }
    LaunchedEffect(Unit) { refresh() }

    val filtered = if (filterCat.isBlank()) entries
    else entries.filter { it.optString("category").equals(filterCat, ignoreCase = true) }

    Column(Modifier.fillMaxSize().background(TauBg)) {
        TopAppBar(
            title   = { Text("Diario", color = TauText1, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { refresh() }) { Icon(CustomIcons.Refresh, "Actualizar", tint = TauText3) } },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        if (entries.isEmpty()) {
            DiarioEmptyState()
        } else {
            val cats = entries.map { it.optString("category") }.distinct().filter { it.isNotBlank() }
            if (cats.isNotEmpty()) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(selected = filterCat.isBlank(), onClick = { filterCat = "" }, label = { Text("Todas") })
                    }
                    items(cats) { cat ->
                        FilterChip(
                            selected = filterCat == cat,
                            onClick  = { filterCat = if (filterCat == cat) "" else cat },
                            label    = { Text(cat) }
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { entry ->
                    JournalCard(entry) {
                        val id  = entry.getString("id")
                        val arr = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
                        val nuevo = JSONArray()
                        for (i in 0 until arr.length()) {
                            if (arr.getJSONObject(i).getString("id") != id) nuevo.put(arr.get(i))
                        }
                        prefs.edit().putString("entries", nuevo.toString()).apply()
                        refresh()
                    }
                }
            }
        }
    }
}

@Composable
private fun DiarioEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(CustomIcons.Book, null, modifier = Modifier.size(64.dp), tint = TauSurface3)
            Spacer(Modifier.height(16.dp))
            Text("Tu diario está vacío", color = TauText3, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

@Composable
private fun JournalCard(e: JSONObject, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ItemCard {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.optString("title"), color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val cat = e.optString("category")
                        if (cat.isNotBlank()) {
                            Text(cat, color = TauAccent, fontSize = 11.sp)
                        }
                        Text(fmtDiarioMs(e.optLong("createdAt")), color = TauText3, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(CustomIcons.Delete, null, tint = TauRed, modifier = Modifier.size(18.dp))
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(e.optString("details"), color = TauText2, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

private fun fmtDiarioMs(ms: Long) =
    if (ms == 0L) "?"
    else SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()).format(Date(ms))
