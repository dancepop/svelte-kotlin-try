package com.backend.movie

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing

@EnableJdbcAuditing
@SpringBootApplication
class MovieApplication

fun main(args: Array<String>) {
    runApplication<MovieApplication>(*args)
}
