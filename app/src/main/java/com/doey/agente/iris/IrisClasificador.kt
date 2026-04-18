package com.doey.agente.iris

import android.content.Context
import android.provider.ContactsContract
import com.doey.agente.iris.IrisDiccionario.SLANG_NORMALIZATIONS
import com.doey.agente.iris.IrisDiccionario.WAKE_WORDS_PREFIX
import com.doey.agente.iris.IrisDiccionario.COURTESY_SUFFIX

/**
 * IRIS v6 — Intent Recognition & Interpretation System
 * Clasificador principal. Expone [classify] como punto de entrada único.
 *
 * Arquitectura modular:
 *   IrisDiccionario.kt  → sinónimos, vocabulario, respuestas sociales
 *   IrisMotor.kt        → funciones match*() por dominio
 *   IrisClasificador.kt → este archivo: tipos, guardas, normalización, orquestación
 *
 * PRINCIPIOS:
 *  1. Verbos centrales definidos UNA VEZ en IrisDiccionario.
 *  2. Solo actúa si la instrucción es directa y sin ambigüedad.
 *  3. Frase con contexto narrativo, condicional o explicativo → Delegate.
 *  4. Objeto externo al dispositivo (carro, TV, casa) → Delegate.
 *  5. Solo ejecuta acciones que Android puede realizar directamente.
 *  6. Tolerancia total a variantes fonéticas, voz-a-texto y slang MX.
 *  7. Comandos complejos (Y LUEGO / Y TAMBIÉN / etc.) → Complex(subtasks).
 *  8. Sin duplicar sinónimos — cada verbo o concepto vive en UN solo lugar.
 */
object IrisClasificador {

    // ══════════════════════════════════════════════════════════════════════════
    // TIPOS DE RESULTADO
    // ══════════════════════════════════════════════════════════════════════════

    sealed class IntentClass {
        data class Local(val action: LocalAction)       : IntentClass()
        data class Complex(val subtasks: List<String>)  : IntentClass()
        object Delegate                                  : IntentClass()
        /**
         * Ejecución híbrida: IRIS resuelve lo que puede, delega el resto a la IA.
         * - [localSteps]   → acciones que IRIS ejecuta directamente (en orden)
         * - [delegateText] → texto reducido que se manda a la IA (sin las partes ya resueltas)
         *                    null si IRIS resolvió TODO y no hay nada que delegar
         */
        data class Hybrid(
            val localSteps: List<LocalAction>,
            val delegateText: String?
        ) : IntentClass()
    }

    sealed class LocalAction {
        // ── Respuestas sociales ───────────────────────────────────────────────
        data class Greeting(val variant: Int)    : LocalAction()
        data class Farewell(val variant: Int)    : LocalAction()
        data class Gratitude(val variant: Int)   : LocalAction()
        data class Affirmation(val variant: Int) : LocalAction()
        // ── Memoria ───────────────────────────────────────────────────────────
        data class QueryMemory(val raw: String)  : LocalAction()
        // ── Dispositivo ───────────────────────────────────────────────────────
        data class ToggleFlashlight(val enable: Boolean)                                          : LocalAction()
        data class SetVolume(val level: Int, val stream: VolumeStream = VolumeStream.MEDIA)        : LocalAction()
        data class VolumeStep(val up: Boolean,  val stream: VolumeStream = VolumeStream.MEDIA)     : LocalAction()
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
        class  TakeScreenshot : LocalAction()
        class  LockScreen     : LocalAction()
        class  TogglePowerSave : LocalAction()
        // ── Alarmas y tiempo ─────────────────────────────────────────────────
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        data class SetAlarmNative(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class  CancelAlarm    : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class  CancelTimer    : LocalAction()
        class  StartStopwatch : LocalAction()
        class  StopStopwatch  : LocalAction()
        // ── Comunicación ──────────────────────────────────────────────────────
        data class Call(val contact: String)              : LocalAction()
        data class CallEmergency(val number: String)      : LocalAction()
        data class SendSms(val contact: String, val message: String)       : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String)  : LocalAction()
        data class SendTelegram(val contact: String, val message: String)  : LocalAction()
        data class OpenWhatsAppChat(val contact: String)  : LocalAction()
        // ── Apps y navegación ─────────────────────────────────────────────────
        data class OpenApp(val query: String)             : LocalAction()
        data class Navigate(val destination: String)      : LocalAction()
        data class SearchWeb(val query: String)           : LocalAction()
        data class SearchMaps(val query: String)          : LocalAction()
        data class OpenUrl(val url: String)               : LocalAction()
        // ── Música ────────────────────────────────────────────────────────────
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        data class SearchAndPlaySpotify(val query: String) : LocalAction()
        class  PauseMusic   : LocalAction()
        class  ResumeMusic  : LocalAction()
        class  NextTrack    : LocalAction()
        class  PrevTrack    : LocalAction()
        class  ShuffleMusic : LocalAction()
        class  RepeatToggle : LocalAction()
        // ── Compartir ──────────────────────────────────────────────────────────
        data class ShareText(val text: String) : LocalAction()
        // ── Apps rápidas ──────────────────────────────────────────────────────
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
        // ── Info del sistema ──────────────────────────────────────────────────
        data class QueryInfo(val type: InfoType) : LocalAction()
        // ── Listas de compras ─────────────────────────────────────────────────
        data class AddShoppingItem(val item: String) : LocalAction()
        class  ClearShoppingList : LocalAction()
        // ── Notas rápidas offline ─────────────────────────────────────────────
        data class QuickNote(val content: String)     : LocalAction()
        data class ReadNotes(val tag: String = "")    : LocalAction()
        // ── Calculadora offline ───────────────────────────────────────────────
        data class Calculate(val expression: String) : LocalAction()
        // ── Conversión de unidades offline ────────────────────────────────────
        data class Convert(val value: Double, val from: String, val to: String) : LocalAction()
        // ── Recordatorio rápido offline ───────────────────────────────────────
        data class QuickReminder(val text: String, val inMinutes: Int) : LocalAction()
        // ── Ajustes ───────────────────────────────────────────────────────────
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
        // ── Navegación del sistema ────────────────────────────────────────────
        class GoHome             : LocalAction()
        class BackButton         : LocalAction()
        class ShowRecentApps     : LocalAction()
        class ClearNotifications : LocalAction()
    }

