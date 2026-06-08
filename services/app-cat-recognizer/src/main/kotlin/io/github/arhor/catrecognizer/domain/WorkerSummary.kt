package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.domain.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class WorkerSummary(
    @Serializable(with = InstantIso8601Serializer::class)
    val lastSuccessAt: Instant?,
    val consecutiveFailures: Int,
    val lastErrorCode: String?,
)
