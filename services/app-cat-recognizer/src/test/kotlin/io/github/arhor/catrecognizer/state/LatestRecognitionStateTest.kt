package io.github.arhor.catrecognizer.state

import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LatestRecognitionStateTest {

    @Test
    fun `records success and resets failure count`() {
        val state = LatestRecognitionState()
        val result = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = 0.9,
            detectorMode = "ALWAYS_PRESENT",
            source = "snapshot",
        )

        state.markWorkerEnabled(true)
        state.markWorkerRunning(true)
        state.recordSuccess(result)

        val snapshot = state.snapshot()
        assertEquals(result, snapshot.latestResult)
        assertEquals(0, snapshot.consecutiveFailures)
        assertNotNull(snapshot.lastSuccessAt)
    }

    @Test
    fun `records failure with latest error`() {
        val state = LatestRecognitionState()
        val failure = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = null,
            detectorMode = "STUB",
            source = "snapshot",
            error = RecognitionError(
                code = "FRAME_FETCH_FAILED",
                message = "camera unavailable",
                retriable = true,
            ),
        )

        state.recordFailure(failure)

        val snapshot = state.snapshot()
        assertEquals(1, snapshot.consecutiveFailures)
        assertEquals("FRAME_FETCH_FAILED", snapshot.lastError?.code)
    }
}
