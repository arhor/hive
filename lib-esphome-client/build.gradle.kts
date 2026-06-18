plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

group = "io.github.arhor.esphome.client"

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    compilerOptions {
        javaParameters = true
        freeCompilerArgs.addAll(
            "-XXLanguage:+ContextParameters",
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
            "-Xname-based-destructuring=complete",
        )
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("kotlin")
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.netty.buffer)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.protobuf)
    implementation(libs.netty.transport)
    implementation(libs.protobuf.kotlin)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
