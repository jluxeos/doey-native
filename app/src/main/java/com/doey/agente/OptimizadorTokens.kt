package com.doey.agente

import com.doey.llm.Message

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

    private val TRIVIAL_PATTERNS = listOf(
        Regex("""(?:qué hora|hora actual|what time)""", RegexOption.IGNORE_CASE),
        Regex("""(?:qué día|fecha actual|what day)""", RegexOption.IGNORE_CASE),
        Regex("""(?:batería|battery level)""", RegexOption.IGNORE_CASE),
        Regex("""(?:abre?|open|launch)\s+\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:llama a|call)\s+\w+""", RegexOption.IGNORE_CASE),
        Regex("""(?:pon|sube|baja)\s+(?:el\s+)?volumen""", RegexOption.IGNORE_CASE),
        Regex("""(?:activa|apaga|enciende)\s+(?:wifi|bluetooth|linterna|flashlight)""", RegexOption.IGNORE_CASE)
    )

    private val COMPLEX_INDICATORS = listOf(
        " y luego ", " después ", " también ", " además ", " mientras ",
        " and then ", " while ", "busca en", "manda el enlace", "checa en"
    )

    fun classifyComplexity(input: String): CommandComplexity {
        val lower = input.lowercase()

        if (TRIVIAL_PATTERNS.any { it.containsMatchIn(input) } && input.length < 50)
            return CommandComplexity.TRIVIAL

        val complexCount = COMPLEX_INDICATORS.count { lower.contains(it) }
        return when {
            complexCount >= 2 || input.length > 150 -> CommandComplexity.COMPLEX
            complexCount == 1 || input.length > 80  -> CommandComplexity.MODERATE
            else                                     -> CommandComplexity.SIMPLE
        }
    }

    // ── System prompt mínimo para comandos simples ─────────────────────────────

    fun buildMinimalSystemPrompt(language: String, soul: String): String {
        val lang = if (language.startsWith("es")) "Español" else language
        return buildString {
            append("Eres Doey, asistente Android. Idioma: $lang.\n")
            append("PARSER PURO: acción solicitada = herramienta inmediata. Sin texto antes de actuar.\n")
            append("Respuesta final: máximo 1 oración. Sin JSON ni tecnicismos.\n")
            if (soul.isNotBlank()) append("Tono: ${soul.take(120)}\n")
        }
    }

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
