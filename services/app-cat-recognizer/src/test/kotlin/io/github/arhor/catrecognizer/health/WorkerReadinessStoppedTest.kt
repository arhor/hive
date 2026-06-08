package io.github.arhor.catrecognizer.health

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(WorkerReadinessStoppedTest.WorkerReadinessStoppedProfile::class)
class WorkerReadinessStoppedTest {

    class WorkerReadinessStoppedProfile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
        )
    }

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `ready endpoint reports warming up then stale after a cached success ages out`() {
        RestAssured.given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", CoreMatchers.`is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.status", CoreMatchers.`is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", CoreMatchers.`is`("warming-up"))
            .body("checks.find { it.name == 'frame-source' }.status", CoreMatchers.`is`("UP"))

        state.recordSuccess(
            RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = Instant.EPOCH,
                confidence = 0.91,
                source = "snapshot",
            ),
        )

        RestAssured.given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(503)
            .body("status", CoreMatchers.`is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.status", CoreMatchers.`is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", CoreMatchers.`is`("stale"))
            .body("checks.find { it.name == 'worker-readiness' }.data.consecutiveFailures", CoreMatchers.`is`(0))
            .body("checks.find { it.name == 'frame-source' }.status", CoreMatchers.`is`("UP"))
    }
}
