package io.github.arhor.catrecognizer.model

import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CoreModelShapeTest {

    @Test
    fun `frame payload compares byte content and accepts nullable content type`() {
        val left = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val right = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val different = FramePayload(
            bytes = byteArrayOf(1, 2, 4),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertNotEquals(left, different)
    }

    @Test
    fun `detection outcomes accept nullable confidence`() {
        assertEquals(
            DetectionOutcome.Present(confidence = null),
            DetectionOutcome.Present(confidence = null),
        )
        assertEquals(
            DetectionOutcome.Absent(confidence = null),
            DetectionOutcome.Absent(confidence = null),
        )
    }

    @Test
    fun `recognition result uses detector mode string`() {
        val result = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = null,
            detectorMode = DetectionMode.STUB.name,
            source = "camera",
            error = RecognitionError(
                code = "stub",
                message = "stub detector",
                retriable = false,
            ),
        )

        assertEquals(DetectionMode.STUB.name, result.detectorMode)
    }
}
