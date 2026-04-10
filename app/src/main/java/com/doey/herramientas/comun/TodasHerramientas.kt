package com.doey.herramientas.comun

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.BatteryManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import com.doey.AplicacionDoey
import com.doey.agente.SkillLoader
import com.doey.ui.comun.MemoryEntry
import com.doey.ui.comun.parseMemoryEntries
import com.doey.ui.comun.toJson
import com.doey.servicios.basico.DoeyAccessibilityService
import com.doey.servicios.comun.DoeyTTSEngine
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

// ── IntentTool ────────────────────────────────────────────────────────────────

class IntentTool : Tool {
    private val ctx get() = AplicacionDoey.instance
    override fun name()        = "intent"
    override fun description() = "Control Android apps via Intents. Opens apps, starts navigation, makes calls, etc."
    override fun systemHint()  = "General-purpose app launcher. Prefer specific tools when available; use intent as fallback."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"  to mapOf("type" to "string", "description" to "Intent action, e.g. android.intent.action.VIEW"),
            "uri"     to mapOf("type" to "string", "description" to "URI for the intent"),
            "package" to mapOf("type" to "string", "description" to "Optional target package"),
            "extras"  to mapOf("type" to "array",
                "items" to mapOf("type" to "object",
                    "properties" to mapOf(
                        "key"   to mapOf("type" to "string"),
                        "value" to mapOf("type" to "string")
                    ), "required" to listOf("key", "value")))
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")
        val uri    = args["uri"]     as? String
        val pkg    = args["package"] as? String
        return withContext(Dispatchers.Main) {
            try {
                val intent: Intent = if (action == Intent.ACTION_MAIN && !pkg.isNullOrBlank() && uri.isNullOrBlank()) {
                    ctx.packageManager.getLaunchIntentForPackage(pkg)
                        ?: return@withContext errorResult("App not found: $pkg")
                } else {
                    Intent(action).apply {
                        if (!uri.isNullOrBlank())  data = Uri.parse(uri)
                        if (!pkg.isNullOrBlank())  setPackage(pkg)
                    }
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                @Suppress("UNCHECKED_CAST")
                (args["extras"] as? List<Map<String, String>>)?.forEach { e ->
                    intent.putExtra(e["key"], e["value"])
                }
                ctx.startActivity(intent)
                successResult("Intent executed: $action${if (uri != null) " → $uri" else ""}")
            } catch (e: Exception) {
                errorResult("Intent failed: ${e.message}")
            }
        }
    }
}

// ── SmsTool ───────────────────────────────────────────────────────────────────

class SmsTool : Tool {
    override fun name()        = "send_sms"
    override fun description() = "Send an SMS text message directly."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "phone_number" to mapOf("type" to "string"),
            "message"      to mapOf("type" to "string")
        ),
        "required" to listOf("phone_number", "message")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val phone   = args["phone_number"] as? String ?: return@withContext errorResult("phone_number required")
        val message = args["message"]      as? String ?: return@withContext errorResult("message required")
        try {
            @Suppress("DEPRECATION")
            val sms = SmsManager.getDefault()
            if (message.length > 160) {
                sms.sendMultipartTextMessage(phone, null, sms.divideMessage(message), null, null)
            } else {
                sms.sendTextMessage(phone, null, message, null, null)
            }
            successResult("SMS sent to $phone")
        } catch (e: Exception) { errorResult("SMS failed: ${e.message}") }
    }
}

// ── BeepTool ──────────────────────────────────────────────────────────────────

class BeepTool : Tool {
    override fun name()        = "beep"
    override fun description() = "Play a short beep or tone to alert the user."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "tone"  to mapOf("type" to "string",  "enum" to listOf("default", "success", "error", "warning")),
            "count" to mapOf("type" to "integer", "description" to "Number of beeps 1-5")
        )
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val count    = (args["count"] as? Number)?.toInt()?.coerceIn(1, 5) ?: 1
        val toneType = when (args["tone"] as? String) {
            "success" -> ToneGenerator.TONE_PROP_ACK
            "error"   -> ToneGenerator.TONE_PROP_NACK
            "warning" -> ToneGenerator.TONE_SUP_ERROR
            else      -> ToneGenerator.TONE_PROP_BEEP
        }
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            repeat(count) { tg.startTone(toneType, 300); Thread.sleep(500) }
            tg.release()
            successResult("Played $count beep(s)")
        } catch (e: Exception) { errorResult("Beep failed: ${e.message}") }
    }
}

