package io.github.arhor.esphome.client.exception

class EspHomeTransportException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)
