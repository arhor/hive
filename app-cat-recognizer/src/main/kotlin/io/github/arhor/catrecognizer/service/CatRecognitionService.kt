package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.util.debugK
import io.github.arhor.catrecognizer.util.toDebugSummary
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class CatRecognitionService @Inject constructor(
    private val frameClient: FrameClient,
    private val detector: CatDetector,
    private val state: LatestRecognitionState,
) {
    companion object {
        private val logger = Logger.getLogger(CatRecognitionService::class.java)
    }

    fun runRecognition(): RecognitionResult {
        var frame: FramePayload? = null
        return try {
            frame = frameClient.fetchFrame()
            logger.debugK { "Fetched snapshot frame: ${frame.toDebugSummary()}" }

            val outcome = detector.detect(frame)
            logger.debugK { "Snapshot detector outcome: ${outcome.toDebugSummary()}" }

            val result = when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "snapshot",
                    boundingBoxes = outcome.boundingBoxes.ifEmpty { null },
                )

                is DetectionOutcome.Absent -> RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "snapshot",
                )

                is DetectionOutcome.Unknown -> RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = frame.observedAt,
                    confidence = null,
                    source = "snapshot",
                    error = RecognitionError(
                        code = "DETECTOR_UNKNOWN",
                        message = outcome.reason,
                        retriable = false,
                    ),
                )
            }

            if (result.error == null) {
                state.recordSuccess(result, frame.bytes)
            } else {
                state.recordFailure(result, frame.bytes)
            }

            result
        } catch (error: FrameSourceError) {
            logger.debugK { "Snapshot frame fetch failed: ${error.message}" }
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.now(),
                confidence = null,
                source = "snapshot",
                error = RecognitionError(
                    code = error.code,
                    message = error.message,
                    retriable = error.retriable,
                ),
            )
            state.recordFailure(result, frame?.bytes)
            result
        } catch (error: Exception) {
            logger.debugK {
                val frameSummary = frame?.toDebugSummary() ?: "none"
                "Snapshot detection failed: frame=$frameSummary. Error: ${error.message}"
            }
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = frame?.observedAt ?: Instant.now(),
                confidence = null,
                source = "snapshot",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = error.message ?: "Detector execution failed",
                    retriable = false,
                ),
            )
            state.recordFailure(result, frame?.bytes)
            result
        }
    }
}
