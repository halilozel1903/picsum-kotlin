package io.github.halilozel1903.picsum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkHeaderTest {

    @Test
    fun parsesPicsumsLinkHeader() {
        val links = LinkHeader.parse(
            "<https://picsum.photos/v2/list?page=1&limit=30>; rel=\"prev\", " +
                "<https://picsum.photos/v2/list?page=3&limit=30>; rel=\"next\"",
        )
        assertEquals(
            mapOf(
                "prev" to "https://picsum.photos/v2/list?page=1&limit=30",
                "next" to "https://picsum.photos/v2/list?page=3&limit=30",
            ),
            links,
        )
    }

    @Test
    fun acceptsUnquotedAndMultipleRelations() {
        val links = LinkHeader.parse("<a?page=1>; rel=first; title=\"x\", <b?page=9>;rel=\"last NEXT\"")
        assertEquals("a?page=1", links["first"])
        assertEquals("b?page=9", links["last"])
        assertEquals("b?page=9", links["next"])
    }

    @Test
    fun ignoresEmptyAndBrokenHeaders() {
        assertTrue(LinkHeader.parse(null).isEmpty())
        assertTrue(LinkHeader.parse("  ").isEmpty())
        assertTrue(LinkHeader.parse("<https://picsum.photos/v2/list?page=2>").isEmpty())
        assertTrue(LinkHeader.parse("not a link").isEmpty())
    }

    @Test
    fun readsQueryParameters() {
        val url = "https://picsum.photos/v2/list?page=3&limit=30"
        assertEquals("3", LinkHeader.queryParameter(url, "page"))
        assertEquals("30", LinkHeader.queryParameter(url, "limit"))
        assertEquals(null, LinkHeader.queryParameter(url, "size"))
    }
}
