package com.backend.movie.exception

/** One broken value rule, so a form can show the message next to its field. */
data class FieldError(val field: String, val message: String)

/** Every broken rule from one request, collected so the user sees them all at once. */
class ValidationException(val fieldErrors: List<FieldError>) :
    RuntimeException(fieldErrors.joinToString("; ") { it.message })

class NotFoundException(message: String) : RuntimeException(message)

class AlreadyExistsException(message: String) : RuntimeException(message)
