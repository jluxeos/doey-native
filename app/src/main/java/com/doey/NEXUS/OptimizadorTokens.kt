package com.doey.NEXUS

import com.doey.ORACLE.Message
import com.doey.FORGE.ToolRegistry

/**
 * TokenOptimizer — Doey 23.4.9 Ultra (Tau Version)
 *
 * Módulo de optimización de tokens para reducir el costo y latencia de las
 * llamadas al LLM. Implementa varias estrategias:
 *
 * 1. **Cache de system prompt**: El system prompt se construye una vez y se
 *    reutiliza mientras no cambien los ajustes. Ahorra ~500-2000 tokens por mensaje.
 *
 * 2. **Compresión de historial**: Mensajes antiguos se comprimen a un resumen
 *    en lugar de enviarse completos.
 *
 * 3. **Modo nano**: Para comandos simples, usa un system prompt mínimo.
 *
 * 4. **Deduplicación de tool results**: Elimina resultados de herramientas
 *    redundantes o muy largos.
 *
 * 5. **Truncado inteligente**: Trunca contenido largo manteniendo el contexto
 *    más relevante (inicio + fin).
 */
object TokenOptimizer {

    // ── Cache de system prompt ─────────────────────────────────────────────────

    data class PromptCacheKey(
        val provider: String,
        val soul: String,
        val personalMemory: String,
        val enabledSkills: List<String>,
        val expertMode: Boolean,
        val drivingMode: Boolean,
        val language: String
    )

    private var cachedKey: PromptCacheKey? = null
    private var cachedPrompt: String = ""

    fun getCachedSystemPrompt(
        key: PromptCacheKey,
        builder: () -> String
    ): String {
        if (cachedKey == key && cachedPrompt.isNotBlank()) {
            return cachedPrompt
        }
        val newPrompt = builder()
        cachedKey   = key
        cachedPrompt = newPrompt
        return newPrompt
    }

    fun invalidateCache() {
        cachedKey    = null
        cachedPrompt = ""
    }

    fun buildCacheKey(
        provider: String,
        soul: String,
        personalMemory: String,
        enabledSkills: List<String>,
        expertMode: Boolean,
        drivingMode: Boolean,
        language: String
    ) = PromptCacheKey(provider, soul, personalMemory, enabledSkills, expertMode, drivingMode, language)

    // ── Clasificación de complejidad del comando ───────────────────────────────

    enum class CommandComplexity {
        /** Comando trivial: hora, fecha, batería, abrir app → sin IA */
        TRIVIAL,
        /** Comando simple: una acción clara → system prompt mínimo */
        SIMPLE,
        /** Comando moderado: una tarea con contexto → system prompt normal */
        MODERATE,
        /** Comando complejo: multi-paso, razonamiento → system prompt completo */
        COMPLEX
    }

