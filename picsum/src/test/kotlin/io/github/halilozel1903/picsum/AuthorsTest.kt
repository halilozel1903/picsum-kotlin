package io.github.halilozel1903.picsum

import io.github.halilozel1903.picsum.model.groupByAuthor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthorsTest {

    @Test
    fun groupsAPageByAuthor() = runTest {
        MockApi().use { api ->
            val page = api.client.list(page = 1, limit = 30)
            assertEquals(30, page.photos.size)
            val authors = page.photos.groupByAuthor()
            assertEquals(
                listOf("Alejandro Escamilla", "Paul Jarvis", "Go Wild", "Aleks Dorohovich", "Jerry Adney", "Vadim Sherbakov", "Yoni Kaplan-Nadel"),
                authors.map { it.author },
            )
            assertEquals(listOf(14, 10, 2, 1, 1, 1, 1), authors.map { it.count })
            assertEquals(listOf("25", "29"), authors[2].photos.map { it.id })
            assertEquals(30, authors.sumOf { it.count })
        }
    }

    @Test
    fun readsPhotoDetails() = runTest {
        MockApi().use { api ->
            val photos = api.client.list(page = 1, limit = 30).photos
            val tall = photos.single { it.id == "23" }
            assertTrue(tall.isPortrait)
            assertEquals(3887.0 / 4899.0, tall.aspectRatio)
            assertTrue(photos.first().isLandscape)
            assertEquals("yC-Yzbqy7PY", photos.first().unsplashId)
            assertNull(photos.first().copy(url = "https://example.com/x.jpg").unsplashId)
        }
    }
}
