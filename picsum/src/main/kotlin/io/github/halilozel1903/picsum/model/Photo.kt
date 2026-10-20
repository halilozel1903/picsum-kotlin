package io.github.halilozel1903.picsum.model

import io.github.halilozel1903.picsum.Picsum
import io.github.halilozel1903.picsum.PicsumImage
import io.github.halilozel1903.picsum.PhotoSize
import io.github.halilozel1903.picsum.WallpaperSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One photo of Lorem Picsum, as `/v2/list`, `/id/{id}/info` and `/seed/{seed}/info` return it.
 *
 * Every Picsum photo comes from [Unsplash](https://unsplash.com); [author] is the photographer and [url] is the
 * photo's page on Unsplash. Show the author next to the photo.
 */
@Serializable
public data class Photo(
    /** The Picsum id, for `/id/{id}/...` urls. Ids are numeric text and not contiguous. */
    public val id: String,
    /** The photographer's name. */
    public val author: String,
    /** The original width in pixels. */
    public val width: Int,
    /** The original height in pixels. */
    public val height: Int,
    /** The photo's page on Unsplash. */
    public val url: String,
    /** The Picsum url of the photo at its original size. */
    @SerialName("download_url") public val downloadUrl: String,
) {
    /** The original size. */
    public val size: PhotoSize get() = PhotoSize(width, height)

    /** Width divided by height. */
    public val aspectRatio: Double get() = width.toDouble() / height

    /** Whether the photo is wider than tall. */
    public val isLandscape: Boolean get() = width > height

    /** Whether the photo is taller than wide. */
    public val isPortrait: Boolean get() = height > width

    /** The id of the photo on Unsplash (the last path segment of [url]), or `null` when [url] is not an Unsplash url. */
    public val unsplashId: String?
        get() {
            val path = url.substringAfter("://", "").takeIf { it.startsWith("unsplash.com/") || it.startsWith("www.unsplash.com/") }
                ?: return null
            return path.substringBefore('?').trimEnd('/').substringAfterLast('/').takeIf { it.isNotEmpty() && '.' !in it }
        }

    /** This photo at [width] x [height]. Picsum crops to that aspect ratio around the center. */
    public fun image(width: Int, height: Int = width): PicsumImage = Picsum.id(id, width, height)

    /** This photo at [size]. */
    public fun image(size: PhotoSize): PicsumImage = Picsum.id(id, size)

    /**
     * This photo at its original aspect ratio, as large as fits in [maxWidth] x [maxHeight] without upscaling and
     * within Picsum's [Picsum.MAX_DIMENSION].
     */
    public fun fitting(maxWidth: Int, maxHeight: Int): PicsumImage =
        image(size.fitInside(PhotoSize(maxWidth, maxHeight)).limitedTo(Picsum.MAX_DIMENSION))

    /** This photo [width] pixels wide at its original aspect ratio, for grids and lists. */
    public fun thumbnail(width: Int): PicsumImage = image(size.withWidth(width).limitedTo(Picsum.MAX_DIMENSION))

    /** This photo at its original size, scaled down to [Picsum.MAX_DIMENSION] when a side is larger. */
    public fun original(): PicsumImage = image(size.limitedTo(Picsum.MAX_DIMENSION))

    /** This photo cropped to the aspect ratio of [wallpaper], at its size (scaled down to Picsum's limit). */
    public fun wallpaper(wallpaper: WallpaperSize): PicsumImage = image(wallpaper.picsumSize)
}
