package com.doey.tools

// Clase simple que maneja "journaling"
class JournalTool {

    fun logEntry(entry: String) {
        // Aquí podrías guardar la entrada en base de datos o archivo
        println("Journal entry: $entry")
    }

    fun getAllEntries(): List<String> {
        // Devuelve una lista de ejemplo
        return listOf("Entrada 1", "Entrada 2")
    }
}