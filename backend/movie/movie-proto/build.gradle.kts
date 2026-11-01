// The Movie team's published contract: .proto files + generated Java/Kotlin stubs.
// Consumers (other services) depend on this module, never on movie-app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    `java-library`
    `maven-publish`
}

// Publish as com.backend.commons with artifactId movie-proto
// i.e. com.backend.commons:movie-proto:0.0.1-SNAPSHOT
group = "com.backend.commons"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

dependencies {
    // `api` (not `implementation`): the generated code exposes these types, so consumers need them too.
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
                // This lets projects that import this module to use kotlin style builders instead of java
                create("kotlin") // Kotlin DSL builders, e.g. movie { title = "Heat" }
            }
        }
    }
}

// `./gradlew :movie:movie-proto:publishToMavenLocal` exports this contract as a jar (the .proto files are included).
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
