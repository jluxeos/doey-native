package com.doey.agente

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.doey.tools.ToolResult
import com.doey.tools.successResult
import com.doey.tools.errorResult

/**
 * Procesador local de intenciones — Doey 23.4.9 Ultra (Tau Version)
 *
 * Motor de reconocimiento de patrones que resuelve comandos comunes sin consumir
 * tokens de IA. Solo escala al LLM cuando el comando es ambiguo, multi-paso
 * o requiere razonamiento real. Esto reduce el costo y la latencia drásticamente.
 *
 * Categorías soportadas de forma local:
 *  - Abrir apps por nombre
 *  - Llamar a contactos
 *  - Enviar mensajes (SMS / WhatsApp)
 *  - Control de volumen / brillo / WiFi / Bluetooth / linterna
 *  - Reproducir música en Spotify / YouTube Music
 *  - Temporizadores y alarmas simples
 *  - Navegación a lugares
 *  - Consultas de hora / fecha / batería
 */
object LocalIntentProcessor {

    // ── Resultado de clasificación ─────────────────────────────────────────────
    sealed class IntentClass {
        /** El procesador local puede manejar este comando completamente */
        data class Local(val action: LocalAction) : IntentClass()
        /** El comando es complejo; se deben extraer sub-tareas para la IA */
        data class Complex(val subtasks: List<String>) : IntentClass()
        /** El procesador no reconoce el patrón; delegar a IA normalmente */
        object Delegate : IntentClass()
    }

