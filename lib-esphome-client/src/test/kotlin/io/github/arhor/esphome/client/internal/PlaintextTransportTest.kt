package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.internal.codec.PlaintextEspHomeFrameCodec
import io.github.arhor.esphome.client.internal.transport.PlaintextTransport
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PlaintextTransportTest {

    @Test
    fun `sends encoded frame over socket`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            val accepted = executor.submit<EspHomeFrame> {
                server.accept().use { socket ->
                    socket.soTimeout = 1_000
                    PlaintextEspHomeFrameCodec.decode(socket.getInputStream())
                }
            }

            PlaintextTransport.connect(configFor(server)).use { transport ->
                transport.send(EspHomeFrame(type = 45, data = byteArrayOf(1, 2, 3)))
            }

            val frame = accepted.get()
            assertEquals(45, frame.type)
            assertContentEquals(byteArrayOf(1, 2, 3), frame.data)
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
                        PlaintextEspHomeFrameCodec.encode(EspHomeFrame(type = 46, data = byteArrayOf(4, 5))),
                    )
                    socket.getOutputStream().flush()
                }
            }

            val frame = PlaintextTransport.connect(configFor(server)).use { transport ->
                transport.receive()
            }

            assertEquals(46, frame.type)
            assertContentEquals(byteArrayOf(4, 5), frame.data)
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
