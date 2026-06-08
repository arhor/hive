package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
class CatRecognitionService @Inject constructor(
    private val frameClient: FrameClient,
    private val detector: OpenCvCatDetector,
    private val state: LatestRecognitionState,
) {

    fun runRecognition(): RecognitionResult {
        var frame: FramePayload? = null
        return try {
            frame = frameClient.fetchFrame()
            val outcome = detector.detect(frame)
            val result = when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "snapshot",
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
                state.recordSuccess(result)
            } else {
                state.recordFailure(result)
            }

            result
        } catch (error: FrameSourceError) {
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
            state.recordFailure(result)
            result
        } catch (error: Exception) {
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
            state.recordFailure(result)
            result
        }
    }
}
