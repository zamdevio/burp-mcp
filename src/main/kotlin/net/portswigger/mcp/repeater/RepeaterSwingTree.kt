package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import javax.swing.JTabbedPane

/** Shared Swing tree helpers for Repeater UI discovery (not a tab registry). */
internal object RepeaterSwingTree {

    fun isUnderComponent(component: Component, ancestor: Component): Boolean {
        var cur: Component? = component
        while (cur != null) {
            if (cur === ancestor) return true
            cur = cur.parent
        }
        return false
    }

    fun looksLikeHttpRequest(text: String): Boolean {
        val line = text.lineSequence().firstOrNull()?.trim()?.uppercase() ?: return false
        if (line.startsWith("HTTP/")) return false
        return HTTP_METHODS.any { line.startsWith("$it ") }
    }

    fun looksLikeHttpResponse(text: String): Boolean =
        text.trimStart().uppercase().startsWith("HTTP/")

    fun tabContentAt(pane: JTabbedPane, index: Int): Component? =
        if (index in 0 until pane.tabCount) pane.getComponentAt(index) else null

    fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            if (root is JTabbedPane) {
                for (i in 0 until root.tabCount) {
                    tabContentAt(root, i)?.let { walk(it, visit) }
                }
                for (child in root.components) {
                    if ((0 until root.tabCount).none { tabContentAt(root, it) === child }) {
                        walk(child, visit)
                    }
                }
            } else {
                for (child in root.components) {
                    walk(child, visit)
                }
            }
        }
    }

    fun collectTabbedPanes(root: Component): List<JTabbedPane> {
        val out = mutableListOf<JTabbedPane>()
        walk(root) { if (it is JTabbedPane) out += it }
        return out
    }

    private val HTTP_METHODS = listOf(
        "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT",
    )
}
