package io.github.arhor.esphome.client

data class EspHomeDeviceInfo(
    val name: String,
    val macAddress: String,
    val esphomeVersion: String,
    val model: String,
)
