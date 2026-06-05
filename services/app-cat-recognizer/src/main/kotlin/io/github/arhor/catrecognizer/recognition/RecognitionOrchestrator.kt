package io.github.arhor.catrecognizer.recognition

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
class RecognitionOrchestrator @Inject constructor(
    private val frameSource: FrameSource,
    private val detector: CatDetector,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) {

    fun runRecognition(): RecognitionResult {
        return try {
            val detectorMode = detectorMode()
            val frame = frameSource.fetchFrame()
            val outcome = detector.detect(frame)
            val result = when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    detectorMode = detectorMode,
                    source = "snapshot",
                )

                is DetectionOutcome.Absent -> RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    detectorMode = detectorMode,
                    source = "snapshot",
                )

                is DetectionOutcome.Unknown -> RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = frame.observedAt,
                    confidence = null,
                    detectorMode = detectorMode,
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
                detectorMode = detectorMode(),
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
                observedAt = Instant.now(),
                confidence = null,
                detectorMode = detectorMode(),
                source = "snapshot",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = error.message ?: "Detector execution failed",
                    retriable = config.detection().unknownOnError(),
                ),
            )
            state.recordFailure(result)
            result
        }
    }

    private fun detectorMode(): String =
        config.detection().mode().name.lowercase()
}
