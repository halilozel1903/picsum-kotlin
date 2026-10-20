package io.github.halilozel1903.picsum

import java.net.URLEncoder

/**
 * Entry point for Picsum image urls. Each function returns a [PicsumImage] that you refine and turn into a url:
 *
 * ```kotlin
 * Picsum.id("237", 600, 400).url                               // https://picsum.photos/id/237/600/400
 * Picsum.id("237", 600, 400).grayscale().blur(2).url           // .../id/237/600/400?grayscale&blur=2
 * Picsum.seed("picsum", 800).webp().url                        // .../seed/picsum/800/800.webp
 * Picsum.random(1200, 800, cacheBuster = 3).url                // .../1200/800?random=3
 * ```
 */
public object Picsum {
    /** The Lorem Picsum root, ending with a slash. */
    public const val BASE_URL: String = "https://picsum.photos/"

    /** The largest width or height Picsum serves. */
    public const val MAX_DIMENSION: Int = 5000

    /** The strongest blur Picsum applies. */
    public const val MAX_BLUR: Int = 10

    /** Photo [id] at [width] x [height] (a square when [height] is left out): `/id/{id}/{width}/{height}`. */
    public fun id(id: String, width: Int, height: Int = width): PicsumImage =
        PicsumImage(ImageSource.Id(id), width, height)

    /** Photo [id] at [size]. */
    public fun id(id: String, size: PhotoSize): PicsumImage = id(id, size.width, size.height)

    /**
     * The photo that [seed] maps to, at [width] x [height]: `/seed/{seed}/{width}/{height}`. The same seed always
     * gives the same photo, so seeds make stable placeholders, for example a user id as the seed of an avatar.
     */
    public fun seed(seed: String, width: Int, height: Int = width): PicsumImage =
        PicsumImage(ImageSource.Seed(seed), width, height)

    /** The photo of [seed] at [size]. */
    public fun seed(seed: String, size: PhotoSize): PicsumImage = seed(seed, size.width, size.height)

    /**
     * A random photo at [width] x [height]: `/{width}/{height}`. Several random images on one screen share one url and
     * so one cached image; give each a different [cacheBuster] (`?random=n`) to get different photos.
     */
    public fun random(width: Int, height: Int = width, cacheBuster: Int? = null): PicsumImage =
        PicsumImage(ImageSource.Random, width, height, cacheBuster = cacheBuster)

    /** A random photo at [size]. */
    public fun random(size: PhotoSize, cacheBuster: Int? = null): PicsumImage =
        random(size.width, size.height, cacheBuster)
}

/** Which photo an image url asks for. */
public sealed interface ImageSource {
    /** A random photo, different on each request unless cached. */
    public data object Random : ImageSource

    /** A photo by its Picsum id. */
    public data class Id(public val id: String) : ImageSource {
        init {
            require(id.isNotBlank()) { "id must not be blank" }
        }
    }

    /** The photo a seed maps to. */
    public data class Seed(public val seed: String) : ImageSource {
        init {
            require(seed.isNotBlank()) { "seed must not be blank" }
        }
    }
}

/** The file type of an image url. Picsum sends JPEG without an extension. */
public enum class ImageFormat(public val extension: String) {
    /** No extension: JPEG. */
    Default(""),

    /** `.jpg`. */
    Jpg(".jpg"),

    /** `.webp`, smaller at the same quality. */
    Webp(".webp"),
}

/**
 * An immutable description of a Picsum image url. Every function returns a changed copy, so one base image can be
 * turned into many variants (a thumbnail, a blurred backdrop, a grayscale version) and [url] is rebuilt each time.
 *
 * Widths and heights must be positive. Picsum serves at most [Picsum.MAX_DIMENSION] pixels per side; the size helpers
 * of [PhotoSize] and [io.github.halilozel1903.picsum.model.Photo] keep to that.
 */
public data class PicsumImage(
    /** Which photo. */
    public val source: ImageSource,
    /** The width in pixels. */
    public val width: Int,
    /** The height in pixels. */
    public val height: Int,
    /** Whether the photo is turned to grayscale (`?grayscale`). */
    public val grayscale: Boolean = false,
    /** The blur, 0 for none or 1 to [Picsum.MAX_BLUR] (`?blur=n`). */
    public val blur: Int = 0,
    /** The file type. */
    public val format: ImageFormat = ImageFormat.Default,
    /** `?random=n`, only for [ImageSource.Random]: makes otherwise equal random urls differ. */
    public val cacheBuster: Int? = null,
    /** The Picsum root, ending with a slash. */
    public val baseUrl: String = Picsum.BASE_URL,
) {
    init {
        require(width > 0 && height > 0) { "Size must be positive, was ${width}x$height" }
        require(blur in 0..Picsum.MAX_BLUR) { "blur must be 0 (off) to ${Picsum.MAX_BLUR}, was $blur" }
        require(cacheBuster == null || source == ImageSource.Random) { "cacheBuster only applies to random images" }
        require(baseUrl.endsWith('/')) { "baseUrl must end with a slash" }
    }

    /** The size. */
    public val size: PhotoSize get() = PhotoSize(width, height)

    /** Whether [blur] is on. */
    public val isBlurred: Boolean get() = blur > 0

    /** With grayscale turned on or off. */
    public fun grayscale(enabled: Boolean = true): PicsumImage = copy(grayscale = enabled)

    /** With a blur of [level], 1 (light) to 10 (strong); 0 turns it off. */
    public fun blur(level: Int): PicsumImage = copy(blur = level)

    /** Without blur. */
    public fun noBlur(): PicsumImage = copy(blur = 0)

    /** With [format]. */
    public fun format(format: ImageFormat): PicsumImage = copy(format = format)

    /** As `.jpg`. */
    public fun jpg(): PicsumImage = format(ImageFormat.Jpg)

    /** As `.webp`. */
    public fun webp(): PicsumImage = format(ImageFormat.Webp)

    /** At [width] x [height]. */
    public fun size(width: Int, height: Int = width): PicsumImage = copy(width = width, height = height)

    /** At [size]. */
    public fun size(size: PhotoSize): PicsumImage = size(size.width, size.height)

    /** With `?random=[value]`; a random image only. */
    public fun cacheBuster(value: Int?): PicsumImage = copy(cacheBuster = value)

    /** The image url. */
    public val url: String
        get() = buildString {
            append(baseUrl)
            when (source) {
                ImageSource.Random -> Unit
                is ImageSource.Id -> append("id/").append(segment(source.id)).append('/')
                is ImageSource.Seed -> append("seed/").append(segment(source.seed)).append('/')
            }
            append(width).append('/').append(height).append(format.extension)
            val query = buildList<String> {
                if (grayscale) add("grayscale")
                if (blur > 0) add("blur=$blur")
                cacheBuster?.let { add("random=$it") }
            }
            if (query.isNotEmpty()) append('?').append(query.joinToString("&"))
        }

    /** The url of the photo's info (`/id/{id}/info` or `/seed/{seed}/info`), or `null` for a random image. */
    public val infoUrl: String?
        get() = when (source) {
            ImageSource.Random -> null
            is ImageSource.Id -> "${baseUrl}id/${segment(source.id)}/info"
            is ImageSource.Seed -> "${baseUrl}seed/${segment(source.seed)}/info"
        }

    /** Same as [url]. */
    override fun toString(): String = url

    private companion object {
        fun segment(value: String): String = URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")
    }
}
