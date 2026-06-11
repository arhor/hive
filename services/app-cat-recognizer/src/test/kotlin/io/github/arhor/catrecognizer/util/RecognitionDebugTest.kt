package io.github.arhor.catrecognizer.util

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RecognitionDebugTest {

    @Test
    fun `summarizes frame payload without exposing bytes`() {
        val frame = FramePayload(
            bytes = "uploaded-frame".encodeToByteArray(),
            contentType = "image/jpeg",
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        )

        val summary = frame.toDebugSummary()

        assertEquals(
            "contentType=image/jpeg, bytes=14, sha256=1dc65b1da983, observedAt=2026-06-11T10:00:00Z",
            summary,
        )
    }

    @Test
    fun `summarizes present detection outcome with bounding boxes`() {
        val outcome = DetectionOutcome.Present(
            confidence = null,
            boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
        )

        val summary = outcome.toDebugSummary()

        assertContains(summary, "type=Present")
        assertContains(summary, "confidence=null")
        assertContains(summary, "boxes=1")
        assertContains(summary, "boxDetails=[(x=10, y=20, w=80, h=100)]")
    }
}
