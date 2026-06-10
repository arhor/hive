package io.github.arhor.esphome.client

interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
    fun listEntities(): List<EspHomeEntity>
    fun subscribeStates(handler: EspHomeStateHandler)
}
