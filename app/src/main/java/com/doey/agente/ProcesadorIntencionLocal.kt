package com.doey.agente

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * IRIS v5 — Intent Recognition & Interpretation System
 *
 * PRINCIPIOS DE DISEÑO:
 *  1. Verbos centrales definidos UNA VEZ — todos los matchers los usan.
 *  2. Solo actúa si la instrucción es directa y sin ambigüedad.
 *  3. Frase con contexto narrativo, condicional o explicativo → Delegate.
 *  4. Objeto externo al dispositivo (carro, TV, casa) → Delegate.
 *  5. Solo ejecuta acciones que Android puede realizar directamente.
 *  6. Tolerancia total a variantes fonéticas, voz-a-texto y slang MX.
 *  7. Comandos complejos (Y LUEGO / Y TAMBIÉN / etc.) → Complex(subtasks).
 *  8. Sin duplicar sinónimos — cada verbo o concepto vive en UN solo lugar.
 */
object LocalIntentProcessor {

    // ═════════════════════════════════════════════════════════════════════════
    // VOCABULARIO CENTRAL — se define UNA vez, se reutiliza en todos los matchers
    // ═════════════════════════════════════════════════════════════════════════

    /** Verbos de apertura/lanzamiento */
    private const val V_OPEN  = "abre?|lanza|inicia|entra a|ve a|open|launch|start|dale a|pon|jala|carga"
    /** Verbos de activación */
    private const val V_ON    = "activa|enciende|prende|turn on|enable|mete|conecta|pon|prender|activar|encender|dale"
    /** Verbos de desactivación */
    private const val V_OFF   = "desactiva|apaga|turn off|disable|quita|saca|corta|desconecta|apagar|desactivar"
    /** Verbos de envío de mensajes */
    private const val V_SEND  = "manda|envia|escribe|send|dile|avisa|manda un|envia un|mandame|enviame|escribele"
    /** Verbos de reproducción de música */
    private const val V_PLAY  = "pon|reproduce|play|toca|escucha|quiero escuchar|ponme|dale play a|quiero oir|oye"
    /** Verbos de subir */
    private const val V_UP    = "sube|aumenta|incrementa|mas|raise|up"
    /** Verbos de bajar */
    private const val V_DOWN  = "baja|reduce|disminuye|menos|lower|down"
    /** Verbos de búsqueda */
    private const val V_SEARCH = "busca|encuentra|search|encuentra|halla|muestrame|dime|googlea"
    /** Conector "al" / "en" / "de" */
    private const val P_IN    = "(?:en|al|a|de|en el|en la|dentro de|para)?"

    // Precompilados reutilizables
    private val RX_OPEN   = Regex("\\b($V_OPEN)\\b", RegexOption.IGNORE_CASE)
    private val RX_ON     = Regex("\\b($V_ON)\\b",   RegexOption.IGNORE_CASE)
    private val RX_OFF    = Regex("\\b($V_OFF)\\b",  RegexOption.IGNORE_CASE)
    private val RX_SEND   = Regex("\\b($V_SEND)\\b", RegexOption.IGNORE_CASE)
    private val RX_UP     = Regex("\\b($V_UP)\\b",   RegexOption.IGNORE_CASE)
    private val RX_DOWN   = Regex("\\b($V_DOWN)\\b", RegexOption.IGNORE_CASE)
    private val RX_SEARCH = Regex("\\b($V_SEARCH)\\b", RegexOption.IGNORE_CASE)

    // ═════════════════════════════════════════════════════════════════════════
    // TIPOS DE RESULTADO
    // ═════════════════════════════════════════════════════════════════════════

    sealed class IntentClass {
        data class Local(val action: LocalAction) : IntentClass()
        data class Complex(val subtasks: List<String>) : IntentClass()
        object Delegate : IntentClass()
    }

    sealed class LocalAction {
        // ── Respuestas sociales (cero tokens) ────────────────────────────────
        data class Greeting(val variant: Int)    : LocalAction()
        data class Farewell(val variant: Int)    : LocalAction()
        data class Gratitude(val variant: Int)   : LocalAction()
        data class Affirmation(val variant: Int) : LocalAction()
        // ── Memoria ──────────────────────────────────────────────────────────
        data class QueryMemory(val raw: String)  : LocalAction()
        // ── Dispositivo ──────────────────────────────────────────────────────
        data class ToggleFlashlight(val enable: Boolean)                                 : LocalAction()
        data class SetVolume(val level: Int, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class VolumeStep(val up: Boolean, val stream: VolumeStream = VolumeStream.MEDIA) : LocalAction()
        data class SetSilentMode(val mode: SilentMode)  : LocalAction()
        data class ToggleWifi(val enable: Boolean)      : LocalAction()
        data class ToggleBluetooth(val enable: Boolean) : LocalAction()
        data class ToggleAirplane(val enable: Boolean)  : LocalAction()
        data class ToggleDoNotDisturb(val enable: Boolean) : LocalAction()
        data class ToggleNfc(val enable: Boolean)       : LocalAction()
        data class ToggleDarkMode(val enable: Boolean)  : LocalAction()
        data class ToggleHotspot(val enable: Boolean)   : LocalAction()
        data class SetBrightness(val level: Int)        : LocalAction()
        data class BrightnessStep(val up: Boolean)      : LocalAction()
        data class ToggleAutoBrightness(val enable: Boolean) : LocalAction()
        data class SetRingtoneVolume(val level: Int)    : LocalAction()
        data class SetAlarmVolume(val level: Int)       : LocalAction()
        data class SetScreenTimeout(val seconds: Int)   : LocalAction()
        class TakeScreenshot : LocalAction()
        class LockScreen     : LocalAction()
        class TogglePowerSave : LocalAction()
        // ── Alarmas y tiempo ─────────────────────────────────────────────────
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        data class SetAlarmNative(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class CancelAlarm    : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class CancelTimer    : LocalAction()
        class StartStopwatch : LocalAction()
        class StopStopwatch  : LocalAction()
        // ── Comunicación ─────────────────────────────────────────────────────
        data class Call(val contact: String)              : LocalAction()
        data class CallEmergency(val number: String)      : LocalAction()
        data class SendSms(val contact: String, val message: String) : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String) : LocalAction()
        data class SendTelegram(val contact: String, val message: String) : LocalAction()
        data class OpenWhatsAppChat(val contact: String)  : LocalAction()
        // ── Apps y navegación ─────────────────────────────────────────────────
        data class OpenApp(val query: String)             : LocalAction()
        data class Navigate(val destination: String)      : LocalAction()
        data class SearchWeb(val query: String)           : LocalAction()
        data class SearchMaps(val query: String)          : LocalAction()
        data class OpenUrl(val url: String)               : LocalAction()
        // ── Música ───────────────────────────────────────────────────────────
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        data class SearchAndPlaySpotify(val query: String) : LocalAction()  // abre+busca+selecciona primera
        class PauseMusic   : LocalAction()
        class ResumeMusic  : LocalAction()
        class NextTrack    : LocalAction()
        class PrevTrack    : LocalAction()
        class ShuffleMusic : LocalAction()
        class RepeatToggle : LocalAction()
        // ── Compartir ────────────────────────────────────────────────────────
        data class ShareText(val text: String) : LocalAction()
        // ── Apps rápidas ─────────────────────────────────────────────────────
        class OpenCamera    : LocalAction()
        class OpenGallery   : LocalAction()
        class OpenContacts  : LocalAction()
        class OpenDialer    : LocalAction()
        class OpenCalculator : LocalAction()
        class OpenCalendar  : LocalAction()
        class OpenMaps      : LocalAction()
        class OpenBrowser   : LocalAction()
        class OpenFiles     : LocalAction()
        class OpenClock     : LocalAction()
        // ── Info del sistema ─────────────────────────────────────────────────
        data class QueryInfo(val type: InfoType) : LocalAction()
        // ── Listas de compras ─────────────────────────────────────────────────
        data class AddShoppingItem(val item: String)  : LocalAction()
        class ClearShoppingList : LocalAction()
        // ── Notas rápidas offline ─────────────────────────────────────────────
        data class QuickNote(val content: String)      : LocalAction()
        data class ReadNotes(val tag: String = "")     : LocalAction()
        // ── Calculadora offline ───────────────────────────────────────────────
        data class Calculate(val expression: String)  : LocalAction()
        // ── Conversión de unidades offline ────────────────────────────────────
        data class Convert(val value: Double, val from: String, val to: String) : LocalAction()
        // ── Recordatorio rápido offline ───────────────────────────────────────
        data class QuickReminder(val text: String, val inMinutes: Int) : LocalAction()
        // ── Ajustes ──────────────────────────────────────────────────────────
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
        class OpenNfcSettings : LocalAction()
        class OpenDataUsageSettings : LocalAction()
        class OpenVpnSettings : LocalAction()
        class OpenSyncSettings : LocalAction()
        class OpenInputMethodSettings : LocalAction()
        class OpenCastSettings : LocalAction()
        class OpenOverlaySettings : LocalAction()
        class OpenWriteSettings : LocalAction()
        class OpenUsageAccessSettings : LocalAction()
        class OpenZenModeSettings : LocalAction()
        class OpenPrintSettings : LocalAction()
        class OpenApnSettings : LocalAction()
        class OpenUserDictionarySettings : LocalAction()
        class OpenDreamSettings : LocalAction()
        class OpenCaptioningSettings : LocalAction()
        class OpenSearchSettings : LocalAction()
        // ── Navegación del sistema ───────────────────────────────────────────
        class GoHome             : LocalAction()
        class BackButton         : LocalAction()
        class ShowRecentApps     : LocalAction()
        class ClearNotifications : LocalAction()
    }

