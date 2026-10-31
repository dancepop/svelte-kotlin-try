package com.backend.movie.controller

import com.backend.movie.exception.FieldError
import com.backend.movie.exception.ValidationException
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val MAX_TITLE_LENGTH = 200
private const val MAX_SYNOPSIS_LENGTH = 5000
private const val MIN_RUNTIME_MINUTES = 1
private const val MAX_RUNTIME_MINUTES = 1000
private const val MAX_NAME_LENGTH = 200
private const val MAX_BIO_LENGTH = 5000
private const val MAX_PAGE_SIZE = 100

internal fun movieFieldErrors(
    title: String,
    synopsis: String?,
    releaseDate: String?,
    runtimeMinutes: Int?,
): List<FieldError> = buildList {
    if (title.isBlank()) add(FieldError("title", "Title is required"))
    if (title.trim().length > MAX_TITLE_LENGTH) {
        add(FieldError("title", "Title must be $MAX_TITLE_LENGTH characters or fewer"))
    }
    if (synopsis != null && synopsis.length > MAX_SYNOPSIS_LENGTH) {
        add(FieldError("synopsis", "Synopsis must be $MAX_SYNOPSIS_LENGTH characters or fewer"))
    }
    if (releaseDate != null && !isValidDate(releaseDate)) {
        add(FieldError("releaseDate", "Release date must be a real date in the format YYYY-MM-DD"))
    }
    if (runtimeMinutes != null && runtimeMinutes !in MIN_RUNTIME_MINUTES..MAX_RUNTIME_MINUTES) {
        add(FieldError("runtimeMinutes", "Runtime must be $MIN_RUNTIME_MINUTES to $MAX_RUNTIME_MINUTES minutes"))
    }
}

internal fun personFieldErrors(name: String, bio: String?): List<FieldError> = buildList {
    if (name.isBlank()) add(FieldError("name", "Name is required"))
    if (name.trim().length > MAX_NAME_LENGTH) {
        add(FieldError("name", "Name must be $MAX_NAME_LENGTH characters or fewer"))
    }
    if (bio != null && bio.length > MAX_BIO_LENGTH) {
        add(FieldError("bio", "Bio must be $MAX_BIO_LENGTH characters or fewer"))
    }
}

internal fun pagingErrors(page: Int, pageSize: Int): List<FieldError> = buildList {
    if (page < 0) add(FieldError("page", "Page must be 0 or greater"))
    if (pageSize !in 1..MAX_PAGE_SIZE) add(FieldError("pageSize", "Page size must be 1 to $MAX_PAGE_SIZE"))
}

internal fun idCountErrors(field: String, count: Int): List<FieldError> = buildList {
    if (count > MAX_PAGE_SIZE) add(FieldError(field, "At most $MAX_PAGE_SIZE ids are allowed"))
}

internal fun requireValid(errors: List<FieldError>) {
    if (errors.isNotEmpty()) throw ValidationException(errors)
}

// LocalDate.parse is strict: 2025-02-29 is rejected, not rounded to March 1.
private fun isValidDate(value: String): Boolean = try {
    LocalDate.parse(value)
    true
} catch (_: DateTimeParseException) {
    false
}
