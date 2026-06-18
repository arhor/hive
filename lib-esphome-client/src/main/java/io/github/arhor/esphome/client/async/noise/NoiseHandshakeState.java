package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.EspHomeProtocolException;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NoiseHandshakeState {

    private static final int X25519_KEY_SIZE = 32;

    private final boolean initiator;
    private final byte[] psk;

    private NoiseCipherState sendCipher;
    private NoiseCipherState receiveCipher;
    private boolean complete;

    private final SymmetricState symmetricState = new SymmetricState();
    private PrivateKey ephemeralPrivateKey;
    private PublicKey remotePublicKey;
    private int patternIndex;

    private NoiseHandshakeState(final boolean initiator, final byte[] psk) {
        this.initiator = initiator;
        this.psk = psk.clone();
        if (psk.length != NoiseConstants.NOISE_PSK_LENGTH) {
            throw new EspHomeProtocolException("Noise PSK must be " + NoiseConstants.NOISE_PSK_LENGTH + " bytes");
        }
        symmetricState.mixHash(NoiseConstants.ESPHOME_NOISE_PROLOGUE);
    }

    public static NoiseHandshakeState initiator(final byte[] psk) {
        return new NoiseHandshakeState(true, psk);
    }

    public static NoiseHandshakeState responder(final byte[] psk) {
        return new NoiseHandshakeState(false, psk);
    }

    public NoiseCipherState getSendCipher() {
        return sendCipher;
    }

    public NoiseCipherState getReceiveCipher() {
        return receiveCipher;
    }

    public boolean isComplete() {
        return complete;
    }

    public byte[] writeMessage() {
        return writeMessage(NoiseConstants.EMPTY);
    }

    public byte[] writeMessage(final byte[] payload) {
        ensureInProgress();
        final var output = new ArrayList<byte[]>();
        switch (patternIndex++) {
            case 0 -> {
                symmetricState.mixKeyAndHash(psk);
                final var keyPair = generateKeyPair();
                ephemeralPrivateKey = keyPair.privateKey();
                output.add(keyPair.publicKeyRaw());
                symmetricState.mixHash(keyPair.publicKeyRaw());
                symmetricState.mixKey(keyPair.publicKeyRaw());
            }
            case 1 -> {
                final var keyPair = generateKeyPair();
                ephemeralPrivateKey = keyPair.privateKey();
                output.add(keyPair.publicKeyRaw());
                symmetricState.mixHash(keyPair.publicKeyRaw());
                symmetricState.mixKey(keyPair.publicKeyRaw());
                symmetricState.mixKey(diffieHellman());
            }
            default -> throw new EspHomeProtocolException("Noise handshake has no pattern to write");
        }

        final var encryptedPayload = symmetricState.encryptAndHash(payload);
        if (encryptedPayload.length > 0) {
            output.add(encryptedPayload);
        }
        if (patternIndex >= 2 && !initiator) {
            split();
        }
        return concat(output);
    }

    public byte[] readMessage(final byte[] message) {
        ensureInProgress();
        var offset = 0;
        final byte[] payload;
        switch (patternIndex++) {
            case 0 -> {
                symmetricState.mixKeyAndHash(psk);
                offset += readRemoteEphemeral(message, offset);
                payload = symmetricState.decryptAndHash(Arrays.copyOfRange(message, offset, message.length));
            }
            case 1 -> {
                offset += readRemoteEphemeral(message, offset);
                symmetricState.mixKey(diffieHellman());
                payload = symmetricState.decryptAndHash(Arrays.copyOfRange(message, offset, message.length));
            }
            default -> throw new EspHomeProtocolException("Noise handshake has no pattern to read");
        }

        if (patternIndex >= 2 && initiator) {
            split();
        }
        return payload;
    }

    private int readRemoteEphemeral(final byte[] message, final int offset) {
        if (message.length - offset < X25519_KEY_SIZE) {
            throw new EspHomeProtocolException("Noise handshake message ended before ephemeral key was complete");
        }
        final var rawKey = Arrays.copyOfRange(message, offset, offset + X25519_KEY_SIZE);
        symmetricState.mixHash(rawKey);
        symmetricState.mixKey(rawKey);
        remotePublicKey = importPublicKey(rawKey);
        return X25519_KEY_SIZE;
    }

    private byte[] diffieHellman() {
        if (ephemeralPrivateKey == null) {
            throw new EspHomeProtocolException("Noise handshake is missing local ephemeral key");
        }
        if (remotePublicKey == null) {
            throw new EspHomeProtocolException("Noise handshake is missing remote ephemeral key");
        }

        try {
            final var agreement = KeyAgreement.getInstance(NoiseConstants.DH_ALGORITHM);
            agreement.init(ephemeralPrivateKey);
            agreement.doPhase(remotePublicKey, true);
            return agreement.generateSecret();
        } catch (Exception exception) {
            throw new EspHomeProtocolException("Noise X25519 key agreement failed", exception);
        }
    }

    private void split() {
        final var keys = symmetricState.split();
        final var first = new NoiseCipherState();
        first.initializeKey(keys[0]);
        final var second = new NoiseCipherState();
        second.initializeKey(keys[1]);
        if (initiator) {
            sendCipher = first;
            receiveCipher = second;
        } else {
            receiveCipher = first;
            sendCipher = second;
        }
        complete = true;
    }

    private void ensureInProgress() {
        if (complete || patternIndex >= 2) {
            throw new EspHomeProtocolException("Noise handshake is already complete");
        }
    }

    private static byte[] protocolNameHash() {
        final var protocolName = NoiseConstants.PROTOCOL_NAME.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (protocolName.length <= 32) {
            final var output = new byte[32];
            System.arraycopy(protocolName, 0, output, 0, protocolName.length);
            return output;
        }
        return sha256(protocolName);
    }

    private static byte[] sha256(final byte[] bytes) {
        try {
            return MessageDigest.getInstance(NoiseConstants.HASH_ALGORITHM).digest(bytes);
        } catch (Exception exception) {
            throw new EspHomeProtocolException("Noise SHA-256 operation failed", exception);
        }
    }

    private static byte[] hmac(final byte[] key, final byte[] data) {
        try {
            final var mac = Mac.getInstance(NoiseConstants.HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, NoiseConstants.HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new EspHomeProtocolException("Noise HMAC operation failed", exception);
        }
    }

    private static byte[][] hkdf(final byte[] chainingKey, final byte[] inputKeyMaterial, final int outputs) {
        final var tempKey = hmac(chainingKey, inputKeyMaterial);
        final var result = new byte[outputs][];
        var previous = NoiseConstants.EMPTY;
        for (var index = 1; index <= outputs; index++) {
            final var combined = new byte[previous.length + 1];
            System.arraycopy(previous, 0, combined, 0, previous.length);
            combined[previous.length] = (byte) index;
            previous = hmac(tempKey, combined);
            result[index - 1] = previous;
        }
        return result;
    }

    private static NoiseKeyPair generateKeyPair() {
        try {
            final var generator = KeyPairGenerator.getInstance(NoiseConstants.DH_ALGORITHM);
            final var keyPair = generator.generateKeyPair();
            final var publicKey = (XECPublicKey) keyPair.getPublic();
            return new NoiseKeyPair(
                keyPair.getPrivate(),
                bigIntegerToLittleEndian(publicKey.getU(), X25519_KEY_SIZE)
            );
        } catch (Exception exception) {
            throw new EspHomeProtocolException("Noise X25519 key generation failed", exception);
        }
    }

    private static PublicKey importPublicKey(final byte[] rawKey) {
        try {
            final var spec = new XECPublicKeySpec(
                NamedParameterSpec.X25519,
                littleEndianToBigInteger(rawKey)
            );
            return KeyFactory.getInstance(NoiseConstants.DH_ALGORITHM).generatePublic(spec);
        } catch (Exception exception) {
            throw new EspHomeProtocolException("Noise X25519 public key import failed", exception);
        }
    }

    private static BigInteger littleEndianToBigInteger(final byte[] bytes) {
        final var reversed = new byte[bytes.length];
        for (var index = 0; index < bytes.length; index++) {
            reversed[index] = bytes[bytes.length - 1 - index];
        }
        return new BigInteger(1, reversed);
    }

    private static byte[] bigIntegerToLittleEndian(final BigInteger value, final int size) {
        final var bigEndian = value.toByteArray();
        final var output = new byte[size];
        var outputIndex = 0;
        for (var index = bigEndian.length - 1; index >= 0; index--) {
            if (outputIndex >= size) {
                break;
            }
            output[outputIndex++] = bigEndian[index];
        }
        return output;
    }

    private static byte[] concat(final List<byte[]> parts) {
        var total = 0;
        for (final var part : parts) {
            total += part.length;
        }
        final var output = new byte[total];
        var offset = 0;
        for (final var part : parts) {
            System.arraycopy(part, 0, output, offset, part.length);
            offset += part.length;
        }
        return output;
    }

    private record NoiseKeyPair(PrivateKey privateKey, byte[] publicKeyRaw) {}

    private final class SymmetricState {

        private byte[] chainingKey = protocolNameHash();
        private byte[] handshakeHash = protocolNameHash();
        private final NoiseCipherState cipherState = new NoiseCipherState();

        void mixHash(final byte[] data) {
            handshakeHash = sha256(concat(handshakeHash, data));
        }

        void mixKey(final byte[] inputKeyMaterial) {
            final var output = hkdf(chainingKey, inputKeyMaterial, 2);
            chainingKey = output[0];
            cipherState.initializeKey(output[1]);
        }

        void mixKeyAndHash(final byte[] inputKeyMaterial) {
            final var output = hkdf(chainingKey, inputKeyMaterial, 3);
            chainingKey = output[0];
            mixHash(output[1]);
            cipherState.initializeKey(output[2]);
        }

        byte[] encryptAndHash(final byte[] plaintext) {
            final var ciphertext = cipherState.encryptWithAd(handshakeHash, plaintext);
            mixHash(ciphertext);
            return ciphertext;
        }

        byte[] decryptAndHash(final byte[] ciphertext) {
            final var plaintext = cipherState.decryptWithAd(handshakeHash, ciphertext);
            mixHash(ciphertext);
            return plaintext;
        }

        byte[][] split() {
            return hkdf(chainingKey, NoiseConstants.EMPTY, 2);
        }

        private byte[] concat(final byte[] first, final byte[] second) {
            final var output = new byte[first.length + second.length];
            System.arraycopy(first, 0, output, 0, first.length);
            System.arraycopy(second, 0, output, first.length, second.length);
            return output;
        }
    }
}
