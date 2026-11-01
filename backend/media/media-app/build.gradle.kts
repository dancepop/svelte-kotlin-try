plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.backend.media"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":media:media-proto"))
    implementation(project(":movie:movie-proto"))
    implementation(libs.spring.boot.starter.grpc.server)
    implementation(libs.spring.boot.starter.grpc.client)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.grpc.server.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
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