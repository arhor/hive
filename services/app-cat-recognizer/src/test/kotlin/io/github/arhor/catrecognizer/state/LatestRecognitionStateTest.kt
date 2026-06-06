package io.github.arhor.catrecognizer.state

import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LatestRecognitionStateTest {

    @Test
    fun `initial snapshot uses safe defaults`() {
        val snapshot = LatestRecognitionState().snapshot()

        assertEquals(false, snapshot.workerEnabled)
        assertEquals(false, snapshot.workerRunning)
        assertNull(snapshot.latestResult)
        assertNull(snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }

    @Test
    fun `worker enabled and running flags update independently`() {
        val state = LatestRecognitionState()

        state.markWorkerEnabled(true)
        state.markWorkerRunning(true)
        assertEquals(true, state.snapshot().workerEnabled)
        assertEquals(true, state.snapshot().workerRunning)

        state.markWorkerRunning(false)
        state.markWorkerEnabled(false)
        assertEquals(false, state.snapshot().workerEnabled)
        assertEquals(false, state.snapshot().workerRunning)
    }

    @Test
    fun `successful result resets last error and consecutive failures`() {
        val state = LatestRecognitionState()
        val failure = failureResult(errorCode = "FRAME_FETCH_FAILED")
        val result = successResult()

        state.markWorkerEnabled(true)
        state.markWorkerRunning(true)
        state.recordFailure(failure)
        state.recordSuccess(result)

        val snapshot = state.snapshot()
        assertEquals(true, snapshot.workerEnabled)
        assertEquals(true, snapshot.workerRunning)
        assertEquals(result, snapshot.latestResult)
        assertEquals(result.observedAt, snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }

    @Test
    fun `failed result stores last error and increments consecutive failures`() {
        val state = LatestRecognitionState()
        val firstFailure = failureResult(
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            errorCode = "FRAME_FETCH_FAILED",
        )
        val secondFailure = failureResult(
            observedAt = Instant.parse("2026-06-05T12:01:00Z"),
            errorCode = "DETECTOR_FAILED",
        )

        state.recordFailure(firstFailure)
        state.recordFailure(secondFailure)

        val snapshot = state.snapshot()
        assertEquals(secondFailure, snapshot.latestResult)
        assertEquals("DETECTOR_FAILED", snapshot.lastError?.code)
        assertEquals(2, snapshot.consecutiveFailures)
        assertNull(snapshot.lastSuccessAt)
    }

    private fun successResult(): RecognitionResult = RecognitionResult(
        status = CatPresenceStatus.DETECTED,
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        confidence = 0.9,
        detectorMode = "ALWAYS_PRESENT",
        source = "snapshot",
    )

    private fun failureResult(
        observedAt: Instant = Instant.parse("2026-06-05T11:59:00Z"),
        errorCode: String,
    ): RecognitionResult = RecognitionResult(
        status = CatPresenceStatus.UNKNOWN,
        observedAt = observedAt,
        confidence = null,
        detectorMode = "STUB",
        source = "snapshot",
        error = RecognitionError(
            code = errorCode,
            message = "recognition failed",
            retriable = true,
        ),
    )
}
