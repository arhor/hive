package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.impl.SnapshotFrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.github.arhor.catrecognizer.service.OpenCvCatDetector
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant

@QuarkusTest
@TestProfile(RecognitionControllerTest.Profile::class)
class RecognitionControllerTest {

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
                source = "snapshot",
                boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
            ),
        )
    }

    @Test
    fun `GET latest returns flattened recognition payload`() {
        given()
            .get("/recognition/latest")
            .then()
            .statusCode(200)
            .body("$", not(hasKey("result")))
            .body("status", `is`("DETECTED"))
            .body("observedAt", `is`("2026-06-05T12:00:00Z"))
            .body("confidence", `is`(0.91f))
            .body("$", not(hasKey("detectorMode")))
            .body("source", `is`("snapshot"))
            .body("error", nullValue())
            .body("boundingBoxes[0].x", `is`(10))
            .body("boundingBoxes[0].y", `is`(20))
            .body("boundingBoxes[0].width", `is`(80))
            .body("boundingBoxes[0].height", `is`(100))
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
                source = "snapshot",
                error = RecognitionError(
                    code = "FRAME_FETCH_FAILED",
                    message = "camera unavailable",
                    retriable = true,
                ),
            ),
        )

        given()
            .get("/recognition/latest")
            .then()
            .statusCode(200)
            .body("status", `is`("UNKNOWN"))
            .body("observedAt", `is`("2026-06-05T12:01:00Z"))
            .body("confidence", nullValue())
            .body("$", not(hasKey("detectorMode")))
            .body("source", `is`("snapshot"))
            .body("error.code", `is`("FRAME_FETCH_FAILED"))
            .body("error.message", `is`("camera unavailable"))
            .body("error.retriable", `is`(true))
            .body("worker.lastSuccessAt", `is`("2026-06-05T12:00:00Z"))
            .body("worker.consecutiveFailures", `is`(1))
            .body("worker.lastErrorCode", `is`("FRAME_FETCH_FAILED"))
    }

    @Test
    fun `POST run returns recognition result when manual trigger is enabled`() {
        given()
            .post("/recognition/run")
            .then()
            .statusCode(200)
            .body("status", `is`("DETECTED"))
            .body("observedAt", `is`("2026-06-05T12:00:00Z"))
            .body("confidence", `is`(0.91f))
            .body("$", not(hasKey("detectorMode")))
            .body("source", `is`("snapshot"))
            .body("error", nullValue())
    }

    @Test
    fun `POST upload returns recognition result when upload testing is enabled`() {
        val image = Files.createTempFile("cat-recognizer-upload", ".jpg").toFile()
        image.writeBytes("uploaded-frame".encodeToByteArray())

        try {
            given()
                .multiPart("image", image, "image/jpeg")
                .post("/recognition/upload")
                .then()
                .statusCode(200)
                .body("status", `is`("DETECTED"))
                .body("observedAt", not(nullValue()))
                .body("confidence", `is`(0.91f))
                .body("source", `is`("upload"))
                .body("error", nullValue())
                .body("boundingBoxes[0].x", `is`(10))
                .body("boundingBoxes[0].y", `is`(20))
                .body("boundingBoxes[0].width", `is`(80))
                .body("boundingBoxes[0].height", `is`(100))
        } finally {
            image.delete()
        }
    }

    @Test
    fun `POST upload returns unknown result when detector rejects image bytes`() {
        QuarkusMock.installMockForType(
            object : OpenCvCatDetector() {
                override fun detect(frame: FramePayload): DetectionOutcome =
                    throw IllegalStateException("OpenCV failed to decode frame")
            },
            OpenCvCatDetector::class.java,
        )

        given()
            .multiPart("image", "invalid.jpg", "not-an-image".encodeToByteArray(), "image/jpeg")
            .post("/recognition/upload")
            .then()
            .statusCode(200)
            .body("status", `is`("UNKNOWN"))
            .body("source", `is`("upload"))
            .body("error.code", `is`("DETECTOR_FAILED"))
            .body("error.message", `is`("OpenCV failed to decode frame"))
            .body("error.retriable", `is`(false))
    }

    @Test
    fun `GET debug config returns safe runtime summary`() {
        given()
            .get("/debug/config")
            .then()
            .statusCode(200)
            .body("pollInterval", `is`("500ms"))
            .body("snapshotConfigured", `is`(true))
            .body("manualTriggerEnabled", `is`(true))
            .body("uploadEnabled", `is`(true))
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
            "cat-recognizer.worker.poll-interval" to "500ms",
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
            "cat-recognizer.debug.upload-enabled" to "true",
            "cat-recognizer.camera.snapshot-url" to "http://example.test/snapshot",
        )
    }
}

internal fun installRecognitionMocks(config: RecognizerConfig) {
    QuarkusMock.installMockForType(
        object : SnapshotFrameClient(config) {
            override fun fetchFrame(): FramePayload =
                FramePayload(
                    bytes = "frame".encodeToByteArray(),
                    contentType = "image/jpeg",
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                )
        },
        FrameClient::class.java,
    )
    QuarkusMock.installMockForType(
        object : OpenCvCatDetector() {
            override fun detect(frame: FramePayload): DetectionOutcome =
                DetectionOutcome.Present(
                    confidence = 0.91,
                    boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
                )
        },
        OpenCvCatDetector::class.java,
    )
}
