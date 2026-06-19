package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.EspHomeClientDefault
import io.github.arhor.esphome.client.internal.EspHomeFrame
import io.github.arhor.esphome.client.internal.EspHomeMessageType
import io.github.arhor.esphome.client.internal.transport.EspHomeTransport
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.HelloResponse
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EspHomeClientDefaultTest {

    @Test
    fun `closes transport when handshake fails`() {
        val transport = ClosingTransport(
            EspHomeFrame(
                EspHomeMessageType.HELLO_RESPONSE,
                HelloResponse.newBuilder().setApiVersionMajor(2).build().toByteArray(),
            ),
        )
        val client = EspHomeClientDefault(
            EspHomeClientConfig(host = "camera"),
            transportFactory = { transport },
        )

        assertFailsWith<EspHomeProtocolException> {
            client.connect()
        }

        assertTrue(transport.closed)
    }

    @Test
    fun `uses encrypted transport factory when encryption is enabled`() {
        val key = ByteArray(32) { index -> index.toByte() }
        val transport = ClosingTransport(
            EspHomeFrame(
                EspHomeMessageType.HELLO_RESPONSE,
                HelloResponse.newBuilder().setApiVersionMajor(1).build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.CONNECT_RESPONSE,
                ConnectResponse.newBuilder().setInvalidPassword(false).build().toByteArray(),
            ),
        )
        var encryptedFactoryUsed = false
        val client = EspHomeClientDefault(
            config = EspHomeClientConfig(
                host = "camera",
                encryption = EspHomeClientConfig.EncryptionConfig(
                    enabled = true,
                    key = Base64.getEncoder().encodeToString(key),
                ),
            ),
            transportFactory = { error("plaintext transport should not be used") },
            encryptedTransportFactory = { _, decodedKey ->
                encryptedFactoryUsed = true
                assertContentEquals(key, decodedKey)
                transport
            },
        )

        client.connect()

        assertTrue(encryptedFactoryUsed)
    }

    private class ClosingTransport(vararg frames: EspHomeFrame) : EspHomeTransport {
        private val incoming = ArrayDeque(frames.toList())
        var closed = false

        override fun send(frame: EspHomeFrame) = Unit
        override fun receive(): EspHomeFrame = incoming.removeFirst()
        override fun close() {
            closed = true
        }
    }
}
