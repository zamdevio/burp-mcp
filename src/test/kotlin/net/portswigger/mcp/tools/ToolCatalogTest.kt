package net.portswigger.mcp.tools

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToolCatalogTest {

    @BeforeEach
    fun setUp() {
        ToolCatalog.clear()
    }

    @Test
    fun `built-in catalog includes core and repeater tools`() {
        ToolCatalog.ensureBuiltIn()
        val names = ToolCatalog.all().map { it.name }.toSet()
        assertTrue(names.contains("url_encode"))
        assertTrue(names.contains("list_repeater_tabs"))
        assertTrue(names.contains("send_repeater_tab"))
        assertTrue(names.contains("send_http1_request"))
        assertTrue(names.contains("get_scanner_issues"))
        assertTrue(ToolCatalog.all().size >= 30)
    }

    @Test
    fun `community edition hides Pro tools`() {
        ToolCatalog.clear()
        ToolCatalog.ensureBuiltIn()
        val community = ToolCatalog.visibleForEdition(professional = false).map { it.name }.toSet()
        assertTrue("url_encode" in community)
        assertTrue("get_scanner_issues" !in community)
        assertTrue("generate_collaborator_payload" !in community)
        val pro = ToolCatalog.visibleForEdition(professional = true).map { it.name }.toSet()
        assertTrue("get_scanner_issues" in pro)
    }

    @Test
    fun `record upserts and infer tags for repeater send`() {
        ToolCatalog.record("send_repeater_tab", "Click Send", ToolCatalog.inferTags("send_repeater_tab"))
        val entry = ToolCatalog.all().single()
        assertEquals("Repeater", entry.group)
        assertTrue(ToolTag.Swing in entry.tags)
        assertTrue(ToolTag.Mutating in entry.tags)
    }

    @Test
    fun `ensureBuiltIn merges missing tools after partial register`() {
        ToolCatalog.record("url_encode", "URL encodes the input string")
        ToolCatalog.ensureBuiltIn()
        val names = ToolCatalog.all().map { it.name }.toSet()
        assertTrue("url_encode" in names)
        assertTrue("get_scanner_issues" in names)
    }
}
