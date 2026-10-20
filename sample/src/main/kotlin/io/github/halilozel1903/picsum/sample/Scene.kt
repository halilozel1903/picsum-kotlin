package io.github.halilozel1903.picsum.sample

/**
 * Screenshot scenes, picked with the `scene` intent extra. They load live data from Lorem Picsum like a normal launch,
 * starting on a fixed [tab] and, for the detail scenes, on one photo with preset effects. [loads] is how many loads
 * must finish before the scene is complete.
 */
enum class Scene(
    val key: String,
    val tab: Tab,
    val photoId: String? = null,
    val grayscale: Boolean = false,
    val blur: Int = 0,
    val galleryPage: Int = 1,
    val loads: Int = 1,
) {
    /** Page 3 of `/v2/list` in the staggered grid, so the paging buttons both show. */
    Gallery("tablet-gallery", Tab.Gallery, galleryPage = 3),

    /** `/id/1015/info`: the photo with its author, original size and the wallpaper picker. */
    Detail("tablet-detail", Tab.Gallery, photoId = "1015"),

    /** `/id/870/info` with grayscale and blur 4 applied, the effect combination Picsum's own docs show on this photo. */
    Effects("tablet-effects", Tab.Gallery, photoId = "870", grayscale = true, blur = 4),

    /** Two pages of 100 from `/v2/list`, followed through the Link header and grouped by author. */
    Authors("tablet-authors", Tab.Authors),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key || it.key == "tablet-$key" }
    }
}

/** The two destinations of the navigation rail. */
enum class Tab(val label: String) {
    Gallery("Gallery"),
    Authors("Authors"),
}
