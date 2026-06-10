package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.EspHomeProtocolException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class NoiseCipherStateTest {

    @Test
    fun `encrypts and decrypts with matching cipher states`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val sender = NoiseCipherState().apply { initializeKey(key) }
        val receiver = NoiseCipherState().apply { initializeKey(key) }
        val associatedData = "ad".encodeToByteArray()
        val plaintext = "hello encrypted esphome".encodeToByteArray()

        val ciphertext = sender.encryptWithAd(associatedData, plaintext)
        val decrypted = receiver.decryptWithAd(associatedData, ciphertext)

        assertFalse(ciphertext.contentEquals(plaintext))
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `rejects tampered ciphertext`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val sender = NoiseCipherState().apply { initializeKey(key) }
        val receiver = NoiseCipherState().apply { initializeKey(key) }
        val ciphertext = sender.encryptWithAd(ByteArray(0), "hello".encodeToByteArray())
        ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 0x01).toByte()

        assertFailsWith<EspHomeProtocolException> {
            receiver.decryptWithAd(ByteArray(0), ciphertext)
        }
    }

    @Test
    fun `uses nonce counter for subsequent messages`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val cipher = NoiseCipherState().apply { initializeKey(key) }

        val first = cipher.encryptWithAd(ByteArray(0), "same".encodeToByteArray())
        val second = cipher.encryptWithAd(ByteArray(0), "same".encodeToByteArray())

        assertFalse(first.contentEquals(second))
    }
}
