plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.quarkus)
}

group = "io.github.arhor.catrecognizer"

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

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.inject.Singleton")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.ws.rs.Path")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform))

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

    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.quarkus.junit)
    testImplementation(libs.rest.assured)
}
