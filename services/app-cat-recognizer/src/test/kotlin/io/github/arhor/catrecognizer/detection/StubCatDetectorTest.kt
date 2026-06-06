package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class StubCatDetectorTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
    )

    @Test
    fun `always present mode reports present`() {
        val detector = StubCatDetector(config(DetectionMode.ALWAYS_PRESENT))

        assertEquals(
            DetectionOutcome.Present(confidence = 1.0),
            detector.detect(sampleFrame),
        )
    }

    @Test
    fun `always absent mode reports absent`() {
        val detector = StubCatDetector(config(DetectionMode.ALWAYS_ABSENT))

        assertEquals(
            DetectionOutcome.Absent(confidence = 1.0),
            detector.detect(sampleFrame),
        )
    }

    @Test
    fun `stub mode reports unknown`() {
        val detector = StubCatDetector(config(DetectionMode.STUB))

        assertEquals(
            DetectionOutcome.Unknown(reason = "stub detector"),
            detector.detect(sampleFrame),
        )
    }

    private fun config(mode: DetectionMode): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = java.time.Duration.ofSeconds(5)
                override fun initialDelay() = java.time.Duration.ofSeconds(1)
                override fun failureBackoff() = java.time.Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = java.time.Duration.ofSeconds(2)
                override fun readTimeout() = java.time.Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = mode
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = java.time.Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