// ── DateTimeTool ──────────────────────────────────────────────────────────────

class DateTimeTool : Tool {
    override fun name()        = "datetime"
    override fun description() = "Get current date/time, convert timezones, calculate date differences."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"   to mapOf("type" to "string", "enum" to listOf("now", "convert_timezone", "diff_days")),
            "timezone" to mapOf("type" to "string"),
            "date1"    to mapOf("type" to "string"),
            "date2"    to mapOf("type" to "string")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return when (args["action"] as? String) {
            "now" -> {
                val tz      = (args["timezone"] as? String)?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
                val now     = Date()
                val iso     = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).also { it.timeZone = tz }
                val human   = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm:ss z", Locale.ENGLISH).also { it.timeZone = tz }
                successResult("Time: ${human.format(now)}\nISO: ${iso.format(now)}\nTimestamp: ${now.time}\nTimezone: ${tz.id}")
            }
            "diff_days" -> {
                val d1s = args["date1"] as? String ?: return errorResult("date1 required")
                val d2s = args["date2"] as? String ?: return errorResult("date2 required")
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val diff = kotlin.math.abs((fmt.parse(d2s)?.time ?: 0) - (fmt.parse(d1s)?.time ?: 0)) / 86_400_000
                successResult("Difference: $diff days")
            }
            else -> errorResult("Unknown action: ${args["action"]}")
        }
    }
}

// ── DeviceTool ────────────────────────────────────────────────────────────────