    // Patrones que siempre usan prompt NANO — no necesitan contexto ni historial.
    // Incluye preguntas de recomendación/info simple que la IA responde en 1 oración.
    private val TRIVIAL_PATTERNS = listOf(
        // Consultas del sistema
        Regex("""(?:qué hora|hora actual|what time)""", RegexOption.IGNORE_CASE),
        Regex("""(?:qué día|qué fecha|fecha actual|what day|what date)""", RegexOption.IGNORE_CASE),
        Regex("""(?:cuánta batería|batería|battery level|carga del tel)""", RegexOption.IGNORE_CASE),
        Regex("""(?:cuánto espacio|cuánta ram|velocidad de internet|mi ip|tiempo encendido)""", RegexOption.IGNORE_CASE),
        // Acciones directas simples
        Regex("""(?:abre?|open|launch|pon|inicia)\s+\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:llama a|llámale a|call)\s+\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:pon|sube|baja)\s+(?:el\s+)?volumen""", RegexOption.IGNORE_CASE),
        Regex("""(?:activa|apaga|enciende|prende)\s+(?:wifi|bluetooth|linterna|flashlight|nfc|hotspot)""", RegexOption.IGNORE_CASE),
        Regex("""(?:toma|haz una?)\s+(?:captura|foto|screenshot)""", RegexOption.IGNORE_CASE),
        Regex("""(?:bloquea|lock)\s+(?:el\s+)?(?:tel|cel|pantalla|screen)""", RegexOption.IGNORE_CASE),
        // Control de media
        Regex("""(?:pausa|parar|detener|stop|pause)\s+(?:música|canción|video|reproductor)""", RegexOption.IGNORE_CASE),
        Regex("""(?:siguiente|anterior|next|prev)\s+(?:canción|pista|track)""", RegexOption.IGNORE_CASE),
        Regex("""(?:reproduce|play|pon)\s+(?:música|canción|radio)""", RegexOption.IGNORE_CASE),
        // Temporizadores y alarmas simples
        Regex("""(?:pon|activa|crea)\s+(?:una?\s+)?(?:alarma|alerta)""", RegexOption.IGNORE_CASE),
        Regex("""(?:pon|inicia)\s+(?:un?\s+)?(?:timer|temporizador|cronómetro)""", RegexOption.IGNORE_CASE),
        // Pantalla y sistema
        Regex("""(?:apaga|prende|enciende|activa)\s+(?:la\s+)?(?:pantalla|linterna|torch)""", RegexOption.IGNORE_CASE),
        Regex("""(?:sube|baja|aumenta|reduce)\s+(?:el\s+)?(?:brillo|brightness)""", RegexOption.IGNORE_CASE),
        Regex("""(?:activa|desactiva|modo)\s+(?:no molestar|vibración|silencio|dnd)""", RegexOption.IGNORE_CASE),
        Regex("""(?:ir a|ve al?|show)\s+(?:inicio|home|pantalla principal)""", RegexOption.IGNORE_CASE),
        Regex("""(?:atrás|volver|back|regresa)""", RegexOption.IGNORE_CASE),
        Regex("""(?:toca|presiona|haz click)\s+(?:en\s+)?\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:escribe|write|type)\s+.{1,50}$""", RegexOption.IGNORE_CASE),
        // Recomendaciones / preguntas de info simple — respuesta 1 oración
        Regex("""(?:recomienda|sugiere|recomiéndame|sugiéreme|suggest)\s+\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:qué (?:canción|música|película|serie|libro|app|juego)|what (?:song|movie|show|book|game|app))""", RegexOption.IGNORE_CASE),
        Regex("""(?:cuál es (?:la mejor|el mejor|una buena|un buen)|which is the best)""", RegexOption.IGNORE_CASE),
        Regex("""(?:dime una?|tell me a?)\s+(?:canción|chiste|dato|fact|song)""", RegexOption.IGNORE_CASE),
        // Saludos/despedidas/confirmaciones ya van por IRIS, pero por si acaso
        Regex("""^(?:hola|hey|hi|gracias|ok|listo|adios|bye)\b""", RegexOption.IGNORE_CASE)
    )

    private val COMPLEX_INDICATORS = listOf(
        " y luego ", " después ", " también ", " además ", " mientras ",
        " and then ", " while ", "busca en", "manda el enlace", "checa en",
        " para que ", " cuando ", " después de ", " antes de "
    )

    fun classifyComplexity(input: String): CommandComplexity {
        val lower = input.lowercase()

        // TRIVIAL: coincide con patrón Y no tiene encadenamiento
        val hasChain = COMPLEX_INDICATORS.any { lower.contains(it) }
        if (!hasChain && TRIVIAL_PATTERNS.any { it.containsMatchIn(input) } && input.length < 100)
            return CommandComplexity.TRIVIAL

        val complexCount = COMPLEX_INDICATORS.count { lower.contains(it) }
        return when {
            complexCount >= 2 || input.length > 200 -> CommandComplexity.COMPLEX
            complexCount == 1 || input.length > 100 -> CommandComplexity.MODERATE
            else                                     -> CommandComplexity.SIMPLE
        }
    }

    // ── System prompt mínimo — delega al builder unificado ─────────────────────

    fun buildMinimalSystemPrompt(language: String, soul: String): String =
        SystemPromptBuilder.build(
            toolRegistry = ToolRegistry(),
            drivingMode  = false,
            language     = language,
            soul         = soul
        )

    // ── Compresión de historial ────────────────────────────────────────────────

    /**
     * Comprime mensajes de historial antiguos para reducir tokens.
     * Mantiene los últimos N mensajes completos y resume los anteriores.
     */
    fun compressHistory(
        messages: List<Message>,
        keepLast: Int = 6,
        maxTokensPerMessage: Int = 300
    ): List<Message> {
        if (messages.size <= keepLast) return messages.map { truncateMessage(it, maxTokensPerMessage) }

        val recent = messages.takeLast(keepLast)
        val old    = messages.dropLast(keepLast)

        // Crear un resumen de los mensajes antiguos
        val summaryContent = buildString {
            append("[Resumen de conversación anterior]\n")
            old.filter { it.role == "user" || it.role == "assistant" }
               .takeLast(10)
               .forEach { msg ->
                   val role   = if (msg.role == "user") "Usuario" else "Doey"
                   val content = msg.content.take(100).replace("\n", " ")
                   append("$role: $content\n")
               }
        }

        val summaryMsg = Message(role = "system", content = summaryContent)
        return listOf(summaryMsg) + recent.map { truncateMessage(it, maxTokensPerMessage) }
    }