    enum class VolumeStream  { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode    { SILENT, VIBRATE, NORMAL }
    enum class InfoType      { TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS,
                               RAM_USAGE, CPU_TEMP, UPTIME, NETWORK_SPEED, IP_ADDRESS }

    // ═════════════════════════════════════════════════════════════════════════
    // GUARDAS — disparan Delegate inmediatamente
    // ═════════════════════════════════════════════════════════════════════════

    private val QUESTION_PREFIXES = Regex(
        "^(como|como se|como puedo|como hago|como funciona|como se usa|como activo|" +
        "que es|que hace|que significa|cual es|cuando|cuanto|cuantos|cuantas|" +
        "por que|para que|sabes|puedes decirme|podrias decirme|dime como|" +
        "explica|explicame|necesito saber|quiero saber|me puedes|es posible|" +
        "hay alguna|existe|what|how|why|when|where|is there|can you|tell me how|" +
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
        "\\b(y luego|y despues|y tambien|y ademas|after that|and then|al mismo tiempo|" +
        "mientras tanto|y enseguida|despues de eso|a continuacion|luego|despues busca|" +
        "y busca|y pon|y abre|y manda|y llama|y reproduce|y escribe)",
        RegexOption.IGNORE_CASE
    )

    // ═════════════════════════════════════════════════════════════════════════
    // NORMALIZACIÓN — quita tildes, signos, wake words, slang voz-a-texto
    // ═════════════════════════════════════════════════════════════════════════

