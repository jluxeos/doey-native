package com.doey.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.window.Dialog
import com.doey.MainViewModel
import com.doey.servicios.comun.AlarmScheduler
import com.doey.servicios.comun.SchedulerEngine
import com.doey.servicios.comun.TimerEngine
import com.doey.ui.core.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Pantalla Utilerías ────────────────────────────────────────────────────────

@Composable
fun SchedulesScreen(vm: MainViewModel) {
    val ctx         = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var schedules   by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var timers      by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var alarms      by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    // Lista de compras persistida en SharedPreferences
    val shopPrefs = ctx.getSharedPreferences("doey_shopping_list", 0)
    var shoppingItems by remember {
        mutableStateOf(
            try { JSONArray(shopPrefs.getString("items", "[]")).let { a -> (0 until a.length()).map { a.getJSONObject(it) } } }
            catch (_: Exception) { emptyList() }
        )
    }

    fun saveShoppingItems(list: List<JSONObject>) {
        val arr = JSONArray(); list.forEach { arr.put(it) }
        shopPrefs.edit().putString("items", arr.toString()).apply()
        shoppingItems = list
    }

    fun refresh() {
        val sa = SchedulerEngine.getAllSchedules(ctx)
        schedules = (0 until sa.length()).map { sa.getJSONObject(it) }
        val ta = TimerEngine.getAllTimers(ctx)
        timers = (0 until ta.length()).map { ta.getJSONObject(it) }
        val alarmPrefs = ctx.getSharedPreferences("doey_alarms_store", 0)
        val alArr = try { JSONArray(alarmPrefs.getString("alarms", "[]")) } catch (_: Exception) { JSONArray() }
        alarms = (0 until alArr.length()).map { alArr.getJSONObject(it) }
    }

    LaunchedEffect(Unit) { refresh() }

    // Diálogos de creación
    var showNewAlarm     by remember { mutableStateOf(false) }
    var showNewTimer     by remember { mutableStateOf(false) }
    var showNewShopItem  by remember { mutableStateOf(false) }
    var showNewSchedule  by remember { mutableStateOf(false) }

    val tabs = listOf("⏰ Alarmas", "⏱ Timers", "⏲ Crono", "🛒 Compras", "📅 Agenda")

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("Utilerías", color = TauText1, fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { refresh() }) { Icon(CustomIcons.Refresh, "Actualizar", tint = TauText3) } },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = TauSurface1)
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { showNewAlarm = true },
                    containerColor = TauAccent
                ) { Icon(CustomIcons.Add, "Nueva alarma", tint = Color.White) }
                1 -> FloatingActionButton(
                    onClick = { showNewTimer = true },
                    containerColor = TauAccent
                ) { Icon(CustomIcons.Add, "Nuevo timer", tint = Color.White) }
                3 -> FloatingActionButton(
                    onClick = { showNewShopItem = true },
                    containerColor = TauAccent
                ) { Icon(CustomIcons.Add, "Agregar ítem", tint = Color.White) }
                4 -> FloatingActionButton(
                    onClick = { showNewSchedule = true },
                    containerColor = TauAccent
                ) { Icon(CustomIcons.Add, "Nueva tarea", tint = Color.White) }
                else -> {}
            }
        },
        containerColor = TauBg
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tabs con scroll horizontal
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(TauSurface1)
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { i, label ->
                    val selected = selectedTab == i
                    Column(
                        Modifier
                            .clickable { selectedTab = i }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            fontSize   = 12.sp,
                            color      = if (selected) TauAccent else TauText3,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(40.dp).height(2.dp).background(if (selected) TauAccent else Color.Transparent))
                    }
                }
            }
            HorizontalDivider(color = TauSeparator)

            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> AlarmList(alarms, onRefresh = { refresh() })
                    1 -> TimerList(timers, onRefresh = { refresh() })
                    2 -> StopwatchTab()
                    3 -> ShoppingList(shoppingItems, onSave = { saveShoppingItems(it) })
                    4 -> ScheduleList(schedules, onRefresh = { refresh() })
                }
            }
        }
    }

    // ── Diálogo nueva alarma ──────────────────────────────────────────────────
    if (showNewAlarm) {
        NewAlarmDialog(
            onDismiss = { showNewAlarm = false },
            onCreate  = { hour, minute, label ->
                // Usar ACTION_SET_ALARM para que aparezca en la app Reloj
                val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                    putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Alarma" })
                    putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { ctx.startActivity(intent) } catch (_: Exception) { }
                // Persistir localmente
                val alarmId = hour * 100 + minute
                val prefs   = ctx.getSharedPreferences("doey_alarms_store", 0)
                val existing = try { JSONArray(prefs.getString("alarms", "[]")) } catch (_: Exception) { JSONArray() }
                val filtered = JSONArray()
                for (i in 0 until existing.length()) { if (existing.getJSONObject(i).optInt("id") != alarmId) filtered.put(existing.get(i)) }
                filtered.put(JSONObject().apply {
                    put("id", alarmId); put("title", label.ifBlank { "Alarma" })
                    put("time", String.format("%02d:%02d", hour, minute)); put("enabled", true); put("recurring", false)
                })
                prefs.edit().putString("alarms", filtered.toString()).apply()
                showNewAlarm = false; refresh()
            }
        )
    }

    // ── Diálogo nuevo timer ───────────────────────────────────────────────────
    if (showNewTimer) {
        NewTimerDialog(
            onDismiss = { showNewTimer = false },
            onCreate  = { minutes, label ->
                val alarmId = (label.hashCode() xor (System.currentTimeMillis() / 1000).toInt()) and 0x7FFFFFFF
                AlarmScheduler.scheduleAlarmInMinutes(alarmId, label.ifBlank { "Timer" }, "", minutes)
                // También con intent nativo para feedback del sistema
                val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(android.provider.AlarmClock.EXTRA_LENGTH, minutes * 60)
                    putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                    if (label.isNotBlank()) putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { ctx.startActivity(intent) } catch (_: Exception) { }
                showNewTimer = false; refresh()
            }
        )
    }

    // ── Diálogo nuevo ítem de compras ─────────────────────────────────────────
    if (showNewShopItem) {
        NewShopItemDialog(
            onDismiss = { showNewShopItem = false },
            onCreate  = { name ->
                val item = JSONObject().apply { put("id", System.currentTimeMillis()); put("name", name); put("done", false) }
                saveShoppingItems(shoppingItems + item)
                showNewShopItem = false
            }
        )
    }

    // ── Diálogo nueva tarea agenda ────────────────────────────────────────────
    if (showNewSchedule) {
        NewScheduleDialog(
            onDismiss = { showNewSchedule = false },
            onCreate  = { label, hour, minute ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                val s = JSONObject().apply {
                    put("id", System.currentTimeMillis().toString())
                    put("label", label); put("instruction", label)
                    put("triggerAtMs", cal.timeInMillis); put("enabled", true)
                    put("recurrence", JSONObject().apply { put("type", "once") })
                }
                SchedulerEngine.setSchedule(ctx, s)
                showNewSchedule = false; refresh()
            }
        )
    }
}

