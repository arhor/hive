plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.quarkus)
}

group = "io.github.arhor.catrecognizer"

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

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.inject.Singleton")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.ws.rs.Path")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform))
    implementation(project(":lib-esphome-client"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.onnxruntime)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.container.image.docker)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.opencv)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.kotlin)
    implementation(libs.quarkus.rest.kotlin.serialization)
    implementation(libs.quarkus.scheduler)
    implementation(libs.quarkus.smallrye.fault.tolerance)
    implementation(libs.quarkus.smallrye.health)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.quarkus.junit)
    testImplementation(libs.rest.assured)
}
