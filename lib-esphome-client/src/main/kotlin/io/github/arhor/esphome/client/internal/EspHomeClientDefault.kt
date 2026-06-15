package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeClient
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.internal.noise.NoiseKeyMaterial
import io.github.arhor.esphome.client.internal.transport.EncryptedTransport
import io.github.arhor.esphome.client.internal.transport.PlaintextTransport

class EspHomeClientDefault(
    private val config: EspHomeClientConfig,
    private val transportFactory: EspHomeTransportFactory = PlaintextTransport::connect,
    private val encryptedTransportFactory: EspHomeEncryptedTransportFactory = EncryptedTransport::connect,
) : EspHomeClient {

    override fun connect(): EspHomeConnection {
        val transport = if (config.encryption.enabled) {
            encryptedTransportFactory(config, NoiseKeyMaterial.decodeBase64(config.encryption.key))
        } else {
            transportFactory(config)
        }
        return try {
            ProtobufConnection(config, transport)
                .apply { initialize() }
        } catch (ex: Exception) {
            transport.close()
            throw ex
        }
    }
}
