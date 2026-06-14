package io.github.arhor.esphome.client.exception

class EspHomeProtocolException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)
