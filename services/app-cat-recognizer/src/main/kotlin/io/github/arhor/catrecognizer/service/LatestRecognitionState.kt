package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@ApplicationScoped
class LatestRecognitionState {

    data class Snapshot(
        val latestResult: RecognitionResult? = null,
        val lastSuccessAt: Instant? = null,
        val lastError: RecognitionError? = null,
        val consecutiveFailures: Int = 0,
    )

    private val state = AtomicReference(Snapshot())

    fun recordSuccess(result: RecognitionResult) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastSuccessAt = result.observedAt,
                lastError = null,
                consecutiveFailures = 0,
            )
        }
    }

    fun recordFailure(result: RecognitionResult) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastError = result.error,
                consecutiveFailures = it.consecutiveFailures + 1,
            )
        }
    }

    fun snapshot(): Snapshot = state.get()
}
