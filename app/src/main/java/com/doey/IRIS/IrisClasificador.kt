package com.doey.IRIS

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║  IrisAcciones.kt  —  ¿QUÉ PUEDE HACER IRIS?                                ║
// ║                                                                              ║
// ║  ESTE ES EL ARCHIVO QUE EDITAS CUANDO QUIERES AÑADIR UNA FUNCIÓN NUEVA.    ║
// ║                                                                              ║
// ║  Pasos para añadir un comando nuevo (ejemplo: "modo teatro"):               ║
// ║                                                                              ║
// ║   1. Añade la acción en LocalAction          (sección 1 abajo)              ║
// ║   2. Registra la función match*() en CADENA  (sección 2 abajo)              ║
// ║   3. Escribe la función match*() en           IrisMotor.kt                  ║
// ║                                                                              ║
// ║  ¡Eso es todo! No hay que tocar nada más.                                   ║
// ╚══════════════════════════════════════════════════════════════════════════════╝

import android.content.Context
import android.provider.ContactsContract
import com.doey.IRIS.IrisDiccionario.SLANG_NORMALIZATIONS
import com.doey.IRIS.IrisDiccionario.WAKE_WORDS_PREFIX
import com.doey.IRIS.IrisDiccionario.COURTESY_SUFFIX

object IrisClasificador {

    // ══════════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: CATÁLOGO DE ACCIONES
    // ══════════════════════════════════════════════════════════════════════════
    //
    //  Aquí vive CADA cosa que IRIS sabe hacer.
    //  Cada línea = un comando = una acción.
    //
    //  ¿Cómo leerlo?
    //    class  NombreAccion              → comando sin parámetros (ej: TakeScreenshot)
    //    data class NombreAccion(val x)   → comando con dato (ej: SetVolume necesita el nivel)
    //
    //  ┌─────────────────────────────────────────────────────────────────────┐
    //  │  Para añadir una acción nueva:                                       │
    //  │  1. Añade una línea aquí en la categoría que corresponda.            │
    //  │  2. Ve a IrisMotor.kt y escribe la función que la detecta.           │
    //  │  3. Registra esa función en la CADENA DE MATCHING (Sección 2).       │
    //  └─────────────────────────────────────────────────────────────────────┘

    sealed class LocalAction {

        // ── 🗣️  RESPUESTAS SOCIALES ──────────────────────────────────────────
        // "hola", "gracias", "adiós", "ok"
        data class Greeting(val variant: Int)    : LocalAction()
        data class Farewell(val variant: Int)    : LocalAction()
        data class Gratitude(val variant: Int)   : LocalAction()
        data class Affirmation(val variant: Int) : LocalAction()

        // ── 🧠  MEMORIA ───────────────────────────────────────────────────────
        // "¿recuerdas lo que te dije?"
        data class QueryMemory(val raw: String)  : LocalAction()

        // ── 📱  HARDWARE DEL DISPOSITIVO ─────────────────────────────────────
        // Linterna, volumen, brillo, modos de conectividad
        data class ToggleFlashlight(val enable: Boolean)                                         : LocalAction()
        data class SetVolume(val level: Int, val stream: VolumeStream = VolumeStream.MEDIA)       : LocalAction()
        data class VolumeStep(val up: Boolean, val stream: VolumeStream = VolumeStream.MEDIA)     : LocalAction()
        data class SetSilentMode(val mode: SilentMode)     : LocalAction()
        data class ToggleWifi(val enable: Boolean)         : LocalAction()
        data class ToggleBluetooth(val enable: Boolean)    : LocalAction()
        data class ToggleAirplane(val enable: Boolean)     : LocalAction()
        data class ToggleDoNotDisturb(val enable: Boolean) : LocalAction()
        data class ToggleNfc(val enable: Boolean)          : LocalAction()
        data class ToggleDarkMode(val enable: Boolean)     : LocalAction()
        data class ToggleHotspot(val enable: Boolean)      : LocalAction()
        data class SetBrightness(val level: Int)           : LocalAction()
        data class BrightnessStep(val up: Boolean)         : LocalAction()
        data class ToggleAutoBrightness(val enable: Boolean) : LocalAction()
        data class SetRingtoneVolume(val level: Int)       : LocalAction()
        data class SetAlarmVolume(val level: Int)          : LocalAction()
        data class SetScreenTimeout(val seconds: Int)      : LocalAction()
        class  TakeScreenshot  : LocalAction()
        class  LockScreen      : LocalAction()
        class  TogglePowerSave : LocalAction()

