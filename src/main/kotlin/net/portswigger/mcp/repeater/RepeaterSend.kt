package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Container
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.JLabel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.text.JTextComponent

/**
 * Repeater Send for Burp builds where **only the toolbar Send button works**
 * (no Enter / Ctrl+Enter). Finds the visible Send control (often Cancel-adjacent),
 * **blocks** when Target is "Not specified" (Host header alone is not enough),
 * dismisses a leftover Configure-target dialog, then activates via doClick and/or AWT Robot.
 */
internal object RepeaterSend {

    const val DEFAULT_RESPONSE_TIMEOUT_MS: Long = 30_000L
    const val RESPONSE_POLL_MS: Long = 100L

    fun sendTab(api: MontoyaApi, tabId: String): RepeaterUiDiscovery.Outcome<String> {
        return when (val resolved = resolveForSend(api, tabId)) {
            is RepeaterUiDiscovery.Outcome.Err -> resolved
            is RepeaterUiDiscovery.Outcome.Ok -> {
                val ctx = resolved.value
                when (val clicked = sendSelectedTab(ctx.discovered, ctx.index)) {
                    is RepeaterUiDiscovery.Outcome.Err -> clicked
                    is RepeaterUiDiscovery.Outcome.Ok ->
                        RepeaterUiDiscovery.Outcome.Ok("Sent Repeater tab $tabId (${clicked.value})")
                }
            }
        }
    }

    fun sendTabAndGetResponse(
        api: MontoyaApi,
        tabId: String,
        timeoutMs: Long = DEFAULT_RESPONSE_TIMEOUT_MS,
    ): RepeaterUiDiscovery.Outcome<String> {
        val budget = timeoutMs.coerceIn(1_000L, 120_000L)
        return when (val resolved = resolveForSend(api, tabId)) {
            is RepeaterUiDiscovery.Outcome.Err -> resolved
            is RepeaterUiDiscovery.Outcome.Ok -> {
                val ctx = resolved.value
                val snapshot = runOnEdt { RepeaterTabSession.capture(ctx.discovered) }
                val beforeFingerprint = runOnEdt {
                    RepeaterTabSession.select(ctx.discovered, ctx.index)
                    RepeaterTabSession.settleEditors(ctx.discovered, ctx.index)
                    responseFingerprint(ctx.discovered, ctx.index)
                }
                when (val clicked = sendSelectedTab(ctx.discovered, ctx.index, alreadySelected = true)) {
                    is RepeaterUiDiscovery.Outcome.Err -> {
                        runOnEdt { RepeaterTabSession.restore(ctx.discovered, snapshot) }
                        return clicked
                    }
                    is RepeaterUiDiscovery.Outcome.Ok -> Unit
                }
                try {
                    waitForResponse(ctx.discovered, ctx.index, beforeFingerprint, budget)
                } finally {
                    runOnEdt { RepeaterTabSession.restore(ctx.discovered, snapshot) }
                }
            }
        }
    }

    data class SendContext(
        val discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        val index: Int,
    )

    /**
     * Select → settle → ensure target → click Send → restore (unless [alreadySelected]).
     * Robot clicks run off the EDT to avoid AWT deadlocks.
     */
    fun sendSelectedTab(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        alreadySelected: Boolean = false,
    ): RepeaterUiDiscovery.Outcome<String> {
        val prepared = runOnEdt {
            val snapshot =
                if (alreadySelected) null else RepeaterTabSession.capture(discovered)
            if (!alreadySelected) {
                RepeaterTabSession.select(discovered, index)
                RepeaterTabSession.settleEditors(discovered, index)
            }
            when (val targetOk = requireTargetPresent(discovered, index)) {
                is RepeaterUiDiscovery.Outcome.Err -> return@runOnEdt PreparedSend(snapshot, targetOk)
                is RepeaterUiDiscovery.Outcome.Ok -> Unit
            }
            val control = resolveSendControl(discovered)
            PreparedSend(snapshot, control)
        }

        if (prepared.control is RepeaterUiDiscovery.Outcome.Err) {
            restoreAfterSend(discovered, prepared.snapshot, alreadySelected)
            return prepared.control
        }

        val control = (prepared.control as RepeaterUiDiscovery.Outcome.Ok).value
        val how = activateSend(control)

        // If Send still opened Configure target details, dismiss and fail (do not leave the modal up).
        val dialogOpened = runOnEdt {
            val open = isConfigureTargetDialogShowing()
            if (open) dismissConfigureTargetDialogs()
            open
        }
        if (dialogOpened) {
            restoreAfterSend(discovered, prepared.snapshot, alreadySelected)
            return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.TargetMissing())
        }

