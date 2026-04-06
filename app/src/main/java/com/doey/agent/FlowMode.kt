package com.doey.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.ContactsContract
import android.provider.Settings
import com.doey.DoeyApplication
import com.doey.tools.*
import com.doey.ui.parseMemoryEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Modelos ────────────────────────────────────────────────────────────────────

data class FlowNode(
    val id: String,
    val label: String,
    val description: String = "",
    val options: List<FlowOption> = emptyList(),
    val isVariableSelector: Boolean = false
)

data class FlowOption(
    val id: String,
    val label: String,
    val icon: String = "",
    val nextNodeId: String? = null,
    val command: FlowCommand? = null,
    val params: Map<String, String> = emptyMap()
)

data class FlowCommand(
    val toolName: String,
    val arguments: Map<String, Any?>
)

// ── Motor principal ────────────────────────────────────────────────────────────

object FlowModeEngine {

    // ── Opciones raíz del carrusel ─────────────────────────────────────────────

    fun getRootOptions(): List<FlowOption> = listOf(
        FlowOption("r_abrir",     "Abrir",        "📱", nextNodeId = "abrir"),
        FlowOption("r_enviar",    "Enviar",        "✉️", nextNodeId = "enviar"),
        FlowOption("r_llamar",    "Llamar",        "📞", nextNodeId = "llamar"),
        FlowOption("r_musica",    "Música",        "🎵", nextNodeId = "musica"),
        FlowOption("r_wifi",      "WiFi",          "📶", nextNodeId = "wifi"),
        FlowOption("r_bluetooth", "Bluetooth",     "🔷", nextNodeId = "bluetooth"),
        FlowOption("r_volumen",   "Volumen",       "🔊", nextNodeId = "volumen"),
        FlowOption("r_brillo",    "Brillo",        "☀️", nextNodeId = "brillo"),
        FlowOption("r_alarma",    "Alarma",        "⏰", nextNodeId = "alarma"),
        FlowOption("r_timer",     "Temporizador",  "⏱️", nextNodeId = "timer"),
        FlowOption("r_linterna",  "Linterna",      "🔦", nextNodeId = "linterna"),
        FlowOption("r_navegar",   "Navegar",       "🗺️", nextNodeId = "navegar"),
        FlowOption("r_sistema",   "Sistema",       "⚙️", nextNodeId = "sistema"),
        FlowOption("r_apps",      "Mis Apps",      "🗂️", nextNodeId = "app_list"),
        FlowOption("r_contactos", "Contactos",     "👤", nextNodeId = "contactos_acciones"),
    )

    // ── Árbol estático de nodos ────────────────────────────────────────────────