        // ── ⏰  ALARMAS, TIMERS Y TIEMPO ─────────────────────────────────────
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        data class SetAlarmNative(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class  CancelAlarm    : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class  CancelTimer    : LocalAction()
        class  StartStopwatch : LocalAction()
        class  StopStopwatch  : LocalAction()

        // ── 📞  COMUNICACIÓN ──────────────────────────────────────────────────
        data class Call(val contact: String)              : LocalAction()
        data class CallEmergency(val number: String)      : LocalAction()
        data class SendSms(val contact: String, val message: String)      : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String) : LocalAction()
        data class SendTelegram(val contact: String, val message: String) : LocalAction()
        data class OpenWhatsAppChat(val contact: String)  : LocalAction()

        // ── 🌐  NAVEGACIÓN Y WEB ─────────────────────────────────────────────
        data class OpenApp(val query: String)          : LocalAction()
        data class Navigate(val destination: String)   : LocalAction()
        data class SearchWeb(val query: String)        : LocalAction()
        data class SearchMaps(val query: String)       : LocalAction()
        data class OpenUrl(val url: String)            : LocalAction()

        // ── 🎵  MÚSICA ────────────────────────────────────────────────────────
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        data class SearchAndPlaySpotify(val query: String) : LocalAction()
        class  PauseMusic   : LocalAction()
        class  ResumeMusic  : LocalAction()
        class  NextTrack    : LocalAction()
        class  PrevTrack    : LocalAction()
        class  ShuffleMusic : LocalAction()
        class  RepeatToggle : LocalAction()

        // ── 📤  COMPARTIR ─────────────────────────────────────────────────────
        data class ShareText(val text: String) : LocalAction()

        // ── 📷  APPS RÁPIDAS ─────────────────────────────────────────────────
        class OpenCamera     : LocalAction()
        class OpenGallery    : LocalAction()
        class OpenContacts   : LocalAction()
        class OpenDialer     : LocalAction()
        class OpenCalculator : LocalAction()
        class OpenCalendar   : LocalAction()
        class OpenMaps       : LocalAction()
        class OpenBrowser    : LocalAction()
        class OpenFiles      : LocalAction()
        class OpenClock      : LocalAction()

        // ── 🔍  CONSULTAS DEL SISTEMA ────────────────────────────────────────
        data class QueryInfo(val type: InfoType) : LocalAction()

        // ── 🛒  LISTA DE COMPRAS ─────────────────────────────────────────────
        data class AddShoppingItem(val item: String) : LocalAction()
        class  ClearShoppingList : LocalAction()

        // ── 📝  NOTAS RÁPIDAS OFFLINE ─────────────────────────────────────────
        data class QuickNote(val content: String)  : LocalAction()
        data class ReadNotes(val tag: String = "") : LocalAction()

        // ── 🧮  CALCULADORA OFFLINE ───────────────────────────────────────────
        data class Calculate(val expression: String) : LocalAction()

        // ── ⚖️  CONVERSIÓN DE UNIDADES ───────────────────────────────────────
        data class Convert(val value: Double, val from: String, val to: String) : LocalAction()

        // ── ⏱️  RECORDATORIO RÁPIDO ───────────────────────────────────────────
        data class QuickReminder(val text: String, val inMinutes: Int) : LocalAction()

