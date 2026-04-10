package com.doey.agente

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

// ── Tipos de entrada de log ───────────────────────────────────────────────────
enum class LogType {
    USER_INPUT,          // Lo que escribe/dice el usuario
    AI_REQUEST,          // Lo que se envía a la IA
    AI_RESPONSE,         // Lo que responde la IA
    TOOL_CALL,           // Herramienta que llama la IA
    TOOL_RESULT,         // Resultado de la herramienta
    SKILL_LOAD,          // Skill que se carga/usa
    ACCESSIBILITY_SEND,  // Acción enviada al servicio de accesibilidad
    ACCESSIBILITY_RECV,  // Resultado recibido del servicio de accesibilidad
    PIPELINE_STATE,      // Cambio de estado del pipeline
    ERROR,               // Error
    INFO                 // Información general
}

data class LogEntry(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val title: String,
    val detail: String = "",
    val isExpanded: Boolean = false
) {
    val formattedTime: String get() {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    val typeLabel: String get() = when (type) {
        LogType.USER_INPUT          -> "USUARIO"
        LogType.AI_REQUEST          -> "→ IA"
        LogType.AI_RESPONSE         -> "← IA"
        LogType.TOOL_CALL           -> "HERRAMIENTA"
        LogType.TOOL_RESULT         -> "RESULTADO"
        LogType.SKILL_LOAD          -> "SKILL"
        LogType.ACCESSIBILITY_SEND  -> "→ PANTALLA"
        LogType.ACCESSIBILITY_RECV  -> "← PANTALLA"
        LogType.PIPELINE_STATE      -> "ESTADO"
        LogType.ERROR               -> "ERROR"
        LogType.INFO                -> "INFO"
    }

    val typeColor: Long get() = when (type) {
        LogType.USER_INPUT          -> 0xFF1565C0L  // Azul oscuro
        LogType.AI_REQUEST          -> 0xFF6A1B9AL  // Morado
        LogType.AI_RESPONSE         -> 0xFF2E7D32L  // Verde
        LogType.TOOL_CALL           -> 0xFFE65100L  // Naranja
        LogType.TOOL_RESULT         -> 0xFF00838FL  // Cyan
        LogType.SKILL_LOAD          -> 0xFF558B2FL  // Verde oliva
        LogType.ACCESSIBILITY_SEND  -> 0xFFC62828L  // Rojo
        LogType.ACCESSIBILITY_RECV  -> 0xFF4527A0L  // Morado oscuro
        LogType.PIPELINE_STATE      -> 0xFF37474FL  // Gris azulado
        LogType.ERROR               -> 0xFFB71C1CL  // Rojo oscuro
        LogType.INFO                -> 0xFF546E7AL  // Gris
    }
}

// ── Singleton de logger ───────────────────────────────────────────────────────
object DoeyLogger {
    private const val TAG = "DoeyLogger"
    private const val MAX_ENTRIES = 500

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val idCounter = AtomicLong(0)

    fun log(type: LogType, title: String, detail: String = "") {
        val entry = LogEntry(
            id     = idCounter.getAndIncrement(),
            type   = type,
            title  = title,
            detail = detail
        )
        val current = _entries.value.toMutableList()
        current.add(entry)
        // Mantener máximo de entradas
        if (current.size > MAX_ENTRIES) {
            current.removeAt(0)
        }
        _entries.value = current
        // También loguear en Logcat para depuración
        Log.d(TAG, "[${entry.typeLabel}] $title${if (detail.isNotBlank()) " | $detail" else ""}")
    }

    fun clear() {
        _entries.value = emptyList()
    }

    // ── Helpers de conveniencia ───────────────────────────────────────────────
    fun userInput(text: String) =
        log(LogType.USER_INPUT, "Mensaje del usuario", text)

    fun aiRequest(model: String, messageCount: Int, systemPromptLen: Int) =
        log(LogType.AI_REQUEST, "Enviando a IA ($model)",
            "Mensajes en contexto: $messageCount | System prompt: $systemPromptLen chars")

    fun aiResponse(content: String, toolCallCount: Int, finishReason: String) {
        val preview = if (content.length > 200) content.take(200) + "…" else content
        log(LogType.AI_RESPONSE, "Respuesta de IA (finish: $finishReason, tools: $toolCallCount)",
            preview)
    }

    fun toolCall(toolName: String, args: Map<String, Any?>) {
        val argsStr = args.entries.joinToString(", ") { (k, v) ->
            val valStr = v?.toString()?.take(100) ?: "null"
            "$k=$valStr"
        }
        log(LogType.TOOL_CALL, "Llamando herramienta: $toolName", argsStr)
    }

    fun toolResult(toolName: String, result: String, isError: Boolean) {
        val preview = if (result.length > 300) result.take(300) + "…" else result
        log(LogType.TOOL_RESULT,
            if (isError) "Error en $toolName" else "Resultado de $toolName",
            preview)
    }

    fun skillLoad(skillName: String) =
        log(LogType.SKILL_LOAD, "Cargando skill: $skillName")

    fun accessibilitySend(action: String, nodeId: String?, extra: String?) {
        val detail = buildString {
            append("Acción: $action")
            if (!nodeId.isNullOrBlank()) append(" | Nodo: $nodeId")
            if (!extra.isNullOrBlank()) append(" | Extra: ${extra.take(80)}")
        }
        log(LogType.ACCESSIBILITY_SEND, "Acción de pantalla: $action", detail)
    }

    fun accessibilityRecv(action: String, result: String) {
        val preview = if (result.length > 300) result.take(300) + "…" else result
        log(LogType.ACCESSIBILITY_RECV, "Resultado de pantalla ($action)", preview)
    }

    fun pipelineState(from: String, to: String) =
        log(LogType.PIPELINE_STATE, "Estado: $from → $to")

    fun error(context: String, message: String) =
        log(LogType.ERROR, "Error en $context", message)

    fun info(message: String, detail: String = "") =
        log(LogType.INFO, message, detail)
}