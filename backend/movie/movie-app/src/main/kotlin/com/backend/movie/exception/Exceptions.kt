package com.backend.movie.exception

class ValidationException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : RuntimeException(message)

class AlreadyExistsException(message: String) : RuntimeException(message)
