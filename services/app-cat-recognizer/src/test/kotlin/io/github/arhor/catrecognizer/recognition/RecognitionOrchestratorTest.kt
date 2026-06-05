package io.github.arhor.catrecognizer.recognition

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecognitionOrchestratorTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
    )

    @Test
    fun `maps present detection to detected result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource { sampleFrame },
            detector = CatDetector { DetectionOutcome.Present(confidence = 0.9) },
            state = LatestRecognitionState(),
            config = config(DetectionMode.ALWAYS_PRESENT),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.DETECTED, result.status)
        assertEquals(0.9, result.confidence)
        assertNull(result.error)
    }

    @Test
    fun `maps frame source failure to unknown result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "camera unavailable",
                    retriable = true,
                )
            },
            detector = CatDetector { DetectionOutcome.Present(confidence = 1.0) },
            state = LatestRecognitionState(),
            config = config(DetectionMode.STUB),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("FRAME_FETCH_FAILED", result.error?.code)
    }

    @Test
    fun `maps detector exceptions to unknown result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource { sampleFrame },
            detector = CatDetector { error("detector crashed") },
            state = LatestRecognitionState(),
            config = config(DetectionMode.STUB),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("DETECTOR_FAILED", result.error?.code)
    }

    private fun config(mode: DetectionMode): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofSeconds(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = mode
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
