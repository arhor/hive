package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.EspHomeProtocolException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoiseCipherStateTest {

    @Test
    void encryptsAndDecryptsWithMatchingCipherStates() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var sender = new NoiseCipherState();
        sender.initializeKey(key);
        final var receiver = new NoiseCipherState();
        receiver.initializeKey(key);
        final var associatedData = "ad".getBytes(StandardCharsets.UTF_8);
        final var plaintext = "hello encrypted esphome".getBytes(StandardCharsets.UTF_8);

        final var ciphertext = sender.encryptWithAd(associatedData, plaintext);
        final var decrypted = receiver.decryptWithAd(associatedData, ciphertext);

        assertFalse(java.util.Arrays.equals(ciphertext, plaintext));
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void rejectsTamperedCiphertext() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var sender = new NoiseCipherState();
        sender.initializeKey(key);
        final var receiver = new NoiseCipherState();
        receiver.initializeKey(key);
        final var ciphertext = sender.encryptWithAd(NoiseConstants.EMPTY, "hello".getBytes(StandardCharsets.UTF_8));
        ciphertext[ciphertext.length - 1] = (byte) (ciphertext[ciphertext.length - 1] ^ 0x01);

        assertThrows(EspHomeProtocolException.class, () -> receiver.decryptWithAd(NoiseConstants.EMPTY, ciphertext));
    }

    @Test
    void usesNonceCounterForSubsequentMessages() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var cipher = new NoiseCipherState();
        cipher.initializeKey(key);

        final var first = cipher.encryptWithAd(NoiseConstants.EMPTY, "same".getBytes(StandardCharsets.UTF_8));
        final var second = cipher.encryptWithAd(NoiseConstants.EMPTY, "same".getBytes(StandardCharsets.UTF_8));

        assertFalse(java.util.Arrays.equals(first, second));
    }
}
