package io.github.halilozel1903.picsum

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A size in pixels, with the aspect ratio math that image urls need: fit inside a box, fill a box, scale to a width
 * or a height, and stay within Picsum's [Picsum.MAX_DIMENSION]. Every result keeps the aspect ratio (up to rounding)
 * and is at least 1 x 1.
 */
public data class PhotoSize(public val width: Int, public val height: Int) {
    init {
        require(width > 0 && height > 0) { "Size must be positive, was ${width}x$height" }
    }

    /** Width divided by height. */
    public val aspectRatio: Double get() = width.toDouble() / height

    /** Whether the size is wider than tall. */
    public val isLandscape: Boolean get() = width > height

    /** Whether the size is taller than wide. */
    public val isPortrait: Boolean get() = height > width

    /** Whether both sides are equal. */
    public val isSquare: Boolean get() = width == height

    /** The size turned by 90 degrees. */
    public fun rotated(): PhotoSize = PhotoSize(height, width)

    /**
     * Aspect fit: the largest size with this aspect ratio that fits inside [bounds]. Without [allowUpscale] a size
     * that already fits is returned as is.
     */
    public fun fitInside(bounds: PhotoSize, allowUpscale: Boolean = false): PhotoSize {
        var scale = min(bounds.width.toDouble() / width, bounds.height.toDouble() / height)
        if (!allowUpscale) scale = min(scale, 1.0)
        return scaled(scale).clampTo(bounds)
    }

    /** Aspect fill: the smallest size with this aspect ratio that covers [bounds] entirely. */
    public fun fill(bounds: PhotoSize): PhotoSize {
        val scale = max(bounds.width.toDouble() / width, bounds.height.toDouble() / height)
        val result = scaled(scale)
        return PhotoSize(max(result.width, bounds.width), max(result.height, bounds.height))
    }

    /** This aspect ratio at [width], with the height to match. */
    public fun withWidth(width: Int): PhotoSize {
        require(width > 0) { "width must be positive, was $width" }
        return PhotoSize(width, (width / aspectRatio).roundToInt().coerceAtLeast(1))
    }

    /** This aspect ratio at [height], with the width to match. */
    public fun withHeight(height: Int): PhotoSize {
        require(height > 0) { "height must be positive, was $height" }
        return PhotoSize((height * aspectRatio).roundToInt().coerceAtLeast(1), height)
    }

    /** Scaled down so that neither side is larger than [maxSide]; unchanged when it already fits. */
    public fun limitedTo(maxSide: Int = Picsum.MAX_DIMENSION): PhotoSize {
        require(maxSide > 0) { "maxSide must be positive, was $maxSide" }
        return fitInside(PhotoSize(maxSide, maxSide))
    }

    /** Both sides multiplied by [factor], rounded, at least 1. */
    public fun scaled(factor: Double): PhotoSize {
        require(factor > 0 && factor.isFinite()) { "factor must be positive, was $factor" }
        return PhotoSize((width * factor).roundToInt().coerceAtLeast(1), (height * factor).roundToInt().coerceAtLeast(1))
    }

    /** `"5000 x 3333"`. */
    override fun toString(): String = "$width x $height"

    // Rounding can push a fitted side one pixel past the box.
    private fun clampTo(bounds: PhotoSize): PhotoSize =
        PhotoSize(min(width, max(bounds.width, 1)), min(height, max(bounds.height, 1)))
}

/** Aspect fit of [source] inside [bounds], without upscaling. See [PhotoSize.fitInside]. */
public fun aspectFit(source: PhotoSize, bounds: PhotoSize, allowUpscale: Boolean = false): PhotoSize =
    source.fitInside(bounds, allowUpscale)

/** Aspect fill of [source] over [bounds]. See [PhotoSize.fill]. */
public fun aspectFill(source: PhotoSize, bounds: PhotoSize): PhotoSize = source.fill(bounds)
