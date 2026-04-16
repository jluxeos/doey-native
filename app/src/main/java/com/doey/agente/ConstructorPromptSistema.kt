package com.doey.agente

import com.doey.herramientas.comun.ToolRegistry
import java.text.SimpleDateFormat
import java.util.*

object SystemPromptBuilder {

    // ── Prompt NANO (~30 tokens) — para comandos triviales que IRIS no resolvió ──
    // Úsalo cuando la complejidad es TRIVIAL o SIMPLE y no hay historial.
    // La IA literalmente solo necesita saber que es un parser de acciones.
    fun buildNano(language: String): String {
        val lang = if (language.startsWith("es")) "es" else language.take(2)
        return "Asistente Android. Idioma:$lang. Petición de acción→herramienta inmediata. " +
               "Pregunta de conocimiento→responde solo si estás seguro; si no, di que no sabes con certeza. " +
               "Respuesta:máx 1-2 oraciones. Sin inventar datos."
    }

    // ── Prompt MINI (~80 tokens) — para comandos simples con 1 acción ───────────
    // Incluye idioma + rol + regla de parser + fecha (necesaria para alarmas)
    fun buildMini(language: String, soul: String): String {
        val lang  = resolveLangName(language)
        val today = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
        return buildString {
            append("Eres Doey, asistente Android. Idioma:$lang. Fecha:$today.\n")
            append("PARSER:acción→herramienta inmediata. Sin texto antes de actuar.\n")
            append("Respuesta final:máx 1 oración corta. Sin JSON ni código al usuario.\n")
            if (soul.isNotBlank()) append("Tono:${soul.take(80)}\n")
        }
    }

    // ── Prompt FULL (~200-300 tokens) — para comandos complejos/encadenados ─────
    // Incluye fecha, memoria del usuario, reglas de encadenamiento y herramientas resumidas.
    // Sigue siendo 10x más pequeño que el system prompt anterior (~2000 tokens).
    fun build(
        toolRegistry: ToolRegistry,
        drivingMode: Boolean,
        language: String = "es",
        soul: String = "",
        personalMemory: String = "",
        expertMode: Boolean = false,
        userName: String = ""
    ): String {
        val lang  = resolveLangName(language)
        val today = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
        val tz    = TimeZone.getDefault().id

        return buildString {
            // Identidad + fecha (esencial para alarmas/timers)
            append("Doey — asistente Android.")
            if (userName.isNotBlank()) append(" Usuario:$userName.")
            append(" Idioma:$lang. Fecha:$today TZ:$tz.\n")

            // Modo
            if (drivingMode) append("CONDUCCIÓN:respuestas de 1 frase. Sin Markdown.\n")

            // Regla de oro — ultra-compacta
            append("PARSER PURO:petición de acción=herramienta inmediata.\n")
            append("NUNCA texto antes de actuar. NUNCA JSON crudo al usuario.\n")
            append("Respuesta final:máx 1 oración en $lang.\n")
            append("Fallo→intenta alternativa(intent→accessibility). No rendirse.\n")
            append("Comandos encadenados:uno por uno,verificar antes de siguiente.\n")

            // Memoria personal (si existe)
            if (personalMemory.isNotBlank()) {
                append("Memoria usuario:\n${personalMemory.take(400)}\n")
            }

            // Las herramientas se envían como parámetro tools[] de la API.
            // No es necesario listarlas aquí — reduce ~2000 chars del system prompt.
        }
    }

    fun buildMinimal(language: String, soul: String): String = buildMini(language, soul)

    private fun resolveLangName(lang: String): String {
        val t = lang.lowercase()
        return when {
            t == "system" -> resolveLangName(Locale.getDefault().toLanguageTag())
            t.startsWith("es") -> "Español"
            t.startsWith("en") -> "English"
            t.startsWith("fr") -> "Français"
            t.startsWith("de") -> "Deutsch"
            t.startsWith("pt") -> "Português"
            t.startsWith("it") -> "Italiano"
            else -> lang
        }
    }
}
