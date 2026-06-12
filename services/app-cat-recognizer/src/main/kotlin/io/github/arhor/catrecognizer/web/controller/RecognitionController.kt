package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.CatDetector
import io.github.arhor.catrecognizer.service.CatRecognitionService
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.github.arhor.catrecognizer.util.debugK
import io.github.arhor.catrecognizer.util.toDebugSummary
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files
import java.time.Instant

@Path("/recognition")
@Produces(MediaType.APPLICATION_JSON)
class RecognitionController @Inject constructor(
    private val recognitionService: CatRecognitionService,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
    private val detector: CatDetector,
) {

    private val logger: Logger = Logger.getLogger(RecognitionController::class.java)

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

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(@RestForm("image") image: FileUpload?): Response {
        if (!config.debug().uploadEnabled()) {
            return Response.status(Response.Status.FORBIDDEN).build()
        }

        if (image == null || image.size() <= 0) {
            logger.debugK { "Rejected upload request: image part is missing or empty" }
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        logger.debugK {
            "Received upload image: fileName=${image.fileName()}, contentType=${image.contentType()}, declaredSize=${image.size()}"
        }

        val frame = FramePayload(
            bytes = Files.readAllBytes(image.uploadedFile()),
            contentType = image.contentType(),
            observedAt = Instant.now(),
        )

        logger.debugK { "Prepared upload frame: ${frame.toDebugSummary()}" }

        return Response.ok(detectUpload(frame)).build()
    }

    private fun detectUpload(frame: FramePayload): RecognitionResult {
        try {
            val outcome = detector.detect(frame)
            logger.debugK { "Upload detector outcome: ${outcome.toDebugSummary()}" }

            return when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "upload",
                    boundingBoxes = outcome.boundingBoxes.ifEmpty { null },
                )

                is DetectionOutcome.Absent -> RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "upload",
                )

                is DetectionOutcome.Unknown -> RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = frame.observedAt,
                    confidence = null,
                    source = "upload",
                    error = RecognitionError(
                        code = "DETECTOR_UNKNOWN",
                        message = outcome.reason,
                        retriable = false,
                    ),
                )
            }
        } catch (error: Exception) {
            logger.debugK(error) { "Upload detection failed: frame=${frame.toDebugSummary()}" }
            return RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = frame.observedAt,
                confidence = null,
                source = "upload",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = error.message ?: "Detector execution failed",
                    retriable = false,
                ),
            )
        }
    }
}
