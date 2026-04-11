package com.doey.agente

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * IRIS — Intent Recognition & Interpretation System
 * Doey · versión robusta
 *
 * Principios de diseño:
 *  1. SOLO actúa si el comando es una instrucción directa sin ambigüedad.
 *  2. Si la frase contiene contexto narrativo, condicional o explicativo → Delegate.
 *  3. Cada patrón exige que el verbo de acción esté al inicio (o muy cerca).
 *  4. Si el objeto mencionado es externo al dispositivo (carro, casa, TV) → Delegate.
 *  5. Solo ejecuta acciones que Android puede realizar directamente.
 */
object LocalIntentProcessor {

    // ─────────────────────────────────────────────────────────────────────────
    // Tipos de resultado
    // ─────────────────────────────────────────────────────────────────────────

    sealed class IntentClass {
        data class Local(val action: LocalAction) : IntentClass()
        data class Complex(val subtasks: List<String>) : IntentClass()
        object Delegate : IntentClass()
    }

    sealed class LocalAction {
        data class ToggleFlashlight(val enable: Boolean) : LocalAction()
        data class SetVolume(val level: Int, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class VolumeStep(val up: Boolean, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class SetSilentMode(val mode: SilentMode) : LocalAction()
        data class ToggleWifi(val enable: Boolean) : LocalAction()
        data class ToggleBluetooth(val enable: Boolean) : LocalAction()
        data class ToggleAirplane(val enable: Boolean) : LocalAction()
        data class ToggleDoNotDisturb(val enable: Boolean) : LocalAction()
        data class SetBrightness(val level: Int) : LocalAction()
        data class BrightnessStep(val up: Boolean) : LocalAction()
        data class ToggleAutoBrightness(val enable: Boolean) : LocalAction()
        class TakeScreenshot : LocalAction()
        class LockScreen : LocalAction()
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class CancelAlarm : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class CancelTimer : LocalAction()
        data class Call(val contact: String) : LocalAction()
        data class CallEmergency(val number: String) : LocalAction()
        data class SendSms(val contact: String, val message: String) : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String) : LocalAction()
        data class SendTelegram(val contact: String, val message: String) : LocalAction()
        data class OpenWhatsAppChat(val contact: String) : LocalAction()
        data class OpenApp(val query: String) : LocalAction()
        data class Navigate(val destination: String) : LocalAction()
        data class SearchWeb(val query: String) : LocalAction()
        data class SearchMaps(val query: String) : LocalAction()
        data class OpenUrl(val url: String) : LocalAction()
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        class PauseMusic : LocalAction()
        class ResumeMusic : LocalAction()
        class NextTrack : LocalAction()
        class PrevTrack : LocalAction()
        data class QueryInfo(val type: InfoType) : LocalAction()
    }

    enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode { SILENT, VIBRATE, NORMAL }
    enum class InfoType { TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS }

    // ─────────────────────────────────────────────────────────────────────────
    // Guardas: estas condiciones hacen Delegate inmediato
    // ─────────────────────────────────────────────────────────────────────────

    // Prefijos que indican pregunta/consulta/explicación → NO es una orden
    private val QUESTION_PREFIXES = Regex(
        "^(como|como se|como puedo|como hago|como funciona|como se usa|como activo|" +
        "que es|que hace|que significa|cual es|cuando|cuanto|cuantos|cuantas|" +
        "por que|para que|sabes|puedes decirme|podrias decirme|dime como|" +
        "explica|explicame|necesito saber|quiero saber|me puedes|es posible|" +
        "hay alguna|existe|what|how|why|when|where|is there|can you|tell me how)",
        RegexOption.IGNORE_CASE
    )

    // Contexto externo al dispositivo → no podemos ejecutarlo
    private val EXTERNAL_CONTEXT = Regex(
        "\\b(del carro|del auto|del coche|del vehiculo|de mi carro|de mi auto|" +
        "del televisor|del tv|de la tele|del ordenador|del computador|de la computadora|" +
        "de la lampara|del foco|del switch|del interruptor|de casa|del cuarto|" +
        "del router|del modem|en el carro|en mi carro|del edificio|de la ciudad)",
        RegexOption.IGNORE_CASE
    )

