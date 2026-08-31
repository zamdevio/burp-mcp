package net.portswigger.mcp.repeater

import java.awt.Component
import java.lang.reflect.Method
import javax.swing.JScrollPane
import javax.swing.text.Document
import javax.swing.text.JTextComponent

/**
 * Burp Pro builds use obfuscated note editors (e.g. view class `Zal6`) inside [JScrollPane],
 * often with [Component.isShowing] false while the panel is visibly open.
 */
internal object RepeaterNotesReflective {

    fun reflectiveHandle(view: Component): RepeaterNotes.NotesEditorHandle? {
        handleFromDocument(view)?.let { return it }
        handleFromGetSetPair(view)?.let { return it }
        if (view is JTextComponent && view.width >= 40 && view.height >= 24) {
            return RepeaterNotes.textComponentHandle(view)
        }
        var nested: RepeaterNotes.NotesEditorHandle? = null
        walk(view) { child ->
            if (child === view || nested != null) return@walk
            when (child) {
                is JTextComponent -> {
                    if (child.width >= 40 && child.height >= 24) {
                        nested = RepeaterNotes.textComponentHandle(child)
                    }
                }
                else -> {
                    handleFromDocument(child)?.let { nested = it }
                    if (nested == null) {
                        handleFromGetSetPair(child)?.let { nested = it }
                    }
                }
            }
        }
        return nested
    }

    fun findLargeScrollEditor(
        searchRoot: Component,
        tabStrip: javax.swing.JTabbedPane,
        anchor: Component,
        messageEditors: RepeaterUiDiscovery.TabEditors?,
        maxScrollWidth: Int = 900,
    ): RepeaterNotes.NotesEditorHandle? {
        val scrolls = mutableListOf<JScrollPane>()
        walk(searchRoot) { c ->
            val scroll = c as? JScrollPane ?: return@walk
            if (RepeaterNotes.isUnderComponent(scroll, tabStrip)) return@walk
            if (scroll.width < 120 || scroll.height < 72) return@walk
            if (scroll.width > maxScrollWidth) return@walk
            scrolls += scroll
        }
        val ranked = scrolls.mapNotNull { scroll ->
            val view = scroll.viewport?.view as? Component ?: return@mapNotNull null
            val handle = reflectiveHandle(view) ?: return@mapNotNull null
            if (messageEditors != null) {
                val sample = runCatching { handle.readText() }.getOrDefault("")
                if (RepeaterNotes.looksLikeHttpRequest(sample) || RepeaterNotes.looksLikeHttpResponse(sample)) {
                    return@mapNotNull null
                }
            }
            handle to scoreScroll(scroll, view, anchor)
        }.sortedByDescending { it.second }
        return ranked.firstOrNull()?.first
    }

    /** For diagnostics when discovery fails. */
    fun describeScrollViews(
        repeaterRoot: Component,
        tabStrip: javax.swing.JTabbedPane,
        suiteFrame: Component,
    ): String {
        val parts = mutableListOf<String>()
        for (root in listOf(repeaterRoot, suiteFrame).distinct()) {
            walk(root) { c ->
                val scroll = c as? JScrollPane ?: return@walk
                if (RepeaterNotes.isUnderComponent(scroll, tabStrip)) return@walk
                if (scroll.width < 120 || scroll.height < 72) return@walk
                if (scroll.width > 900) return@walk
                val view = scroll.viewport?.view ?: return@walk
                val methods = view.javaClass.methods
                    .map { it.name }
                    .filter { it.startsWith("get") || it.startsWith("set") }
                    .distinct()
                    .sorted()
                    .take(8)
                    .joinToString(",")
                parts += "Scroll ${scroll.width}x${scroll.height} view=${view.javaClass.simpleName} methods=[$methods]"
            }
        }
        return parts.take(3).joinToString(" | ")
    }

