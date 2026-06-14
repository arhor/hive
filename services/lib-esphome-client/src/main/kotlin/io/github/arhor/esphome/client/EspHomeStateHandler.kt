package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.model.EspHomeState

fun interface EspHomeStateHandler {
    fun onState(state: EspHomeState)
}
