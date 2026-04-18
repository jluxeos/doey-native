package com.doey.herramientas.comun

import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.doey.AplicacionDoey
import com.doey.servicios.basico.DoeyAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HerramientaControlUI — Control total de cualquier app
 *
 * Acciones de alto nivel que evitan un round-trip de get_tree→click.
 * La IA no necesita leer el árbol completo: puede buscar por texto/clase y actuar
 * directamente. Ahorra ~1-3 llamadas al LLM por tarea de UI.
 *
 * Acciones disponibles:
 *  - find_and_tap       → busca nodo por texto y lo toca
 *  - find_and_type      → busca campo de texto y escribe
 *  - find_and_scroll    → busca nodo scrollable y hace scroll hasta encontrar texto
 *  - tap_xy             → toca coordenadas absolutas (para apps con canvas/juegos)
 *  - long_tap_xy        → mantiene pulsado en coordenadas
 *  - pinch              → gesto de pellizco (zoom out)
 *  - spread             → gesto de expansión (zoom in)
 *  - find_node          → busca y devuelve info del nodo sin leer árbol completo
 *  - get_interactive    → árbol comprimido: SOLO nodos clicables/editables (10x menos tokens)
 *  - scroll_to_text     → hace scroll hasta encontrar un texto en pantalla
 *  - double_tap         → doble toque en nodo o coordenadas
 *  - drag               → arrastra de un punto a otro
 */
class HerramientaControlUI : Tool {
    override fun name()        = "ui_control"
    override fun description() = "Control total de cualquier app: busca elementos por texto/clase y actúa sin necesitar leer todo el árbol. Más eficiente que accessibility+get_tree."
    override fun systemHint()  = "Prefiere ui_control sobre accessibility para tareas de UI. Usa find_and_tap en lugar de get_tree+click. Usa get_interactive en lugar de get_tree para ahorrar tokens."

    override fun parameters() = mapOf<String, Any?>(
        "type" to "object",
        "properties" to mapOf(
            "action" to mapOf(
                "type" to "string",
                "enum" to listOf(
                    "find_and_tap", "find_and_type", "find_and_scroll",
                    "tap_xy", "long_tap_xy", "double_tap",
                    "pinch", "spread", "drag",
                    "find_node", "get_interactive", "scroll_to_text",
                    "wait_ms"
                )
            ),
            "text"       to mapOf("type" to "string",  "description" to "Texto a buscar o escribir"),
            "hint"       to mapOf("type" to "string",  "description" to "Texto alternativo / contentDescription / res-id a buscar"),
            "class_name" to mapOf("type" to "string",  "description" to "Clase del widget, p.ej. EditText, Button, RecyclerView"),
            "index"      to mapOf("type" to "integer", "description" to "Índice del resultado (0=primero) cuando hay varios matches"),
            "x"          to mapOf("type" to "number",  "description" to "Coordenada X en píxeles"),
            "y"          to mapOf("type" to "number",  "description" to "Coordenada Y en píxeles"),
            "x2"         to mapOf("type" to "number",  "description" to "Coordenada X2 para drag/pinch"),
            "y2"         to mapOf("type" to "number",  "description" to "Coordenada Y2 para drag/pinch"),
            "duration_ms" to mapOf("type" to "integer", "description" to "Duración del gesto en ms (default 300)"),
            "package_name" to mapOf("type" to "string", "description" to "Filtrar árbol por paquete")
        ),
        "required" to listOf("action")
    )

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return errorResult("action required")

        val svc = DoeyAccessibilityService.instance
            ?: return errorResult("Servicio de accesibilidad no activo. Actívalo en Ajustes → Accesibilidad → Doey.")

