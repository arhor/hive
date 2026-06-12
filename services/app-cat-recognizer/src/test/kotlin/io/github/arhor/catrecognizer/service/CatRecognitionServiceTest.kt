package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FrameSourceError
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertNull(result.confidence)
        assertEquals("DETECTOR_UNKNOWN", result.error?.code)
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

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("FRAME_FETCH_FAILED", result.error?.code)
        assertEquals(1, state.snapshot().consecutiveFailures)
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

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("DETECTOR_FAILED", result.error?.code)
        assertEquals("detector crashed", result.error?.message)
        assertEquals(1, state.snapshot().consecutiveFailures)
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

        assertEquals(CatPresenceStatus.DETECTED, result.status)
        assertEquals(0.87, result.confidence)
        assertEquals(boxes, result.boundingBoxes)
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

        assertEquals(CatPresenceStatus.NOT_DETECTED, result.status)
        assertEquals(0.95, result.confidence)
        assertNull(result.boundingBoxes)
        assertEquals(0, state.snapshot().consecutiveFailures)
    }

    private fun detectorStub(outcome: DetectionOutcome): CatDetector =
        object : CatDetector {
            override fun detect(frame: FramePayload): DetectionOutcome = outcome
        }
}
