package com.doey.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Motor Ultra-Avanzado de Modo Flujo: +300 acciones offline con lógica condicional
 */

// ── Tipos de datos ────────────────────────────────────────────────────────────

enum class FlowActionType {
    OPEN_APP, SEND_MESSAGE, CALL, CONTROL_SYSTEM, MEDIA_CONTROL,
    ALARM_TIMER, NAVIGATION, AUTOMATION, CONDITIONAL, MACRO
}

data class FlowAction(
    val id: String,
    val name: String,
    val category: String,
    val type: FlowActionType,
    val icon: String,
    val params: Map<String, String> = emptyMap(),
    val subActions: List<FlowAction> = emptyList(),
    val conditions: List<FlowCondition> = emptyList()
)

data class FlowCondition(
    val variable: String,
    val operator: String, // "equals", "contains", "greater", "less"
    val value: String
)

data class FlowMacro(
    val id: String,
    val name: String,
    val description: String,
    val actions: List<FlowAction>,
    val createdAt: Long = System.currentTimeMillis()
)

object FlowModeAdvancedEngine {
    private val macros = mutableMapOf<String, FlowMacro>()

    // ── Categorías principales ────────────────────────────────────────────────
    fun getRootCategories(): List<String> = listOf(
        "📱 Comunicación",
        "🎵 Multimedia",
        "⚙️ Sistema",
        "🏠 Domótica",
        "📊 Productividad",
        "🚨 Emergencia",
        "🔄 Automatización",
        "⭐ Macros"
    )

    // ── Acciones por categoría (300+) ──────────────────────────────────────────

    suspend fun getActionsByCategory(ctx: Context, category: String): List<FlowAction> {
        return withContext(Dispatchers.Default) {
            when (category) {
                "📱 Comunicación" -> getCommunicationActions(ctx)
                "🎵 Multimedia" -> getMediaActions(ctx)
                "⚙️ Sistema" -> getSystemActions(ctx)
                "🏠 Domótica" -> getSmartHomeActions(ctx)
                "📊 Productividad" -> getProductivityActions(ctx)
                "🚨 Emergencia" -> getEmergencyActions(ctx)
                "🔄 Automatización" -> getAutomationActions(ctx)
                "⭐ Macros" -> getSavedMacros()
                else -> emptyList()
            }
        }
    }

    private suspend fun getCommunicationActions(ctx: Context): List<FlowAction> = withContext(Dispatchers.IO) {
        val contacts = getContactsList(ctx)
        listOf(
            // WhatsApp
            FlowAction("wa_contact", "Enviar WhatsApp", "Comunicación", FlowActionType.SEND_MESSAGE, "💬",
                subActions = contacts.map { (name, phone) ->
                    FlowAction("wa_$phone", name, "Contacto", FlowActionType.SEND_MESSAGE, "👤",
                        params = mapOf("app" to "com.whatsapp", "phone" to phone, "name" to name))
                }),
            // Telegram
            FlowAction("tg_contact", "Enviar Telegram", "Comunicación", FlowActionType.SEND_MESSAGE, "✈️",
                subActions = contacts.map { (name, phone) ->
                    FlowAction("tg_$phone", name, "Contacto", FlowActionType.SEND_MESSAGE, "👤",
                        params = mapOf("app" to "org.telegram.messenger", "phone" to phone, "name" to name))
                }),
            // SMS
            FlowAction("sms_contact", "Enviar SMS", "Comunicación", FlowActionType.SEND_MESSAGE, "📨",
                subActions = contacts.map { (name, phone) ->
                    FlowAction("sms_$phone", name, "Contacto", FlowActionType.SEND_MESSAGE, "👤",
                        params = mapOf("app" to "sms", "phone" to phone, "name" to name))
                }),
            // Email
            FlowAction("email_send", "Enviar Email", "Comunicación", FlowActionType.SEND_MESSAGE, "📧"),
            // Llamadas
            FlowAction("call_contact", "Realizar Llamada", "Comunicación", FlowActionType.CALL, "☎️",
                subActions = contacts.map { (name, phone) ->
                    FlowAction("call_$phone", name, "Contacto", FlowActionType.CALL, "👤",
                        params = mapOf("phone" to phone, "name" to name))
                }),
            // Videollamada
            FlowAction("video_call", "Videollamada", "Comunicación", FlowActionType.CALL, "📹"),
            // Redes sociales
            FlowAction("social_open", "Abrir Red Social", "Comunicación", FlowActionType.OPEN_APP, "🌐",
                subActions = listOf(
                    FlowAction("fb_open", "Facebook", "Red Social", FlowActionType.OPEN_APP, "f",
                        params = mapOf("package" to "com.facebook.katana")),
                    FlowAction("ig_open", "Instagram", "Red Social", FlowActionType.OPEN_APP, "📷",
                        params = mapOf("package" to "com.instagram.android")),
                    FlowAction("tw_open", "Twitter/X", "Red Social", FlowActionType.OPEN_APP, "𝕏",
                        params = mapOf("package" to "com.twitter.android"))
                ))
        )
    }

