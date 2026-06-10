package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.EspHomeProtocolException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class NoiseCipherState {

    private var key: ByteArray? = null
    private var nonce: Long = 0

    fun initializeKey(key: ByteArray?) {
        this.key = key?.copyOf()
        nonce = 0
    }

    fun hasKey(): Boolean =
        key != null

    fun encryptWithAd(associatedData: ByteArray, plaintext: ByteArray): ByteArray {
        val currentKey = key ?: return plaintext
        if (plaintext.size + NoiseConstants.AUTH_TAG_LENGTH > NoiseConstants.NOISE_MAX_MESSAGE_LENGTH) {
            throw EspHomeProtocolException("Noise plaintext is too large: ${plaintext.size} bytes")
        }

        return runCipher(Cipher.ENCRYPT_MODE, currentKey, associatedData, plaintext)
    }

    fun decryptWithAd(associatedData: ByteArray, ciphertext: ByteArray): ByteArray {
        val currentKey = key ?: return ciphertext
        if (ciphertext.size < NoiseConstants.AUTH_TAG_LENGTH) {
            throw EspHomeProtocolException("Noise ciphertext is too short")
        }

        return runCipher(Cipher.DECRYPT_MODE, currentKey, associatedData, ciphertext)
    }

    private fun runCipher(mode: Int, currentKey: ByteArray, associatedData: ByteArray, input: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance(NoiseConstants.CIPHER_ALGORITHM)
            cipher.init(
                mode,
                SecretKeySpec(currentKey, "ChaCha20"),
                IvParameterSpec(nonceBytes()),
            )
            cipher.updateAAD(associatedData)
            val output = cipher.doFinal(input)
            nonce += 1
            return output
        } catch (exception: GeneralSecurityException) {
            throw EspHomeProtocolException("Noise cipher operation failed", exception)
        }
    }

    private fun nonceBytes(): ByteArray {
        val bytes = ByteArray(12)
        var remaining = nonce
        for (index in 4 until 12) {
            bytes[index] = (remaining and 0xffL).toByte()
            remaining = remaining ushr 8
        }
        return bytes
    }
}
