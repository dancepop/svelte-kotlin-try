plugins {
    // Downloads the JDK requested by `jvmToolchain` if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "backend"

// One place to declare repositories for every module (like <repositories> in a parent POM).
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Each service is owned by a "team": an -api module (published .proto contract) and a -service module
// (the Spring Boot app, layered into controller/service/repository packages).
// `movie` and `media` are plain folders; only the modules listed here have a build.gradle.kts.
include(
    "movie:movie-api",
    "movie:movie-service",
    "media:media-api",
    "media:media-service",
)
