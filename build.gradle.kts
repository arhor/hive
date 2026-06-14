plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.quarkus) apply false
    idea
}

idea {
    println()

    module {
        excludeDirs.addAll(
            listOf(
                rootDir.resolve("esphome/config/.esphome"),
                rootDir.resolve("homeassistant/config/.cache"),
                rootDir.resolve("homeassistant/config/.storage"),
            )
        )
    }
}
