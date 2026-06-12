package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.DetectionOutcome

interface CatDetector {
    fun detect(frame: FramePayload): DetectionOutcome
}
