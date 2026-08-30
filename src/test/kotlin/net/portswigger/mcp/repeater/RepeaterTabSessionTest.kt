package net.portswigger.mcp.repeater

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.event.ChangeListener

class RepeaterTabSessionTest {

    @Nested
    inner class RestoreTests {
        @Test
        fun `withTab restores suite and strip selection`() {
            val tree = SharedEditorTree.build(
                requests = listOf("GET /a HTTP/1.1", "GET /b HTTP/1.1"),
                responses = listOf("HTTP/1.1 200 OK", null),
                selectedIndex = 0,
                suiteSelected = "Proxy",
            )
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value

            assertEquals(0, discovered.suiteTabbedPane.selectedIndex) // Proxy
            assertEquals(0, discovered.tabStrip.selectedIndex)

            RepeaterTabSession.withTab(discovered, 1) {
                assertEquals(discovered.suiteRepeaterIndex, discovered.suiteTabbedPane.selectedIndex)
                assertEquals(1, discovered.tabStrip.selectedIndex)
            }

            assertEquals(0, discovered.suiteTabbedPane.selectedIndex)
            assertEquals(0, discovered.tabStrip.selectedIndex)
        }

        @Test
        fun `withTab restores even when block throws`() {
            val tree = SharedEditorTree.build(
                requests = listOf("GET /a HTTP/1.1", "GET /b HTTP/1.1"),
                responses = listOf(null, null),
                selectedIndex = 0,
            )
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value

            assertThrows(IllegalStateException::class.java) {
                RepeaterTabSession.withTab(discovered, 1) {
                    throw IllegalStateException("boom")
                }
            }
            assertEquals(0, discovered.tabStrip.selectedIndex)
            assertEquals(discovered.suiteRepeaterIndex, discovered.suiteTabbedPane.selectedIndex)
        }
    }

    @Nested
    inner class SharedEditorTests {
        @Test
        fun `read while selected returns target tab body then restores prior content`() {
            val tree = SharedEditorTree.build(
                requests = listOf("GET /a HTTP/1.1", "GET /b HTTP/1.1"),
                responses = listOf("HTTP/1.1 200 a", "HTTP/1.1 200 b"),
                selectedIndex = 0,
            )
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val read = RepeaterTabSession.withTab(discovered, 1) {
                val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, 1)
                assertFalse(editors.requestAmbiguous)
                editors.request!!.text
            }

            assertEquals("GET /b HTTP/1.1", read)
            assertEquals(0, discovered.tabStrip.selectedIndex)
            assertEquals("GET /a HTTP/1.1", tree.sharedRequest.text)
        }

        @Test
        fun `write while selected mutates shared editor for that tab and restores selection`() {
            val tree = SharedEditorTree.build(
                requests = listOf("GET /a HTTP/1.1", "GET /b HTTP/1.1"),
                responses = listOf(null, null),
                selectedIndex = 0,
            )
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val newRequest = "POST /b HTTP/1.1\r\nHost: x\r\n\r\n"
            RepeaterTabSession.withTab(discovered, 1) {
                val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, 1)
                editors.request!!.text = newRequest
                tree.storeRequest(1, newRequest)
            }

            assertEquals(0, discovered.tabStrip.selectedIndex)
            assertEquals("GET /a HTTP/1.1", tree.sharedRequest.text)

            RepeaterTabSession.withTab(discovered, 1) {
                assertEquals(newRequest, RepeaterUiDiscovery.findEditorsWhileSelected(discovered, 1).request!!.text)
            }
        }

        @Test
        fun `buildTabInfo probes shared editor without leaving selection`() {
            val tree = SharedEditorTree.build(
                requests = listOf("GET /a HTTP/1.1", "GET /b HTTP/1.1"),
                responses = listOf(null, "HTTP/1.1 404"),
                selectedIndex = 0,
            )
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value

            val infos = RepeaterUiDiscovery.buildTabInfos(discovered)
            assertEquals(2, infos.size)
            assertTrue(infos[0].hasRequest)
            assertFalse(infos[0].hasResponse)
            assertTrue(infos[0].selected)
            assertTrue(infos[1].hasRequest)
            assertTrue(infos[1].hasResponse)
            assertFalse(infos[1].selected)

            assertEquals(0, discovered.tabStrip.selectedIndex)
            assertEquals("GET /a HTTP/1.1", tree.sharedRequest.text)
        }
    }

    @Nested
    inner class ErrorMessageTests {
        @Test
        fun `editor did not update message is stable for agents`() {
            assertEquals(
                "<Repeater editor did not update; retry list_repeater_tabs then the operation>",
                RepeaterUiDiscovery.DiscoveryError.EditorDidNotUpdate().message,
            )
        }
    }

    private class SharedEditorTree(
        val frame: JFrame,
        val sharedRequest: JTextArea,
        private val requests: MutableList<String>,
        private val responses: MutableList<String?>,
    ) {
        fun storeRequest(index: Int, text: String) {
            requests[index] = text
        }

        companion object {
            fun build(
                requests: List<String>,
                responses: List<String?>,
                selectedIndex: Int,
                suiteSelected: String = "Repeater",
            ): SharedEditorTree {
                require(requests.size == responses.size)
                val storedRequests = requests.toMutableList()
                val storedResponses = responses.toMutableList()

                val sharedRequest = JTextArea().also { it.isEditable = true }
                val sharedResponse = JTextArea().also { it.isEditable = false }
                val editors = JPanel(BorderLayout()).also {
                    it.add(sharedRequest, BorderLayout.WEST)
                    it.add(sharedResponse, BorderLayout.EAST)
                }

                val strip = JTabbedPane()
                requests.indices.forEach { i ->
                    // Empty tab content forces discovery to use the shared editors under repeater root.
                    strip.addTab("${i + 1}", JPanel())
                }

                val sync = ChangeListener {
                    val i = strip.selectedIndex
                    if (i in storedRequests.indices) {
                        sharedRequest.text = storedRequests[i]
                        sharedResponse.text = storedResponses[i] ?: ""
                    }
                }
                strip.addChangeListener(sync)
                strip.selectedIndex = selectedIndex
                sync.stateChanged(null)

                val repeaterRoot = JPanel(BorderLayout()).also {
                    it.add(strip, BorderLayout.CENTER)
                    it.add(editors, BorderLayout.SOUTH)
                }

                val suite = JTabbedPane()
                suite.addTab("Proxy", JPanel())
                suite.addTab("Repeater", repeaterRoot)
                suite.addTab("Intruder", JPanel())
                suite.selectedIndex = when (suiteSelected) {
                    "Proxy" -> 0
                    "Intruder" -> 2
                    else -> 1
                }

                val frame = JFrame().also { it.contentPane.add(suite) }
                return SharedEditorTree(frame, sharedRequest, storedRequests, storedResponses)
            }
        }
    }
}