class DeviceTool : Tool {
    private val ctx get() = AplicacionDoey.instance
    override fun name()        = "device"
    override fun description() = "Query device state: GPS location, battery, volume, Wi-Fi, Bluetooth. Calculate GPS distance."
    override fun systemHint()  = "Fetch GPS location before weather or navigation requests."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"     to mapOf("type" to "string",
                "enum" to listOf(
                    "get_location","get_battery","get_volume","set_volume",
                    "get_wifi_status","get_bluetooth_status","calculate_distance",
                    "set_brightness","set_brightness_auto",
                    "enable_wifi","disable_wifi",
                    "enable_bluetooth","disable_bluetooth",
                    "set_ringer_mode",
                    "flashlight_on","flashlight_off"
                )),
            "volume"     to mapOf("type" to "number"),
            "brightness" to mapOf("type" to "number"),
            "mode"       to mapOf("type" to "string"),
            "latitude1"  to mapOf("type" to "number"),
            "longitude1" to mapOf("type" to "number"),
            "latitude2"  to mapOf("type" to "number"),
            "longitude2" to mapOf("type" to "number")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        return when (args["action"] as? String) {
            "get_location"          -> getLocation()
            "get_battery"           -> getBattery()
            "get_volume"            -> getVolume()
            "set_volume"            -> setVolume((args["volume"] as? Number)?.toDouble() ?: 50.0)
            "get_wifi_status"       -> getWifiStatus()
            "get_bluetooth_status"  -> getBluetoothStatus()
            "calculate_distance"    -> calcDistance(args)
            "set_brightness"        -> setBrightness((args["brightness"] as? Number)?.toInt() ?: 128)
            "set_brightness_auto"   -> setBrightnessAuto()
            "enable_wifi"           -> setWifi(true)
            "disable_wifi"          -> setWifi(false)
            "enable_bluetooth"      -> setBluetooth(true)
            "disable_bluetooth"     -> setBluetooth(false)
            "set_ringer_mode"       -> setRingerMode(args["mode"] as? String ?: "normal")
            "flashlight_on"         -> setFlashlight(true)
            "flashlight_off"        -> setFlashlight(false)
            else                    -> errorResult("Acción desconocida: ${args["action"]}")
        }
    }

    private suspend fun getLocation(): ToolResult = withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val loc    = suspendCancellableCoroutine<Location?> { cont ->
                @Suppress("MissingPermission")
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            if (loc != null)
                successResult("lat=${loc.latitude}, lon=${loc.longitude}, accuracy=${loc.accuracy}m")
            else
                errorResult("Location unavailable – GPS may be off or cold start")
        } catch (e: Exception) { errorResult("Location error: ${e.message}") }
    }

    private fun getBattery(): ToolResult {
        val bm  = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return successResult("Battery: $pct%")
    }

    private fun getVolume(): ToolResult {
        val am  = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return successResult("Volume: ${if (max > 0) cur * 100 / max else 0}% ($cur/$max)")
    }

    private fun setVolume(pct: Double): ToolResult {
        val am     = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max    = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (pct.coerceIn(0.0, 100.0) * max / 100).toInt(), 0)
        return successResult("Volume set to ${pct.toInt()}%")
    }

    private fun getWifiStatus(): ToolResult {
        val cm        = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val connected = cm.activeNetworkInfo?.isConnected == true
        return successResult("WiFi: ${if (connected) "connected" else "disconnected"}")
    }

    private fun getBluetoothStatus(): ToolResult {
        @Suppress("DEPRECATION")
        val bt      = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val enabled = bt?.isEnabled == true
        return successResult("Bluetooth: ${if (enabled) "on" else "off"}")
    }

    private fun calcDistance(args: Map<String, Any?>): ToolResult {
        val lat1 = (args["latitude1"]  as? Number)?.toDouble() ?: return errorResult("latitude1 required")
        val lon1 = (args["longitude1"] as? Number)?.toDouble() ?: return errorResult("longitude1 required")
        val lat2 = (args["latitude2"]  as? Number)?.toDouble() ?: return errorResult("latitude2 required")
        val lon2 = (args["longitude2"] as? Number)?.toDouble() ?: return errorResult("longitude2 required")
        val R    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val km   = R * 2 * atan2(sqrt(a), sqrt(1 - a))
        return successResult("Distance: ${"%.2f".format(km)} km (${"%.1f".format(km * 0.621371)} mi)")
    }

    private fun setBrightness(level: Int): ToolResult {
        return try {
            val clamped = level.coerceIn(0, 255)
            val resolver: ContentResolver = ctx.contentResolver
            android.provider.Settings.System.putInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            android.provider.Settings.System.putInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                clamped
            )
            val pct = (clamped * 100 / 255)
            successResult("Brightness set to $clamped", "☀️ Brillo al $pct%")
        } catch (e: Exception) {
            errorResult("No se pudo ajustar el brillo: ${e.message}")
        }
    }

    private fun setBrightnessAuto(): ToolResult {
        return try {
            android.provider.Settings.System.putInt(
                ctx.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            successResult("Brightness set to auto", "✨ Brillo automático activado")
        } catch (e: Exception) {
            errorResult("No se pudo activar brillo automático: ${e.message}")
        }
    }

    private fun setWifi(enable: Boolean): ToolResult {
        return try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            val msg = if (enable) "Abriendo ajustes WiFi para activar" else "Abriendo ajustes WiFi para desactivar"
            successResult(msg, "📶 $msg")
        } catch (e: Exception) {
            errorResult("No se pudo abrir ajustes WiFi: ${e.message}")
        }
    }

    private fun setBluetooth(enable: Boolean): ToolResult {
        return try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            val msg = if (enable) "Abriendo ajustes Bluetooth para activar" else "Abriendo ajustes Bluetooth para desactivar"
            successResult(msg, "🔷 $msg")
        } catch (e: Exception) {
            errorResult("No se pudo abrir ajustes Bluetooth: ${e.message}")
        }
    }

    private fun setRingerMode(mode: String): ToolResult {
        return try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val (amMode, label) = when (mode.lowercase()) {
                "silent"  -> AudioManager.RINGER_MODE_SILENT  to "🔇 Modo silencio activado"
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE to "📳 Modo vibración activado"
                else      -> AudioManager.RINGER_MODE_NORMAL  to "🔔 Modo sonido activado"
            }
            am.ringerMode = amMode
            successResult("Ringer mode: $mode", label)
        } catch (e: Exception) {
            errorResult("No se pudo cambiar el modo de sonido: ${e.message}")
        }
    }

    private fun setFlashlight(enable: Boolean): ToolResult {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return errorResult("No se encontró cámara")
            cm.setTorchMode(cameraId, enable)
            val label = if (enable) "🔦 Linterna encendida" else "🌑 Linterna apagada"
            successResult("Flashlight ${if (enable) "on" else "off"}", label)
        } catch (e: Exception) {
            errorResult("No se pudo controlar la linterna: ${e.message}")
        }
    }
}

// ── QueryContactsTool ─────────────────────────────────────────────────────────

class QueryContactsTool : Tool {
    override fun name()        = "query_contacts"
    override fun description() = "Search device contacts by name."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "query" to mapOf("type" to "string"),
            "limit" to mapOf("type" to "integer")
        ),
        "required" to listOf("query")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val ctx   = AplicacionDoey.instance
        val query = args["query"] as? String ?: return@withContext errorResult("query required")
        val limit = (args["limit"] as? Number)?.toInt() ?: 10
        val list  = mutableListOf<String>()
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
        )?.use { c ->
            val ni = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pi = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) list.add("${c.getString(ni)}: ${c.getString(pi)}")
        }
        if (list.isEmpty()) successResult("No contacts for \"$query\"")
        else successResult("Contacts:\n${list.joinToString("\n")}")
    }
}

