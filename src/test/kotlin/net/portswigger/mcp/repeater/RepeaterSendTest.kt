package net.portswigger.mcp.repeater

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField

class RepeaterSendTest {

    @Nested
    inner class Labels {
        @Test
        fun `send labels`() {
            assertTrue(RepeaterSend.isSendLabel("send"))
            assertTrue(RepeaterSend.isSendLabel("send request"))
            assertFalse(RepeaterSend.isSendLabel("send to intruder"))
            assertFalse(RepeaterSend.isSendLabel("sending"))
        }

        @Test
        fun `target url from request`() {
            assertEquals(
                "https://api.example.com",
                RepeaterSend.targetUrlFromRequest("GET /health HTTP/2\r\nHost: api.example.com\r\n\r\n"),
            )
            assertEquals(
                "http://example.com",
                RepeaterSend.targetUrlFromRequest("GET / HTTP/1.0\r\nHost: example.com\r\n\r\n"),
            )
        }

        @Test
        fun `target missing message guides agents`() {
            val msg = RepeaterUiDiscovery.DiscoveryError.TargetMissing().message
            assertTrue(msg.contains("not specified") || msg.contains("Target"))
            assertTrue(msg.contains("send_repeater_tab"))
        }

        @Test
        fun `Not specified is treated as missing target`() {
            assertTrue(RepeaterSend.isTargetUnspecified("Not specified"))
            assertTrue(RepeaterSend.isTargetUnspecified(""))
            assertFalse(RepeaterSend.isTargetUnspecified("https://api.example.com"))
            assertFalse(RepeaterSend.isTargetUnspecified(null))
        }

        @Test
        fun `requireTargetPresent fails without host or target field`() {
            val request = JTextArea("GET / HTTP/1.1\r\n\r\n").also { it.isEditable = true }
            val strip = JTabbedPane()
            strip.addTab("1", JPanel())
            val root = JPanel(BorderLayout()).also {
                it.add(javax.swing.JLabel("Target: Not specified"), BorderLayout.NORTH)
                it.add(strip, BorderLayout.CENTER)
                it.add(request, BorderLayout.SOUTH)
            }
            val suite = JTabbedPane()
            suite.addTab("Repeater", root)
            suite.selectedIndex = 0
            val frame = JFrame().also { it.contentPane.add(suite) }
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value
            val result = RepeaterSend.requireTargetPresent(discovered, 0)
            assertTrue(result is RepeaterUiDiscovery.Outcome.Err)
            assertTrue(
                (result as RepeaterUiDiscovery.Outcome.Err).error.message.contains("not specified"),
            )
        }

        @Test
        fun `Host header alone does not bypass Not specified Target`() {
            val request = JTextArea("GET / HTTP/1.1\r\nHost: example.com\r\n\r\n").also {
                it.isEditable = true
            }
            val strip = JTabbedPane()
            strip.addTab("1", JPanel())
            val root = JPanel(BorderLayout()).also {
                it.add(javax.swing.JLabel("Target: Not specified"), BorderLayout.NORTH)
                it.add(strip, BorderLayout.CENTER)
                it.add(request, BorderLayout.SOUTH)
            }
            val suite = JTabbedPane()
            suite.addTab("Repeater", root)
            suite.selectedIndex = 0
            val frame = JFrame().also { it.contentPane.add(suite) }
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value
            val result = RepeaterSend.requireTargetPresent(discovered, 0)
            assertTrue(result is RepeaterUiDiscovery.Outcome.Err)
        }
    }