        restoreAfterSend(discovered, prepared.snapshot, alreadySelected)
        return RepeaterUiDiscovery.Outcome.Ok(how)
    }

    private fun restoreAfterSend(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        snapshot: RepeaterTabSession.UiSnapshot?,
        alreadySelected: Boolean,
    ) {
        if (alreadySelected) return
        snapshot?.let { snap -> runOnEdt { RepeaterTabSession.restore(discovered, snap) } }
    }

    private data class PreparedSend(
        val snapshot: RepeaterTabSession.UiSnapshot?,
        val control: RepeaterUiDiscovery.Outcome<Component>,
    )

    fun resolveForSend(api: MontoyaApi, tabId: String): RepeaterUiDiscovery.Outcome<SendContext> {
        val index = RepeaterTabId.parseIndex(tabId)
            ?: return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.InvalidTabId(tabId))
        return when (val discovered = runOnEdt { RepeaterUiDiscovery.discoverRepeater(api) }) {
            is RepeaterUiDiscovery.Outcome.Err -> discovered
            is RepeaterUiDiscovery.Outcome.Ok -> {
                val strip = discovered.value.tabStrip
                if (index >= strip.tabCount) {
                    RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.TabNotFound(tabId))
                } else {
                    RepeaterUiDiscovery.Outcome.Ok(SendContext(discovered.value, index))
                }
            }
        }
    }

    fun resolveSendControl(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
    ): RepeaterUiDiscovery.Outcome<Component> {
        val roots = listOf(discovered.repeaterRoot, discovered.suiteTabbedPane)

        findSendNearCancel(roots)?.let { return RepeaterUiDiscovery.Outcome.Ok(it) }

        val visible = findVisibleSendControls(roots)
        val best = pickBestSend(visible)
        val hint = describeSendCandidates(roots).take(8).joinToString("; ")
        return when {
            best == null ->
                RepeaterUiDiscovery.Outcome.Err(
                    RepeaterUiDiscovery.DiscoveryError.SendButtonNotFound(
                        hint.takeIf { it.isNotBlank() },
                    )
                )
            !best.isEnabled ->
                RepeaterUiDiscovery.Outcome.Err(
                    RepeaterUiDiscovery.DiscoveryError.SendButtonDisabled()
                )
            else -> RepeaterUiDiscovery.Outcome.Ok(best)
        }
    }

    /**
     * Prefer exact "Send", then largest visible control (toolbar button beats tiny icons/labels).
     */
    fun pickBestSend(candidates: List<Component>): Component? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.single()
        val exact = candidates.filter { primaryLabel(it) == "send" }
        val pool = exact.ifEmpty { candidates }
        return pool.maxWithOrNull(
            compareBy<Component> { if (it.isShowing) 1 else 0 }
                .thenBy { it.width * it.height }
                .thenByDescending { runCatching { it.locationOnScreen.x }.getOrDefault(Int.MAX_VALUE) },
        )
    }

    /**
     * Require Burp Repeater Target to be set before Send.
     * "Target: Not specified" must block Send even if a Host header exists — otherwise Burp
     * opens the Configure target details dialog.
     */
    fun requireTargetPresent(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
    ): RepeaterUiDiscovery.Outcome<Unit> {
        // Never leave a stale configure-target modal around.
        dismissConfigureTargetDialogs()

        ensureTargetUrl(discovered, index)
        val display = readTargetDisplay(discovered.repeaterRoot)
        if (isTargetUnspecified(display)) {
            return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.TargetMissing())
        }

        val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        val request = editors.request?.text.orEmpty()
        val host = hostFromRequest(request)
        val targetFilled = collectTargetFields(discovered.repeaterRoot)
            .any { !it.text.isNullOrBlank() && looksLikeTargetField(it) && !isUnspecifiedText(it.text) }

        if (host != null || targetFilled || (!display.isNullOrBlank() && !isTargetUnspecified(display))) {
            return RepeaterUiDiscovery.Outcome.Ok(Unit)
        }
        return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.TargetMissing())
    }

    fun isTargetUnspecified(display: String?): Boolean {
        if (display == null) return false
        return isUnspecifiedText(display)
    }

    fun isUnspecifiedText(text: String?): Boolean {
        val d = text?.trim().orEmpty()
        if (d.isEmpty()) return true
        return d.equals("not specified", ignoreCase = true)
    }

    /** Reads value after "Target:" in toolbar labels; also matches accessible names. */
    fun readTargetDisplay(root: Component): String? {
        var found: String? = null
        walk(root) { c ->
            val raw = when (c) {
                is JLabel -> c.text
                else -> c.accessibleContext?.accessibleName
            }?.replace(Regex("(?i)<[^>]+>"), " ")?.trim().orEmpty()
            if (raw.isEmpty()) return@walk
            if (!raw.contains("Target", ignoreCase = true)) return@walk
            // "Target: Not specified" / "Target: https://…"
            val after = when {
                raw.contains(':') -> raw.substringAfter(':').trim()
                else -> raw
            }
            if (after.equals("not specified", ignoreCase = true) ||
                after.contains("://") ||
                after.contains('.')
            ) {
                found = after
            }
        }
        return found
    }

    /** Dismiss Burp "Configure target details" modal if present (Cancel / dispose). */
    fun dismissConfigureTargetDialogs() {
        for (window in configureTargetDialogs()) {
            var cancelled = false
            walk(window) { c ->
                if (cancelled) return@walk
                if (c is AbstractButton && isCancelLabel(primaryLabel(c)) && c.isEnabled) {
                    runCatching { c.doClick() }
                    cancelled = true
                }
            }
            if (!cancelled) {
                runCatching { window.isVisible = false }
                runCatching { window.dispose() }
            }
        }
    }

    fun isConfigureTargetDialogShowing(): Boolean = configureTargetDialogs().isNotEmpty()

    fun configureTargetDialogs(): List<java.awt.Window> =
        java.awt.Window.getWindows().filter { window ->
            window.isShowing && isConfigureTargetDialogTitle(
                when (window) {
                    is java.awt.Dialog -> window.title.orEmpty()
                    is java.awt.Frame -> window.title.orEmpty()
                    else -> ""
                },
            )
        }

    fun isConfigureTargetDialogTitle(title: String): Boolean =
        title.contains("target", ignoreCase = true) &&
            (title.contains("configure", ignoreCase = true) || title.contains("details", ignoreCase = true))


    fun hostFromRequest(request: String): String? =
        request.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Host:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun ensureTargetUrl(discovered: RepeaterUiDiscovery.DiscoveredRepeater, index: Int) {
        val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        val request = editors.request?.text ?: return
        val desired = targetUrlFromRequest(request) ?: return
        val fields = collectTargetFields(discovered.repeaterRoot)
        val blank = fields.filter { it.text.isNullOrBlank() }
        val target = when {
            blank.size == 1 -> blank.single()
            fields.size == 1 && looksLikeTargetField(fields.single()) -> fields.single()
            else -> fields.firstOrNull { looksLikeTargetField(it) && it.text.isNullOrBlank() }
        } ?: return
        if (target.text.isNullOrBlank()) {
            target.text = desired
        }
    }

    fun targetUrlFromRequest(request: String): String? {
        val host = hostFromRequest(request) ?: return null
        if (host.contains("://")) return host
        val requestLine = request.lineSequence().firstOrNull()?.uppercase().orEmpty()
        val https = requestLine.contains("HTTP/2") || !requestLine.contains("HTTP/1.0")
        val scheme = if (https) "https" else "http"
        return "$scheme://$host"
    }

    /** @return how the click was performed */
    fun activateSend(control: Component): String {
        runOnEdt {
            javax.swing.SwingUtilities.getWindowAncestor(control)?.toFront()
            control.requestFocusInWindow()
            when (control) {
                is AbstractButton -> {
                    // Prefer listeners — some Burp buttons ignore doClick/Robot.
                    fireActionListeners(control)
                    control.doClick()
                }
                else -> {
                    fireActionListeners(control)
                    reflectiveDoClick(control) || synthesizeMouseClick(control)
                }
            }
        }
        Thread.sleep(50)
        val robotOk = robotClick(control)
        return when {
            robotOk && control is AbstractButton -> "button+robot"
            robotOk -> "robot"
            control is AbstractButton -> "button"
            else -> "synthetic"
        }
    }

    fun fireActionListeners(control: Component) {
        val listeners = when (control) {
            is AbstractButton -> control.actionListeners
            else -> emptyArray()
        }
        if (listeners.isEmpty()) return
        val event = java.awt.event.ActionEvent(
            control,
            java.awt.event.ActionEvent.ACTION_PERFORMED,
            "Send",
            System.currentTimeMillis(),
            0,
        )
        for (listener in listeners) {
            runCatching { listener.actionPerformed(event) }
        }
    }

    fun collectTargetFields(root: Component): List<JTextField> {
        val out = mutableListOf<JTextField>()
        walk(root) { c ->
            if (c is JTextField) out += c
        }
        return out
    }

    fun looksLikeTargetField(field: JTextField): Boolean {
        val t = field.text?.trim().orEmpty()
        if (t.isEmpty()) return true
        return t.contains("://") || t.contains('.')
    }

    fun findSendNearCancel(roots: List<Component>): Component? {
        for (root in roots) {
            val cancels = mutableListOf<Component>()
            walk(root) { c ->
                if (isCancelLabel(primaryLabel(c)) && isUsable(c)) cancels += clickableAncestor(c)
            }
            for (cancel in cancels.distinct()) {
                // Burp nests Send/Cancel in sub-panels — walk up a few ancestors.
                var ancestor: Container? = cancel.parent
                repeat(4) {
                    val scope = ancestor ?: return@repeat
                    val sends = mutableListOf<Component>()
                    walk(scope) { c ->
                        if (c === cancel) return@walk
                        if (isSendLabel(primaryLabel(c)) && isUsable(c) && c.isEnabled) {
                            sends += clickableAncestor(c)
                        }
                    }
                    val distinct = sends.distinct()
                    val exact = distinct.filter { primaryLabel(it) == "send" }
                    val pool = exact.ifEmpty { distinct }
                    // Toolbar scope is small; avoid matching unrelated Sends higher up.
                    if (pool.isNotEmpty() && distinct.size <= 4) {
                        pickBestSend(pool)?.let { return it }
                    }
                    ancestor = scope.parent
                }
            }
        }
        return null
    }

    fun findVisibleSendControls(roots: List<Component>): List<Component> {
        val out = mutableListOf<Component>()
        for (root in roots) {
            walk(root) { c ->
                if (!isUsable(c)) return@walk
                if (!isSendLabel(primaryLabel(c))) return@walk
                out += clickableAncestor(c)
            }
        }
        return out.distinct().filter { it.isEnabled }
    }

    fun describeSendCandidates(roots: List<Component>): List<String> {
        val out = mutableListOf<String>()
        for (root in roots) {
            walk(root) { c ->
                val label = primaryLabel(c)
                if (label.contains("send") || label.contains("cancel")) {
                    out += "${c.javaClass.simpleName}(label='$label',show=${c.isShowing},en=${c.isEnabled},${c.width}x${c.height})"
                }
            }
        }
        return out
    }

    fun primaryLabel(component: Component): String {
        val parts = mutableListOf<String>()
        when (component) {
            is AbstractButton -> {
                parts += norm(component.text)
                parts += norm(component.toolTipText)
                parts += norm(component.actionCommand)
            }
            is JLabel -> parts += norm(component.text)
            is JTextComponent -> {
                // Avoid huge editor bodies — only short fields.
                val t = component.text
                if (t != null && t.length <= 64) parts += norm(t)
            }
        }
        parts += norm(component.name)
        parts += norm(component.accessibleContext?.accessibleName)
        return parts.firstOrNull { it.isNotEmpty() }.orEmpty()
    }

    fun clickableAncestor(component: Component): Component {
        var cur: Component? = component
        while (cur != null) {
            if (cur is AbstractButton) return cur
            if (hasDoClick(cur)) return cur
            val p = cur.parent ?: break
            // Don't climb past the Repeater root panel into the suite chrome.
            if (p is JTabbedPane && p.tabCount > 3) break
            cur = p
        }
        return component
    }

    fun isUsable(c: Component): Boolean {
        if (!c.isDisplayable) return false
        // During layout width/height may be 0 briefly — still allow if showing.
        return c.isShowing || (c.width > 0 && c.height > 0)
    }

    fun isSendLabel(label: String): Boolean {
        if (label == "send") return true
        // e.g. "send request" — exclude "send to intruder" / "sending"
        if (label.startsWith("send ") && !label.contains(" to ")) return true
        return false
    }

    fun isCancelLabel(label: String): Boolean =
        label == "cancel" || label.startsWith("cancel")

    fun robotClick(control: Component): Boolean {
        return runCatching {
            if (!control.isShowing) return false
            val loc = control.locationOnScreen
            val x = loc.x + (control.width.coerceAtLeast(1) / 2)
            val y = loc.y + (control.height.coerceAtLeast(1) / 2)
            val robot = Robot()
            robot.isAutoWaitForIdle = true
            robot.autoDelay = 15
            robot.mouseMove(x, y)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            true
        }.getOrDefault(false)
    }

    fun reflectiveDoClick(control: Component): Boolean {
        return runCatching {
            control.javaClass.methods
                .firstOrNull { it.name == "doClick" && it.parameterCount == 0 }
                ?.also { it.isAccessible = true }
                ?.invoke(control)
            true
        }.getOrDefault(false)
    }

    fun synthesizeMouseClick(control: Component): Boolean {
        return runCatching {
            val w = control.width.coerceAtLeast(1)
            val h = control.height.coerceAtLeast(1)
            val x = w / 2
            val y = h / 2
            val now = System.currentTimeMillis()
            fun fire(id: Int) {
                control.dispatchEvent(
                    MouseEvent(control, id, now, 0, x, y, 1, false, MouseEvent.BUTTON1)
                )
            }
            fire(MouseEvent.MOUSE_PRESSED)
            fire(MouseEvent.MOUSE_RELEASED)
            fire(MouseEvent.MOUSE_CLICKED)
            true
        }.getOrDefault(false)
    }

    fun hasDoClick(c: Component): Boolean =
        c.javaClass.methods.any { it.name == "doClick" && it.parameterCount == 0 }

    fun norm(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.replace(Regex("(?i)<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .trim()
            .lowercase()
    }

    private fun waitForResponse(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        beforeFingerprint: String,
        timeoutMs: Long,
    ): RepeaterUiDiscovery.Outcome<String> {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        var sawUnavailable = beforeFingerprint.isEmpty()
        while (System.nanoTime() < deadline) {
            Thread.sleep(RESPONSE_POLL_MS)
            val sample = runOnEdt {
                if (discovered.tabStrip.selectedIndex != index) {
                    discovered.tabStrip.selectedIndex = index
                }
                val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
                when {
                    editors.responseAmbiguous -> Sample.Ambiguous
                    else -> Sample.Body(editors.response?.text)
                }
            }
            when (sample) {
                is Sample.Ambiguous -> sawUnavailable = true
                is Sample.Body -> {
                    val body = sample.text
                    if (body == null || !RepeaterUiDiscovery.isResponseAvailable(body)) {
                        sawUnavailable = true
                        continue
                    }
                    val fp = "${body.length}:${body.hashCode()}"
                    if (sawUnavailable || beforeFingerprint.isEmpty() || fp != beforeFingerprint) {
                        return RepeaterUiDiscovery.Outcome.Ok(body)
                    }
                }
            }
        }
        return RepeaterUiDiscovery.Outcome.Err(
            RepeaterUiDiscovery.DiscoveryError.ResponseTimeout(timeoutMs)
        )
    }

    private sealed class Sample {
        data object Ambiguous : Sample()
        data class Body(val text: String?) : Sample()
    }

    private fun responseFingerprint(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
    ): String {
        val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        val body = editors.response?.text
        return if (body != null && RepeaterUiDiscovery.isResponseAvailable(body)) {
            "${body.length}:${body.hashCode()}"
        } else {
            ""
        }
    }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            if (root is JTabbedPane) {
                for (i in 0 until root.tabCount) {
                    root.getComponentAt(i)?.let { walk(it, visit) }
                }
                for (child in root.components) {
                    if ((0 until root.tabCount).none { root.getComponentAt(it) === child }) {
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
}
