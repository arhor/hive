package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.internal.transport.EspHomeTransport

typealias EspHomeTransportFactory = (EspHomeClientConfig) -> EspHomeTransport
typealias EspHomeEncryptedTransportFactory = (EspHomeClientConfig, ByteArray) -> EspHomeTransport
typealias EspHomeConnectionFactory = (EspHomeClientConfig) -> EspHomeConnection
