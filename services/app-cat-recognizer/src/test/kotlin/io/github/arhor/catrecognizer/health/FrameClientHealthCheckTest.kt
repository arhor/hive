package io.github.arhor.catrecognizer.health

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(FrameClientHealthCheckTest.FrameSourceFailureDisabledProfile::class)
class FrameClientHealthCheckTest {

    class FrameSourceFailureDisabledProfile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
        )
    }

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `ready endpoint reports frame source down from cached failures only`() {
        state.recordFailure(
            RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.parse("2026-06-05T12:00:00Z"),
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
            .get("/q/health/ready")
            .then()
            .statusCode(503)
            .body("status", CoreMatchers.`is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.status", CoreMatchers.`is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", CoreMatchers.`is`("failing"))
            .body("checks.find { it.name == 'frame-source' }.status", CoreMatchers.`is`("DOWN"))
            .body("checks.find { it.name == 'frame-source' }.data.errorCode", CoreMatchers.`is`("FRAME_FETCH_FAILED"))
            .body("checks.find { it.name == 'frame-source' }.data.retriable", CoreMatchers.`is`(true))
            .body("checks.find { it.name == 'frame-source' }.data.consecutiveFailures", CoreMatchers.`is`(1))
    }
}
