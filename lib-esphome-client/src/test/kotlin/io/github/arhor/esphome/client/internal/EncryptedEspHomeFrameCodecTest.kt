package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.noise.NoiseCipherState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncryptedEspHomeFrameCodecTest {

    @Test
    fun `encodes encrypted payload with big-endian length`() {
        val encoded = EncryptedEspHomeFrameCodec.encode(byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(0x01, 0x00, 0x03, 1, 2, 3), encoded)
    }

    @Test
    fun `decodes encrypted payload`() {
        val payload = EncryptedEspHomeFrameCodec.decode(byteArrayOf(0x01, 0x00, 0x02, 4, 5).inputStream())

        assertContentEquals(byteArrayOf(4, 5), payload)
    }

    @Test
    fun `encrypts message type and payload length in fixed header`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val sender = NoiseCipherState().apply { initializeKey(key) }
        val receiver = NoiseCipherState().apply { initializeKey(key) }

        val encoded = EncryptedEspHomeFrameCodec.encodeFrame(
            EspHomeFrame(messageType = 45, payload = byteArrayOf(1, 2, 3)),
            sender,
        )
        val encryptedPayload = EncryptedEspHomeFrameCodec.decode(encoded.inputStream())
        val decryptedPayload = receiver.decryptWithAd(ByteArray(0), encryptedPayload)

        assertContentEquals(byteArrayOf(0x00, 0x2d, 0x00, 0x03, 1, 2, 3), decryptedPayload)
    }

    @Test
    fun `decodes encrypted frame from fixed header`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val sender = NoiseCipherState().apply { initializeKey(key) }
        val receiver = NoiseCipherState().apply { initializeKey(key) }
        val encryptedPayload = sender.encryptWithAd(
            ByteArray(0),
            byteArrayOf(0x00, 0x2e, 0x00, 0x02, 9, 8),
        )

        val frame = EncryptedEspHomeFrameCodec.decodeFrame(
            EncryptedEspHomeFrameCodec.encode(encryptedPayload).inputStream(),
            receiver,
        )

        assertEquals(46, frame.messageType)
        assertContentEquals(byteArrayOf(9, 8), frame.payload)
    }

    @Test
    fun `rejects payloads too large for encrypted data frame`() {
        val key = ByteArray(32) { index -> (index + 1).toByte() }
        val sender = NoiseCipherState().apply { initializeKey(key) }

        val error = assertFailsWith<EspHomeProtocolException> {
            EncryptedEspHomeFrameCodec.encodeFrame(
                EspHomeFrame(messageType = 45, payload = ByteArray(65_516)),
                sender,
            )
        }

        assertEquals("ESPHome encrypted payload is too large: 65516 bytes", error.message)
    }

    @Test
    fun `rejects invalid indicator`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EncryptedEspHomeFrameCodec.decode(byteArrayOf(0x00, 0x00, 0x02, 4, 5).inputStream())
        }

        assertEquals("Invalid ESPHome encrypted frame indicator: 0x00", error.message)
    }

    @Test
    fun `rejects truncated payload`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EncryptedEspHomeFrameCodec.decode(byteArrayOf(0x01, 0x00, 0x02, 4).inputStream())
        }

        assertEquals("ESPHome encrypted frame ended before payload was complete", error.message)
    }
}