// ── QuerySmsTool ──────────────────────────────────────────────────────────────

class QuerySmsTool : Tool {
    override fun name()        = "query_sms"
    override fun description() = "Read SMS inbox messages."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "contact" to mapOf("type" to "string"),
            "limit"   to mapOf("type" to "integer")
        )
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val ctx     = AplicacionDoey.instance
        val contact = args["contact"] as? String
        val limit   = (args["limit"] as? Number)?.toInt() ?: 10
        val list    = mutableListOf<String>()
        ctx.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            if (contact != null) "${Telephony.Sms.ADDRESS} LIKE ?" else null,
            if (contact != null) arrayOf("%$contact%") else null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )?.use { c ->
            val ai = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bi = c.getColumnIndex(Telephony.Sms.BODY)
            val di = c.getColumnIndex(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val d = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(c.getLong(di)))
                list.add("From: ${c.getString(ai)} [$d]\n${c.getString(bi)}")
            }
        }
        if (list.isEmpty()) successResult("No SMS found") else successResult(list.joinToString("\n\n---\n\n"))
    }
}

// ── QueryCallLogTool ──────────────────────────────────────────────────────────

class QueryCallLogTool : Tool {
    override fun name()        = "query_call_log"
    override fun description() = "Read recent call history."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "limit" to mapOf("type" to "integer"),
            "type"  to mapOf("type" to "string", "enum" to listOf("all","incoming","outgoing","missed"))
        )
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val ctx    = AplicacionDoey.instance
        val limit  = (args["limit"] as? Number)?.toInt() ?: 10
        val tf     = args["type"] as? String ?: "all"
        val sel    = when (tf) {
            "incoming" -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.INCOMING_TYPE}"
            "outgoing" -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
            "missed"   -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
            else       -> null
        }
        val list   = mutableListOf<String>()
        ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
            sel, null, "${CallLog.Calls.DATE} DESC LIMIT $limit"
        )?.use { c ->
            val ni = c.getColumnIndex(CallLog.Calls.NUMBER)
            val oi = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val ti = c.getColumnIndex(CallLog.Calls.TYPE)
            val di = c.getColumnIndex(CallLog.Calls.DATE)
            val ri = c.getColumnIndex(CallLog.Calls.DURATION)
            while (c.moveToNext()) {
                val tp   = when (c.getInt(ti)) { CallLog.Calls.INCOMING_TYPE -> "in"; CallLog.Calls.OUTGOING_TYPE -> "out"; CallLog.Calls.MISSED_TYPE -> "missed"; else -> "?" }
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(c.getLong(di)))
                val name = c.getString(oi)?.takeIf { it.isNotBlank() } ?: c.getString(ni)
                list.add("$name [$tp, $date, ${c.getLong(ri)}s]")
            }
        }
        if (list.isEmpty()) successResult("No calls found") else successResult(list.joinToString("\n"))
    }
}

// ── HttpTool ──────────────────────────────────────────────────────────────────

class HttpTool : Tool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun name()        = "http"
    override fun description() = "Make REST API requests (GET/POST/PUT/PATCH/DELETE) with optional headers and body."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "method"     to mapOf("type" to "string", "enum" to listOf("GET","POST","PUT","PATCH","DELETE")),
            "url"        to mapOf("type" to "string"),
            "headers"    to mapOf("type" to "array",
                "items" to mapOf("type" to "object",
                    "properties" to mapOf("key" to mapOf("type" to "string"), "value" to mapOf("type" to "string")))),
            "body"       to mapOf("type" to "string"),
            "auth_token" to mapOf("type" to "string", "description" to "Bearer token")
        ),
        "required" to listOf("method", "url")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val method    = args["method"]     as? String ?: return@withContext errorResult("method required")
        val url       = args["url"]        as? String ?: return@withContext errorResult("url required")
        val bodyStr   = args["body"]       as? String
        val authToken = args["auth_token"] as? String
        try {
            val rb = Request.Builder().url(url)
            authToken?.let { rb.addHeader("Authorization", "Bearer $it") }
            @Suppress("UNCHECKED_CAST")
            (args["headers"] as? List<Map<String, String>>)?.forEach { h ->
                rb.addHeader(h["key"] ?: "", h["value"] ?: "")
            }
            val bodyObj = (bodyStr ?: "{}").toRequestBody("application/json".toMediaType())
            val req = when (method) {
                "GET"    -> rb.get().build()
                "DELETE" -> rb.delete().build()
                "POST"   -> rb.post(bodyObj).build()
                "PUT"    -> rb.put(bodyObj).build()
                "PATCH"  -> rb.patch(bodyObj).build()
                else     -> return@withContext errorResult("Unknown method: $method")
            }
            val resp     = client.newCall(req).execute()
            val respBody = resp.body?.string() ?: ""
            val truncated = if (respBody.length > 8000) respBody.take(8000) + "\n[truncated]" else respBody
            if (resp.isSuccessful) successResult("HTTP ${resp.code}\n$truncated")
            else errorResult("HTTP ${resp.code}: $truncated")
        } catch (e: Exception) { errorResult("HTTP failed: ${e.message}") }
    }
}

