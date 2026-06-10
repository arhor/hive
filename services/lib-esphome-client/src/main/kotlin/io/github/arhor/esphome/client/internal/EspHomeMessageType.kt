package io.github.arhor.esphome.client.internal

object EspHomeMessageType {
    const val HELLO_REQUEST = 1
    const val HELLO_RESPONSE = 2
    const val CONNECT_REQUEST = 3
    const val CONNECT_RESPONSE = 4
    const val DISCONNECT_REQUEST = 5
    const val PING_REQUEST = 7
    const val PING_RESPONSE = 8
    const val DEVICE_INFO_REQUEST = 9
    const val DEVICE_INFO_RESPONSE = 10
    const val CAMERA_IMAGE_RESPONSE = 44
    const val CAMERA_IMAGE_REQUEST = 45
}
