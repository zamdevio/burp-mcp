package net.portswigger.mcp.repeater

import java.awt.Component
import java.lang.reflect.Method
import javax.swing.text.Document
import javax.swing.text.JTextComponent

/**
 * Obfuscated Burp text views (e.g. `Zwlc`) for [RepeaterNotesScanner] debug scans only —
 * not used for production get/set notes (see [RepeaterTabNotes]).
 */
internal object RepeaterNotesReflective {

    interface TextProbe {
        val component: Component
        fun readText(): String
    }

    fun reflectiveHandle(view: Component): TextProbe? {
        handleFromDocument(view)?.let { return it }
        handleFromGetSetPair(view)?.let { return it }
        if (view is JTextComponent && view.width >= 40 && view.height >= 24) {
            return textComponentProbe(view)
        }
        var nested: TextProbe? = null
        RepeaterSwingTree.walk(view) { child ->
            if (child === view || nested != null) return@walk
            when (child) {
                is JTextComponent -> {
                    if (child.width >= 40 && child.height >= 24) {
                        nested = textComponentProbe(child)
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

    fun textComponentProbe(text: JTextComponent): TextProbe =
        object : TextProbe {
            override val component: Component = text
            override fun readText(): String = text.text.orEmpty()
        }

    private fun handleFromDocument(view: Any): TextProbe? {
        val getDoc = findMethod(view, "getDocument") ?: return null
        val component = view as? Component ?: return null
        return object : TextProbe {
            override val component: Component = component
            override fun readText(): String {
                val doc = getDoc.invoke(view) as? Document ?: return ""
                return doc.getText(0, doc.length).trim()
            }
        }
    }

    private fun handleFromGetSetPair(view: Any): TextProbe? {
        val pair = discoverGetSetPair(view) ?: return null
        val getter = pair.first
        val component = view as? Component ?: return null
        return object : TextProbe {
            override val component: Component = component
            override fun readText(): String = runCatching { getter.invoke(view)?.toString().orEmpty() }.getOrDefault("")
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
}
