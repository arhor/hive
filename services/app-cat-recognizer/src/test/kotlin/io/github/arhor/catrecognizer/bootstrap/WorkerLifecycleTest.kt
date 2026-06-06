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
import java.time.Duration
import java.time.Instant
import java.util.concurrent.RejectedExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

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
        val harness = lifecycle(workerEnabled = true)

        assertEquals(Duration.ofSeconds(5), harness.lifecycle.delayFor(successResult))
    }

    @Test
    fun `delayFor returns failure backoff after error`() {
        val harness = lifecycle(workerEnabled = true)

        assertEquals(Duration.ofSeconds(30), harness.lifecycle.delayFor(failureResult))
    }

    @Test
    fun `disabled start keeps worker stopped and skips recognition work`() {
        val harness = lifecycle(workerEnabled = false)

        harness.lifecycle.startWorker()

        val snapshot = harness.state.snapshot()
        assertEquals(false, snapshot.workerEnabled)
        assertEquals(false, snapshot.workerRunning)
        assertEquals(0, harness.submittedTasks.count)
    }

    @Test
    fun `enabled start submits work and stop clears running after termination`() {
        val harness = lifecycle(workerEnabled = true)

        harness.lifecycle.startWorker()

        assertEquals(true, harness.state.snapshot().workerEnabled)
        assertEquals(true, harness.state.snapshot().workerRunning)
        assertEquals(1, harness.submittedTasks.count)

        harness.lifecycle.stopWorker()

        assertFalse(harness.state.snapshot().workerRunning)
    }

    @Test
    fun `start rolls back workerRunning when submission fails`() {
        val harness = lifecycle(workerEnabled = true, failSubmission = true)

        assertFailsWith<RejectedExecutionException> {
            harness.lifecycle.startWorker()
        }

        val snapshot = harness.state.snapshot()
        assertTrue(snapshot.workerEnabled)
        assertFalse(snapshot.workerRunning)
        assertEquals(1, harness.submittedTasks.count)
    }

    @Test
    fun `stop keeps workerRunning true when termination times out`() {
        val harness = lifecycle(workerEnabled = true, terminationResult = false)

        harness.lifecycle.startWorker()
        harness.lifecycle.stopWorker()

        assertTrue(harness.state.snapshot().workerRunning)
    }

    private fun lifecycle(
        workerEnabled: Boolean,
        failSubmission: Boolean = false,
        terminationResult: Boolean = true,
        state: LatestRecognitionState = LatestRecognitionState(),
    ): LifecycleHarness {
        val config = config(workerEnabled = workerEnabled)
        val submittedTasks = TaskCounter()
        val lifecycle = WorkerLifecycle(
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
        ).apply {
            submitWorkerAction = { _ ->
                submittedTasks.count += 1
                if (failSubmission) {
                    throw RejectedExecutionException("executor rejected worker submission")
                }
            }
            awaitWorkerTerminationAction = { _ -> terminationResult }
        }
        return LifecycleHarness(lifecycle = lifecycle, state = state, submittedTasks = submittedTasks)
    }

    private data class LifecycleHarness(
        val lifecycle: WorkerLifecycle,
        val state: LatestRecognitionState,
        val submittedTasks: TaskCounter,
    )

    private class TaskCounter {
        var count: Int = 0
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
}
