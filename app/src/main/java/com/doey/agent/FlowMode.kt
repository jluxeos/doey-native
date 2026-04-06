package com.doey.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sistema de Modo Flujo: Árbol de decisiones offline sin IA.
 * Permite construir comandos paso a paso seleccionando opciones predefinidas.
 */

data class FlowNode(
    val id: String,
    val label: String,
    val description: String = "",
    val options: List<FlowOption> = emptyList(),
    val action: suspend (Context, Map<String, String>) -> String = { _, _ -> "Completado" }
)

data class FlowOption(
    val id: String,
    val label: String,
    val nextNodeId: String? = null,
    val params: Map<String, String> = emptyMap()
)

object FlowModeEngine {
    private val context: Context? = null
    
    // ── Nodos principales del árbol de decisiones ──────────────────────────────
    fun getRootNodes(): List<FlowNode> = listOf(
        FlowNode(
            id = "root_open",
            label = "Abrir",
            description = "Abre aplicaciones o archivos",
            options = listOf(
                FlowOption("app_select", "Seleccionar App", "app_list"),
                FlowOption("contact_open", "Contacto", "contact_list")
            )
        ),
        FlowNode(
            id = "root_send",
            label = "Enviar",
            description = "Envía mensajes o contenido",
            options = listOf(
                FlowOption("msg_whatsapp", "WhatsApp", "contact_list", mapOf("app" to "com.whatsapp")),
                FlowOption("msg_telegram", "Telegram", "contact_list", mapOf("app" to "org.telegram.messenger")),
                FlowOption("msg_sms", "SMS", "contact_list", mapOf("app" to "sms")),
                FlowOption("msg_email", "Email", "contact_list", mapOf("app" to "email"))
            )
        ),
        FlowNode(
            id = "root_control",
            label = "Controlar",
            description = "Controla dispositivo y conectividad",
            options = listOf(
                FlowOption("wifi_toggle", "WiFi", "wifi_options"),
                FlowOption("bluetooth_toggle", "Bluetooth", "bluetooth_options"),
                FlowOption("volume_control", "Volumen", "volume_options"),
                FlowOption("brightness_control", "Brillo", "brightness_options"),
                FlowOption("airplane_toggle", "Modo Avión", "airplane_options")
            )
        ),
        FlowNode(
            id = "root_music",
            label = "Música",
            description = "Controla reproducción de música",
            options = listOf(
                FlowOption("music_play", "Reproducir", "music_apps"),
                FlowOption("music_pause", "Pausar", "music_apps"),
                FlowOption("music_next", "Siguiente", "music_apps"),
                FlowOption("music_prev", "Anterior", "music_apps")
            )
        ),
        FlowNode(
            id = "root_call",
            label = "Llamar",
            description = "Realiza llamadas telefónicas",
            options = listOf(
                FlowOption("call_contact", "Contacto", "contact_list", mapOf("action" to "call"))
            )
        ),
        FlowNode(
            id = "root_schedule",
            label = "Agendar",
            description = "Crea alarmas y recordatorios",
            options = listOf(
                FlowOption("alarm_set", "Alarma", "alarm_time"),
                FlowOption("timer_set", "Temporizador", "timer_duration"),
                FlowOption("reminder_set", "Recordatorio", "reminder_time")
            )
        ),
        FlowNode(
            id = "root_navigate",
            label = "Navegar",
            description = "Abre mapas y navegación",
            options = listOf(
                FlowOption("maps_open", "Google Maps", "address_input"),
                FlowOption("waze_open", "Waze", "address_input")
            )
        )
    )

    // ── Nodos secundarios ──────────────────────────────────────────────────────
    suspend fun getNodeById(ctx: Context, nodeId: String): FlowNode? = withContext(Dispatchers.Default) {
        when (nodeId) {
            "app_list" -> getAppListNode(ctx)
            "contact_list" -> getContactListNode(ctx)
            "wifi_options" -> getWiFiOptionsNode()
            "bluetooth_options" -> getBluetoothOptionsNode()
            "volume_options" -> getVolumeOptionsNode()
            "brightness_options" -> getBrightnessOptionsNode()
            "airplane_options" -> getAirplaneOptionsNode()
            "music_apps" -> getMusicAppsNode(ctx)
            "alarm_time" -> getAlarmTimeNode()
            "timer_duration" -> getTimerDurationNode()
            "reminder_time" -> getReminderTimeNode()
            "address_input" -> getAddressInputNode()
            else -> null
        }
    }

    private suspend fun getAppListNode(ctx: Context): FlowNode {
        val apps = getInstalledApps(ctx)
        return FlowNode(
            id = "app_list",
            label = "Selecciona una App",
            options = apps.map { (name, pkg) ->
                FlowOption(
                    id = "app_$pkg",
                    label = name,
                    nextNodeId = null,
                    params = mapOf("package" to pkg)
                )
            }
        )
    }

    private suspend fun getContactListNode(ctx: Context): FlowNode {
        val contacts = getContactsList(ctx)
        return FlowNode(
            id = "contact_list",
            label = "Selecciona un contacto",
            options = contacts.map { (name, phone) ->
                FlowOption(
                    id = "contact_$phone",
                    label = name,
                    nextNodeId = "message_input",
                    params = mapOf("contact" to name, "phone" to phone)
                )
            }
        )
    }

