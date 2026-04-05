package com.doey.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "DoeyAccessibility"

class DoeyAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: DoeyAccessibilityService? = null
        fun isRunning() = instance != null
    }

    private val nodeMap = ConcurrentHashMap<String, AccessibilityNodeInfo>()
    private val nodeCounter = AtomicInteger(0)

    @Volatile
    private var currentPackageName: String = ""

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = (AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
        serviceInfo = info
    }

    override fun onInterrupt() { Log.w(TAG, "Service interrupted") }

    override fun onDestroy() {
        clearNodeMap()
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName
        if (pkg != null && pkg.isNotBlank()) {
            currentPackageName = pkg.toString()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Build a human-readable text representation of the current UI tree.
     * Retries up to 5 times with increasing delays for slow-rendering apps.
     */
    fun buildAccessibilityTree(): String {
        val retryDelays = longArrayOf(50, 100, 200, 300, 500)

        for (attempt in retryDelays.indices) {
            clearNodeMap()
            val root = rootInActiveWindow

            if (root != null) {
                val sb = StringBuilder()
                try {
                    traverseNode(root, sb, 0)
                    root.recycle()
                } catch (e: Exception) {
                    Log.w(TAG, "Tree traversal error: ${e.message}")
                    try { root.recycle() } catch (_: Exception) {}
                }
                val result = sb.toString().trim()
                if (result.isNotEmpty()) return result
            }

            if (attempt < retryDelays.size - 1) {
                Thread.sleep(retryDelays[attempt])
            }
        }
        return "No active window found. Make sure the target app is open and in the foreground."
    }

    private fun traverseNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (node.packageName?.toString() == "com.doey") return

        val nodeId = "node_${nodeCounter.getAndIncrement()}"
        val indent = "  ".repeat(depth)

        try {
            nodeMap[nodeId] = AccessibilityNodeInfo.obtain(node)
        } catch (e: Exception) {
            Log.w(TAG, "Could not obtain node copy: ${e.message}")
        }

        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val text = node.text?.toString()?.take(120) ?: ""
        val contentDesc = node.contentDescription?.toString()?.take(120) ?: ""
        val viewId = node.viewIdResourceName?.substringAfterLast('/') ?: ""

        val attrs = mutableListOf<String>()
        if (text.isNotBlank()) attrs.add("text=\"$text\"")
        if (contentDesc.isNotBlank() && contentDesc != text) attrs.add("desc=\"$contentDesc\"")
        if (viewId.isNotBlank()) attrs.add("res-id=\"$viewId\"")
        if (node.isClickable) attrs.add("clickable")
        if (node.isLongClickable) attrs.add("long-clickable")
        if (node.isFocusable) attrs.add("focusable")
        if (node.isEditable) attrs.add("editable")
        if (node.isScrollable) attrs.add("scrollable")
        if (node.isChecked) attrs.add("checked")
        if (node.isSelected) attrs.add("selected")
        if (!node.isEnabled) attrs.add("disabled")

        val attrStr = if (attrs.isNotEmpty()) " [${attrs.joinToString(", ")}]" else ""
        sb.appendLine("$indent[$nodeId] $className$attrStr")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, sb, depth + 1)
            child.recycle()
        }
    }

    /**
     * Perform an action on a node or a global action.
     * Returns true on success, false on failure.
     */
    fun performNodeAction(action: String, nodeId: String?, text: String?): Boolean {
        // Global actions
        when (action) {
            "back" -> { performGlobalAction(GLOBAL_ACTION_BACK); return true }
            "home" -> { performGlobalAction(GLOBAL_ACTION_HOME); return true }
            "recents" -> { performGlobalAction(GLOBAL_ACTION_RECENTS); return true }
            "screenshot" -> {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else false
            }
            "clipboard_set" -> {
                if (text == null) return false
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("doey", text))
                return true
            }
            "clipboard_get" -> return true // handled differently - see AccessibilityTool
            "paste" -> {
                val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null) {
                    return try {
                        focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    } finally { focused.recycle() }
                }
                return false
            }
        }

        if (nodeId == null) return false
        val node = nodeMap[nodeId] ?: return false

        return when (action) {
            "click" -> {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    val cx = (rect.left + rect.right) / 2
                    val cy = (rect.top + rect.bottom) / 2
                    dispatchTapGestureSync(cx, cy)
                } else {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            "long_click" -> {
                val ok = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                if (!ok) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0) {
                        dispatchLongPressGestureSync((rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2)
                    } else false
                } else true
            }
            "type" -> {
                if (text == null) return false
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val bundle = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            }
            "clear" -> {
                val bundle = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            }
            "scroll_down", "scroll_forward" -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            "scroll_up", "scroll_backward" -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            "scroll_left" -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            "scroll_right" -> node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            "focus" -> node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            else -> false
        }
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        return dispatchSwipeGestureSync(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt(), durationMs.coerceAtLeast(1))
    }

    fun waitForPackage(packageName: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (currentPackageName == packageName) return true
            try {
                val root = rootInActiveWindow
                if (root != null) {
                    val pkg = root.packageName?.toString()
                    root.recycle()
                    if (pkg == packageName) {
                        currentPackageName = packageName
                        return true
                    }
                }
            } catch (_: Exception) {}
            Thread.sleep(100)
        }
        return currentPackageName == packageName
    }

    // ── Gesture Helpers ───────────────────────────────────────────────────────

    private fun dispatchTapGestureSync(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSync(gesture)
    }

    private fun dispatchLongPressGestureSync(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 1000)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSync(gesture)
    }

    private fun dispatchSwipeGestureSync(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureSync(gesture)
    }

    private fun dispatchGestureSync(gesture: GestureDescription): Boolean {
        val latch = CountDownLatch(1)
        var succeeded = false

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { succeeded = true; latch.countDown() }
            override fun onCancelled(g: GestureDescription?) { latch.countDown() }
        }, null)

        if (!dispatched) return false
        latch.await(3, TimeUnit.SECONDS)
        return succeeded
    }

    private fun clearNodeMap() {
        nodeMap.values.forEach { try { it.recycle() } catch (_: Exception) {} }
        nodeMap.clear()
        nodeCounter.set(0)
    }
}