        // ── ⚙️  AJUSTES DEL SISTEMA ──────────────────────────────────────────
        class OpenSettings              : LocalAction()
        class OpenBatterySettings       : LocalAction()
        class OpenWifiSettings          : LocalAction()
        class OpenBluetoothSettings     : LocalAction()
        class OpenDisplaySettings       : LocalAction()
        class OpenSoundSettings         : LocalAction()
        class OpenStorageSettings       : LocalAction()
        class OpenAccessibilitySettings : LocalAction()
        class OpenDeveloperSettings     : LocalAction()
        class OpenLocationSettings      : LocalAction()
        class OpenSecuritySettings      : LocalAction()
        class OpenAppsSettings          : LocalAction()
        class OpenDateSettings          : LocalAction()
        class OpenLanguageSettings      : LocalAction()
        class OpenAccountSettings       : LocalAction()
        class OpenPrivacySettings       : LocalAction()
        class OpenNotificationSettings  : LocalAction()
        class OpenNfcSettings           : LocalAction()
        class OpenDataUsageSettings     : LocalAction()
        class OpenVpnSettings           : LocalAction()
        class OpenSyncSettings          : LocalAction()
        class OpenInputMethodSettings   : LocalAction()
        class OpenCastSettings          : LocalAction()
        class OpenOverlaySettings       : LocalAction()
        class OpenWriteSettings         : LocalAction()
        class OpenUsageAccessSettings   : LocalAction()
        class OpenZenModeSettings       : LocalAction()
        class OpenPrintSettings         : LocalAction()
        class OpenApnSettings           : LocalAction()
        class OpenUserDictionarySettings : LocalAction()
        class OpenDreamSettings         : LocalAction()
        class OpenCaptioningSettings    : LocalAction()
        class OpenSearchSettings        : LocalAction()

        // ── 🏠  NAVEGACIÓN DEL SISTEMA ───────────────────────────────────────
        class GoHome             : LocalAction()
        class BackButton         : LocalAction()
        class ShowRecentApps     : LocalAction()
        class ClearNotifications : LocalAction()

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        //  👇 AÑADE TUS ACCIONES NUEVAS AQUÍ ABAJO
        //
        //  Ejemplo:
        //    // ── 🎬  MODO TEATRO ────────────────────────────────────────────
        //    class  ActivarModoTeatro : LocalAction()
        //
        //  Luego ve a IrisMotor.kt y añade la función matchModoTeatro()
        //  y regístrala en la CADENA DE MATCHING (Sección 2 aquí abajo).
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    }

    // ── Enumeraciones de apoyo ───────────────────────────────────────────────

    enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode   { SILENT, VIBRATE, NORMAL }
    enum class InfoType     {
        TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS,
        RAM_USAGE, CPU_TEMP, UPTIME, NETWORK_SPEED, IP_ADDRESS
    }

    // ── Tipos de resultado que devuelve classify() ──────────────────────────

    sealed class IntentClass {
        data class Local(val action: LocalAction)      : IntentClass()
        data class Complex(val subtasks: List<String>) : IntentClass()
        object Delegate                                 : IntentClass()
        data class Hybrid(
            val localSteps: List<LocalAction>,
            val delegateText: String?           // null = IRIS resolvió todo
        ) : IntentClass()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: CADENA DE MATCHING  ← REGISTRA AQUÍ TUS FUNCIONES NUEVAS
    // ══════════════════════════════════════════════════════════════════════════
    //
    //  Esto es la "receta" de IRIS:
    //  IRIS prueba cada función match*() de arriba hacia abajo.
    //  En cuanto UNA devuelve algo distinto de null, para y usa ese resultado.
    //
    //  REGLA IMPORTANTE:
    //    - Pon los casos MÁS ESPECÍFICOS primero.
    //    - matchOpenApp() siempre al final (es el comodín genérico).
    //
    //  ┌──────────────────────────────────────────────────────────────────────┐
    //  │  Para registrar una función nueva:                                    │
    //  │  Añade  ?: matchTuFuncion(lo)                                         │
    //  │  ANTES de  ?: matchOpenApp(lo)                                        │
    //  └──────────────────────────────────────────────────────────────────────┘