    private fun truncateMessage(msg: Message, maxChars: Int): Message {
        if (msg.content.length <= maxChars) return msg
        // Mantener inicio y fin del mensaje (más informativo que solo el inicio)
        val half = maxChars / 2 - 10
        val truncated = "${msg.content.take(half)}\n...[truncado]...\n${msg.content.takeLast(half)}"
        return msg.copy(content = truncated)
    }

    // ── Deduplicación de tool results ─────────────────────────────────────────

    /**
     * Elimina tool results duplicados o muy similares para reducir tokens.
     */
    fun deduplicateToolResults(messages: List<Message>): List<Message> {
        val seen = mutableSetOf<String>()
        return messages.map { msg ->
            if (msg.role == "tool" && msg.content.length > 200) {
                val key = msg.content.take(50)
                if (seen.contains(key)) {
                    msg.copy(content = "[resultado duplicado omitido]")
                } else {
                    seen.add(key)
                    msg
                }
            } else msg
        }
    }

    // ── Estimación de tokens ───────────────────────────────────────────────────

    /**
     * Estimación aproximada de tokens (1 token ≈ 4 caracteres en inglés,
     * ~3.5 en español).
     */
    fun estimateTokens(text: String): Int = (text.length / 3.5).toInt()

    fun estimateMessageTokens(messages: List<Message>): Int =
        messages.sumOf { estimateTokens(it.content) + 4 } // +4 por overhead de rol

    // ── Selección de estrategia según complejidad ──────────────────────────────

    data class OptimizationStrategy(
        val useMinimalPrompt: Boolean,
        val maxHistoryMessages: Int,
        val maxIterations: Int,
        val compressHistory: Boolean,
        val includeSkills: Boolean,
        val includeTools: Boolean
    )

    // ── Compresión de árbol de accesibilidad ──────────────────────────────────

    /**
     * Comprime un árbol de accesibilidad largo para reducir tokens.
     * Estrategia:
     *  - Si el árbol tiene < 60 líneas: devolver tal cual
     *  - Si tiene 60-150 líneas: eliminar nodos sin atributos útiles
     *  - Si tiene > 150 líneas: mantener solo nodos interactivos + primeras/últimas líneas
     *
     * Ahorra hasta 80% de tokens en apps con UIs densas (Instagram, TikTok, etc.)
     */
    fun compressAccessibilityTree(tree: String, maxLines: Int = 80): String {
        val lines = tree.lines()
        if (lines.size <= maxLines) return tree

        // Filtrar: mantener líneas con atributos interesantes
        val interestingLines = lines.filter { line ->
            line.contains("clickable") || line.contains("editable") ||
            line.contains("scrollable") || line.contains("text=") ||
            line.contains("desc=") || line.contains("res-id=") ||
            line.contains("checked") || line.contains("[node_")
        }

        return if (interestingLines.size <= maxLines) {
            interestingLines.joinToString("\n") +
            "\n[árbol comprimido: ${lines.size}→${interestingLines.size} nodos]"
        } else {
            // Demasiado incluso filtrado: mantener inicio y fin
            val half = maxLines / 2
            val head = interestingLines.take(half)
            val tail = interestingLines.takeLast(half)
            (head + listOf("...[${interestingLines.size - maxLines} nodos omitidos]...") + tail)
                .joinToString("\n")
        }
    }

    // ── Estrategias de optimización ──────────────────────────────────────────

    fun getStrategy(complexity: CommandComplexity, userMaxIterations: Int): OptimizationStrategy {
        return when (complexity) {
            CommandComplexity.TRIVIAL -> OptimizationStrategy(
                useMinimalPrompt    = true,
                maxHistoryMessages  = 0,   // Sin historial para comandos triviales
                maxIterations       = 2,   // Máximo 2 iteraciones — 1 herramienta + confirmación
                compressHistory     = false,
                includeSkills       = false,
                includeTools        = true
            )
            CommandComplexity.SIMPLE -> OptimizationStrategy(
                useMinimalPrompt    = true,
                maxHistoryMessages  = 2,
                maxIterations       = 4,
                compressHistory     = false,
                includeSkills       = false,
                includeTools        = true
            )
            CommandComplexity.MODERATE -> OptimizationStrategy(
                useMinimalPrompt    = false,
                maxHistoryMessages  = 6,
                maxIterations       = minOf(userMaxIterations, 7),
                compressHistory     = true,
                includeSkills       = true,
                includeTools        = true
            )
            CommandComplexity.COMPLEX -> OptimizationStrategy(
                useMinimalPrompt    = false,
                maxHistoryMessages  = 10,
                maxIterations       = minOf(userMaxIterations, 8), // Cap en 8 — más no ayuda, solo gasta tokens
                compressHistory     = true,
                includeSkills       = true,
                includeTools        = true
            )
        }
    }
}
