package com.backend.movie.service

/** Long, because `page * pageSize` can overflow an Int. A huge page just returns nothing. */
internal fun offsetOf(page: Int, pageSize: Int): Long = page.toLong() * pageSize

/**
 * Turns a search term into an ILIKE pattern that matches it anywhere in the text, or "" for no filter.
 * `%` and `_` are wildcards in ILIKE, so they're escaped, or searching `100%` would match everything.
 * The backslash goes first, so the ones added for the wildcards aren't escaped again.
 */
internal fun likePattern(term: String): String {
    val trimmed = term.trim()
    if (trimmed.isEmpty()) return ""
    val escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return "%$escaped%"
}
