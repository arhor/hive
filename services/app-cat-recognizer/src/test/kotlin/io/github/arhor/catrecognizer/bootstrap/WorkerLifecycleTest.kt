package io.github.arhor.catrecognizer.bootstrap

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerLifecycleTest {

    private val successResult = RecognitionResult(
        status = CatPresenceStatus.DETECTED,
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        confidence = 0.9,
        detectorMode = "stub",
        source = "snapshot",
    )

    private val failureResult = RecognitionResult(
        status = CatPresenceStatus.UNKNOWN,
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        confidence = null,
        detectorMode = "stub",
        source = "snapshot",
        error = RecognitionError(
            code = "FRAME_FETCH_FAILED",
            message = "camera unavailable",
            retriable = true,
        ),
    )

    @Test
    fun `delayFor returns poll interval after success`() {
        val lifecycle = lifecycle(workerEnabled = true)

        assertEquals(Duration.ofSeconds(5), lifecycle.delayFor(successResult))
    }

    @Test
    fun `delayFor returns failure backoff after error`() {
        val lifecycle = lifecycle(workerEnabled = true)

        assertEquals(Duration.ofSeconds(30), lifecycle.delayFor(failureResult))
    }

    @Test
    fun `startup with worker disabled marks worker as disabled and keeps it stopped`() {
        val state = LatestRecognitionState()
        val lifecycle = lifecycle(workerEnabled = false, state = state)

        lifecycle.onStart(startupEvent())

        val snapshot = state.snapshot()
        assertFalse(snapshot.workerEnabled)
        assertFalse(snapshot.workerRunning)

        lifecycle.onShutdown(shutdownEvent())
    }

    @Test
    fun `startup with worker enabled marks worker as enabled`() {
        val state = LatestRecognitionState()
        val lifecycle = lifecycle(workerEnabled = true, state = state)

        lifecycle.onStart(startupEvent())

        val snapshot = state.snapshot()
        assertTrue(snapshot.workerEnabled)
        assertTrue(snapshot.workerRunning)

        lifecycle.onShutdown(shutdownEvent())
        assertFalse(state.snapshot().workerRunning)
    }

    private fun lifecycle(
        workerEnabled: Boolean,
        state: LatestRecognitionState = LatestRecognitionState(),
    ): WorkerLifecycle {
        val config = config(workerEnabled = workerEnabled)
        return WorkerLifecycle(
            config = config,
            orchestrator = RecognitionOrchestrator(
                frameSource = FrameSource {
                    FramePayload(
                        bytes = "frame".encodeToByteArray(),
                        contentType = "image/jpeg",
                        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                    )
                },
                detector = CatDetector { DetectionOutcome.Present(confidence = 1.0) },
                state = state,
                config = config,
            ),
            state = state,
        )
    }

    private fun config(workerEnabled: Boolean): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = workerEnabled
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofDays(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = DetectionMode.STUB
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }

    private fun startupEvent(): StartupEvent =
        StartupEvent::class.java.getDeclaredConstructor().newInstance()

    private fun shutdownEvent(): ShutdownEvent =
        ShutdownEvent::class.java.getDeclaredConstructor().newInstance()
}
