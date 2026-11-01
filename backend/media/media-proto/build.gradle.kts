plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    `java-library`
    `maven-publish`
}

// Publish as com.backend.commons with artifactId media-proto
// i.e. com.backend.commons:media-proto:0.0.1-SNAPSHOT
group = "com.backend.commons"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(platform(libs.spring.boot.bom))
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.grpc.kotlin.stub)
    api(libs.protobuf.kotlin)
    api(libs.kotlinx.coroutines.core)
    api(libs.protovalidate)
}

// Apparently just standard configuration for proto projects
protobuf {
    protoc {
        artifact = libs.protoc.compiler.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = "${libs.protoc.gen.grpc.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")   // Java stubs (the Kotlin stubs build on them)
                create("grpckt") // Kotlin coroutine stubs: suspend functions and Flows
            }
            task.builtins {
                // This lets projects that import this module to use kotlin style builders instead of java style
                create("kotlin") // Kotlin DSL builders, e.g. fileInfo { totalSize = 100 }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
