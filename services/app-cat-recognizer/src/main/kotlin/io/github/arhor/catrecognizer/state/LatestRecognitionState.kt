package io.github.arhor.catrecognizer.state

import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@ApplicationScoped
class LatestRecognitionState {

    data class Snapshot(
        val workerEnabled: Boolean = false,
        val workerRunning: Boolean = false,
        val latestResult: RecognitionResult? = null,
        val lastSuccessAt: Instant? = null,
        val lastError: RecognitionError? = null,
        val consecutiveFailures: Int = 0,
    )

    private val state = AtomicReference(Snapshot())

    fun markWorkerEnabled(enabled: Boolean) {
        state.updateAndGet { it.copy(workerEnabled = enabled) }
    }

    fun markWorkerRunning(running: Boolean) {
        state.updateAndGet { it.copy(workerRunning = running) }
    }

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
