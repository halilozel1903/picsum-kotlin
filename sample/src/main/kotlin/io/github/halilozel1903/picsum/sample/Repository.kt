package io.github.halilozel1903.picsum.sample

import io.github.halilozel1903.picsum.PicsumClient
import io.github.halilozel1903.picsum.model.AuthorPhotos
import io.github.halilozel1903.picsum.model.Photo
import io.github.halilozel1903.picsum.model.PhotoPage
import io.github.halilozel1903.picsum.model.groupByAuthor
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.toList
import java.util.concurrent.ConcurrentHashMap

/** The authors view: the photographers of the first [photoCount] photos, most photos first. */
data class AuthorsIndex(val photoCount: Int, val authors: List<AuthorPhotos>)

/**
 * The sample's only data source: the live Lorem Picsum API through [PicsumClient]. There is no bundled or fake data;
 * when Picsum cannot be reached the screen shows the error. Photo info that a list page already returned is reused.
 */
class GalleryRepository(private val client: PicsumClient) {
    private val known = ConcurrentHashMap<String, Photo>()

    suspend fun page(page: Int): PhotoPage =
        client.list(page = page, limit = PAGE_SIZE).also { result -> result.photos.forEach { known[it.id] = it } }

    suspend fun photo(id: String): Photo = known[id] ?: client.photo(id).also { known[id] = it }

    /** The first [AUTHOR_PAGES] pages of 100, followed through the Link header, grouped by author. */
    suspend fun authors(): AuthorsIndex {
        val photos = client.photos(limit = 100, maxPages = AUTHOR_PAGES).toList()
        photos.forEach { known[it.id] = it }
        return AuthorsIndex(photos.size, photos.groupByAuthor())
    }

    companion object {
        const val PAGE_SIZE = 30
        const val AUTHOR_PAGES = 2

        val shared: GalleryRepository by lazy {
            GalleryRepository(
                PicsumClient {
                    // Shares OkHttp with Coil.
                    engine = OkHttp.create()
                    userAgent = "picsum-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/picsum-kotlin)"
                    maxRetries = 3
                    retryDelayMillis = 1_000
                },
            )
        }
    }
}
