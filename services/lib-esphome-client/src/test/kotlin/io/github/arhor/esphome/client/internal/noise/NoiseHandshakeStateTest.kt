package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.EspHomeProtocolException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NoiseHandshakeStateTest {

    @Test
    fun `initiator and responder derive compatible cipher states`() {
        val psk = ByteArray(32) { index -> (index + 1).toByte() }
        val initiator = NoiseHandshakeState.initiator(psk)
        val responder = NoiseHandshakeState.responder(psk)

        val firstMessage = initiator.writeMessage()
        responder.readMessage(firstMessage)
        val secondMessage = responder.writeMessage()
        initiator.readMessage(secondMessage)

        assertTrue(initiator.isComplete)
        assertTrue(responder.isComplete)

        val clientPlaintext = "hello from client".encodeToByteArray()
        val clientCiphertext = initiator.sendCipher.encryptWithAd(ByteArray(0), clientPlaintext)
        assertContentEquals(clientPlaintext, responder.receiveCipher.decryptWithAd(ByteArray(0), clientCiphertext))

        val serverPlaintext = "hello from server".encodeToByteArray()
        val serverCiphertext = responder.sendCipher.encryptWithAd(ByteArray(0), serverPlaintext)
        assertContentEquals(serverPlaintext, initiator.receiveCipher.decryptWithAd(ByteArray(0), serverCiphertext))
    }

    @Test
    fun `handshake rejects mismatched pre shared keys`() {
        val initiator = NoiseHandshakeState.initiator(ByteArray(32) { index -> (index + 1).toByte() })
        val responder = NoiseHandshakeState.responder(ByteArray(32) { index -> (index + 2).toByte() })

        val firstMessage = initiator.writeMessage()

        assertFailsWith<EspHomeProtocolException> {
            responder.readMessage(firstMessage)
        }
    }
}
