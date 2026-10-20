package io.github.halilozel1903.picsum

import io.github.halilozel1903.picsum.model.Photo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ImageUrlTest {

    private val puppy = Photo(
        id = "237",
        author = "André Spieker",
        width = 3500,
        height = 2095,
        url = "https://unsplash.com/photos/8wTPqxlnKM4",
        downloadUrl = "https://picsum.photos/id/237/3500/2095",
    )

    @Test
    fun buildsIdSeedAndRandomUrls() {
        assertEquals("https://picsum.photos/id/237/200/300", Picsum.id("237", 200, 300).url)
        assertEquals("https://picsum.photos/id/237/200/200", Picsum.id("237", 200).url)
        assertEquals("https://picsum.photos/seed/picsum/200/300", Picsum.seed("picsum", 200, 300).url)
        assertEquals("https://picsum.photos/200/300", Picsum.random(200, 300).url)
    }

    @Test
    fun addsGrayscaleAndBlurInPicsumsOrder() {
        val base = Picsum.id("870", 200, 300)
        assertEquals("https://picsum.photos/id/870/200/300?grayscale", base.grayscale().url)
        assertEquals("https://picsum.photos/id/870/200/300?blur=2", base.blur(2).url)
        assertEquals("https://picsum.photos/id/870/200/300?grayscale&blur=2", base.blur(2).grayscale().url)
        assertEquals("https://picsum.photos/id/870/200/300?blur=10", base.blur(10).url)
        // Turning effects off rebuilds the plain url.
        assertEquals(base.url, base.grayscale().blur(5).grayscale(false).noBlur().url)
        assertEquals(base.url, base.blur(4).blur(0).url)
    }

    @Test
    fun addsTheFormatExtensionToThePath() {
        assertEquals("https://picsum.photos/id/237/200/300.jpg", Picsum.id("237", 200, 300).jpg().url)
        assertEquals("https://picsum.photos/200/300.webp", Picsum.random(200, 300).webp().url)
        assertEquals(
            "https://picsum.photos/seed/picsum/200/300.webp?grayscale&blur=3",
            Picsum.seed("picsum", 200, 300).webp().grayscale().blur(3).url,
        )
        assertEquals("https://picsum.photos/id/1/10/10", Picsum.id("1", 10).webp().format(ImageFormat.Default).url)
    }

    @Test
    fun bustsTheCacheOfRandomImages() {
        assertEquals("https://picsum.photos/200/300?random=1", Picsum.random(200, 300, cacheBuster = 1).url)
        assertEquals(
            "https://picsum.photos/200/300?grayscale&blur=1&random=7",
            Picsum.random(200, 300).cacheBuster(7).grayscale().blur(1).url,
        )
        assertFailsWith<IllegalArgumentException> { Picsum.id("1", 10).cacheBuster(1) }
    }

    @Test
    fun encodesSeeds() {
        assertEquals("https://picsum.photos/seed/user%2042/64/64", Picsum.seed("user 42", 64).url)
        assertEquals("https://picsum.photos/seed/a%2Fb%3Fc/64/64", Picsum.seed("a/b?c", 64).url)
    }

    @Test
    fun hasInfoUrls() {
        assertEquals("https://picsum.photos/id/237/info", Picsum.id("237", 1).infoUrl)
        assertEquals("https://picsum.photos/seed/picsum/info", Picsum.seed("picsum", 1).infoUrl)
        assertNull(Picsum.random(1).infoUrl)
    }

    @Test
    fun rejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { Picsum.id("1", 0, 10) }
        assertFailsWith<IllegalArgumentException> { Picsum.id("1", 10, -1) }
        assertFailsWith<IllegalArgumentException> { Picsum.id("1", 10).blur(11) }
        assertFailsWith<IllegalArgumentException> { Picsum.id("1", 10).blur(-1) }
        assertFailsWith<IllegalArgumentException> { Picsum.id(" ", 10) }
        assertFailsWith<IllegalArgumentException> { Picsum.seed("", 10) }
    }

    @Test
    fun buildsUrlsFromAPhoto() {
        assertEquals("https://picsum.photos/id/237/3500/2095", puppy.original().url)
        assertEquals(puppy.downloadUrl, puppy.original().url)
        assertEquals("https://picsum.photos/id/237/400/239", puppy.thumbnail(400).url)
        assertEquals("https://picsum.photos/id/237/1200/718", puppy.fitting(1200, 1200).url)
        assertEquals("https://picsum.photos/id/237/2560/1600", puppy.wallpaper(WallpaperSize.PixelTablet).url)
        assertEquals("https://picsum.photos/id/237/300/300?blur=1", puppy.image(300).blur(1).url)
    }

    @Test
    fun keepsLargePhotosWithinPicsumsLimit() {
        val river = puppy.copy(id = "1015", width = 6000, height = 4000)
        assertEquals("https://picsum.photos/id/1015/5000/3333", river.original().url)
        assertEquals("https://picsum.photos/id/1015/1600/1067", river.fitting(1600, 1600).url)
    }
}
