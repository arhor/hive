package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeTransportException
import java.net.InetSocketAddress
import java.net.Socket

class PlaintextEspHomeTransport private constructor(
    private val socket: Socket,
) : EspHomeTransport {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun send(frame: EspHomeFrame) {
        try {
            output.write(EspHomeFrameCodec.encode(frame))
            output.flush()
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to write ESPHome frame", exception)
        }
    }

    override fun receive(): EspHomeFrame =
        try {
            EspHomeFrameCodec.decode(input)
        } catch (exception: EspHomeTransportException) {
            throw exception
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to read ESPHome frame", exception)
        }

    override fun close() {
        socket.close()
    }

    companion object {
        @JvmStatic
        fun connect(config: EspHomeClientConfig): PlaintextEspHomeTransport {
            val socket = Socket()
            try {
                socket.soTimeout = config.readTimeout.toMillis().toInt()
                socket.tcpNoDelay = true
                socket.connect(
                    InetSocketAddress(config.host, config.port),
                    config.connectTimeout.toMillis().toInt(),
                )
                return PlaintextEspHomeTransport(socket)
            } catch (exception: Exception) {
                socket.close()
                throw EspHomeTransportException(
                    "Failed to connect to ESPHome device at ${config.host}:${config.port}",
                    exception,
                )
            }
        }
    }
}
