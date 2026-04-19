package com.doey.NEXUS

import com.doey.FORGE.ToolRegistry
import java.text.SimpleDateFormat
import java.util.*

object SystemPromptBuilder {

    /**
     * Prompt UNICO ultra-comprimido con toda la funcionalidad del full (~920 chars)
     * en ~310-380 chars segun contexto.
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
            append("Doey,Android,$lang,$date.")
            if (userName.isNotBlank()) append("User:${userName.take(20)}.")
            append("MODOS:")
            append("1)ACCION->tool inmediata,sin texto antes,confirma en 1 frase.")
            append("2)PREGUNTA->responde directo con tu conocimiento,sin disclaimers,1-3 frases.")
            append("3)NO SABES O DATO RECIENTE->intent(android.intent.action.VIEW,https://www.google.com/search?q=QUERY) y di 'Buscando en Google...'.")
            append("UI:ui_control>a11y,wait 1500ms post-app.")
            append("Fallo:intent->ui->a11y.Cadena:1a1.")
            append("Mem:memory_personal(upsert/delete_fact).Diario:journal(add/update).")
            if (drivingMode) append("Drive:1 frase,sin MD.")
            if (soul.isNotBlank()) append("Tono:${soul.take(60)}.")
            if (personalMemory.isNotBlank()) append("Mem_usr:${personalMemory.take(300)}.")
        }
    }

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
            t.startsWith("es") -> "Espanol"
            t.startsWith("en") -> "English"
            t.startsWith("fr") -> "Francais"
            t.startsWith("de") -> "Deutsch"
            t.startsWith("pt") -> "Portugues"
            t.startsWith("it") -> "Italiano"
            else -> lang
        }
    }
}