    private fun getStaticNode(nodeId: String): FlowNode? = when (nodeId) {

        "abrir" -> FlowNode("abrir", "¿Qué abrir?", options = listOf(
            FlowOption("abrir_app",       "Otra app",       "📱", nextNodeId = "app_list"),
            FlowOption("abrir_wa",        "WhatsApp",       "💬", command = launchPkg("com.whatsapp")),
            FlowOption("abrir_telegram",  "Telegram",       "✈️", command = launchPkg("org.telegram.messenger")),
            FlowOption("abrir_chrome",    "Chrome",         "🌐", command = launchPkg("com.android.chrome")),
            FlowOption("abrir_youtube",   "YouTube",        "▶️", command = launchPkg("com.google.android.youtube")),
            FlowOption("abrir_maps",      "Maps",           "🗺️", command = launchPkg("com.google.android.apps.maps")),
            FlowOption("abrir_camara",    "Cámara",         "📷", command = FlowCommand("intent", mapOf(
                "action" to "android.media.action.IMAGE_CAPTURE"))),
            FlowOption("abrir_galeria",   "Galería",        "🖼️", command = FlowCommand("intent", mapOf(
                "action" to Intent.ACTION_VIEW, "type" to "image/*"))),
            FlowOption("abrir_configs",   "Configuración",  "⚙️", command = FlowCommand("intent", mapOf(
                "action" to Settings.ACTION_SETTINGS))),
            FlowOption("abrir_calc",      "Calculadora",    "🧮", command = launchPkg("com.google.android.calculator")),
            FlowOption("abrir_reloj",     "Reloj",          "🕐", command = launchPkg("com.google.android.deskclock")),
            FlowOption("abrir_notas",     "Keep Notes",     "📝", command = launchPkg("com.google.android.keep")),
            FlowOption("abrir_gmail",     "Gmail",          "📧", command = launchPkg("com.google.android.gm")),
            FlowOption("abrir_spotify",   "Spotify",        "🎵", command = launchPkg("com.spotify.music")),
            FlowOption("abrir_netflix",   "Netflix",        "📺", command = launchPkg("com.netflix.mediaclient")),
            FlowOption("abrir_drive",     "Google Drive",   "📁", command = launchPkg("com.google.android.apps.docs")),
            FlowOption("abrir_photos",    "Google Fotos",   "🖼️", command = launchPkg("com.google.android.apps.photos")),
            FlowOption("abrir_play",      "Play Store",     "🏪", command = launchPkg("com.android.vending")),
            FlowOption("abrir_fb",        "Facebook",       "📘", command = launchPkg("com.facebook.katana")),
            FlowOption("abrir_instagram", "Instagram",      "📸", command = launchPkg("com.instagram.android")),
            FlowOption("abrir_tiktok",    "TikTok",         "🎶", command = launchPkg("com.zhiliaoapp.musically")),
        ))

        "enviar" -> FlowNode("enviar", "¿Enviar por?", options = listOf(
            FlowOption("env_wa",       "WhatsApp",  "💬", nextNodeId = "contact_list", params = mapOf("app" to "whatsapp")),
            FlowOption("env_sms",      "SMS",       "📱", nextNodeId = "contact_list", params = mapOf("app" to "sms")),
            FlowOption("env_telegram", "Telegram",  "✈️", nextNodeId = "contact_list", params = mapOf("app" to "telegram")),
            FlowOption("env_email",    "Email",     "📧", nextNodeId = "contact_list", params = mapOf("app" to "email")),
        ))

        "llamar" -> FlowNode("llamar", "¿A quién llamar?", options = listOf(
            FlowOption("llamar_contacto",   "Seleccionar contacto", "👤", nextNodeId = "contact_list", params = mapOf("app" to "call")),
            FlowOption("llamar_emergencia", "Emergencias (911)",    "🚨", command = dialNumber("911")),
            FlowOption("llamar_ambulancia", "Ambulancia (065)",     "🚑", command = dialNumber("065")),
            FlowOption("llamar_policia",    "Policía (060)",        "👮", command = dialNumber("060")),
            FlowOption("llamar_bomberos",   "Bomberos (068)",       "🚒", command = dialNumber("068")),
        ))

        "musica" -> FlowNode("musica", "Control de música", options = listOf(
            FlowOption("mus_play",    "Reproducir/Pausar", "▶️", command = mediaCmd("togglepause")),
            FlowOption("mus_next",    "Siguiente",         "⏭️", command = mediaCmd("next")),
            FlowOption("mus_prev",    "Anterior",          "⏮️", command = mediaCmd("previous")),
            FlowOption("mus_stop",    "Detener",           "⏹️", command = mediaCmd("stop")),
            FlowOption("mus_spotify", "Abrir Spotify",     "🎵", command = launchPkg("com.spotify.music")),
            FlowOption("mus_yt",      "Abrir YouTube",     "▶️", command = launchPkg("com.google.android.youtube")),
        ))

        "wifi" -> FlowNode("wifi", "Configuración WiFi", options = listOf(
            FlowOption("wifi_on",  "Activar WiFi",    "📶", command = FlowCommand("device", mapOf("action" to "enable_wifi"))),
            FlowOption("wifi_off", "Desactivar WiFi", "📵", command = FlowCommand("device", mapOf("action" to "disable_wifi"))),
            FlowOption("wifi_cfg", "Ajustes de WiFi", "⚙️", command = openSettings(Settings.ACTION_WIFI_SETTINGS)),
        ))

        "bluetooth" -> FlowNode("bluetooth", "Bluetooth", options = listOf(
            FlowOption("bt_on",  "Activar Bluetooth",    "🔷", command = FlowCommand("device", mapOf("action" to "enable_bluetooth"))),
            FlowOption("bt_off", "Desactivar Bluetooth", "🔕", command = FlowCommand("device", mapOf("action" to "disable_bluetooth"))),
            FlowOption("bt_cfg", "Ajustes Bluetooth",    "⚙️", command = openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)),
        ))