    @Nested
    inner class SendButtonDiscovery {
        @Test
        fun `Send beside Cancel is preferred`() {
            val bar = JPanel()
            val send = JButton("Send")
            val cancel = JButton("Cancel")
            bar.add(send)
            bar.add(cancel)
            val frame = JFrame().also {
                it.add(bar)
                it.pack()
                it.isVisible = true
            }
            try {
                assertEquals(send, RepeaterSend.findSendNearCancel(listOf(bar)))
            } finally {
                frame.isVisible = false
                frame.dispose()
            }
        }

        @Test
        fun `ensureTarget fills blank target field from Host`() {
            val target = JTextField("")
            val request = JTextArea("GET /x HTTP/2\r\nHost: api.example.com\r\n\r\n").also {
                it.isEditable = true
            }
            val strip = JTabbedPane()
            strip.addTab("1", JPanel())
            val root = JPanel(BorderLayout()).also {
                it.add(target, BorderLayout.NORTH)
                it.add(strip, BorderLayout.CENTER)
                it.add(request, BorderLayout.SOUTH)
            }
            val suite = JTabbedPane()
            suite.addTab("Repeater", root)
            suite.selectedIndex = 0
            val frame = JFrame().also { it.contentPane.add(suite) }
            val discovered =
                (RepeaterUiDiscovery.discoverRepeaterFromRoot(frame) as RepeaterUiDiscovery.Outcome.Ok).value
            RepeaterSend.ensureTargetUrl(discovered, 0)
            assertEquals("https://api.example.com", target.text)
        }

        @Test
        fun `pickBestSend prefers exact Send with largest area`() {
            val tiny = JButton("Send").also {
                it.setSize(10, 10)
            }
            val big = JButton("Send").also {
                it.setSize(80, 28)
            }
            // width/height used by pickBestSend — setSize alone may not stick without layout
            tiny.setBounds(0, 0, 10, 10)
            big.setBounds(0, 0, 80, 28)
            assertEquals(big, RepeaterSend.pickBestSend(listOf(tiny, big)))
        }
    }

    @Nested
    inner class ClickPath {
        @Test
        fun `resolveSendControl finds cancel-adjacent Send`() {
            val clicks = AtomicInteger(0)
            val tree = buildTreeWithSend(clicks)
            tree.frame.pack()
            tree.frame.isVisible = true
            try {
                val discovered =
                    (RepeaterUiDiscovery.discoverRepeaterFromRoot(tree.frame) as RepeaterUiDiscovery.Outcome.Ok).value
                val resolved = RepeaterSend.resolveSendControl(discovered)
                assertTrue(resolved is RepeaterUiDiscovery.Outcome.Ok)
                assertEquals(tree.send, (resolved as RepeaterUiDiscovery.Outcome.Ok).value)
            } finally {
                tree.frame.isVisible = false
                tree.frame.dispose()
            }
        }
    }

    private data class Tree(val frame: JFrame, val send: JButton)

    private fun buildTreeWithSend(clicks: AtomicInteger): Tree {
        val send = JButton("Send").also { btn ->
            btn.addActionListener { clicks.incrementAndGet() }
        }
        val cancel = JButton("Cancel").also { it.isEnabled = false }
        val sharedRequest = JTextArea("GET /a HTTP/1.1\r\nHost: example.com\r\n\r\n").also {
            it.isEditable = true
        }
        val sharedResponse = JTextArea("").also { it.isEditable = false }
        val editors = JPanel(BorderLayout()).also {
            it.add(sharedRequest, BorderLayout.WEST)
            it.add(sharedResponse, BorderLayout.EAST)
        }
        val toolbar = JPanel().also {
            it.add(send)
            it.add(cancel)
        }
        val strip = JTabbedPane()
        strip.addTab("1", JPanel())
        strip.addTab("2", JPanel())
        strip.selectedIndex = 0
        val repeaterRoot = JPanel(BorderLayout()).also {
            it.add(toolbar, BorderLayout.NORTH)
            it.add(strip, BorderLayout.CENTER)
            it.add(editors, BorderLayout.SOUTH)
        }
        val suite = JTabbedPane()
        suite.addTab("Proxy", JPanel())
        suite.addTab("Repeater", repeaterRoot)
        suite.selectedIndex = 1
        val frame = JFrame().also { it.contentPane.add(suite) }
        return Tree(frame, send)
    }
}
