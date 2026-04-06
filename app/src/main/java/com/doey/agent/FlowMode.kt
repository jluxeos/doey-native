package com.doey.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val params: Map<String, String> = emptyMap(),
    val action: (suspend (Context, Map<String, String>) -> Unit)? = null
)

object FlowModeEngine {

    fun getRootNodes(): List<FlowNode> = listOf(
        FlowNode(
            id = "root_open",
            label = "Abrir",
            options = listOf(
                FlowOption("app_select", "Seleccionar App", "app_list"),
                FlowOption("contact_open", "Contacto", "contact_list")
            )
        ),
        FlowNode(
            id = "root_send",
            label = "Enviar",
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
            options = listOf(
                FlowOption("wifi_toggle", "WiFi", "wifi_options"),
                FlowOption("bluetooth_toggle", "Bluetooth", "bluetooth_options")
            )
        )
    )

    suspend fun getNodeById(ctx: Context, nodeId: String): FlowNode? = withContext(Dispatchers.Default) {
        when (nodeId) {
            "app_list" -> getAppListNode(ctx)
            "contact_list" -> getContactListNode(ctx)
            "wifi_options" -> getWiFiOptionsNode()
            "bluetooth_options" -> getBluetoothOptionsNode()
            "music_apps" -> getMusicAppsNode(ctx)
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
                    params = mapOf("package" to pkg),
                    action = { ctx, _ ->
                        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (intent != null) ctx.startActivity(intent)
                    }
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
                    params = mapOf("contact" to name, "phone" to phone)
                )
            }
        )
    }

    private fun getWiFiOptionsNode() = FlowNode(
        id = "wifi_options",
        label = "WiFi",
        options = listOf(
            FlowOption("wifi_on", "Activar"),
            FlowOption("wifi_off", "Desactivar")
        )
    )

    private fun getBluetoothOptionsNode() = FlowNode(
        id = "bluetooth_options",
        label = "Bluetooth",
        options = listOf(
            FlowOption("bt_on", "Activar"),
            FlowOption("bt_off", "Desactivar")
        )
    )

    private suspend fun getMusicAppsNode(ctx: Context): FlowNode {
        val musicApps = getMusicApplications(ctx)
        return FlowNode(
            id = "music_apps",
            label = "Apps de música",
            options = musicApps.map { (name, pkg) ->
                FlowOption(
                    id = "music_$pkg",
                    label = name,
                    params = mapOf("package" to pkg)
                )
            }
        )
    }

    // ── UTILIDADES ─────────────────────

    private suspend fun getInstalledApps(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { pm.getApplicationLabel(it).toString() to it.packageName }
            .take(30)
    }

    private suspend fun getContactsList(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<String, String>>()

        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phone = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                list.add(name to phone)
            }
        }

        list.take(30)
    }

    private suspend fun getMusicApplications(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apps = listOf(
            "Spotify" to "com.spotify.music",
            "YouTube Music" to "com.google.android.apps.youtube.music"
        )

        apps.filter { (_, pkg) ->
            try {
                ctx.packageManager.getApplicationInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}