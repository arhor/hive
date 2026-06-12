package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CatRecognitionServiceTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-08T12:00:00Z"),
    )

    @Test
    fun `maps unknown detector outcome and records failure`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient { sampleFrame },
            detector = detectorStub(DetectionOutcome.Unknown("opencv placeholder detector")),
            state = state,
        )

        val result = service.runRecognition()

        result.status shouldBe CatPresenceStatus.UNKNOWN
        result.confidence shouldBe null
        result.error?.code shouldBe "DETECTOR_UNKNOWN"
        assertEquals(1, state.snapshot().consecutiveFailures)
    }

    @Test
    fun `maps frame source error and records failure`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient {
                throw FrameSourceError("FRAME_FETCH_FAILED", "camera unavailable", true)
            },
            detector = detectorStub(DetectionOutcome.Unknown("ignored")),
            state = state,
        )

        val result = service.runRecognition()

        result.status shouldBe CatPresenceStatus.UNKNOWN
        result.error?.code shouldBe "FRAME_FETCH_FAILED"
        state.snapshot().consecutiveFailures shouldBe 1
    }

    @Test
    fun `maps detector exception to detector failed`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient { sampleFrame },
            detector = object : CatDetector {
                override fun detect(frame: FramePayload): DetectionOutcome =
                    error("detector crashed")
            },
            state = state,
        )

        val result = service.runRecognition()

        result.status shouldBe CatPresenceStatus.UNKNOWN
        result.error?.code shouldBe "DETECTOR_FAILED"
        result.error?.message shouldBe "detector crashed"
        state.snapshot().consecutiveFailures shouldBe 1
    }

    @Test
    fun `maps present detector outcome with bounding boxes and records success`() {
        val state = LatestRecognitionState()
        val boxes = listOf(BoundingBox(x = 5, y = 10, width = 40, height = 50))
        val service = CatRecognitionService(
            frameClient = FrameClient { sampleFrame },
            detector = detectorStub(DetectionOutcome.Present(confidence = 0.87, boundingBoxes = boxes)),
            state = state,
        )

        val result = service.runRecognition()

        result.status shouldBe CatPresenceStatus.DETECTED
        result.confidence shouldBe 0.87
        result.boundingBoxes shouldBe boxes
        assertEquals(0, state.snapshot().consecutiveFailures)
        assertTrue(state.snapshot().frameBytes!!.contentEquals(sampleFrame.bytes))
    }

    @Test
    fun `maps absent detector outcome and records success`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient { sampleFrame },
            detector = detectorStub(DetectionOutcome.Absent(confidence = 0.95)),
            state = state,
        )

        val result = service.runRecognition()

        result.status shouldBe CatPresenceStatus.NOT_DETECTED
        result.confidence shouldBe 0.95
        result.boundingBoxes shouldBe null
        assertEquals(0, state.snapshot().consecutiveFailures)
    }

    private fun detectorStub(outcome: DetectionOutcome): CatDetector =
        object : CatDetector {
            override fun detect(frame: FramePayload): DetectionOutcome = outcome
        }
}
