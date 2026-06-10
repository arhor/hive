package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.internal.EncryptedEspHomeTransport
import io.github.arhor.esphome.client.internal.EspHomeProtocolClient
import io.github.arhor.esphome.client.internal.EspHomeTransport
import io.github.arhor.esphome.client.internal.PlaintextEspHomeTransport
import io.github.arhor.esphome.client.internal.noise.NoiseKeyMaterial

typealias EspHomeTransportFactory = (EspHomeClientConfig) -> EspHomeTransport
typealias EncryptedEspHomeTransportFactory = (EspHomeClientConfig, ByteArray) -> EspHomeTransport

class DefaultEspHomeClient(
    private val config: EspHomeClientConfig,
    private val transportFactory: EspHomeTransportFactory = PlaintextEspHomeTransport::connect,
    private val encryptedTransportFactory: EncryptedEspHomeTransportFactory = EncryptedEspHomeTransport::connect,
) : EspHomeClient {

    private var connection: EspHomeProtocolClient? = null

    override fun connect(): EspHomeConnection {
        val transport = if (config.encryption.enabled) {
            encryptedTransportFactory(config, NoiseKeyMaterial.decodeBase64(config.encryption.key))
        } else {
            transportFactory(config)
        }
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
