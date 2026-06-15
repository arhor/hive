package io.github.arhor.esphome.client

interface EspHomeClient {
    fun connect(): EspHomeConnection
}
