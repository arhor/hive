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
        val frameBytes: ByteArray? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Snapshot) return false
            return latestResult == other.latestResult &&
                lastSuccessAt == other.lastSuccessAt &&
                lastError == other.lastError &&
                consecutiveFailures == other.consecutiveFailures &&
                (frameBytes == null && other.frameBytes == null ||
                    frameBytes != null && other.frameBytes != null && frameBytes.contentEquals(other.frameBytes))
        }

        override fun hashCode(): Int {
            var result = latestResult?.hashCode() ?: 0
            result = 31 * result + (lastSuccessAt?.hashCode() ?: 0)
            result = 31 * result + (lastError?.hashCode() ?: 0)
            result = 31 * result + consecutiveFailures
            result = 31 * result + (frameBytes?.contentHashCode() ?: 0)
            return result
        }
    }

    private val state = AtomicReference(Snapshot())

    fun recordSuccess(result: RecognitionResult, frameBytes: ByteArray? = null) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastSuccessAt = result.observedAt,
                lastError = null,
                consecutiveFailures = 0,
                frameBytes = frameBytes ?: it.frameBytes,
            )
        }
    }

    fun recordFailure(result: RecognitionResult, frameBytes: ByteArray? = null) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastError = result.error,
                consecutiveFailures = it.consecutiveFailures + 1,
                frameBytes = frameBytes ?: it.frameBytes,
            )
        }
    }

    fun snapshot(): Snapshot = state.get()
}
