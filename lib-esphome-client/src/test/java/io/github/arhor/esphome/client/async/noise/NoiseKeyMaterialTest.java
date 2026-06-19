package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseKeyMaterialTest {

    @Test
    void decodesValidBase64Key() {
        final var raw = new byte[32];
        for (var i = 0; i < 32; i++) {
            raw[i] = (byte) (i + 1);
        }
        final var encoded = Base64.getEncoder().encodeToString(raw);

        assertArrayEquals(raw, NoiseKeyMaterial.decodeBase64(encoded));
    }

    @Test
    void rejectsBlankKey() {
        assertThrows(EspHomeProtocolException.class, () -> NoiseKeyMaterial.decodeBase64("  "));
    }

    @Test
    void rejectsWrongLength() {
        final var encoded = Base64.getEncoder().encodeToString(new byte[16]);
        final var ex = assertThrows(EspHomeProtocolException.class, () -> NoiseKeyMaterial.decodeBase64(encoded));
        assertTrue(ex.getMessage().contains("32 bytes"));
    }
}
