package io.github.halilozel1903.picsum

import io.github.halilozel1903.picsum.transport.PicsumTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [PicsumClient], set in its builder block. Every setting is optional; Picsum needs no key. */
public class PicsumConfig internal constructor() {
    /** The API root. Change it for a proxy or a self-hosted Picsum. */
    public var baseUrl: String = Picsum.BASE_URL

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] then does not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example to serve recorded responses. Retries do not apply to it. */
    public var transport: PicsumTransport? = null

    /** Sent with every request. */
    public var userAgent: String = "picsum-kotlin/1.0.0 (+https://github.com/halilozel1903/picsum-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 30_000

    /** How often a request that failed on the network, with 429 or with 5xx is tried again. */
    public var maxRetries: Int = 2

    /** The wait before the first retry; it doubles for each further one. */
    public var retryDelayMillis: Long = 500
}
