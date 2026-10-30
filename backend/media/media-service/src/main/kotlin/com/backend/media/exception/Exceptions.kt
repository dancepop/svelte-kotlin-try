package com.backend.media.exception

/** A broken rule, such as a malformed movie id. [field] tells the form where to show the message. */
class ValidationException(val field: String, message: String) : RuntimeException(message)

class NotFoundException(message: String) : RuntimeException(message)

/** movie-service couldn't be reached, or failed in a way the user can't act on. */
class MovieServiceUnavailableException(cause: Throwable) : RuntimeException("movie-service is unavailable", cause)
