package io.github.arhor.esphome.client

import java.time.Duration

data class EspHomeClientConfig(
    val host: String,
    val port: Int = 6053,
    val clientName: String = "hive-lib-esphome-client",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
    val password: String? = null,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be between 1 and 65535" }
        require(!connectTimeout.isNegative && !connectTimeout.isZero) { "connectTimeout must be positive" }
        require(!readTimeout.isNegative && !readTimeout.isZero) { "readTimeout must be positive" }
    }
}