    private fun normalize(text: String): String = text.trim()
        .replace(Regex("[¿¡.,;!?]+"), " ")
        .replace('á','a').replace('é','e').replace('í','i')
        .replace('ó','o').replace('ú','u').replace('ü','u')
        .replace('Á','A').replace('É','E').replace('Í','I')
        .replace('Ó','O').replace('Ú','U')
        // slang y fonética
        .replace(Regex("\\bk\\b"), "que")
        .replace(Regex("\\bq\\b"), "que")
        .replace(Regex("\\bxfa\\b"), "")
        .replace(Regex("\\b(plis|pliss|porfa|por fis)\\b"), "")
        .replace(Regex("\\bguasap\\b"), "whatsapp")
        .replace(Regex("\\bwasa\\b"),   "whatsapp")
        .replace(Regex("\\bwapp\\b"),   "whatsapp")
        .replace(Regex("\\bflas\\b"),   "linterna")
        .replace(Regex("\\bflash\\b"),  "linterna")
        .replace(Regex("\\bspoti\\b"),  "spotify")
        .replace(Regex("\\byt\\b"),     "youtube")
        .replace(Regex("\\btele\\b"),   "telefono")
        .replace(Regex("\\bcel\\b"),    "telefono")
        // wake words
        .replace(Regex("^(oye|hey|eh|ey|iris|doey|asistente|ok|okay|oiga),?\\s+"), "")
        .replace(Regex("\\s+(por favor|porfa|plis|gracias)\\.?$"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    // ═════════════════════════════════════════════════════════════════════════
    // CLASIFICADOR PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    fun classify(input: String): IntentClass {
        val lo = normalize(input).lowercase()

        if (QUESTION_PREFIXES.containsMatchIn(lo)) return IntentClass.Delegate
        if (EXTERNAL_CONTEXT.containsMatchIn(lo))  return IntentClass.Delegate

        if (MULTI_TASK.containsMatchIn(lo)) {
            val parts = splitSubtasks(input)
            if (parts.size >= 2) return IntentClass.Complex(parts)
        }

        tryLocal(lo)?.let { return IntentClass.Local(it) }
        return IntentClass.Delegate
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CADENA DE MATCHING — orden importa (más específico primero)
    // ═════════════════════════════════════════════════════════════════════════

    private fun tryLocal(lo: String): LocalAction? =
        matchGreeting(lo)
        ?: matchFarewell(lo)
        ?: matchGratitude(lo)
        ?: matchAffirmation(lo)
        ?: matchMemoryQuery(lo)
        ?: matchCalculate(lo)        // antes que matchOpenApp — "calcula 5+5" no es "abrir calc"
        ?: matchConvert(lo)          // conversiones de unidades offline
        ?: matchQuickNote(lo)        // notas rápidas offline
        ?: matchQuickReminder(lo)    // recordatorio rápido
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
        ?: matchSearchAndPlaySpotify(lo)   // ANTES que matchMusic — "abre Spotify y busca X"
        ?: matchMusic(lo)
        ?: matchUrl(lo)
        ?: matchSystemQuery(lo)
        ?: matchNavigationActions(lo)
        ?: matchSettingsShortcuts(lo)
        ?: matchDeviceMaintenance(lo)
        ?: matchShopping(lo)
        ?: matchShare(lo)
        ?: matchQuickApps(lo)
        ?: matchOpenApp(lo)              // siempre al final — captura genérica

    // ═════════════════════════════════════════════════════════════════════════
    // ── RESPUESTAS SOCIALES ───────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private val GREETING_REGEX = Regex(
        "^(hola|hey|hi|buenas|buenos dias|buenas tardes|buenas noches|buen dia|que tal|" +
        "como estas|como andas|que onda|que pedo|que paso|que hay|ey|" +
        "que hubo|quiubo|saludos|ola|alo|que hay de nuevo|que me cuentas)$",
        RegexOption.IGNORE_CASE
    )
    private val GREETING_RESPONSES = listOf(
        "¡Hola! 👋 ¿En qué te ayudo?", "¡Hey! ¿Qué necesitas?",
        "¡Buenas! Listo para lo que necesites 😊", "¡Hola! Dime, ¿qué hacemos?",
        "¡Aquí estoy! ¿Qué se te ofrece?", "¡Hey! ¿Qué onda? ¿En qué te echo la mano?",
        "¡Hola hola! Cuéntame. 🤖", "¡Qué tal! ¿Y tú?"
    )
    private fun matchGreeting(lo: String): LocalAction? {
        if (!GREETING_REGEX.containsMatchIn(lo) || lo.split(" ").size > 5) return null
        return LocalAction.Greeting((System.currentTimeMillis() % GREETING_RESPONSES.size).toInt())
    }

    private val FAREWELL_REGEX = Regex(
        "^(adios|hasta luego|nos vemos|chao|chau|bye|hasta pronto|me voy|" +
        "me despido|hasta la proxima|nos vidrios|a dormir|bye bye)$",
        RegexOption.IGNORE_CASE
    )
    private val FAREWELL_RESPONSES = listOf(
        "¡Hasta luego! 👋", "¡Cuídate mucho! 😊", "¡Chao! Vuelve cuando quieras.",
        "¡Hasta pronto!", "¡Que te vaya bien! 👋", "¡Nos vemos!"
    )
    private fun matchFarewell(lo: String): LocalAction? {
        if (!FAREWELL_REGEX.containsMatchIn(lo) || lo.split(" ").size > 6) return null
        return LocalAction.Farewell((System.currentTimeMillis() % FAREWELL_RESPONSES.size).toInt())
    }

    private val GRATITUDE_REGEX = Regex(
        "^(gracias|muchas gracias|grax|grasias|te lo agradezco|muy amable|" +
        "thanks|thank you|de lujo|chido|estuvo bien|" +
        "(ok |sale |genial |perfecto |listo |orale |andale )?gracias)$",
        RegexOption.IGNORE_CASE
    )
    private val GRATITUDE_RESPONSES = listOf(
        "¡De nada! 😊 Para eso estoy.", "¡Con gusto! ¿Algo más?",
        "¡A tus órdenes siempre! 🤖", "No hay de qué. ¿Algo más?",
        "¡Fue un placer! 😄"
    )
    private fun matchGratitude(lo: String): LocalAction? {
        if (!GRATITUDE_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Gratitude((System.currentTimeMillis() % GRATITUDE_RESPONSES.size).toInt())
    }

    private val AFFIRMATION_REGEX = Regex(
        "^(ok|okay|okey|listo|entendido|de acuerdo|sale|andale|orale|" +
        "claro|por supuesto|si|yes|yep|yup|va|dale|adelante|perfecto|excelente|" +
        "esta bien|de una|con todo|ten|ahi esta|eso|correcto)$",
        RegexOption.IGNORE_CASE
    )
    private val AFFIRMATION_RESPONSES = listOf(
        "👍", "¡Perfecto!", "¡Entendido!", "¡Claro!", "¡Listo!"
    )
    private fun matchAffirmation(lo: String): LocalAction? {
        if (!AFFIRMATION_REGEX.containsMatchIn(lo)) return null
        return LocalAction.Affirmation((System.currentTimeMillis() % AFFIRMATION_RESPONSES.size).toInt())
    }

    private fun matchMemoryQuery(lo: String): LocalAction? {
        val trigger = Regex(
            """\b(recuerdas|sabes algo de|te dije|me dijiste|te acuerdas|dijiste|acuerdas|lo que te conte|lo que te dije)\b""",
            RegexOption.IGNORE_CASE
        )
        if (!trigger.containsMatchIn(lo)) return null
        return LocalAction.QueryMemory(lo)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── CALCULADORA OFFLINE ───────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════
    // Resuelve expresiones matemáticas sin IA ni internet.
    // "cuanto es 15% de 230", "calcula 450/3", "cuanto es 2 elevado a 8"

    private fun matchCalculate(lo: String): LocalAction? {
        // Trigger: calcula / cuánto es / cuántos son / resultado de / quanto es
        val trigger = Regex("\\b(calcula|calculo|cuanto es|cuantos son|resultado de|cuanto da|quanto es|cuanto queda|cuanto seria|cuanto serian)\\b", RegexOption.IGNORE_CASE)
        if (!trigger.containsMatchIn(lo)) return null

        // Extraer la expresión matemática
        val expr = trigger.replace(lo, "").trim()
            .replace("por",   "*").replace("x",       "*")
            .replace("entre", "/").replace("dividido","  /")
            .replace("mas",   "+").replace("\\+".toRegex(), "+")
            .replace("menos", "-")
            .replace("al cuadrado", "^2").replace("al cubo","^3")
            .replace(Regex("elevado a (\\d+)"), "^$1")
            .replace(Regex("(\\d+)\\s*%\\s*de\\s*(\\d+)")) { m ->
                "(${m.groupValues[1]}/100)*${m.groupValues[2]}"
            }
            .trim()

        if (expr.isBlank()) return null
        return LocalAction.Calculate(expr)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── CONVERSIÓN DE UNIDADES OFFLINE ───────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════
    // "convierte 100 dolares a pesos", "cuanto son 5 km en millas",
    // "convierte 37 celsius a fahrenheit", "cuanto es 1 libra en kilos"

    private val UNIT_ALIASES = mapOf(
        "km" to "km", "kilometros" to "km", "kilómetros" to "km",
        "mi" to "mi", "millas" to "mi", "milla" to "mi",
        "m" to "m",  "metros" to "m", "metro" to "m",
        "cm" to "cm","centimetros" to "cm","centímetros" to "cm",
        "ft" to "ft","pies" to "ft","pie" to "ft","feet" to "ft",
        "in" to "in","pulgadas" to "in","pulgada" to "in","inches" to "in",
        "kg" to "kg","kilos" to "kg","kilo" to "kg","kilogramos" to "kg",
        "lb" to "lb","libras" to "lb","libra" to "lb","pounds" to "lb",
        "g"  to "g", "gramos" to "g","gramo" to "g",
        "oz" to "oz","onzas" to "oz","onza" to "oz",
        "c"  to "c", "celsius" to "c","centigrados" to "c","centígrados" to "c",
        "f"  to "f", "fahrenheit" to "f",
        "k"  to "k", "kelvin" to "k",
        "usd" to "usd","dolares" to "usd","dólares" to "usd","dolar" to "usd","dollar" to "usd",
        "mxn" to "mxn","pesos" to "mxn","peso mexicano" to "mxn",
        "eur" to "eur","euros" to "eur","euro" to "eur",
        "l"   to "l", "litros" to "l","litro" to "l",
        "ml"  to "ml","mililitros" to "ml",
        "gal" to "gal","galones" to "gal","galon" to "gal","gallon" to "gal"
    )

    private fun matchConvert(lo: String): LocalAction? {
        val rx = Regex("(?:convierte|cuanto son|cuanto es|pasar|convertir|a cuanto equivale|equivale)\\s+([\\d.,]+)\\s+(.+?)\\s+(?:en|a|to)\\s+(.+?)\\s*$", RegexOption.IGNORE_CASE)
        val m  = rx.find(lo) ?: return null
        val value = m.groupValues[1].replace(",",".").toDoubleOrNull() ?: return null
        val from  = UNIT_ALIASES[m.groupValues[2].trim().lowercase()] ?: m.groupValues[2].trim().lowercase()
        val to    = UNIT_ALIASES[m.groupValues[3].trim().lowercase()] ?: m.groupValues[3].trim().lowercase()
        if (from.isBlank() || to.isBlank()) return null
        return LocalAction.Convert(value, from, to)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── NOTAS RÁPIDAS OFFLINE ─────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════
    // "anota: llamar al doctor", "guarda nota: contraseña wifi es 1234",
    // "recuerda que tengo cita el jueves", "qué tengo anotado", "mis notas"

    private fun matchQuickNote(lo: String): LocalAction? {
        // Guardar nota
        Regex("^(?:anota|apunta|guarda|nota|recuerda que|agrega nota|nota rapida|escribe)\\s*[:\\-]?\\s*(.{3,200})$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val content = m.groupValues[1].trim()
            if (content.isNotBlank()) return LocalAction.QuickNote(content)
        }
        // Leer notas
        if (Regex("\\b(que tengo anotado|mis notas|ver notas|leer notas|que anote|notas guardadas|que guarde)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ReadNotes()
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── RECORDATORIO RÁPIDO OFFLINE ───────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════
    // "recuérdame tomar el medicamento en 30 minutos",
    // "avísame en 2 horas que tengo junta"

    private fun matchQuickReminder(lo: String): LocalAction? {
        val rx = Regex(
            "^(?:recuerdame|avisame|notificame|reminder|recuerda|avisa)\\s+(.+?)\\s+" +
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

    // ═════════════════════════════════════════════════════════════════════════
    // ── DISPOSITIVO ───────────────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchFlashlight(lo: String): LocalAction? {
        val flash = "\\b(linterna|flashlight|flash|flas|torch)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*$flash", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("$flash.*\\b(on|encendida|activa)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleFlashlight(true)
            Regex("\\b($V_OFF)\\b.*$flash", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("$flash.*\\b(off|apagada|desactiva)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleFlashlight(false)
            else -> null
        }
    }

    private fun matchVolume(lo: String): LocalAction? {
        val vol = "\\b(volumen|vol|audio|sonido)\\b"
        val ring = "\\b(timbre|ringtone|tono de llamada|tono)\\b"
        val alarm = "\\b(alarma|alarm)\\b"

        // Volumen de timbre
        Regex("\\b($V_UP|$V_DOWN)\\b.*$ring", RegexOption.IGNORE_CASE).find(lo)?.let {
            val up = RX_UP.containsMatchIn(lo)
            val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
            if (pct != null) return LocalAction.SetRingtoneVolume(pct.coerceIn(0,100))
            return LocalAction.VolumeStep(up, VolumeStream.RING)
        }
        // Volumen de alarma
        Regex("\\b($V_UP|$V_DOWN)\\b.*$alarm|$alarm.*\\b(volumen|vol)\\b", RegexOption.IGNORE_CASE).find(lo)?.let {
            val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
            if (pct != null) return LocalAction.SetAlarmVolume(pct.coerceIn(0,100))
            return LocalAction.VolumeStep(RX_UP.containsMatchIn(lo), VolumeStream.ALARM)
        }
        // Silencia completamente
        if (Regex("\\b(silencia|silenciar|mute|mutea|mutear)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.SetSilentMode(SilentMode.SILENT)

        if (!Regex("\\b($V_UP|$V_DOWN|pon|ajusta|sube|baja|volumen)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        if (!Regex(vol, RegexOption.IGNORE_CASE).containsMatchIn(lo) &&
            !Regex("\\b($V_UP|$V_DOWN)\\b.*(musica|media|el sonido|el audio)", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
        if (pct != null) return LocalAction.SetVolume(pct.coerceIn(0,100))
        return LocalAction.VolumeStep(RX_UP.containsMatchIn(lo))
    }

    private fun matchSilent(lo: String): LocalAction? = when {
        Regex("\\b(modo silencio|silencio total|no hacer ruido|silencioso)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.SILENT)
        Regex("\\b(modo vibracion|vibrar|vibrador|vibra|vibration)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.VIBRATE)
        Regex("\\b(sonido normal|quita el silencio|activa el sonido|pon sonido|sonido on|deja sonar)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.SetSilentMode(SilentMode.NORMAL)
        else -> null
    }

    private fun matchBrightness(lo: String): LocalAction? {
        val brillo = "\\b(brillo|brightness|luminosidad|pantalla)\\b"
        if (!Regex(brillo, RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        if (Regex("\\b(automatico|auto|auto brillo|auto-brillo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ToggleAutoBrightness(RX_ON.containsMatchIn(lo))
        val pct = Regex("\\b(\\d{1,3})\\b").find(lo)?.groupValues?.get(1)?.toIntOrNull()
        if (pct != null) return LocalAction.SetBrightness(pct.coerceIn(0,100))
        return LocalAction.BrightnessStep(RX_UP.containsMatchIn(lo))
    }

    private fun matchWifi(lo: String): LocalAction? {
        val wifi = "\\b(wifi|wi-fi|wifi|red inalambrica|wireless)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($wifi)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($wifi).*\\b(on|activo|activalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleWifi(true)
            Regex("\\b($V_OFF)\\b.*($wifi)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($wifi).*\\b(off|apagado|apagalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleWifi(false)
            else -> null
        }
    }

    private fun matchBluetooth(lo: String): LocalAction? {
        val bt = "\\b(bluetooth|bt\\b|b\\.t\\.|blutus|bluetoth|blue tooth|bluetoo?th)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($bt)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($bt).*\\b(on|activo|activalo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleBluetooth(true)
            Regex("\\b($V_OFF)\\b.*($bt)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($bt).*\\b(off|apagado|desactiva)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleBluetooth(false)
            else -> null
        }
    }

    private fun matchAirplane(lo: String): LocalAction? {
        val air = "\\b(modo avion|airplane mode|flight mode|modo vuelo|modo aereo)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($air)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($air).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(true)
            Regex("\\b($V_OFF)\\b.*($air)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($air).*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleAirplane(false)
            else -> null
        }
    }

    private fun matchDoNotDisturb(lo: String): LocalAction? {
        val dnd = "\\b(no molestar|dnd|do not disturb|no interrumpir|no me molestes)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($dnd)", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(true)
            Regex("\\b($V_OFF)\\b.*($dnd)", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDoNotDisturb(false)
            else -> null
        }
    }

    private fun matchNfc(lo: String): LocalAction? = when {
        Regex("\\b($V_ON)\\b.*\\bnfc\\b|\\bnfc\\b.*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleNfc(true)
        Regex("\\b($V_OFF)\\b.*\\bnfc\\b|\\bnfc\\b.*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ToggleNfc(false)
        else -> null
    }

    private fun matchDarkMode(lo: String): LocalAction? {
        val dark  = "\\b(modo oscuro|dark mode|modo noche|tema oscuro|modo dark)\\b"
        val light = "\\b(modo claro|light mode|modo dia|tema claro|modo normal)\\b"
        return when {
            Regex("\\b($V_ON|usa|pon)\\b.*($dark)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($dark).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(true)
            Regex("\\b($V_OFF|quita)\\b.*($dark)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex(light, RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleDarkMode(false)
            else -> null
        }
    }

    private fun matchHotspot(lo: String): LocalAction? {
        val hot = "\\b(hotspot|punto de acceso|compartir datos|tethering|internet movil compartido)\\b"
        return when {
            Regex("\\b($V_ON)\\b.*($hot)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($hot).*\\b(on|activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(true)
            Regex("\\b($V_OFF)\\b.*($hot)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("($hot).*\\b(off|apagado)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
                -> LocalAction.ToggleHotspot(false)
            else -> null
        }
    }

    private fun matchScreenshot(lo: String): LocalAction? {
        return if (Regex("\\b(toma|saca|haz|captura|take|screenshot|printscreen)\\b.*(captura|screenshot|pantalla|screen shot|foto de pantalla)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
                   Regex("^(screenshot|captura de pantalla|foto de pantalla)$", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            LocalAction.TakeScreenshot() else null
    }

    private fun matchLock(lo: String): LocalAction? {
        return if (Regex("^(bloquea(r)?( el telefono| la pantalla| el cel| el movil)?|lock( the)? (screen|phone)|apaga(r)? la pantalla|bloquear cel)$", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            LocalAction.LockScreen() else null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── ALARMAS Y TIEMPO ─────────────────────────────────────────════════════
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchAlarm(lo: String): LocalAction? {
        if (Regex("\\b(cancela|elimina|borra|quita|cancel)\\b.*(alarma|alarm)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.CancelAlarm()
        val alarmTrigger = Regex("\\b(pon|crea|programa|set|ponme|despiertame|despertarme)\\b.*(alarma|alarm|despertar)", RegexOption.IGNORE_CASE)
        if (!alarmTrigger.containsMatchIn(lo) &&
            !Regex("\\b(despiertame|levantame)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        // Relativa: "en 2 horas", "en 45 minutos"
        Regex("\\ben\\s+(\\d+)\\s+(hora|horas|minuto|minutos)\\b", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val value = m.groupValues[1].toInt()
            val unit  = m.groupValues[2].lowercase()
            val cal   = java.util.Calendar.getInstance()
            if (unit.startsWith("hora")) cal.add(java.util.Calendar.HOUR_OF_DAY, value)
            else cal.add(java.util.Calendar.MINUTE, value)
            return LocalAction.SetAlarm(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }

        // Absoluta: "a las 7", "a las 6:30", "a las 10 am"
        Regex("\\ba las\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m|p\\.m)?", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            var h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase().replace(".", "")
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            val label = Regex("(?:llamada|nota|label|etiqueta|para|con titulo)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.groupValues?.get(1)?.trim() ?: ""
            return LocalAction.SetAlarm(h, min, label)
        }
        // Sin "a las": "pon alarma 7" o "despiértame 6:30"
        Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            var h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase()
            if (ampm == "pm" && h < 12) h += 12
            if (h in 0..23) return LocalAction.SetAlarm(h, min)
        }
        return null
    }

    private fun matchTimer(lo: String): LocalAction? {
        if (Regex("\\b(cancela|para|detén|quita)\\b.*(timer|temporizador|cuenta regresiva)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.CancelTimer()
        val trigger = Regex("\\b(timer|temporizador|cuenta regresiva|pon|crea|inicia|ponme)\\b.*(timer|temporizador|minuto|minutos|segundo|segundos|hora|horas)|\\bde\\s+\\d+\\s+(minutos|segundos|horas)\\b", RegexOption.IGNORE_CASE)
        if (!trigger.containsMatchIn(lo) &&
            !Regex("^(timer|temporizador)\\s+(de\\s+)?\\d+", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null

        var totalSeconds = 0L
        Regex("(\\d+)\\s*(hora|horas|h\\b)",  RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() * 3600 }
        Regex("(\\d+)\\s*(minuto|minutos|min\\b|m\\b)", RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() * 60 }
        Regex("(\\d+)\\s*(segundo|segundos|seg\\b|s\\b)", RegexOption.IGNORE_CASE).find(lo)?.let { totalSeconds += it.groupValues[1].toLong() }

        if (totalSeconds <= 0) return null
        val label = Regex("(?:llamado|label|para|con titulo)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.groupValues?.get(1)?.trim() ?: ""
        return LocalAction.SetTimer(totalSeconds, label)
    }

    private fun matchStopwatch(lo: String): LocalAction? = when {
        Regex("\\b(inicia|empieza|arranca|start)\\b.*(cronometro|stopwatch)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
        Regex("^(cronometro|stopwatch)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.StartStopwatch()
        Regex("\\b(detén|para|stop|pausa)\\b.*(cronometro|stopwatch)", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.StopStopwatch()
        else -> null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── COMUNICACIÓN ──────────────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchEmergencyCall(lo: String): LocalAction? {
        val em = Regex("\\b(911|112|066|080|060|emergencia|ambulancia|policia|bomberos|urgencias)\\b", RegexOption.IGNORE_CASE)
        return if (Regex("\\b($V_ON|llama|call|marca|llama al|marca al)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) && em.containsMatchIn(lo))
            LocalAction.CallEmergency(em.find(lo)!!.value) else null
    }

    private fun matchCall(lo: String): LocalAction? {
        val m = Regex("^(?:llama(r)?( a| le a)?|marca(r)?( a| le a)?|call|llamar a)\\s+(?:a\\s+)?(.+?)\\s*$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        val contact = (m.groupValues[5].ifBlank { m.groupValues[6] }).trim()
        if (contact.isBlank() || contact.length > 40) return null
        return LocalAction.Call(contact)
    }

    private fun matchWhatsApp(lo: String): LocalAction? {
        val isWA = Regex("\\b(whatsapp|whats|wapp|wasa|wa\\b|w\\.a\\.)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
        if (!isWA) return null

        // Patrón 1: manda/envía [un] [mensaje/WA] a CONTACTO diciendo MENSAJE
        Regex("^(?:$V_SEND)\\s+(?:un\\s+)?(?:mensaje|whatsapp|wa|wapp|wasa)?\\s*(?:a\\s+)?(.+?)\\s+(?:por\\s+(?:whatsapp|wa)\\s+)?(?:diciendo|que diga|que dice|:|con el texto|el mensaje|el texto|dile que)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c   = m.groupValues[1].trim().removePrefix("a ").trim()
            val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank() && c.length < 40) return LocalAction.SendWhatsApp(c, msg)
        }
        // Patrón 2: manda por WA a CONTACTO: MENSAJE
        Regex("^(?:$V_SEND)\\s+(?:por\\s+)?(?:whatsapp|wa\\b|wapp)\\s+(?:a\\s+)?(.+?)[: ]+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }
        // Patrón 3: wa a CONTACTO dile MENSAJE
        Regex("^(?:wa|wapp)\\s+a\\s+(.+?)\\s+(?:diciendo|dile|que diga|:|el mensaje)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendWhatsApp(c, msg)
        }
        // Solo abrir chat
        Regex("^(?:$V_OPEN|chatea con|chatear con|habla con|hablar con)\\s+(?:(?:whatsapp|wa)\\s+(?:con|de)\\s+|chat de (?:whatsapp|wa)\\s+con\\s+)?(.+?)(?:\\s+(?:en|por)\\s+(?:whatsapp|wa))?$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c = m.groupValues[1].trim().removePrefix("en whatsapp").removePrefix("por whatsapp").removePrefix("en wa").trim()
            if (c.isNotBlank() && !c.contains("whatsapp", ignoreCase = true) && c.length < 40)
                return LocalAction.OpenWhatsAppChat(c)
        }
        return null
    }

    private fun matchTelegram(lo: String): LocalAction? {
        if (!lo.contains("telegram")) return null
        Regex("^(?:$V_SEND)\\s+(?:un\\s+)?(?:mensaje|telegram)?\\s*(?:a\\s+)?(.+?)\\s+(?:por telegram\\s+)?(?:diciendo|que diga|:|el mensaje)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendTelegram(c, msg)
        }
        return null
    }

    private fun matchSms(lo: String): LocalAction? {
        if (Regex("\\b(whatsapp|telegram|wapp|guasap)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        Regex("^(?:$V_SEND)\\s+(?:un\\s+)?(?:sms|mensaje|texto|mensajito)\\s+(?:a\\s+)?(.+?)\\s+(?:diciendo|que diga|:|con el texto|el texto)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val c = m.groupValues[1].trim(); val msg = m.groupValues[2].trim()
            if (c.isNotBlank() && msg.isNotBlank()) return LocalAction.SendSms(c, msg)
        }
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── NAVEGACIÓN / MAPAS / WEB ──────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchNavigation(lo: String): LocalAction? {
        val m = Regex("^(?:llevame a|navega a|como llego a|como voy a|navigate to|directions? to|lleva a|ir a)\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        val dest = m.groupValues[1].trim()
        if (dest.length > 80) return null
        return LocalAction.Navigate(dest)
    }

    private fun matchWebSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca en google|googlea|busca en internet|search|busca en la web|busca online)\\s+(?:sobre\\s+|acerca de\\s+)?(.+)$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        return LocalAction.SearchWeb(m.groupValues[1].trim())
    }

    private fun matchMapsSearch(lo: String): LocalAction? {
        val m = Regex("^(?:busca en maps|busca en google maps|donde esta|donde hay|busca|find)\\s+(.+?)\\s+(?:cerca|en el mapa|en maps|near me|nearby)?$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        val q = m.groupValues[1].trim()
        if (q.length < 3 || q.length > 80) return null
        return LocalAction.SearchMaps(q)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── MÚSICA ───────────────────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * BUG ORIGINAL:
     * "abre Spotify y busca música de Carin, pon la primera que aparezca"
     * IRIS detectaba "y" → MULTI_TASK → splitSubtasks →
     *   parte 2: "pon la primera que aparezca" → matchShopping → AddShoppingItem("la primera que aparezca") ← ERROR
     *
     * FIX: Detectar el patrón completo ANTES de que llegue al splitter.
     * "abre [APP] y busca [QUERY] [pon/selecciona/escoge] [la primera|el primero|el que salga]"
     * → SearchAndPlaySpotify(query)
     */
    private fun matchSearchAndPlaySpotify(lo: String): LocalAction? {
        // Patrón: "abre spotify y busca X pon la primera / el primero / el que salga"
        val rx = Regex(
            "^(?:$V_OPEN)\\s+(spotify|musica|apple music|deezer|soundcloud|youtube music)\\s+" +
            "(?:y\\s+)?(?:busca|search|encuentra|pon)\\s+(?:musica\\s+de|canciones\\s+de|artista\\s+)?(.+?)\\s*" +
            "(?:,|y)?\\s*(?:pon|selecciona|escoge|dale click a|reproduce|abre)\\s+" +
            "(?:la primera|el primero|el primer resultado|la primera opcion|el que salga|la que salga|la que aparezca|el que aparezca|el primero que aparezca|la primera que aparezca)\\s*$",
            RegexOption.IGNORE_CASE
        )
        rx.find(lo)?.let { m ->
            val query = m.groupValues[2].trim()
            if (query.isNotBlank()) return LocalAction.SearchAndPlaySpotify(query)
        }
        // Variante más corta: "busca X en spotify y pon la primera"
        val rx2 = Regex(
            "^(?:$V_SEARCH)\\s+(?:musica\\s+de|canciones\\s+de|artista\\s+)?(.+?)\\s+(?:en\\s+)?(spotify|apple music|deezer)\\s+" +
            "(?:y\\s+)?(?:pon|selecciona|dale play a|reproduce)\\s+" +
            "(?:la primera|el primero|el primer resultado|la primera opcion|el que salga|la que aparezca)\\s*$",
            RegexOption.IGNORE_CASE
        )
        rx2.find(lo)?.let { m ->
            val query = m.groupValues[1].trim()
            if (query.isNotBlank()) return LocalAction.SearchAndPlaySpotify(query)
        }
        return null
    }

    private fun matchMediaControl(lo: String): LocalAction? = when {
        Regex("^(pausa(r)?|pause|deten la musica|para la musica|stop music|pausalo|pausala)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.PauseMusic()
        Regex("^(resume|reanuda(r)?|continua(r)? musica|sigue la musica|play again|dale play|dale|sigue|unpause)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ResumeMusic()
        Regex("^(siguiente|proxima cancion|next|salta(r)?|skip|la que sigue|cambia la cancion)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.NextTrack()
        Regex("^(anterior|cancion anterior|prev(ia)?|back|la de antes|regresar cancion)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.PrevTrack()
        Regex("^(aleatoria|shuffle|modo aleatorio|random|mezclar|mezcla)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ShuffleMusic()
        Regex("^(repite|repeat|repetir|loop|en loop)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.RepeatToggle()
        else -> null
    }

    private fun matchMusic(lo: String): LocalAction? {
        if (!Regex("\\b($V_PLAY|busca)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        val blacklist = setOf("volumen","brillo","wifi","bluetooth","alarma","linterna","silencio","modo","pantalla","lista","compras","whatsapp","telegram","nota","recuerda")

        // "pon/ver video de X en youtube"
        Regex("^(?:$V_PLAY|ver|mira|busca)\\s+(?:un\\s+)?(?:video|videos?)\\s+(?:de\\s+)?['\"]?(.+?)['\"]?\\s+en\\s+(youtube|yt|youtube music|yt music)\\s*$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            if (m.groupValues[1].isNotBlank()) return LocalAction.PlayMusic(m.groupValues[1].trim(), "youtube")
        }
        // "pon X en PLATAFORMA"
        Regex("^(?:$V_PLAY)\\s+['\"]?(.+?)['\"]?\\s+en\\s+(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)\\s*$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val q = m.groupValues[1].trim(); val app = resolveApp(m.groupValues[2])
            if (q.isNotBlank() && blacklist.none { q.contains(it) }) return LocalAction.PlayMusic(q, app)
        }
        // "abre/pon música en spotify" (sin canción)
        Regex("^(?:$V_OPEN|$V_PLAY)\\s+(?:musica\\s+)?(?:en\\s+)?(spotify|youtube music|yt music|youtube|yt|apple music|deezer|soundcloud)\\s*$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            return LocalAction.PlayMusic("", resolveApp(m.groupValues[1]))
        }
        // "pon/toca CANCIÓN" sin plataforma → Spotify
        Regex("^(?:$V_PLAY)\\s+(?:la cancion\\s+|la musica\\s+|el tema\\s+|la rola\\s+)?['\"]?(.{2,60}?)['\"]?\\s*$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val q = m.groupValues[1].trim()
            if (q.isNotBlank() && blacklist.none { q.contains(it) }) return LocalAction.PlayMusic(q, "spotify")
        }
        return null
    }

    private fun resolveApp(raw: String): String = when {
        raw == "youtube" || raw == "yt" -> "youtube"
        raw.contains("youtube music", ignoreCase = true) || raw.contains("yt music", ignoreCase = true) -> "youtube music"
        raw.contains("apple", ignoreCase = true)  -> "apple music"
        raw.contains("deezer", ignoreCase = true) -> "deezer"
        raw.contains("soundcloud", ignoreCase = true) -> "soundcloud"
        else -> "spotify"
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── URL, SISTEMA, NAVEGACIÓN UI, AJUSTES, MANTENIMIENTO ──────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchUrl(lo: String): LocalAction? {
        val m = Regex("^(?:$V_OPEN|ve a|go to|entra a)\\s+(https?://\\S+|www\\.\\S+)$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        var url = m.groupValues[1].trim()
        if (!url.startsWith("http")) url = "https://$url"
        return LocalAction.OpenUrl(url)
    }

    private fun matchSystemQuery(lo: String): LocalAction? = when {
        Regex("\\b(que hora es|dime la hora|hora actual|que hora son|la hora)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.TIME)
        Regex("\\b(que dia es|que fecha es|fecha actual|que dia estamos|la fecha)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.DATE)
        Regex("\\b(cuanta bateria|nivel de bateria|battery level|como esta la bateria|porcentaje de bateria|la bateria)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BATTERY)
        Regex("\\b(cuanto espacio|almacenamiento disponible|espacio libre|cuanto almacenamiento|cuanta memoria)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.STORAGE)
        Regex("\\b(esta (el )?wifi (activo|conectado|on)|tengo wifi|el wifi)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.WIFI_STATUS)
        Regex("\\b(esta (el )?bluetooth (activo|on)|tengo bluetooth activo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.BT_STATUS)
        Regex("\\b(cuanta ram|uso de ram|memoria ram|ram libre|la ram)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.RAM_USAGE)
        Regex("\\b(temperatura|esta caliente|temp del cpu|calentamiento)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.CPU_TEMP)
        Regex("\\b(tiempo encendido|uptime|cuanto lleva prendido)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.UPTIME)
        Regex("\\b(velocidad de internet|velocidad de red|que tan rapido|el internet)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.NETWORK_SPEED)
        Regex("\\b(mi ip|ip del celular|cual es mi ip|ip local)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.QueryInfo(InfoType.IP_ADDRESS)
        else -> null
    }

    private fun matchNavigationActions(lo: String): LocalAction? = when {
        Regex("\\b(ve a inicio|pantalla principal|go home|vuelve a inicio|inicio|home)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) &&
        !lo.contains("ajuste") && !lo.contains("aplicaci")
            -> LocalAction.GoHome()
        Regex("\\b(atras|regresa|vuelve|go back|pa atras|regresar)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo) && lo.length < 20
            -> LocalAction.BackButton()
        Regex("\\b(apps recientes|multitarea|cambiar de app|recent apps|apps abiertas)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.ShowRecentApps()
        else -> null
    }

    private fun matchSettingsShortcuts(lo: String): LocalAction? {
        if (!Regex("\\b(ajustes|configuracion|settings|configurar|preferencias)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo)) return null
        return when {
            lo.contains("wifi") || lo.contains("red") -> LocalAction.OpenWifiSettings()
            lo.contains("bluetooth") || lo.contains("bt") -> LocalAction.OpenBluetoothSettings()
            lo.contains("bateria") || lo.contains("pila") -> LocalAction.OpenBatterySettings()
            lo.contains("pantalla") || lo.contains("brillo") -> LocalAction.OpenDisplaySettings()
            lo.contains("sonido") || lo.contains("volumen") || lo.contains("audio") -> LocalAction.OpenSoundSettings()
            lo.contains("almacenamiento") || lo.contains("espacio") -> LocalAction.OpenStorageSettings()
            lo.contains("ubicacion") || lo.contains("gps") -> LocalAction.OpenLocationSettings()
            lo.contains("seguridad") || lo.contains("huella") || lo.contains("pin") -> LocalAction.OpenSecuritySettings()
            lo.contains("aplicaciones") || lo.contains("apps") -> LocalAction.OpenAppsSettings()
            lo.contains("fecha") || lo.contains("hora") -> LocalAction.OpenDateSettings()
            lo.contains("idioma") || lo.contains("lenguaje") -> LocalAction.OpenLanguageSettings()
            lo.contains("accesibilidad") -> LocalAction.OpenAccessibilitySettings()
            lo.contains("desarrollador") -> LocalAction.OpenDeveloperSettings()
            lo.contains("notificaciones") -> LocalAction.OpenNotificationSettings()
            lo.contains("privacidad") -> LocalAction.OpenPrivacySettings()
            lo.contains("cuenta") -> LocalAction.OpenAccountSettings()
            lo.contains("nfc") -> LocalAction.OpenNfcSettings()
            lo.contains("datos") -> LocalAction.OpenDataUsageSettings()
            lo.contains("vpn") -> LocalAction.OpenVpnSettings()
            lo.contains("sincronizacion") || lo.contains("sync") -> LocalAction.OpenSyncSettings()
            lo.contains("teclado") -> LocalAction.OpenInputMethodSettings()
            else -> LocalAction.OpenSettings()
        }
    }

    private fun matchDeviceMaintenance(lo: String): LocalAction? {
        if (Regex("\\b(limpia|borra|quita|clear|elimina)\\b.*(notificaciones|avisos)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ClearNotifications()
        if (Regex("\\b(ahorro de energia|modo ahorro|power save|ahorro de bateria|bajo consumo)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.TogglePowerSave()
        Regex("\\b(pantalla se apague en|screen timeout|timeout pantalla)\\b.*(\\d+)\\s*(segundos|minutos)?", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val value = m.groupValues[2].toIntOrNull() ?: return@let
            val secs  = if (m.groupValues[3].contains("minuto")) value * 60 else value
            return LocalAction.SetScreenTimeout(secs)
        }
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── LISTA DE COMPRAS OFFLINE ──────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchShopping(lo: String): LocalAction? {
        if (Regex("\\b(limpia|borra|vacía|vacia|borrar|limpiar)\\b.*(lista|compras)", RegexOption.IGNORE_CASE).containsMatchIn(lo) ||
            Regex("\\b(lista|compras)\\b.*(limpia|borrar|vaciar)", RegexOption.IGNORE_CASE).containsMatchIn(lo))
            return LocalAction.ClearShoppingList()

        // CRÍTICO: solo captura si hay mención explícita de "lista" o "compras"
        // Antes capturaba "la primera que aparezca" porque el regex era muy amplio
        val addRx = Regex(
            "\\b(agrega|agregame|añade|añademe|pon|ponme|añadir|agregar|incluye|apunta|mete)\\b\\s+(.+?)\\s*" +
            "(?:\\b(?:a la|en la|en mi|a mi)\\b.*(lista|compras))",
            RegexOption.IGNORE_CASE
        )
        addRx.find(lo)?.let { m ->
            val raw = m.groupValues[2].trim()
            if (raw.isNotBlank() && raw.length < 60) return LocalAction.AddShoppingItem(raw)
        }
        // Patrón inverso: "a la lista de compras agrega X"
        Regex("\\b(?:a la lista|lista de compras|en la lista)\\b.*?\\b(agrega|añade|pon)\\b\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            val raw = m.groupValues[2].trim()
            if (raw.isNotBlank() && raw.length < 60) return LocalAction.AddShoppingItem(raw)
        }
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── COMPARTIR ─────────────────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchShare(lo: String): LocalAction? {
        Regex("^(?:comparte|compartir|share)\\s+(?:esto|esta publicacion|este post|esta foto|lo que estoy viendo|el link|el enlace)(?:\\s+con\\s+(.+))?$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            return LocalAction.ShareText(m.groupValues[1].trim())
        }
        Regex("^(?:comparte|share)\\s+con\\s+(.+)$", RegexOption.IGNORE_CASE).find(lo)?.let { m ->
            return LocalAction.ShareText(m.groupValues[1].trim())
        }
        return null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── APPS RÁPIDAS ─────────────────────────────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchQuickApps(lo: String): LocalAction? = when {
        Regex("^(?:$V_OPEN)\\s+(la )?camara$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenCamera()
        Regex("^(?:$V_OPEN)\\s+(la )?galeria$|^(mis fotos|ver fotos)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
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
        Regex("^(?:$V_OPEN)\\s+(el )?(navegador|browser|chrome|firefox|edge)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenBrowser()
        Regex("^(?:$V_OPEN)\\s+(el )?(explorador de archivos|mis archivos|archivos|files)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenFiles()
        Regex("^(?:$V_OPEN)\\s+(el )?(reloj|clock)$", RegexOption.IGNORE_CASE).containsMatchIn(lo)
            -> LocalAction.OpenClock()
        else -> null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ── ABRIR APP GENÉRICO — siempre al final ────────────────────────────────
    // ═════════════════════════════════════════════════════════════════════════

    private fun matchOpenApp(lo: String): LocalAction? {
        val m = Regex("^(?:$V_OPEN)\\s+(?:la\\s+app\\s+(?:de\\s+)?|la\\s+aplicacion\\s+(?:de\\s+)?|el\\s+)?(.{2,30})$", RegexOption.IGNORE_CASE).find(lo) ?: return null
        val appName = m.groupValues[1].trim()
        val geoWords = listOf("calle","avenida","colonia","ciudad","pais","cerca","pueblo","estado","nota","lista","primera","primera")
        if (geoWords.any { appName.contains(it) }) return null
        if (appName.split(" ").size > 4) return null
        return LocalAction.OpenApp(appName)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SPLITTER DE SUB-TAREAS
    // ═════════════════════════════════════════════════════════════════════════

    private fun splitSubtasks(text: String): List<String> {
        var parts = listOf(text)
        listOf(
            Regex("\\s+y luego\\s+",       RegexOption.IGNORE_CASE),
            Regex("\\s+y despues\\s+",     RegexOption.IGNORE_CASE),
            Regex("\\s+y tambien\\s+",     RegexOption.IGNORE_CASE),
            Regex("\\s+ademas\\s+",        RegexOption.IGNORE_CASE),
            Regex("\\s+and then\\s+",      RegexOption.IGNORE_CASE),
            Regex("\\s+y enseguida\\s+",   RegexOption.IGNORE_CASE),
            Regex("\\s+despues de eso\\s+",RegexOption.IGNORE_CASE)
        ).forEach { r ->
            parts = parts.flatMap { r.split(it).map(String::trim).filter { p -> p.length > 5 } }
        }
        return parts
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESOLUCIÓN DE CONTACTOS
    // ═════════════════════════════════════════════════════════════════════════

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

    fun resolveContactFromMemory(memory: String, nameQuery: String): String? {
        if (memory.isBlank()) return null
        val q = nameQuery.lowercase()
        val phonePat = Regex("\\+?[\\d\\s\\-]{7,}")
        return memory.lines()
            .firstOrNull { line -> line.lowercase().contains(q) && phonePat.containsMatchIn(line) }
            ?.let { phonePat.find(it)?.value?.trim() }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ACCESORES PÚBLICOS — respuestas sociales y prompt optimizado
    // ═════════════════════════════════════════════════════════════════════════

    fun greetingResponse()    = GREETING_RESPONSES[(System.currentTimeMillis() % GREETING_RESPONSES.size).toInt()]
    fun farewellResponse()    = FAREWELL_RESPONSES[(System.currentTimeMillis() % FAREWELL_RESPONSES.size).toInt()]
    fun gratitudeResponse()   = GRATITUDE_RESPONSES[(System.currentTimeMillis() % GRATITUDE_RESPONSES.size).toInt()]
    fun affirmationResponse() = AFFIRMATION_RESPONSES[(System.currentTimeMillis() % AFFIRMATION_RESPONSES.size).toInt()]

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String {
        if (subtasks.size <= 1) return originalInput
        val sb = StringBuilder("SECUENCIA:\n")
        subtasks.forEachIndexed { i, t -> sb.append("${i + 1}.$t\n") }
        sb.append("REGLA:ejecuta herramienta por herramienta.Sin texto entre pasos.")
        return sb.toString()
    }
}