    enum class VolumeStream  { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode    { SILENT, VIBRATE, NORMAL }
    enum class InfoType      {
        TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS,
        RAM_USAGE, CPU_TEMP, UPTIME, NETWORK_SPEED, IP_ADDRESS
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GUARDAS — disparan Delegate inmediatamente
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    // NORMALIZACIÓN — quita tildes, signos, wake words, slang voz-a-texto
    // ══════════════════════════════════════════════════════════════════════════

    fun normalize(text: String): String {
        var s = text.trim()
            .replace(Regex("[¿¡.,;!?]+"), " ")
            .replace('á','a').replace('é','e').replace('í','i')
            .replace('ó','o').replace('ú','u').replace('ü','u')
            .replace('Á','A').replace('É','E').replace('Í','I')
            .replace('Ó','O').replace('Ú','U')

        // Slang y fonética MX desde IrisDiccionario
        for ((pattern, replacement) in SLANG_NORMALIZATIONS) {
            s = s.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        }

        // Wake words y cortesías
        s = WAKE_WORDS_PREFIX.replace(s, "")
        s = COURTESY_SUFFIX.replace(s, "")

        return s.replace(Regex("\\s+"), " ").trim()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLASIFICADOR PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════════

    fun classify(input: String): IntentClass {
        val lo = normalize(input).lowercase()

        if (QUESTION_PREFIXES.containsMatchIn(lo)) return IntentClass.Delegate
        if (EXTERNAL_CONTEXT.containsMatchIn(lo))  return IntentClass.Delegate

        // Comando con múltiples subtareas → intentar resolución híbrida
        if (MULTI_TASK.containsMatchIn(lo)) {
            val parts = splitSubtasks(input)
            if (parts.size >= 2) return classifyHybrid(parts)
        }

        tryLocal(lo)?.let { return IntentClass.Local(it) }
        return IntentClass.Delegate
    }

    /**
     * Para un comando multi-paso, clasifica cada subtarea individualmente:
     *  - Las que IRIS puede resolver → [Hybrid.localSteps]
     *  - Las que no puede → se juntan en [Hybrid.delegateText] para la IA
     *
     * Si IRIS puede resolver TODAS → Hybrid(localSteps, delegateText=null)
     * Si IRIS no puede resolver NINGUNA → Complex(parts) (comportamiento anterior)
     * Si resuelve ALGUNAS → Hybrid(localSteps, delegateText)
     */
    private fun classifyHybrid(parts: List<String>): IntentClass {
        val localSteps   = mutableListOf<LocalAction>()
        val delegateParts = mutableListOf<String>()

        for (part in parts) {
            val lo = normalize(part).lowercase()
            val action = tryLocal(lo)
            if (action != null) {
                localSteps.add(action)
            } else {
                delegateParts.add(part)
            }
        }

        return when {
            // IRIS resuelve todo — no hay nada que delegar
            delegateParts.isEmpty() -> IntentClass.Hybrid(localSteps, null)
            // IRIS no resuelve nada — comportamiento anterior (todo a la IA)
            localSteps.isEmpty()    -> IntentClass.Complex(parts)
            // IRIS resuelve algunas, delega el resto — el caso nuevo
            else -> IntentClass.Hybrid(
                localSteps   = localSteps,
                delegateText = delegateParts.joinToString(" y luego ")
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CADENA DE MATCHING — orden importa (más específico primero)
    // ══════════════════════════════════════════════════════════════════════════

    private fun tryLocal(lo: String): LocalAction? = with(IrisMotor) {
        matchGreeting(lo)
        ?: matchFarewell(lo)
        ?: matchGratitude(lo)
        ?: matchAffirmation(lo)
        ?: matchMemoryQuery(lo)
        ?: matchCalculate(lo)          // antes que matchOpenApp — "calcula 5+5" no es "abrir calc"
        ?: matchConvert(lo)            // conversiones de unidades offline
        ?: matchQuickNote(lo)          // notas rápidas offline
        ?: matchQuickReminder(lo)      // recordatorio rápido
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
        ?: matchSearchAndPlaySpotify(lo)  // ANTES que matchMusic
        ?: matchMusic(lo)
        ?: matchUrl(lo)
        ?: matchSystemQuery(lo)
        ?: matchNavigationActions(lo)
        ?: matchSettingsShortcuts(lo)
        ?: matchDeviceMaintenance(lo)
        ?: matchShopping(lo)
        ?: matchShare(lo)
        ?: matchQuickApps(lo)
        ?: matchOpenApp(lo)               // siempre al final — captura genérica
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SPLITTER DE SUB-TAREAS
    // ══════════════════════════════════════════════════════════════════════════

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
