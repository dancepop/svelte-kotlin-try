plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.backend.movie"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":movie:movie-api"))
    // The client stubs for calling media-service.
    implementation(project(":media:media-api"))
    implementation(libs.spring.boot.starter.grpc.server)
    implementation(libs.spring.boot.starter.grpc.client)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.kotlin.reflect)
    // Only needed when the app runs, not to compile it (Maven's <scope>runtime</scope>).
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.grpc.server.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Only build the executable Spring Boot jar (skip the extra "-plain" jar), so Docker can copy a single file.
tasks.named<Jar>("jar") {
    enabled = false
}
