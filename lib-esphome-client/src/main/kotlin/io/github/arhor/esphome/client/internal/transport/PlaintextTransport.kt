package io.github.arhor.esphome.client.internal.transport

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.exception.EspHomeTransportException
import io.github.arhor.esphome.client.internal.EspHomeFrame
import io.github.arhor.esphome.client.internal.codec.PlaintextEspHomeFrameCodec
import java.net.InetSocketAddress
import java.net.Socket

class PlaintextTransport private constructor(
    private val socket: Socket,
) : EspHomeTransport {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun send(frame: EspHomeFrame) {
        try {
            val bytes = PlaintextEspHomeFrameCodec.encode(frame)

            output.write(bytes)
            output.flush()
        } catch (ex: Exception) {
            throw EspHomeTransportException("Failed to write ESPHome frame", ex)
        }
    }

    override fun receive(): EspHomeFrame =
        try {
            PlaintextEspHomeFrameCodec.decode(input)
        } catch (ex: EspHomeTransportException) {
            throw ex
        } catch (ex: Exception) {
            throw EspHomeTransportException("Failed to read ESPHome frame", ex)
        }

    override fun close() {
        socket.close()
    }

    companion object {
        @JvmStatic
        fun connect(config: EspHomeClientConfig): PlaintextTransport =
            Socket().let {
                val host = config.host
                val port = config.port

                try {
                    val sockAddress = InetSocketAddress(host, port)
                    val connTimeout = config.connectTimeoutMillis

                    it.soTimeout = config.readTimeoutMillis
                    it.tcpNoDelay = true
                    it.connect(sockAddress, connTimeout)

                    PlaintextTransport(it)
                } catch (ex: Exception) {
                    it.close()
                    throw EspHomeTransportException("Failed to connect to ESPHome device at $host:$port", ex)
                }
            }
    }
}
