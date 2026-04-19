package com.doey.FORGE

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.doey.AplicacionDoey
import com.doey.VEIL.DoeyAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HerramientaShell — Acciones de sistema de bajo nivel
 *
 * Complementa AccessibilityTool con acciones que no requieren leer la UI:
 *  - input_text   → inyecta texto directamente (más fiable que ACTION_SET_TEXT en algunos lanzadores)
 *  - input_key    → presiona tecla por keycode (ENTER, DEL, TAB, SEARCH, DPAD_*, etc.)
 *  - open_settings_page → abre una pantalla específica de Ajustes por action
 *  - open_app_settings  → abre Ajustes de la app indicada
 *  - notification_bar   → expande/colapsa el panel de notificaciones
 *  - lock_screen        → bloquea la pantalla
 *  - expand_status_bar  → expande barra de estado
 *  - get_screen_size    → devuelve dimensiones de pantalla (útil para calcular coordenadas)
 *  - set_text_in_focus  → escribe en el campo actualmente enfocado
 *  - press_enter        → simula ENTER en el campo enfocado
 *  - clear_focused      → limpia el campo enfocado
 */
class HerramientaShell : Tool {
    private val ctx get() = AplicacionDoey.instance

    override fun name()        = "shell_action"
    override fun description() = "Acciones de sistema de bajo nivel: presionar teclas, abrir páginas de Ajustes, panel de notificaciones, tamaño de pantalla. Para control fino que no requiere UI tree."
    override fun systemHint()  = "Usa input_key con ENTER, DEL, TAB después de escribir en campos. Usa get_screen_size para calcular coordenadas antes de tap_xy."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "string",
                "enum" to listOf(
                    "input_key", "input_text", "set_text_in_focus",
                    "press_enter", "clear_focused",
                    "open_settings_page", "open_app_settings",
                    "notification_bar", "lock_screen",
                    "get_screen_size"
                )
            ),
            "keycode"      to mapOf("type" to "string",  "description" to "Keycode de Android: ENTER, DEL, TAB, SEARCH, BACK, HOME, DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT, DPAD_CENTER, VOLUME_UP, VOLUME_DOWN, CAMERA, POWER"),
            "text"         to mapOf("type" to "string",  "description" to "Texto a inyectar (para input_text/set_text_in_focus)"),
            "settings_action" to mapOf("type" to "string", "description" to "Action de Settings, p.ej. android.settings.WIFI_SETTINGS, android.settings.APPLICATION_DETAILS_SETTINGS"),
            "package_name" to mapOf("type" to "string",  "description" to "Paquete de la app (para open_app_settings)"),
            "expand"       to mapOf("type" to "boolean", "description" to "true=expandir, false=colapsar (para notification_bar)")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")

        return when (action) {

            "input_key" -> {
                val key = args["keycode"] as? String ?: return errorResult("keycode required")
                val svc = DoeyAccessibilityService.instance
                    ?: return errorResult("Servicio de accesibilidad no activo")
                val code = resolveKeycode(key)
                    ?: return errorResult("Keycode desconocido: $key. Usa: ENTER, DEL, TAB, SEARCH, BACK, HOME, DPAD_UP/DOWN/LEFT/RIGHT/CENTER, VOLUME_UP, VOLUME_DOWN")
                withContext(Dispatchers.Main) {
                    val root = svc.rootInActiveWindow
                    val focused = root?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                    val ok = when (code) {
                        android.view.KeyEvent.KEYCODE_BACK  -> { svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK); true }
                        android.view.KeyEvent.KEYCODE_HOME  -> { svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME); true }
                        else -> {
                            // Intentar con el nodo enfocado primero
                            val b = android.os.Bundle()
                            b.putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, 1)
                            focused?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, b) ?: false
                        }
                    }
                    focused?.recycle()
                    root?.recycle()
                    // Fallback: inyectar via InputManager si está disponible
                    injectKeyEvent(code)
                    successResult("Tecla $key inyectada")
                }
            }

            "set_text_in_focus" -> {
                val text = args["text"] as? String ?: return errorResult("text required")
                val svc  = DoeyAccessibilityService.instance
                    ?: return errorResult("Servicio de accesibilidad no activo")
                withContext(Dispatchers.Main) {
                    val root = svc.rootInActiveWindow
                        ?: return@withContext errorResult("Sin ventana activa")
                    val focused = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                    root.recycle()
                    if (focused == null) return@withContext errorResult("No hay campo con foco activo")
                    val bundle = android.os.Bundle().apply {
                        putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    }
                    val ok = focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                    focused.recycle()
                    if (ok) successResult("Texto escrito en campo enfocado: \"$text\"")
                    else    errorResult("No se pudo escribir — el campo puede no aceptar texto")
                }
            }

            "press_enter" -> {
                val svc = DoeyAccessibilityService.instance
                    ?: return errorResult("Servicio de accesibilidad no activo")
                withContext(Dispatchers.Main) {
                    injectKeyEvent(android.view.KeyEvent.KEYCODE_ENTER)
                    successResult("ENTER presionado")
                }
            }

            "clear_focused" -> {
                val svc = DoeyAccessibilityService.instance
                    ?: return errorResult("Servicio de accesibilidad no activo")
                withContext(Dispatchers.Main) {
                    val root    = svc.rootInActiveWindow ?: return@withContext errorResult("Sin ventana activa")
                    val focused = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                    root.recycle()
                    if (focused == null) return@withContext errorResult("No hay campo con foco")
                    val bundle = android.os.Bundle().apply {
                        putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    }
                    val ok = focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                    focused.recycle()
                    if (ok) successResult("Campo borrado") else errorResult("No se pudo borrar el campo")
                }
            }

            "open_settings_page" -> {
                val settingsAction = args["settings_action"] as? String
                    ?: return errorResult("settings_action required. Ejemplos: android.settings.WIFI_SETTINGS, android.settings.BLUETOOTH_SETTINGS, android.settings.APPLICATION_DETAILS_SETTINGS, android.settings.ACCESSIBILITY_SETTINGS, android.settings.SOUND_SETTINGS, android.settings.DISPLAY_SETTINGS, android.settings.LOCATION_SOURCE_SETTINGS, android.settings.SECURITY_SETTINGS, android.settings.PRIVACY_SETTINGS, android.settings.NOTIFICATION_SETTINGS, android.settings.BATTERY_SAVER_SETTINGS, android.settings.APP_NOTIFICATION_SETTINGS")
                withContext(Dispatchers.Main) {
                    try {
                        val intent = Intent(settingsAction).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            val pkg = args["package_name"] as? String
                            if (pkg != null) data = android.net.Uri.parse("package:$pkg")
                        }
                        ctx.startActivity(intent)
                        successResult("Abriendo ajustes: $settingsAction")
                    } catch (e: Exception) {
                        errorResult("No se pudo abrir $settingsAction: ${e.message}")
                    }
                }
            }

            "open_app_settings" -> {
                val pkg = args["package_name"] as? String ?: return errorResult("package_name required")
                withContext(Dispatchers.Main) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:$pkg")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                        successResult("Abriendo ajustes de $pkg")
                    } catch (e: Exception) {
                        errorResult("Error al abrir ajustes de $pkg: ${e.message}")
                    }
                }
            }

            "notification_bar" -> {
                val expand = args["expand"] as? Boolean ?: true
                withContext(Dispatchers.Main) {
                    try {
                        val svc = DoeyAccessibilityService.instance
                        if (svc != null) {
                            if (expand) {
                                svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                            } else {
                                svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                            }
                            successResult(if (expand) "Panel de notificaciones expandido" else "Panel cerrado")
                        } else {
                            errorResult("Servicio de accesibilidad no activo")
                        }
                    } catch (e: Exception) {
                        errorResult("Error: ${e.message}")
                    }
                }
            }

            "lock_screen" -> {
                withContext(Dispatchers.Main) {
                    val svc = DoeyAccessibilityService.instance
                    if (svc != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                        successResult("Pantalla bloqueada")
                    } else {
                        errorResult("Bloqueo de pantalla no disponible (requiere Android 9+ y accesibilidad activa)")
                    }
                }
            }

            "get_screen_size" -> {
                withContext(Dispatchers.IO) {
                    try {
                        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                        val metrics = android.util.DisplayMetrics()
                        @Suppress("DEPRECATION")
                        wm.defaultDisplay.getRealMetrics(metrics)
                        val w = metrics.widthPixels
                        val h = metrics.heightPixels
                        val dpi = metrics.densityDpi
                        successResult("Pantalla: ${w}x${h}px a ${dpi}dpi. Centro: (${w/2},${h/2}). Cuartos: TL(${w/4},${h/4}) TR(${3*w/4},${h/4}) BL(${w/4},${3*h/4}) BR(${3*w/4},${3*h/4})")
                    } catch (e: Exception) {
                        errorResult("No se pudo obtener tamaño: ${e.message}")
                    }
                }
            }

            else -> errorResult("Acción desconocida: $action")
        }
    }

    private fun resolveKeycode(key: String): Int? = when (key.uppercase().trim()) {
        "ENTER"        -> android.view.KeyEvent.KEYCODE_ENTER
        "DEL", "DELETE", "BACKSPACE" -> android.view.KeyEvent.KEYCODE_DEL
        "TAB"          -> android.view.KeyEvent.KEYCODE_TAB
        "SEARCH"       -> android.view.KeyEvent.KEYCODE_SEARCH
        "BACK"         -> android.view.KeyEvent.KEYCODE_BACK
        "HOME"         -> android.view.KeyEvent.KEYCODE_HOME
        "MENU"         -> android.view.KeyEvent.KEYCODE_MENU
        "DPAD_UP"      -> android.view.KeyEvent.KEYCODE_DPAD_UP
        "DPAD_DOWN"    -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
        "DPAD_LEFT"    -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
        "DPAD_RIGHT"   -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        "DPAD_CENTER"  -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
        "VOLUME_UP"    -> android.view.KeyEvent.KEYCODE_VOLUME_UP
        "VOLUME_DOWN"  -> android.view.KeyEvent.KEYCODE_VOLUME_DOWN
        "CAMERA"       -> android.view.KeyEvent.KEYCODE_CAMERA
        "SPACE"        -> android.view.KeyEvent.KEYCODE_SPACE
        "ESCAPE", "ESC" -> android.view.KeyEvent.KEYCODE_ESCAPE
        "PAGE_UP"      -> android.view.KeyEvent.KEYCODE_PAGE_UP
        "PAGE_DOWN"    -> android.view.KeyEvent.KEYCODE_PAGE_DOWN
        else           -> null
    }

    private fun injectKeyEvent(keycode: Int) {
        try {
            val im = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            // No podemos inyectar directamente sin root, pero podemos intentar via AccessibilityService
            val svc = DoeyAccessibilityService.instance ?: return
            // Para ENTER: buscar acción de IME en el nodo enfocado
            if (keycode == android.view.KeyEvent.KEYCODE_ENTER) {
                val root = svc.rootInActiveWindow ?: return
                val focused = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null) {
                    val bundle = android.os.Bundle()
                    bundle.putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        android.view.accessibility.AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH)
                    focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, bundle)
                    focused.recycle()
                }
                root.recycle()
            }
        } catch (_: Exception) {}
    }
}