    // Conectores que crean multi-tarea real
    private val MULTI_TASK = Regex(
        "\\b(y luego|y despues|y tambien|y ademas|after that|and then|al mismo tiempo|mientras tanto)",
        RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Normalización
    // ─────────────────────────────────────────────────────────────────────────

    private fun normalize(text: String): String = text.trim()
        .replace(Regex("[¿¡.,;!?]+"), " ")
        .replace('á','a').replace('é','e').replace('í','i')
        .replace('ó','o').replace('ú','u').replace('ü','u')
        .replace('Á','A').replace('É','E').replace('Í','I')
        .replace('Ó','O').replace('Ú','U')
        .replace(Regex("\\s+"), " ")
        .trim()

    // ─────────────────────────────────────────────────────────────────────────
    // Clasificador principal
    // ─────────────────────────────────────────────────────────────────────────

    fun classify(input: String): IntentClass {
        val lo = normalize(input).lowercase()

        // 1. Pregunta/consulta → siempre IA
        if (QUESTION_PREFIXES.containsMatchIn(lo)) return IntentClass.Delegate

        // 2. Contexto de objeto externo → IA
        if (EXTERNAL_CONTEXT.containsMatchIn(lo)) return IntentClass.Delegate

        // 3. Multi-tarea → Complex
        if (MULTI_TASK.containsMatchIn(lo)) {
            val parts = splitSubtasks(input)
            if (parts.size >= 2) return IntentClass.Complex(parts)
        }

        // 4. Intentar resolver localmente
        tryLocal(lo)?.let { return IntentClass.Local(it) }

        return IntentClass.Delegate
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Motor de matching — orden importa (más específico primero)
    // ─────────────────────────────────────────────────────────────────────────

    private fun tryLocal(lo: String): LocalAction? =
        matchFlashlight(lo)
            ?: matchVolume(lo)
            ?: matchSilent(lo)
            ?: matchBrightness(lo)
            ?: matchWifi(lo)
            ?: matchBluetooth(lo)
            ?: matchAirplane(lo)
            ?: matchDoNotDisturb(lo)
            ?: matchScreenshot(lo)
            ?: matchLock(lo)
            ?: matchAlarm(lo)
            ?: matchTimer(lo)
            ?: matchEmergencyCall(lo)
            ?: matchCall(lo)
            ?: matchWhatsApp(lo)
            ?: matchTelegram(lo)
            ?: matchSms(lo)
            ?: matchNavigation(lo)
            ?: matchWebSearch(lo)
            ?: matchMapsSearch(lo)
            ?: matchMediaControl(lo)
            ?: matchMusic(lo)
            ?: matchUrl(lo)
            ?: matchSystemQuery(lo)
            ?: matchOpenApp(lo)

    // ─── Linterna ────────────────────────────────────────────────────────────

    private fun matchFlashlight(lo: String): LocalAction? {
        val ON  = Regex("\\b(enciende|prende|activa|turn on|on)\\b.*(linterna|flash|torch|luz trasera)")
        val OFF = Regex("\\b(apaga|desactiva|apagar|turn off|off)\\b.*(linterna|flash|torch|luz trasera)")
        // Solo "linterna" = encender
        val SOLO = Regex("^linterna$")
        return when {
            ON.containsMatchIn(lo)   -> LocalAction.ToggleFlashlight(true)
            OFF.containsMatchIn(lo)  -> LocalAction.ToggleFlashlight(false)
            SOLO.matches(lo)         -> LocalAction.ToggleFlashlight(true)
            else                     -> null
        }
    }

    // ─── Volumen ─────────────────────────────────────────────────────────────

    private fun matchVolume(lo: String): LocalAction? {
        val UP   = Regex("\\b(sube|aumenta|incrementa|raise|turn up|volume up|mas volumen)\\b.*(volumen|volume|sonido|audio)?")
        val DOWN = Regex("\\b(baja|reduce|disminuye|lower|turn down|volume down|menos volumen)\\b.*(volumen|volume|sonido|audio)?")
        // Pero solo si la frase empieza cerca del comando — evitar "necesito subir el volumen de mi tele"
        if (UP.containsMatchIn(lo) && !lo.contains("tele") && !lo.contains("tv"))
            return LocalAction.VolumeStep(up = true)
        if (DOWN.containsMatchIn(lo) && !lo.contains("tele") && !lo.contains("tv"))
            return LocalAction.VolumeStep(up = false)

        // Valor exacto: "pon el volumen a 70" / "volumen 70%" / "volumen al 70"
        Regex("\\b(volumen|volume|sonido)\\b[^\\d]*(\\d{1,3})\\s*%?").find(lo)?.let {
            val lvl = it.groupValues[2].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetVolume(lvl)
        }
        Regex("\\b(pon|ajusta|set|coloca)\\b[^\\d]*(\\d{1,3})\\s*%?[^\\w]*(volumen|volume|sonido)").find(lo)?.let {
            val lvl = it.groupValues[2].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetVolume(lvl)
        }
        return null
    }

    // ─── Silencio / Vibración ────────────────────────────────────────────────

    private fun matchSilent(lo: String): LocalAction? = when {
        Regex("\\b(silencia(r)?|mute|pon.*silencio|modo silencio|silent mode|poner.*mudo)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.SILENT)
        Regex("\\b(vibra(cion)?|vibrate|modo vibracion|pon.*vibracion)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.VIBRATE)
        Regex("\\b(quita.*silencio|desmutea|unmute|sonido normal|modo normal|modo sonido|activa.*sonido)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.NORMAL)
        else -> null
    }

    // ─── Brillo ──────────────────────────────────────────────────────────────

    private fun matchBrightness(lo: String): LocalAction? {
        if (Regex("\\b(activa|pon|usa|enable)\\b.*(brillo automatico|auto.?brillo|adaptive brightness)").containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(true)
        if (Regex("\\b(desactiva|quita|disable)\\b.*(brillo automatico|auto.?brillo|adaptive brightness)").containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(false)
        if (Regex("\\b(sube|aumenta|raise|turn up)\\b.*(brillo|brightness)").containsMatchIn(lo))
            return LocalAction.BrightnessStep(up = true)
        if (Regex("\\b(baja|reduce|lower|turn down)\\b.*(brillo|brightness)").containsMatchIn(lo))
            return LocalAction.BrightnessStep(up = false)
        Regex("\\b(brillo|brightness)\\b[^\\d]*(\\d{1,3})\\s*%?").find(lo)?.let {
            val lvl = it.groupValues[2].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetBrightness(lvl)
        }
        return null
    }

    // ─── WiFi ─────────────────────────────────────────────────────────────────

    private fun matchWifi(lo: String): LocalAction? {
        val wifi = "\\b(wifi|wi.?fi|internet inalambrico|red inalambrica)\\b"
        return when {
            Regex("\\b(activa|enciende|prende|conecta|turn on|enable)\\b.*$wifi").containsMatchIn(lo) ||
            Regex("$wifi.*\\b(on|activo)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleWifi(true)
            Regex("\\b(desactiva|apaga|desconecta|turn off|disable)\\b.*$wifi").containsMatchIn(lo) ||
            Regex("$wifi.*\\b(off|apagado)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleWifi(false)
            else -> null
        }
    }

    // ─── Bluetooth ───────────────────────────────────────────────────────────

    private fun matchBluetooth(lo: String): LocalAction? = when {
        Regex("\\b(activa|enciende|prende|turn on|enable)\\b.*bluetooth").containsMatchIn(lo) ||
        Regex("bluetooth.*\\b(on|activo)\\b").containsMatchIn(lo)
            -> LocalAction.ToggleBluetooth(true)
        Regex("\\b(desactiva|apaga|turn off|disable)\\b.*bluetooth").containsMatchIn(lo) ||
        Regex("bluetooth.*\\b(off|apagado)\\b").containsMatchIn(lo)
            -> LocalAction.ToggleBluetooth(false)
        else -> null
    }

    // ─── Modo avión ───────────────────────────────────────────────────────────

    private fun matchAirplane(lo: String): LocalAction? {
        val air = "\\b(modo avion|airplane mode|flight mode|modo vuelo)\\b"
        return when {
            Regex("\\b(activa|enciende|pon|turn on)\\b.*($air)").containsMatchIn(lo) ||
            Regex("($air).*\\b(on|activo)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(true)
            Regex("\\b(desactiva|apaga|quita|turn off)\\b.*($air)").containsMatchIn(lo) ||
            Regex("($air).*\\b(off|apagado)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(false)
            else -> null
        }
    }

    // ─── No molestar ──────────────────────────────────────────────────────────

    private fun matchDoNotDisturb(lo: String): LocalAction? {
        val dnd = "\\b(no molestar|dnd|do not disturb|no interrumpir|no interrupciones)\\b"
        return when {
            Regex("\\b(activa|enciende|pon|enable)\\b.*($dnd)").containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(true)
            Regex("\\b(desactiva|apaga|quita|disable)\\b.*($dnd)").containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(false)
            else -> null
        }
    }

    // ─── Captura / Bloqueo ────────────────────────────────────────────────────

    private fun matchScreenshot(lo: String): LocalAction? {
        return if (Regex("\\b(toma|saca|haz|captura|take)\\b.*(captura|screenshot|pantalla|screen shot|foto de pantalla)").containsMatchIn(lo))
            LocalAction.TakeScreenshot() else null
    }

    private fun matchLock(lo: String): LocalAction? {
        return if (Regex("^(bloquea(r)?( el telefono| la pantalla)?|lock( the)? (screen|phone)|apaga(r)? la pantalla)$").containsMatchIn(lo))
            LocalAction.LockScreen() else null
    }

    // ─── Alarmas ──────────────────────────────────────────────────────────────

    private fun matchAlarm(lo: String): LocalAction? {
        // Cancelar
        if (Regex("\\b(cancela|elimina|borra|quita|cancel|delete)\\b.*(alarma|alarm|despertador)").containsMatchIn(lo))
            return LocalAction.CancelAlarm()

        // Requiere trigger explícito para no capturar frases como "a las 7 tengo reunión"
        val hasTrigger = Regex("\\b(pon|pone|ponme|set|crea|activa|programa|despiertame|despertarme|wake me|alarma|alarm|despertador)\\b").containsMatchIn(lo)
        if (!hasTrigger) return null

        // HH:MM con AM/PM opcional
        Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm|a\\.m|p\\.m)?").find(lo)?.let { m ->
            var h  = m.groupValues[1].toIntOrNull() ?: return null
            val mi = m.groupValues[2].toIntOrNull() ?: return null
            val ap = m.groupValues[3].lowercase()
            if (ap.startsWith("p") && h < 12) h += 12
            if (ap.startsWith("a") && h == 12) h = 0
            return LocalAction.SetAlarm(h, mi, extractLabel(lo))
        }

        // "a las N am/pm / de la mañana / tarde / noche"
        Regex("(?:a las?|at)\\s*(\\d{1,2})\\s*(am|pm|de la manana|de la tarde|de la noche|hrs?|h\\b)?").find(lo)?.let { m ->
            var h   = m.groupValues[1].toIntOrNull() ?: return null
            val mod = m.groupValues[2].lowercase()
            when {
                mod.contains("pm") || mod.contains("tarde") || mod.contains("noche") -> if (h < 12) h += 12
                mod.contains("am") || mod.contains("mana") -> if (h == 12) h = 0
            }
            return LocalAction.SetAlarm(h, 0, extractLabel(lo))
        }

        // "en N horas" → alarma relativa
        Regex("en (\\d+) hora").find(lo)?.let { m ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.HOUR_OF_DAY, m.groupValues[1].toInt())
            return LocalAction.SetAlarm(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }

        return null
    }

    private fun extractLabel(lo: String): String =
        Regex("(?:llamada|etiqueta|label|para|titulada?)[ :]+(.{3,30})").find(lo)?.groupValues?.get(1)?.trim() ?: ""

    // ─── Temporizador ─────────────────────────────────────────────────────────

    private fun matchTimer(lo: String): LocalAction? {
        if (Regex("\\b(cancela|detiene|para|stop|cancel)\\b.*(temporizador|timer|cuenta regresiva)").containsMatchIn(lo))
            return LocalAction.CancelTimer()

        if (!Regex("\\b(pon|pone|ponme|set|crea|inicia|activa|temporizador|timer|cuenta regresiva|countdown)\\b").containsMatchIn(lo))
            return null

        var secs = 0L
        Regex("(\\d+)\\s*hora").find(lo)?.let { secs += it.groupValues[1].toLong() * 3600 }
        Regex("(\\d+)\\s*(minuto|min\\b)").find(lo)?.let { secs += it.groupValues[1].toLong() * 60 }
        Regex("(\\d+)\\s*(segundo|seg\\b|sec\\b)").find(lo)?.let { secs += it.groupValues[1].toLong() }

        return if (secs > 0) LocalAction.SetTimer(secs) else null
    }

    // ─── Llamadas de emergencia ───────────────────────────────────────────────

    private fun matchEmergencyCall(lo: String): LocalAction? {
        if (!Regex("\\b(llama(r)?|call|marca|dial)\\b").containsMatchIn(lo)) return null
        return when {
            lo.contains("911")        -> LocalAction.CallEmergency("911")
            lo.contains("112")        -> LocalAction.CallEmergency("112")
            lo.contains("066")        -> LocalAction.CallEmergency("066")
            lo.contains("ambulancia") -> LocalAction.CallEmergency("112")
            lo.contains("bomberos")   -> LocalAction.CallEmergency("911")
            lo.contains("policia")    -> LocalAction.CallEmergency("911")
            else -> null
        }
    }

    // ─── Llamada normal ───────────────────────────────────────────────────────

    private fun matchCall(lo: String): LocalAction? {
        // Patrón estricto: el comando debe ser básicamente solo "llama a CONTACTO"
        val m = Regex("^(llama(r)?(?: a)?|call|marca(r)?(?: a)?|comunicame con|conectame con)\\s+(.{2,40})$").find(lo)
            ?: return null
        val contact = m.groupValues.last().trim()
        // Rechazar si parece que hay más contexto (demasiadas palabras = probablemente frase compleja)
        if (contact.split(" ").size > 5) return null
        return LocalAction.Call(contact)
    }

    // ─── WhatsApp ─────────────────────────────────────────────────────────────

    private fun matchWhatsApp(lo: String): LocalAction? {
        if (!lo.contains("whatsapp") && !lo.contains("whats") && !lo.contains("wapp")) return null

        // Enviar mensaje
        Regex("^(?:manda|envia|escribe|send)\\s+(?:un\\s+)?(?:mensaje|whatsapp|wha?ts?)\\s+(?:a\\s+)?(.+?)\\s+(?:por\\s+whatsapp\\s+)?(?:diciendo|que diga|que dice|:|con el texto|con mensaje)\\s+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }
        Regex("^(?:manda|envia)\\s+(?:por\\s+)?whatsapp\\s+(?:a\\s+)?(.+?)[: ]+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }
        // Solo abrir chat
        Regex("^(?:abre|abrir|chatea con|chatear con|open)\\s+(?:whatsapp\\s+(?:con|de)\\s+|chat de whatsapp con\\s+)(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim()
            if (c.isNotBlank()) return LocalAction.OpenWhatsAppChat(c)
        }
        return null
    }

    // ─── Telegram ────────────────────────────────────────────────────────────

    private fun matchTelegram(lo: String): LocalAction? {
        if (!lo.contains("telegram")) return null
        Regex("^(?:manda|envia|escribe|send)\\s+(?:un\\s+)?(?:mensaje|telegram)\\s+(?:a\\s+)?(.+?)\\s+(?:por\\s+telegram\\s+)?(?:diciendo|que diga|que dice|:|con el texto)\\s+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendTelegram(c, msg)
        }
        return null
    }

    // ─── SMS ─────────────────────────────────────────────────────────────────

    private fun matchSms(lo: String): LocalAction? {
        if (lo.contains("whatsapp") || lo.contains("telegram")) return null
        Regex("^(?:manda|envia|escribe|send)\\s+(?:un\\s+)?(?:sms|mensaje|texto)\\s+(?:a\\s+)?(.+?)\\s+(?:diciendo|que diga|que dice|:|con el texto|con mensaje)\\s+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendSms(c, msg)
        }
        return null
    }

    // ─── Navegación ───────────────────────────────────────────────────────────

    private fun matchNavigation(lo: String): LocalAction? {
        val m = Regex("^(?:llevame a|navega a|como llego a|como voy a|navigate to|directions? to|lleva a|ir a)\\s+(.+)$").find(lo) ?: return null
        val dest = m.groupValues[1].trim()
        return if (dest.isNotBlank() && dest.length < 80) LocalAction.Navigate(dest) else null
    }

    // ─── Búsqueda web ─────────────────────────────────────────────────────────

    private fun matchWebSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca(r)?|busca en (?:google|internet|web|la web)|search(?:\\s+for)?|googlea)\\s+(.{3,120})$").find(lo) ?: return null
        val q = m.groupValues.last().trim()
        return if (q.isNotBlank()) LocalAction.SearchWeb(q) else null
    }

    // ─── Búsqueda en Maps ─────────────────────────────────────────────────────

    private fun matchMapsSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca(r)? en (?:maps|google maps)|muestra(me)? en (?:el mapa|maps)|encuentra en maps)\\s+(.{3,80})$").find(lo) ?: return null
        val q = m.groupValues.last().trim()
        return if (q.isNotBlank()) LocalAction.SearchMaps(q) else null
    }

    // ─── Controles de reproducción ────────────────────────────────────────────

    private fun matchMediaControl(lo: String): LocalAction? = when {
        Regex("^(pausa(r)?|pause|detén musica|para la musica|parar musica|stop music)$").containsMatchIn(lo)
            -> LocalAction.PauseMusic()
        Regex("^(resume|reanuda(r)?|continua(r)? musica|sigue la musica|play again|seguir musica)$").containsMatchIn(lo)
            -> LocalAction.ResumeMusic()
        Regex("^(siguiente|proxima cancion|next|salta(r)|skip|siguiente cancion|siguiente pista)$").containsMatchIn(lo)
            -> LocalAction.NextTrack()
        Regex("^(anterior|cancion anterior|prev(ia)?|back|atras|pista anterior|regresa(r)?)$").containsMatchIn(lo)
            -> LocalAction.PrevTrack()
        else -> null
    }

    // ─── Música ───────────────────────────────────────────────────────────────

    private fun matchMusic(lo: String): LocalAction? {
        val musicTrigger = Regex("\\b(pon|reproduce|play|toca|ver|mira|busca|quiero ver|quiero escuchar|escucha|escuchar)\\b")
        if (!musicTrigger.containsMatchIn(lo)) return null

        // "pon/ver un video de X en youtube / yt"
        Regex("^(?:pon|reproduce|play|ver|mira|busca)\\s+(?:un\\s+)?(?:video|videos)\\s+(?:de\\s+)?['\"]?(.+?)['\"]?\\s+en\\s+(youtube|yt|youtube music|yt music)").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        // "pon/ver un video de X" sin mencionar plataforma → YouTube
        Regex("^(?:pon|reproduce|play|ver|mira|busca|quiero ver)\\s+(?:un\\s+)?(?:video|videos)\\s+(?:de\\s+)?['\"]?(.{2,80}?)['\"]?\\s*$").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        // "ver X en youtube"
        Regex("^(?:ver|mira|busca)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(youtube|yt)\\s*$").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        // "pon CANCIÓN en PLATAFORMA"
        Regex("^(?:pon|reproduce|play|toca|escucha)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(spotify|youtube music|yt music|youtube|yt|apple music|deezer)$").find(lo)?.let { m ->
            val q   = m.groupValues[1].trim()
            val app = resolveApp(m.groupValues[2])
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, app)
        }
        // "pon música/spotify/youtube en [plataforma]" (sin canción)
        Regex("^(?:pon|abre|reproduce|play)\\s+(?:musica\\s+)?(?:en\\s+)?(spotify|youtube music|yt music|youtube|yt|apple music|deezer)$").find(lo)?.let { m ->
            return LocalAction.PlayMusic("", resolveApp(m.groupValues[1]))
        }
        // "pon/toca CANCIÓN" sin plataforma → Spotify por defecto
        val blacklist = setOf("volumen","brillo","wifi","bluetooth","alarma","linterna","silencio","modo","pantalla","video","videos")
        Regex("^(?:pon|reproduce|play|toca|escucha|quiero escuchar)\\s+(?:la cancion\\s+|la musica\\s+|el tema\\s+)?['\"]?(.{2,60}?)['\"]?\\s*$").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank() && blacklist.none { q.contains(it) })
                return LocalAction.PlayMusic(q, "spotify")
        }
        return null
    }

    private fun resolveApp(raw: String): String = when {
        raw == "youtube" || raw == "yt" -> "youtube"
        raw.contains("youtube music") || raw.contains("yt music") -> "youtube music"
        raw.contains("apple")  -> "apple music"
        raw.contains("deezer") -> "deezer"
        else -> "spotify"
    }

    // ─── URL directa ─────────────────────────────────────────────────────────

    private fun matchUrl(lo: String): LocalAction? {
        val m = Regex("^(?:abre?|ve a|open|go to)\\s+(https?://\\S+|www\\.\\S+)$").find(lo) ?: return null
        var url = m.groupValues[1].trim()
        if (!url.startsWith("http")) url = "https://$url"
        return LocalAction.OpenUrl(url)
    }

    // ─── Consultas del sistema ────────────────────────────────────────────────

    private fun matchSystemQuery(lo: String): LocalAction? = when {
        Regex("\\b(que hora es|dime la hora|what time is it|hora actual|que hora son)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.TIME)
        Regex("\\b(que dia es|que fecha es|what day is it|fecha actual|que dia estamos)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.DATE)
        Regex("\\b(cuanta bateria|nivel de bateria|battery level|como esta la bateria|cuanto de bateria|porcentaje de bateria)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BATTERY)
        Regex("\\b(cuanto espacio|almacenamiento disponible|espacio libre|storage available|cuanto almacenamiento|cuanta memoria)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.STORAGE)
        Regex("\\b(esta (el )?wifi (activo|conectado|on)|tengo wifi|wifi esta on)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.WIFI_STATUS)
        Regex("\\b(esta (el )?bluetooth (activo|on)|tengo bluetooth activo)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BT_STATUS)
        else -> null
    }

    // ─── Abrir app (genérico — siempre al final) ──────────────────────────────

    private fun matchOpenApp(lo: String): LocalAction? {
        val m = Regex("^(?:abre?|lanza|inicia|entra a|ve a|open|launch|start)\\s+(?:la\\s+app\\s+(?:de\\s+)?|la\\s+aplicacion\\s+(?:de\\s+)?)?(.{2,30})$").find(lo) ?: return null
        val appName = m.groupValues[1].trim()
        // Rechazar si parece destino de navegación
        val geoWords = listOf("calle","avenida","colonia","ciudad","pais","cerca")
        if (geoWords.any { appName.contains(it) }) return null
        if (appName.split(" ").size > 4) return null // demasiadas palabras = no es nombre de app
        return LocalAction.OpenApp(appName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Splitter de sub-tareas
    // ─────────────────────────────────────────────────────────────────────────

    private fun splitSubtasks(text: String): List<String> {
        var parts = listOf(text)
        listOf(
            Regex("\\s+y luego\\s+",   RegexOption.IGNORE_CASE),
            Regex("\\s+y despues\\s+", RegexOption.IGNORE_CASE),
            Regex("\\s+y tambien\\s+", RegexOption.IGNORE_CASE),
            Regex("\\s+ademas\\s+",    RegexOption.IGNORE_CASE),
            Regex("\\s+and then\\s+",  RegexOption.IGNORE_CASE)
        ).forEach { r ->
            parts = parts.flatMap { r.split(it).map(String::trim).filter { p -> p.length > 5 } }
        }
        return parts
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resolución de contactos
    // ─────────────────────────────────────────────────────────────────────────

    fun resolveContactNumber(context: Context, nameOrNumber: String): String? {
        if (nameOrNumber.matches(Regex("[\\+\\d\\s\\-\\(\\)]{7,}"))) return nameOrNumber
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameOrNumber%"),
            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
        ) ?: return null
        return cursor.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) else null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt optimizado para IA con sub-tareas
    // ─────────────────────────────────────────────────────────────────────────

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String {
        if (subtasks.size <= 1) return originalInput
        val sb = StringBuilder("Ejecuta estas tareas en orden:\n")
        subtasks.forEachIndexed { i, t -> sb.append("${i + 1}. $t\n") }
        sb.append("\nEjecútalas secuencialmente y confirma cada una.")
        return sb.toString()
    }
}
