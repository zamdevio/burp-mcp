package net.portswigger.mcp.repeater

import java.lang.reflect.InvocationTargetException
import javax.swing.SwingUtilities

/**
 * Runs [block] on the Swing EDT and returns its result.
 * Safe if already on the EDT (runs inline). Unwraps [InvocationTargetException].
 */
internal fun <T> runOnEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var result: Result<T>? = null
    try {
        SwingUtilities.invokeAndWait {
            result = runCatching(block)
        }
    } catch (e: InvocationTargetException) {
        throw e.cause ?: e
    }

    return result!!.getOrThrow()
}
