package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.internal.EspHomeFrame
import io.github.arhor.esphome.client.internal.EspHomeMessageType
import io.github.arhor.esphome.client.internal.EspHomeTransport
import io.github.arhor.esphome.client.proto.HelloResponse
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultEspHomeClientTest {

    @Test
    fun `closes transport when handshake fails`() {
        val transport = ClosingTransport(
            EspHomeFrame(
                EspHomeMessageType.HELLO_RESPONSE,
                HelloResponse.newBuilder().setApiVersionMajor(2).build().toByteArray(),
            ),
        )
        val client = DefaultEspHomeClient(
            EspHomeClientConfig(host = "camera"),
            transportFactory = { transport },
        )

        assertFailsWith<EspHomeProtocolException> {
            client.connect()
        }

        assertTrue(transport.closed)
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
