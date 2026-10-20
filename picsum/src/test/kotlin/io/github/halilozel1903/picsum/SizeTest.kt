package io.github.halilozel1903.picsum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SizeTest {

    @Test
    fun fitsInsideABox() {
        assertEquals(PhotoSize(1200, 800), PhotoSize(6000, 4000).fitInside(PhotoSize(1200, 1200)))
        assertEquals(PhotoSize(533, 800), PhotoSize(4000, 6000).fitInside(PhotoSize(1200, 800)))
        assertEquals(PhotoSize(1600, 900), aspectFit(PhotoSize(3840, 2160), PhotoSize(1600, 1600)))
    }

    @Test
    fun doesNotUpscaleUnlessAsked() {
        assertEquals(PhotoSize(300, 200), PhotoSize(300, 200).fitInside(PhotoSize(1200, 1200)))
        assertEquals(PhotoSize(1200, 800), PhotoSize(300, 200).fitInside(PhotoSize(1200, 1200), allowUpscale = true))
    }

    @Test
    fun fillsABox() {
        assertEquals(PhotoSize(2400, 1600), PhotoSize(6000, 4000).fill(PhotoSize(1600, 1600)))
        assertEquals(PhotoSize(1920, 1280), aspectFill(PhotoSize(3, 2), PhotoSize(1920, 1080)))
        val filled = PhotoSize(5000, 3333).fill(PhotoSize(1080, 2400))
        assertTrue(filled.width >= 1080 && filled.height == 2400)
    }

    @Test
    fun scalesToAWidthOrHeight() {
        assertEquals(PhotoSize(400, 267), PhotoSize(5000, 3333).withWidth(400))
        assertEquals(PhotoSize(600, 400), PhotoSize(3000, 2000).withHeight(400))
        assertEquals(PhotoSize(1000, 1), PhotoSize(100000, 10).withWidth(1000))
    }

    @Test
    fun limitsToPicsumsMaximum() {
        assertEquals(PhotoSize(5000, 3333), PhotoSize(6000, 4000).limitedTo())
        assertEquals(PhotoSize(3333, 5000), PhotoSize(4000, 6000).limitedTo())
        assertEquals(PhotoSize(4000, 3000), PhotoSize(4000, 3000).limitedTo())
    }

    @Test
    fun describesOrientation() {
        assertTrue(PhotoSize(3, 2).isLandscape)
        assertTrue(PhotoSize(2, 3).isPortrait)
        assertTrue(PhotoSize(5, 5).isSquare)
        assertEquals(PhotoSize(2, 3), PhotoSize(3, 2).rotated())
        assertEquals(1.5, PhotoSize(3, 2).aspectRatio)
        assertEquals("5000 x 3333", PhotoSize(5000, 3333).toString())
    }

    @Test
    fun rejectsEmptySizes() {
        assertFailsWith<IllegalArgumentException> { PhotoSize(0, 1) }
        assertFailsWith<IllegalArgumentException> { PhotoSize(1, 1).withWidth(0) }
        assertFailsWith<IllegalArgumentException> { PhotoSize(1, 1).scaled(0.0) }
    }

    @Test
    fun hasWallpaperPresets() {
        assertEquals(10, WallpaperSize.presets.size)
        assertEquals(listOf("Pixel Tablet", "iPad Pro 13\""), WallpaperSize.presets(DeviceCategory.Tablet).map { it.name })
        assertTrue(WallpaperSize.presets(DeviceCategory.Phone).all { it.height > it.width })
        assertTrue(WallpaperSize.presets(DeviceCategory.Desktop).all { it.width > it.height })
        assertEquals(PhotoSize(3840, 2160), WallpaperSize.Desktop4k.picsumSize)
    }

    @Test
    fun turnsAndLimitsWallpapers() {
        val tablet = WallpaperSize.PixelTablet
        assertEquals(PhotoSize(1600, 2560), tablet.portrait().size)
        assertEquals(tablet, tablet.landscape())
        val huge = WallpaperSize.forScreen(7680, 4320, "8K")
        assertEquals(5000, huge.picsumSize.width)
        assertTrue(huge.picsumSize.height in 2812..2813)
        assertEquals(DeviceCategory.Custom, huge.category)
        assertEquals("8K, 7680 x 4320", huge.toString())
    }
}
