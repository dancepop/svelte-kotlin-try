package com.backend.media.enums

private const val MB = 1024L * 1024L
private val IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private val VIDEO_TYPES = setOf("video/mp4", "video/webm")

/** Each kind's folder on disk and its upload rules. These are rules with tests, so they're constants. */
enum class MediaKind(val folder: String, val maxBytes: Long, val allowedTypes: Set<String>) {
    ARTWORK("artwork", 5 * MB, IMAGE_TYPES),
    PREVIEW("preview", 25 * MB, VIDEO_TYPES),
    FULL("full", 500 * MB, VIDEO_TYPES),
}
