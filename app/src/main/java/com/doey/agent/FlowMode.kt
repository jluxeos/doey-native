package com.doey.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.ContactsContract
import com.doey.DoeyApplication
import com.doey.tools.*
import com.doey.ui.MemoryEntry
import com.doey.ui.parseMemoryEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Representa un nodo en el árbol de navegación del modo flujo.
 */
data class FlowNode(
    val id: String,
    val label: String,
    val description: String = "",
    val options: List<FlowOption> = emptyList(),
    val isVariableSelector: Boolean = false
)

/**
 * Representa una opción dentro de un nodo.
 */
data class FlowOption(
    val id: String,
    val label: String,
    val nextNodeId: String? = null,
    val command: FlowCommand? = null,
    val params: Map<String, String> = emptyMap()
)

/**
 * Comando predefinido que el modo flujo puede ejecutar offline.
 */
data class FlowCommand(
    val toolName: String,
    val arguments: Map<String, Any?>
)

object FlowModeEngine {

    /**
     * Resuelve las variables en un mapa de argumentos.
     * Busca patrones como {{variable_name}} y los reemplaza con el valor de la memoria personal.
     */
    private suspend fun resolveVariables(args: Map<String, Any?>, context: Context): Map<String, Any?> {
        val settings = (context.applicationContext as DoeyApplication).settingsStore
        val memories = parseMemoryEntries(settings.getPersonalMemory())
        
        fun resolveString(input: String): String {
            var result = input
            memories.forEach { entry ->
                val placeholder = "{{${entry.variable}}}"
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, entry.definition)
                }
            }
            return result
        }

        return args.mapValues { (_, value) ->
            when (value) {
                is String -> resolveString(value)
                is Map<*, *> -> @Suppress("UNCHECKED_CAST") resolveVariables(value as Map<String, Any?>, context)
                else -> value
            }
        }
    }

    /**
     * Ejecuta un comando utilizando el ToolRegistry global.
     */
    suspend fun executeCommand(context: Context, command: FlowCommand): ToolResult = withContext(Dispatchers.Main) {
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
            errorResult("Error al ejecutar ${command.toolName}: ${e.message}")
        }
    }

    /**
     * Retorna las acciones rápidas para mostrar en la HomeScreen.
     */
    fun getQuickActions(): List<FlowOption> = listOf(
        FlowOption(
            id = "quick_wa",
            label = "WhatsApp",
            nextNodeId = "contact_list",
            params = mapOf("app" to "com.whatsapp")
        ),
        FlowOption(
            id = "quick_music_pause",
            label = "Pausar Música",
            command = FlowCommand("intent", mapOf(
                "action" to "com.android.music.musicservicecommand",
                "extras" to listOf(mapOf("key" to "command", "value" to "pause"))
            ))
        ),
        FlowOption(
            id = "quick_music_skip",
            label = "Siguiente",
            command = FlowCommand("intent", mapOf(
                "action" to "com.android.music.musicservicecommand",
                "extras" to listOf(mapOf("key" to "command", "value" to "next"))
            ))
        )
    )

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
                FlowOption("msg_whatsapp", "WhatsApp", "contact_list", params = mapOf("app" to "com.whatsapp")),
                FlowOption("msg_sms", "SMS", "contact_list", params = mapOf("app" to "sms")),
                FlowOption("msg_email", "Email", "contact_list", params = mapOf("app" to "email"))
            )
        ),
        FlowNode(
            id = "root_music",
            label = "Música",
            options = listOf(
                FlowOption("music_play", "Reproducir/Pausar", command = FlowCommand("intent", mapOf(
                    "action" to "com.android.music.musicservicecommand",
                    "extras" to listOf(mapOf("key" to "command", "value" to "togglepause"))
                ))),
                FlowOption("music_next", "Siguiente Canción", command = FlowCommand("intent", mapOf(
                    "action" to "com.android.music.musicservicecommand",
                    "extras" to listOf(mapOf("key" to "command", "value" to "next"))
                ))),
                FlowOption("music_prev", "Canción Anterior", command = FlowCommand("intent", mapOf(
                    "action" to "com.android.music.musicservicecommand",
                    "extras" to listOf(mapOf("key" to "command", "value" to "previous"))
                )))
            )
        ),
        FlowNode(
            id = "root_control",
            label = "Dispositivo",
            options = listOf(
                FlowOption("vol_up", "Subir Volumen", command = FlowCommand("device", mapOf("action" to "set_volume", "volume" to 80))),
                FlowOption("vol_down", "Bajar Volumen", command = FlowCommand("device", mapOf("action" to "set_volume", "volume" to 20))),
                FlowOption("beep_test", "Emitir Beep", command = FlowCommand("beep", mapOf("tone" to "default", "count" to 1)))
            )
        ),
        FlowNode(
            id = "root_vars",
            label = "Mis Variables",
            options = listOf(
                FlowOption("var_sms", "Enviar SMS a {{mamá}}", command = FlowCommand("send_sms", mapOf("phone_number" to "{{mamá}}", "message" to "Hola mamá, estoy usando el modo flujo."))),
                FlowOption("var_call", "Llamar a {{papá}}", command = FlowCommand("intent", mapOf("action" to Intent.ACTION_DIAL, "uri" to "tel:{{papá}}"))),
                FlowOption("var_home", "Navegar a {{casa}}", command = FlowCommand("intent", mapOf("action" to Intent.ACTION_VIEW, "uri" to "google.navigation:q={{casa}}")))
            )
        )
    )

    suspend fun getNodeById(ctx: Context, nodeId: String, params: Map<String, String> = emptyMap()): FlowNode? = withContext(Dispatchers.Default) {
        when (nodeId) {
            "app_list" -> getAppListNode(ctx)
            "contact_list" -> getContactListNode(ctx, params)
            "variable_list" -> getVariableListNode(ctx)
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
                    command = FlowCommand("intent", mapOf("action" to Intent.ACTION_MAIN, "package" to pkg))
                )
            }
        )
    }

    private suspend fun getContactListNode(ctx: Context, params: Map<String, String>): FlowNode {
        val contacts = getContactsList(ctx)
        val targetApp = params["app"]
        
        return FlowNode(
            id = "contact_list",
            label = "Selecciona un contacto",
            options = contacts.map { (name, phone) ->
                val command = when (targetApp) {
                    "com.whatsapp" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_VIEW,
                        "uri" to "https://wa.me/${phone.replace("+", "").replace(" ", "")}",
                        "package" to "com.whatsapp"
                    ))
                    "sms" -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_SENDTO,
                        "uri" to "smsto:$phone"
                    ))
                    else -> FlowCommand("intent", mapOf(
                        "action" to Intent.ACTION_DIAL,
                        "uri" to "tel:$phone"
                    ))
                }
                FlowOption(
                    id = "contact_$phone",
                    label = name,
                    command = command
                )
            }
        )
    }

    private suspend fun getVariableListNode(ctx: Context): FlowNode {
        val settings = (ctx.applicationContext as DoeyApplication).settingsStore
        val memories = parseMemoryEntries(settings.getPersonalMemory())
        return FlowNode(
            id = "variable_list",
            label = "Mis Variables",
            isVariableSelector = true,
            options = memories.map { entry ->
                FlowOption(
                    id = "var_${entry.id}",
                    label = "${entry.variable}: ${entry.definition}",
                    params = mapOf("var_name" to entry.variable, "var_value" to entry.definition)
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
            .sortedBy { it.first }
            .take(50)
    }

    private suspend fun getContactsList(ctx: Context): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<String, String>>()
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)
                val phone = it.getString(phoneIdx)
                list.add(name to phone)
            }
        }
        list.distinctBy { it.second }.sortedBy { it.first }.take(50)
    }
}
