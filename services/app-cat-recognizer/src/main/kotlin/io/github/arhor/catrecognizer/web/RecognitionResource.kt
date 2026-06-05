package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator
import io.github.arhor.catrecognizer.recognition.model.InstantIso8601Serializer
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable
import java.time.Instant

@Path("/api/recognition")
@Produces(MediaType.APPLICATION_JSON)
class RecognitionResource @Inject constructor(
    private val orchestrator: RecognitionOrchestrator,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/latest")
    fun latest(): RecognitionLatestResponse {
        val snapshot = state.snapshot()
        return RecognitionLatestResponse(
            result = snapshot.latestResult,
            worker = WorkerSummary(
                enabled = snapshot.workerEnabled,
                running = snapshot.workerRunning,
                lastSuccessAt = snapshot.lastSuccessAt,
                consecutiveFailures = snapshot.consecutiveFailures,
                lastErrorCode = snapshot.lastError?.code,
            ),
        )
    }

    @POST
    @Path("/run")
    fun run(): Response {
        if (!config.debug().manualTriggerEnabled()) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }

        return Response.ok(orchestrator.runRecognition()).build()
    }
}

@Serializable
data class RecognitionLatestResponse(
    val result: RecognitionResult?,
    val worker: WorkerSummary,
)

@Serializable
data class WorkerSummary(
    val enabled: Boolean,
    val running: Boolean,
    @Serializable(with = InstantIso8601Serializer::class)
    val lastSuccessAt: Instant?,
    val consecutiveFailures: Int,
    val lastErrorCode: String?,
)
