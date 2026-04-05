package com.doey.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ── SchedulesScreen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var schedules by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var timers    by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    fun refresh() {
        val sa = SchedulerEngine.getAllSchedules(ctx)
        schedules = (0 until sa.length()).map { sa.getJSONObject(it) }
        val ta = TimerEngine.getAllTimers(ctx)
        timers = (0 until ta.length()).map { ta.getJSONObject(it) }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().background(Surface0)) {
        TopAppBar(
            title   = { Text("Schedules & Timers", color = Label1, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh", tint = Label3) } },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
        )

        if (schedules.isEmpty() && timers.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No schedules or timers yet.\nAsk Doey to 'set a reminder' or 'start a timer'.",
                    color = Label3, textAlign = TextAlign.Center, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timers.isNotEmpty()) {
                item { SectionLabel("Timers (${timers.size})") }
                items(timers) { t ->
                    TimerRow(t) { TimerEngine.removeTimer(ctx, t.getString("id")); refresh() }
                }
            }
            if (schedules.isNotEmpty()) {
                item { SectionLabel("Schedules (${schedules.size})") }
                items(schedules) { s ->
                    ScheduleRow(s,
                        onToggle = {
                            s.put("enabled", !s.optBoolean("enabled", true))
                            SchedulerEngine.setSchedule(ctx, s); refresh()
                        },
                        onDelete = { SchedulerEngine.removeSchedule(ctx, s.getString("id")); refresh() }
                    )
                }
            }
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
        "Remaining: ${fmtDur(rem.coerceAtLeast(0))}"
    } else "Elapsed: ${fmtDur(now - start)}"

    ItemCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (type == "timer") "⏱️" else "⏲️", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = Label1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(sub, color = Label3, fontSize = 12.sp)
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Cancel, "Cancel", tint = ErrorRed) }
        }
    }
}

@Composable
private fun ScheduleRow(s: JSONObject, onToggle: () -> Unit, onDelete: () -> Unit) {
    val enabled = s.optBoolean("enabled", true)
    val label   = s.optString("label").ifBlank { s.optString("instruction").take(40) }
    val rec     = s.optJSONObject("recurrence")
    val recStr  = when (rec?.optString("type", "once")) {
        "interval" -> "Every ${(rec.optLong("intervalMs", 60_000) / 60_000)} min"
        "daily"    -> "Daily ${rec.optString("time")}"
        "weekly"   -> "Weekly ${rec.optString("time")}"
        else       -> "Once"
    }

    ItemCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (enabled) "🟢" else "⏸️", fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = Label1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${fmtMs(s.optLong("triggerAtMs"))} · $recStr", color = Label3, fontSize = 12.sp)
            }
            IconButton(onClick = onToggle) {
                Icon(if (enabled) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Purple)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = ErrorRed) }
        }
    }
}

// ── JournalScreen ─────────────────────────────────────────────────────────────

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

    Column(Modifier.fillMaxSize().background(Surface0)) {
        TopAppBar(
            title   = { Text("Journal", color = Label1, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh", tint = Label3) } },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
        )

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No journal entries yet.\nAsk Doey to 'add a journal entry'.",
                    color = Label3, textAlign = TextAlign.Center, fontSize = 14.sp)
            }
            return@Column
        }

        val cats = entries.map { it.optString("category") }.distinct().filter { it.isNotBlank() }
        if (cats.isNotEmpty()) {
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = filterCat.isBlank(), onClick = { filterCat = "" }, label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleDark))
                }
                items(cats) { cat ->
                    FilterChip(selected = filterCat == cat, onClick = { filterCat = if (filterCat == cat) "" else cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleDark))
                }
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun JournalCard(e: JSONObject, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ItemCard {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.optString("title"), color = Label1, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.optString("category"), color = Purple, fontSize = 11.sp)
                        Text(fmtMs(e.optLong("createdAt")), color = Label3, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(e.optString("details"), color = Label2, fontSize = 13.sp)
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
        PermItem("Accessibility Service",
            "Required for UI automation (controlling other apps).",
            DoeyAccessibilityService.isRunning()) {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Notification Listener",
            "Required to monitor incoming notifications and react automatically.",
            NotificationAccessManager.isAccessGranted(ctx)) {
            ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Microphone",
            "Required for voice commands and wake word detection.",
            ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Contacts",
            "Required to look up contacts by name.",
            ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("SMS",
            "Required to read and send SMS messages.",
            ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        PermItem("Location (GPS)",
            "Required for location-based queries and navigation.",
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    )

    Column(Modifier.fillMaxSize().background(Surface0)) {
        TopAppBar(
            title  = { Text("Permissions", color = Label1, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
        )
        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                PermCard(item.title, item.desc, item.granted, item.onGrant)
            }
        }
    }
}

@Composable
private fun PermCard(title: String, desc: String, granted: Boolean, onGrant: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Surface1) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(if (granted) "✅" else "❌", fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Label1, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(desc, color = Label3, fontSize = 12.sp)
                if (!granted) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onGrant,
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                        border  = BorderStroke(1.dp, Purple)
                    ) { Text("Open Settings", fontSize = 12.sp) }
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(text, color = Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun ItemCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Surface1) {
        Box(Modifier.fillMaxWidth().padding(12.dp)) { content() }
    }
}

fun fmtMs(ms: Long): String =
    if (ms == 0L) "?" else SimpleDateFormat("EEE d MMM HH:mm", Locale.ENGLISH).format(Date(ms))

fun fmtDur(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return when { h > 0 -> "${h}h ${m}m"; m > 0 -> "${m}m ${sec}s"; else -> "${sec}s" }
}
