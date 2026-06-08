package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.domain.FramePayload

fun interface FrameClient {
    fun fetchFrame(): FramePayload
}
