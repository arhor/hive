package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.internal.codec.EncryptedEspHomeFrameCodec
import io.github.arhor.esphome.client.internal.noise.NoiseHandshakeState
import io.github.arhor.esphome.client.internal.transport.EncryptedTransport
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class EncryptedTransportTest {

    @Test
    fun `sends and receives frames over encrypted socket`() {
        val psk = ByteArray(32) { index -> (index + 1).toByte() }

        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            val serverResult = executor.submit<EspHomeFrame> {
                server.accept().use { socket ->
                    socket.soTimeout = 1_000
                    val responder = NoiseHandshakeState.responder(psk)
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()

                    assertContentEquals(ByteArray(0), EncryptedEspHomeFrameCodec.decode(input))
                    output.write(EncryptedEspHomeFrameCodec.encode(byteArrayOf(0x01, 'c'.code.toByte(), 0x00)))
                    output.flush()

                    val clientHandshake = EncryptedEspHomeFrameCodec.decode(input)
                    assertEquals(0x00, clientHandshake.first().toInt())
                    responder.readMessage(clientHandshake.copyOfRange(1, clientHandshake.size))
                    output.write(EncryptedEspHomeFrameCodec.encode(byteArrayOf(0x00) + responder.writeMessage()))
                    output.flush()

                    val received = EncryptedEspHomeFrameCodec.decodeFrame(input, responder.receiveCipher)

                    output.write(
                        EncryptedEspHomeFrameCodec.encodeFrame(
                            EspHomeFrame(type = 46, data = byteArrayOf(9, 8)),
                            responder.sendCipher,
                        ),
                    )
                    output.flush()
                    received
                }
            }

            EncryptedTransport.connect(configFor(server), psk).use { transport ->
                transport.send(EspHomeFrame(type = 45, data = byteArrayOf(1, 2, 3)))

                val response = transport.receive()

                assertEquals(46, response.type)
                assertContentEquals(byteArrayOf(9, 8), response.data)
            }

            val received = serverResult.get()
            assertEquals(45, received.type)
            assertContentEquals(byteArrayOf(1, 2, 3), received.data)
            executor.shutdown()
        }
    }

    private fun configFor(server: ServerSocket): EspHomeClientConfig =
        EspHomeClientConfig(
            host = InetAddress.getLoopbackAddress().hostAddress,
            port = server.localPort,
            connectTimeout = Duration.ofSeconds(1),
            readTimeout = Duration.ofSeconds(1),
        )
}
