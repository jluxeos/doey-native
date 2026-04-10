package com.doey.ui.basico
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
