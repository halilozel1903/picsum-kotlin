package io.github.halilozel1903.picsum

/**
 * Parses an RFC 8288 `Link` header, as `/v2/list` sends it:
 *
 * ```
 * Link: <https://picsum.photos/v2/list?page=1&limit=30>; rel="prev", <https://picsum.photos/v2/list?page=3&limit=30>; rel="next"
 * ```
 */
public object LinkHeader {
    private val linkValue = Regex("""<([^>]*)>((?:\s*;\s*[^;,<]+)*)""")
    private val relParam = Regex("""(?i)\brel\s*=\s*(?:"([^"]*)"|([^\s;,"]+))""")

    /**
     * The urls of [header] by relation (`next`, `prev`, `first`, `last`, lowercased). A link with several relations
     * (`rel="prev first"`) is listed under each; when a relation repeats, the first link wins. Returns an empty map for
     * `null` or a header without links.
     */
    public fun parse(header: String?): Map<String, String> {
        if (header.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        for (match in linkValue.findAll(header)) {
            val url = match.groupValues[1].trim()
            val rel = relParam.find(match.groupValues[2]) ?: continue
            val names = (rel.groups[1]?.value ?: rel.groups[2]?.value).orEmpty()
            for (name in names.split(' ', '\t').filter { it.isNotEmpty() }) {
                result.putIfAbsent(name.lowercase(), url)
            }
        }
        return result
    }

    /** The value of query parameter [name] in [url], or `null`. */
    internal fun queryParameter(url: String, name: String): String? =
        url.substringAfter('?', "").substringBefore('#').split('&')
            .map { it.substringBefore('=') to it.substringAfter('=', "") }
            .firstOrNull { it.first == name }?.second
}
