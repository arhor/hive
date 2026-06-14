package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.model.EspHomeDeviceInfo
import io.github.arhor.esphome.client.model.EspHomeEntity

interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
    fun listEntities(): List<EspHomeEntity>
    fun subscribeStates(handler: EspHomeStateHandler)
}
