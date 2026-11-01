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

// Each service is owned by a "team": a -proto module (published .proto contract) and an -app module
// (the Spring Boot app, layered into controller/service/repository packages).
// `movie` and `media` are plain folders; only the modules listed here have a build.gradle.kts.
// These are just project paths
include(
    "movie:movie-proto",
    "movie:movie-app",
    "media:media-proto",
    "media:media-app",
)