        return when (action) {

            "get_interactive" -> withContext(Dispatchers.Main) {
                val pkg  = args["package_name"] as? String
                val root = svc.rootInActiveWindow
                    ?: return@withContext errorResult("No hay ventana activa")
                val sb = StringBuilder()
                collectInteractiveNodes(root, sb, 0, pkg)
                root.recycle()
                val result = sb.toString().trim()
                successResult(if (result.isNotEmpty()) result else "Sin elementos interactivos visibles")
            }

            "find_node" -> withContext(Dispatchers.Main) {
                val text  = args["text"]  as? String
                val hint  = args["hint"]  as? String
                val cls   = args["class_name"] as? String
                val idx   = (args["index"] as? Number)?.toInt() ?: 0

                val root  = svc.rootInActiveWindow
                    ?: return@withContext errorResult("No hay ventana activa")
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                findNodes(root, text, hint, cls, nodes)
                root.recycle()

                if (nodes.isEmpty()) {
                    return@withContext errorResult("No se encontró nodo: text=$text hint=$hint class=$cls")
                }

                val node = nodes.getOrElse(idx) { nodes.last() }
                val rect = Rect(); node.getBoundsInScreen(rect)
                val info = buildString {
                    append("nodo encontrado: ")
                    val t = node.text?.toString()
                    val d = node.contentDescription?.toString()
                    val r = node.viewIdResourceName?.substringAfterLast('/')
                    if (t != null) append("text=\"$t\" ")
                    if (d != null && d != t) append("desc=\"$d\" ")
                    if (r != null) append("res-id=\"$r\" ")
                    append("class=${node.className?.toString()?.substringAfterLast('.')} ")
                    append("bounds=[${rect.left},${rect.top},${rect.right},${rect.bottom}] ")
                    append("clickable=${node.isClickable} editable=${node.isEditable}")
                    if (nodes.size > 1) append(" (${nodes.size} matches, usando idx=$idx)")
                }
                nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                successResult(info)
            }

            "find_and_tap" -> withContext(Dispatchers.Main) {
                val text = args["text"] as? String
                val hint = args["hint"] as? String
                val cls  = args["class_name"] as? String
                val idx  = (args["index"] as? Number)?.toInt() ?: 0

                if (text == null && hint == null && cls == null)
                    return@withContext errorResult("Se requiere text, hint o class_name")

                val root  = svc.rootInActiveWindow
                    ?: return@withContext errorResult("No hay ventana activa")
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                findNodes(root, text, hint, cls, nodes)
                root.recycle()

                if (nodes.isEmpty())
                    return@withContext errorResult("No encontré elemento: text=$text hint=$hint class=$cls")

                val node = nodes.getOrElse(idx) { nodes.last() }
                val rect = Rect(); node.getBoundsInScreen(rect)
                val cx   = (rect.left + rect.right) / 2
                val cy   = (rect.top + rect.bottom) / 2
                val label = text ?: hint ?: cls ?: "nodo"

                // 1) Intentar ACTION_CLICK directo (más fiable en la mayoría de apps)
                var ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                nodes.forEach { try { it.recycle() } catch (_: Exception) {} }

                if (ok) return@withContext successResult("Tocado '$label'")

                // 2) Fallback: gesto de toque real en coordenadas
                if (rect.width() > 0 && rect.height() > 0) {
                    // Intentar con duración 100ms primero, luego 200ms
                    ok = svc.performSwipe(cx.toFloat(), cy.toFloat(), cx.toFloat(), cy.toFloat(), 100)
                    if (!ok) ok = svc.performSwipe(cx.toFloat(), cy.toFloat(), cx.toFloat(), cy.toFloat(), 200)
                    if (ok) successResult("Tocado '$label' en ($cx,$cy) [gesto]")
                    else    errorResult("Toque falló en '$label' ($cx,$cy) — app puede estar bloqueando gestos")
                } else {
                    errorResult("No se pudo tocar '$label' — nodo sin coordenadas visibles")
                }
            }

            "find_and_type" -> withContext(Dispatchers.Main) {
                val searchText = args["text"]  as? String
                val hint       = args["hint"]  as? String
                val typeText   = args["text"]  as? String ?: return@withContext errorResult("text required")
                // Para escribir usamos hint como búsqueda y text como lo que se escribe
                // Si hay hint = buscar por hint, escribir text
                // Si no hay hint = buscar campo editable, escribir text
                val searchBy = hint ?: searchText

                val root  = svc.rootInActiveWindow
                    ?: return@withContext errorResult("No hay ventana activa")
                val nodes = mutableListOf<AccessibilityNodeInfo>()

                if (searchBy != null) {
                    findNodes(root, searchBy, null, "EditText", nodes)
                    if (nodes.isEmpty()) findNodes(root, searchBy, null, null, nodes)
                } else {
                    // Buscar cualquier EditText visible y habilitado
                    findNodes(root, null, null, "EditText", nodes)
                }
                root.recycle()

                if (nodes.isEmpty())
                    return@withContext errorResult("No encontré campo de texto editable")

                val idx  = (args["index"] as? Number)?.toInt() ?: 0
                val node = nodes.getOrElse(idx) { nodes.first() }
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(80)
                val bundle = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, typeText)
                }
                val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                if (ok) successResult("Escrito en campo: \"$typeText\"")
                else    errorResult("No se pudo escribir en el campo")
            }

            "find_and_scroll" -> withContext(Dispatchers.Main) {
                val text = args["text"] as? String
                // "direction" es el parámetro correcto (hint era bug)
                val dirStr = (args["direction"] as? String) ?: (args["hint"] as? String) ?: "down"
                val actionScroll = when (dirStr.lowercase()) {
                    "up"   -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    else   -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                val root = svc.rootInActiveWindow
                    ?: return@withContext errorResult("No hay ventana activa")

                // Si se pasa text, buscar nodo scrollable que CONTENGA ese texto
                // Si no, buscar el primer nodo scrollable disponible
                val scrollNodes = mutableListOf<AccessibilityNodeInfo>()
                findScrollableNodes(root, scrollNodes)
                root.recycle()

                if (scrollNodes.isEmpty()) {
                    // Último recurso: swipe en el centro de la pantalla
                    val wm = AplicacionDoey.instance.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
                    val metrics = android.util.DisplayMetrics()
                    @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
                    val cx = metrics.widthPixels / 2f
                    val cy = metrics.heightPixels / 2f
                    val (y1, y2) = if (dirStr == "up") Pair(cy - 300f, cy + 300f) else Pair(cy + 300f, cy - 300f)
                    val ok = svc.performSwipe(cx, y1, cx, y2, 400)
                    return@withContext if (ok) successResult("Scroll $dirStr (gesto pantalla)")
                    else errorResult("No hay nodo scrollable y el gesto también falló")
                }

                // Intentar ACTION_SCROLL en el nodo más grande (normalmente el RecyclerView principal)
                val best = scrollNodes.maxByOrNull {
                    val r = Rect(); it.getBoundsInScreen(r); r.width() * r.height()
                } ?: scrollNodes.first()

                var ok = best.performAction(actionScroll)
                scrollNodes.forEach { try { it.recycle() } catch (_: Exception) {} }

                if (!ok) {
                    // Fallback: swipe gesture en el centro de pantalla
                    val wm = AplicacionDoey.instance.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
                    val metrics = android.util.DisplayMetrics()
                    @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
                    val cx = metrics.widthPixels / 2f
                    val cy = metrics.heightPixels / 2f
                    val (y1, y2) = if (dirStr == "up") Pair(cy - 400f, cy + 400f) else Pair(cy + 400f, cy - 400f)
                    ok = svc.performSwipe(cx, y1, cx, y2, 400)
                }

                if (ok) successResult("Scroll $dirStr realizado")
                else    errorResult("Scroll $dirStr falló en todos los intentos")
            }

            "tap_xy" -> withContext(Dispatchers.Main) {
                val x   = (args["x"] as? Number)?.toFloat() ?: return@withContext errorResult("x required")
                val y   = (args["y"] as? Number)?.toFloat() ?: return@withContext errorResult("y required")
                val ok  = svc.performSwipe(x, y, x, y, 50)
                if (ok) successResult("Toque en ($x,$y)") else errorResult("Toque falló en ($x,$y)")
            }

            "long_tap_xy" -> withContext(Dispatchers.Main) {
                val x  = (args["x"] as? Number)?.toFloat() ?: return@withContext errorResult("x required")
                val y  = (args["y"] as? Number)?.toFloat() ?: return@withContext errorResult("y required")
                val dur = (args["duration_ms"] as? Number)?.toLong() ?: 800L
                val ok  = svc.performSwipe(x, y, x, y, dur)
                if (ok) successResult("Pulsación larga en ($x,$y)") else errorResult("Fallo pulsación larga")
            }

            "double_tap" -> withContext(Dispatchers.Main) {
                val text = args["text"] as? String
                val x    = (args["x"] as? Number)?.toFloat()
                val y    = (args["y"] as? Number)?.toFloat()

                if (x != null && y != null) {
                    svc.performSwipe(x, y, x, y, 50)
                    Thread.sleep(80)
                    val ok = svc.performSwipe(x, y, x, y, 50)
                    if (ok) successResult("Doble toque en ($x,$y)") else errorResult("Doble toque falló")
                } else if (text != null) {
                    val root  = svc.rootInActiveWindow ?: return@withContext errorResult("Sin ventana activa")
                    val nodes = mutableListOf<AccessibilityNodeInfo>()
                    findNodes(root, text, null, null, nodes)
                    root.recycle()
                    if (nodes.isEmpty()) return@withContext errorResult("No encontré: $text")
                    val rect = Rect(); nodes.first().getBoundsInScreen(rect)
                    val cx = (rect.left + rect.right) / 2f
                    val cy = (rect.top  + rect.bottom) / 2f
                    nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                    svc.performSwipe(cx, cy, cx, cy, 50)
                    Thread.sleep(80)
                    val ok = svc.performSwipe(cx, cy, cx, cy, 50)
                    if (ok) successResult("Doble toque en '$text'") else errorResult("Doble toque falló")
                } else {
                    errorResult("Se requiere text o x+y")
                }
            }

            "pinch" -> withContext(Dispatchers.Main) {
                // Gesto de pellizco: dos dedos que se juntan (zoom out)
                val cx  = (args["x"]  as? Number)?.toFloat() ?: 540f
                val cy  = (args["y"]  as? Number)?.toFloat() ?: 960f
                val dur = (args["duration_ms"] as? Number)?.toLong() ?: 400L
                val dist = 300f
                // Simular con dos swipes en paralelo (limitación: AccessibilityService no soporta multi-touch real)
                // Usamos swipe como aproximación
                val ok1 = svc.performSwipe(cx - dist, cy, cx, cy, dur)
                val ok2 = svc.performSwipe(cx + dist, cy, cx, cy, dur)
                if (ok1 || ok2) successResult("Gesto pinch en ($cx,$cy)")
                else errorResult("Gesto pinch falló — puede requerir ShellTool para multi-touch real")
            }

            "spread" -> withContext(Dispatchers.Main) {
                // Gesto de expansión: dos dedos que se abren (zoom in)
                val cx  = (args["x"]  as? Number)?.toFloat() ?: 540f
                val cy  = (args["y"]  as? Number)?.toFloat() ?: 960f
                val dur = (args["duration_ms"] as? Number)?.toLong() ?: 400L
                val dist = 300f
                val ok1 = svc.performSwipe(cx, cy, cx - dist, cy, dur)
                val ok2 = svc.performSwipe(cx, cy, cx + dist, cy, dur)
                if (ok1 || ok2) successResult("Gesto spread/zoom en ($cx,$cy)")
                else errorResult("Gesto spread falló")
            }

            "drag" -> withContext(Dispatchers.Main) {
                val x1  = (args["x"]  as? Number)?.toFloat() ?: return@withContext errorResult("x required")
                val y1  = (args["y"]  as? Number)?.toFloat() ?: return@withContext errorResult("y required")
                val x2  = (args["x2"] as? Number)?.toFloat() ?: return@withContext errorResult("x2 required")
                val y2  = (args["y2"] as? Number)?.toFloat() ?: return@withContext errorResult("y2 required")
                val dur = (args["duration_ms"] as? Number)?.toLong() ?: 600L
                val ok  = svc.performSwipe(x1, y1, x2, y2, dur)
                if (ok) successResult("Arrastrado de ($x1,$y1) a ($x2,$y2)")
                else errorResult("Arrastre falló")
            }

            "scroll_to_text" -> withContext(Dispatchers.Main) {
                val target  = args["text"] as? String ?: return@withContext errorResult("text required")
                val maxTries = 8
                var found = false
                for (i in 0 until maxTries) {
                    val root = svc.rootInActiveWindow ?: break
                    val nodes = mutableListOf<AccessibilityNodeInfo>()
                    findNodes(root, target, null, null, nodes)
                    root.recycle()
                    if (nodes.isNotEmpty()) {
                        nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                        found = true
                        break
                    }
                    // Hacer scroll hacia abajo para buscar
                    val r2 = svc.rootInActiveWindow ?: break
                    val scrollables = mutableListOf<AccessibilityNodeInfo>()
                    findScrollableNodes(r2, scrollables)
                    r2.recycle()
                    if (scrollables.isEmpty()) break
                    scrollables.first().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    scrollables.forEach { try { it.recycle() } catch (_: Exception) {} }
                    Thread.sleep(300)
                }
                if (found) successResult("Texto '$target' encontrado en pantalla")
                else errorResult("No se encontró '$target' después de $maxTries scrolls")
            }

            "wait_ms" -> {
                val ms = (args["duration_ms"] as? Number)?.toLong()?.coerceIn(100, 5000) ?: 1500L
                withContext(Dispatchers.IO) { Thread.sleep(ms) }
                successResult("Esperado ${ms}ms")
            }

            else -> errorResult("Acción desconocida: $action")
        }
    }

    // ── Helpers de búsqueda de nodos ─────────────────────────────────────────

    private fun findNodes(
        node: AccessibilityNodeInfo,
        text: String?,
        hint: String?,
        className: String?,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.packageName?.toString() == "com.doey") return

        val nodeText  = node.text?.toString() ?: ""
        val nodeDesc  = node.contentDescription?.toString() ?: ""
        val nodeRes   = node.viewIdResourceName?.substringAfterLast('/') ?: ""
        val nodeCls   = node.className?.toString()?.substringAfterLast('.') ?: ""

        val textMatch  = text == null || nodeText.contains(text, ignoreCase = true)
                       || nodeDesc.contains(text, ignoreCase = true)
        val hintMatch  = hint == null || nodeRes.contains(hint, ignoreCase = true)
                       || nodeText.contains(hint, ignoreCase = true)
                       || nodeDesc.contains(hint, ignoreCase = true)
        val classMatch = className == null || nodeCls.equals(className, ignoreCase = true)
                       || node.className?.toString()?.endsWith(className, ignoreCase = true) == true

        if (textMatch && hintMatch && classMatch && node.isVisibleToUser) {
            try { results.add(AccessibilityNodeInfo.obtain(node)) } catch (_: Exception) {}
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodes(child, text, hint, className, results)
            child.recycle()
        }
    }

    private fun findScrollableNodes(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isScrollable && node.isVisibleToUser) {
            try { results.add(AccessibilityNodeInfo.obtain(node)) } catch (_: Exception) {}
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollableNodes(child, results)
            child.recycle()
        }
    }

    /** Árbol comprimido — solo nodos interactivos o con texto relevante */
    private fun collectInteractiveNodes(
        node: AccessibilityNodeInfo,
        sb: StringBuilder,
        depth: Int,
        pkgFilter: String?
    ) {
        if (node.packageName?.toString() == "com.doey") return
        if (pkgFilter != null && node.packageName?.toString() != pkgFilter) {
            // Seguir bajando para encontrar la ventana correcta
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectInteractiveNodes(child, sb, depth, pkgFilter)
                child.recycle()
            }
            return
        }

        val isInteresting = node.isClickable || node.isLongClickable ||
                            node.isEditable  || node.isScrollable   ||
                            (node.text?.isNotBlank() == true && depth < 8)

        if (isInteresting && node.isVisibleToUser) {
            val indent  = "  ".repeat(depth.coerceAtMost(6))
            val cls     = node.className?.toString()?.substringAfterLast('.') ?: "View"
            val text    = node.text?.toString()?.take(60) ?: ""
            val desc    = node.contentDescription?.toString()?.take(60) ?: ""
            val resId   = node.viewIdResourceName?.substringAfterLast('/') ?: ""
            val rect    = Rect(); node.getBoundsInScreen(rect)
            val attrs   = buildString {
                if (text.isNotBlank()) append("\"$text\"")
                if (desc.isNotBlank() && desc != text) append(" desc=\"$desc\"")
                if (resId.isNotBlank()) append(" id=$resId")
                if (node.isClickable)  append(" [tap]")
                if (node.isEditable)   append(" [edit]")
                if (node.isScrollable) append(" [scroll]")
                if (node.isChecked)    append(" [✓]")
                if (!node.isEnabled)   append(" [off]")
                append(" (${rect.left},${rect.top})")
            }
            sb.appendLine("$indent$cls $attrs")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectInteractiveNodes(child, sb, depth + 1, pkgFilter)
            child.recycle()
        }
    }
}
