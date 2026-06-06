package io.github.arhor.catrecognizer.web

import io.quarkus.test.junit.QuarkusIntegrationTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusIntegrationTest
class RecognitionResourceIT {

    @Test
    fun `live endpoint responds in the packaged application`() {
        given()
            .`when`().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
    }

    @Test
    fun `debug config endpoint is reachable without camera access`() {
        given()
            .`when`().get("/api/debug/config")
            .then()
            .statusCode(200)
            .body("pollInterval", `is`("5s"))
            .body("failureBackoff", `is`("30s"))
            .body("detectionMode", `is`("stub"))
            .body("snapshotConfigured", `is`(true))
    }

    @Test
    fun `ready endpoint exposes the packaged health checks`() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(503)))
            .body("checks.name", hasItems("worker-readiness", "frame-source"))
    }

    @Test
    fun `latest endpoint is reachable in the packaged application`() {
        given()
            .`when`().get("/api/recognition/latest")
            .then()
            .statusCode(200)
            .body("worker.enabled", `is`(true))
    }
}
