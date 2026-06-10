package io.github.arhor.esphome.client.internal

interface EspHomeTransport : AutoCloseable {
    fun send(frame: EspHomeFrame)
    fun receive(): EspHomeFrame
}
