package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.Instant

class LatestRecognitionStateTest {

    @Test
    fun `initial snapshot only exposes recognition state defaults`() {
        val snapshot = LatestRecognitionState().snapshot()

        assertNull(snapshot.latestResult)
        assertNull(snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }

    @Test
    fun `success resets error state`() {
        val state = LatestRecognitionState()
        val failure = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-08T11:59:00Z"),
            confidence = null,
            source = "snapshot",
            error = RecognitionError("FRAME_FETCH_FAILED", "camera unavailable", true),
        )
        val success = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-08T12:00:00Z"),
            confidence = 0.9,
            source = "snapshot",
        )

        state.recordFailure(failure)
        state.recordSuccess(success)

        val snapshot = state.snapshot()
        assertEquals(success, snapshot.latestResult)
        assertEquals(success.observedAt, snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }

    @Test
    fun `failure increments consecutive failures`() {
        val state = LatestRecognitionState()
        val first = failure("FRAME_FETCH_FAILED", Instant.parse("2026-06-08T11:59:00Z"))
        val second = failure("DETECTOR_FAILED", Instant.parse("2026-06-08T12:00:00Z"))

        state.recordFailure(first)
        state.recordFailure(second)

        val snapshot = state.snapshot()
        assertEquals(second, snapshot.latestResult)
        assertEquals("DETECTOR_FAILED", snapshot.lastError?.code)
        assertEquals(2, snapshot.consecutiveFailures)
    }

    @Test
    fun `frame bytes are stored alongside success result`() {
        val state = LatestRecognitionState()
        val result = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
            confidence = 0.9,
            source = "snapshot",
        )
        val frameBytes = "fake-jpeg".encodeToByteArray()

        state.recordSuccess(result, frameBytes)

        assertTrue(state.snapshot().frameBytes!!.contentEquals(frameBytes))
    }

    @Test
    fun `frame bytes are stored alongside failure result`() {
        val state = LatestRecognitionState()
        val result = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
            source = "snapshot",
            error = RecognitionError("FRAME_FETCH_FAILED", "camera down", true),
        )
        val frameBytes = "fake-jpeg".encodeToByteArray()

        state.recordFailure(result, frameBytes)

        assertTrue(state.snapshot().frameBytes!!.contentEquals(frameBytes))
    }

    @Test
    fun `frame bytes are null when no frame has been stored`() {
        assertNull(LatestRecognitionState().snapshot().frameBytes)
    }

    private fun failure(code: String, observedAt: Instant): RecognitionResult =
        RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = observedAt,
            confidence = null,
            source = "snapshot",
            error = RecognitionError(code, "recognition failed", true),
        )
}
