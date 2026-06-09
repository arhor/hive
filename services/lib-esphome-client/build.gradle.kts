plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.google.protobuf") version "0.10.0"
}

group = "io.github.arhor.esphome.client"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
        javaParameters = true
    }
    jvmToolchain(25)
}

protobuf {
    protoc {
        // Download the compiler artifact
        artifact = "com.google.protobuf:protoc:4.35.0"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Generates Kotlin extensions (DSL) for the Java classes
                create("kotlin")
            }
        }
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin:4.35.0")
}
