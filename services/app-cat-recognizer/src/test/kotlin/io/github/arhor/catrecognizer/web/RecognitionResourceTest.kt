package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.SnapshotFrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.not
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(RecognitionResourceTest.Profile::class)
open class RecognitionResourceTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Inject
    lateinit var state: LatestRecognitionState

    @BeforeEach
    fun setUp() {
        installRecognitionMocks(config)
        state.recordSuccess(
            RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                confidence = 0.91,
                detectorMode = "stub",
                source = "snapshot",
            ),
        )
    }

    @Test
    fun `GET latest returns flattened recognition payload`() {
        given()
            .`when`().get("/api/recognition/latest")
            .then()
            .statusCode(200)
            .body("$", not(hasKey("result")))
            .body("status", `is`("DETECTED"))
            .body("observedAt", `is`("2026-06-05T12:00:00Z"))
            .body("confidence", `is`(0.91f))
            .body("detectorMode", `is`("stub"))
            .body("source", `is`("snapshot"))
            .body("error", nullValue())
            .body("worker.enabled", `is`(false))
            .body("worker.running", `is`(false))
            .body("worker.lastSuccessAt", `is`("2026-06-05T12:00:00Z"))
            .body("worker.consecutiveFailures", `is`(0))
            .body("worker.lastErrorCode", nullValue())
    }

    @Test
    fun `GET latest returns failure payload with nested error details`() {
        state.recordFailure(
            RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.parse("2026-06-05T12:01:00Z"),
                confidence = null,
                detectorMode = "stub",
                source = "snapshot",
                error = RecognitionError(
                    code = "FRAME_FETCH_FAILED",
                    message = "camera unavailable",
                    retriable = true,
                ),
            ),
        )

        given()
            .`when`().get("/api/recognition/latest")
            .then()
            .statusCode(200)
            .body("status", `is`("UNKNOWN"))
            .body("observedAt", `is`("2026-06-05T12:01:00Z"))
            .body("confidence", nullValue())
            .body("detectorMode", `is`("stub"))
            .body("source", `is`("snapshot"))
            .body("error.code", `is`("FRAME_FETCH_FAILED"))
            .body("error.message", `is`("camera unavailable"))
            .body("error.retriable", `is`(true))
            .body("worker.enabled", `is`(false))
            .body("worker.running", `is`(false))
            .body("worker.lastSuccessAt", `is`("2026-06-05T12:00:00Z"))
            .body("worker.consecutiveFailures", `is`(1))
            .body("worker.lastErrorCode", `is`("FRAME_FETCH_FAILED"))
    }

    @Test
    fun `POST run returns recognition result when manual trigger is enabled`() {
        given()
            .`when`().post("/api/recognition/run")
            .then()
            .statusCode(200)
            .body("status", `is`("DETECTED"))
            .body("observedAt", `is`("2026-06-05T12:00:00Z"))
            .body("confidence", `is`(0.91f))
            .body("detectorMode", `is`("stub"))
            .body("source", `is`("snapshot"))
            .body("error", nullValue())
    }

    @Test
    fun `GET debug config returns safe runtime summary`() {
        given()
            .`when`().get("/api/debug/config")
            .then()
            .statusCode(200)
            .body("workerEnabled", `is`(false))
            .body("pollInterval", `is`("500ms"))
            .body("failureBackoff", `is`("1500ms"))
            .body("detectionMode", `is`("stub"))
            .body("snapshotConfigured", `is`(true))
    }

    @Test
    fun `camera snapshot URL is configurable through Quarkus config`() {
        assertEquals(
            "http://example.test/snapshot",
            config.camera().snapshotUrl(),
        )
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.worker.enabled" to "false",
            "cat-recognizer.worker.poll-interval" to "500ms",
            "cat-recognizer.worker.failure-backoff" to "1500ms",
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
            "cat-recognizer.camera.snapshot-url" to "http://example.test/snapshot",
        )
    }
}

@QuarkusTest
@TestProfile(RecognitionResourceDisabledTest.Profile::class)
class RecognitionResourceDisabledTest {

    @Inject
    lateinit var config: RecognizerConfig

    @BeforeEach
    fun setUp() {
        installRecognitionMocks(config)
    }

    @Test
    fun `POST run returns forbidden when manual trigger is disabled`() {
        given()
            .`when`().post("/api/recognition/run")
            .then()
            .statusCode(403)
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.worker.enabled" to "false",
            "cat-recognizer.debug.manual-trigger-enabled" to "false",
        )
    }
}

private fun installRecognitionMocks(config: RecognizerConfig) {
    QuarkusMock.installMockForType(
        object : SnapshotFrameSource(config) {
            override fun fetchFrame(): FramePayload =
                FramePayload(
                    bytes = "frame".encodeToByteArray(),
                    contentType = "image/jpeg",
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                )
        },
        FrameSource::class.java,
    )
    QuarkusMock.installMockForType(
        CatDetector { DetectionOutcome.Present(confidence = 0.91) },
        CatDetector::class.java,
    )
}
