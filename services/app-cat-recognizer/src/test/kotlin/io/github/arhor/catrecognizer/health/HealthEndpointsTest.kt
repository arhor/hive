package io.github.arhor.catrecognizer.health

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(HealthEndpointsTest.HealthEndpointsDisabledProfile::class)
class HealthEndpointsTest {

    class HealthEndpointsDisabledProfile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "cat-recognizer.debug.manual-trigger-enabled" to "true",
        )
    }

    @Test
    fun `live endpoint responds`() {
        given()
            .`when`().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
    }

    @Test
    fun `ready endpoint reports worker and frame source checks during warm up`() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
            .body("checks.name", hasItems("worker-readiness", "frame-source"))
            .body("checks.find { it.name == 'worker-readiness' }.status", `is`("UP"))
            .body("checks.find { it.name == 'worker-readiness' }.data.state", `is`("warming-up"))
            .body("checks.find { it.name == 'frame-source' }.status", `is`("UP"))
            .body("checks.find { it.name == 'frame-source' }.data.consecutiveFailures", `is`(0))
    }
}
