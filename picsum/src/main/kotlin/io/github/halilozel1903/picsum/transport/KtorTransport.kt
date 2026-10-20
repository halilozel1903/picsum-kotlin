package io.github.halilozel1903.picsum.transport

import io.github.halilozel1903.picsum.PicsumException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * The default transport: GET requests with Ktor. Network failures, 429 and 5xx answers are retried [maxRetries] times
 * with exponential backoff from [retryDelayMillis]. Closes the client only when it created it.
 */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String,
    requestTimeoutMillis: Long,
    private val maxRetries: Int,
    private val retryDelayMillis: Long,
) : PicsumTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): PicsumResponse {
        var attempt = 0
        while (true) {
            val response: HttpResponse = try {
                client.get(url) {
                    header(HttpHeaders.UserAgent, userAgent)
                    accept(ContentType.Application.Json)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt < maxRetries) {
                    delay(backoff(attempt++))
                    continue
                }
                throw PicsumException.Network(url, e)
            }

            val status = response.status.value
            if (response.status.isSuccess()) {
                val body = try {
                    response.bodyAsText()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw PicsumException.Network(url, e)
                }
                val headers = buildMap<String, String> {
                    response.headers.names().forEach { name ->
                        put(name, response.headers.getAll(name).orEmpty().joinToString(", "))
                    }
                }
                return PicsumResponse(body, headers)
            }

            val body = runCatching { response.bodyAsText() }.getOrNull()
            if ((status == 429 || status >= 500) && attempt < maxRetries) {
                delay(backoff(attempt++))
                continue
            }
            throw if (status == 404) PicsumException.NotFound(url, body) else PicsumException.Http(url, status, body)
        }
    }

    private fun backoff(attempt: Int): Long = retryDelayMillis shl attempt.coerceAtMost(6)

    override fun close() {
        if (ownsClient) client.close()
    }
}
