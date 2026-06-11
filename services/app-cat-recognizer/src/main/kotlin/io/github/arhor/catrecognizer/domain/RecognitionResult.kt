package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RecognitionResult(
    val status: CatPresenceStatus,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant,
    val confidence: Double? = null,
    val source: String,
    val error: RecognitionError? = null,
    val boundingBoxes: List<BoundingBox>? = null,
)
