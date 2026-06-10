package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.internal.EspHomeProtocolClient
import io.github.arhor.esphome.client.internal.EspHomeTransport
import io.github.arhor.esphome.client.internal.PlaintextEspHomeTransport

interface EspHomeClient : AutoCloseable {
    fun connect(): EspHomeConnection
}

interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
}

class DefaultEspHomeClient(
    private val config: EspHomeClientConfig,
    private val transportFactory: (EspHomeClientConfig) -> EspHomeTransport = { PlaintextEspHomeTransport.connect(it) },
) : EspHomeClient {

    private var connection: EspHomeProtocolClient? = null

    override fun connect(): EspHomeConnection {
        val transport = transportFactory(config)
        return try {
            val protocol = EspHomeProtocolClient(config, transport)
            protocol.connect()
            connection = protocol
            protocol
        } catch (exception: Exception) {
            transport.close()
            throw exception
        }
    }

    override fun close() {
        connection?.close()
        connection = null
    }
}
