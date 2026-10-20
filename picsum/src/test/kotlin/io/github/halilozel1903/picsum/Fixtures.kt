package io.github.halilozel1903.picsum

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reads `src/test/resources/fixtures/<name>`. The fixtures are hand-written in the shape of Picsum's responses (field
 * names, string ids, Unsplash page urls, `download_url` at the original size); they are test data, not a recording.
 */
internal fun fixture(name: String): String =
    requireNotNull(MockApi::class.java.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()

private const val BASE = Picsum.BASE_URL

private fun list(page: Int, limit: Int) = "${BASE}v2/list?page=$page&limit=$limit"

/** A route: the fixture that answers a path below the API root, and the `Link` header it comes with. */
internal data class Route(val fixture: String, val link: String? = null, val status: HttpStatusCode = HttpStatusCode.OK)

/** Which fixture answers which path. Anything else is a 404 like Picsum's. The 5 per page list ends after page 3. */
internal val routes: Map<String, Route> = mapOf(
    "v2/list?page=1&limit=5" to Route("list-1-5.json", """<${list(2, 5)}>; rel="next""""),
    "v2/list?page=2&limit=5" to Route("list-2-5.json", """<${list(1, 5)}>; rel="prev", <${list(3, 5)}>; rel="next""""),
    "v2/list?page=3&limit=5" to Route("list-3-5.json", """<${list(2, 5)}>; rel="prev""""),
    "v2/list?page=9&limit=5" to Route("list-empty.json", """<${list(8, 5)}>; rel="prev""""),
    "v2/list?page=1&limit=30" to Route("list-1-30.json", """<${list(2, 30)}>; rel="next""""),
    "id/237/info" to Route("id-237-info.json"),
    "seed/picsum/info" to Route("seed-picsum-info.json"),
)

/** A client on a Ktor [MockEngine] that serves [routes], or [override] for every request when it is set. */
internal class MockApi(
    private val override: ((String) -> Route)? = null,
    configure: PicsumConfig.() -> Unit = {},
) : AutoCloseable {
    val requests: MutableList<HttpRequestData> = CopyOnWriteArrayList()

    val engine = MockEngine { request ->
        requests += request
        val path = request.url.toString().removePrefix(BASE)
        val route = override?.invoke(path) ?: routes[path] ?: Route("not-found.txt", status = HttpStatusCode.NotFound)
        val headers = if (route.link != null) {
            headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.Link to listOf(route.link))
        } else {
            headersOf(HttpHeaders.ContentType, "application/json")
        }
        respond(fixture(route.fixture), route.status, headers)
    }

    val client = PicsumClient {
        engine = this@MockApi.engine
        maxRetries = 0
        configure()
    }

    val urls: List<String> get() = requests.map { it.url.toString() }

    override fun close() {
        client.close()
        engine.close()
    }
}