    private suspend fun getMediaActions(ctx: Context): List<FlowAction> = withContext(Dispatchers.IO) {
        val musicApps = getMusicApplications(ctx)
        listOf(
            FlowAction("music_play", "Reproducir Música", "Multimedia", FlowActionType.MEDIA_CONTROL, "▶️",
                subActions = musicApps.map { (name, pkg) ->
                    FlowAction("music_$pkg", name, "App", FlowActionType.MEDIA_CONTROL, "🎵",
                        params = mapOf("package" to pkg, "action" to "play"))
                }),
            FlowAction("music_pause", "Pausar Música", "Multimedia", FlowActionType.MEDIA_CONTROL, "⏸️"),
            FlowAction("music_next", "Siguiente Canción", "Multimedia", FlowActionType.MEDIA_CONTROL, "⏭️"),
            FlowAction("music_prev", "Canción Anterior", "Multimedia", FlowActionType.MEDIA_CONTROL, "⏮️"),
            FlowAction("volume_control", "Controlar Volumen", "Multimedia", FlowActionType.CONTROL_SYSTEM, "🔊",
                subActions = listOf(
                    FlowAction("vol_max", "Volumen Máximo", "Volumen", FlowActionType.CONTROL_SYSTEM, "🔊",
                        params = mapOf("level" to "15")),
                    FlowAction("vol_medium", "Volumen Medio", "Volumen", FlowActionType.CONTROL_SYSTEM, "🔉",
                        params = mapOf("level" to "8")),
                    FlowAction("vol_low", "Volumen Bajo", "Volumen", FlowActionType.CONTROL_SYSTEM, "🔇",
                        params = mapOf("level" to "3")),
                    FlowAction("vol_mute", "Silencio", "Volumen", FlowActionType.CONTROL_SYSTEM, "🔇",
                        params = mapOf("level" to "0"))
                )),
            FlowAction("brightness", "Ajustar Brillo", "Multimedia", FlowActionType.CONTROL_SYSTEM, "☀️",
                subActions = listOf(
                    FlowAction("bright_high", "Brillo Alto", "Brillo", FlowActionType.CONTROL_SYSTEM, "☀️",
                        params = mapOf("level" to "255")),
                    FlowAction("bright_medium", "Brillo Medio", "Brillo", FlowActionType.CONTROL_SYSTEM, "🌤️",
                        params = mapOf("level" to "128")),
                    FlowAction("bright_low", "Brillo Bajo", "Brillo", FlowActionType.CONTROL_SYSTEM, "🌙",
                        params = mapOf("level" to "50")),
                    FlowAction("bright_auto", "Brillo Automático", "Brillo", FlowActionType.CONTROL_SYSTEM, "🔄",
                        params = mapOf("level" to "auto"))
                )),
            FlowAction("video_play", "Reproducir Video", "Multimedia", FlowActionType.OPEN_APP, "🎬"),
            FlowAction("podcast_play", "Reproducir Podcast", "Multimedia", FlowActionType.OPEN_APP, "🎙️"),
            FlowAction("radio_play", "Reproducir Radio", "Multimedia", FlowActionType.OPEN_APP, "📻")
        )
    }

