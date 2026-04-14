package com.doey.herramientas.comun

import android.content.Context
import com.doey.AplicacionDoey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JournalTool : Tool {
    override fun name() = "journal"
    override fun description() = "Keep a personal journal or notes. Entries are stored with a title, details, and an optional category."
    override fun parameters() = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "title"    to mapOf("type" to "string", "description" to "A concise title for the journal entry."),
            "details"  to mapOf("type" to "string", "description" to "The main content or details of the journal entry."),
            "category" to mapOf("type" to "string", "description" to "An optional category for the entry (e.g., 'Work', 'Personal', 'Ideas')."),
            "action"   to mapOf("type" to "string", "enum" to listOf("add", "list", "delete"), "description" to "Action to perform: 'add' a new entry, 'list' all entries, or 'delete' an entry."),
            "id"       to mapOf("type" to "string", "description" to "Required for 'delete' action. The ID of the entry to delete.")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")
        val ctx = AplicacionDoey.instance
        val prefs = ctx.getSharedPreferences("doey_journal", Context.MODE_PRIVATE)

        return when (action) {
            "add" -> {
                val title = args["title"] as? String ?: return errorResult("title required for 'add' action")
                val details = args["details"] as? String ?: return errorResult("details required for 'add' action")
                val category = args["category"] as? String ?: ""

                val newEntry = JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("title", title)
                    put("details", details)
                    put("category", category)
                    put("createdAt", System.currentTimeMillis())
                }

                val entriesArray = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
                entriesArray.put(newEntry)
                prefs.edit().putString("entries", entriesArray.toString()).apply()
                successResult("Journal entry added with ID: ${newEntry.getString("id")}")
            }
            "list" -> {
                val entriesArray = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
                if (entriesArray.length() == 0) {
                    successResult("No journal entries found.")
                } else {
                    val sb = StringBuilder("Journal Entries:\n")
                    for (i in 0 until entriesArray.length()) {
                        val entry = entriesArray.getJSONObject(i)
                        sb.append("- ID: ${entry.optString("id").take(8)}..., Title: ${entry.optString("title")}, Category: ${entry.optString("category", "None")}\n")
                    }
                    successResult(sb.toString())
                }
            }
            "delete" -> {
                val idToDelete = args["id"] as? String ?: return errorResult("id required for 'delete' action")
                val entriesArray = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
                val newEntriesArray = JSONArray()
                var deleted = false
                for (i in 0 until entriesArray.length()) {
                    val entry = entriesArray.getJSONObject(i)
                    if (entry.optString("id") == idToDelete) {
                        deleted = true
                    } else {
                        newEntriesArray.put(entry)
                    }
                }
                prefs.edit().putString("entries", newEntriesArray.toString()).apply()
                if (deleted) {
                    successResult("Journal entry with ID $idToDelete deleted.")
                } else {
                    errorResult("Journal entry with ID $idToDelete not found.")
                }
            }
            else -> errorResult("Unknown action: $action")
        }
    }
}
