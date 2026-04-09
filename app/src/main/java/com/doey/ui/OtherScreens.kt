package com.doey.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doey.services.DoeyAccessibilityService
import com.doey.services.NotificationAccessManager
import com.doey.services.SchedulerEngine
import com.doey.services.TimerEngine
import com.doey.services.AlarmScheduler
import com.doey.services.ScheduledAlarm
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── ClockScreen (Punto 10: Integración de Reloj, Alarmas, Timers y Agenda) ──────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var schedules by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var timers    by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Simulación de alarmas editables (ya que AlarmScheduler no tiene persistencia visible)
    // En una app real usaríamos una DB o SharedPreferences dedicada
    val alarmPrefs = ctx.getSharedPreferences("doey_alarms_store", 0)
    var alarms by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    fun refresh() {
        val sa = SchedulerEngine.getAllSchedules(ctx)
        schedules = (0 until sa.length()).map { sa.getJSONObject(it) }
        val ta = TimerEngine.getAllTimers(ctx)
        timers = (0 until ta.length()).map { ta.getJSONObject(it) }
        
        val alStr = alarmPrefs.getString("alarms", "[]")
        val alArr = try { JSONArray(alStr) } catch(_: Exception) { JSONArray() }
        alarms = (0 until alArr.length()).map { alArr.getJSONObject(it) }
    }
    
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().background(TauBg)) {
        TopAppBar(
            title   = { Text("Reloj", color = TauText1, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Actualizar", tint = TauText3) } },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TauSurface1,
            contentColor = TauAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TauAccent
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Alarmas", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Timers", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Agenda", fontSize = 12.sp) })
        }

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AlarmList(alarms, onRefresh = { refresh() })
                1 -> TimerList(timers, onRefresh = { refresh() })
                2 -> ScheduleList(schedules, onRefresh = { refresh() })
            }
        }
    }
}

@Composable
private fun AlarmList(alarms: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (alarms.isEmpty()) {
        EmptyState("No hay alarmas activas", Icons.Default.Alarm)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(alarms) { alarm ->
                AlarmRow(alarm) {
                    // Lógica para borrar/editar
                    val id = alarm.getInt("id")
                    AlarmScheduler.cancelAlarm(id)
                    
                    val prefs = ctx.getSharedPreferences("doey_alarms_store", 0)
                    val current = JSONArray(prefs.getString("alarms", "[]"))
                    val next = JSONArray()
                    for (i in 0 until current.length()) {
                        if (current.getJSONObject(i).getInt("id") != id) next.put(current.get(i))
                    }
                    prefs.edit().putString("alarms", next.toString()).apply()
                    onRefresh()
                }
            }
        }
    }
}

@Composable
private fun TimerList(timers: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (timers.isEmpty()) {
        EmptyState("No hay temporizadores", Icons.Default.Timer)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(timers) { t ->
                TimerRow(t) { TimerEngine.removeTimer(ctx, t.getString("id")); onRefresh() }
            }
        }
    }
}

@Composable
private fun ScheduleList(schedules: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (schedules.isEmpty()) {
        EmptyState("Tu agenda está vacía", Icons.Default.EventNote)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(schedules) { s ->
                ScheduleRow(s,
                    onToggle = {
                        s.put("enabled", !s.optBoolean("enabled", true))
                        SchedulerEngine.setSchedule(ctx, s); onRefresh()
                    },
                    onDelete = { SchedulerEngine.removeSchedule(ctx, s.getString("id")); onRefresh() }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(msg: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = TauSurface3)
            Spacer(Modifier.height(16.dp))
            Text(msg, color = TauText3, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AlarmRow(a: JSONObject, onDelete: () -> Unit) {
    val title = a.optString("title", "Alarma")
    val time = a.optString("time", "--:--")
    val enabled = a.optBoolean("enabled", true)
    
    ItemCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(time, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (enabled) TauText1 else TauText3)
                Text(title, fontSize = 14.sp, color = TauText3)
            }
            Switch(checked = enabled, onCheckedChange = { /* Toggle */ }, colors = SwitchDefaults.colors(checkedThumbColor = TauAccent))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = TauRed) }
        }
    }
}

