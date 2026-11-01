package com.backend.media.exception

/** A broken rule, such as a malformed movie id or chunks in the wrong order. */
class ValidationException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : RuntimeException(message)

/** movie-app couldn't be reached, or failed in a way the user can't act on. */
class MovieServiceUnavailableException(cause: Throwable) : RuntimeException("movie-app is unavailable", cause)