// ── TTSTool ───────────────────────────────────────────────────────────────────

class TTSTool : Tool {
    override fun name()        = "tts"
    override fun description() = "Speak text aloud using text-to-speech."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "text"     to mapOf("type" to "string"),
            "language" to mapOf("type" to "string")
        ),
        "required" to listOf("text")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val text = args["text"]     as? String ?: return errorResult("text required")
        val lang = args["language"] as? String ?: "en-US"
        return try {
            DoeyTTSEngine.speakAndWait(text, lang)
            successResult("Spoken: ${text.take(60)}")
        } catch (e: Exception) { errorResult("TTS failed: ${e.message}") }
    }
}

// ── AccessibilityTool ─────────────────────────────────────────────────────────

class AccessibilityTool : Tool {
    override fun name()        = "accessibility"
    override fun description() = "Read and control any app's UI. Get screen content, click, type, scroll, swipe."
    override fun systemHint()  = "Use to automate UI tasks in other apps when no direct API is available."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"       to mapOf("type" to "string",
                "enum" to listOf("get_tree","click","long_click","type","scroll","swipe","wait_for_app","is_running","back","home","paste","clipboard_set","clipboard_get")),
            "node_id"      to mapOf("type" to "string"),
            "text"         to mapOf("type" to "string"),
            "package_name" to mapOf("type" to "string"),
            "direction"    to mapOf("type" to "string", "enum" to listOf("up","down","left","right")),
            "x1"           to mapOf("type" to "number"),
            "y1"           to mapOf("type" to "number"),
            "x2"           to mapOf("type" to "number"),
            "y2"           to mapOf("type" to "number"),
            "duration_ms"  to mapOf("type" to "integer"),
            "timeout_ms"   to mapOf("type" to "integer")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")

        if (action == "is_running") {
            return successResult("Accessibility service running: ${DoeyAccessibilityService.isRunning()}")
        }

        val svc = DoeyAccessibilityService.instance
            ?: return errorResult("Accessibility service not enabled. Enable in Android Settings → Accessibility → Doey.")

        return when (action) {
            "get_tree"   -> withContext(Dispatchers.Main) {
                val tree = svc.buildAccessibilityTree()
                successResult(tree.ifBlank { "Screen is empty or not readable" })
            }
            "click"      -> {
                val nodeId = args["node_id"] as? String ?: return errorResult("node_id required")
                withContext(Dispatchers.Main) {
                    if (svc.performNodeAction("click", nodeId, null))
                        successResult("Clicked $nodeId")
                    else errorResult("Click failed on $nodeId")
                }
            }
            "long_click" -> {
                val nodeId = args["node_id"] as? String ?: return errorResult("node_id required")
                withContext(Dispatchers.Main) {
                    if (svc.performNodeAction("long_click", nodeId, null))
                        successResult("Long-clicked $nodeId")
                    else errorResult("Long-click failed on $nodeId")
                }
            }
            "type"       -> {
                val nodeId = args["node_id"] as? String ?: return errorResult("node_id required")
                val text   = args["text"]    as? String ?: return errorResult("text required")
                withContext(Dispatchers.Main) {
                    if (svc.performNodeAction("type", nodeId, text))
                        successResult("Typed into $nodeId")
                    else errorResult("Type failed on $nodeId")
                }
            }
            "scroll"     -> {
                val nodeId = args["node_id"] as? String ?: return errorResult("node_id required")
                val dir    = args["direction"] as? String ?: "down"
                withContext(Dispatchers.Main) {
                    if (svc.performNodeAction("scroll_$dir", nodeId, null))
                        successResult("Scrolled $dir")
                    else errorResult("Scroll failed")
                }
            }
            "swipe"      -> {
                val x1  = (args["x1"] as? Number)?.toFloat() ?: return errorResult("x1 required")
                val y1  = (args["y1"] as? Number)?.toFloat() ?: return errorResult("y1 required")
                val x2  = (args["x2"] as? Number)?.toFloat() ?: return errorResult("x2 required")
                val y2  = (args["y2"] as? Number)?.toFloat() ?: return errorResult("y2 required")
                val dur = (args["duration_ms"] as? Number)?.toLong() ?: 300L
                withContext(Dispatchers.Main) {
                    if (svc.performSwipe(x1, y1, x2, y2, dur))
                        successResult("Swiped from ($x1,$y1) to ($x2,$y2)")
                    else errorResult("Swipe failed")
                }
            }
            "wait_for_app" -> {
                val pkg     = args["package_name"] as? String ?: return errorResult("package_name required")
                val timeout = (args["timeout_ms"] as? Number)?.toLong() ?: 5000L
                val ok      = withContext(Dispatchers.IO) { svc.waitForPackage(pkg, timeout) }
                if (ok) successResult("$pkg is in foreground") else errorResult("Timed out waiting for $pkg")
            }
            "back"  -> withContext(Dispatchers.Main) { svc.performNodeAction("back",  null, null); successResult("Back pressed") }
            "home"  -> withContext(Dispatchers.Main) { svc.performNodeAction("home",  null, null); successResult("Home pressed") }
            "paste" -> withContext(Dispatchers.Main) { svc.performNodeAction("paste", null, null); successResult("Paste executed") }
            "clipboard_set" -> {
                val text = args["text"] as? String ?: return errorResult("text required")
                withContext(Dispatchers.Main) { svc.performNodeAction("clipboard_set", null, text); successResult("Clipboard set") }
            }
            "clipboard_get" -> {
                withContext(Dispatchers.Main) { svc.performNodeAction("clipboard_get", null, null); successResult("Clipboard retrieved") }
            }
            else -> errorResult("Unknown action: $action")
        }
    }
}

