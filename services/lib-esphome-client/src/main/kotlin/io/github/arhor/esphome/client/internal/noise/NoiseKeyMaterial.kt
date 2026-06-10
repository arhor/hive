package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.EspHomeProtocolException
import java.util.Base64

internal object NoiseKeyMaterial {

    fun decodeBase64(value: String?): ByteArray {
        if (value.isNullOrBlank()) {
            throw EspHomeProtocolException("ESPHome encryption key must be configured")
        }

        val decoded = try {
            Base64.getDecoder().decode(value)
        } catch (exception: IllegalArgumentException) {
            throw EspHomeProtocolException("ESPHome encryption key must be valid base64", exception)
        }

        if (decoded.size != NoiseConstants.NOISE_PSK_LENGTH) {
            throw EspHomeProtocolException(
                "ESPHome encryption key must decode to ${NoiseConstants.NOISE_PSK_LENGTH} bytes",
            )
        }
        return decoded
    }
}
