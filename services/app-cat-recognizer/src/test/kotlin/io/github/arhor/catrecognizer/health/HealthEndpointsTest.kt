package io.github.arhor.catrecognizer.health

import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(HealthEndpointsDisabledProfile::class)
class HealthEndpointsTest {

    @Test
    fun `live endpoint responds`() {
        given()
            .`when`().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
    }

    @Test
    fun `ready endpoint reports worker and frame source checks when worker is disabled`() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
            .body("checks.name", hasItems("worker-readiness", "frame-source"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("disabled"))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("UP"))
            .body("checks.find { it.name == 'frame-source' }.data.consecutiveFailures", `is`(0))
    }
}

@QuarkusTest
@TestProfile(FrameSourceFailureDisabledProfile::class)
class FrameSourceHealthCheckTest {

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `ready endpoint reports frame source down from cached failures only`() {
        state.markWorkerEnabled(false)
        state.recordFailure(
            RecognitionResult(
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
            ),
        )

        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(503)
            .body("status", `is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("disabled"))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("DOWN"))
            .body("checks.find { it.name == 'frame-source' }.data.errorCode", `is`("FRAME_FETCH_FAILED"))
            .body("checks.find { it.name == 'frame-source' }.data.retriable", `is`(true))
            .body("checks.find { it.name == 'frame-source' }.data.consecutiveFailures", `is`(1))
    }
}

@QuarkusTest
@TestProfile(DetectorFailureDisabledProfile::class)
class FrameSourceHealthCheckIgnoresDetectorFailuresTest {

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `ready endpoint keeps worker warming up while ignoring detector failures in frame source health`() {
        state.markWorkerEnabled(true)
        state.recordFailure(
            RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                confidence = null,
                detectorMode = "stub",
                source = "snapshot",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = "detector crashed",
                    retriable = true,
                ),
            ),
        )

        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("warming-up"))
            .body("checks.find { it.name == 'worker-readiness' }.data.consecutiveFailures", `is`(1))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("UP"))
            .body("checks.find { it.name == 'frame-source' }.data.consecutiveFailures", `is`(1))
    }
}

@QuarkusTest
@TestProfile(WorkerEnabledProfile::class)
class WorkerReadinessHealthCheckTest {

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `ready endpoint reports warming up then stale after a cached success ages out`() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("warming-up"))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("UP"))

        state.recordSuccess(
            RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = Instant.EPOCH,
                confidence = 0.91,
                detectorMode = "stub",
                source = "snapshot",
            ),
        )

        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(503)
            .body("status", `is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("DOWN"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("stale"))
            .body("checks.find { it.name == 'worker-readiness' }.data.consecutiveFailures", `is`(0))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("UP"))
    }
}

class HealthEndpointsDisabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "cat-recognizer.worker.enabled" to "false",
        "cat-recognizer.debug.manual-trigger-enabled" to "true",
    )
}

class FrameSourceFailureDisabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "cat-recognizer.worker.enabled" to "false",
        "cat-recognizer.debug.manual-trigger-enabled" to "true",
    )
}

class DetectorFailureDisabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "cat-recognizer.worker.enabled" to "false",
        "cat-recognizer.debug.manual-trigger-enabled" to "true",
    )
}

class WorkerEnabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "cat-recognizer.worker.enabled" to "true",
        "cat-recognizer.worker.initial-delay" to "3600S",
        "cat-recognizer.worker.poll-interval" to "3600S",
        "cat-recognizer.worker.failure-backoff" to "3600S",
        "cat-recognizer.debug.manual-trigger-enabled" to "true",
    )
}
