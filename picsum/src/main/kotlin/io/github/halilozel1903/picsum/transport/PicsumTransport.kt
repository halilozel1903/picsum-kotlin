package io.github.halilozel1903.picsum.transport

/**
 * Fetches an API url. The client uses a Ktor based transport by default; supply your own through
 * `PicsumConfig.transport` to serve responses from files, a cache or another HTTP stack.
 *
 * Throw [io.github.halilozel1903.picsum.PicsumException] subclasses for HTTP errors; other exceptions are reported as
 * [io.github.halilozel1903.picsum.PicsumException.Network].
 */
public fun interface PicsumTransport {
    public suspend fun get(url: String): PicsumResponse
}

/** A successful response: its [body] and the [headers] the client reads (`Link` for paging). */
public class PicsumResponse(
    public val body: String,
    headers: Map<String, String> = emptyMap(),
) {
    private val headers: Map<String, String> = headers.mapKeys { it.key.lowercase() }

    /** The value of header [name], ignoring case, or `null`. */
    public fun header(name: String): String? = headers[name.lowercase()]
}
