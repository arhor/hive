package io.github.arhor.catrecognizer.util

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.time.Instant

class RecognitionDebugTest {

    @Test
    fun `summarizes frame payload without exposing bytes`() {
        val frame = FramePayload(
            bytes = "uploaded-frame".encodeToByteArray(),
            contentType = "image/jpeg",
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        )

        val summary = frame.toDebugSummary()

        summary shouldBe "contentType=image/jpeg, bytes=14, sha256=1dc65b1da983, observedAt=2026-06-11T10:00:00Z"
    }

    @Test
    fun `summarizes present detection outcome with bounding boxes`() {
        val outcome = DetectionOutcome.Present(
            confidence = null,
            boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
        )

        val summary = outcome.toDebugSummary()

        summary shouldContain "type=Present"
        summary shouldContain "confidence=null"
        summary shouldContain "boxes=1"
        summary shouldContain "boxDetails=[(x=10, y=20, w=80, h=100)]"
    }
}