// ── AppSearchTool ─────────────────────────────────────────────────────────────

class AppSearchTool : Tool {
    override fun name()        = "app_search"
    override fun description() = "Search for installed apps on the device."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf("query" to mapOf("type" to "string")),
        "required" to listOf("query")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val ctx   = AplicacionDoey.instance
        val query = (args["query"] as? String)?.lowercase() ?: return@withContext errorResult("query required")
        val pm    = ctx.packageManager
        val apps  = pm.getInstalledApplications(0)
            .filter { pm.getApplicationLabel(it).toString().lowercase().contains(query) || it.packageName.lowercase().contains(query) }
            .take(10)
            .map { "${pm.getApplicationLabel(it)} → ${it.packageName}" }
        if (apps.isEmpty()) successResult("No apps found for \"$query\"")
        else successResult("Apps:\n${apps.joinToString("\n")}")
    }
}

// ── SkillDetailTool ───────────────────────────────────────────────────────────

class SkillDetailTool(private val skillLoader: SkillLoader) : Tool {
    override fun name()        = "skill_detail"
    override fun description() = "Get full instructions for a skill. ALWAYS call this before using any skill."
    override fun systemHint()  = "Call this FIRST, before executing any skill task."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "skill_name" to mapOf("type" to "string"),
            "fragment" to mapOf("type" to "string", "description" to "Optional fragment name to load only a specific part of a mega skill")
        ),
        "required" to listOf("skill_name")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val name  = args["skill_name"] as? String ?: return errorResult("skill_name required")
        val skill = skillLoader.getSkill(name)    ?: return errorResult("Skill not found: $name")

        val fragment = args["fragment"] as? String
        val expertMode = kotlinx.coroutines.runBlocking { AplicacionDoey.instance.settingsStore.getExpertMode() }
        
        val contentToReturn = if (fragment != null && skill.content.contains("## Fragmento: $fragment", ignoreCase = true)) {
            val regex = Regex("## Fragmento: $fragment(?i)(.*?)(?=## Fragmento:|\$)", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(skill.content)
            match?.value?.trim() ?: skill.content
        } else {
            skill.content
        }
        
        val sb = StringBuilder("# Skill: ${skill.name}\n\n$contentToReturn")

        if (!expertMode && skill.credentials.isNotEmpty()) {
            val credLabels = skill.credentials.joinToString { it.label }
            sb.append("\n\n⚠️ **NOTA DE MODO BÁSICO**: Esta skill normalmente requiere claves API ($credLabels). " +
                      "Como el Modo Experto está desactivado, NO intentes usar herramientas HTTP con estas claves. " +
                      "En su lugar, usa `intent` para abrir la aplicación correspondiente o `accessibility` para interactuar con ella.")
        }

        return successResult(sb.toString())
    }
}

