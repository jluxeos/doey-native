package com.doey.agente

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * IRIS — Intent Recognition & Interpretation System
 * Doey · versión mejorada v3
 *
 * Principios de diseño:
 *  1. SOLO actúa si el comando es una instrucción directa sin ambigüedad.
 *  2. Si la frase contiene contexto narrativo, condicional o explicativo → Delegate.
 *  3. Orden de palabras flexible: verbo antes o después del objeto.
 *  4. Si el objeto mencionado es externo al dispositivo (carro, casa, TV) → Delegate.
 *  5. Solo ejecuta acciones que Android puede realizar directamente.
 *  6. Tolerancia a variantes fonéticas comunes de voz-a-texto.
 *  7. Aliases coloquiales MX soportados.
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
        // ── Respuestas sociales (sin IA, cero tokens) ─────────────────────────
        data class Greeting(val variant: Int) : LocalAction()
        data class Farewell(val variant: Int) : LocalAction()
        data class Gratitude(val variant: Int) : LocalAction()
        data class Affirmation(val variant: Int) : LocalAction()
        // ── Consulta de memorias ──────────────────────────────────────────────
        data class QueryMemory(val raw: String) : LocalAction()
        // ── Alarma nativa (no abre app de reloj) ──────────────────────────────
        data class SetAlarmNative(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()

        data class ToggleFlashlight(val enable: Boolean) : LocalAction()
        data class SetVolume(val level: Int, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class VolumeStep(val up: Boolean, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class SetSilentMode(val mode: SilentMode) : LocalAction()
        data class ToggleWifi(val enable: Boolean) : LocalAction()
        data class ToggleBluetooth(val enable: Boolean) : LocalAction()
        data class ToggleAirplane(val enable: Boolean) : LocalAction()
        data class ToggleDoNotDisturb(val enable: Boolean) : LocalAction()
        data class ToggleNfc(val enable: Boolean) : LocalAction()
        data class ToggleDarkMode(val enable: Boolean) : LocalAction()
        data class ToggleHotspot(val enable: Boolean) : LocalAction()
        data class SetBrightness(val level: Int) : LocalAction()
        data class BrightnessStep(val up: Boolean) : LocalAction()
        data class ToggleAutoBrightness(val enable: Boolean) : LocalAction()
        class TakeScreenshot : LocalAction()
        class LockScreen : LocalAction()
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class CancelAlarm : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class CancelTimer : LocalAction()
        class StopStopwatch : LocalAction()
        class StartStopwatch : LocalAction()
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
        class ShuffleMusic : LocalAction()
        class RepeatToggle : LocalAction()
        data class QueryInfo(val type: InfoType) : LocalAction()
        class ClearNotifications : LocalAction()
        class ShowRecentApps : LocalAction()
        class GoHome : LocalAction()
        class BackButton : LocalAction()
        data class SetScreenTimeout(val seconds: Int) : LocalAction()
        class TogglePowerSave : LocalAction()
        class OpenSettings : LocalAction()
        class OpenBatterySettings : LocalAction()
        class OpenWifiSettings : LocalAction()
        class OpenBluetoothSettings : LocalAction()
        class OpenDisplaySettings : LocalAction()
        class OpenSoundSettings : LocalAction()
        class OpenStorageSettings : LocalAction()
        class OpenAccessibilitySettings : LocalAction()
        class OpenDeveloperSettings : LocalAction()
        class OpenLocationSettings : LocalAction()
        class OpenSecuritySettings : LocalAction()
        class OpenAppsSettings : LocalAction()
        class OpenDateSettings : LocalAction()
        class OpenLanguageSettings : LocalAction()
        class OpenAccountSettings : LocalAction()
        class OpenPrivacySettings : LocalAction()
        class OpenNotificationSettings : LocalAction()
        class OpenSearchSettings : LocalAction()
        class OpenPrintSettings : LocalAction()
        class OpenCastSettings : LocalAction()
        class OpenNfcSettings : LocalAction()
        class OpenDataUsageSettings : LocalAction()
        class OpenVpnSettings : LocalAction()
        class OpenApnSettings : LocalAction()
        class OpenUserDictionarySettings : LocalAction()
        class OpenSyncSettings : LocalAction()
        class OpenInputMethodSettings : LocalAction()
        class OpenCaptioningSettings : LocalAction()
        class OpenDreamSettings : LocalAction()
        class OpenZenModeSettings : LocalAction()
        class OpenUsageAccessSettings : LocalAction()
        class OpenOverlaySettings : LocalAction()
        class OpenWriteSettings : LocalAction()
        // Nuevas acciones
        data class ShareText(val text: String) : LocalAction()
        class OpenCamera : LocalAction()
        class OpenGallery : LocalAction()
        class OpenContacts : LocalAction()
        class OpenDialer : LocalAction()
        class OpenCalculator : LocalAction()
        class OpenCalendar : LocalAction()
        class OpenMaps : LocalAction()
        class OpenBrowser : LocalAction()
        class OpenFiles : LocalAction()
        class OpenClock : LocalAction()
        data class SetRingtoneVolume(val level: Int) : LocalAction()
        data class SetAlarmVolume(val level: Int) : LocalAction()
    }

    enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode { SILENT, VIBRATE, NORMAL }
    enum class InfoType { TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS, RAM_USAGE, CPU_TEMP, UPTIME, NETWORK_SPEED, IP_ADDRESS }

    // ─────────────────────────────────────────────────────────────────────────
    // Guardas: estas condiciones hacen Delegate inmediato
    // ─────────────────────────────────────────────────────────────────────────

    private val QUESTION_PREFIXES = Regex(
        "^(como|como se|como puedo|como hago|como funciona|como se usa|como activo|" +
        "que es|que hace|que significa|cual es|cuando|cuanto|cuantos|cuantas|" +
        "por que|para que|sabes|puedes decirme|podrias decirme|dime como|" +
        "explica|explicame|necesito saber|quiero saber|me puedes|es posible|" +
        "hay alguna|existe|what|how|why|when|where|is there|can you|tell me how|" +
        // Variantes voz-a-texto comunes
        "como ke|ke es|komo|kiero saber)",
        RegexOption.IGNORE_CASE
    )

    private val EXTERNAL_CONTEXT = Regex(
        "\\b(del carro|del auto|del coche|del vehiculo|de mi carro|de mi auto|" +
        "del televisor|del tv|de la tele|del ordenador|del computador|de la computadora|" +
        "de la lampara|del foco|del switch|del interruptor|de casa|del cuarto|" +
        "del router|del modem|en el carro|en mi carro|del edificio|de la ciudad|" +
        "de la consola|del xbox|del playstation|del ps[0-9]|de la smart tv)",
        RegexOption.IGNORE_CASE
    )

    private val MULTI_TASK = Regex(
        "\\b(y luego|y despues|y tambien|y ademas|after that|and then|al mismo tiempo|mientras tanto|" +
        "y enseguida|despues de eso|a continuacion)",
        RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Normalización: quita tildes, signos, espacios, slang voz-a-texto
    // ─────────────────────────────────────────────────────────────────────────

    private fun normalize(text: String): String = text.trim()
        .replace(Regex("[¿¡.,;!?]+"), " ")
        .replace('á','a').replace('é','e').replace('í','i')
        .replace('ó','o').replace('ú','u').replace('ü','u')
        .replace('Á','A').replace('É','E').replace('Í','I')
        .replace('Ó','O').replace('Ú','U')
        // Variantes voz-a-texto fonéticas frecuentes
        .replace(Regex("\\bk\\b"), "que")
        .replace(Regex("\\bq\\b"), "que")
        .replace(Regex("\\bxfa\\b"), "por favor")
        .replace(Regex("\\bplis\\b"), "")
        .replace(Regex("\\bpliss\\b"), "")
        .replace(Regex("\\bporfa\\b"), "por favor")
        .replace(Regex("oye,?\\s*"), "")           // "oye pon..." → "pon..."
        .replace(Regex("hey,?\\s*"), "")            // "hey sube..." → "sube..."
        .replace(Regex("iris,?\\s*"), "")           // "iris pon..." → "pon..."
        .replace(Regex("doey,?\\s*"), "")           // "doey abre..." → "abre..."
        .replace(Regex("por favor\\.?$"), "")       // quitar "por favor" al final
        .replace(Regex("\\s+"), " ")
        .trim()

    // ─────────────────────────────────────────────────────────────────────────
    // Clasificador principal
    // ─────────────────────────────────────────────────────────────────────────

    fun classify(input: String): IntentClass {
        val lo = normalize(input).lowercase()

        if (QUESTION_PREFIXES.containsMatchIn(lo)) return IntentClass.Delegate
        if (EXTERNAL_CONTEXT.containsMatchIn(lo)) return IntentClass.Delegate

        if (MULTI_TASK.containsMatchIn(lo)) {
            val parts = splitSubtasks(input)
            if (parts.size >= 2) return IntentClass.Complex(parts)
        }

        tryLocal(lo)?.let { return IntentClass.Local(it) }

        return IntentClass.Delegate
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Motor de matching — orden importa (más específico primero)
    // ─────────────────────────────────────────────────────────────────────────

    private fun tryLocal(lo: String): LocalAction? =
        matchGreeting(lo)
            ?: matchFarewell(lo)
            ?: matchGratitude(lo)
            ?: matchAffirmation(lo)
            ?: matchMemoryQuery(lo)
            ?: matchFlashlight(lo)
            ?: matchVolume(lo)
            ?: matchSilent(lo)
            ?: matchBrightness(lo)
            ?: matchWifi(lo)
            ?: matchBluetooth(lo)
            ?: matchAirplane(lo)
            ?: matchDoNotDisturb(lo)
            ?: matchNfc(lo)
            ?: matchDarkMode(lo)
            ?: matchHotspot(lo)
            ?: matchScreenshot(lo)
            ?: matchLock(lo)
            ?: matchAlarm(lo)
            ?: matchTimer(lo)
            ?: matchStopwatch(lo)
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
            ?: matchNavigationActions(lo)
            ?: matchSettingsShortcuts(lo)
            ?: matchDeviceMaintenance(lo)
            ?: matchQuickApps(lo)
            ?: matchOpenApp(lo)

    // ─── Saludos ─────────────────────────────────────────────────────────────
    // Detecta saludos puros sin instrucción adicional

    private val GREETING_REGEX = Regex(
        "^(hola|hey|hi|buenas|buenos dias|buenas tardes|buenas noches|buen dia|que tal|" +
        "como estas|como te encuentras|como andas|que onda|que pedo|que paso|que hay|" +
        "ey|oye|que onda doey|hola doey|hey doey|hola iris|hey iris|que hubo|quiubo|" +
        "como estas?|como estas hoy|saludos|ola|alo|que hay de nuevo|que me cuentas)$",
        RegexOption.IGNORE_CASE
    )

    private val GREETING_RESPONSES = listOf(
        "¡Hola! 👋 Aquí estoy, ¿en qué te ayudo?",
        "¡Hola! Todo bien por acá. ¿Qué necesitas?",
        "¡Hey! ¿Cómo te puedo ayudar hoy?",
        "¡Buenas! Listo para lo que necesites 😊",
        "¡Hola! Dime, ¿qué hacemos hoy?",
        "¡Aquí estoy! ¿Qué se te ofrece?",
        "¡Hey! ¿Qué onda? ¿En qué te echó la mano?",
        "¡Hola hola! Cuéntame, ¿qué necesitas?",
        "¡Buenas! Listo y a tus órdenes 🤖",
        "¡Qué tal! Por acá todo bien, ¿y tú?"
    )

    private fun matchGreeting(lo: String): LocalAction? {
        if (!GREETING_REGEX.containsMatchIn(lo)) return null
        // Aseguramos que no lleve instrucción adicional
        if (lo.split(" ").size > 5) return null
        return LocalAction.Greeting((System.currentTimeMillis() % GREETING_RESPONSES.size).toInt())
    }

    // ─── Despedidas ──────────────────────────────────────────────────────────

    private val FAREWELL_REGEX = Regex(
        "^(adios|hasta luego|nos vemos|chao|chau|bye|hasta pronto|hasta manana|" +
        "me voy|me despido|cuiate|cuídate|hasta la proxima|nos vidrios|" +
        "a dormir|buenas noches doey|bye bye|bye doey|adios doey|chao doey)$",
        RegexOption.IGNORE_CASE
    )

    private val FAREWELL_RESPONSES = listOf(
        "¡Hasta luego! 👋 Aquí estaré cuando me necesites.",
        "¡Cuídate mucho! 😊",
        "¡Chao! Vuelve cuando quieras.",
        "¡Hasta pronto! Fue un placer ayudarte.",
        "¡Que te vaya bien! 👋",
        "¡Nos vemos! Aquí estaré.",
        "¡Cuídate! Y si necesitas algo, ya sabes dónde encontrarme."
    )

    private fun matchFarewell(lo: String): LocalAction? {
        if (!FAREWELL_REGEX.containsMatchIn(lo)) return null
        if (lo.split(" ").size > 6) return null
        return LocalAction.Farewell((System.currentTimeMillis() % FAREWELL_RESPONSES.size).toInt())
    }

    // ─── Gratitud / Agradecimientos ──────────────────────────────────────────

    private val GRATITUDE_REGEX = Regex(
        "^(gracias|muchas gracias|grax|grasias|te lo agradezco|muy amable|" +
        "thanks|thank you|cheers|de lujo|chido|estuvo bien|ok gracias|" +
        "gracias doey|gracias iris|perfecto gracias|listo gracias|" +
        "orale gracias|sale gracias|genial gracias|excelente gracias)$",
        RegexOption.IGNORE_CASE
    )

    private val GRATITUDE_RESPONSES = listOf(
        "¡De nada! 😊 Para eso estoy.",
        "¡Con gusto! ¿Algo más?",
        "¡A tus órdenes siempre! 🤖",
        "No hay de qué. ¿Necesitas algo más?",
        "¡Claro que sí! Aquí para lo que sea.",
        "¡Para eso estoy! ¿Algo más en lo que te ayude?",
        "¡Fue un placer! 😄"
    )

    private fun matchGratitude(lo: String): LocalAction? {
        if (!GRATITUDE_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Gratitude((System.currentTimeMillis() % GRATITUDE_RESPONSES.size).toInt())
    }

    // ─── Afirmaciones cortas (ok, listo, entendido…) ─────────────────────────

    private val AFFIRMATION_REGEX = Regex(
        "^(ok|okay|okey|listo|entendido|de acuerdo|sale|andale|orale|" +
        "perfecto|excelente|bien|muy bien|chevere|genial|super|al 100|" +
        "esta bien|esta ok|ya entendi|ya vi|lo tengo|roger|copy)$",
        RegexOption.IGNORE_CASE
    )

    private val AFFIRMATION_RESPONSES = listOf(
        "👍 ¡Listo!",
        "¡Perfecto! ¿Algo más?",
        "👌 Entendido.",
        "¡Genial! Aquí por si me necesitas.",
        "😊 ¡De nada!",
        "¡Sale! Avísame si necesitas algo."
    )

    private fun matchAffirmation(lo: String): LocalAction? {
        if (!AFFIRMATION_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Affirmation((System.currentTimeMillis() % AFFIRMATION_RESPONSES.size).toInt())
    }

    // ─── Consulta de memorias ─────────────────────────────────────────────────
    // Si IRIS no puede resolver un contacto, puede buscar en memorias del usuario

    private fun matchMemoryQuery(lo: String): LocalAction? {
        val m = Regex(
            "^(?:quien es|quien era|que es|a quien conoce|busca en mis memorias|" +
            "en mis memorias busca|recuerdas quien es|recuerdas a)\\s+(.{2,40})$"
        ).find(lo) ?: return null
        return LocalAction.QueryMemory(m.groupValues[1].trim())
    }

    // ─── Linterna ────────────────────────────────────────────────────────────

    private fun matchFlashlight(lo: String): LocalAction? {
        val TORCH = "(linterna|flash\\b|torch|luz trasera|flas)"  // "flas" = voz-a-texto
        val ON  = Regex("\\b(enciende|prende|activa|turn on|on|dale|mete)\\b.*$TORCH")
        val OFF = Regex("\\b(apaga|desactiva|apagar|turn off|off|quita)\\b.*$TORCH")
        // Orden inverso: "la linterna prende"
        val ON2  = Regex("$TORCH.*\\b(on|activa|prende|enciende)\\b")
        val OFF2 = Regex("$TORCH.*\\b(off|apaga|desactiva)\\b")
        val SOLO = Regex("^linterna$")
        return when {
            ON.containsMatchIn(lo) || ON2.containsMatchIn(lo)   -> LocalAction.ToggleFlashlight(true)
            OFF.containsMatchIn(lo) || OFF2.containsMatchIn(lo) -> LocalAction.ToggleFlashlight(false)
            SOLO.matches(lo)                                     -> LocalAction.ToggleFlashlight(true)
            else                                                 -> null
        }
    }

    // ─── Volumen ─────────────────────────────────────────────────────────────

    private fun matchVolume(lo: String): LocalAction? {
        val UP   = Regex("\\b(sube|aumenta|incrementa|raise|turn up|volume up|mas volumen|subele|subelo|jalale|mas alto)\\b.*(volumen|volume|sonido|audio)?")
        val DOWN = Regex("\\b(baja|reduce|disminuye|lower|turn down|volume down|menos volumen|bajale|bajalo|menos alto)\\b.*(volumen|volume|sonido|audio)?")

        // Stream de timbre / ringtone
        val RING_UP   = Regex("\\b(sube|aumenta|mas)\\b.*(timbre|ringtone|tono de llamada)")
        val RING_DOWN = Regex("\\b(baja|reduce|menos)\\b.*(timbre|ringtone|tono de llamada)")
        // Stream de alarma
        val ALARM_UP   = Regex("\\b(sube|aumenta|mas)\\b.*(volumen.*(de|del).*alarma|alarma.*volumen)")
        val ALARM_DOWN = Regex("\\b(baja|reduce|menos)\\b.*(volumen.*(de|del).*alarma|alarma.*volumen)")

        if (RING_UP.containsMatchIn(lo))   return LocalAction.VolumeStep(up = true,  stream = VolumeStream.RING)
        if (RING_DOWN.containsMatchIn(lo)) return LocalAction.VolumeStep(up = false, stream = VolumeStream.RING)
        if (ALARM_UP.containsMatchIn(lo))   return LocalAction.VolumeStep(up = true,  stream = VolumeStream.ALARM)
        if (ALARM_DOWN.containsMatchIn(lo)) return LocalAction.VolumeStep(up = false, stream = VolumeStream.ALARM)

        if (UP.containsMatchIn(lo) && !lo.contains("tele") && !lo.contains("tv"))
            return LocalAction.VolumeStep(up = true)
        if (DOWN.containsMatchIn(lo) && !lo.contains("tele") && !lo.contains("tv"))
            return LocalAction.VolumeStep(up = false)

        // Valor exacto con % o sin %
        Regex("\\b(volumen|volume|sonido)\\b[^\\d]*(\\d{1,3})\\s*%?").find(lo)?.let {
            val lvl = it.groupValues[2].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetVolume(lvl)
        }
        Regex("\\b(pon|ajusta|set|coloca|dejalo|ponlo)\\b[^\\d]*(\\d{1,3})\\s*%?[^\\w]*(volumen|volume|sonido|de sonido)").find(lo)?.let {
            val lvl = it.groupValues[2].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetVolume(lvl)
        }
        // "al máximo" / "al mínimo"
        if (Regex("\\b(volumen|sonido|audio)\\b.*(maximo|full|100|al tope|a tope)").containsMatchIn(lo))
            return LocalAction.SetVolume(100)
        if (Regex("\\b(volumen|sonido|audio)\\b.*(minimo|0|cero|mudo)").containsMatchIn(lo))
            return LocalAction.SetVolume(0)
        return null
    }

    // ─── Silencio / Vibración ────────────────────────────────────────────────

    private fun matchSilent(lo: String): LocalAction? = when {
        Regex("\\b(silencia(r)?|mute|pon.*silencio|modo silencio|silent mode|poner.*mudo|" +
              "modo mudo|pon.*mudo|mutea|mutear|hazlo mudo)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.SILENT)
        Regex("\\b(vibra(cion)?|vibrate|modo vibracion|pon.*vibracion|solo vibra|vibrar)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.VIBRATE)
        Regex("\\b(quita.*silencio|desmutea|unmute|sonido normal|modo normal|modo sonido|" +
              "activa.*sonido|quita.*mudo|dejar sonar|que suene)\\b").containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.NORMAL)
        else -> null
    }

    // ─── Brillo ──────────────────────────────────────────────────────────────

    private fun matchBrightness(lo: String): LocalAction? {
        val BRIGHT = "(brillo|brightness|pantalla mas (clara|oscura)|iluminacion)"
        if (Regex("\\b(activa|pon|usa|enable|auto)\\b.*(brillo automatico|auto.?brillo|adaptive brightness|brillo auto)").containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(true)
        if (Regex("\\b(desactiva|quita|disable)\\b.*(brillo automatico|auto.?brillo|adaptive brightness)").containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(false)
        if (Regex("\\b(sube|aumenta|raise|turn up|mas brillo|subele el brillo|mas luz)\\b.*$BRIGHT|$BRIGHT.*(arriba|mas)").containsMatchIn(lo))
            return LocalAction.BrightnessStep(up = true)
        if (Regex("\\b(baja|reduce|lower|turn down|menos brillo|bajale el brillo|menos luz)\\b.*$BRIGHT|$BRIGHT.*(abajo|menos)").containsMatchIn(lo))
            return LocalAction.BrightnessStep(up = false)
        // "brillo al máximo / mínimo"
        if (Regex("\\b$BRIGHT\\b.*(maximo|100|tope|full)").containsMatchIn(lo)) return LocalAction.SetBrightness(100)
        if (Regex("\\b$BRIGHT\\b.*(minimo|0|cero|nada|oscuro)").containsMatchIn(lo)) return LocalAction.SetBrightness(0)
        Regex("\\b$BRIGHT\\b[^\\d]*(\\d{1,3})\\s*%?").find(lo)?.let {
            val lvl = it.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: return null
            return LocalAction.SetBrightness(lvl)
        }
        return null
    }

    // ─── WiFi ─────────────────────────────────────────────────────────────────
    // Cubre: wifi, wi-fi, wi fi, internet, red wifi, el internet, los datos wifi

    private fun matchWifi(lo: String): LocalAction? {
        val wifi = "\\b(wifi|wi.?fi|el internet inalambrico|red inalambrica|la red wifi|internet wifi|el wifi|la wifi|wi fi)\\b"
        // "enciende el internet" → solo si NO hay "datos moviles" (eso es otra cosa)
        val internet = "\\b(internet|la red|el internet)\\b"
        val on  = "\\b(activa|enciende|prende|conecta|turn on|enable|mete|dale|pon|prender|activar|encender|conectar)\\b"
        val off = "\\b(desactiva|apaga|desconecta|turn off|disable|quita|saca|apagar|desactivar)\\b"

        if (lo.contains("datos moviles") || lo.contains("datos celulares") || lo.contains("datos del celular")) return null

        return when {
            Regex("$on.*($wifi)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($wifi).*\\b(on|activo|activalo|prendelo|encendido)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            (Regex("$on.*$internet", RegexOption.IGNORE_CASE).containsMatchIn(lo) &&
             !lo.contains("datos") && !lo.contains("movil") && !lo.contains("4g") && !lo.contains("5g"))
                -> LocalAction.ToggleWifi(true)
            Regex("$off.*($wifi)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($wifi).*\\b(off|apagado|apagalo|desactivalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleWifi(false)
            else -> null
        }
    }

    // ─── Bluetooth ───────────────────────────────────────────────────────────
    // Cubre: bluetooth, bt, b.t., blutus, bluetoth, blue tooth, el blue, bluetooh

    private fun matchBluetooth(lo: String): LocalAction? {
        val bt = "\\b(bluetooth|bt\\b|b\\.t\\.|blutus|bluetoth|blue tooth|el blue|bluetooh|bluetu[st]|bl[uü]etooth)\\b"
        val on  = "\\b(activa|enciende|prende|turn on|enable|mete|dale|conecta|jala|abre|sube|pon|prender|activar|encender)\\b"
        val off = "\\b(desactiva|apaga|turn off|disable|quita|saca|corta|desconecta|apagar|desactivar)\\b"
        return when {
            Regex("$on.*$bt", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("$bt.*\\b(on|activo|activalo|prendelo|prendido|encendido)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleBluetooth(true)
            Regex("$off.*$bt", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("$bt.*\\b(off|apagado|apagalo|desactivalo|desactivado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleBluetooth(false)
            else -> null
        }
    }

    // ─── Modo avión ───────────────────────────────────────────────────────────

    private fun matchAirplane(lo: String): LocalAction? {
        val air = "\\b(modo avion|airplane mode|flight mode|modo vuelo|modo aereo)\\b"
        return when {
            Regex("\\b(activa|enciende|pon|turn on|mete)\\b.*($air)").containsMatchIn(lo) ||
            Regex("($air).*\\b(on|activo)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(true)
            Regex("\\b(desactiva|apaga|quita|turn off|saca)\\b.*($air)").containsMatchIn(lo) ||
            Regex("($air).*\\b(off|apagado)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(false)
            else -> null
        }
    }

    // ─── No molestar ──────────────────────────────────────────────────────────

    private fun matchDoNotDisturb(lo: String): LocalAction? {
        val dnd = "\\b(no molestar|dnd|do not disturb|no interrumpir|no interrupciones|no me molestes)\\b"
        return when {
            Regex("\\b(activa|enciende|pon|enable|mete)\\b.*($dnd)").containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(true)
            Regex("\\b(desactiva|apaga|quita|disable|saca)\\b.*($dnd)").containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(false)
            else -> null
        }
    }

    // ─── NFC ──────────────────────────────────────────────────────────────────

    private fun matchNfc(lo: String): LocalAction? = when {
        Regex("\\b(activa|enciende|prende|turn on|enable)\\b.*\\bnfc\\b").containsMatchIn(lo) ||
        Regex("\\bnfc\\b.*\\b(on|activo|activa)\\b").containsMatchIn(lo)
            -> LocalAction.ToggleNfc(true)
        Regex("\\b(desactiva|apaga|turn off|disable)\\b.*\\bnfc\\b").containsMatchIn(lo) ||
        Regex("\\bnfc\\b.*\\b(off|apagado|desactiva)\\b").containsMatchIn(lo)
            -> LocalAction.ToggleNfc(false)
        else -> null
    }

    // ─── Modo oscuro ─────────────────────────────────────────────────────────

    private fun matchDarkMode(lo: String): LocalAction? {
        val dark = "\\b(modo oscuro|dark mode|modo noche|tema oscuro|modo dark)\\b"
        val light = "\\b(modo claro|light mode|modo dia|tema claro|modo normal)\\b"
        return when {
            Regex("\\b(activa|pon|enable|mete|usa)\\b.*($dark)").containsMatchIn(lo) ||
            Regex("($dark).*\\b(on|activo)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(true)
            Regex("\\b(desactiva|quita|disable|saca)\\b.*($dark)").containsMatchIn(lo) ||
            Regex("($light)").containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(false)
            else -> null
        }
    }

    // ─── Hotspot / Punto de acceso ───────────────────────────────────────────

    private fun matchHotspot(lo: String): LocalAction? {
        val hot = "\\b(hotspot|punto de acceso|compartir datos|tethering|internet movil compartido)\\b"
        return when {
            Regex("\\b(activa|enciende|prende|turn on|enable|mete|pon)\\b.*($hot)").containsMatchIn(lo) ||
            Regex("($hot).*\\b(on|activo|activa)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(true)
            Regex("\\b(desactiva|apaga|turn off|disable|quita|saca)\\b.*($hot)").containsMatchIn(lo) ||
            Regex("($hot).*\\b(off|apagado|desactiva)\\b").containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(false)
            else -> null
        }
    }

    // ─── Captura / Bloqueo ────────────────────────────────────────────────────

    private fun matchScreenshot(lo: String): LocalAction? {
        return if (Regex("\\b(toma|saca|haz|captura|take|screenshot|printscreen)\\b.*(captura|screenshot|pantalla|screen shot|foto de pantalla|screenshotear)").containsMatchIn(lo) ||
                   Regex("^(screenshot|captura de pantalla|foto de pantalla)$").containsMatchIn(lo))
            LocalAction.TakeScreenshot() else null
    }

    private fun matchLock(lo: String): LocalAction? {
        return if (Regex("^(bloquea(r)?( el telefono| la pantalla| el cel| el movil)?|lock( the)? (screen|phone)|apaga(r)? la pantalla|bloquear cel|bloquear celu)$").containsMatchIn(lo))
            LocalAction.LockScreen() else null
    }

    // ─── Alarmas ──────────────────────────────────────────────────────────────

    private fun matchAlarm(lo: String): LocalAction? {
        if (Regex("\\b(cancela|elimina|borra|quita|cancel|delete|apaga|desactiva)\\b.*(alarma|alarm|despertador)").containsMatchIn(lo))
            return LocalAction.CancelAlarm()

        val hasTrigger = Regex("\\b(pon|pone|ponme|set|crea|activa|programa|despiertame|despertarme|wake me|alarma|alarm|despertador|ponme una|pon una)\\b").containsMatchIn(lo)
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

        // "a las N am/pm / de la mañana / tarde / noche / en punto"
        Regex("(?:a las?|at|para las?)\\s*(\\d{1,2})\\s*(am|pm|de la manana|de la mañana|de la tarde|de la noche|en punto|hrs?|h\\b)?").find(lo)?.let { m ->
            var h   = m.groupValues[1].toIntOrNull() ?: return null
            val mod = m.groupValues[2].lowercase()
            when {
                mod.contains("pm") || mod.contains("tarde") || mod.contains("noche") -> if (h < 12) h += 12
                mod.contains("am") || mod.contains("mana") -> if (h == 12) h = 0
            }
            return LocalAction.SetAlarm(h, 0, extractLabel(lo))
        }

        // "en N horas"
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
        if (Regex("\\b(cancela|detiene|para|stop|cancel|apaga)\\b.*(temporizador|timer|cuenta regresiva|cronometro)").containsMatchIn(lo))
            return LocalAction.CancelTimer()

        if (!Regex("\\b(pon|pone|ponme|set|crea|inicia|activa|temporizador|timer|cuenta regresiva|countdown|cronometro de|cuenta de)\\b").containsMatchIn(lo))
            return null

        var secs = 0L
        Regex("(\\d+)\\s*hora").find(lo)?.let { secs += it.groupValues[1].toLong() * 3600 }
        Regex("(\\d+)\\s*(minuto|min\\b)").find(lo)?.let { secs += it.groupValues[1].toLong() * 60 }
        Regex("(\\d+)\\s*(segundo|seg\\b|sec\\b)").find(lo)?.let { secs += it.groupValues[1].toLong() }

        // "media hora" = 30 min, "cuarto de hora" = 15 min
        if (lo.contains("media hora")) secs += 1800
        if (lo.contains("cuarto de hora") || lo.contains("15 minutos")) secs = if (secs == 0L) 900 else secs

        return if (secs > 0) LocalAction.SetTimer(secs) else null
    }

    // ─── Cronómetro ──────────────────────────────────────────────────────────

    private fun matchStopwatch(lo: String): LocalAction? = when {
        Regex("\\b(inicia|empieza|start|pon|activa)\\b.*(cronometro|stopwatch)").containsMatchIn(lo) ||
        lo == "cronometro"
            -> LocalAction.StartStopwatch()
        Regex("\\b(detiene|para|stop|pausa|cancela)\\b.*(cronometro|stopwatch)").containsMatchIn(lo)
            -> LocalAction.StopStopwatch()
        else -> null
    }

    // ─── Llamadas de emergencia ───────────────────────────────────────────────

    private fun matchEmergencyCall(lo: String): LocalAction? {
        if (!Regex("\\b(llama(r)?|call|marca|dial|marcame|comunicame)\\b").containsMatchIn(lo)) return null
        return when {
            lo.contains("911")        -> LocalAction.CallEmergency("911")
            lo.contains("112")        -> LocalAction.CallEmergency("112")
            lo.contains("066")        -> LocalAction.CallEmergency("066")
            lo.contains("080")        -> LocalAction.CallEmergency("080")
            lo.contains("ambulancia") -> LocalAction.CallEmergency("112")
            lo.contains("bomberos")   -> LocalAction.CallEmergency("911")
            lo.contains("policia") || lo.contains("policía") -> LocalAction.CallEmergency("911")
            lo.contains("emergencia") && Regex("\\d{3}").containsMatchIn(lo) ->
                LocalAction.CallEmergency(Regex("\\d{3}").find(lo)!!.value)
            else -> null
        }
    }

    // ─── Llamada normal ───────────────────────────────────────────────────────

    private fun matchCall(lo: String): LocalAction? {
        val m = Regex("^(llama(r)?(?: a)?|call|marca(r)?(?: a)?|comunicame con|conectame con|habla(r)? con|marcar a)\\s+(.{2,40})$").find(lo)
            ?: return null
        val contact = m.groupValues.last().trim()
        if (contact.split(" ").size > 5) return null
        return LocalAction.Call(contact)
    }

    // ─── WhatsApp ─────────────────────────────────────────────────────────────
    // Cubre: whatsapp, whats, wapp, wa, W.A., guasap, wasa, what sap, wap

    private fun matchWhatsApp(lo: String): LocalAction? {
        val isWA = lo.contains("whatsapp") || lo.contains("whats") || lo.contains("wapp") ||
                   lo.contains("what sap") || lo.contains("guasap") || lo.contains("wasa") ||
                   Regex("\\bwa\\b").containsMatchIn(lo) || Regex("\\bw\\.a\\.\\b").containsMatchIn(lo)
        if (!isWA) return null

        // Patrón 1: "manda/envía [un] [mensaje/whatsapp] a CONTACTO [por wa] diciendo/que diga MENSAJE"
        Regex(
            "^(?:manda|envia|escribe|send|dile|avisa|manda un|envia un)\\s+" +
            "(?:un\\s+)?(?:mensaje|whatsapp|wha?ts?a?p?p?|guasap|wa|wapp|wasa)?\\s*" +
            "(?:a\\s+)?(.+?)\\s+" +
            "(?:por\\s+(?:whatsapp|wha?ts?|guasap|wa|wapp)\\s+)?" +
            "(?:diciendo|que diga|que dice|que le digas|:|con el texto|con mensaje|el mensaje|el texto)\\s+(.+)$"
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim().removePrefix("a ").trim()
            val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank() && c.length < 40) return LocalAction.SendWhatsApp(c, msg)
        }

        // Patrón 2: "manda por whatsapp a CONTACTO: MENSAJE"
        Regex(
            "^(?:manda|envia|send)\\s+(?:por\\s+)?(?:whatsapp|wha?ts?|guasap|wa\\b|wapp)\\s+" +
            "(?:a\\s+)?(.+?)[: ]+(.+)$"
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim()
            val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }

        // Patrón 3: "wa a CONTACTO MENSAJE" (forma ultra-corta)
        Regex(
            "^(?:wa|wapp)\\s+a\\s+(.+?)\\s+(?:diciendo|que diga|:|el mensaje|dile)\\s+(.+)$"
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim()
            val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }

        // Solo abrir chat
        Regex(
            "^(?:abre|abrir|chatea con|chatear con|open|mensaje a|habla con|hablar con|" +
            "abre el chat de|ve al chat de)\\s+" +
            "(?:(?:whatsapp|wha?ts?|guasap|wa\\b)\\s+(?:con|de)\\s+|" +
            "chat de (?:whatsapp|wha?ts?)\\s+con\\s+)?(.+)$"
        ).find(lo)?.let { m ->
            val c = m.groupValues[1].trim()
                .removePrefix("en whatsapp").removePrefix("por whatsapp")
                .removePrefix("en wa").removePrefix("por wa").trim()
            if (c.isNotBlank() && !c.contains("whatsapp") && !c.contains("\\bwa\\b".toRegex()))
                return LocalAction.OpenWhatsAppChat(c)
        }
        return null
    }

    // ─── Telegram ────────────────────────────────────────────────────────────

    private fun matchTelegram(lo: String): LocalAction? {
        if (!lo.contains("telegram")) return null
        Regex("^(?:manda|envia|escribe|send)\\s+(?:un\\s+)?(?:mensaje|telegram)?\\s*(?:a\\s+)?(.+?)\\s+(?:por\\s+telegram\\s+)?(?:diciendo|que diga|que dice|:|con el texto|el mensaje)\\s+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendTelegram(c, msg)
        }
        return null
    }

    // ─── SMS ─────────────────────────────────────────────────────────────────

    private fun matchSms(lo: String): LocalAction? {
        if (lo.contains("whatsapp") || lo.contains("telegram") || lo.contains("guasap")) return null
        Regex("^(?:manda|envia|escribe|send)\\s+(?:un\\s+)?(?:sms|mensaje|texto|mensajito)\\s+(?:a\\s+)?(.+?)\\s+(?:diciendo|que diga|que dice|:|con el texto|con mensaje|el texto)\\s+(.+)$").find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendSms(c, msg)
        }
        return null
    }

    // ─── Navegación ───────────────────────────────────────────────────────────

    private fun matchNavigation(lo: String): LocalAction? {
        val m = Regex("^(?:llevame a|navega a|como llego a|como voy a|navigate to|directions? to|lleva a|ir a|" +
                      "ruta a|ruta hacia|lleva(me)? hasta|quiero ir a|como llegar a|guiame a)\\s+(.+)$").find(lo) ?: return null
        val dest = m.groupValues.last().trim()
        return if (dest.isNotBlank() && dest.length < 100) LocalAction.Navigate(dest) else null
    }

    // ─── Búsqueda web ─────────────────────────────────────────────────────────

    private fun matchWebSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca(r)?|busca en (?:google|internet|web|la web)|search(?:\\s+for)?|googlea|" +
                      "busqueda de|googlear|buscar en internet)\\s+(.{3,120})$").find(lo) ?: return null
        val q = m.groupValues.last().trim()
        return if (q.isNotBlank()) LocalAction.SearchWeb(q) else null
    }

    // ─── Búsqueda en Maps ─────────────────────────────────────────────────────

    private fun matchMapsSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca(r)? en (?:maps|google maps)|muestra(me)? en (?:el mapa|maps)|" +
                      "encuentra en maps|busca en el mapa|donde esta|donde queda)\\s+(.{3,80})$").find(lo) ?: return null
        val q = m.groupValues.last().trim()
        return if (q.isNotBlank()) LocalAction.SearchMaps(q) else null
    }

    // ─── Controles de reproducción ────────────────────────────────────────────

    private fun matchMediaControl(lo: String): LocalAction? = when {
        Regex("^(pausa(r)?|pause|deten la musica|para la musica|parar musica|stop music|detente|pausalo|pausala)$").containsMatchIn(lo)
            -> LocalAction.PauseMusic()
        Regex("^(resume|reanuda(r)?|continua(r)? musica|sigue la musica|play again|seguir musica|dale play|dale|sigue)$").containsMatchIn(lo)
            -> LocalAction.ResumeMusic()
        Regex("^(siguiente|proxima cancion|next|salta(r)?|skip|siguiente cancion|siguiente pista|la que sigue|cambia)$").containsMatchIn(lo)
            -> LocalAction.NextTrack()
        Regex("^(anterior|cancion anterior|prev(ia)?|back|atras|pista anterior|regresa(r)?|la de antes)$").containsMatchIn(lo)
            -> LocalAction.PrevTrack()
        Regex("^(aleatoria|shuffle|modo aleatorio|random|mezclar)$").containsMatchIn(lo)
            -> LocalAction.ShuffleMusic()
        Regex("^(repite|repeat|repetir|loop|en loop)$").containsMatchIn(lo)
            -> LocalAction.RepeatToggle()
        else -> null
    }

    // ─── Música ───────────────────────────────────────────────────────────────

    private fun matchMusic(lo: String): LocalAction? {
        val musicTrigger = Regex("\\b(pon|reproduce|play|toca|ver|mira|busca|quiero ver|quiero escuchar|escucha|escuchar|ponme|dale play a)\\b")
        if (!musicTrigger.containsMatchIn(lo)) return null

        // "pon/ver un video de X en youtube"
        Regex("^(?:pon|reproduce|play|ver|mira|busca)\\s+(?:un\\s+)?(?:video|videos)\\s+(?:de\\s+)?['\"]?(.+?)['\"]?\\s+en\\s+(youtube|yt|youtube music|yt music)").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        // "ver X en youtube"
        Regex("^(?:pon|reproduce|play|ver|mira|busca|quiero ver)\\s+(?:un\\s+)?(?:video|videos)\\s+(?:de\\s+)?['\"]?(.{2,80}?)['\"]?\\s*$").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        Regex("^(?:ver|mira|busca)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(youtube|yt)\\s*$").find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, "youtube")
        }
        // "pon CANCIÓN en PLATAFORMA"
        Regex("^(?:pon|reproduce|play|toca|escucha|ponme|dale play a)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)$").find(lo)?.let { m ->
            val q   = m.groupValues[1].trim()
            val app = resolveApp(m.groupValues[2])
            if (q.isNotBlank()) return LocalAction.PlayMusic(q, app)
        }
        // "abre/pon música/spotify" (sin canción)
        Regex("^(?:pon|abre|reproduce|play)\\s+(?:musica\\s+)?(?:en\\s+)?(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)$").find(lo)?.let { m ->
            return LocalAction.PlayMusic("", resolveApp(m.groupValues[1]))
        }
        // "pon/toca CANCIÓN" sin plataforma → Spotify
        val blacklist = setOf("volumen","brillo","wifi","bluetooth","alarma","linterna","silencio","modo","pantalla","video","videos","whatsapp","telegram")
        Regex("^(?:pon|reproduce|play|toca|escucha|quiero escuchar|ponme|dale play a)\\s+(?:la cancion\\s+|la musica\\s+|el tema\\s+|la rola\\s+)?['\"]?(.{2,60}?)['\"]?\\s*$").find(lo)?.let { m ->
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
        raw.contains("soundcloud") -> "soundcloud"
        else -> "spotify"
    }

    // ─── URL directa ─────────────────────────────────────────────────────────

    private fun matchUrl(lo: String): LocalAction? {
        val m = Regex("^(?:abre?|ve a|open|go to|entra a|ir a)\\s+(https?://\\S+|www\\.\\S+)$").find(lo) ?: return null
        var url = m.groupValues[1].trim()
        if (!url.startsWith("http")) url = "https://$url"
        return LocalAction.OpenUrl(url)
    }

    // ─── Consultas del sistema ────────────────────────────────────────────────

    private fun matchSystemQuery(lo: String): LocalAction? = when {
        Regex("\\b(que hora es|dime la hora|what time is it|hora actual|que hora son|la hora)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.TIME)
        Regex("\\b(que dia es|que fecha es|what day is it|fecha actual|que dia estamos|la fecha)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.DATE)
        Regex("\\b(cuanta bateria|nivel de bateria|battery level|como esta la bateria|cuanto de bateria|porcentaje de bateria|la bateria|bateria del cel)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BATTERY)
        Regex("\\b(cuanto espacio|almacenamiento disponible|espacio libre|storage available|cuanto almacenamiento|cuanta memoria|el almacenamiento)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.STORAGE)
        Regex("\\b(esta (el )?wifi (activo|conectado|on)|tengo wifi|wifi esta on|el wifi)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.WIFI_STATUS)
        Regex("\\b(esta (el )?bluetooth (activo|on)|tengo bluetooth activo)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BT_STATUS)
        Regex("\\b(cuanta ram|uso de ram|memoria ram|ram libre|la ram)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.RAM_USAGE)
        Regex("\\b(temperatura|esta caliente|temp del cpu|calentamiento|el cpu|que tan caliente)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.CPU_TEMP)
        Regex("\\b(tiempo encendido|uptime|cuanto lleva prendido|cuanto tiene encendido)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.UPTIME)
        Regex("\\b(velocidad de internet|velocidad de red|ping|que tan rapido|el internet esta rapido)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.NETWORK_SPEED)
        Regex("\\b(mi ip|ip del celular|ip del telefono|cual es mi ip|ip local)\\b").containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.IP_ADDRESS)
        else -> null
    }

    // ─── Navegación del Sistema ──────────────────────────────────────────────

    private fun matchNavigationActions(lo: String): LocalAction? = when {
        Regex("\\b(ve a inicio|pantalla principal|go home|vuelve a inicio|ir a home|inicio|home)\\b").containsMatchIn(lo) &&
        !lo.contains("ajuste") && !lo.contains("setting") && !lo.contains("aplicaci")
            -> LocalAction.GoHome()
        Regex("\\b(atras|regresa|vuelve|go back|boton atras|pa atras|regresar)\\b").containsMatchIn(lo) &&
        lo.length < 20  // solo si es un comando corto, evitar falsas capturas
            -> LocalAction.BackButton()
        Regex("\\b(apps recientes|multitarea|cambiar de app|recent apps|ver aplicaciones abiertas|apps abiertas)\\b").containsMatchIn(lo)
            -> LocalAction.ShowRecentApps()
        else -> null
    }

    // ─── Accesos Directos a Ajustes ──────────────────────────────────────────

    private fun matchSettingsShortcuts(lo: String): LocalAction? = when {
        Regex("\\b(ajustes|configuracion|settings|configurar|preferencias)\\b").containsMatchIn(lo) -> when {
            lo.contains("wifi") || lo.contains("wi-fi") || lo.contains("red") -> LocalAction.OpenWifiSettings()
            lo.contains("bluetooth") || lo.contains("bt") -> LocalAction.OpenBluetoothSettings()
            lo.contains("bateria") || lo.contains("pila") || lo.contains("carga") -> LocalAction.OpenBatterySettings()
            lo.contains("pantalla") || lo.contains("brillo") || lo.contains("display") -> LocalAction.OpenDisplaySettings()
            lo.contains("sonido") || lo.contains("volumen") || lo.contains("audio") -> LocalAction.OpenSoundSettings()
            lo.contains("almacenamiento") || lo.contains("espacio") || lo.contains("memoria") -> LocalAction.OpenStorageSettings()
            lo.contains("ubicacion") || lo.contains("gps") || lo.contains("localizacion") -> LocalAction.OpenLocationSettings()
            lo.contains("seguridad") || lo.contains("huella") || lo.contains("pin") -> LocalAction.OpenSecuritySettings()
            lo.contains("aplicaciones") || lo.contains("apps") -> LocalAction.OpenAppsSettings()
            lo.contains("fecha") || lo.contains("hora") || lo.contains("reloj") -> LocalAction.OpenDateSettings()
            lo.contains("idioma") || lo.contains("lenguaje") -> LocalAction.OpenLanguageSettings()
            lo.contains("accesibilidad") -> LocalAction.OpenAccessibilitySettings()
            lo.contains("desarrollador") || lo.contains("developer") -> LocalAction.OpenDeveloperSettings()
            lo.contains("notificaciones") -> LocalAction.OpenNotificationSettings()
            lo.contains("privacidad") -> LocalAction.OpenPrivacySettings()
            lo.contains("cuenta") || lo.contains("google") -> LocalAction.OpenAccountSettings()
            lo.contains("nfc") -> LocalAction.OpenNfcSettings()
            lo.contains("datos") || lo.contains("data") -> LocalAction.OpenDataUsageSettings()
            lo.contains("vpn") -> LocalAction.OpenVpnSettings()
            lo.contains("sincronizacion") || lo.contains("sync") -> LocalAction.OpenSyncSettings()
            lo.contains("teclado") || lo.contains("entrada") -> LocalAction.OpenInputMethodSettings()
            lo.contains("sueño") || lo.contains("descanso") -> LocalAction.OpenDreamSettings()
            else -> LocalAction.OpenSettings()
        }
        else -> null
    }

    // ─── Mantenimiento y Dispositivo ─────────────────────────────────────────

    private fun matchDeviceMaintenance(lo: String): LocalAction? {
        if (Regex("\\b(limpia|borra|quita|clear|elimina)\\b.*(notificaciones|avisos|alerts)").containsMatchIn(lo))
            return LocalAction.ClearNotifications()

        if (Regex("\\b(ahorro de energia|modo ahorro|power save|ahorro de bateria|bajo consumo)\\b").containsMatchIn(lo))
            return LocalAction.TogglePowerSave()

        Regex("\\b(apaga la pantalla en|tiempo de espera|screen timeout|pantalla se apague en)\\b.*(\\d+)\\s*(segundos|minutos)?").find(lo)?.let { m ->
            val value = m.groupValues[2].toIntOrNull() ?: return@let
            val unit = m.groupValues[3]
            val seconds = if (unit == "minutos") value * 60 else value
            return LocalAction.SetScreenTimeout(seconds)
        }

        return null
    }

    // ─── Apps comunes por nombre rápido ──────────────────────────────────────

    private fun matchQuickApps(lo: String): LocalAction? = when {
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(la )?camara$").containsMatchIn(lo)
            -> LocalAction.OpenCamera()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(la )?galeria$").containsMatchIn(lo) ||
        Regex("^(mis fotos|ver fotos|la galeria)$").containsMatchIn(lo)
            -> LocalAction.OpenGallery()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(los |mis )?contactos?$").containsMatchIn(lo)
            -> LocalAction.OpenContacts()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(el )?marcador|(abrir|ver) el marcador$").containsMatchIn(lo)
            -> LocalAction.OpenDialer()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(la )?calculadora?$").containsMatchIn(lo)
            -> LocalAction.OpenCalculator()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(el )?calendario$").containsMatchIn(lo)
            -> LocalAction.OpenCalendar()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(google )?maps?$").containsMatchIn(lo)
            -> LocalAction.OpenMaps()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(el )?(navegador|browser|chrome|firefox|edge)$").containsMatchIn(lo)
            -> LocalAction.OpenBrowser()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(el )?(explorador de archivos|mis archivos|archivos|files)$").containsMatchIn(lo)
            -> LocalAction.OpenFiles()
        Regex("^(abre|lanza|entra a|ve a|open)\\s+(el )?(reloj|clock)$").containsMatchIn(lo)
            -> LocalAction.OpenClock()
        else -> null
    }

    // ─── Abrir app genérico — siempre al final ────────────────────────────────

    private fun matchOpenApp(lo: String): LocalAction? {
        val m = Regex("^(?:abre?|lanza|inicia|entra a|ve a|open|launch|start|dale a)\\s+(?:la\\s+app\\s+(?:de\\s+)?|la\\s+aplicacion\\s+(?:de\\s+)?|el\\s+)?(.{2,30})$").find(lo) ?: return null
        val appName = m.groupValues[1].trim()
        val geoWords = listOf("calle","avenida","colonia","ciudad","pais","cerca","pueblo","estado")
        if (geoWords.any { appName.contains(it) }) return null
        if (appName.split(" ").size > 4) return null
        return LocalAction.OpenApp(appName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Splitter de sub-tareas
    // ─────────────────────────────────────────────────────────────────────────

    private fun splitSubtasks(text: String): List<String> {
        var parts = listOf(text)
        listOf(
            Regex("\\s+y luego\\s+",      RegexOption.IGNORE_CASE),
            Regex("\\s+y despues\\s+",    RegexOption.IGNORE_CASE),
            Regex("\\s+y tambien\\s+",    RegexOption.IGNORE_CASE),
            Regex("\\s+ademas\\s+",       RegexOption.IGNORE_CASE),
            Regex("\\s+and then\\s+",     RegexOption.IGNORE_CASE),
            Regex("\\s+y enseguida\\s+",  RegexOption.IGNORE_CASE),
            Regex("\\s+despues de eso\\s+", RegexOption.IGNORE_CASE)
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

    /**
     * Intenta encontrar un número de teléfono en el texto de memorias del usuario.
     * Útil cuando el contacto no está en la agenda pero sí lo anotó el usuario.
     */
    fun resolveContactFromMemory(memory: String, nameQuery: String): String? {
        if (memory.isBlank()) return null
        val q = nameQuery.lowercase()
        val phonePat = Regex("\\+?[\\d\\s\\-]{7,}")
        return memory.lines()
            .firstOrNull { line -> line.lowercase().contains(q) && phonePat.containsMatchIn(line) }
            ?.let { phonePat.find(it)?.value?.trim() }
    }

    // Accesores públicos para respuestas sociales (usados en VM)
    fun greetingResponse()     = GREETING_RESPONSES[(System.currentTimeMillis() % GREETING_RESPONSES.size).toInt()]
    fun farewellResponse()     = FAREWELL_RESPONSES[(System.currentTimeMillis() % FAREWELL_RESPONSES.size).toInt()]
    fun gratitudeResponse()    = GRATITUDE_RESPONSES[(System.currentTimeMillis() % GRATITUDE_RESPONSES.size).toInt()]
    fun affirmationResponse()  = AFFIRMATION_RESPONSES[(System.currentTimeMillis() % AFFIRMATION_RESPONSES.size).toInt()]

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
