package com.doey.agente.iris

import com.doey.agente.iris.IrisDiccionario.V_OPEN
import com.doey.agente.iris.IrisDiccionario.V_ON
import com.doey.agente.iris.IrisDiccionario.V_OFF
import com.doey.agente.iris.IrisDiccionario.V_SEND
import com.doey.agente.iris.IrisDiccionario.V_PLAY
import com.doey.agente.iris.IrisDiccionario.V_UP
import com.doey.agente.iris.IrisDiccionario.V_DOWN
import com.doey.agente.iris.IrisDiccionario.V_SEARCH
import com.doey.agente.iris.IrisDiccionario.V_NAV
import com.doey.agente.iris.IrisDiccionario.S_FLASH
import com.doey.agente.iris.IrisDiccionario.S_BT
import com.doey.agente.iris.IrisDiccionario.S_WIFI
import com.doey.agente.iris.IrisDiccionario.S_VOL
import com.doey.agente.iris.IrisDiccionario.S_BRILLO
import com.doey.agente.iris.IrisDiccionario.S_ALARM
import com.doey.agente.iris.IrisDiccionario.S_CHRONO
import com.doey.agente.iris.IrisDiccionario.S_SHOPPING
import com.doey.agente.iris.IrisDiccionario.S_WHATSAPP
import com.doey.agente.iris.IrisDiccionario.GEO_WORDS
import com.doey.agente.iris.IrisDiccionario.UNIT_ALIASES
import com.doey.agente.iris.IrisClasificador.LocalAction
import com.doey.agente.iris.IrisClasificador.VolumeStream
import com.doey.agente.iris.IrisClasificador.SilentMode
import com.doey.agente.iris.IrisClasificador.InfoType