// ── PersonalMemoryTool ────────────────────────────────────────────────────────

class PersonalMemoryTool : Tool {
    private val store get() = AplicacionDoey.instance.settingsStore
    override fun name()        = "memory_personal_upsert"
    override fun description() = "Store personal facts about the user for future context."
    override fun systemHint()  = "Call FIRST when you detect any stable personal fact in the user's message."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "facts" to mapOf("type" to "array",
                "items" to mapOf("type" to "object",
                    "properties" to mapOf(
                        "fact"     to mapOf("type" to "string"),
                        "category" to mapOf("type" to "string")
                    ),
                    "required" to listOf("fact")))
        ),
        "required" to listOf("facts")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        @Suppress("UNCHECKED_CAST")
        val facts    = args["facts"] as? List<Map<String, String>> ?: return errorResult("facts required")
        val existing = store.getPersonalMemory()
        // Parsear entradas existentes (JSON o Markdown legacy)
        val entries  = parseMemoryEntries(existing).toMutableList()
        facts.forEach { f ->
            val fact = f["fact"] ?: return@forEach
            val cat  = f["category"] ?: "otro"
            // Separar variable:definicion si viene con :
            val colonIdx = fact.indexOf(':')
            val variable   = if (colonIdx != -1) fact.substring(0, colonIdx).trim() else fact.trim()
            val definition = if (colonIdx != -1) fact.substring(colonIdx + 1).trim() else ""
            // Actualizar si ya existe la misma variable en la misma categoría
            val idx = entries.indexOfFirst {
                it.category == cat && it.variable.equals(variable, ignoreCase = true)
            }
            if (idx >= 0) {
                entries[idx] = entries[idx].copy(definition = definition)
            } else {
                entries.add(MemoryEntry(category = cat, variable = variable, definition = definition))
            }
        }
        store.setPersonalMemory((entries as List<MemoryEntry>).toJson())
        return successResult("Stored ${facts.size} fact(s)")
    }
}

// ── FileStorageTool ───────────────────────────────────────────────────────────

class FileStorageTool : Tool {
    private val prefs get() = AplicacionDoey.instance.getSharedPreferences("doey_files", Context.MODE_PRIVATE)
    override fun name()        = "file_storage"
    override fun description() = "Read/write text files to persistent storage (notes, lists, data)."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action"  to mapOf("type" to "string", "enum" to listOf("read","write","append","delete","list")),
            "key"     to mapOf("type" to "string"),
            "content" to mapOf("type" to "string")
        ),
        "required" to listOf("action", "key")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")
        val key    = "file_${args["key"] as? String ?: return errorResult("key required")}"
        return when (action) {
            "read"   -> prefs.getString(key, null)?.let { successResult(it) } ?: errorResult("File not found")
            "write"  -> { prefs.edit().putString(key, args["content"] as? String ?: "").apply(); successResult("Written") }
            "append" -> {
                val existing = prefs.getString(key, "") ?: ""
                prefs.edit().putString(key, "$existing\n${args["content"] ?: ""}").apply()
                successResult("Appended")
            }
            "delete" -> { prefs.edit().remove(key).apply(); successResult("Deleted") }
            "list"   -> {
                val keys = prefs.all.keys.filter { it.startsWith("file_") }.map { it.removePrefix("file_") }
                successResult("Files: ${keys.joinToString(", ").ifBlank { "(none)" }}")
            }
            else -> errorResult("Unknown action: $action")
        }
    }
}
// ── AlarmTool ─────────────────────────────────────────────────────────────────

