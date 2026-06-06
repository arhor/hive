package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class StubCatDetector @Inject constructor(
    private val config: RecognizerConfig,
) {

    fun detect(frame: FramePayload): DetectionOutcome =
        when (config.detection().mode()) {
            DetectionMode.ALWAYS_PRESENT -> DetectionOutcome.Present(confidence = 1.0)
            DetectionMode.ALWAYS_ABSENT -> DetectionOutcome.Absent(confidence = 1.0)
            DetectionMode.STUB -> DetectionOutcome.Unknown(reason = "stub detector")
            DetectionMode.OPENCV -> error("StubCatDetector does not support OPENCV mode")
        }
}
