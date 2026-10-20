package io.github.halilozel1903.picsum

import io.github.halilozel1903.picsum.model.PageRequest
import io.github.halilozel1903.picsum.model.Photo
import io.github.halilozel1903.picsum.model.PhotoPage
import io.github.halilozel1903.picsum.transport.KtorTransport
import io.github.halilozel1903.picsum.transport.PicsumResponse
import io.github.halilozel1903.picsum.transport.PicsumTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutine client for [Lorem Picsum](https://picsum.photos): the photo list, photo info by id or seed, and image
 * urls for those photos.
 *
 * ```kotlin
 * val client = PicsumClient()
 * val page = client.list(page = 1, limit = 30)        // photos, plus page.next from the Link header
 * val photo = client.photo("237")
 * val url = photo.fitting(1200, 800).grayscale().blur(2).url
 * ```
 *
 * Image urls need no request: build them with [Picsum] or from a [Photo] and hand them to your image loader. Every
 * function here is main-safe and throws a [PicsumException] on failure. Close the client when you no longer need it.
 */
public class PicsumClient(configure: PicsumConfig.() -> Unit = {}) : AutoCloseable {

    private val config = PicsumConfig().apply(configure)

    init {
        require(config.maxRetries >= 0) { "maxRetries must not be negative, was ${config.maxRetries}" }
        require(config.retryDelayMillis >= 0) { "retryDelayMillis must not be negative, was ${config.retryDelayMillis}" }
    }

    /** The API root, always ending with a slash. */
    public val baseUrl: String = config.baseUrl.let { if (it.endsWith('/')) it else "$it/" }

    private val transport: PicsumTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
        maxRetries = config.maxRetries,
        retryDelayMillis = config.retryDelayMillis,
    )

    // region Photo list

    /**
     * `GET /v2/list?page={page}&limit={limit}`: one page of photos in id order, with the next and previous pages from
     * the `Link` header. A page past the end has no photos.
     */
    public suspend fun list(page: Int = 1, limit: Int = DEFAULT_LIMIT): PhotoPage {
        require(page >= 1) { "page starts at 1, was $page" }
        require(limit in 1..MAX_LIMIT) { "limit must be 1 to $MAX_LIMIT, was $limit" }
        return loadPage(listUrl(page, limit), page, limit)
    }

    /** Loads [request], a page a `Link` header announced, from the url the server gave. */
    public suspend fun list(request: PageRequest): PhotoPage = loadPage(request.url, request.page, request.limit)

    /** The page after [page], or `null` when the `Link` header announced none. */
    public suspend fun next(page: PhotoPage): PhotoPage? = page.next?.let { list(it) }

    /** The page before [page], or `null` when the `Link` header announced none. */
    public suspend fun previous(page: PhotoPage): PhotoPage? = page.previous?.let { list(it) }

    /**
     * Every page from [startPage] on, following the `Link` header's `next` until there is none, a page is empty, or
     * [maxPages] pages were emitted. Pages are loaded as they are collected.
     */
    public fun pages(limit: Int = DEFAULT_LIMIT, startPage: Int = 1, maxPages: Int = Int.MAX_VALUE): Flow<PhotoPage> {
        require(maxPages >= 1) { "maxPages must be at least 1, was $maxPages" }
        require(startPage >= 1) { "page starts at 1, was $startPage" }
        require(limit in 1..MAX_LIMIT) { "limit must be 1 to $MAX_LIMIT, was $limit" }
        return flow {
            var page: PhotoPage? = list(startPage, limit)
            var emitted = 0
            while (page != null && page.photos.isNotEmpty()) {
                emit(page)
                if (++emitted >= maxPages) break
                page = next(page)
            }
        }
    }

    /** The photos of up to [maxPages] pages of [limit], one after the other. */
    public fun photos(limit: Int = MAX_LIMIT, maxPages: Int = Int.MAX_VALUE): Flow<Photo> = flow {
        pages(limit, maxPages = maxPages).collect { page -> page.photos.forEach { emit(it) } }
    }

    // endregion

    // region Photo info

    /** `GET /id/{id}/info`: one photo. Throws [PicsumException.NotFound] for an id Picsum does not have. */
    public suspend fun photo(id: String): Photo {
        require(id.isNotBlank()) { "id must not be blank" }
        return fetch(url("id/${segment(id)}/info"), Photo.serializer())
    }

    /** `GET /id/{id}/info` for a numeric id. */
    public suspend fun photo(id: Int): Photo = photo(id.toString())

    /** `GET /seed/{seed}/info`: the photo that [seed] maps to, the one `/seed/{seed}/{w}/{h}` shows. */
    public suspend fun photoForSeed(seed: String): Photo {
        require(seed.isNotBlank()) { "seed must not be blank" }
        return fetch(url("seed/${segment(seed)}/info"), Photo.serializer())
    }

    // endregion

    // region Image urls

    /** [Picsum.id] on this client's [baseUrl]. */
    public fun image(id: String, width: Int, height: Int = width): PicsumImage =
        Picsum.id(id, width, height).copy(baseUrl = baseUrl)

    /** [Picsum.seed] on this client's [baseUrl]. */
    public fun seedImage(seed: String, width: Int, height: Int = width): PicsumImage =
        Picsum.seed(seed, width, height).copy(baseUrl = baseUrl)

    /** [Picsum.random] on this client's [baseUrl]. */
    public fun randomImage(width: Int, height: Int = width, cacheBuster: Int? = null): PicsumImage =
        Picsum.random(width, height, cacheBuster).copy(baseUrl = baseUrl)

    // endregion

    // region Generic access

    /** Loads and decodes any url with [deserializer], through the configured transport. */
    public suspend fun <T> fetch(url: String, deserializer: DeserializationStrategy<T>): T =
        decode(url, get(url).body, deserializer)

    /** The url of [path] below [baseUrl]. */
    public fun url(path: String): String = baseUrl + path.trimStart('/')

    // endregion

    private fun listUrl(page: Int, limit: Int): String = url("v2/list?page=$page&limit=$limit")

    private suspend fun loadPage(url: String, page: Int, limit: Int): PhotoPage {
        val response = get(url)
        val photos = decode(url, response.body, ListSerializer(Photo.serializer()))
        val links = LinkHeader.parse(response.header("Link"))
        return PhotoPage(
            photos = photos,
            page = page,
            limit = limit,
            next = links["next"]?.let { pageRequest(it, limit) },
            previous = links["prev"]?.let { pageRequest(it, limit) } ?: links["previous"]?.let { pageRequest(it, limit) },
        )
    }

    private fun pageRequest(link: String, fallbackLimit: Int): PageRequest? {
        val page = LinkHeader.queryParameter(link, "page")?.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val limit = LinkHeader.queryParameter(link, "limit")?.toIntOrNull()?.takeIf { it >= 1 } ?: fallbackLimit
        // A relative link is resolved against the API root.
        val absolute = if (link.startsWith("http://") || link.startsWith("https://")) link else url(link)
        return PageRequest(page, limit, absolute)
    }

    private suspend fun get(url: String): PicsumResponse = try {
        transport.get(url)
    } catch (e: PicsumException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw PicsumException.Network(url, e)
    }

    private fun <T> decode(url: String, body: String, deserializer: DeserializationStrategy<T>): T = try {
        json.decodeFromString(deserializer, body)
    } catch (e: SerializationException) {
        throw PicsumException.Decoding(url, e)
    } catch (e: IllegalArgumentException) {
        throw PicsumException.Decoding(url, e)
    }

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    public companion object {
        /** The page size Picsum uses when none is given. */
        public const val DEFAULT_LIMIT: Int = 30

        /** The largest page size Picsum serves. */
        public const val MAX_LIMIT: Int = 100

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        private fun segment(value: String): String = URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")
    }
}
