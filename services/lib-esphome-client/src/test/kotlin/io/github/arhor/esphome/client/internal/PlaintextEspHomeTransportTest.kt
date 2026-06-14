package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PlaintextEspHomeTransportTest {

    @Test
    fun `sends encoded frame over socket`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            val accepted = executor.submit<EspHomeFrame> {
                server.accept().use { socket ->
                    socket.soTimeout = 1_000
                    EspHomeFrameCodec.decode(socket.getInputStream())
                }
            }

            PlaintextEspHomeTransport.connect(configFor(server)).use { transport ->
                transport.send(EspHomeFrame(messageType = 45, payload = byteArrayOf(1, 2, 3)))
            }

            val frame = accepted.get()
            assertEquals(45, frame.messageType)
            assertContentEquals(byteArrayOf(1, 2, 3), frame.payload)
            executor.shutdown()
        }
    }

    @Test
    fun `receives decoded frame from socket`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            val sent = executor.submit<Unit> {
                server.accept().use { socket ->
                    socket.getOutputStream().write(
                        EspHomeFrameCodec.encode(EspHomeFrame(messageType = 46, payload = byteArrayOf(4, 5))),
                    )
                    socket.getOutputStream().flush()
                }
            }

            val frame = PlaintextEspHomeTransport.connect(configFor(server)).use { transport ->
                transport.receive()
            }

            assertEquals(46, frame.messageType)
            assertContentEquals(byteArrayOf(4, 5), frame.payload)
            sent.get()
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