        "volumen" -> FlowNode("volumen", "Control de volumen", options = listOf(
            FlowOption("vol_max",  "Volumen máximo",   "🔊", command = setVolume(100)),
            FlowOption("vol_alto", "Volumen alto 75%", "🔊", command = setVolume(75)),
            FlowOption("vol_med",  "Volumen medio 50%","🔉", command = setVolume(50)),
            FlowOption("vol_bajo", "Volumen bajo 25%", "🔈", command = setVolume(25)),
            FlowOption("vol_mute", "Silenciar",        "🔇", command = setVolume(0)),
            FlowOption("vol_vibra","Modo vibración",   "📳", command = setRinger("vibrate")),
            FlowOption("vol_ring", "Modo sonido",      "🔔", command = setRinger("normal")),
            FlowOption("vol_dnd",  "No molestar",      "🤫", command = setRinger("silent")),
        ))

        "brillo" -> FlowNode("brillo", "Control de brillo", options = listOf(
            FlowOption("bri_max",  "Brillo máximo", "☀️", command = setBrightness(255)),
            FlowOption("bri_alto", "Brillo alto",   "🌤️", command = setBrightness(180)),
            FlowOption("bri_med",  "Brillo medio",  "⛅", command = setBrightness(120)),
            FlowOption("bri_bajo", "Brillo bajo",   "🌑", command = setBrightness(50)),
            FlowOption("bri_auto", "Automático",    "✨", command = FlowCommand("device", mapOf("action" to "set_brightness_auto"))),
        ))

        "alarma" -> FlowNode("alarma", "¿Cuándo?", options = listOf(
            FlowOption("alarm_5m",  "En 5 minutos",  "⏰", command = alarmIn(5)),
            FlowOption("alarm_10m", "En 10 minutos", "⏰", command = alarmIn(10)),
            FlowOption("alarm_15m", "En 15 minutos", "⏰", command = alarmIn(15)),
            FlowOption("alarm_30m", "En 30 minutos", "⏰", command = alarmIn(30)),
            FlowOption("alarm_1h",  "En 1 hora",     "⏰", command = alarmIn(60)),
            FlowOption("alarm_2h",  "En 2 horas",    "⏰", command = alarmIn(120)),
            FlowOption("alarm_app", "Abrir Reloj",   "🕐", command = openSettings("android.intent.action.SET_ALARM")),
        ))

        "timer" -> FlowNode("timer", "¿Cuánto tiempo?", options = listOf(
            FlowOption("t_1m",  "1 minuto",   "⏱️", command = timerFor(60)),
            FlowOption("t_2m",  "2 minutos",  "⏱️", command = timerFor(120)),
            FlowOption("t_3m",  "3 minutos",  "⏱️", command = timerFor(180)),
            FlowOption("t_5m",  "5 minutos",  "⏱️", command = timerFor(300)),
            FlowOption("t_10m", "10 minutos", "⏱️", command = timerFor(600)),
            FlowOption("t_15m", "15 minutos", "⏱️", command = timerFor(900)),
            FlowOption("t_20m", "20 minutos", "⏱️", command = timerFor(1200)),
            FlowOption("t_30m", "30 minutos", "⏱️", command = timerFor(1800)),
            FlowOption("t_45m", "45 minutos", "⏱️", command = timerFor(2700)),
            FlowOption("t_60m", "60 minutos", "⏱️", command = timerFor(3600)),
        ))

        "linterna" -> FlowNode("linterna", "Linterna", options = listOf(
            FlowOption("lint_on",  "Encender", "🔦", command = FlowCommand("device", mapOf("action" to "flashlight_on"))),
            FlowOption("lint_off", "Apagar",   "🌑", command = FlowCommand("device", mapOf("action" to "flashlight_off"))),
        ))

        "navegar" -> FlowNode("navegar", "¿A dónde?", options = listOf(
            FlowOption("nav_casa",      "A casa",           "🏠", command = navTo("{{casa}}")),
            FlowOption("nav_trabajo",   "Al trabajo",       "🏢", command = navTo("{{trabajo}}")),
            FlowOption("nav_hospital",  "Hospital cercano", "🏥", command = navSearch("hospital")),
            FlowOption("nav_gasolina",  "Gasolinera",       "⛽", command = navSearch("gasolinera")),
            FlowOption("nav_farmacia",  "Farmacia",         "💊", command = navSearch("farmacia")),
            FlowOption("nav_super",     "Supermercado",     "🛒", command = navSearch("supermercado")),
            FlowOption("nav_restaurante","Restaurante",     "🍽️", command = navSearch("restaurante")),
            FlowOption("nav_maps",      "Abrir Maps",       "🗺️", command = launchPkg("com.google.android.apps.maps")),
        ))

