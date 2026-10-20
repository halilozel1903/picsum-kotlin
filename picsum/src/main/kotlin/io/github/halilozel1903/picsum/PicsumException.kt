package io.github.halilozel1903.picsum

/** Every failure of [PicsumClient]. Catch the subclasses you care about, or all of them. */
public sealed class PicsumException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Picsum answered 404: there is no photo with that id. Picsum's ids are not contiguous. */
    public class NotFound(url: String, public val body: String? = null) :
        PicsumException(url, "Not found: $url" + describe(body))

    /** Picsum answered with another unsuccessful [status], after the retries for 429 and 5xx. */
    public class Http(url: String, public val status: Int, public val body: String? = null) :
        PicsumException(url, "HTTP $status for $url" + describe(body))

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        PicsumException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        PicsumException(url, "Could not decode the response of $url: ${cause.message}", cause)

    private companion object {
        fun describe(body: String?): String = body?.trim()?.takeIf { it.isNotEmpty() }?.take(200)?.let { ": $it" } ?: ""
    }
}
