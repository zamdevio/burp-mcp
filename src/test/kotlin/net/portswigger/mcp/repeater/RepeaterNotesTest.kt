package net.portswigger.mcp.repeater

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JLabel

class RepeaterNotesTest {

    @Nested
    inner class FindNotesInRootTests {
        @Test
        fun `residual editable non-http area is notes`() {
            val panel = JPanel(BorderLayout())
            val request = JTextArea("GET / HTTP/1.1").also { it.isEditable = true }
            val response = JTextArea("HTTP/1.1 200 OK").also { it.isEditable = false }
            val notes = JTextArea("finding: xss").also { it.isEditable = true }
            panel.add(request, BorderLayout.WEST)
            panel.add(response, BorderLayout.EAST)
            panel.add(notes, BorderLayout.SOUTH)

            val messageEditors = RepeaterUiDiscovery.findEditors(panel)
            val lookup = RepeaterNotes.findNotesInRoot(panel, messageEditors)
            assertFalse(lookup.ambiguous)
            assertEquals(notes, lookup.editor)
            assertEquals("finding: xss", lookup.editor?.text)
        }

        @Test
        fun `notes subtab is preferred`() {
            val inner = JTabbedPane()
            val notesArea = JTextArea("from subtab").also { it.isEditable = true }
            inner.addTab("Request", JPanel())
            inner.addTab("Notes", notesArea)
            val messageEditors = RepeaterUiDiscovery.TabEditors(null, null, false, false)

            val lookup = RepeaterNotes.findNotesInRoot(inner, messageEditors)
            assertEquals(notesArea, lookup.editor)
        }

        @Test
        fun `multiple extra editables are ambiguous`() {
            val panel = JPanel()
            panel.add(JTextArea("note a").also { it.isEditable = true })
            panel.add(JTextArea("note b").also { it.isEditable = true })
            val messageEditors = RepeaterUiDiscovery.TabEditors(null, null, false, false)

            val lookup = RepeaterNotes.findNotesInRoot(panel, messageEditors)
            assertTrue(lookup.ambiguous)
            assertNull(lookup.editor)
        }

        @Test
        fun `notes label sibling is discovered`() {
            val panel = JPanel(BorderLayout())
            panel.add(JLabel("Notes"), BorderLayout.NORTH)
            val notes = JTextArea("labeled").also { it.isEditable = true }
            panel.add(notes, BorderLayout.CENTER)
            val messageEditors = RepeaterUiDiscovery.TabEditors(null, null, false, false)

            val lookup = RepeaterNotes.findNotesInRoot(panel, messageEditors)
            assertEquals(notes, lookup.editor)
        }
    }

    @Nested
    inner class SessionTests {
        @Test
        fun `read and write notes on synthetic repeater tree`() {
            val frame = buildTreeWithNotes("agent note")
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val read = RepeaterTabSession.withTab(discovered, 0) {
                RepeaterNotes.readWhileSelected(discovered, 0)
            }
            assertTrue(read is RepeaterUiDiscovery.Outcome.Ok)
            assertEquals("agent note", (read as RepeaterUiDiscovery.Outcome.Ok).value)

            val write = RepeaterTabSession.withTab(discovered, 0) {
                RepeaterNotes.writeWhileSelected(discovered, 0, "repeater-tab-0", "updated")
            }
            assertTrue(write is RepeaterUiDiscovery.Outcome.Ok)

            val readAgain = RepeaterTabSession.withTab(discovered, 0) {
                RepeaterNotes.readWhileSelected(discovered, 0)
            }
            assertEquals("updated", (readAgain as RepeaterUiDiscovery.Outcome.Ok).value)
        }

        @Test
        fun `write flips read-only notes field editable`() {
            val frame = buildTreeWithNotes("seed", notesEditable = false)
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val write = RepeaterTabSession.withTab(discovered, 0) {
                RepeaterNotes.writeWhileSelected(discovered, 0, "repeater-tab-0", "after write")
            }
            assertTrue(write is RepeaterUiDiscovery.Outcome.Ok)
        }
    }

    private fun buildTreeWithNotes(notesText: String, notesEditable: Boolean = true): javax.swing.JFrame {
        val repeaterTabs = JTabbedPane()
        val content = JPanel(BorderLayout())
        content.add(JTextArea("GET / HTTP/1.1").also { it.isEditable = true }, BorderLayout.WEST)
        content.add(JTextArea("").also { it.isEditable = false }, BorderLayout.EAST)
        content.add(JTextArea(notesText).also { it.isEditable = notesEditable }, BorderLayout.SOUTH)
        repeaterTabs.addTab("1", content)

        val suite = JTabbedPane()
        suite.addTab("Repeater", repeaterTabs)
        suite.selectedIndex = 0
        return javax.swing.JFrame().also { it.contentPane.add(suite) }
    }
}