        "sistema" -> FlowNode("sistema", "Sistema", options = listOf(
            FlowOption("sys_avion",   "Modo avión",         "✈️", command = openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)),
            FlowOption("sys_bateria", "Ahorro batería",     "🔋", command = openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)),
            FlowOption("sys_nfc",     "Ajustes NFC",        "📡", command = openSettings(Settings.ACTION_NFC_SETTINGS)),
            FlowOption("sys_datos",   "Datos móviles",      "📶", command = openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)),
            FlowOption("sys_apps",    "Gestor de apps",     "📦", command = openSettings(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)),
            FlowOption("sys_almacen", "Almacenamiento",     "💾", command = openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)),
            FlowOption("sys_accesib", "Accesibilidad",      "♿", command = openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)),
            FlowOption("sys_idioma",  "Idioma y teclado",   "🌐", command = openSettings(Settings.ACTION_LOCALE_SETTINGS)),
            FlowOption("sys_notif",   "Notificaciones",     "🔔", command = openSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS)),
            FlowOption("sys_fecha",   "Fecha y hora",       "📅", command = openSettings(Settings.ACTION_DATE_SETTINGS)),
            FlowOption("sys_red",     "Ajustes de red",     "🌐", command = openSettings(Settings.ACTION_WIRELESS_SETTINGS)),
            FlowOption("sys_display", "Pantalla",           "📺", command = openSettings(Settings.ACTION_DISPLAY_SETTINGS)),
            FlowOption("sys_sonido",  "Sonido",             "🔊", command = openSettings(Settings.ACTION_SOUND_SETTINGS)),
            FlowOption("sys_segur",   "Seguridad",          "🔒", command = openSettings(Settings.ACTION_SECURITY_SETTINGS)),
        ))

        "contactos_acciones" -> FlowNode("contactos_acciones", "¿Qué hacer?", options = listOf(
            FlowOption("cta_llamar", "Llamar",       "📞", nextNodeId = "contact_list", params = mapOf("app" to "call")),
            FlowOption("cta_wa",     "WhatsApp",     "💬", nextNodeId = "contact_list", params = mapOf("app" to "whatsapp")),
            FlowOption("cta_sms",    "Enviar SMS",   "📱", nextNodeId = "contact_list", params = mapOf("app" to "sms")),
            FlowOption("cta_email",  "Enviar email", "📧", nextNodeId = "contact_list", params = mapOf("app" to "email")),
            FlowOption("cta_telg",   "Telegram",     "✈️", nextNodeId = "contact_list", params = mapOf("app" to "telegram")),
        ))

        else -> null
    }

    // ── Helpers constructores de FlowCommand ──────────────────────────────────

    private fun launchPkg(pkg: String) =
        FlowCommand("intent", mapOf("action" to Intent.ACTION_MAIN, "package" to pkg))

    private fun dialNumber(number: String) =
        FlowCommand("intent", mapOf("action" to Intent.ACTION_DIAL, "uri" to "tel:$number"))

    private fun mediaCmd(cmd: String) =
        FlowCommand("intent", mapOf(
            "action" to "com.android.music.musicservicecommand",
            "extras" to listOf(mapOf("key" to "command", "value" to cmd))
        ))

    private fun openSettings(action: String) =
        FlowCommand("intent", mapOf("action" to action))

    private fun setVolume(level: Int) =
        FlowCommand("device", mapOf("action" to "set_volume", "volume" to level))

    private fun setRinger(mode: String) =
        FlowCommand("device", mapOf("action" to "set_ringer_mode", "mode" to mode))

    private fun setBrightness(level: Int) =
        FlowCommand("device", mapOf("action" to "set_brightness", "brightness" to level))

    private fun alarmIn(minutes: Int) =
        FlowCommand("set_alarm", mapOf("label" to "Alarma Doey", "minutes" to minutes))

    private fun timerFor(seconds: Int) =
        FlowCommand("set_timer", mapOf("label" to "Temporizador Doey", "seconds" to seconds))

    private fun navTo(dest: String) =
        FlowCommand("intent", mapOf("action" to Intent.ACTION_VIEW, "uri" to "google.navigation:q=$dest"))

    private fun navSearch(query: String) =
        FlowCommand("intent", mapOf("action" to Intent.ACTION_VIEW, "uri" to "geo:0,0?q=$query"))

    // ── Resolución de variables {{...}} ───────────────────────────────────────

    private suspend fun resolveVariables(args: Map<String, Any?>, context: Context): Map<String, Any?> {
        val settings = (context.applicationContext as DoeyApplication).settingsStore
        val memories = parseMemoryEntries(settings.getPersonalMemory())

        fun resolveString(s: String): String {
            var r = s
            memories.forEach { entry -> r = r.replace("{{${entry.variable}}}", entry.definition) }
            return r
        }

        @Suppress("UNCHECKED_CAST")
        return args.mapValues { (_, value) ->
            when (value) {
                is String    -> resolveString(value)
                is Map<*, *> -> resolveVariables(value as Map<String, Any?>, context)
                else         -> value
            }
        }
    }

    // ── Ejecución ─────────────────────────────────────────────────────────────

    suspend fun executeCommand(context: Context, command: FlowCommand): ToolResult =
        withContext(Dispatchers.Main) {
            val registry = ToolRegistry().apply {
                register(IntentTool())
                register(SmsTool())
                register(BeepTool())
                register(DateTimeTool())
                register(DeviceTool())
                register(TTSTool())
                register(AccessibilityTool())
                register(AppSearchTool())
                register(FileStorageTool())
                register(AlarmTool())
                register(TimerTool())
            }
            val resolvedArgs = resolveVariables(command.arguments, context)
            try {
                registry.execute(command.toolName, resolvedArgs)
            } catch (e: Exception) {
                errorResult("Error: ${e.message}")
            }
        }

    // ── Resolución dinámica de nodos ──────────────────────────────────────────

    suspend fun getNodeById(
        ctx: Context,
        nodeId: String,
        params: Map<String, String> = emptyMap()
    ): FlowNode? = withContext(Dispatchers.Default) {
        when (nodeId) {
            "app_list"     -> getAppListNode(ctx)
            "contact_list" -> getContactListNode(ctx, params)
            else           -> getStaticNode(nodeId)
        }
    }

    private suspend fun getAppListNode(ctx: Context): FlowNode {
        val apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            pm.getInstalledApplications(0)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { pm.getApplicationLabel(it).toString() to it.packageName }
                .sortedBy { it.first }
                .take(80)
        }
        return FlowNode(
            id = "app_list",
            label = "Selecciona una app",
            options = apps.map { (name, pkg) ->
                FlowOption(id = "app_$pkg", label = name, icon = "📱", command = launchPkg(pkg))
            }
        )
    }

    private suspend fun getContactListNode(ctx: Context, params: Map<String, String>): FlowNode {
        val contacts = withContext(Dispatchers.IO) {
            val list = mutableListOf<Pair<String, String>>()
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
            cursor?.use {
                val nameIdx  = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) list.add(it.getString(nameIdx) to it.getString(phoneIdx))
            }
            list.distinctBy { it.second }.sortedBy { it.first }.take(80)
        }
        val targetApp = params["app"] ?: "call"
        return FlowNode(
            id = "contact_list",
            label = "Selecciona un contacto",
            options = contacts.map { (name, phone) ->
                val clean = phone.replace("+", "").replace(" ", "").replace("-", "")
                val command = when (targetApp) {
                    "whatsapp" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_VIEW,
                        "uri"    to "https://wa.me/$clean",
                        "package" to "com.whatsapp"))
                    "sms" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_SENDTO, "uri" to "smsto:$phone"))
                    "email" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_SENDTO, "uri" to "mailto:$phone"))
                    "telegram" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_VIEW,
                        "uri"    to "tg://resolve?phone=$clean",
                        "package" to "org.telegram.messenger"))
                    else -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_DIAL, "uri" to "tel:$phone"))
                }
                FlowOption(id = "contact_$clean", label = name, icon = "👤", command = command)
            }
        )
    }
}
