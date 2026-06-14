package io.github.arhor.esphome.client.config

data class EspHomeEncryptionConfig(
    val enabled: Boolean = false,
    val key: String? = null,
) {
    init {
        require(!enabled || !key.isNullOrBlank()) {
            "key must be configured when encryption is enabled"
        }
    }
}
