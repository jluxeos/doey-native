package com.doey.agente

import com.doey.herramientas.comun.ToolRegistry
import java.text.SimpleDateFormat
import java.util.*

object SystemPromptBuilder {

    /**
     * Prompt ÚNICO ultra-comprimido (~230–320 chars según contexto).
     * Contiene toda la funcionalidad del prompt full anterior (~920 chars)
     * en tamaño cercano al nano (~212 chars).
     *
     * Técnica: abreviaturas semánticas, eliminación de artículos/conectores,
     * símbolos (→ > /) y colapso de reglas redundantes en una sola línea.
     */
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
        val date  = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())

        return buildString {
            // Identidad + idioma + fecha (esencial para alarmas/timers)
            append("Doey,Android,$lang,$date.")

            // Usuario (opcional)
            if (userName.isNotBlank()) append("User:${userName.take(20)}.")

            // Reglas core comprimidas en una línea
            append("PARSER:acción→tool inmediata.Sin texto antes.Resp:1 frase.")

            // Reglas UI/accessibility comprimidas
            append("UI:ui_control>a11y,wait 1500ms post-app.")

            // Fallback + encadenado
            append("Fallo:intent→ui→a11y.Cadena:1a1.")

            // Memoria + Diario
            append("Mem:memory_personal(upsert/delete_fact).Diario:journal(add/update).")

            // Conducción
            if (drivingMode) append("Drive:1 frase,sin MD.")

            // Tono/Soul (opcional, truncado)
            if (soul.isNotBlank()) append("Tono:${soul.take(60)}.")

            // Memoria personal (opcional, truncada agresivamente)
            if (personalMemory.isNotBlank()) append("Mem_usr:${personalMemory.take(300)}.")
        }
    }

    // Aliases para compatibilidad con el código que llama buildNano/buildMini/buildMinimal
    fun buildNano(language: String): String = build(
        toolRegistry = ToolRegistry(), drivingMode = false, language = language
    )

    fun buildMini(language: String, soul: String): String = build(
        toolRegistry = ToolRegistry(), drivingMode = false, language = language, soul = soul
    )

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
