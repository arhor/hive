package io.github.arhor.esphome.client

interface EspHomeClient : AutoCloseable {
    fun connect(): EspHomeConnection
}