    private fun handleFromDocument(view: Any): RepeaterNotes.NotesEditorHandle? {
        val getDoc = findMethod(view, "getDocument") ?: return null
        val component = view as? Component ?: return null
        return object : RepeaterNotes.NotesEditorHandle {
            override val component: Component = component
            override fun readText(): String {
                val doc = getDoc.invoke(view) as? Document ?: return ""
                return doc.getText(0, doc.length).trim()
            }
            override fun writeText(text: String) {
                val doc = getDoc.invoke(view) as? Document ?: return
                doc.remove(0, doc.length)
                doc.insertString(0, text, null)
            }
            override fun ensureWritable(): Boolean {
                findMethod(view, "setEditable", Boolean::class.javaPrimitiveType!!)?.let { m ->
                    runCatching { m.invoke(view, true) }
                }
                return true
            }
            override fun isVisibleEnough(): Boolean = burpNotesScrollVisibleEnough(component)
        }
    }

    private fun handleFromGetSetPair(view: Any): RepeaterNotes.NotesEditorHandle? {
        val pair = discoverGetSetPair(view) ?: return null
        val (getter, setter) = pair
        val component = view as? Component ?: return null
        return object : RepeaterNotes.NotesEditorHandle {
            override val component: Component = component
            override fun readText(): String = runCatching { getter.invoke(view)?.toString().orEmpty() }.getOrDefault("")
            override fun writeText(text: String) {
                runCatching {
                    when (setter.parameterTypes[0]) {
                        CharSequence::class.java -> setter.invoke(view, text as CharSequence)
                        else -> setter.invoke(view, text)
                    }
                }
            }
            override fun ensureWritable(): Boolean = true
            override fun isVisibleEnough(): Boolean = burpNotesScrollVisibleEnough(component)
        }
    }

    private fun discoverGetSetPair(target: Any): Pair<Method, Method>? {
        for (method in allMethods(target)) {
            if (method.parameterCount != 0 || !String::class.java.isAssignableFrom(method.returnType)) continue
            if (!method.name.startsWith("get") && !method.name.startsWith("is")) continue
            val prop = when {
                method.name.startsWith("get") -> method.name.removePrefix("get")
                else -> method.name.removePrefix("is")
            }
            if (prop.isEmpty()) continue
            val setter = allMethods(target).find { m ->
                m.name == "set$prop" && m.parameterCount == 1 &&
                    (m.parameterTypes[0] == String::class.java || m.parameterTypes[0] == CharSequence::class.java)
            } ?: continue
            method.isAccessible = true
            setter.isAccessible = true
            return method to setter
        }
        return null
    }

    private fun scoreScroll(scroll: JScrollPane, view: Component, anchor: Component): Int {
        var score = scroll.width * scroll.height
        val rootPoint = runCatching { anchor.locationOnScreen }.getOrNull()
        val scrollPoint = runCatching { scroll.locationOnScreen }.getOrNull()
        if (rootPoint != null && scrollPoint != null) {
            if (scrollPoint.x >= rootPoint.x + anchor.width / 2) score += 500_000
        }
        if (view.javaClass.simpleName.length <= 6 && view.javaClass.simpleName.firstOrNull()?.isUpperCase() == true) {
            score += 100_000
        }
        return score
    }

    private fun burpNotesScrollVisibleEnough(view: Component): Boolean {
        var cur: Component? = view
        while (cur != null) {
            if (cur is JScrollPane && cur.width >= 180 && cur.height >= 72) {
                return true
            }
            cur = cur.parent
        }
        return view.width >= 80 && view.height >= 40
    }

    private fun allMethods(target: Any): List<Method> {
        val out = mutableListOf<Method>()
        var cls: Class<*>? = target.javaClass
        while (cls != null) {
            out += cls.declaredMethods
            cls = cls.superclass
        }
        return out
    }

    private fun findMethod(target: Any, name: String, vararg params: Class<*>): Method? =
        runCatching {
            target.javaClass.getMethod(name, *params).also { it.isAccessible = true }
        }.getOrNull()
            ?: allMethods(target).find { m ->
                m.name == name && m.parameterCount == params.size &&
                    params.indices.all { i -> m.parameterTypes[i].isAssignableFrom(params[i]) }
            }?.also { it.isAccessible = true }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is java.awt.Container) {
            for (child in root.components) {
                walk(child, visit)
            }
        }
    }
}
