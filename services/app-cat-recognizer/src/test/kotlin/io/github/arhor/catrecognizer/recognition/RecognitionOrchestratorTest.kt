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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class RecognitionOrchestratorTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
    )

    @Test
    fun `maps present detection to detected result and records success`() {
        val state = LatestRecognitionState()
        val orchestrator = orchestrator(
            state = state,
            detectorMode = DetectionMode.ALWAYS_PRESENT,
            detector = CatDetector { DetectionOutcome.Present(confidence = 0.9) },
        )

        val result = orchestrator.runRecognition()
        val snapshot = state.snapshot()

        assertEquals(CatPresenceStatus.DETECTED, result.status)
        assertEquals(0.9, result.confidence)
        assertEquals("always_present", result.detectorMode)
        assertNull(result.error)
        assertEquals(result, snapshot.latestResult)
        assertEquals(result.observedAt, snapshot.lastSuccessAt)
        assertEquals(0, snapshot.consecutiveFailures)
        assertNull(snapshot.lastError)
    }

    @Test
    fun `maps absent detection to not detected result and records success`() {
        val state = LatestRecognitionState()
        val orchestrator = orchestrator(
            state = state,
            detectorMode = DetectionMode.ALWAYS_ABSENT,
            detector = CatDetector { DetectionOutcome.Absent(confidence = 1.0) },
        )

        val result = orchestrator.runRecognition()
        val snapshot = state.snapshot()

        assertEquals(CatPresenceStatus.NOT_DETECTED, result.status)
        assertEquals(1.0, result.confidence)
        assertEquals("always_absent", result.detectorMode)
        assertNull(result.error)
        assertEquals(result, snapshot.latestResult)
        assertEquals(result.observedAt, snapshot.lastSuccessAt)
        assertEquals(0, snapshot.consecutiveFailures)
        assertNull(snapshot.lastError)
    }

    @Test
    fun `maps unknown detection to detector unknown result and records failure`() {
        val state = LatestRecognitionState()
        val orchestrator = orchestrator(
            state = state,
            detectorMode = DetectionMode.STUB,
            detector = CatDetector { DetectionOutcome.Unknown(reason = "stub detector") },
        )

        val result = orchestrator.runRecognition()
        val snapshot = state.snapshot()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertNull(result.confidence)
        assertEquals("stub", result.detectorMode)
        assertEquals("DETECTOR_UNKNOWN", result.error?.code)
        assertEquals("stub detector", result.error?.message)
        assertEquals(false, result.error?.retriable)
        assertEquals(result, snapshot.latestResult)
        assertEquals(1, snapshot.consecutiveFailures)
        assertEquals("DETECTOR_UNKNOWN", snapshot.lastError?.code)
        assertNull(snapshot.lastSuccessAt)
    }

    @Test
    fun `maps frame source failure to unknown result and records failure`() {
        val state = LatestRecognitionState()
        val orchestrator = orchestrator(
            state = state,
            detectorMode = DetectionMode.STUB,
            frameSource = FrameSource {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "camera unavailable",
                    retriable = true,
                )
            },
            detector = CatDetector { DetectionOutcome.Present(confidence = 1.0) },
        )

        val result = orchestrator.runRecognition()
        val snapshot = state.snapshot()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("FRAME_FETCH_FAILED", result.error?.code)
        assertEquals("camera unavailable", result.error?.message)
        assertEquals(true, result.error?.retriable)
        assertEquals("stub", result.detectorMode)
        assertNotEquals(sampleFrame.observedAt, result.observedAt)
        assertEquals(result, snapshot.latestResult)
        assertEquals(1, snapshot.consecutiveFailures)
        assertEquals("FRAME_FETCH_FAILED", snapshot.lastError?.code)
        assertNull(snapshot.lastSuccessAt)
    }

    @Test
    fun `maps unexpected detector failure to unknown result and records failure`() {
        val state = LatestRecognitionState()
        val orchestrator = orchestrator(
            state = state,
            detectorMode = DetectionMode.STUB,
            detector = CatDetector { error("detector crashed") },
        )

        val result = orchestrator.runRecognition()
        val snapshot = state.snapshot()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("DETECTOR_FAILED", result.error?.code)
        assertEquals("detector crashed", result.error?.message)
        assertEquals(true, result.error?.retriable)
        assertEquals("stub", result.detectorMode)
        assertEquals(sampleFrame.observedAt, result.observedAt)
        assertEquals(result, snapshot.latestResult)
        assertEquals(1, snapshot.consecutiveFailures)
        assertEquals("DETECTOR_FAILED", snapshot.lastError?.code)
        assertNull(snapshot.lastSuccessAt)
    }

    private fun orchestrator(
        state: LatestRecognitionState,
        detectorMode: DetectionMode,
        frameSource: FrameSource = FrameSource { sampleFrame },
        detector: CatDetector,
    ): RecognitionOrchestrator = RecognitionOrchestrator(
        frameSource = frameSource,
        detector = detector,
        state = state,
        config = config(detectorMode),
    )

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
