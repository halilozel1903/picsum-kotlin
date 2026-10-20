package io.github.halilozel1903.picsum.model

/**
 * One page of `/v2/list`. [next] and [previous] come from the response's `Link` header, so paging follows what the
 * server says instead of guessing: the last page has no [next], the first has no [previous].
 */
public data class PhotoPage(
    /** The photos of this page, in id order. */
    public val photos: List<Photo>,
    /** The page number that was requested, starting at 1. */
    public val page: Int,
    /** The page size that was requested. */
    public val limit: Int,
    /** The next page, from `Link: <...>; rel="next"`. */
    public val next: PageRequest?,
    /** The previous page, from `Link: <...>; rel="prev"`. */
    public val previous: PageRequest?,
) {
    /** Whether there is a next page. */
    public val hasNext: Boolean get() = next != null

    /** Whether there is a previous page. */
    public val hasPrevious: Boolean get() = previous != null
}

/** A page of `/v2/list` to load, as a `Link` header announced it. */
public data class PageRequest(
    /** The page number. */
    public val page: Int,
    /** The page size. */
    public val limit: Int,
    /** The url the server gave for it. */
    public val url: String,
)

/** The photos of one photographer. */
public data class AuthorPhotos(
    /** The photographer's name, as Picsum spells it. */
    public val author: String,
    /** Their photos, in the order they were given. */
    public val photos: List<Photo>,
) {
    /** How many photos there are. */
    public val count: Int get() = photos.size
}

/**
 * Groups photos by [Photo.author]: the photographers with the most photos first, then by name. Names are compared
 * exactly as Picsum spells them.
 */
public fun Iterable<Photo>.groupByAuthor(): List<AuthorPhotos> =
    groupBy { it.author }
        .map { (author, photos) -> AuthorPhotos(author, photos) }
        .sortedWith(compareByDescending<AuthorPhotos> { it.count }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.author })
