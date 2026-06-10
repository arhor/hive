package io.github.arhor.esphome.client.internal

data class EspHomeFrame(
    val messageType: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is EspHomeFrame &&
            messageType == other.messageType &&
            payload.contentEquals(other.payload)

    override fun hashCode(): Int =
        31 * messageType + payload.contentHashCode()
}