    sealed class LocalAction {
        data class OpenApp(val query: String) : LocalAction()
        data class Call(val contact: String) : LocalAction()
        data class SendSms(val contact: String, val message: String) : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String) : LocalAction()
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        data class SetVolume(val level: Int) : LocalAction()
        data class ToggleWifi(val enable: Boolean) : LocalAction()
        data class ToggleBluetooth(val enable: Boolean) : LocalAction()
        data class ToggleFlashlight(val enable: Boolean) : LocalAction()
        data class Navigate(val destination: String) : LocalAction()
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "") : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        data class QueryInfo(val type: InfoType) : LocalAction()
    }

    enum class InfoType { TIME, DATE, BATTERY, WEATHER }

    // ── Patrones de reconocimiento ─────────────────────────────────────────────

    private val OPEN_PATTERNS = listOf(
        Regex("""(?:abre?|lanza|inicia|entra a|ve a)\s+(?:la\s+app\s+)?(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:open|launch|start)\s+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val CALL_PATTERNS = listOf(
        Regex("""(?:llama(?:r)?(?: a)?|llama a|call)\s+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val SMS_PATTERNS = listOf(
        Regex("""(?:manda|envía|escribe|send)\s+(?:un\s+)?(?:mensaje|sms|texto)\s+(?:a\s+)?(.+?)\s+(?:diciendo|que diga|que dice|:|con el texto)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:manda|envía)\s+(?:sms|mensaje)\s+a\s+(.+?)[:\s]+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val WHATSAPP_PATTERNS = listOf(
        Regex("""(?:manda|envía|escribe|send)\s+(?:un\s+)?(?:mensaje|whatsapp|wha?ts?)\s+(?:a\s+)?(.+?)\s+(?:por\s+whatsapp\s+)?(?:diciendo|que diga|que dice|:|con el texto)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:manda|envía)\s+(?:por\s+)?whatsapp\s+(?:a\s+)?(.+?)[:\s]+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val MUSIC_PATTERNS = listOf(
        Regex("""(?:pon|reproduce|play|toca)\s+(?:la\s+canción\s+)?['""]?(.+?)['""]?\s+(?:en|on)\s+(spotify|youtube music|apple music|deezer|tidal)""", RegexOption.IGNORE_CASE),
        Regex("""(?:pon|reproduce|play|toca)\s+(?:música|music)?\s*(?:en\s+)?(spotify|youtube music)""", RegexOption.IGNORE_CASE),
        Regex("""(?:pon|reproduce|play|toca)\s+['""]?(.+?)['""]?\s+(?:de\s+.+?)?\s+en\s+(spotify|youtube\s*music|yt\s*music)""", RegexOption.IGNORE_CASE)
    )

    private val VOLUME_PATTERNS = listOf(
        Regex("""(?:sube|baja|pon|set|ajusta)\s+(?:el\s+)?volumen\s+(?:a\s+)?(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""volumen\s+(?:a\s+)?(\d+)%?""", RegexOption.IGNORE_CASE)
    )

    private val WIFI_PATTERNS = listOf(
        Regex("""(?:activa|enciende|prende|turn on|enable)\s+(?:el\s+)?(?:wifi|wi-fi)""", RegexOption.IGNORE_CASE),
        Regex("""(?:desactiva|apaga|turn off|disable)\s+(?:el\s+)?(?:wifi|wi-fi)""", RegexOption.IGNORE_CASE)
    )

    private val NAVIGATE_PATTERNS = listOf(
        Regex("""(?:llévame a|navega a|cómo llego a|cómo voy a|navigate to|directions to)\s+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val ALARM_PATTERNS = listOf(
        Regex("""(?:pon|pone|set|crea)\s+(?:una\s+)?alarma\s+(?:a\s+las?\s+|para\s+las?\s+)?(\d{1,2})(?::(\d{2}))?""", RegexOption.IGNORE_CASE)
    )

    private val TIMER_PATTERNS = listOf(
        Regex("""(?:pon|pone|set|crea)\s+(?:un\s+)?(?:temporizador|timer)\s+(?:de\s+)?(\d+)\s*(minutos?|segundos?|horas?)""", RegexOption.IGNORE_CASE)
    )

    private val INFO_PATTERNS = mapOf(
        InfoType.TIME    to Regex("""(?:qué hora es|dime la hora|what time is it|hora actual)""", RegexOption.IGNORE_CASE),
        InfoType.DATE    to Regex("""(?:qué día es|qué fecha es|what day is it|fecha actual)""", RegexOption.IGNORE_CASE),
        InfoType.BATTERY to Regex("""(?:cuánta batería|nivel de batería|battery level|battery status)""", RegexOption.IGNORE_CASE)
    )

    // Indicadores de comandos complejos multi-paso
    private val COMPLEX_CONNECTORS = listOf(
        " y luego ", " después ", " y después ", " también ", " además ",
        " and then ", " while you ", " mientras ", " al mismo tiempo "
    )

    private val COMPLEX_APPS = listOf(
        "youtube", "didi", "uber", "rappi", "mercado libre", "amazon", "netflix"
    )

    // ── Clasificación principal ────────────────────────────────────────────────

    fun classify(input: String): IntentClass {
        val text = input.trim()

        // 1. Detectar comandos complejos multi-paso
        val lowerText = text.lowercase()
        val isComplex = COMPLEX_CONNECTORS.any { lowerText.contains(it) }
            && (lowerText.length > 80 || COMPLEX_APPS.any { lowerText.contains(it) })

        if (isComplex) {
            val subtasks = splitIntoSubtasks(text)
            if (subtasks.size > 1) return IntentClass.Complex(subtasks)
        }

        // 2. Intentar resolver localmente
        tryLocal(text)?.let { return IntentClass.Local(it) }

        // 3. Delegar a IA
        return IntentClass.Delegate
    }

    private fun tryLocal(text: String): LocalAction? {
        // Información rápida
        INFO_PATTERNS.forEach { (type, pattern) ->
            if (pattern.containsMatchIn(text)) return LocalAction.QueryInfo(type)
        }

        // WhatsApp (antes que SMS para evitar falsos positivos)
        WHATSAPP_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val contact = m.groupValues.getOrElse(1) { "" }.trim()
                val message = m.groupValues.getOrElse(2) { "" }.trim()
                if (contact.isNotBlank() && message.isNotBlank())
                    return LocalAction.SendWhatsApp(contact, message)
            }
        }

        // SMS
        SMS_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val contact = m.groupValues.getOrElse(1) { "" }.trim()
                val message = m.groupValues.getOrElse(2) { "" }.trim()
                if (contact.isNotBlank() && message.isNotBlank())
                    return LocalAction.SendSms(contact, message)
            }
        }

        // Llamadas
        CALL_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val contact = m.groupValues.getOrElse(1) { "" }.trim()
                if (contact.isNotBlank()) return LocalAction.Call(contact)
            }
        }

        // Música
        MUSIC_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val query = m.groupValues.getOrElse(1) { "" }.trim()
                val app   = m.groupValues.getOrElse(2) { "spotify" }.trim().lowercase()
                if (query.isNotBlank()) return LocalAction.PlayMusic(query, app)
            }
        }

        // Volumen
        VOLUME_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val level = m.groupValues.getOrElse(1) { "50" }.toIntOrNull() ?: 50
                return LocalAction.SetVolume(level.coerceIn(0, 100))
            }
        }

        // WiFi
        val wifiLower = text.lowercase()
        if (Regex("""(?:activa|enciende|prende|turn on|enable)\s+(?:el\s+)?(?:wifi|wi-fi)""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleWifi(true)
        if (Regex("""(?:desactiva|apaga|turn off|disable)\s+(?:el\s+)?(?:wifi|wi-fi)""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleWifi(false)

        // Bluetooth
        if (Regex("""(?:activa|enciende|turn on)\s+(?:el\s+)?bluetooth""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleBluetooth(true)
        if (Regex("""(?:desactiva|apaga|turn off)\s+(?:el\s+)?bluetooth""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleBluetooth(false)

        // Linterna
        if (Regex("""(?:enciende|prende|activa|turn on)\s+(?:la\s+)?linterna""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleFlashlight(true)
        if (Regex("""(?:apaga|desactiva|turn off)\s+(?:la\s+)?linterna""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            return LocalAction.ToggleFlashlight(false)

        // Navegación
        NAVIGATE_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val dest = m.groupValues.getOrElse(1) { "" }.trim()
                if (dest.isNotBlank()) return LocalAction.Navigate(dest)
            }
        }

        // Alarma
        ALARM_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val hour   = m.groupValues.getOrElse(1) { "0" }.toIntOrNull() ?: 0
                val minute = m.groupValues.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                return LocalAction.SetAlarm(hour, minute)
            }
        }

        // Timer
        TIMER_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val amount = m.groupValues.getOrElse(1) { "1" }.toLongOrNull() ?: 1L
                val unit   = m.groupValues.getOrElse(2) { "minutos" }.lowercase()
                val secs   = when {
                    unit.startsWith("segundo") -> amount
                    unit.startsWith("hora")    -> amount * 3600L
                    else                       -> amount * 60L
                }
                return LocalAction.SetTimer(secs)
            }
        }

        // Abrir app (al final para no capturar comandos más específicos)
        OPEN_PATTERNS.forEach { pattern ->
            pattern.find(text)?.let { m ->
                val appName = m.groupValues.getOrElse(1) { "" }.trim()
                if (appName.isNotBlank() && appName.length < 30)
                    return LocalAction.OpenApp(appName)
            }
        }

        return null
    }

    // ── Separador de sub-tareas para comandos complejos ────────────────────────

    private fun splitIntoSubtasks(text: String): List<String> {
        // Separar por conectores temporales/aditivos manteniendo el contexto
        val connectors = listOf(
            Regex("""\s+y luego\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+después\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+y después\s+""", RegexOption.IGNORE_CASE),
            Regex(""",\s+también\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+también\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+además\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+pero\s+""", RegexOption.IGNORE_CASE)
        )

        var parts = listOf(text)
        for (connector in connectors) {
            parts = parts.flatMap { part ->
                connector.split(part).map { it.trim() }.filter { it.isNotBlank() }
            }
        }
        return parts.filter { it.length > 5 }
    }

    // ── Resolución de contactos ────────────────────────────────────────────────

    fun resolveContactNumber(context: Context, nameOrNumber: String): String? {
        // Si ya es un número, devolverlo directamente
        if (nameOrNumber.matches(Regex("""[\+\d\s\-\(\)]{7,}"""))) return nameOrNumber

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameOrNumber%"),
            null
        ) ?: return null

        return cursor.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else null
        }
    }

    // ── Generación de prompt optimizado para IA ────────────────────────────────

    /**
     * Para comandos complejos, genera un prompt comprimido que describe las sub-tareas
     * de forma estructurada, reduciendo tokens y mejorando la precisión.
     */
    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String {
        if (subtasks.size <= 1) return originalInput

        val sb = StringBuilder()
        sb.append("Ejecuta estas tareas en orden:\n")
        subtasks.forEachIndexed { i, task ->
            sb.append("${i + 1}. $task\n")
        }
        sb.append("\nEjecútalas secuencialmente. Confirma cada una antes de continuar.")
        return sb.toString()
    }
}
