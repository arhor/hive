package io.github.arhor.catrecognizer.frame.model

import java.time.Instant

data class FramePayload(
    val bytes: ByteArray,
    val contentType: String,
    val observedAt: Instant,
)
