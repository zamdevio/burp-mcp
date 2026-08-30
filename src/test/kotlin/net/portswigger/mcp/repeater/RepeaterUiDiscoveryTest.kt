package net.portswigger.mcp.repeater

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField

class RepeaterUiDiscoveryTest {

    @Nested
    inner class TabIdTests {
        @Test
        fun `fromIndex formats deterministic ids`() {
            assertEquals("repeater-tab-0", RepeaterTabId.fromIndex(0))
            assertEquals("repeater-tab-12", RepeaterTabId.fromIndex(12))
        }

        @Test
        fun `parseIndex reads valid ids`() {
            assertEquals(0, RepeaterTabId.parseIndex("repeater-tab-0"))
            assertEquals(3, RepeaterTabId.parseIndex("repeater-tab-3"))
            assertEquals(3, RepeaterTabId.parseIndex("  repeater-tab-3  "))
        }

        @Test
        fun `parseIndex rejects invalid ids`() {
            assertNull(RepeaterTabId.parseIndex("repeater-tab"))
            assertNull(RepeaterTabId.parseIndex("repeater-tab-"))
            assertNull(RepeaterTabId.parseIndex("tab-0"))
            assertNull(RepeaterTabId.parseIndex("repeater-tab-x"))
            assertNull(RepeaterTabId.parseIndex(""))
        }

        @Test
        fun `fromIndex rejects negative`() {
            assertThrows(IllegalArgumentException::class.java) {
                RepeaterTabId.fromIndex(-1)
            }
        }
    }

    @Nested
    inner class SerializationTests {
        @Test
        fun `RepeaterTabInfo round-trips`() {
            val info = RepeaterTabInfo(
                id = "repeater-tab-1",
                name = "Login",
                index = 1,
                selected = false,
                hasRequest = true,
                hasResponse = true,
            )
            val json = Json.encodeToString(RepeaterTabInfo.serializer(), info)
            val decoded = Json.decodeFromString(RepeaterTabInfo.serializer(), json)
            assertEquals(info, decoded)
        }
    }

    @Nested
    inner class ResponseAvailabilityTests {
        @Test
        fun `null blank and non-http are unavailable`() {
            assertFalse(RepeaterUiDiscovery.isResponseAvailable(null))
            assertFalse(RepeaterUiDiscovery.isResponseAvailable(""))
            assertFalse(RepeaterUiDiscovery.isResponseAvailable("   "))
            assertFalse(RepeaterUiDiscovery.isResponseAvailable("Waiting for response…"))
        }

        @Test
        fun `http status line counts as available even with empty body`() {
            assertTrue(RepeaterUiDiscovery.isResponseAvailable("HTTP/1.1 200 OK\r\n\r\n"))
            assertTrue(RepeaterUiDiscovery.isResponseAvailable("http/2 204\n"))
        }
    }

    @Nested
    inner class EditorLookupTests {
        @Test
        fun `findEditors distinguishes request and response`() {
            val panel = JPanel(BorderLayout())
            val request = JTextArea("GET / HTTP/1.1").also { it.isEditable = true }
            val response = JTextArea("HTTP/1.1 200 OK").also { it.isEditable = false }
            panel.add(request, BorderLayout.WEST)
            panel.add(response, BorderLayout.EAST)

            val editors = RepeaterUiDiscovery.findEditors(panel)
            assertFalse(editors.requestAmbiguous)
            assertFalse(editors.responseAmbiguous)
            assertEquals(request, editors.request)
            assertEquals(response, editors.response)
        }

        @Test
        fun `multiple editable areas are ambiguous`() {
            val panel = JPanel()
            panel.add(JTextArea("a").also { it.isEditable = true })
            panel.add(JTextArea("b").also { it.isEditable = true })
            panel.add(JTextArea("c").also { it.isEditable = false })

            val editors = RepeaterUiDiscovery.findEditors(panel)
            assertTrue(editors.requestAmbiguous)
            assertNull(editors.request)
            assertEquals("c", editors.response?.text)
        }

        @Test
        fun `missing editors are reported cleanly`() {
            val editors = RepeaterUiDiscovery.findEditors(JPanel())
            assertNull(editors.request)
            assertNull(editors.response)
            assertFalse(editors.requestAmbiguous)
            assertFalse(editors.responseAmbiguous)
        }
    }

