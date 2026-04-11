package com.doey.ui.comun

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.doey.ui.core.*
import com.doey.MainViewModel
import com.doey.servicios.comun.SchedulerEngine
import com.doey.servicios.comun.TimerEngine
import com.doey.servicios.comun.AlarmScheduler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SchedulesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var schedules   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var timers      by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }

    val alarmPrefs = ctx.getSharedPreferences("doey_alarms_store", 0)
    var alarms by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    fun refresh() {
        val sa = SchedulerEngine.getAllSchedules(ctx)
        schedules = (0 until sa.length()).map { sa.getJSONObject(it) }
        val ta = TimerEngine.getAllTimers(ctx)
        timers = (0 until ta.length()).map { ta.getJSONObject(it) }
        val alArr = try { JSONArray(alarmPrefs.getString("alarms", "[]")) } catch (_: Exception) { JSONArray() }
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
            containerColor   = TauSurface1,
            contentColor     = TauAccent,
            indicator        = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Alarmas",  fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Timers",   fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Agenda",   fontSize = 12.sp) })
        }

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AlarmList(alarms,    onRefresh = { refresh() })
                1 -> TimerList(timers,    onRefresh = { refresh() })
                2 -> ScheduleList(schedules, onRefresh = { refresh() })
            }
        }
    }
}

// ── AlarmList ─────────────────────────────────────────────────────────────────

@Composable
private fun AlarmList(alarms: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (alarms.isEmpty()) {
        AgendaEmptyState("No hay alarmas activas", Icons.Default.Alarm)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(alarms) { alarm ->
                AlarmRow(alarm) {
                    val id = alarm.optInt("id")
                    AlarmScheduler.cancelAlarm(id)
                    val prefs   = ctx.getSharedPreferences("doey_alarms_store", 0)
                    val current = try { JSONArray(prefs.getString("alarms", "[]")) } catch (_: Exception) { JSONArray() }
                    val next    = JSONArray()
                    for (i in 0 until current.length()) {
                        if (current.getJSONObject(i).optInt("id") != id) next.put(current.get(i))
                    }
                    prefs.edit().putString("alarms", next.toString()).apply()
                    onRefresh()
                }
            }
        }
    }
}

// ── TimerList ─────────────────────────────────────────────────────────────────

@Composable
private fun TimerList(timers: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (timers.isEmpty()) {
        AgendaEmptyState("No hay temporizadores", Icons.Default.Timer)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(timers) { t ->
                TimerRow(t) { TimerEngine.removeTimer(ctx, t.getString("id")); onRefresh() }
            }
        }
    }
}

// ── ScheduleList ──────────────────────────────────────────────────────────────

@Composable
private fun ScheduleList(schedules: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (schedules.isEmpty()) {
        AgendaEmptyState("Tu agenda está vacía", Icons.Default.EventNote)
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

// ── Rows ──────────────────────────────────────────────────────────────────────

@Composable
private fun AlarmRow(a: JSONObject, onDelete: () -> Unit) {
    val title   = a.optString("title", "Alarma")
    val time    = a.optString("time", "--:--")
    val enabled = a.optBoolean("enabled", true)

    ItemCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(time,  fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (enabled) TauText1 else TauText3)
                Text(title, fontSize = 14.sp, color = TauText3)
            }
            Switch(
                checked         = enabled,
                onCheckedChange = { /* toggle pendiente */ },
                colors          = SwitchDefaults.colors(checkedThumbColor = TauAccent)
            )
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = TauRed) }
        }
    }
}

@Composable
private fun TimerRow(t: JSONObject, onCancel: () -> Unit) {
    val now   = System.currentTimeMillis()
    val label = t.optString("label", "Timer")
    val type  = t.optString("type", "timer")
    val start = t.optLong("startTimeMs")
    val sub   = if (type == "timer") {
        val rem = (start + t.optLong("durationMs")) - now
        "Restante: ${fmtDur(rem.coerceAtLeast(0))}"
    } else "Transcurrido: ${fmtDur(now - start)}"

    ItemCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(TauAccent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (type == "timer") Icons.Default.HourglassEmpty else Icons.Default.Timer,
                    null, tint = TauAccent, modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sub,   color = TauText3, fontSize = 12.sp)
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
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(TauBlue.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, null, tint = TauBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${fmtMs(s.optLong("triggerAtMs"))} · $recStr", color = TauText3, fontSize = 12.sp)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (enabled) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    null, tint = TauAccent
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = TauRed) }
        }
    }
}

// ── ItemCard (componente compartido) ──────────────────────────────────────────

@Composable
fun ItemCard(content: @Composable () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = TauSurface1,
        modifier = Modifier.fillMaxWidth(),
        border   = BorderStroke(1.dp, TauSeparator)
    ) {
        content()
    }
}

// ── EmptyState interno ────────────────────────────────────────────────────────

@Composable
private fun AgendaEmptyState(msg: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = TauSurface3)
            Spacer(Modifier.height(16.dp))
            Text(msg, color = TauText3, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fmtDur(ms: Long): String {
    val s   = ms / 1000
    val h   = s / 3600
    val m   = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${sec}s"
        else  -> "${sec}s"
    }
}

private fun fmtMs(ms: Long) =
    if (ms == 0L) "?"
    else SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()).format(Date(ms))
