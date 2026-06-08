package io.github.arhor.catrecognizer.config

import io.github.arhor.catrecognizer.service.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.Duration
import java.time.Instant

@Readiness
@ApplicationScoped
class WorkerReadinessCheck(
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) : HealthCheck {

    override fun call(): HealthCheckResponse {
        val snapshot = state.snapshot()
        val lastError = snapshot.lastError
        if (lastError != null && snapshot.consecutiveFailures > 0) {
            return HealthCheckResponse.named(NAME)
                .down()
                .withData("state", "failing")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .withData("errorCode", lastError.code)
                .build()
        }

        val lastSuccessAt = snapshot.lastSuccessAt
        if (lastSuccessAt == null) {
            return HealthCheckResponse.named(NAME)
                .up()
                .withData("state", "warming-up")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        }

        val isFresh = Duration.between(lastSuccessAt, Instant.now()) <= config.state().staleAfter()
        return if (isFresh) {
            HealthCheckResponse.named(NAME)
                .up()
                .withData("state", "fresh")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        } else {
            HealthCheckResponse.named(NAME)
                .down()
                .withData("state", "stale")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        }
    }

    private companion object {
        const val NAME = "worker-readiness"
    }
}