    private fun tryLocal(lo: String): LocalAction? = with(IrisMotor) {
        matchGreeting(lo)
        ?: matchFarewell(lo)
        ?: matchGratitude(lo)
        ?: matchAffirmation(lo)
        ?: matchMemoryQuery(lo)
        ?: matchCalculate(lo)               // ← antes que matchOpenApp (evita "calcula" → abrir calc)
        ?: matchConvert(lo)
        ?: matchQuickNote(lo)
        ?: matchQuickReminder(lo)
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
        ?: matchSearchAndPlaySpotify(lo)    // ← ANTES que matchMusic
        ?: matchMusic(lo)
        ?: matchUrl(lo)
        ?: matchSystemQuery(lo)
        ?: matchNavigationActions(lo)
        ?: matchSettingsShortcuts(lo)
        ?: matchDeviceMaintenance(lo)
        ?: matchShopping(lo)
        ?: matchShare(lo)
        ?: matchQuickApps(lo)
        ?: matchKnowledgeSearch(lo)         // ← preguntas generales → Google directo
        // ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
        //  👇 REGISTRA AQUÍ TUS FUNCIONES NUEVAS  (antes de matchOpenApp)
        //
        //  Ejemplo:
        //     ?: matchModoTeatro(lo)
        //
        // ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
        ?: matchOpenApp(lo)                 // ← SIEMPRE AL FINAL (comodín genérico)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GUARDAS — Reglas que hacen que IRIS le pase el control a la IA
    // ══════════════════════════════════════════════════════════════════════════
    //
    //  Si el texto del usuario coincide con alguna de estas expresiones,
    //  IRIS no intenta resolverlo: se lo manda directamente a la IA.
    //
    //  QUESTION_PREFIXES → preguntas complejas: "¿cómo funciona X?", "¿qué es Y?"
    //  EXTERNAL_CONTEXT  → cosas fuera del teléfono: "del carro", "del televisor"
    //  MULTI_TASK        → comandos en cadena: "haz X y luego Y"

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
        "de mi pc|de la laptop|de mi laptop|de la consola|del xbox|del playstation|" +
        "de mi tablet|de la tablet|del kindle|del ebook)\\b",
        RegexOption.IGNORE_CASE
    )

    private val MULTI_TASK = Regex(
        "\\b(y luego|y despues|y tambien|ademas|and then|y enseguida|despues de eso)\\b",
        RegexOption.IGNORE_CASE
    )

    // ══════════════════════════════════════════════════════════════════════════
    // PUNTO DE ENTRADA PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════════
    //
    //  Aquí comienza todo. ViewModelPrincipal llama a classify(texto).
    //  No deberías necesitar editar nada aquí abajo.

    fun classify(input: String): IntentClass {
        val lo = normalize(input).lowercase()

        // Primero intenta acciones locales (pueden tener prefijos de pregunta,
        // ej: "cuánta batería" → QueryInfo.BATTERY)
        tryLocal(lo)?.let { return IntentClass.Local(it) }

        // Preguntas complejas y objetos externos → siempre a la IA
        if (QUESTION_PREFIXES.containsMatchIn(lo)) return IntentClass.Delegate
        if (EXTERNAL_CONTEXT.containsMatchIn(lo))  return IntentClass.Delegate

        // Comandos en cadena → resolución híbrida (IRIS + IA)
        if (MULTI_TASK.containsMatchIn(lo)) {
            val parts = splitSubtasks(input)
            if (parts.size >= 2) return classifyHybrid(parts)
        }

        return IntentClass.Delegate
    }

