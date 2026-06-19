package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.async.internal.noise.NoiseConstants;
import io.github.arhor.esphome.client.async.internal.noise.NoiseHandshakeState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseHandshakeStateTest {

    @Test
    void initiatorAndResponderDeriveCompatibleCipherStates() {
        final var psk = new byte[32];
        for (var index = 0; index < 32; index++) {
            psk[index] = (byte) (index + 1);
        }
        final var initiator = NoiseHandshakeState.initiator(psk);
        final var responder = NoiseHandshakeState.responder(psk);

        final var firstMessage = initiator.writeMessage();
        responder.readMessage(firstMessage);
        final var secondMessage = responder.writeMessage();
        initiator.readMessage(secondMessage);

        assertTrue(initiator.isComplete());
        assertTrue(responder.isComplete());

        final var clientPlaintext = "hello from client".getBytes(StandardCharsets.UTF_8);
        final var clientCiphertext = initiator.getSendCipher().encryptWithAd(NoiseConstants.EMPTY, clientPlaintext);
        assertArrayEquals(
            clientPlaintext,
            responder.getReceiveCipher().decryptWithAd(NoiseConstants.EMPTY, clientCiphertext)
        );

        final var serverPlaintext = "hello from server".getBytes(StandardCharsets.UTF_8);
        final var serverCiphertext = responder.getSendCipher().encryptWithAd(NoiseConstants.EMPTY, serverPlaintext);
        assertArrayEquals(
            serverPlaintext,
            initiator.getReceiveCipher().decryptWithAd(NoiseConstants.EMPTY, serverCiphertext)
        );
    }

    @Test
    void handshakeRejectsMismatchedPreSharedKeys() {
        final var initiatorPsk = new byte[32];
        final var responderPsk = new byte[32];
        for (var index = 0; index < 32; index++) {
            initiatorPsk[index] = (byte) (index + 1);
            responderPsk[index] = (byte) (index + 2);
        }
        final var initiator = NoiseHandshakeState.initiator(initiatorPsk);
        final var responder = NoiseHandshakeState.responder(responderPsk);

        final var firstMessage = initiator.writeMessage();

        assertThrows(EspHomeProtocolException.class, () -> responder.readMessage(firstMessage));
    }
}
