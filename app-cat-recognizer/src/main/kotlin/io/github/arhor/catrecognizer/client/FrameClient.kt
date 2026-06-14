package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.model.FramePayload

fun interface FrameClient {
    fun fetchFrame(): FramePayload
}