    /**
     * Para un comando multi-paso, IRIS clasifica cada parte por separado:
     *  - Las que puede resolver → [Hybrid.localSteps]
     *  - Las que no puede      → [Hybrid.delegateText] para la IA
     */
    private fun classifyHybrid(parts: List<String>): IntentClass {
        val localSteps    = mutableListOf<LocalAction>()
        val delegateParts = mutableListOf<String>()
        for (part in parts) {
            val lo = normalize(part).lowercase()
            tryLocal(lo)?.let { localSteps.add(it) } ?: delegateParts.add(part)
        }
        return when {
            delegateParts.isEmpty() -> IntentClass.Hybrid(localSteps, null)
            localSteps.isEmpty()    -> IntentClass.Complex(parts)
            else                    -> IntentClass.Hybrid(localSteps, delegateParts.joinToString(" y luego "))
        }
    }

    /** Parte un comando en múltiples subtareas separadas por conectores. */
    private fun splitSubtasks(text: String): List<String> {
        var parts = listOf(text)
        listOf(
            Regex("\\s+y luego\\s+",        RegexOption.IGNORE_CASE),
            Regex("\\s+y despues\\s+",      RegexOption.IGNORE_CASE),
            Regex("\\s+y tambien\\s+",      RegexOption.IGNORE_CASE),
            Regex("\\s+ademas\\s+",         RegexOption.IGNORE_CASE),
            Regex("\\s+and then\\s+",       RegexOption.IGNORE_CASE),
            Regex("\\s+y enseguida\\s+",    RegexOption.IGNORE_CASE),
            Regex("\\s+despues de eso\\s+", RegexOption.IGNORE_CASE)
        ).forEach { r ->
            parts = parts.flatMap { r.split(it).map(String::trim).filter { p -> p.length > 5 } }
        }
        return parts
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NORMALIZACIÓN DE TEXTO
    // ══════════════════════════════════════════════════════════════════════════

    fun normalize(text: String): String {
        var s = text.trim()
            .replace(Regex("[¿¡.,;!?]+"), " ")
            .replace('á','a').replace('é','e').replace('í','i')
            .replace('ó','o').replace('ú','u').replace('ü','u')
            .replace('Á','A').replace('É','E').replace('Í','I')
            .replace('Ó','O').replace('Ú','U')
        for ((pattern, replacement) in SLANG_NORMALIZATIONS)
            s = s.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        s = WAKE_WORDS_PREFIX.replace(s, "")
        s = COURTESY_SUFFIX.replace(s, "")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESOLUCIÓN DE CONTACTOS
    // ══════════════════════════════════════════════════════════════════════════

    fun resolveContactNumber(context: Context, nameOrNumber: String): String? {
        if (nameOrNumber.matches(Regex("[\\+\\d\\s\\-\\(\\)]{7,}"))) return nameOrNumber
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameOrNumber%"),
            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
        ) ?: return null
        return cursor.use {
            if (it.moveToFirst())
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            else null
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

    // ══════════════════════════════════════════════════════════════════════════
    // ACCESORES PÚBLICOS — respuestas sociales y prompt optimizado
    // ══════════════════════════════════════════════════════════════════════════

    fun greetingResponse()    = IrisDiccionario.GREETING_RESPONSES[(System.currentTimeMillis() % IrisDiccionario.GREETING_RESPONSES.size).toInt()]
    fun farewellResponse()    = IrisDiccionario.FAREWELL_RESPONSES[(System.currentTimeMillis() % IrisDiccionario.FAREWELL_RESPONSES.size).toInt()]
    fun gratitudeResponse()   = IrisDiccionario.GRATITUDE_RESPONSES[(System.currentTimeMillis() % IrisDiccionario.GRATITUDE_RESPONSES.size).toInt()]
    fun affirmationResponse() = IrisDiccionario.AFFIRMATION_RESPONSES[(System.currentTimeMillis() % IrisDiccionario.AFFIRMATION_RESPONSES.size).toInt()]

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String {
        if (subtasks.size <= 1) return originalInput
        val sb = StringBuilder("SECUENCIA:\n")
        subtasks.forEachIndexed { i, t -> sb.append("${i + 1}.$t\n") }
        sb.append("REGLA:ejecuta herramienta por herramienta.Sin texto entre pasos.")
        return sb.toString()
    }
}
