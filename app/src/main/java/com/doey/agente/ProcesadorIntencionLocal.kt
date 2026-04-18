package com.doey.agente

import android.content.Context
import com.doey.agente.iris.IrisClasificador

/**
 * ProcesadorIntencionLocal — Wrapper de compatibilidad para IRIS v6
 *
 * Mantiene la API pública original (tipos, constantes, métodos) para que
 * ViewModelPrincipal, ServicioModoAmigable y cualquier otro consumidor sigan
 * compilando sin cambios.
 *
 * El motor real vive en com.doey.agente.iris.*
 */
object LocalIntentProcessor {

    // ══════════════════════════════════════════════════════════════════════════
    // TIPOS PÚBLICOS — re-expuestos desde IrisClasificador para compatibilidad
    // ══════════════════════════════════════════════════════════════════════════

    sealed class IntentClass {
        data class Local(val action: LocalAction)       : IntentClass()
        data class Complex(val subtasks: List<String>)  : IntentClass()
        object Delegate                                  : IntentClass()
        data class Hybrid(
            val localSteps: List<LocalAction>,
            val delegateText: String?
        ) : IntentClass()
    }

    sealed class LocalAction {
        data class Greeting(val variant: Int)    : LocalAction()
        data class Farewell(val variant: Int)    : LocalAction()
        data class Gratitude(val variant: Int)   : LocalAction()
        data class Affirmation(val variant: Int) : LocalAction()
        data class QueryMemory(val raw: String)  : LocalAction()
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
        class  TakeScreenshot  : LocalAction()
        class  LockScreen      : LocalAction()
        class  TogglePowerSave : LocalAction()
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        data class SetAlarmNative(val hour: Int, val minute: Int, val label: String = "", val daysOfWeek: List<Int> = emptyList()) : LocalAction()
        class  CancelAlarm     : LocalAction()
        data class SetTimer(val seconds: Long, val label: String = "") : LocalAction()
        class  CancelTimer     : LocalAction()
        class  StartStopwatch  : LocalAction()
        class  StopStopwatch   : LocalAction()
        data class Call(val contact: String)              : LocalAction()
        data class CallEmergency(val number: String)      : LocalAction()
        data class SendSms(val contact: String, val message: String)       : LocalAction()
        data class SendWhatsApp(val contact: String, val message: String)  : LocalAction()
        data class SendTelegram(val contact: String, val message: String)  : LocalAction()
        data class OpenWhatsAppChat(val contact: String)  : LocalAction()
        data class OpenApp(val query: String)             : LocalAction()
        data class Navigate(val destination: String)      : LocalAction()
        data class SearchWeb(val query: String)           : LocalAction()
        data class SearchMaps(val query: String)          : LocalAction()
        data class OpenUrl(val url: String)               : LocalAction()
        data class PlayMusic(val query: String, val app: String = "spotify") : LocalAction()
        data class SearchAndPlaySpotify(val query: String) : LocalAction()
        class  PauseMusic   : LocalAction()
        class  ResumeMusic  : LocalAction()
        class  NextTrack    : LocalAction()
        class  PrevTrack    : LocalAction()
        class  ShuffleMusic : LocalAction()
        class  RepeatToggle : LocalAction()
        data class ShareText(val text: String)            : LocalAction()
        class  OpenCamera     : LocalAction()
        class  OpenGallery    : LocalAction()
        class  OpenContacts   : LocalAction()
        class  OpenDialer     : LocalAction()
        class  OpenCalculator : LocalAction()
        class  OpenCalendar   : LocalAction()
        class  OpenMaps       : LocalAction()
        class  OpenBrowser    : LocalAction()
        class  OpenFiles      : LocalAction()
        class  OpenClock      : LocalAction()
        data class QueryInfo(val type: InfoType)          : LocalAction()
        data class AddShoppingItem(val item: String)      : LocalAction()
        class  ClearShoppingList : LocalAction()
        data class QuickNote(val content: String)         : LocalAction()
        data class ReadNotes(val tag: String = "")        : LocalAction()
        data class Calculate(val expression: String)      : LocalAction()
        data class Convert(val value: Double, val from: String, val to: String) : LocalAction()
        data class QuickReminder(val text: String, val inMinutes: Int) : LocalAction()
        class  OpenSettings               : LocalAction()
        class  OpenBatterySettings        : LocalAction()
        class  OpenWifiSettings           : LocalAction()
        class  OpenBluetoothSettings      : LocalAction()
        class  OpenDisplaySettings        : LocalAction()
        class  OpenSoundSettings          : LocalAction()
        class  OpenStorageSettings        : LocalAction()
        class  OpenAccessibilitySettings  : LocalAction()
        class  OpenDeveloperSettings      : LocalAction()
        class  OpenLocationSettings       : LocalAction()
        class  OpenSecuritySettings       : LocalAction()
        class  OpenAppsSettings           : LocalAction()
        class  OpenDateSettings           : LocalAction()
        class  OpenLanguageSettings       : LocalAction()
        class  OpenAccountSettings        : LocalAction()
        class  OpenPrivacySettings        : LocalAction()
        class  OpenNotificationSettings   : LocalAction()
        class  OpenNfcSettings            : LocalAction()
        class  OpenDataUsageSettings      : LocalAction()
        class  OpenVpnSettings            : LocalAction()
        class  OpenSyncSettings           : LocalAction()
        class  OpenInputMethodSettings    : LocalAction()
        class  OpenCastSettings           : LocalAction()
        class  OpenOverlaySettings        : LocalAction()
        class  OpenWriteSettings          : LocalAction()
        class  OpenUsageAccessSettings    : LocalAction()
        class  OpenZenModeSettings        : LocalAction()
        class  OpenPrintSettings          : LocalAction()
        class  OpenApnSettings            : LocalAction()
        class  OpenUserDictionarySettings : LocalAction()
        class  OpenDreamSettings          : LocalAction()
        class  OpenCaptioningSettings     : LocalAction()
        class  OpenSearchSettings         : LocalAction()
        class  GoHome             : LocalAction()
        class  BackButton         : LocalAction()
        class  ShowRecentApps     : LocalAction()
        class  ClearNotifications : LocalAction()
    }

