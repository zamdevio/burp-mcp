package net.portswigger.mcp.repeater

import burp.api.montoya.http.message.HttpRequestResponse
import java.awt.Component
import java.awt.Container
import java.lang.reflect.Field
import javax.swing.JTabbedPane

/**
 * Per-tab Repeater notes via Montoya [burp.api.montoya.core.Annotations] on the tab's
 * [HttpRequestResponse]. Burp 2026.x binds this through an internal tab bundle (jar spike:
 * `burp.Zemd` → `HttpRequestResponse`); we locate that bundle from the live Swing tree
 * without a Repeater registry.
 */
internal object RepeaterTabAnnotations {

    /** Internal tab bundle type (2026.8); located by class name only — no decompiled sources in git. */
    private const val TAB_BUNDLE_CLASS = "burp.Zemd"

    private const val MAX_GRAPH_DEPTH = 16

    fun findForTab(discovered: RepeaterUiDiscovery.DiscoveredRepeater, index: Int): Any? {
        val root = tabContentAt(discovered.tabStrip, index) ?: return null
        val matches = mutableListOf<Any>()
        collectZemdInTabSubtree(root, matches)
        val distinct = matches.distinctBy { System.identityHashCode(it) }
        return when (distinct.size) {
            1 -> distinct.single()
            else -> null
        }
    }

    fun readNotes(tabBundle: Any): String? =
        httpRequestResponse(tabBundle)?.annotations()?.notes()

    fun writeNotes(tabBundle: Any, notes: String): Boolean {
        val hrr = httpRequestResponse(tabBundle) ?: return false
        return runCatching {
            hrr.annotations().setNotes(notes)
            true
        }.getOrDefault(false)
    }

    internal fun httpRequestResponse(tabBundle: Any): HttpRequestResponse? {
        val methods = tabBundle.javaClass.methods.filter {
            it.parameterCount == 0 && HttpRequestResponse::class.java.isAssignableFrom(it.returnType)
        }
        if (methods.size != 1) return null
        return runCatching {
            methods.single().apply { isAccessible = true }.invoke(tabBundle) as HttpRequestResponse
        }.getOrNull()
    }

    internal fun directZemdField(obj: Any): Any? {
        if (obj.javaClass.name == TAB_BUNDLE_CLASS) return obj
        if (!obj.javaClass.name.startsWith("burp.")) return null
        for (field in burpDeclaredFields(obj.javaClass)) {
            if (field.type.name != TAB_BUNDLE_CLASS) continue
            runCatching { field.get(obj) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun collectZemdInTabSubtree(root: Component, out: MutableList<Any>) {
        val seen = mutableSetOf<Int>()
        val queue = ArrayDeque<Pair<Any, Int>>()
        queue.add(root to 0)
        while (queue.isNotEmpty()) {
            val (obj, depth) = queue.removeFirst()
            val id = System.identityHashCode(obj)
            if (!seen.add(id)) continue
            if (depth > MAX_GRAPH_DEPTH) continue

            directZemdField(obj)?.let { out += it }

            if (obj.javaClass.name.startsWith("burp.")) {
                for (field in burpDeclaredFields(obj.javaClass)) {
                    val value = runCatching { field.get(obj) }.getOrNull() ?: continue
                    if (value.javaClass.name == TAB_BUNDLE_CLASS) {
                        out += value
                    } else if (shouldTraverseField(value)) {
                        queue.add(value to depth + 1)
                    }
                }
            }

            if (obj is Container) {
                for (child in obj.components) {
                    queue.add(child to depth + 1)
                }
            }
        }
    }

    private fun shouldTraverseField(value: Any): Boolean {
        if (value is Component) return false
        val name = value.javaClass.name
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("kotlin.")) {
            return false
        }
        if (name.startsWith("javafx.") || name.startsWith("com.sun.")) return false
        return name.startsWith("burp.")
    }

    /** Only `burp.*` declared fields — never touch Swing superclasses (module access). */
    private fun burpDeclaredFields(cls: Class<*>): List<Field> {
        val out = mutableListOf<Field>()
        var c: Class<*>? = cls
        while (c != null && c.name.startsWith("burp.")) {
            for (field in c.declaredFields) {
                runCatching { field.isAccessible = true }.onSuccess { out += field }
            }
            c = c.superclass
        }
        return out
    }

    private fun tabContentAt(pane: JTabbedPane, index: Int): Component? =
        if (index in 0 until pane.tabCount) pane.getComponentAt(index) else null
}
