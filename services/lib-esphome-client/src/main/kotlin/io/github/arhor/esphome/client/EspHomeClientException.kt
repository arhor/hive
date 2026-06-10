package io.github.arhor.esphome.client

open class EspHomeClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class EspHomeTransportException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)

class EspHomeProtocolException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)

class EspHomeAuthenticationException(
    message: String,
) : EspHomeClientException(message)
