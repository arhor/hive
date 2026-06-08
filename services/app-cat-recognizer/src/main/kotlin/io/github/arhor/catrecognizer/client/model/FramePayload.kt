package io.github.arhor.catrecognizer.client.model

import java.time.Instant

data class FramePayload(
    val bytes: ByteArray,
    val contentType: String?,
    val observedAt: Instant,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramePayload) return false

        return bytes.contentEquals(other.bytes) &&
            contentType == other.contentType &&
            observedAt == other.observedAt
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + observedAt.hashCode()
        return result
    }
}
