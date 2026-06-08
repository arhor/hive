package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.InstantIso8601Serializer
import io.github.arhor.catrecognizer.domain.RecognitionError
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RecognitionLatestResponse(
    val status: CatPresenceStatus?,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant?,
    val confidence: Double?,
    val source: String?,
    val error: RecognitionError?,
    val worker: WorkerSummary,
)
