package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload

fun interface CatDetector {
    fun detect(frame: FramePayload): DetectionOutcome
}