    enum class VolumeStream  { MEDIA, RING, ALARM, NOTIFICATION }
    enum class SilentMode    { SILENT, VIBRATE, NORMAL }
    enum class InfoType      {
        TIME, DATE, BATTERY, STORAGE, WIFI_STATUS, BT_STATUS,
        RAM_USAGE, CPU_TEMP, UPTIME, NETWORK_SPEED, IP_ADDRESS
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLASIFICADOR — convierte resultado de IrisClasificador a tipos locales
    // ══════════════════════════════════════════════════════════════════════════

    fun classify(input: String): IntentClass {
        return when (val r = IrisClasificador.classify(input)) {
            is IrisClasificador.IntentClass.Complex  -> IntentClass.Complex(r.subtasks)
            is IrisClasificador.IntentClass.Delegate -> IntentClass.Delegate
            is IrisClasificador.IntentClass.Local    -> {
                val mapped = mapAction(r.action) ?: return IntentClass.Delegate
                IntentClass.Local(mapped)
            }
            is IrisClasificador.IntentClass.Hybrid   -> {
                val mappedSteps = r.localSteps.mapNotNull { mapAction(it) }
                if (mappedSteps.isEmpty()) IntentClass.Complex(listOf(input))
                else IntentClass.Hybrid(mappedSteps, r.delegateText)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAPEADOR — IrisClasificador.LocalAction → LocalIntentProcessor.LocalAction
    // ══════════════════════════════════════════════════════════════════════════

    private fun mapAction(a: IrisClasificador.LocalAction): LocalAction? = when (a) {
        is IrisClasificador.LocalAction.Greeting          -> LocalAction.Greeting(a.variant)
        is IrisClasificador.LocalAction.Farewell          -> LocalAction.Farewell(a.variant)
        is IrisClasificador.LocalAction.Gratitude         -> LocalAction.Gratitude(a.variant)
        is IrisClasificador.LocalAction.Affirmation       -> LocalAction.Affirmation(a.variant)
        is IrisClasificador.LocalAction.QueryMemory       -> LocalAction.QueryMemory(a.raw)
        is IrisClasificador.LocalAction.ToggleFlashlight  -> LocalAction.ToggleFlashlight(a.enable)
        is IrisClasificador.LocalAction.SetVolume         -> LocalAction.SetVolume(a.level, mapStream(a.stream))
        is IrisClasificador.LocalAction.VolumeStep        -> LocalAction.VolumeStep(a.up, mapStream(a.stream))
        is IrisClasificador.LocalAction.SetSilentMode     -> LocalAction.SetSilentMode(mapSilent(a.mode))
        is IrisClasificador.LocalAction.ToggleWifi        -> LocalAction.ToggleWifi(a.enable)
        is IrisClasificador.LocalAction.ToggleBluetooth   -> LocalAction.ToggleBluetooth(a.enable)
        is IrisClasificador.LocalAction.ToggleAirplane    -> LocalAction.ToggleAirplane(a.enable)
        is IrisClasificador.LocalAction.ToggleDoNotDisturb-> LocalAction.ToggleDoNotDisturb(a.enable)
        is IrisClasificador.LocalAction.ToggleNfc         -> LocalAction.ToggleNfc(a.enable)
        is IrisClasificador.LocalAction.ToggleDarkMode    -> LocalAction.ToggleDarkMode(a.enable)
        is IrisClasificador.LocalAction.ToggleHotspot     -> LocalAction.ToggleHotspot(a.enable)
        is IrisClasificador.LocalAction.SetBrightness     -> LocalAction.SetBrightness(a.level)
        is IrisClasificador.LocalAction.BrightnessStep    -> LocalAction.BrightnessStep(a.up)
        is IrisClasificador.LocalAction.ToggleAutoBrightness -> LocalAction.ToggleAutoBrightness(a.enable)
        is IrisClasificador.LocalAction.SetRingtoneVolume -> LocalAction.SetRingtoneVolume(a.level)
        is IrisClasificador.LocalAction.SetAlarmVolume    -> LocalAction.SetAlarmVolume(a.level)
        is IrisClasificador.LocalAction.SetScreenTimeout  -> LocalAction.SetScreenTimeout(a.seconds)
        is IrisClasificador.LocalAction.TakeScreenshot    -> LocalAction.TakeScreenshot()
        is IrisClasificador.LocalAction.LockScreen        -> LocalAction.LockScreen()
        is IrisClasificador.LocalAction.TogglePowerSave   -> LocalAction.TogglePowerSave()
        is IrisClasificador.LocalAction.SetAlarm          -> LocalAction.SetAlarm(a.hour, a.minute, a.label, a.daysOfWeek)
        is IrisClasificador.LocalAction.SetAlarmNative    -> LocalAction.SetAlarmNative(a.hour, a.minute, a.label, a.daysOfWeek)
        is IrisClasificador.LocalAction.CancelAlarm       -> LocalAction.CancelAlarm()
        is IrisClasificador.LocalAction.SetTimer          -> LocalAction.SetTimer(a.seconds, a.label)
        is IrisClasificador.LocalAction.CancelTimer       -> LocalAction.CancelTimer()
        is IrisClasificador.LocalAction.StartStopwatch    -> LocalAction.StartStopwatch()
        is IrisClasificador.LocalAction.StopStopwatch     -> LocalAction.StopStopwatch()
        is IrisClasificador.LocalAction.Call              -> LocalAction.Call(a.contact)
        is IrisClasificador.LocalAction.CallEmergency     -> LocalAction.CallEmergency(a.number)
        is IrisClasificador.LocalAction.SendSms           -> LocalAction.SendSms(a.contact, a.message)
        is IrisClasificador.LocalAction.SendWhatsApp      -> LocalAction.SendWhatsApp(a.contact, a.message)
        is IrisClasificador.LocalAction.SendTelegram      -> LocalAction.SendTelegram(a.contact, a.message)
        is IrisClasificador.LocalAction.OpenWhatsAppChat  -> LocalAction.OpenWhatsAppChat(a.contact)
        is IrisClasificador.LocalAction.OpenApp           -> LocalAction.OpenApp(a.query)
        is IrisClasificador.LocalAction.Navigate          -> LocalAction.Navigate(a.destination)
        is IrisClasificador.LocalAction.SearchWeb         -> LocalAction.SearchWeb(a.query)
        is IrisClasificador.LocalAction.SearchMaps        -> LocalAction.SearchMaps(a.query)
        is IrisClasificador.LocalAction.OpenUrl           -> LocalAction.OpenUrl(a.url)
        is IrisClasificador.LocalAction.PlayMusic         -> LocalAction.PlayMusic(a.query, a.app)
        is IrisClasificador.LocalAction.SearchAndPlaySpotify -> LocalAction.SearchAndPlaySpotify(a.query)
        is IrisClasificador.LocalAction.PauseMusic        -> LocalAction.PauseMusic()
        is IrisClasificador.LocalAction.ResumeMusic       -> LocalAction.ResumeMusic()
        is IrisClasificador.LocalAction.NextTrack         -> LocalAction.NextTrack()
        is IrisClasificador.LocalAction.PrevTrack         -> LocalAction.PrevTrack()
        is IrisClasificador.LocalAction.ShuffleMusic      -> LocalAction.ShuffleMusic()
        is IrisClasificador.LocalAction.RepeatToggle      -> LocalAction.RepeatToggle()
        is IrisClasificador.LocalAction.ShareText         -> LocalAction.ShareText(a.text)
        is IrisClasificador.LocalAction.OpenCamera        -> LocalAction.OpenCamera()
        is IrisClasificador.LocalAction.OpenGallery       -> LocalAction.OpenGallery()
        is IrisClasificador.LocalAction.OpenContacts      -> LocalAction.OpenContacts()
        is IrisClasificador.LocalAction.OpenDialer        -> LocalAction.OpenDialer()
        is IrisClasificador.LocalAction.OpenCalculator    -> LocalAction.OpenCalculator()
        is IrisClasificador.LocalAction.OpenCalendar      -> LocalAction.OpenCalendar()
        is IrisClasificador.LocalAction.OpenMaps          -> LocalAction.OpenMaps()
        is IrisClasificador.LocalAction.OpenBrowser       -> LocalAction.OpenBrowser()
        is IrisClasificador.LocalAction.OpenFiles         -> LocalAction.OpenFiles()
        is IrisClasificador.LocalAction.OpenClock         -> LocalAction.OpenClock()
        is IrisClasificador.LocalAction.QueryInfo         -> LocalAction.QueryInfo(mapInfo(a.type))
        is IrisClasificador.LocalAction.AddShoppingItem   -> LocalAction.AddShoppingItem(a.item)
        is IrisClasificador.LocalAction.ClearShoppingList -> LocalAction.ClearShoppingList()
        is IrisClasificador.LocalAction.QuickNote         -> LocalAction.QuickNote(a.content)
        is IrisClasificador.LocalAction.ReadNotes         -> LocalAction.ReadNotes(a.tag)
        is IrisClasificador.LocalAction.Calculate         -> LocalAction.Calculate(a.expression)
        is IrisClasificador.LocalAction.Convert           -> LocalAction.Convert(a.value, a.from, a.to)
        is IrisClasificador.LocalAction.QuickReminder     -> LocalAction.QuickReminder(a.text, a.inMinutes)
        is IrisClasificador.LocalAction.OpenSettings               -> LocalAction.OpenSettings()
        is IrisClasificador.LocalAction.OpenBatterySettings        -> LocalAction.OpenBatterySettings()
        is IrisClasificador.LocalAction.OpenWifiSettings           -> LocalAction.OpenWifiSettings()
        is IrisClasificador.LocalAction.OpenBluetoothSettings      -> LocalAction.OpenBluetoothSettings()
        is IrisClasificador.LocalAction.OpenDisplaySettings        -> LocalAction.OpenDisplaySettings()
        is IrisClasificador.LocalAction.OpenSoundSettings          -> LocalAction.OpenSoundSettings()
        is IrisClasificador.LocalAction.OpenStorageSettings        -> LocalAction.OpenStorageSettings()
        is IrisClasificador.LocalAction.OpenAccessibilitySettings  -> LocalAction.OpenAccessibilitySettings()
        is IrisClasificador.LocalAction.OpenDeveloperSettings      -> LocalAction.OpenDeveloperSettings()
        is IrisClasificador.LocalAction.OpenLocationSettings       -> LocalAction.OpenLocationSettings()
        is IrisClasificador.LocalAction.OpenSecuritySettings       -> LocalAction.OpenSecuritySettings()
        is IrisClasificador.LocalAction.OpenAppsSettings           -> LocalAction.OpenAppsSettings()
        is IrisClasificador.LocalAction.OpenDateSettings           -> LocalAction.OpenDateSettings()
        is IrisClasificador.LocalAction.OpenLanguageSettings       -> LocalAction.OpenLanguageSettings()
        is IrisClasificador.LocalAction.OpenAccountSettings        -> LocalAction.OpenAccountSettings()
        is IrisClasificador.LocalAction.OpenPrivacySettings        -> LocalAction.OpenPrivacySettings()
        is IrisClasificador.LocalAction.OpenNotificationSettings   -> LocalAction.OpenNotificationSettings()
        is IrisClasificador.LocalAction.OpenNfcSettings            -> LocalAction.OpenNfcSettings()
        is IrisClasificador.LocalAction.OpenDataUsageSettings      -> LocalAction.OpenDataUsageSettings()
        is IrisClasificador.LocalAction.OpenVpnSettings            -> LocalAction.OpenVpnSettings()
        is IrisClasificador.LocalAction.OpenSyncSettings           -> LocalAction.OpenSyncSettings()
        is IrisClasificador.LocalAction.OpenInputMethodSettings    -> LocalAction.OpenInputMethodSettings()
        is IrisClasificador.LocalAction.OpenCastSettings           -> LocalAction.OpenCastSettings()
        is IrisClasificador.LocalAction.OpenOverlaySettings        -> LocalAction.OpenOverlaySettings()
        is IrisClasificador.LocalAction.OpenWriteSettings          -> LocalAction.OpenWriteSettings()
        is IrisClasificador.LocalAction.OpenUsageAccessSettings    -> LocalAction.OpenUsageAccessSettings()
        is IrisClasificador.LocalAction.OpenZenModeSettings        -> LocalAction.OpenZenModeSettings()
        is IrisClasificador.LocalAction.OpenPrintSettings          -> LocalAction.OpenPrintSettings()
        is IrisClasificador.LocalAction.OpenApnSettings            -> LocalAction.OpenApnSettings()
        is IrisClasificador.LocalAction.OpenUserDictionarySettings -> LocalAction.OpenUserDictionarySettings()
        is IrisClasificador.LocalAction.OpenDreamSettings          -> LocalAction.OpenDreamSettings()
        is IrisClasificador.LocalAction.OpenCaptioningSettings     -> LocalAction.OpenCaptioningSettings()
        is IrisClasificador.LocalAction.OpenSearchSettings         -> LocalAction.OpenSearchSettings()
        is IrisClasificador.LocalAction.GoHome             -> LocalAction.GoHome()
        is IrisClasificador.LocalAction.BackButton         -> LocalAction.BackButton()
        is IrisClasificador.LocalAction.ShowRecentApps     -> LocalAction.ShowRecentApps()
        is IrisClasificador.LocalAction.ClearNotifications -> LocalAction.ClearNotifications()
        else -> null
    }

    private fun mapStream(s: IrisClasificador.VolumeStream): VolumeStream = when (s) {
        IrisClasificador.VolumeStream.MEDIA        -> VolumeStream.MEDIA
        IrisClasificador.VolumeStream.RING         -> VolumeStream.RING
        IrisClasificador.VolumeStream.ALARM        -> VolumeStream.ALARM
        IrisClasificador.VolumeStream.NOTIFICATION -> VolumeStream.NOTIFICATION
    }

    private fun mapSilent(s: IrisClasificador.SilentMode): SilentMode = when (s) {
        IrisClasificador.SilentMode.SILENT  -> SilentMode.SILENT
        IrisClasificador.SilentMode.VIBRATE -> SilentMode.VIBRATE
        IrisClasificador.SilentMode.NORMAL  -> SilentMode.NORMAL
    }

    private fun mapInfo(t: IrisClasificador.InfoType): InfoType = when (t) {
        IrisClasificador.InfoType.TIME          -> InfoType.TIME
        IrisClasificador.InfoType.DATE          -> InfoType.DATE
        IrisClasificador.InfoType.BATTERY       -> InfoType.BATTERY
        IrisClasificador.InfoType.STORAGE       -> InfoType.STORAGE
        IrisClasificador.InfoType.WIFI_STATUS   -> InfoType.WIFI_STATUS
        IrisClasificador.InfoType.BT_STATUS     -> InfoType.BT_STATUS
        IrisClasificador.InfoType.RAM_USAGE     -> InfoType.RAM_USAGE
        IrisClasificador.InfoType.CPU_TEMP      -> InfoType.CPU_TEMP
        IrisClasificador.InfoType.UPTIME        -> InfoType.UPTIME
        IrisClasificador.InfoType.NETWORK_SPEED -> InfoType.NETWORK_SPEED
        IrisClasificador.InfoType.IP_ADDRESS    -> InfoType.IP_ADDRESS
    }

    // ══════════════════════════════════════════════════════════════════════════
    // API PÚBLICA — respuestas sociales y utilitarios
    // ══════════════════════════════════════════════════════════════════════════

    fun greetingResponse(): String    = IrisClasificador.greetingResponse()
    fun farewellResponse(): String    = IrisClasificador.farewellResponse()
    fun gratitudeResponse(): String   = IrisClasificador.gratitudeResponse()
    fun affirmationResponse(): String = IrisClasificador.affirmationResponse()

    fun resolveContactNumber(context: Context, nameOrNumber: String): String? =
        IrisClasificador.resolveContactNumber(context, nameOrNumber)

    fun resolveContactFromMemory(memory: String, nameQuery: String): String? =
        IrisClasificador.resolveContactFromMemory(memory, nameQuery)

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String =
        IrisClasificador.buildOptimizedPrompt(subtasks, originalInput)
}