@Composable
private fun TimerRow(t: JSONObject, onCancel: () -> Unit) {
    val now   = System.currentTimeMillis()
    val label = t.optString("label", "Timer")
    val type  = t.getString("type")
    val start = t.optLong("startTimeMs")
    val sub   = if (type == "timer") {
        val rem = (start + t.optLong("durationMs")) - now
        "Restante: ${fmtDur(rem.coerceAtLeast(0))}"
    } else "Transcurrido: ${fmtDur(now - start)}"

    ItemCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(TauAccent.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(if (type == "timer") Icons.Default.HourglassEmpty else Icons.Default.Timer, null, tint = TauAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sub, color = TauText3, fontSize = 12.sp)
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null, tint = TauText3) }
        }
    }
}

@Composable
private fun ScheduleRow(s: JSONObject, onToggle: () -> Unit, onDelete: () -> Unit) {
    val enabled = s.optBoolean("enabled", true)
    val label   = s.optString("label").ifBlank { s.optString("instruction").take(40) }
    val rec     = s.optJSONObject("recurrence")
    val recStr  = when (rec?.optString("type", "once")) {
        "interval" -> "Cada ${(rec.optLong("intervalMs", 60_000) / 60_000)} min"
        "daily"    -> "Diario ${rec.optString("time")}"
        "weekly"   -> "Semanal ${rec.optString("time")}"
        else       -> "Una vez"
    }

    ItemCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(TauBlue.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Event, null, tint = TauBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${fmtMs(s.optLong("triggerAtMs"))} · $recStr", color = TauText3, fontSize = 12.sp)
            }
            IconButton(onClick = onToggle) {
                Icon(if (enabled) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, null, tint = TauAccent)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = TauRed) }
        }
    }
}

@Composable
fun ItemCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TauSurface1,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, TauSeparator)
    ) {
        content()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fmtDur(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${sec}s"
        else -> "${sec}s"
    }
}

private fun fmtMs(ms: Long) =
    if (ms == 0L) "?"
    else SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()).format(Date(ms))

// ── JournalScreen (Diario) ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Actualizar", tint = TauText3) } },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        if (entries.isEmpty()) {
            EmptyState("Tu diario está vacío", Icons.Default.Book)
        } else {
            val cats = entries.map { it.optString("category") }.distinct().filter { it.isNotBlank() }
            if (cats.isNotEmpty()) {
                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(selected = filterCat.isBlank(), onClick = { filterCat = "" }, label = { Text("Todas") })
                    }
                    items(cats) { cat ->
                        FilterChip(selected = filterCat == cat, onClick = { filterCat = if (filterCat == cat) "" else cat },
                            label = { Text(cat) })
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
                        val new = JSONArray()
                        for (i in 0 until arr.length()) { if (arr.getJSONObject(i).getString("id") != id) new.put(arr.get(i)) }
                        prefs.edit().putString("entries", new.toString()).apply()
                        refresh()
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalCard(e: JSONObject, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ItemCard {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.optString("title"), color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.optString("category"), color = TauAccent, fontSize = 11.sp)
                        Text(fmtMs(e.optLong("createdAt")), color = TauText3, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = TauRed, modifier = Modifier.size(18.dp))
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(e.optString("details"), color = TauText2, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ── PermissionsScreen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen() {
    val ctx = LocalContext.current

    data class PermItem(val title: String, val desc: String, val granted: Boolean, val onGrant: () -> Unit)

    val items = listOf(
        PermItem("Servicio de Accesibilidad",
            "Necesario para la automatización de la interfaz (controlar otras apps).",
            DoeyAccessibilityService.isRunning()) {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Lector de Notificaciones",
            "Necesario para monitorear notificaciones entrantes y reaccionar automáticamente.",
            NotificationAccessManager.isAccessGranted(ctx)) {
            ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Micrófono",
            "Necesario para comandos de voz y detección de palabra de activación.",
            ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    )

    Column(Modifier.fillMaxSize().background(TauBg)) {
        TopAppBar(
            title = { Text("Permisos", color = TauText1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
        )

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                ItemCard {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, color = TauText1, fontSize = 15.sp)
                            Text(item.desc, color = TauText3, fontSize = 12.sp)
                        }
                        Button(
                            onClick = item.onGrant,
                            enabled = !item.granted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.granted) TauGreen.copy(0.2f) else TauAccent,
                                contentColor = if (item.granted) TauGreen else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (item.granted) "OK" else "Activar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
