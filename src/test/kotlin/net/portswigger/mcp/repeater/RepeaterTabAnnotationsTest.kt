package net.portswigger.mcp.repeater

import burp.api.montoya.core.Annotations
import burp.api.montoya.http.message.HttpRequestResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepeaterTabAnnotationsTest {

    @Test
    fun `httpRequestResponse resolves single no-arg Montoya return`() {
        val annotations = mockk<Annotations>()
        every { annotations.notes() } returns "stored note"
        val hrr = mockk<HttpRequestResponse>()
        every { hrr.annotations() } returns annotations

        val bundle = TabBundleStub(hrr)

        val resolved = RepeaterTabAnnotations.httpRequestResponse(bundle)
        assertEquals(hrr, resolved)
        assertEquals("stored note", RepeaterTabAnnotations.readNotes(bundle))
    }

    @Test
    fun `writeNotes mutates annotations on bundle`() {
        var current = "before"
        val annotations = mockk<Annotations>()
        every { annotations.notes() } answers { current }
        every { annotations.setNotes(any()) } answers { current = firstArg() }
        val hrr = mockk<HttpRequestResponse>()
        every { hrr.annotations() } returns annotations

        val bundle = TabBundleStub(hrr)
        assertTrue(RepeaterTabAnnotations.writeNotes(bundle, "after"))
        assertEquals("after", RepeaterTabAnnotations.readNotes(bundle))
        verify { annotations.setNotes("after") }
    }

    @Test
    fun `ambiguous HttpRequestResponse methods returns null`() {
        val bundle = AmbiguousBundleStub()
        assertNull(RepeaterTabAnnotations.httpRequestResponse(bundle))
    }

    /** Mimics 2026.x tab bundle: one no-arg method returning [HttpRequestResponse]. */
    private class TabBundleStub(private val model: HttpRequestResponse) {
        @Suppress("unused")
        fun model(): HttpRequestResponse = model
    }

    private class AmbiguousBundleStub {
        @Suppress("unused")
        fun a(): HttpRequestResponse = mockk()

        @Suppress("unused")
        fun b(): HttpRequestResponse = mockk()
    }
}
