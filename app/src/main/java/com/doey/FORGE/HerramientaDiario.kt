package com.doey.FORGE

import android.content.Context
import com.doey.AplicacionDoey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JournalTool : Tool {
    private val ctx get() = AplicacionDoey.instance

    override fun name() = "journal"
    override fun description() = "Keep a personal journal or diary. Add, update, list, search, or delete entries. Each entry has a title, details, and optional category."
    override fun systemHint() = "Use 'add' to create new diary/journal entries. Use 'update' to edit existing ones. Always confirm the entry was saved."

    override fun parameters() = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "action"   to mapOf(
                "type" to "string",
                "enum" to listOf("add", "update", "list", "search", "delete"),
                "description" to "Action: 'add' new entry, 'update' existing, 'list' all, 'search' by keyword, 'delete' by id."
            ),
            "title"    to mapOf("type" to "string", "description" to "Title for the entry (required for 'add')."),
            "details"  to mapOf("type" to "string", "description" to "Main content of the entry (required for 'add', optional for 'update')."),
            "category" to mapOf("type" to "string", "description" to "Category, e.g. 'Personal', 'Work', 'Ideas'."),
            "id"       to mapOf("type" to "string", "description" to "Entry ID — required for 'update' and 'delete'."),
            "query"    to mapOf("type" to "string", "description" to "Keyword to search in title/details (for 'search' action).")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val action = args["action"] as? String ?: return@withContext errorResult("action required")
        val prefs  = ctx.getSharedPreferences("doey_journal", Context.MODE_PRIVATE)

        fun loadEntries(): JSONArray =
            try { JSONArray(prefs.getString("entries", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }

        fun saveEntries(arr: JSONArray) =
            prefs.edit().putString("entries", arr.toString()).apply()

        when (action) {
            // ── Agregar entrada nueva ─────────────────────────────────────────
            "add" -> {
                val title    = args["title"]    as? String ?: return@withContext errorResult("title required for 'add'")
                val details  = args["details"]  as? String ?: return@withContext errorResult("details required for 'add'")
                val category = args["category"] as? String ?: ""
                val id       = UUID.randomUUID().toString()

                val entry = JSONObject().apply {
                    put("id",        id)
                    put("title",     title)
                    put("details",   details)
                    put("category",  category)
                    put("createdAt", System.currentTimeMillis())
                    put("updatedAt", System.currentTimeMillis())
                }
                val entries = loadEntries()
                entries.put(entry)
                saveEntries(entries)
                successResult("Entrada de diario guardada con ID: ${id.take(8)}")
            }

            // ── Actualizar entrada existente ──────────────────────────────────
            "update" -> {
                val id = args["id"] as? String ?: return@withContext errorResult("id required for 'update'")
                val entries = loadEntries()
                var found = false
                val updated = JSONArray()
                for (i in 0 until entries.length()) {
                    val e = entries.getJSONObject(i)
                    if (e.optString("id") == id) {
                        found = true
                        args["title"]?.let    { e.put("title",    it as String) }
                        args["details"]?.let  { e.put("details",  it as String) }
                        args["category"]?.let { e.put("category", it as String) }
                        e.put("updatedAt", System.currentTimeMillis())
                        updated.put(e)
                    } else {
                        updated.put(e)
                    }
                }
                if (!found) return@withContext errorResult("No se encontró entrada con ID $id")
                saveEntries(updated)
                successResult("Entrada actualizada correctamente.")
            }

            // ── Listar todas ──────────────────────────────────────────────────
            "list" -> {
                val entries = loadEntries()
                if (entries.length() == 0) return@withContext successResult("No hay entradas en el diario todavía.")
                val sb = StringBuilder()
                for (i in 0 until entries.length()) {
                    val e = entries.getJSONObject(i)
                    sb.append("ID:${e.optString("id").take(8)} | ${e.optString("title")} | ${e.optString("category","Sin categoría")}\n")
                }
                successResult(sb.toString().trimEnd())
            }

            // ── Buscar por palabra clave ──────────────────────────────────────
            "search" -> {
                val query   = (args["query"] as? String ?: "").lowercase()
                val entries = loadEntries()
                val sb      = StringBuilder()
                for (i in 0 until entries.length()) {
                    val e = entries.getJSONObject(i)
                    val title   = e.optString("title").lowercase()
                    val details = e.optString("details").lowercase()
                    if (title.contains(query) || details.contains(query)) {
                        sb.append("ID:${e.optString("id").take(8)} | ${e.optString("title")}\n${e.optString("details").take(120)}\n---\n")
                    }
                }
                if (sb.isEmpty()) successResult("Sin resultados para \"$query\".")
                else successResult(sb.toString().trimEnd())
            }

            // ── Eliminar ──────────────────────────────────────────────────────
            "delete" -> {
                val id      = args["id"] as? String ?: return@withContext errorResult("id required for 'delete'")
                val entries = loadEntries()
                val kept    = JSONArray()
                var deleted = false
                for (i in 0 until entries.length()) {
                    val e = entries.getJSONObject(i)
                    if (e.optString("id").startsWith(id) || e.optString("id") == id) {
                        deleted = true
                    } else {
                        kept.put(e)
                    }
                }
                if (!deleted) return@withContext errorResult("No se encontró entrada con ID $id")
                saveEntries(kept)
                successResult("Entrada eliminada.")
            }

            else -> errorResult("Acción desconocida: $action")
        }
    }
}
