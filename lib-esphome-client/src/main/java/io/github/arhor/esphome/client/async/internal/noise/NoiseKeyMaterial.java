package io.github.arhor.esphome.client.async.internal.noise;

import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;

import java.util.Base64;

public final class NoiseKeyMaterial {

    private NoiseKeyMaterial() {}

    public static byte[] decodeBase64(final String value) {
        if (value == null || value.isBlank()) {
            throw new EspHomeProtocolException("ESPHome encryption key must be configured");
        }

        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new EspHomeProtocolException("ESPHome encryption key must be valid base64", exception);
        }

        if (decoded.length != NoiseConstants.NOISE_PSK_LENGTH) {
            throw new EspHomeProtocolException(
                "ESPHome encryption key must decode to " + NoiseConstants.NOISE_PSK_LENGTH + " bytes"
            );
        }
        return decoded;
    }
}
