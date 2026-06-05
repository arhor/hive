package io.github.arhor.catrecognizer.detection.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface DetectionOutcome {

    @Serializable
    data class Present(
        val confidence: Double,
    ) : DetectionOutcome

    @Serializable
    data class Absent(
        val confidence: Double,
    ) : DetectionOutcome

    @Serializable
    data class Unknown(
        val reason: String,
    ) : DetectionOutcome
}
