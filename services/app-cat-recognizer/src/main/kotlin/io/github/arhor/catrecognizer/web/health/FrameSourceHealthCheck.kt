package io.github.arhor.catrecognizer.config

import io.github.arhor.catrecognizer.service.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness

@Readiness
@ApplicationScoped
class FrameSourceHealthCheck(
    private val state: LatestRecognitionState,
) : HealthCheck {

    override fun call(): HealthCheckResponse {
        val snapshot = state.snapshot()
        val lastError = snapshot.lastError

        return if (lastError == null) {
            HealthCheckResponse.named(NAME)
                .up()
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        } else if (lastError.code == FRAME_FETCH_FAILED) {
            HealthCheckResponse.named(NAME)
                .down()
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .withData("errorCode", lastError.code)
                .withData("retriable", lastError.retriable)
                .build()
        } else {
            HealthCheckResponse.named(NAME)
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .up()
                .build()
        }
    }

    private companion object {
        const val NAME = "frame-source"
        const val FRAME_FETCH_FAILED = "FRAME_FETCH_FAILED"
    }
}
