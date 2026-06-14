package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.config.EspHomeEncryptionConfig
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EspHomeClientConfigTest {

    @Test
    fun `uses safe defaults for plaintext native api`() {
        val config = EspHomeClientConfig(host = "esp32-cam.local")

        assertEquals(6053, config.port)
        assertEquals("hive-lib-esphome-client", config.clientName)
        assertEquals(Duration.ofSeconds(2), config.connectTimeout)
        assertEquals(Duration.ofSeconds(5), config.readTimeout)
        assertEquals(null, config.password)
        assertEquals(EspHomeEncryptionConfig(), config.encryption)
    }

    @Test
    fun `rejects invalid endpoint and timeout values`() {
        assertFailsWith<IllegalArgumentException> { EspHomeClientConfig(host = " ") }
        assertFailsWith<IllegalArgumentException> { EspHomeClientConfig(host = "camera", port = 0) }
        assertFailsWith<IllegalArgumentException> {
            EspHomeClientConfig(host = "camera", connectTimeout = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            EspHomeClientConfig(host = "camera", readTimeout = Duration.ZERO)
        }
    }

    @Test
    fun `rejects enabled encryption without key`() {
        assertFailsWith<IllegalArgumentException> {
            EspHomeEncryptionConfig(enabled = true, key = null)
        }
        assertFailsWith<IllegalArgumentException> {
            EspHomeEncryptionConfig(enabled = true, key = " ")
        }
    }
}
