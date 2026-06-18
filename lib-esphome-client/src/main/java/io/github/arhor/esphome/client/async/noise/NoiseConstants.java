package io.github.arhor.esphome.client.async.noise;

import java.nio.charset.StandardCharsets;

public final class NoiseConstants {

    public static final int NOISE_MAX_MESSAGE_LENGTH = 65_535;
    public static final int NOISE_PSK_LENGTH = 32;
    public static final int AUTH_TAG_LENGTH = 16;
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";
    public static final String DH_ALGORITHM = "X25519";
    public static final String PROTOCOL_NAME = "Noise_NNpsk0_25519_ChaChaPoly_SHA256";

    public static final byte[] EMPTY = new byte[0];
    public static final byte[] ESPHOME_NOISE_PROLOGUE =
        "NoiseAPIInit\u0000\u0000".getBytes(StandardCharsets.US_ASCII);

    private NoiseConstants() {}
}
