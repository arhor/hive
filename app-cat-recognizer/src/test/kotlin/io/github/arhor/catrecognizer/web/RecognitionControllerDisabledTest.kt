package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(RecognitionControllerDisabledTest.Profile::class)
class RecognitionControllerDisabledTest {

    @Inject
    lateinit var config: RecognizerConfig

    @BeforeEach
    fun setUp() {
        installRecognitionMocks(config)
    }

    @Test
    fun `POST run returns forbidden when manual trigger is disabled`() {
        given()
            .post("/recognition/run")
            .then()
            .statusCode(403)
    }

    @Test
    fun `POST upload returns forbidden when upload testing is disabled`() {
        given()
            .multiPart("image", "frame.jpg", "uploaded-frame".encodeToByteArray(), "image/jpeg")
            .post("/recognition/upload")
            .then()
            .statusCode(403)
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.debug.manual-trigger-enabled" to "false",
            "cat-recognizer.debug.upload-enabled" to "false",
        )
    }
}
