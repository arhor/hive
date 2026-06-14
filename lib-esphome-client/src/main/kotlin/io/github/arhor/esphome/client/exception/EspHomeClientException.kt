package io.github.arhor.esphome.client.exception

abstract class EspHomeClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
