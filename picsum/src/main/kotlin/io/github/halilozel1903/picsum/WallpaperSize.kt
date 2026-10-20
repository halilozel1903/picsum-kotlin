package io.github.halilozel1903.picsum

/** What kind of screen a [WallpaperSize] is for. */
public enum class DeviceCategory { Phone, Tablet, Desktop, Custom }

/**
 * A wallpaper size in pixels, for a device or a screen. Picsum crops a photo to any size you ask for, so
 * `photo.wallpaper(WallpaperSize.PixelTablet)` is that photo cropped to the tablet's screen. Sizes larger than
 * [Picsum.MAX_DIMENSION] are scaled down for the url ([picsumSize]) and keep their aspect ratio.
 *
 * The presets are the native resolutions of common screens, portrait for phones and in the device's natural
 * orientation for tablets; use [landscape] or [portrait] to turn one.
 */
public data class WallpaperSize(
    /** A short label, such as `"Pixel Tablet"`. */
    public val name: String,
    /** The width in pixels. */
    public val width: Int,
    /** The height in pixels. */
    public val height: Int,
    /** What kind of screen it is. */
    public val category: DeviceCategory = DeviceCategory.Custom,
) {
    init {
        require(width > 0 && height > 0) { "Size must be positive, was ${width}x$height" }
    }

    /** The size. */
    public val size: PhotoSize get() = PhotoSize(width, height)

    /** The size to ask Picsum for: [size], scaled down to [Picsum.MAX_DIMENSION] when a side is larger. */
    public val picsumSize: PhotoSize get() = size.limitedTo(Picsum.MAX_DIMENSION)

    /** This size with the longer side horizontal. */
    public fun landscape(): WallpaperSize = if (width >= height) this else copy(width = height, height = width)

    /** This size with the longer side vertical. */
    public fun portrait(): WallpaperSize = if (height >= width) this else copy(width = height, height = width)

    /** `"Pixel Tablet, 2560 x 1600"`. */
    override fun toString(): String = "$name, $width x $height"

    public companion object {
        /** A typical Android phone, 20:9. */
        public val PhoneFhd: WallpaperSize = WallpaperSize("Phone FHD+", 1080, 2400, DeviceCategory.Phone)

        /** A high end Android phone, 20:9. */
        public val PhoneQhd: WallpaperSize = WallpaperSize("Phone QHD+", 1440, 3200, DeviceCategory.Phone)

        /** Pixel 9 Pro. */
        public val Pixel9Pro: WallpaperSize = WallpaperSize("Pixel 9 Pro", 1280, 2856, DeviceCategory.Phone)

        /** iPhone 16 Pro. */
        public val IPhone16Pro: WallpaperSize = WallpaperSize("iPhone 16 Pro", 1206, 2622, DeviceCategory.Phone)

        /** Pixel Tablet, landscape. */
        public val PixelTablet: WallpaperSize = WallpaperSize("Pixel Tablet", 2560, 1600, DeviceCategory.Tablet)

        /** 13 inch iPad Pro (M4), portrait. */
        public val IPadPro13: WallpaperSize = WallpaperSize("iPad Pro 13\"", 2064, 2752, DeviceCategory.Tablet)

        /** Full HD desktop. */
        public val Desktop1080p: WallpaperSize = WallpaperSize("1080p", 1920, 1080, DeviceCategory.Desktop)

        /** QHD desktop. */
        public val Desktop1440p: WallpaperSize = WallpaperSize("1440p", 2560, 1440, DeviceCategory.Desktop)

        /** 4K UHD desktop. */
        public val Desktop4k: WallpaperSize = WallpaperSize("4K UHD", 3840, 2160, DeviceCategory.Desktop)

        /** 21:9 ultrawide monitor. */
        public val UltraWide: WallpaperSize = WallpaperSize("UltraWide", 3440, 1440, DeviceCategory.Desktop)

        /** Every preset, phones first. */
        public val presets: List<WallpaperSize> = listOf(
            PhoneFhd, PhoneQhd, Pixel9Pro, IPhone16Pro,
            PixelTablet, IPadPro13,
            Desktop1080p, Desktop1440p, Desktop4k, UltraWide,
        )

        /** The presets for one kind of screen. */
        public fun presets(category: DeviceCategory): List<WallpaperSize> = presets.filter { it.category == category }

        /**
         * The size of a screen of [widthPx] x [heightPx], for example from Android's `WindowMetrics`. The result
         * keeps the orientation that was passed in.
         */
        public fun forScreen(widthPx: Int, heightPx: Int, name: String = "This screen"): WallpaperSize =
            WallpaperSize(name, widthPx, heightPx, DeviceCategory.Custom)
    }
}
