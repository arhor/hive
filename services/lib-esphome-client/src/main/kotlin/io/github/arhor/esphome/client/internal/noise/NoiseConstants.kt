package io.github.arhor.esphome.client.internal.noise

import java.nio.charset.StandardCharsets

internal object NoiseConstants {
    const val NOISE_MAX_MESSAGE_LENGTH = 65_535
    const val NOISE_PSK_LENGTH = 32
    const val AUTH_TAG_LENGTH = 16
    const val HASH_ALGORITHM = "SHA-256"
    const val HMAC_ALGORITHM = "HmacSHA256"
    const val CIPHER_ALGORITHM = "ChaCha20-Poly1305"
    const val DH_ALGORITHM = "X25519"
    const val PROTOCOL_NAME = "Noise_NNpsk0_25519_ChaChaPoly_SHA256"

    val EMPTY = ByteArray(0)
    val ESPHOME_NOISE_PROLOGUE: ByteArray =
        "NoiseAPIInit\u0000\u0000".toByteArray(StandardCharsets.US_ASCII)
}
