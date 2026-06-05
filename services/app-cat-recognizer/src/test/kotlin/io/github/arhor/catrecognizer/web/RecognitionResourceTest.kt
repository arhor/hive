package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.detection.StubCatDetector
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.SnapshotFrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.closeTo
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(RecognitionResourceTest.Profile::class)
class RecognitionResourceTest {

    @Inject
    lateinit var config: RecognizerConfig

    @BeforeEach
    fun setUp() {
        installRecognitionMocks(config)
    }

    @Test
    fun `GET latest returns worker summary`() {
        given()
            .`when`().get("/api/recognition/latest")
            .then()
            .statusCode(200)
            .body("worker.enabled", `is`(false))
            .body("worker.running", `is`(false))
    }

    @Test
    fun `POST run returns recognition result when manual trigger is enabled`() {
        given()
            .`when`().post("/api/recognition/run")
            .then()
            .statusCode(200)
            .body("status", `is`("DETECTED"))
            .body("confidence", `is`(0.91f))
            .body("detectorMode", `is`("stub"))
    }

    @Test
    fun `GET debug config returns safe runtime summary`() {
        given()
            .`when`().get("/api/debug/config")
            .then()
            .statusCode(200)
            .body("workerEnabled", `is`(false))
            .body("detectionMode", `is`("stub"))
            .body("snapshotConfigured", `is`(true))
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.worker.enabled" to "false",
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
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
        object : StubCatDetector(config) {
            override fun detect(frame: FramePayload): DetectionOutcome =
                DetectionOutcome.Present(confidence = 0.91)
        },
        CatDetector::class.java,
    )
}