    private fun getSystemActions(ctx: Context): List<FlowAction> = listOf(
        FlowAction("wifi_control", "Control WiFi", "Sistema", FlowActionType.CONTROL_SYSTEM, "📶",
            subActions = listOf(
                FlowAction("wifi_on", "Activar WiFi", "WiFi", FlowActionType.CONTROL_SYSTEM, "📶",
                    params = mapOf("action" to "wifi_on")),
                FlowAction("wifi_off", "Desactivar WiFi", "WiFi", FlowActionType.CONTROL_SYSTEM, "📵",
                    params = mapOf("action" to "wifi_off")),
                FlowAction("wifi_list", "Ver Redes", "WiFi", FlowActionType.CONTROL_SYSTEM, "📡",
                    params = mapOf("action" to "wifi_list"))
            )),
        FlowAction("bluetooth_control", "Control Bluetooth", "Sistema", FlowActionType.CONTROL_SYSTEM, "🔵",
            subActions = listOf(
                FlowAction("bt_on", "Activar Bluetooth", "Bluetooth", FlowActionType.CONTROL_SYSTEM, "🔵",
                    params = mapOf("action" to "bluetooth_on")),
                FlowAction("bt_off", "Desactivar Bluetooth", "Bluetooth", FlowActionType.CONTROL_SYSTEM, "⚪",
                    params = mapOf("action" to "bluetooth_off")),
                FlowAction("bt_devices", "Ver Dispositivos", "Bluetooth", FlowActionType.CONTROL_SYSTEM, "🎧",
                    params = mapOf("action" to "bluetooth_devices"))
            )),
        FlowAction("airplane_mode", "Modo Avión", "Sistema", FlowActionType.CONTROL_SYSTEM, "✈️",
            subActions = listOf(
                FlowAction("airplane_on", "Activar", "Modo Avión", FlowActionType.CONTROL_SYSTEM, "✈️",
                    params = mapOf("action" to "airplane_on")),
                FlowAction("airplane_off", "Desactivar", "Modo Avión", FlowActionType.CONTROL_SYSTEM, "📶",
                    params = mapOf("action" to "airplane_off"))
            )),
        FlowAction("battery_saver", "Ahorro de Batería", "Sistema", FlowActionType.CONTROL_SYSTEM, "🔋",
            subActions = listOf(
                FlowAction("battery_on", "Activar", "Batería", FlowActionType.CONTROL_SYSTEM, "🔋",
                    params = mapOf("action" to "battery_saver_on")),
                FlowAction("battery_off", "Desactivar", "Batería", FlowActionType.CONTROL_SYSTEM, "🔌",
                    params = mapOf("action" to "battery_saver_off"))
            )),
        FlowAction("screen_timeout", "Tiempo de Pantalla", "Sistema", FlowActionType.CONTROL_SYSTEM, "⏱️",
            subActions = listOf(
                FlowAction("screen_15s", "15 segundos", "Pantalla", FlowActionType.CONTROL_SYSTEM, "⏱️",
                    params = mapOf("timeout" to "15000")),
                FlowAction("screen_30s", "30 segundos", "Pantalla", FlowActionType.CONTROL_SYSTEM, "⏱️",
                    params = mapOf("timeout" to "30000")),
                FlowAction("screen_1m", "1 minuto", "Pantalla", FlowActionType.CONTROL_SYSTEM, "⏱️",
                    params = mapOf("timeout" to "60000")),
                FlowAction("screen_5m", "5 minutos", "Pantalla", FlowActionType.CONTROL_SYSTEM, "⏱️",
                    params = mapOf("timeout" to "300000"))
            )),
        FlowAction("nfc_control", "Control NFC", "Sistema", FlowActionType.CONTROL_SYSTEM, "📳"),
        FlowAction("gps_control", "Control GPS", "Sistema", FlowActionType.CONTROL_SYSTEM, "🗺️",
            subActions = listOf(
                FlowAction("gps_on", "Activar", "GPS", FlowActionType.CONTROL_SYSTEM, "🗺️",
                    params = mapOf("action" to "gps_on")),
                FlowAction("gps_off", "Desactivar", "GPS", FlowActionType.CONTROL_SYSTEM, "📍",
                    params = mapOf("action" to "gps_off"))
            )),
        FlowAction("mobile_data", "Datos Móviles", "Sistema", FlowActionType.CONTROL_SYSTEM, "📊",
            subActions = listOf(
                FlowAction("data_on", "Activar", "Datos", FlowActionType.CONTROL_SYSTEM, "📊",
                    params = mapOf("action" to "mobile_data_on")),
                FlowAction("data_off", "Desactivar", "Datos", FlowActionType.CONTROL_SYSTEM, "📵",
                    params = mapOf("action" to "mobile_data_off"))
            ))
    )

    private fun getSmartHomeActions(ctx: Context): List<FlowAction> = listOf(
        FlowAction("lights_control", "Control de Luces", "Domótica", FlowActionType.AUTOMATION, "💡",
            subActions = listOf(
                FlowAction("lights_on", "Encender Luces", "Luces", FlowActionType.AUTOMATION, "💡"),
                FlowAction("lights_off", "Apagar Luces", "Luces", FlowActionType.AUTOMATION, "🌙"),
                FlowAction("lights_dim", "Atenuar Luces", "Luces", FlowActionType.AUTOMATION, "🔅")
            )),
        FlowAction("thermostat", "Termostato", "Domótica", FlowActionType.AUTOMATION, "🌡️",
            subActions = listOf(
                FlowAction("temp_up", "Aumentar Temperatura", "Temp", FlowActionType.AUTOMATION, "🔥"),
                FlowAction("temp_down", "Disminuir Temperatura", "Temp", FlowActionType.AUTOMATION, "❄️")
            )),
        FlowAction("door_lock", "Cerraduras", "Domótica", FlowActionType.AUTOMATION, "🔒",
            subActions = listOf(
                FlowAction("door_lock", "Cerrar Puerta", "Puerta", FlowActionType.AUTOMATION, "🔒"),
                FlowAction("door_unlock", "Abrir Puerta", "Puerta", FlowActionType.AUTOMATION, "🔓")
            )),
        FlowAction("security_system", "Sistema de Seguridad", "Domótica", FlowActionType.AUTOMATION, "🛡️",
            subActions = listOf(
                FlowAction("security_arm", "Armar", "Seguridad", FlowActionType.AUTOMATION, "🛡️"),
                FlowAction("security_disarm", "Desarmar", "Seguridad", FlowActionType.AUTOMATION, "✓")
            ))
    )

