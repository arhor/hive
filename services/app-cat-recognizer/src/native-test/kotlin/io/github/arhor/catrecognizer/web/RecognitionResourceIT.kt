package io.github.arhor.catrecognizer.web

import io.quarkus.test.junit.QuarkusIntegrationTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
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
}