    private fun getWiFiOptionsNode() = FlowNode(
        id = "wifi_options",
        label = "WiFi",
        options = listOf(
            FlowOption("wifi_on", "Activar", params = mapOf("action" to "wifi_on")),
            FlowOption("wifi_off", "Desactivar", params = mapOf("action" to "wifi_off")),
            FlowOption("wifi_list", "Ver redes", params = mapOf("action" to "wifi_list"))
        )
    )

    private fun getBluetoothOptionsNode() = FlowNode(
        id = "bluetooth_options",
        label = "Bluetooth",
        options = listOf(
            FlowOption("bt_on", "Activar", params = mapOf("action" to "bluetooth_on")),
            FlowOption("bt_off", "Desactivar", params = mapOf("action" to "bluetooth_off")),
            FlowOption("bt_devices", "Dispositivos", params = mapOf("action" to "bluetooth_devices"))
        )
    )

    private fun getVolumeOptionsNode() = FlowNode(
        id = "volume_options",
        label = "Volumen",
        options = listOf(
            FlowOption("vol_high", "Alto", params = mapOf("level" to "15")),
            FlowOption("vol_medium", "Medio", params = mapOf("level" to "8")),
            FlowOption("vol_low", "Bajo", params = mapOf("level" to "3")),
            FlowOption("vol_mute", "Silencio", params = mapOf("level" to "0"))
        )
    )

    private fun getBrightnessOptionsNode() = FlowNode(
        id = "brightness_options",
        label = "Brillo",
        options = listOf(
            FlowOption("bright_high", "Alto", params = mapOf("level" to "255")),
            FlowOption("bright_medium", "Medio", params = mapOf("level" to "128")),
            FlowOption("bright_low", "Bajo", params = mapOf("level" to "50")),
            FlowOption("bright_auto", "Automático", params = mapOf("level" to "auto"))
        )
    )

    private fun getAirplaneOptionsNode() = FlowNode(
        id = "airplane_options",
        label = "Modo Avión",
        options = listOf(
            FlowOption("airplane_on", "Activar", params = mapOf("action" to "airplane_on")),
            FlowOption("airplane_off", "Desactivar", params = mapOf("action" to "airplane_off"))
        )
    )

    private suspend fun getMusicAppsNode(ctx: Context): FlowNode {
        val musicApps = getMusicApplications(ctx)
        return FlowNode(
            id = "music_apps",
            label = "Selecciona app de música",
            options = musicApps.map { (name, pkg) ->
                FlowOption(
                    id = "music_$pkg",
                    label = name,
                    params = mapOf("package" to pkg)
                )
            }
        )
    }

    private fun getAlarmTimeNode() = FlowNode(
        id = "alarm_time",
        label = "Hora de alarma",
        options = (0..23).flatMap { hour ->
            listOf(0, 15, 30, 45).map { minute ->
                FlowOption(
                    id = "time_${hour}_${minute}",
                    label = String.format("%02d:%02d", hour, minute),
                    params = mapOf("hour" to hour.toString(), "minute" to minute.toString())
                )
            }
        }
    )

    private fun getTimerDurationNode() = FlowNode(
        id = "timer_duration",
        label = "Duración del temporizador",
        options = listOf(
            FlowOption("timer_1", "1 minuto", params = mapOf("minutes" to "1")),
            FlowOption("timer_5", "5 minutos", params = mapOf("minutes" to "5")),
            FlowOption("timer_10", "10 minutos", params = mapOf("minutes" to "10")),
            FlowOption("timer_15", "15 minutos", params = mapOf("minutes" to "15")),
            FlowOption("timer_30", "30 minutos", params = mapOf("minutes" to "30")),
            FlowOption("timer_60", "1 hora", params = mapOf("minutes" to "60"))
        )
    )

    private fun getReminderTimeNode() = FlowNode(
        id = "reminder_time",
        label = "Hora del recordatorio",
        options = (0..23).flatMap { hour ->
            listOf(0, 30).map { minute ->
                FlowOption(
                    id = "reminder_${hour}_${minute}",
                    label = String.format("%02d:%02d", hour, minute),
                    params = mapOf("hour" to hour.toString(), "minute" to minute.toString())
                )
            }
        }
    )

    private fun getAddressInputNode() = FlowNode(
        id = "address_input",
        label = "Ingresa dirección",
        options = emptyList()
    )

    // ── Utilidades ─────────────────────────────────────────────────────────────
    private suspend fun getInstalledApps(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .map { app ->
                pm.getApplicationLabel(app).toString() to app.packageName
            }
        apps.take(50) // Limitar a 50 apps más relevantes
    }

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
        contacts.take(30) // Limitar a 30 contactos
    }

    private suspend fun getMusicApplications(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val musicApps = listOf(
            "Spotify" to "com.spotify.music",
            "YouTube Music" to "com.google.android.apps.youtube.music",
            "Apple Music" to "com.apple.android.music",
            "Amazon Music" to "com.amazon.mp3",
            "SoundCloud" to "com.soundcloud.android"
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
}
