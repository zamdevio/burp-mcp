package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea

class RepeaterTabCloseTest {

    private val api = mockk<MontoyaApi>(relaxed = true)

    @Test
    fun `closeTab removes tab and shifts ids`() {
        val frame = buildTree(listOf("a", "b", "c"))
        every { api.userInterface().swingUtils().suiteFrame() } returns frame

        val result = RepeaterTabClose.closeTab(api, "repeater-tab-1")
        assertTrue(result is RepeaterUiDiscovery.Outcome.Ok)

        val discovered =
            (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value
        assertEquals(2, discovered.tabStrip.tabCount)
        assertEquals("a", discovered.tabStrip.getTitleAt(0))
        assertEquals("c", discovered.tabStrip.getTitleAt(1))
    }

    @Test
    fun `cannot close last message tab`() {
        val frame = buildTree(listOf("only"))
        every { api.userInterface().swingUtils().suiteFrame() } returns frame

        val result = RepeaterTabClose.closeTab(api, "repeater-tab-0")
        assertTrue(result is RepeaterUiDiscovery.Outcome.Err)
        assertEquals(
            RepeaterUiDiscovery.DiscoveryError.CannotCloseLastTab().message,
            (result as RepeaterUiDiscovery.Outcome.Err).error.message,
        )
    }

    @Test
    fun `closeOtherTabs keeps one tab`() {
        val frame = buildTree(listOf("keep", "x", "y"))
        every { api.userInterface().swingUtils().suiteFrame() } returns frame

        val result = RepeaterTabClose.closeOtherTabs(api, "repeater-tab-0")
        assertTrue(result is RepeaterUiDiscovery.Outcome.Ok)

        val discovered =
            (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value
        assertEquals(1, discovered.tabStrip.tabCount)
        assertEquals("keep", discovered.tabStrip.getTitleAt(0))
    }

    private fun buildTree(names: List<String>): JFrame {
        val strip = JTabbedPane()
        names.forEach { name ->
            val content = JPanel(BorderLayout())
            content.add(JTextArea("GET / HTTP/1.1").also { it.isEditable = true }, BorderLayout.CENTER)
            strip.addTab(name, content)
        }
        val suite = JTabbedPane()
        suite.addTab("Repeater", strip)
        suite.selectedIndex = 0
        return JFrame().also { it.contentPane.add(suite) }
    }
}
