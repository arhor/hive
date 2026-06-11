package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.service.CatRecognitionService
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/recognition")
@Produces(MediaType.APPLICATION_JSON)
class RecognitionController @Inject constructor(
    private val recognitionService: CatRecognitionService,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/latest")
    fun latest(): RecognitionLatestResponse {
        val snapshot = state.snapshot()
        val result = snapshot.latestResult
        return RecognitionLatestResponse(
            status = result?.status,
            observedAt = result?.observedAt,
            confidence = result?.confidence,
            source = result?.source,
            error = result?.error,
            worker = WorkerSummary(
                lastSuccessAt = snapshot.lastSuccessAt,
                consecutiveFailures = snapshot.consecutiveFailures,
                lastErrorCode = snapshot.lastError?.code,
            ),
            boundingBoxes = result?.boundingBoxes,
        )
    }

    @POST
    @Path("/run")
    fun run(): Response {
        if (!config.debug().manualTriggerEnabled()) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }

        return Response.ok(recognitionService.runRecognition()).build()
    }
}
