package com.backend.media.service

/** What a file really is, read from its bytes. */
data class FileType(val contentType: String, val extension: String)

private val JPEG = FileType("image/jpeg", "jpg")
private val PNG = FileType("image/png", "png")
private val WEBP = FileType("image/webp", "webp")
private val MP4 = FileType("video/mp4", "mp4")
private val WEBM = FileType("video/webm", "webm")

private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val WEBM_SIGNATURE = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())
private val RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
private val WEBP_MARKER = "WEBP".toByteArray(Charsets.US_ASCII)
private val MP4_MARKER = "ftyp".toByteArray(Charsets.US_ASCII)

/** How many of a file's first bytes [detectFileType] needs to recognise every type. */
const val SIGNATURE_LENGTH = 12

/**
 * Identifies a file from its first bytes. The browser's file name and claimed type are ignored,
 * so a renamed file can't get past this. Returns null for anything unrecognised, including a file that's too short.
 */
fun detectFileType(head: ByteArray): FileType? = when {
    head.hasBytesAt(0, JPEG_SIGNATURE) -> JPEG
    head.hasBytesAt(0, PNG_SIGNATURE) -> PNG
    // RIFF, then 4 bytes of size, then WEBP.
    head.hasBytesAt(0, RIFF) && head.hasBytesAt(8, WEBP_MARKER) -> WEBP
    // 4 bytes of box size, then ftyp.
    head.hasBytesAt(4, MP4_MARKER) -> MP4
    head.hasBytesAt(0, WEBM_SIGNATURE) -> WEBM
    else -> null
}

/** The content type for a stored file, which is named with the extension [detectFileType] chose. */
fun contentTypeOf(storageKey: String): String =
    when (storageKey.substringAfterLast('.')) {
        JPEG.extension -> JPEG.contentType
        PNG.extension -> PNG.contentType
        WEBP.extension -> WEBP.contentType
        MP4.extension -> MP4.contentType
        WEBM.extension -> WEBM.contentType
        else -> "application/octet-stream"
    }

private fun ByteArray.hasBytesAt(offset: Int, expected: ByteArray): Boolean =
    size >= offset + expected.size && expected.indices.all { this[offset + it] == expected[it] }
