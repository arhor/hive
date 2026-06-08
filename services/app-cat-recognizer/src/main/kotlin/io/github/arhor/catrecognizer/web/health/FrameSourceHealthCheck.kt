package io.github.arhor.catrecognizer.web.health

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
        val failures = snapshot.consecutiveFailures.toLong()
        val lastError = snapshot.lastError

        return when {
            lastError == null -> {
                HealthCheckResponse.named(NAME)
                    .up()
                    .withData("consecutiveFailures", failures)
                    .build()
            }

            lastError.code == FRAME_FETCH_FAILED -> {
                HealthCheckResponse.named(NAME)
                    .down()
                    .withData("consecutiveFailures", failures)
                    .withData("errorCode", lastError.code)
                    .withData("retriable", lastError.retriable)
                    .build()
            }

            else -> {
                HealthCheckResponse.named(NAME)
                    .withData("consecutiveFailures", failures)
                    .up()
                    .build()
            }
        }
    }

    private companion object {
        const val NAME = "frame-source"
        const val FRAME_FETCH_FAILED = "FRAME_FETCH_FAILED"
    }
}