/**
 * IRIS — Motor de Funciones / Matchers
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  Cada función match*() recibe el texto normalizado (lowercase sin         │
 * │  tildes) y retorna una LocalAction o null.                                │
 * │  EDITA AQUÍ para mejorar la detección de un comando específico.           │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object IrisMotor {

    // ── Regex precompilados de verbos ─────────────────────────────────────────
    private val RX_OPEN   = Regex("\\b($V_OPEN)\\b",   RegexOption.IGNORE_CASE)
    private val RX_ON     = Regex("\\b($V_ON)\\b",     RegexOption.IGNORE_CASE)
    private val RX_OFF    = Regex("\\b($V_OFF)\\b",    RegexOption.IGNORE_CASE)
    private val RX_UP     = Regex("\\b($V_UP)\\b",     RegexOption.IGNORE_CASE)
    private val RX_DOWN   = Regex("\\b($V_DOWN)\\b",   RegexOption.IGNORE_CASE)
    private val RX_SEARCH = Regex("\\b($V_SEARCH)\\b", RegexOption.IGNORE_CASE)
    private val RX_NAV    = Regex("^($V_NAV)\\s+(.+)$",RegexOption.IGNORE_CASE)

    // ══════════════════════════════════════════════════════════════════════════
    // RESPUESTAS SOCIALES
    // ══════════════════════════════════════════════════════════════════════════

    private val GREETING_REGEX = Regex(
        "^(hola|hey|hi|buenas|buenos dias|buenas tardes|buenas noches|buen dia|que tal|" +
        "como estas|como andas|que onda|que pedo|que paso|que hay|ey|" +
        "que hubo|quiubo|saludos|ola|alo|que hay de nuevo|que me cuentas|" +
        "buenas buenas|hola como estas|holi|holaa|hola hola)$",
        RegexOption.IGNORE_CASE
    )

    fun matchGreeting(lo: String): LocalAction? {
        if (!GREETING_REGEX.containsMatchIn(lo) || lo.split(" ").size > 5) return null
        return LocalAction.Greeting((System.currentTimeMillis() % IrisDiccionario.GREETING_RESPONSES.size).toInt())
    }

    private val FAREWELL_REGEX = Regex(
        "^(adios|hasta luego|nos vemos|chao|chau|bye|hasta pronto|me voy|" +
        "me despido|hasta la proxima|nos vidrios|a dormir|bye bye|" +
        "hasta manana|hasta el rato|ya me voy|nos vemos luego|chao chao)$",
        RegexOption.IGNORE_CASE
    )

    fun matchFarewell(lo: String): LocalAction? {
        if (!FAREWELL_REGEX.containsMatchIn(lo) || lo.split(" ").size > 6) return null
        return LocalAction.Farewell((System.currentTimeMillis() % IrisDiccionario.FAREWELL_RESPONSES.size).toInt())
    }

    private val GRATITUDE_REGEX = Regex(
        "^(gracias|muchas gracias|grax|grasias|te lo agradezco|muy amable|" +
        "thanks|thank you|de lujo|chido|estuvo bien|estuvo chido|" +
        "eres lo maximo|eres el mejor|que bueno|excelente trabajo|" +
        "(ok |sale |genial |perfecto |listo |orale |andale )?gracias)$",
        RegexOption.IGNORE_CASE
    )

    fun matchGratitude(lo: String): LocalAction? {
        if (!GRATITUDE_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Gratitude((System.currentTimeMillis() % IrisDiccionario.GRATITUDE_RESPONSES.size).toInt())
    }

    private val AFFIRMATION_REGEX = Regex(
        "^(ok|okay|okey|listo|entendido|de acuerdo|sale|andale|orale|" +
        "claro|por supuesto|si|yes|yep|yup|va|dale|adelante|perfecto|excelente|" +
        "esta bien|de una|con todo|ten|ahi esta|eso|correcto|" +
        "va bien|entendido|copiado|recibido|roger|afirmativo)$",
        RegexOption.IGNORE_CASE
    )

    fun matchAffirmation(lo: String): LocalAction? {
        if (!AFFIRMATION_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Affirmation((System.currentTimeMillis() % IrisDiccionario.AFFIRMATION_RESPONSES.size).toInt())
    }

    fun matchMemoryQuery(lo: String): LocalAction? {
        val trigger = Regex(
            """\\b(recuerdas|sabes algo de|te dije|me dijiste|te acuerdas|dijiste|acuerdas|lo que te conte|lo que te dije|te platique|te mencione)\\b""",
            RegexOption.IGNORE_CASE
        )
        if (!trigger.containsMatchIn(lo)) return null
        return LocalAction.QueryMemory(lo)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CALCULADORA OFFLINE
    // ══════════════════════════════════════════════════════════════════════════

    fun matchCalculate(lo: String): LocalAction? {
        val trigger = Regex(
            "\\b(calcula|calculo|cuanto es|cuantos son|resultado de|cuanto da|quanto es|" +
            "cuanto queda|cuanto seria|cuanto serian|cuanto son|a cuanto da|" +
            "operacion|opera|matematica de)\\b",
            RegexOption.IGNORE_CASE
        )
        if (!trigger.containsMatchIn(lo)) return null
        val expr = trigger.replace(lo, "").trim()
            .replace("por",    "*").replace("x",        "*")
            .replace("entre",  "/").replace("dividido", "/")
            .replace("mas",    "+")
            .replace("menos",  "-")
            .replace("al cuadrado", "^2").replace("al cubo", "^3")
            .replace(Regex("elevado a (\\d+)"), "^$1")
            .replace(Regex("(\\d+)\\s*%\\s*de\\s*(\\d+)")) { m ->
                "(${m.groupValues[1]}/100)*${m.groupValues[2]}"
            }
            .trim()
        if (expr.isBlank()) return null
        return LocalAction.Calculate(expr)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONVERSIÓN DE UNIDADES OFFLINE
    // ══════════════════════════════════════════════════════════════════════════

    fun matchConvert(lo: String): LocalAction? {
        val rx = Regex(
            "(?:convierte|cuanto son|cuanto es|pasar|convertir|a cuanto equivale|equivale)" +
            "\\s+([\\d.,]+)\\s+(.+?)\\s+(?:en|a|to)\\s+(.+?)\\s*$",
            RegexOption.IGNORE_CASE
        )
        val m = rx.find(lo) ?: return null
        val value = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
        val from  = UNIT_ALIASES[m.groupValues[2].trim().lowercase()] ?: m.groupValues[2].trim().lowercase()
        val to    = UNIT_ALIASES[m.groupValues[3].trim().lowercase()] ?: m.groupValues[3].trim().lowercase()
        if (from.isBlank() || to.isBlank()) return null
        return LocalAction.Convert(value, from, to)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTAS RÁPIDAS OFFLINE
    // ══════════════════════════════════════════════════════════════════════════

    fun matchQuickNote(lo: String): LocalAction? {
        Regex(
            "^(?:anota|apunta|guarda|nota|recuerda que|agrega nota|nota rapida|escribe|" +
            "apuntame|guardame|memoriza|toma nota)\\s*[:\\-]?\\s*(.{3,200})$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val content = m.groupValues[1].trim()
            if (content.isNotBlank()) return LocalAction.QuickNote(content)
        }
        if (Regex(
            "\\b(que tengo anotado|mis notas|ver notas|leer notas|que anote|" +
            "notas guardadas|que guarde|muestra mis notas|ver mis notas)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo)) return LocalAction.ReadNotes()
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RECORDATORIO RÁPIDO OFFLINE
    // ══════════════════════════════════════════════════════════════════════════

    fun matchQuickReminder(lo: String): LocalAction? {
        val rx = Regex(
            "^(?:recuerdame|avisame|notificame|reminder|recuerda|avisa|" +
            "ponme un recordatorio|recordatorio)\\s+(.+?)\\s+" +
            "(?:en|dentro de)\\s+(\\d+)\\s+(minuto|minutos|hora|horas|segundo|segundos)$",
            RegexOption.IGNORE_CASE
        )
        val m = rx.find(lo) ?: return null
        val text  = m.groupValues[1].trim()
        val value = m.groupValues[2].toIntOrNull() ?: return null
        val unit  = m.groupValues[3].lowercase()
        val minutes = when {
            unit.startsWith("hora") -> value * 60
            unit.startsWith("seg")  -> maxOf(1, value / 60)
            else                    -> value
        }
        return LocalAction.QuickReminder(text, minutes)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DISPOSITIVO — LINTERNA
    // ══════════════════════════════════════════════════════════════════════════

    fun matchFlashlight(lo: String): LocalAction? {
        return when {
            Regex("\\b($V_ON)\\b.*($S_FLASH)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($S_FLASH).*\\b(on|encendida|activa)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleFlashlight(true)
            Regex("\\b($V_OFF)\\b.*($S_FLASH)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($S_FLASH).*\\b(off|apagada|desactiva)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleFlashlight(false)
            else -> null
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VOLUMEN
    // ══════════════════════════════════════════════════════════════════════════

    fun matchVolume(lo: String): LocalAction? {
        val ring  = "\\b(timbre|ringtone|tono de llamada|tono|tono del cel)\\b"
        val alarm = "\\b($S_ALARM)\\b"

        Regex("\\b($V_UP|$V_DOWN)\\b.*$ring", RegexOption.IGNORE_CASE).find(lo)?.let {
            val up  = RX_UP.containsMatchIn(lo)
            val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
            if (pct != null) return LocalAction.SetRingtoneVolume(pct.coerceIn(0, 100))
            return LocalAction.VolumeStep(up, VolumeStream.RING)
        }
        Regex("\\b($V_UP|$V_DOWN)\\b.*$alarm|$alarm.*\\b($S_VOL)\\b", RegexOption.IGNORE_CASE).find(lo)?.let {
            val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
            if (pct != null) return LocalAction.SetAlarmVolume(pct.coerceIn(0, 100))
            return LocalAction.VolumeStep(RX_UP.containsMatchIn(lo), VolumeStream.ALARM)
        }
        if (Regex("\\b(silencia|silenciar|mute|mutea|mutear|silencia el cel)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.SetSilentMode(SilentMode.SILENT)

        if (!Regex("\\b($V_UP|$V_DOWN|pon|ajusta|sube|baja|volumen)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        if (!Regex("\\b($S_VOL)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) &&
            !Regex("\\b($V_UP|$V_DOWN)\\b.*(musica|media|el sonido|el audio)", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
        if (pct != null) return LocalAction.SetVolume(pct.coerceIn(0, 100))
        return LocalAction.VolumeStep(RX_UP.containsMatchIn(lo))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MODO SILENCIO / VIBRACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    fun matchSilent(lo: String): LocalAction? = when {
        Regex(
            "\\b(modo silencio|silencio total|no hacer ruido|silencioso|ponlo en silencio|" +
            "pon el silencio|activa el silencio)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo) -> LocalAction.SetSilentMode(SilentMode.SILENT)

        Regex(
            "\\b(modo vibracion|vibrar|vibrador|vibra|vibration|solo vibrar|solo vibra)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo) -> LocalAction.SetSilentMode(SilentMode.VIBRATE)

        Regex(
            "\\b(sonido normal|quita el silencio|activa el sonido|pon sonido|sonido on|" +
            "deja sonar|quitale el silencio|quita el mute|desmutea)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo) -> LocalAction.SetSilentMode(SilentMode.NORMAL)

        else -> null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BRILLO
    // ══════════════════════════════════════════════════════════════════════════

    fun matchBrightness(lo: String): LocalAction? {
        if (!Regex("\\b($S_BRILLO)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        if (Regex("\\b(automatico|auto|auto brillo|auto-brillo|adaptativo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(RX_ON.containsMatchIn(lo))
        val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
        if (pct != null) return LocalAction.SetBrightness(pct.coerceIn(0, 100))
        return LocalAction.BrightnessStep(RX_UP.containsMatchIn(lo))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WIFI / BLUETOOTH / AVIÓN / NO-MOLESTAR / NFC / MODO OSCURO / HOTSPOT
    // ══════════════════════════════════════════════════════════════════════════

    fun matchWifi(lo: String): LocalAction? = when {
        Regex("\\b($V_ON)\\b.*($S_WIFI)|($S_WIFI).*\\b(on|activo|activalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleWifi(true)
        Regex("\\b($V_OFF)\\b.*($S_WIFI)|($S_WIFI).*\\b(off|apagado|apagalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleWifi(false)
        else -> null
    }

    fun matchBluetooth(lo: String): LocalAction? = when {
        Regex("\\b($V_ON)\\b.*($S_BT)|($S_BT).*\\b(on|activo|activalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleBluetooth(true)
        Regex("\\b($V_OFF)\\b.*($S_BT)|($S_BT).*\\b(off|apagado|desactiva)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleBluetooth(false)
        else -> null
    }

    fun matchAirplane(lo: String): LocalAction? {
        val air = "\\b(modo avion|airplane mode|flight mode|modo vuelo|modo aereo|modo de avion)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($air)|($air).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(true)
            Regex("\\b($V_OFF)\\b.*($air)|($air).*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(false)
            else -> null
        }
    }

    fun matchDoNotDisturb(lo: String): LocalAction? {
        val dnd = "\\b(no molestar|dnd|do not disturb|no interrumpir|no me molestes|modo zen|modo enfoque)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($dnd)|($dnd).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(true)
            Regex("\\b($V_OFF)\\b.*($dnd)|($dnd).*\\b(off|desactiva)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(false)
            else -> null
        }
    }

    fun matchNfc(lo: String): LocalAction? = when {
        Regex("\\b($V_ON)\\b.*\\bnfc\\b|\\bnfc\\b.*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleNfc(true)
        Regex("\\b($V_OFF)\\b.*\\bnfc\\b|\\bnfc\\b.*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleNfc(false)
        else -> null
    }

    fun matchDarkMode(lo: String): LocalAction? {
        val dark  = "\\b(modo oscuro|dark mode|modo noche|tema oscuro|modo dark|pantalla oscura)\\b"
        val light = "\\b(modo claro|light mode|modo dia|tema claro|modo normal|pantalla clara)\\b"
        return when {
            Regex("\\b($V_ON|usa|pon)\\b.*($dark)|($dark).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(true)
            Regex("\\b($V_OFF|quita)\\b.*($dark)|($light)", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(false)
            else -> null
        }
    }

    fun matchHotspot(lo: String): LocalAction? {
        val hot = "\\b(hotspot|punto de acceso|compartir datos|tethering|internet movil compartido|datos compartidos)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($hot)|($hot).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(true)
            Regex("\\b($V_OFF)\\b.*($hot)|($hot).*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(false)
            else -> null
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCREENSHOT / BLOQUEO
    // ══════════════════════════════════════════════════════════════════════════

    fun matchScreenshot(lo: String): LocalAction? {
        return if (
            Regex(
                "\\b(toma|saca|haz|captura|take|screenshot|printscreen)\\b.*(captura|screenshot|pantalla|screen shot|foto de pantalla)",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(lo) ||
            Regex("^(screenshot|captura de pantalla|foto de pantalla|captura la pantalla)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
        ) LocalAction.TakeScreenshot() else null
    }

    fun matchLock(lo: String): LocalAction? {
        return if (Regex(
            "^(bloquea(r)?( el telefono| la pantalla| el cel| el movil)?|" +
            "lock( the)? (screen|phone)|apaga(r)? la pantalla|bloquear cel|" +
            "apaga pantalla|bloquea el celu)$",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo)) LocalAction.LockScreen() else null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ALARMAS Y TIEMPO
    // ══════════════════════════════════════════════════════════════════════════

    fun matchAlarm(lo: String): LocalAction? {
        if (Regex("\\b(cancela|elimina|borra|quita|cancel)\\b.*($S_ALARM)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.CancelAlarm()

        val alarmTrigger = Regex(
            "\\b(pon|crea|programa|set|ponme|despiertame|despertarme|ponme una|programa una|levantame)\\b" +
            ".*($S_ALARM)",
            RegexOption.IGNORE_CASE
        )
        if (!alarmTrigger.containsMatchIn(lo) &&
            !Regex("\\b(despiertame|levantame|ponme a las)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        Regex("\\ben\\s+(\\d+)\\s+(hora|horas|minuto|minutos)\\b", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val value = m.groupValues[1].toInt()
            val unit  = m.groupValues[2].lowercase()
            val cal   = java.util.Calendar.getInstance()
            if (unit.startsWith("hora")) cal.add(java.util.Calendar.HOUR_OF_DAY, value)
            else cal.add(java.util.Calendar.MINUTE, value)
            return LocalAction.SetAlarm(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }

        Regex("\\ba las\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m|p\\.m)?", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            var h   = m.groupValues[1].toInt()
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase().replace(".", "")
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            val label = Regex("(?:llamada|nota|label|etiqueta|para|con titulo)\\s+(.+)$", RegexOption.IGNORE_CASE)
                .find(lo)?.groupValues?.get(1)?.trim() ?: ""
            return LocalAction.SetAlarm(h, min, label)
        }

        Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            var h   = m.groupValues[1].toInt()
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase()
            if (ampm == "pm" && h < 12) h += 12
            if (h in 0..23) return LocalAction.SetAlarm(h, min)
        }
        return null
    }

    fun matchTimer(lo: String): LocalAction? {
        if (Regex("\\b(cancela|para|detén|quita)\\b.*(timer|temporizador|cuenta regresiva)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.CancelTimer()

        val trigger = Regex(
            "\\b(timer|temporizador|cuenta regresiva|pon|crea|inicia|ponme)\\b" +
            ".*(timer|temporizador|minuto|minutos|segundo|segundos|hora|horas)|" +
            "\\bde\\s+\\d+\\s+(minutos|segundos|horas)\\b",
            RegexOption.IGNORE_CASE
        )
        if (!trigger.containsMatchIn(lo) &&
            !Regex("^(timer|temporizador)\\s+(de\\s+)?\\d+", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        var totalSeconds = 0L
        Regex("(\\d+)\\s*(hora|horas|h\\b)",          RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() * 3600 }
        Regex("(\\d+)\\s*(minuto|minutos|min\\b|m\\b)",RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() * 60 }
        Regex("(\\d+)\\s*(segundo|segundos|seg\\b|s\\b)",RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() }

        if (totalSeconds <= 0) return null
        val label = Regex("(?:llamado|label|para|con titulo)\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(lo)?.groupValues?.get(1)?.trim() ?: ""
        return LocalAction.SetTimer(totalSeconds, label)
    }

    fun matchStopwatch(lo: String): LocalAction? = when {
        Regex(
            "\\b(inicia|empieza|arranca|start)\\b.*($S_CHRONO)|^($S_CHRONO)$",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo) -> LocalAction.StartStopwatch()

        Regex("\\b(detén|para|stop|pausa)\\b.*($S_CHRONO)", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.StopStopwatch()

        else -> null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMUNICACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    fun matchEmergencyCall(lo: String): LocalAction? {
        val em = Regex("\\b(911|112|066|080|060|emergencia|ambulancia|policia|bomberos|urgencias)\\b", RegexOption.IGNORE_CASE)
        return if (Regex("\\b($V_ON|llama|call|marca|llama al|marca al)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) && em.containsMatchIn(lo))
            LocalAction.CallEmergency(em.find(lo)!!.value) else null
    }

    fun matchCall(lo: String): LocalAction? {
        val m = Regex(
            "^(?:llama(r)?( a| le a)?|marca(r)?( a| le a)?|call|llamar a|" +
            "comunica(r)?me con|comunicate con|contacta a)\\s+(?:a\\s+)?(.+?)\\s*$",
            RegexOption.IGNORE_CASE
        ).find(lo) ?: return null
        val contact = (m.groupValues[5].ifBlank { m.groupValues[6] }).trim()
        if (contact.isBlank() || contact.length > 40) return null
        return LocalAction.Call(contact)
    }

    fun matchWhatsApp(lo: String): LocalAction? {
        val isWA = Regex("\\b($S_WHATSAPP)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
        if (!isWA) return null

        Regex(
            "^(?:$V_SEND)\\s+(?:un\\s+)?(?:mensaje|whatsapp|wa|wapp|wasa)?\\s*(?:a\\s+)?(.+?)\\s+" +
            "(?:por\\s+(?:whatsapp|wa)\\s+)?(?:diciendo|que diga|que dice|:|con el texto|el mensaje|el texto|dile que)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim().removePrefix("a ").trim()
            val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank() && c.length < 40) return LocalAction.SendWhatsApp(c, msg)
        }

        Regex(
            "^(?:$V_SEND)\\s+(?:por\\s+)?(?:whatsapp|wa\\b|wapp)\\s+(?:a\\s+)?(.+?)[: ]+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }

        Regex(
            "^(?:wa|wapp)\\s+a\\s+(.+?)\\s+(?:diciendo|dile|que diga|:|el mensaje)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }

        Regex(
            "^(?:$V_OPEN|chatea con|chatear con|habla con|hablar con)\\s+" +
            "(?:(?:whatsapp|wa)\\s+(?:con|de)\\s+|chat de (?:whatsapp|wa)\\s+con\\s+)?(.+?)" +
            "(?:\\s+(?:en|por)\\s+(?:whatsapp|wa))?$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim()
                .removePrefix("en whatsapp").removePrefix("por whatsapp")
                .removePrefix("en wa").trim()
            if (c.isNotBlank() && !c.contains("whatsapp", ignoreCase = true) && c.length < 40)
                return LocalAction.OpenWhatsAppChat(c)
        }
        return null
    }

    fun matchTelegram(lo: String): LocalAction? {
        if (!lo.contains("telegram")) return null
        Regex(
            "^(?:$V_SEND)\\s+(?:un\\s+)?(?:mensaje|telegram)?\\s*(?:a\\s+)?(.+?)\\s+" +
            "(?:por telegram\\s+)?(?:diciendo|que diga|:|el mensaje)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendTelegram(c, msg)
        }
        return null
    }

    fun matchSms(lo: String): LocalAction? {
        if (Regex("\\b(whatsapp|telegram|wapp|guasap)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        Regex(
            "^(?:$V_SEND)\\s+(?:un\\s+)?(?:sms|mensaje|texto|mensajito)\\s+(?:a\\s+)?(.+?)\\s+" +
            "(?:diciendo|que diga|:|con el texto|el texto)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendSms(c, msg)
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NAVEGACIÓN / MAPAS / WEB
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * matchNavigation — MEJORADO
     *
     * Acepta TODAS las formas de pedir una ruta, incluyendo:
     *   "llévame a", "llévame al", "llévame a la", "vamos al", "quiero ir al",
     *   "cómo llego al", "me llevas a", "llévame pal", "ir a la", etc.
     *
     * El secreto: V_NAV en IrisDiccionario cubre todas las variantes. Solo
     * necesitamos extraer el DESTINO que sigue después del verbo, incluyendo
     * artículos (el, la, los, las, un, una) que antes se perdían.
     */
    fun matchNavigation(lo: String): LocalAction? {
        val m = RX_NAV.find(lo) ?: return null
        // groupValues[2] = todo lo que sigue al verbo de navegación
        val dest = m.groupValues[2].trim()
        if (dest.length > 100 || dest.isBlank()) return null
        return LocalAction.Navigate(dest)
    }

    fun matchWebSearch(lo: String): LocalAction? {
        val m = Regex(
            "^(?:busca en google|googlea|busca en internet|search|busca en la web|" +
            "busca online|busca en bing|busca en el buscador|haz una busqueda de)\\s+" +
            "(?:sobre\\s+|acerca de\\s+)?(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo) ?: return null
        return LocalAction.SearchWeb(m.groupValues[1].trim())
    }

    fun matchMapsSearch(lo: String): LocalAction? {
        val m = Regex(
            "^(?:busca en maps|busca en google maps|donde esta|donde hay|" +
            "encuentra en el mapa|busca en el mapa|find)\\s+(.+?)" +
            "\\s*(?:cerca|en el mapa|en maps|near me|nearby|en google maps)?$",
            RegexOption.IGNORE_CASE
        ).find(lo) ?: return null
        val q = m.groupValues[1].trim()
        if (q.length < 3 || q.length > 80) return null
        return LocalAction.SearchMaps(q)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MÚSICA
    // ══════════════════════════════════════════════════════════════════════════

    fun matchSearchAndPlaySpotify(lo: String): LocalAction? {
        val rx = Regex(
            "^(?:$V_OPEN)\\s+(spotify|musica|apple music|deezer|soundcloud|youtube music)\\s+" +
            "(?:y\\s+)?(?:busca|search|encuentra|pon)\\s+(?:musica\\s+de|canciones\\s+de|artista\\s+)?(.+?)\\s*" +
            "(?:,|y)?\\s*(?:pon|selecciona|escoge|dale click a|reproduce|abre)\\s+" +
            "(?:la primera|el primero|el primer resultado|la primera opcion|el que salga|" +
            "la que salga|la que aparezca|el que aparezca|el primero que aparezca|la primera que aparezca)\\s*$",
            RegexOption.IGNORE_CASE
        )
        rx.find(lo)?.let { m ->
            val query = m.groupValues[2].trim()
            if (query.isNotBlank()) return LocalAction.SearchAndPlaySpotify(query)
        }
        val rx2 = Regex(
            "^(?:$V_SEARCH)\\s+(?:musica\\s+de|canciones\\s+de|artista\\s+)?(.+?)\\s+(?:en\\s+)?" +
            "(spotify|apple music|deezer)\\s+(?:y\\s+)?(?:pon|selecciona|dale play a|reproduce)\\s+" +
            "(?:la primera|el primero|el primer resultado|la primera opcion|el que salga|la que aparezca)\\s*$",
            RegexOption.IGNORE_CASE
        )
        rx2.find(lo)?.let { m ->
            val query = m.groupValues[1].trim()
            if (query.isNotBlank()) return LocalAction.SearchAndPlaySpotify(query)
        }
        return null
    }

    fun matchMediaControl(lo: String): LocalAction? = when {
        Regex("^(pausa(r)?|pause|deten la musica|para la musica|stop music|pausalo|pausala)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.PauseMusic()
        Regex("^(resume|reanuda(r)?|continua(r)? musica|sigue la musica|play again|dale play|dale|sigue|unpause)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ResumeMusic()
        Regex("^(siguiente|proxima cancion|next|salta(r)?|skip|la que sigue|cambia la cancion|otra cancion)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.NextTrack()
        Regex("^(anterior|cancion anterior|prev(ia)?|back|la de antes|regresar cancion|regresa la cancion)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.PrevTrack()
        Regex("^(aleatoria|shuffle|modo aleatorio|random|mezclar|mezcla|pon en aleatorio)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ShuffleMusic()
        Regex("^(repite|repeat|repetir|loop|en loop|pon en loop)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.RepeatToggle()
        else -> null
    }

    fun matchMusic(lo: String): LocalAction? {
        if (!Regex("\\b($V_PLAY|busca)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        val blacklist = setOf(
            "volumen", "brillo", "wifi", "bluetooth", "alarma", "linterna",
            "silencio", "modo", "pantalla", "lista", "compras",
            "whatsapp", "telegram", "nota", "recuerda"
        )

        Regex(
            "^(?:$V_PLAY|ver|mira|busca)\\s+(?:un\\s+)?(?:video|videos?)\\s+(?:de\\s+)?['\"]?(.+?)['\"]?" +
            "\\s+en\\s+(youtube|yt|youtube music|yt music)\\s*$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            if (m.groupValues[1].isNotBlank()) return LocalAction.PlayMusic(m.groupValues[1].trim(), "youtube")
        }

        Regex(
            "^(?:$V_PLAY)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)\\s*$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val q = m.groupValues[1].trim(); val app = resolveApp(m.groupValues[2])
            if (q.isNotBlank() && blacklist.none { q.contains(it) }) return LocalAction.PlayMusic(q, app)
        }

        Regex(
            "^(?:$V_OPEN|$V_PLAY)\\s+(?:musica\\s+)?(?:en\\s+)?(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)\\s*$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            return LocalAction.PlayMusic("", resolveApp(m.groupValues[1]))
        }

        Regex(
            "^(?:$V_PLAY)\\s+(?:la cancion\\s+|la musica\\s+|el tema\\s+|la rola\\s+|la banda\\s+)?['\"]?(.{2,60}?)['\"]?\\s*$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank() && blacklist.none { q.contains(it) }) return LocalAction.PlayMusic(q, "spotify")
        }
        return null
    }

    fun resolveApp(raw: String): String = when {
        raw == "youtube" || raw == "yt" -> "youtube"
        raw.contains("youtube music", ignoreCase = true) || raw.contains("yt music", ignoreCase = true) -> "youtube music"
        raw.contains("apple",    ignoreCase = true) -> "apple music"
        raw.contains("deezer",   ignoreCase = true) -> "deezer"
        raw.contains("soundcloud", ignoreCase = true) -> "soundcloud"
        else -> "spotify"
    }

    // ══════════════════════════════════════════════════════════════════════════
    // URL, SISTEMA, NAVEGACIÓN UI, AJUSTES, MANTENIMIENTO
    // ══════════════════════════════════════════════════════════════════════════

    fun matchUrl(lo: String): LocalAction? {
        val m = Regex(
            "^(?:$V_OPEN|ve a|go to|entra a)\\s+(https?://\\S+|www\\.\\S+)$",
            RegexOption.IGNORE_CASE
        ).find(lo) ?: return null
        var url = m.groupValues[1].trim()
        if (!url.startsWith("http")) url = "https://$url"
        return LocalAction.OpenUrl(url)
    }

    fun matchSystemQuery(lo: String): LocalAction? = when {
        Regex("\\b(que hora es|dime la hora|hora actual|que hora son|la hora|a que hora estamos)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.TIME)
        Regex("\\b(que dia es|que fecha es|fecha actual|que dia estamos|la fecha|a que fecha estamos)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.DATE)
        Regex("\\b(cuanta bateria|nivel de bateria|battery level|como esta la bateria|porcentaje de bateria|la bateria|cuanto le queda de bateria)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BATTERY)
        Regex("\\b(cuanto espacio|almacenamiento disponible|espacio libre|cuanto almacenamiento|cuanta memoria|cuanto le queda de espacio)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.STORAGE)
        Regex("\\b(esta (el )?wifi (activo|conectado|on)|tengo wifi|el wifi|tengo internet)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.WIFI_STATUS)
        Regex("\\b(esta (el )?bluetooth (activo|on)|tengo bluetooth activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BT_STATUS)
        Regex("\\b(cuanta ram|uso de ram|memoria ram|ram libre|la ram|cuanta memoria ram)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.RAM_USAGE)
        Regex("\\b(temperatura|esta caliente|temp del cpu|calentamiento|como esta la temperatura)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.CPU_TEMP)
        Regex("\\b(tiempo encendido|uptime|cuanto lleva prendido)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.UPTIME)
        Regex("\\b(velocidad de internet|velocidad de red|que tan rapido|el internet|velocidad de la red)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.NETWORK_SPEED)
        Regex("\\b(mi ip|ip del celular|cual es mi ip|ip local|mi direccion ip)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.IP_ADDRESS)
        else -> null
    }

    fun matchNavigationActions(lo: String): LocalAction? = when {
        Regex(
            "\\b(ve a inicio|pantalla principal|go home|vuelve a inicio|inicio|home|ir al inicio|regresar a inicio)\\b",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lo) && !lo.contains("ajuste") && !lo.contains("aplicaci")
            -> LocalAction.GoHome()

        Regex("\\b(atras|regresa|vuelve|go back|pa atras|regresar|para atras)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) && lo.length < 25
            -> LocalAction.BackButton()

        Regex("\\b(apps recientes|multitarea|cambiar de app|recent apps|apps abiertas|ver apps abiertas)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ShowRecentApps()

        else -> null
    }

    fun matchSettingsShortcuts(lo: String): LocalAction? {
        if (!Regex("\\b(ajustes|configuracion|settings|configurar|preferencias|opciones del sistema)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        return when {
            lo.contains("wifi") || lo.contains("red")          -> LocalAction.OpenWifiSettings()
            lo.contains("bluetooth") || lo.contains("bt")      -> LocalAction.OpenBluetoothSettings()
            lo.contains("bateria") || lo.contains("pila")      -> LocalAction.OpenBatterySettings()
            lo.contains("pantalla") || lo.contains("brillo")   -> LocalAction.OpenDisplaySettings()
            lo.contains("sonido") || lo.contains("volumen") || lo.contains("audio") -> LocalAction.OpenSoundSettings()
            lo.contains("almacenamiento") || lo.contains("espacio") -> LocalAction.OpenStorageSettings()
            lo.contains("ubicacion") || lo.contains("gps")     -> LocalAction.OpenLocationSettings()
            lo.contains("seguridad") || lo.contains("huella") || lo.contains("pin") -> LocalAction.OpenSecuritySettings()
            lo.contains("aplicaciones") || lo.contains("apps") -> LocalAction.OpenAppsSettings()
            lo.contains("fecha") || lo.contains("hora")        -> LocalAction.OpenDateSettings()
            lo.contains("idioma") || lo.contains("lenguaje")   -> LocalAction.OpenLanguageSettings()
            lo.contains("accesibilidad")                       -> LocalAction.OpenAccessibilitySettings()
            lo.contains("desarrollador")                       -> LocalAction.OpenDeveloperSettings()
            lo.contains("notificaciones")                      -> LocalAction.OpenNotificationSettings()
            lo.contains("privacidad")                          -> LocalAction.OpenPrivacySettings()
            lo.contains("cuenta")                              -> LocalAction.OpenAccountSettings()
            lo.contains("nfc")                                 -> LocalAction.OpenNfcSettings()
            lo.contains("datos")                               -> LocalAction.OpenDataUsageSettings()
            lo.contains("vpn")                                 -> LocalAction.OpenVpnSettings()
            lo.contains("sincronizacion") || lo.contains("sync") -> LocalAction.OpenSyncSettings()
            lo.contains("teclado")                             -> LocalAction.OpenInputMethodSettings()
            else -> LocalAction.OpenSettings()
        }
    }

    fun matchDeviceMaintenance(lo: String): LocalAction? {
        if (Regex("\\b(limpia|borra|quita|clear|elimina)\\b.*(notificaciones|avisos)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ClearNotifications()
        if (Regex("\\b(ahorro de energia|modo ahorro|power save|ahorro de bateria|bajo consumo|modo bajo consumo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.TogglePowerSave()
        Regex("\\b(pantalla se apague en|screen timeout|timeout pantalla)\\b.*(\\d+)\\s*(segundos|minutos)?", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val value = m.groupValues[2].toIntOrNull() ?: return@let
            val secs  = if (m.groupValues[3].contains("minuto")) value * 60 else value
            return LocalAction.SetScreenTimeout(secs)
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LISTA DE COMPRAS
    // ══════════════════════════════════════════════════════════════════════════

    fun matchShopping(lo: String): LocalAction? {
        if (Regex("\\b(limpia|borra|vacía|vacia|borrar|limpiar)\\b.*($S_SHOPPING)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("\\b($S_SHOPPING)\\b.*(limpia|borrar|vaciar)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ClearShoppingList()

        val addRx = Regex(
            "\\b(agrega|agregame|añade|añademe|pon|ponme|añadir|agregar|incluye|apunta|mete)\\b\\s+(.+?)\\s*" +
            "(?:\\b(?:a la|en la|en mi|a mi)\\b.*($S_SHOPPING))",
            RegexOption.IGNORE_CASE
        )
        addRx.find(lo)?.let { m ->
            val raw = m.groupValues[2].trim()
            if (raw.isNotBlank() && raw.length < 60) return LocalAction.AddShoppingItem(raw)
        }
        Regex(
            "\\b(?:a la lista|lista de compras|en la lista|lista del super|lista del mandado)\\b.*?\\b(agrega|añade|pon)\\b\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m ->
            val raw = m.groupValues[2].trim()
            if (raw.isNotBlank() && raw.length < 60) return LocalAction.AddShoppingItem(raw)
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMPARTIR
    // ══════════════════════════════════════════════════════════════════════════

    fun matchShare(lo: String): LocalAction? {
        Regex(
            "^(?:comparte|compartir|share)\\s+" +
            "(?:esto|esta publicacion|este post|esta foto|lo que estoy viendo|el link|el enlace|esto que estoy viendo)" +
            "(?:\\s+con\\s+(.+))?$",
            RegexOption.IGNORE_CASE
        ).find(lo)?.let { m -> return LocalAction.ShareText(m.groupValues[1].trim()) }

        Regex("^(?:comparte|share)\\s+con\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            return LocalAction.ShareText(m.groupValues[1].trim())
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // APPS RÁPIDAS
    // ══════════════════════════════════════════════════════════════════════════

    fun matchQuickApps(lo: String): LocalAction? = when {
        Regex("^(?:$V_OPEN)\\s+(la )?camara$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenCamera()
        Regex("^(?:$V_OPEN)\\s+(la )?galeria$|^(mis fotos|ver fotos|abrir fotos)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenGallery()
        Regex("^(?:$V_OPEN)\\s+(los |mis )?contactos?$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenContacts()
        Regex("^(?:$V_OPEN)\\s+(el )?marcador$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenDialer()
        Regex("^(?:$V_OPEN)\\s+(la )?calculadora?$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenCalculator()
        Regex("^(?:$V_OPEN)\\s+(el )?calendario$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenCalendar()
        Regex("^(?:$V_OPEN)\\s+(google )?maps?$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenMaps()
        Regex("^(?:$V_OPEN)\\s+(el )?(navegador|browser|chrome|firefox|edge|brave)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenBrowser()
        Regex("^(?:$V_OPEN)\\s+(el )?(explorador de archivos|mis archivos|archivos|files|gestor de archivos)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenFiles()
        Regex("^(?:$V_OPEN)\\s+(el )?(reloj|clock)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenClock()
        else -> null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ABRIR APP GENÉRICO — siempre al final
    // ══════════════════════════════════════════════════════════════════════════

    fun matchOpenApp(lo: String): LocalAction? {
        val m = Regex(
            "^(?:$V_OPEN)\\s+(?:la\\s+app\\s+(?:de\\s+)?|la\\s+aplicacion\\s+(?:de\\s+)?|el\\s+)?(.{2,30})$",
            RegexOption.IGNORE_CASE
        ).find(lo) ?: return null
        val appName = m.groupValues[1].trim()
        if (GEO_WORDS.any { appName.contains(it) }) return null
        if (appName.split(" ").size > 4) return null
        return LocalAction.OpenApp(appName)
    }
}
