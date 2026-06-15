package io.github.arhor.esphome.client.internal.transport

import io.github.arhor.esphome.client.internal.EspHomeFrame

interface EspHomeTransport : AutoCloseable {
    fun send(frame: EspHomeFrame)
    fun receive(): EspHomeFrame
}
