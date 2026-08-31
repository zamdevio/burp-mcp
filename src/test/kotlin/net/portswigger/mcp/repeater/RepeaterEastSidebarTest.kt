package net.portswigger.mcp.repeater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

class RepeaterEastSidebarTest {

    @Test
    fun `isRailExpanded false when tabbed pane has zero width`() {
        val pane = JTabbedPane().apply {
            addTab("Inspector", JPanel())
            addTab("Notes", JPanel())
            setSize(0, 200)
        }
        assertFalse(RepeaterEastSidebar.isRailExpanded(pane))
    }

    @Test
    fun `readState lists rail tab titles`() {
        withFrame { frame ->
            val discovered = when (val d = RepeaterUiDiscovery.discoverRepeaterFromRoot(frame)) {
                is RepeaterUiDiscovery.Outcome.Ok -> d.value
                is RepeaterUiDiscovery.Outcome.Err -> error(d.error.message)
            }
            val state = RepeaterEastSidebar.readState(discovered)
            assertTrue(state is RepeaterUiDiscovery.Outcome.Ok, (state as? RepeaterUiDiscovery.Outcome.Err)?.error?.message)
            val body = (state as RepeaterUiDiscovery.Outcome.Ok).value
            assertTrue(body.railTabs.any { it.equals("Notes", ignoreCase = true) })
            assertEquals("Notes", body.selectedRailTab)
        }
    }

    private fun withFrame(block: (JFrame) -> Unit) {
        val eastRail = JTabbedPane().apply {
            addTab("Inspector", JPanel())
            addTab("Notes", JPanel())
            addTab("Explanations", JPanel())
            selectedIndex = 1
        }
        val messageTabs = JTabbedPane().apply {
            addTab("1", JPanel(BorderLayout()).apply {
                add(javax.swing.JTextArea("GET / HTTP/1.1").also { it.isEditable = true }, BorderLayout.CENTER)
            })
        }
        val repeater = JPanel(BorderLayout()).apply {
            add(messageTabs, BorderLayout.CENTER)
            add(eastRail, BorderLayout.EAST)
        }
        val suite = JTabbedPane().apply {
            addTab("Dashboard", JPanel())
            addTab("Repeater", repeater)
            selectedIndex = 1
        }
        val frame = JFrame().apply { contentPane.add(suite) }
        try {
            SwingUtilities.invokeAndWait {
                frame.pack()
                frame.setSize(900, 500)
                frame.isVisible = true
            }
            block(frame)
        } finally {
            SwingUtilities.invokeAndWait {
                frame.isVisible = false
                frame.dispose()
            }
        }
    }
}