// ── AlarmList ─────────────────────────────────────────────────────────────────

@Composable
private fun AlarmList(alarms: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (alarms.isEmpty()) {
        UtilEmptyState("No hay alarmas\nDi: 'pon alarma a las 7' o usa el +", CustomIcons.Alarm)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(alarms, key = { it.optInt("id") }) { alarm ->
                AlarmRow(alarm) {
                    val id = alarm.optInt("id")
                    AlarmScheduler.cancelAlarm(id)
                    // También cancelar via intent del sistema
                    val cancelIntent = android.content.Intent(android.provider.AlarmClock.ACTION_DISMISS_ALARM).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { ctx.startActivity(cancelIntent) } catch (_: Exception) { }
                    val prefs   = ctx.getSharedPreferences("doey_alarms_store", 0)
                    val current = try { JSONArray(prefs.getString("alarms", "[]")) } catch (_: Exception) { JSONArray() }
                    val next    = JSONArray()
                    for (i in 0 until current.length()) { if (current.getJSONObject(i).optInt("id") != id) next.put(current.get(i)) }
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
        UtilEmptyState("No hay timers activos\nDi: 'pon timer de 5 minutos' o usa el +", CustomIcons.Timer)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(timers, key = { it.optString("id") }) { t ->
                TimerRow(t) { TimerEngine.removeTimer(ctx, t.getString("id")); onRefresh() }
            }
        }
    }
}

// ── Cronómetro ────────────────────────────────────────────────────────────────

@Composable
private fun StopwatchTab() {
    var running    by remember { mutableStateOf(false) }
    var elapsedMs  by remember { mutableStateOf(0L) }
    var startMs    by remember { mutableStateOf(0L) }
    var laps       by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(running) {
        if (running) {
            startMs = System.currentTimeMillis() - elapsedMs
            while (running) {
                delay(50)
                elapsedMs = System.currentTimeMillis() - startMs
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Display tiempo
        val h   = elapsedMs / 3_600_000
        val m   = (elapsedMs % 3_600_000) / 60_000
        val s   = (elapsedMs % 60_000) / 1_000
        val cs  = (elapsedMs % 1_000) / 10
        val display = if (h > 0) String.format("%d:%02d:%02d.%02d", h, m, s, cs)
                      else       String.format("%02d:%02d.%02d", m, s, cs)

        Text(display, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = TauText1)

        Spacer(Modifier.height(32.dp))

        // Controles
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Reset / Vuelta
            OutlinedButton(
                onClick = {
                    if (running) {
                        laps = laps + elapsedMs
                    } else {
                        elapsedMs = 0L; laps = emptyList()
                    }
                },
                border = BorderStroke(1.dp, TauSeparator)
            ) {
                Text(if (running) "Vuelta" else "Reset", color = TauText2)
            }

            // Start / Stop
            Button(
                onClick  = { running = !running },
                colors   = ButtonDefaults.buttonColors(containerColor = if (running) TauRed else TauAccent),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (running) "Parar" else "Iniciar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Vueltas
        if (laps.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f)) {
                items(laps.reversed().mapIndexed { i, ms -> Pair(laps.size - i, ms) }) { (num, ms) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vuelta $num", color = TauText3, fontSize = 13.sp)
                        val lm = ms / 60_000; val ls = (ms % 60_000) / 1_000; val lc = (ms % 1_000) / 10
                        Text(String.format("%02d:%02d.%02d", lm, ls, lc), color = TauText1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = TauSeparator.copy(alpha = 0.4f))
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Lista de compras ──────────────────────────────────────────────────────────

@Composable
private fun ShoppingList(items: List<JSONObject>, onSave: (List<JSONObject>) -> Unit) {
    if (items.isEmpty()) {
        UtilEmptyState("Lista de compras vacía\nDi: 'agrega leche a la lista' o usa el +", Icons.Default.ShoppingCart)
    } else {
        Column(Modifier.fillMaxSize()) {
            // Botón limpiar marcados
            val hasDone = items.any { it.optBoolean("done") }
            if (hasDone) {
                TextButton(
                    onClick = { onSave(items.filter { !it.optBoolean("done") }) },
                    modifier = Modifier.align(Alignment.End).padding(end = 16.dp, top = 8.dp)
                ) { Text("Limpiar marcados", color = TauRed, fontSize = 12.sp) }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.optLong("id") }) { item ->
                    val done = item.optBoolean("done")
                    ItemCard {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Check/uncheck con ícono (sin Checkbox de Material3)
                            IconButton(onClick = {
                                val newDone = !done
                                item.put("done", newDone as Any)
                                onSave(items.map { if (it.optLong("id") == item.optLong("id")) item else it })
                            }) {
                                Icon(
                                    if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (done) TauAccent else TauText3,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                item.optString("name"),
                                color    = if (done) TauText3 else TauText1,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                            IconButton(onClick = {
                                onSave(items.filter { it.optLong("id") != item.optLong("id") })
                            }) { Icon(CustomIcons.Delete, null, tint = TauRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ── Agenda (tareas programadas) ───────────────────────────────────────────────

@Composable
private fun ScheduleList(schedules: List<JSONObject>, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    if (schedules.isEmpty()) {
        UtilEmptyState("Tu agenda está vacía\nDi: 'recuérdame llamar a las 3' o usa el +", CustomIcons.EventNote)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(schedules, key = { it.optString("id") }) { s ->
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
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(TauAccent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(CustomIcons.Alarm, null, tint = TauAccent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(time,  fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (enabled) TauText1 else TauText3)
                Text(title, fontSize = 13.sp, color = TauText3)
            }
            IconButton(onClick = onDelete) { Icon(CustomIcons.Delete, null, tint = TauRed) }
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
                Modifier.size(44.dp).clip(CircleShape).background(TauAccent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(CustomIcons.Timer, null, tint = TauAccent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sub,   color = TauText3, fontSize = 12.sp)
            }
            IconButton(onClick = onCancel) { Icon(CustomIcons.Close, null, tint = TauText3) }
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
                Modifier.size(44.dp).clip(CircleShape).background(TauBlue.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(CustomIcons.Event, null, tint = TauBlue, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TauText1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${fmtMs(s.optLong("triggerAtMs"))} · $recStr", color = TauText3, fontSize = 12.sp)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (enabled) CustomIcons.PauseCircleFilled else CustomIcons.PlayCircleFilled,
                    null, tint = TauAccent
                )
            }
            IconButton(onClick = onDelete) { Icon(CustomIcons.Delete, null, tint = TauRed) }
        }
    }
}

// ── Diálogos de creación ──────────────────────────────────────────────────────

@Composable
private fun NewAlarmDialog(onDismiss: () -> Unit, onCreate: (Int, Int, String) -> Unit) {
    var hourText   by remember { mutableStateOf("07") }
    var minuteText by remember { mutableStateOf("00") }
    var label      by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = TauSurface1) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nueva alarma", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText, onValueChange = { if (it.length <= 2) hourText = it },
                        label = { Text("Hora") }, modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                    )
                    Text(":", color = TauText1, fontSize = 28.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = minuteText, onValueChange = { if (it.length <= 2) minuteText = it },
                        label = { Text("Min") }, modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                    )
                }
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Etiqueta (opcional)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TauText3) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 7
                            val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            onCreate(h, m, label)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TauAccent)
                    ) { Text("Crear", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun NewTimerDialog(onDismiss: () -> Unit, onCreate: (Int, String) -> Unit) {
    var minutes by remember { mutableStateOf("5") }
    var label   by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = TauSurface1) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nuevo timer", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 18.sp)
                OutlinedTextField(
                    value = minutes, onValueChange = { minutes = it },
                    label = { Text("Minutos") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                )
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Etiqueta (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TauText3) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(minutes.toIntOrNull()?.coerceAtLeast(1) ?: 5, label) },
                        colors  = ButtonDefaults.buttonColors(containerColor = TauAccent)
                    ) { Text("Iniciar", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun NewShopItemDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = TauSurface1) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Agregar a la lista", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 18.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("¿Qué necesitas?") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TauText3) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick  = { if (name.isNotBlank()) onCreate(name.trim()) },
                        colors   = ButtonDefaults.buttonColors(containerColor = TauAccent),
                        enabled  = name.isNotBlank()
                    ) { Text("Agregar", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun NewScheduleDialog(onDismiss: () -> Unit, onCreate: (String, Int, Int) -> Unit) {
    var label      by remember { mutableStateOf("") }
    var hourText   by remember { mutableStateOf("09") }
    var minuteText by remember { mutableStateOf("00") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = TauSurface1) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nueva tarea", fontWeight = FontWeight.Bold, color = TauText1, fontSize = 18.sp)
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText, onValueChange = { if (it.length <= 2) hourText = it },
                        label = { Text("Hora") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                    )
                    Text(":", color = TauText1, fontSize = 28.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = minuteText, onValueChange = { if (it.length <= 2) minuteText = it },
                        label = { Text("Min") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TauAccent, cursorColor = TauAccent)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TauText3) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (label.isNotBlank()) {
                                val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
                                val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                onCreate(label.trim(), h, m)
                            }
                        },
                        colors  = ButtonDefaults.buttonColors(containerColor = TauAccent),
                        enabled = label.isNotBlank()
                    ) { Text("Guardar", color = Color.White) }
                }
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
fun ItemCard(content: @Composable () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = TauSurface1,
        modifier = Modifier.fillMaxWidth(),
        border   = BorderStroke(1.dp, TauSeparator)
    ) { content() }
}

@Composable
private fun UtilEmptyState(msg: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = TauSurface3)
            Spacer(Modifier.height(16.dp))
            Text(msg, color = TauText3, textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 22.sp)
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