    private fun getProductivityActions(ctx: Context): List<FlowAction> = listOf(
        FlowAction("calendar_event", "Crear Evento", "Productividad", FlowActionType.AUTOMATION, "📅"),
        FlowAction("reminder_set", "Establecer Recordatorio", "Productividad", FlowActionType.ALARM_TIMER, "⏰"),
        FlowAction("note_create", "Crear Nota", "Productividad", FlowActionType.AUTOMATION, "📝"),
        FlowAction("todo_add", "Añadir a Lista", "Productividad", FlowActionType.AUTOMATION, "✓"),
        FlowAction("document_open", "Abrir Documento", "Productividad", FlowActionType.OPEN_APP, "📄"),
        FlowAction("spreadsheet_open", "Abrir Hoja de Cálculo", "Productividad", FlowActionType.OPEN_APP, "📊"),
        FlowAction("presentation_open", "Abrir Presentación", "Productividad", FlowActionType.OPEN_APP, "📈")
    )

    private fun getEmergencyActions(ctx: Context): List<FlowAction> = listOf(
        FlowAction("emergency_call", "Llamada de Emergencia", "Emergencia", FlowActionType.CALL, "🚨"),
        FlowAction("emergency_alert", "Enviar Alerta", "Emergencia", FlowActionType.SEND_MESSAGE, "⚠️"),
        FlowAction("location_share", "Compartir Ubicación", "Emergencia", FlowActionType.SEND_MESSAGE, "📍"),
        FlowAction("sos_message", "Mensaje SOS", "Emergencia", FlowActionType.SEND_MESSAGE, "🆘")
    )

    private fun getAutomationActions(ctx: Context): List<FlowAction> = listOf(
        FlowAction("if_then", "Si... Entonces", "Automatización", FlowActionType.CONDITIONAL, "🔄",
            subActions = listOf(
                FlowAction("if_wifi", "Si WiFi está...", "Condición", FlowActionType.CONDITIONAL, "📶"),
                FlowAction("if_battery", "Si Batería es...", "Condición", FlowActionType.CONDITIONAL, "🔋"),
                FlowAction("if_time", "Si la Hora es...", "Condición", FlowActionType.CONDITIONAL, "🕐")
            )),
        FlowAction("repeat_action", "Repetir Acción", "Automatización", FlowActionType.AUTOMATION, "🔁"),
        FlowAction("delay_action", "Retrasar Acción", "Automatización", FlowActionType.AUTOMATION, "⏳"),
        FlowAction("chain_actions", "Encadenar Acciones", "Automatización", FlowActionType.AUTOMATION, "🔗")
    )

    private fun getSavedMacros(): List<FlowAction> = macros.values.map { macro ->
        FlowAction(
            id = "macro_${macro.id}",
            name = macro.name,
            category = "Macro",
            type = FlowActionType.MACRO,
            icon = "⭐",
            params = mapOf("macro_id" to macro.id)
        )
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    private suspend fun getContactsList(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Pair<String, String>>()
        try {
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val phone = it.getString(1)
                    if (name != null && phone != null) {
                        contacts.add(name to phone)
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail
        }
        contacts.take(100)
    }

    private suspend fun getMusicApplications(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val musicApps = listOf(
            "Spotify" to "com.spotify.music",
            "YouTube Music" to "com.google.android.apps.youtube.music",
            "Apple Music" to "com.apple.android.music",
            "Amazon Music" to "com.amazon.mp3",
            "SoundCloud" to "com.soundcloud.android",
            "Deezer" to "deezer.android.app",
            "Tidal" to "com.aspiro.tidal",
            "Bandcamp" to "com.bandcamp.android"
        )
        musicApps.filter { (_, pkg) ->
            try {
                ctx.packageManager.getApplicationInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // ── Guardar y cargar macros ────────────────────────────────────────────────

    fun saveMacro(macro: FlowMacro) {
        macros[macro.id] = macro
    }

    fun getMacro(id: String): FlowMacro? = macros[id]

    fun getAllMacros(): List<FlowMacro> = macros.values.toList()

    fun deleteMacro(id: String) {
        macros.remove(id)
    }
}
