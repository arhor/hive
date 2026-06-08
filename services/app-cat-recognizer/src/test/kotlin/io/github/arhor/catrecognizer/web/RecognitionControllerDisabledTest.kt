package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured
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
        RestAssured.given()
            .`when`().post("/recognition/run")
            .then()
            .statusCode(403)
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.debug.manual-trigger-enabled" to "false",
        )
    }
}
