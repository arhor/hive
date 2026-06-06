package io.github.arhor.catrecognizer.frame

import io.github.arhor.catrecognizer.frame.model.FramePayload

fun interface FrameSource {
    fun fetchFrame(): FramePayload
}
