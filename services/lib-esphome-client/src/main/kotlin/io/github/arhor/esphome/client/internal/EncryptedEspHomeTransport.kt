package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeClientException
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.EspHomeTransportException
import io.github.arhor.esphome.client.internal.noise.NoiseCipherState
import io.github.arhor.esphome.client.internal.noise.NoiseHandshakeState
import java.net.InetSocketAddress
import java.net.Socket

internal class EncryptedEspHomeTransport private constructor(
    private val socket: Socket,
    private val sendCipher: NoiseCipherState,
    private val receiveCipher: NoiseCipherState,
) : EspHomeTransport {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun send(frame: EspHomeFrame) {
        try {
            output.write(EncryptedEspHomeFrameCodec.encodeFrame(frame, sendCipher))
            output.flush()
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to write encrypted ESPHome frame", exception)
        }
    }

    override fun receive(): EspHomeFrame =
        try {
            EncryptedEspHomeFrameCodec.decodeFrame(input, receiveCipher)
        } catch (exception: EspHomeTransportException) {
            throw exception
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to read encrypted ESPHome frame", exception)
        }

    override fun close() {
        socket.close()
    }

    companion object {
        fun connect(config: EspHomeClientConfig, psk: ByteArray): EncryptedEspHomeTransport {
            val socket = Socket()
            try {
                socket.soTimeout = config.readTimeout.toMillis().toInt()
                socket.tcpNoDelay = true
                socket.connect(
                    InetSocketAddress(config.host, config.port),
                    config.connectTimeout.toMillis().toInt(),
                )

                val handshake = NoiseHandshakeState.initiator(psk)
                val output = socket.getOutputStream()
                val input = socket.getInputStream()

                output.write(EncryptedEspHomeFrameCodec.encode(ByteArray(0)))
                output.flush()

                val serverHello = EncryptedEspHomeFrameCodec.decode(input)
                if (serverHello.isEmpty() || serverHello[0] != 0x01.toByte()) {
                    throw EspHomeProtocolException("ESPHome encrypted server hello selected an unsupported protocol")
                }

                output.write(EncryptedEspHomeFrameCodec.encode(byteArrayOf(0x00) + handshake.writeMessage()))
                output.flush()

                val serverHandshake = EncryptedEspHomeFrameCodec.decode(input)
                if (serverHandshake.isEmpty()) {
                    throw EspHomeProtocolException("ESPHome encrypted handshake response was empty")
                }
                if (serverHandshake[0] != 0x00.toByte()) {
                    val reason = serverHandshake.copyOfRange(1, serverHandshake.size).decodeToString()
                    throw EspHomeProtocolException("ESPHome encrypted handshake was rejected: $reason")
                }
                handshake.readMessage(serverHandshake.copyOfRange(1, serverHandshake.size))

                return EncryptedEspHomeTransport(socket, handshake.sendCipher, handshake.receiveCipher)
            } catch (exception: EspHomeClientException) {
                socket.close()
                throw exception
            } catch (exception: Exception) {
                socket.close()
                throw EspHomeTransportException(
                    "Failed to connect to encrypted ESPHome device at ${config.host}:${config.port}",
                    exception,
                )
            }
        }
    }
}
