package io.github.arhor.esphome.client

fun interface EspHomeStateHandler {
    fun onState(state: EspHomeState)
}