class AlarmTool : Tool {
    private val ctx get() = AplicacionDoey.instance
    override fun name()        = "set_alarm"
    override fun description() = "Schedule alarms, timers, and reminders that sound even when app is closed."
    override fun systemHint()  = "Use for scheduling alarms, timers, and wake-up reminders. Always confirm time with user."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "type"        to mapOf("type" to "string", "enum" to listOf("alarm", "timer", "reminder")),
            "hour"        to mapOf("type" to "number", "description" to "Hour (0-23) for alarm"),
            "minute"      to mapOf("type" to "number", "description" to "Minute (0-59)"),
            "delay_minutes" to mapOf("type" to "number", "description" to "Minutes from now for timer"),
            "title"       to mapOf("type" to "string", "description" to "Alarm title/label"),
            "description" to mapOf("type" to "string", "description" to "Alarm description"),
            "recurring"   to mapOf("type" to "boolean", "description" to "Daily recurring alarm")
        ),
        "required" to listOf("type", "title")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val type = args["type"] as? String ?: return errorResult("type required")
        val title = args["title"] as? String ?: "Alarma"
        val desc = args["description"] as? String ?: ""
        val alarmId = (System.currentTimeMillis() % 100000).toInt()

        return try {
            when (type) {
                "alarm" -> {
                    val hour = (args["hour"] as? Number)?.toInt() ?: return errorResult("hour required")
                    val minute = (args["minute"] as? Number)?.toInt() ?: 0
                    val recurring = args["recurring"] as? Boolean ?: false
                    com.doey.servicios.comun.AlarmScheduler.scheduleAlarmAtTime(alarmId, title, desc, hour, minute, recurring)
                    
                    // Persistir alarma para la UI de Reloj
                    val prefs = ctx.getSharedPreferences("doey_alarms_store", 0)
                    val alarms = JSONArray(prefs.getString("alarms", "[]"))
                    alarms.put(JSONObject().apply {
                        put("id", alarmId)
                        put("title", title)
                        put("time", String.format("%02d:%02d", hour, minute))
                        put("enabled", true)
                        put("recurring", recurring)
                    })
                    prefs.edit().putString("alarms", alarms.toString()).apply()

                    try {
                        val intent = Intent("android.intent.action.SET_ALARM").apply {
                            putExtra("android.intent.extra.alarm.HOUR", hour)
                            putExtra("android.intent.extra.alarm.MINUTES", minute)
                            putExtra("android.intent.extra.alarm.MESSAGE", title)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                    } catch (e: Exception) {}
                    
                    successResult("Alarma programada para las ${String.format("%02d:%02d", hour, minute)}${if (recurring) " diariamente" else ""}")
                }
                "timer" -> {
                    val delayMin = (args["delay_minutes"] as? Number)?.toInt() ?: return errorResult("delay_minutes required")
                    com.doey.servicios.comun.AlarmScheduler.scheduleAlarmInMinutes(alarmId, title, desc, delayMin)
                    successResult("Temporizador de $delayMin minutos iniciado")
                }
                "reminder" -> {
                    val hour = (args["hour"] as? Number)?.toInt() ?: return errorResult("hour required")
                    val minute = (args["minute"] as? Number)?.toInt() ?: 0
                    com.doey.servicios.comun.AlarmScheduler.scheduleAlarmAtTime(alarmId, title, desc, hour, minute, false)
                    successResult("Recordatorio programado para las ${String.format("%02d:%02d", hour, minute)}")
                }
                else -> errorResult("Unknown alarm type: $type")
            }
        } catch (e: Exception) {
            errorResult("Alarm error: ${e.message}")
        }
    }
}

// ── AppSearchAndLaunchTool ─────────────────────────────────────────────────────

class AppSearchAndLaunchTool : Tool {
    private val ctx get() = AplicacionDoey.instance
    override fun name()        = "find_and_launch_app"
    override fun description() = "Search for installed apps by name and launch them. Uses accessibility if needed."
    override fun systemHint()  = "Use when you don't know the exact package name. Searches installed apps and launches them."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "app_name" to mapOf("type" to "string", "description" to "Name of the app to find and launch")
        ),
        "required" to listOf("app_name")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val appName = args["app_name"] as? String ?: return errorResult("app_name required")
        
        return withContext(Dispatchers.Main) {
            try {
                val pm = ctx.packageManager
                val packages = pm.getInstalledPackages(0)
                
                // Buscar por nombre exacto o parcial
                val matchedPackage = packages.find { pkg ->
                    val label = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
                    } catch (e: Exception) { "" }
                    label.equals(appName, ignoreCase = true) || label.lowercase().contains(appName.lowercase())
                }
                
                if (matchedPackage != null) {
                    val intent = pm.getLaunchIntentForPackage(matchedPackage.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                        return@withContext successResult("App '${matchedPackage.packageName}' launched")
                    }
                }
                
                errorResult("App '$appName' not found or not launchable")
            } catch (e: Exception) {
                errorResult("App search error: ${e.message}")
            }
        }
    }
}
