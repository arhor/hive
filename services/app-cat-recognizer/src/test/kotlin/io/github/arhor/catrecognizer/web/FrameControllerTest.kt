package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(FrameControllerTest.Profile::class)
class FrameControllerTest {

    @Inject
    lateinit var state: LatestRecognitionState

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(LatestRecognitionState(), LatestRecognitionState::class.java)
    }

    @Test
    fun `GET frame latest returns 204 when no frame has been stored`() {
        given()
            .get("/frame/latest")
            .then()
            .statusCode(204)
    }

    @Test
    fun `GET frame latest returns jpeg bytes with correct content type`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        state.recordSuccess(
            RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = Instant.parse("2026-06-11T10:00:00Z"),
                confidence = 0.9,
                source = "snapshot",
            ),
            jpeg,
        )

        given()
            .get("/frame/latest")
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .header("Cache-Control", "no-store")
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.camera.snapshot-url" to "http://example.test/snapshot",
        )
    }
}
