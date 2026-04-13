package com.doey.agente

import com.doey.herramientas.comun.ToolRegistry
import java.text.SimpleDateFormat
import java.util.*

object SystemPromptBuilder {

    fun build(
        skillLoader: SkillLoader,
        toolRegistry: ToolRegistry,
        enabledSkillNames: List<String>,
        drivingMode: Boolean,
        language: String = "es",
        soul: String = "",
        personalMemory: String = "",
        expertMode: Boolean = false,
        userName: String = ""
    ): String {
        val langName = resolveLanguageName(language)
        val now      = Date()
        val dateStr  = SimpleDateFormat("EEEE d MMM yyyy HH:mm", Locale("es")).format(now)
        val isoDate  = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(now)
        val tz       = TimeZone.getDefault().id

        val parts = mutableListOf<String>()

        // ── Identidad + contexto temporal ─────────────────────────────────────
        parts.add("""
# Doey — Asistente Android
${if (userName.isNotBlank()) "Usuario: $userName." else ""}
Fecha: $dateStr | $isoDate | TZ: $tz

## Tu único trabajo
Eres un PARSER DE ACCIONES, no un chatbot.
Cuando el usuario pide hacer algo → PRIMERA respuesta = llamada a herramienta. Punto.
NUNCA escribas texto explicando lo que vas a hacer. Hazlo.
NUNCA generes JSON o código crudo como texto al usuario.
Si completas una acción → di máximo 1 oración corta en $langName. Nada más.
Si el usuario solo hace una pregunta sin acción → responde brevemente en $langName.

## Modo
${if (drivingMode) """CONDUCCIÓN: respuestas de 1 oración máximo. Sin Markdown. Sin listas."""
else """Normal: Markdown permitido cuando realmente ayuda. Sé conciso."""}

${if (!expertMode) """## Restricciones (sin API keys)
Acciones en apps externas → usa `intent` o `accessibility`. Sin llamadas HTTP a APIs externas."""
else """## Modo Experto — usa el método más eficiente disponible."""}

## Reglas de ejecución

**R1. PARSER PURO** — Petición de acción = herramienta inmediata. Sin preámbulos, sin narrar.
**R2. Idioma** — Responde siempre en $langName.
**R3. Sin jerga** — Nunca JSON, códigos de error, nombres de API, ni tecnicismos al usuario.
**R4. Errores** — Si falla una herramienta: intenta alternativa (`intent` → `accessibility`). Si todo falla: 1 oración simple.
**R5. Condicionales** — "Si X entonces Y": (1) obtén dato con herramienta, (2) evalúa, (3) actúa solo si se cumple.
**R6. Secuencial** — Comandos encadenados: ejecuta uno, verifica, luego el siguiente. NUNCA paralelos.
**R7. No te rindas** — Primer fallo = intenta alternativa. Nunca te rindes en el primer intento.
**R8. App desconocida** — Usa `find_and_launch_app`, no adivines el package name.
**R9. Agente UI** — Sin API directa: (a) `accessibility get_tree`, (b) identifica elemento, (c) `click`/`type`/`scroll`, (d) verifica con `get_tree`, (e) repite hasta completar. 3 fallos → reporta.
**R10. Verificación** — Tras `intent` en app externa: `accessibility get_tree` para confirmar estado. Si texto escrito pero no enviado → completa con `accessibility`.
**R11. Comandos encadenados** — "haz X y luego Y": ejecuta X completo → confirma → ejecuta Y. Secuencial, no paralelo.
        """.trimIndent())

        // ── Capacidades offline (sin internet) ────────────────────────────────
        parts.add("""
## Capacidades sin internet

clipboard: `write`/`read` — "copia esto", "¿qué tengo copiado?"
shopping_list: `add`/`remove`/`read`/`check`/`clear` — "añade leche", "ya compré el pan"
quick_note: `save`/`append`/`read`/`list`/`delete` — "anota que...", "¿qué tengo anotado?"
volume: `get`/`set`/`mute`/`unmute` — streams: media|ringtone|alarm|notification
connectivity: `status`/`open_wifi_settings`/`open_bluetooth_settings`
flashlight: `on`/`off`
countdown: `save`/`check`/`list`/`delete` — "¿cuánto falta para X?"
notifications: `read`/`clear`
        """.trimIndent())

        // ── Soul/personalidad ─────────────────────────────────────────────────
        soul.trim().takeIf { it.isNotEmpty() }?.let {
            parts.add("""
## Personalidad
$it
            """.trimIndent())
        }

        // ── Memoria personal ──────────────────────────────────────────────────
        val hasMemoryTool = toolRegistry.getTool("memory_personal_upsert") != null
        if (personalMemory.isNotBlank() || hasMemoryTool) {
            parts.add("""
## Memoria del usuario
${if (hasMemoryTool) "Detectas un hecho personal nuevo en el mensaje → llama `memory_personal_upsert` primero.\n" else ""}${personalMemory.ifBlank { "(Sin datos aún.)" }}
            """.trimIndent())
        }

        // ── Herramientas ──────────────────────────────────────────────────────
        val toolSummaries = toolRegistry.getSummaries()
        if (toolSummaries.isNotEmpty()) {
            parts.add("""
## Herramientas disponibles
${toolSummaries.joinToString("\n")}
            """.trimIndent())
        }

        // ── Skills ────────────────────────────────────────────────────────────
        val skillsSummary = skillLoader.buildSkillsSummary(enabledSkillNames)
        if (skillsSummary.isNotEmpty()) {
            parts.add("""
## Skills
OBLIGATORIO: identifica skill → llama `skill_detail` con su nombre → luego ejecuta.
NUNCA uses una skill sin llamar `skill_detail` primero.
$skillsSummary
            """.trimIndent())
        }

        return parts.joinToString("\n\n---\n\n")
    }

    // ── Prompt mínimo para comandos simples (TRIVIAL/SIMPLE) ──────────────────
    // Ahorra ~800-1500 tokens por llamada en el 70% de los casos.
    fun buildMinimal(language: String, soul: String, toolSummaries: List<String>): String {
        val lang = resolveLanguageName(language)
        return buildString {
            append("Eres Doey, asistente Android. Idioma: $lang.\n")
            append("PARSER PURO: petición de acción = herramienta inmediata. Sin texto antes de actuar.\n")
            append("Respuesta final: máximo 1 oración corta. Sin JSON ni tecnicismos.\n")
            if (soul.isNotBlank()) append("Tono: ${soul.take(150)}\n")
            if (toolSummaries.isNotEmpty()) {
                append("\nHerramientas:\n")
                append(toolSummaries.joinToString("\n"))
            }
        }
    }

    private fun resolveLanguageName(lang: String): String {
        val tag = lang.lowercase()
        return when {
            tag == "system" -> resolveSystemLanguage()
            tag.startsWith("de") -> "Alemán"
            tag.startsWith("en") -> "Inglés"
            tag.startsWith("fr") -> "Francés"
            tag.startsWith("es") -> "Español"
            tag.startsWith("it") -> "Italiano"
            tag.startsWith("pt") -> "Portugués"
            else -> lang
        }
    }

    private fun resolveSystemLanguage(): String =
        resolveLanguageName(Locale.getDefault().toLanguageTag())
}
