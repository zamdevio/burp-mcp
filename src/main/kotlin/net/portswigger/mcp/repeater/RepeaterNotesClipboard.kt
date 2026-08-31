package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

/**
 * Burp 2026 Repeater Notes often do not expose plain text on Swing components ([burp.Zwlc] HTML).
 * Fallback: focus the east notes area and use system clipboard (select-all / copy / paste).
 */
internal object RepeaterNotesClipboard {

    fun readPlainText(_api: MontoyaApi, discovered: RepeaterUiDiscovery.DiscoveredRepeater): String? {
        val area = findNotesFocusArea(discovered) ?: return null
        return runClipboardRead(area)
    }

    fun writePlainText(
        _api: MontoyaApi,
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        text: String,
    ): Boolean {
        val area = findNotesFocusArea(discovered) ?: return false
        return runClipboardWrite(area, text)
    }

    /** Exposed for [RepeaterNotesScanner] when needle calibration fails on component text. */
    fun readClipboardPreview(discovered: RepeaterUiDiscovery.DiscoveredRepeater): String? =
        findNotesFocusArea(discovered)?.let { runClipboardRead(it) }?.trim()

    private fun findNotesFocusArea(discovered: RepeaterUiDiscovery.DiscoveredRepeater): Rectangle? {
        RepeaterNotesSidebar.selectEastSidebarNotesTab(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        RepeaterNotesSidebar.ensureEastNotesRailSelected(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        findFocusAreaViaRichTextToolbar(discovered)?.let { return it }
        val anchor = discovered.repeaterRoot
        val anchorPoint = runCatching { anchor.locationOnScreen }.getOrNull()
        val midX = anchorPoint?.let { it.x + anchor.width / 2 }
        var best: Rectangle? = null
        var bestScore = Int.MIN_VALUE
        if (midX != null) {
            for (root in listOf(discovered.repeaterRoot, discovered.suiteFrame)) {
                walk(root) { c ->
                    val scroll = c as? JScrollPane ?: return@walk
                    if (RepeaterNotes.isUnderComponent(scroll, discovered.tabStrip)) return@walk
                    if (scroll.width !in 120..520 || scroll.height < 120) return@walk
                    if (!scroll.isShowing && !scrollHasShowingViewport(scroll)) return@walk
                    val loc = runCatching { scroll.locationOnScreen }.getOrNull() ?: return@walk
                    if (loc.x < midX) return@walk
                    var score = scroll.width * scroll.height
                    if (scroll.width in 200..450) score += 500_000
                    if (scroll.isShowing) score += 300_000
                    if (score > bestScore) {
                        bestScore = score
                        best = Rectangle(loc.x, loc.y, scroll.width, scroll.height)
                    }
                }
            }
        }
        return best ?: fallbackEastSidebarContentRect(discovered)
    }

    /** Last resort: click east band of Repeater root (Notes beside vertical rail). */
    private fun fallbackEastSidebarContentRect(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
    ): Rectangle? {
        val root = discovered.repeaterRoot
        if (!root.isDisplayable || root.width < 400 || root.height < 200) return null
        val loc = runCatching { root.locationOnScreen }.getOrNull() ?: return null
        val rail = 36
        val top = 100
        val w = (root.width * 0.22).toInt().coerceIn(180, 420)
        val h = (root.height - top - 80).coerceAtLeast(120)
        val x = loc.x + root.width - w - rail
        val y = loc.y + top
        return Rectangle(x, y, w, h)
    }

    private fun scrollHasShowingViewport(scroll: JScrollPane): Boolean {
        val vp = scroll.viewport ?: return false
        return scroll.isDisplayable && vp.isShowing && scroll.width > 16 && scroll.height > 16
    }

    private fun findFocusAreaViaRichTextToolbar(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
    ): Rectangle? {
        val tips = listOf("bold", "italic", "underline", "strikethrough", "bullet")
        val anchor = discovered.repeaterRoot
        val midX = runCatching { anchor.locationOnScreen }.getOrNull()?.let { it.x + anchor.width / 2 }
        var best: Rectangle? = null
        var bestScore = Int.MIN_VALUE
        for (root in listOf(discovered.repeaterRoot, discovered.suiteFrame)) {
            walk(root) { component ->
                val button = component as? javax.swing.AbstractButton ?: return@walk
                if (RepeaterNotes.isUnderComponent(button, discovered.tabStrip)) return@walk
                val tip = button.toolTipText?.lowercase().orEmpty()
                if (tips.none { tip.contains(it) }) return@walk
                if (!button.isShowing) return@walk
                val btnLoc = runCatching { button.locationOnScreen }.getOrNull() ?: return@walk
                if (midX != null && btnLoc.x < midX) return@walk
                var host: java.awt.Container? = button.parent
                repeat(4) { host = host?.parent as? java.awt.Container }
                val searchRoot = host ?: return@walk
                walk(searchRoot) { c ->
                    val scroll = c as? JScrollPane ?: return@walk
                    if (scroll.width < 80 || scroll.height < 80) return@walk
                    val loc = runCatching { scroll.locationOnScreen }.getOrNull() ?: return@walk
                    var score = scroll.width * scroll.height + 2_000_000
                    if (scroll.isShowing) score += 1_000_000
                    if (score > bestScore) {
                        bestScore = score
                        best = Rectangle(loc.x, loc.y, scroll.width, scroll.height)
                    }
                }
            }
        }
        return best
    }

    private fun runClipboardRead(area: Rectangle): String? {
        if (!SwingUtilities.isEventDispatchThread()) {
            clickCenter(area)
            Thread.sleep(80)
            sendSelectAllCopy()
            Thread.sleep(80)
            return readClipboardString()?.trim()
        }
        var out: String? = null
        SwingUtilities.invokeLater {
            clickCenter(area)
        }
        Thread.sleep(100)
        sendSelectAllCopy()
        Thread.sleep(100)
        out = readClipboardString()?.trim()
        return out
    }

    private fun runClipboardWrite(area: Rectangle, text: String): Boolean {
        writeClipboardString(text)
        clickCenter(area)
        Thread.sleep(80)
        sendSelectAllPaste()
        Thread.sleep(120)
        val after = runClipboardRead(area)
        return after == text.trim()
    }

    private fun clickCenter(area: Rectangle) {
        val robot = Robot()
        val x = area.x + area.width / 2
        val y = area.y + area.height / 2
        robot.mouseMove(x, y)
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    }

    private fun sendSelectAllCopy() {
        val robot = Robot()
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.delay(40)
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_C)
        robot.keyRelease(KeyEvent.VK_C)
        robot.keyRelease(KeyEvent.VK_CONTROL)
    }

    private fun sendSelectAllPaste() {
        val robot = Robot()
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_A)
        robot.keyRelease(KeyEvent.VK_CONTROL)
        robot.delay(40)
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_CONTROL)
    }

    private fun readClipboardString(): String? =
        runCatching {
            val clip = Toolkit.getDefaultToolkit().systemClipboard
            clip.getData(DataFlavor.stringFlavor) as? String
        }.getOrNull()

    private fun writeClipboardString(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is java.awt.Container) {
            for (child in root.components) {
                walk(child, visit)
            }
        }
    }
}