    @Nested
    inner class SwingTreeDiscoveryTests {
        @Test
        fun `discovers repeater tabs from synthetic suite tree`() {
            val tree = buildSyntheticBurpTree(
                tabs = listOf(
                    TabSpec("1", "GET /a HTTP/1.1", "HTTP/1.1 200 OK"),
                    TabSpec("Login", "POST /login HTTP/1.1", null),
                ),
                selectedIndex = 1,
            )

            val discovered = RepeaterUiDiscovery.discoverRepeaterFromRoot(tree)
            assertTrue(discovered is RepeaterUiDiscovery.Outcome.Ok)
            val ok = discovered as RepeaterUiDiscovery.Outcome.Ok
            val infos = RepeaterUiDiscovery.buildTabInfos(ok.value)

            assertEquals(2, infos.size)
            assertEquals("repeater-tab-0", infos[0].id)
            assertEquals("1", infos[0].name)
            assertTrue(infos[0].hasRequest)
            assertTrue(infos[0].hasResponse)
            assertFalse(infos[0].selected)

            assertEquals("repeater-tab-1", infos[1].id)
            assertEquals("Login", infos[1].name)
            assertTrue(infos[1].selected)
            assertTrue(infos[1].hasRequest)
            assertFalse(infos[1].hasResponse)
        }

        @Test
        fun `missing tab index fails safely`() {
            val tree = buildSyntheticBurpTree(tabs = listOf(TabSpec("1", "GET / HTTP/1.1", null)))
            val discovered = (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree) as RepeaterUiDiscovery.Outcome.Ok).value
            assertNull(RepeaterTabId.parseIndex("nope"))
            assertTrue(1 >= discovered.tabStrip.tabCount || discovered.tabStrip.tabCount == 1)
            assertEquals(1, discovered.tabStrip.tabCount)
        }

        @Test
        fun `select and rename work on discovered strip`() {
            val tree = buildSyntheticBurpTree(
                tabs = listOf(
                    TabSpec("1", "GET /a HTTP/1.1", null),
                    TabSpec("2", "GET /b HTTP/1.1", null),
                ),
                selectedIndex = 0,
            )
            val discovered = (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree) as RepeaterUiDiscovery.Outcome.Ok).value
            discovered.tabStrip.selectedIndex = 1
            discovered.tabStrip.setTitleAt(1, "Renamed")
            assertEquals(1, discovered.tabStrip.selectedIndex)
            assertEquals("Renamed", discovered.tabStrip.getTitleAt(1))
        }

        @Test
        fun `non-editable request editor is detectable`() {
            val panel = JPanel()
            panel.add(JTextArea("GET / HTTP/1.1").also { it.isEditable = false })
            val editors = RepeaterUiDiscovery.findEditors(panel)
            assertNull(editors.request)
            assertNotNull(editors.response)
            assertFalse(editors.response!!.isEditable)
        }

        @Test
        fun `http-like text helps pick request among multiple editables`() {
            val panel = JPanel()
            panel.add(javax.swing.JTextArea("search").also { it.isEditable = true })
            panel.add(javax.swing.JTextArea("POST /login HTTP/1.1\r\nHost: x\r\n\r\n").also { it.isEditable = true })
            val editors = RepeaterUiDiscovery.findEditors(panel)
            assertFalse(editors.requestAmbiguous)
            assertTrue(editors.request!!.text.startsWith("POST"))
        }

        @Test
        fun `null tab content does not crash listing`() {
            val frame = JFrame()
            val suite = JTabbedPane()
            val strip = JTabbedPane()
            strip.addTab("1", JPanel())
            suite.addTab("Repeater", strip)
            frame.contentPane.add(suite)
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val editors = RepeaterUiDiscovery.findEditors(null)
            assertNull(editors.request)
            assertNull(editors.response)

            val info = RepeaterUiDiscovery.buildTabInfo(discovered, 0)
            assertEquals("repeater-tab-0", info.id)
            assertFalse(info.hasRequest)
        }

        @Test
        fun `repeater not found when suite has no Repeater tab`() {
            val suite = JTabbedPane()
            suite.addTab("Proxy", JPanel())
            val frame = JFrame().also {
                it.add(suite)
            }
            val result = RepeaterUiDiscovery.discoverRepeaterFromRoot(frame)
            assertTrue(result is RepeaterUiDiscovery.Outcome.Err)
            assertEquals(
                "<Repeater UI not found>",
                (result as RepeaterUiDiscovery.Outcome.Err).error.message
            )
        }
    }

    private data class TabSpec(val name: String, val request: String, val response: String?)

    private fun buildSyntheticBurpTree(tabs: List<TabSpec>, selectedIndex: Int = 0): JFrame {
        val repeaterTabs = JTabbedPane()
        tabs.forEach { tab ->
            val content = JPanel(BorderLayout())
            content.add(JTextArea(tab.request).also { it.isEditable = true }, BorderLayout.WEST)
            if (tab.response != null) {
                content.add(JTextArea(tab.response).also { it.isEditable = false }, BorderLayout.EAST)
            } else {
                // Target field-like noise that should not be treated as a message editor.
                content.add(JTextField("example.com:443"), BorderLayout.NORTH)
                content.add(JTextArea("").also { it.isEditable = false }, BorderLayout.EAST)
            }
            repeaterTabs.addTab(tab.name, content)
        }
        repeaterTabs.selectedIndex = selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

        val suite = JTabbedPane()
        suite.addTab("Proxy", JPanel())
        suite.addTab("Repeater", repeaterTabs)
        suite.addTab("Intruder", JPanel())
        suite.selectedIndex = 1

        return JFrame().also { it.contentPane.add(suite) }
    }
}
