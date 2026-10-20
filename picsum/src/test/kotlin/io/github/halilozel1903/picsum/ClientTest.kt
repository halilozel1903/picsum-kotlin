package io.github.halilozel1903.picsum

import io.github.halilozel1903.picsum.transport.PicsumResponse
import io.github.halilozel1903.picsum.transport.PicsumTransport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientTest {

    @Test
    fun decodesAPageOfTheList() = runTest {
        MockApi().use { api ->
            val page = api.client.list(page = 1, limit = 5)
            assertEquals(listOf("${Picsum.BASE_URL}v2/list?page=1&limit=5"), api.urls)
            assertEquals(listOf("0", "1", "2", "3", "4"), page.photos.map { it.id })
            val first = page.photos.first()
            assertEquals("Alejandro Escamilla", first.author)
            assertEquals(5000, first.width)
            assertEquals(3333, first.height)
            assertEquals("https://unsplash.com/photos/yC-Yzbqy7PY", first.url)
            assertEquals("https://picsum.photos/id/0/5000/3333", first.downloadUrl)
            assertEquals(1, page.page)
            assertEquals(5, page.limit)
        }
    }

    @Test
    fun readsPagingFromTheLinkHeader() = runTest {
        MockApi().use { api ->
            val first = api.client.list(1, 5)
            assertNull(first.previous)
            assertEquals(2, first.next!!.page)
            assertEquals(5, first.next!!.limit)

            val second = api.client.next(first)!!
            assertEquals(listOf("5", "6", "7", "8", "9"), second.photos.map { it.id })
            assertEquals(1, second.previous!!.page)
            assertEquals(3, second.next!!.page)

            val third = api.client.next(second)!!
            assertFalse(third.hasNext)
            assertTrue(third.hasPrevious)
            assertNull(api.client.next(third))

            assertEquals("0", api.client.previous(second)!!.photos.first().id)
            // next() follows the url from the header.
            assertEquals("${Picsum.BASE_URL}v2/list?page=2&limit=5", api.urls[1])
        }
    }

    @Test
    fun pagesFollowLinksUntilTheEnd() = runTest {
        MockApi().use { api ->
            val pages = api.client.pages(limit = 5).toList()
            assertEquals(listOf(1, 2, 3), pages.map { it.page })
            assertEquals(15, pages.sumOf { it.photos.size })
            assertEquals(3, api.requests.size)
        }
    }

    @Test
    fun pagesStopAtMaxPages() = runTest {
        MockApi().use { api ->
            val photos = api.client.photos(limit = 5, maxPages = 2).toList()
            assertEquals((0..9).map { it.toString() }, photos.map { it.id })
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun pagesStopAtAnEmptyPage() = runTest {
        MockApi().use { api ->
            val page = api.client.list(page = 9, limit = 5)
            assertTrue(page.photos.isEmpty())
            assertFalse(page.hasNext)
            assertTrue(api.client.pages(limit = 5, startPage = 9).toList().isEmpty())
        }
    }

    @Test
    fun loadsPhotoInfoByIdAndSeed() = runTest {
        MockApi().use { api ->
            val puppy = api.client.photo(237)
            assertEquals("André Spieker", puppy.author)
            assertEquals(3500, puppy.width)
            assertEquals("8wTPqxlnKM4", puppy.unsplashId)

            val seeded = api.client.photoForSeed("picsum")
            assertEquals("1015", seeded.id)
            assertEquals(
                listOf("${Picsum.BASE_URL}id/237/info", "${Picsum.BASE_URL}seed/picsum/info"),
                api.urls,
            )
        }
    }

    @Test
    fun mapsAMissingPhotoToNotFound() = runTest {
        MockApi().use { api ->
            val error = assertFailsWith<PicsumException.NotFound> { api.client.photo("86") }
            assertEquals("${Picsum.BASE_URL}id/86/info", error.url)
            assertEquals("Image does not exist", error.body?.trim())
        }
    }

    @Test
    fun mapsOtherStatuses() = runTest {
        MockApi(override = { Route("not-found.txt", status = HttpStatusCode.ServiceUnavailable) }).use { api ->
            val error = assertFailsWith<PicsumException.Http> { api.client.list() }
            assertEquals(503, error.status)
        }
    }

    @Test
    fun retriesServerErrors() = runTest {
        var calls = 0
        MockApi(
            override = { path ->
                calls++
                if (calls < 3) Route("not-found.txt", status = HttpStatusCode.BadGateway) else routes.getValue(path)
            },
            configure = {
                maxRetries = 2
                retryDelayMillis = 10
            },
        ).use { api ->
            assertEquals("André Spieker", api.client.photo("237").author)
            assertEquals(3, api.requests.size)
        }
    }

    @Test
    fun sendsAUserAgent() = runTest {
        MockApi().use { api ->
            api.client.photo("237")
            assertTrue(api.requests.single().headers[HttpHeaders.UserAgent]!!.startsWith("picsum-kotlin/"))
        }
    }

    @Test
    fun usesACustomBaseUrl() = runTest {
        val seen = mutableListOf<String>()
        val client = PicsumClient {
            baseUrl = "https://photos.example.com"
            transport = PicsumTransport { url -> seen += url; PicsumResponse(fixture("id-237-info.json")) }
        }
        client.photo("237")
        assertEquals(listOf("https://photos.example.com/id/237/info"), seen)
        assertEquals("https://photos.example.com/id/1/10/20", client.image("1", 10, 20).url)
        assertEquals("https://photos.example.com/40/40?random=2", client.randomImage(40, cacheBuster = 2).url)
    }

    @Test
    fun resolvesRelativeLinks() = runTest {
        val client = PicsumClient {
            transport = PicsumTransport {
                PicsumResponse(fixture("list-1-5.json"), mapOf("link" to "</v2/list?page=2&limit=5>; rel=next"))
            }
        }
        val next = client.list(1, 5).next!!
        assertEquals("${Picsum.BASE_URL}v2/list?page=2&limit=5", next.url)
    }

    @Test
    fun wrapsTransportFailures() = runTest {
        val client = PicsumClient { transport = PicsumTransport { throw java.io.IOException("offline") } }
        val error = assertFailsWith<PicsumException.Network> { client.photo("1") }
        assertEquals("${Picsum.BASE_URL}id/1/info", error.url)
    }

    @Test
    fun reportsDecodingErrors() = runTest {
        val client = PicsumClient { transport = PicsumTransport { PicsumResponse("<html>maintenance</html>") } }
        assertFailsWith<PicsumException.Decoding> { client.list() }
        assertFailsWith<PicsumException.Decoding> { client.photo("1") }
    }

    @Test
    fun validatesArgumentsBeforeSending() = runTest {
        MockApi().use { api ->
            assertFailsWith<IllegalArgumentException> { api.client.list(page = 0) }
            assertFailsWith<IllegalArgumentException> { api.client.list(limit = 101) }
            assertFailsWith<IllegalArgumentException> { api.client.photo(" ") }
            assertFailsWith<IllegalArgumentException> { api.client.photoForSeed("") }
            assertFailsWith<IllegalArgumentException> { api.client.pages(maxPages = 0) }
            assertTrue(api.requests.isEmpty())
        }
        assertFailsWith<IllegalArgumentException> { PicsumClient { maxRetries = -1 } }
    }
}
