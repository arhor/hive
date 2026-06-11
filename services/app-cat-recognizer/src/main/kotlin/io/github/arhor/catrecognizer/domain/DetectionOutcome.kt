package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface DetectionOutcome {

    @Serializable
    data class Present(
        val confidence: Double?,
        val boundingBoxes: List<BoundingBox> = emptyList(),
    ) : DetectionOutcome

    @Serializable
    data class Absent(
        val confidence: Double?,
    ) : DetectionOutcome

    @Serializable
    data class Unknown(
        val reason: String,
    ) : DetectionOutcome
}
