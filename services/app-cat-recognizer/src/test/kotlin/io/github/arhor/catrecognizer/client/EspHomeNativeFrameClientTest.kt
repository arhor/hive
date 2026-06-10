package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.impl.EspHomeNativeFrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.EspHomeClientException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeConnection
import java.time.Duration
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EspHomeNativeFrameClientTest {

    @Test
    fun `maps native camera bytes to frame payload`() {
        val frameClient = EspHomeNativeFrameClient(config(), factory = { FakeConnection(byteArrayOf(1, 2, 3)) })

        val payload = frameClient.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), payload.bytes)
        assertEquals("image/jpeg", payload.contentType)
    }

    @Test
    fun `maps esphome failures to frame source errors`() {
        val frameClient = EspHomeNativeFrameClient(
            config(),
            factory = { throw EspHomeClientException("native failure") },
        )

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
        assertEquals("Failed to fetch ESPHome camera frame from esp32-cam.local:6053", error.message)
    }

    @Test
    fun `passes encryption settings to esphome client config`() {
        val key = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        var capturedConfig: EspHomeClientConfig? = null
        val frameClient = EspHomeNativeFrameClient(
            config(encryptionEnabled = true, encryptionKey = key),
            factory = {
                capturedConfig = it
                FakeConnection(byteArrayOf(1))
            },
        )

        frameClient.fetchFrame()

        val config = capturedConfig ?: error("ESPHome client config was not captured")
        assertTrue(config.encryption.enabled)
        assertEquals(key, config.encryption.key)
    }

    private class FakeConnection(private val image: ByteArray) : EspHomeConnection {
        override fun deviceInfo() = error("not used")
        override fun fetchCameraImage(single: Boolean): ByteArray = image
        override fun close() = Unit
    }

    private fun config(
        encryptionEnabled: Boolean = false,
        encryptionKey: String? = null,
    ): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = error("not used")
            override fun camera() = object : RecognizerConfig.Camera {
                override fun source() = RecognizerConfig.CameraSource.NATIVE_API
                override fun snapshotUrl() = "http://example.test/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
                override fun nativeApi() = object : RecognizerConfig.NativeApi {
                    override fun host() = "esp32-cam.local"
                    override fun port() = 6053
                    override fun connectTimeout() = Duration.ofSeconds(2)
                    override fun readTimeout() = Duration.ofSeconds(5)
                    override fun encryption() = object : RecognizerConfig.Encryption {
                        override fun enabled() = encryptionEnabled
                        override fun key(): Optional<String> = Optional.ofNullable(encryptionKey)
                    }
                }
            }
            override fun state() = error("not used")
            override fun debug() = error("not used")
        }
}
