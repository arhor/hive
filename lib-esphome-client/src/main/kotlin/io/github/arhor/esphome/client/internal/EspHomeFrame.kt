package io.github.arhor.esphome.client.internal

data class EspHomeFrame(
    val type: Int,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EspHomeFrame

        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + data.contentHashCode()
        return result
    }
}
