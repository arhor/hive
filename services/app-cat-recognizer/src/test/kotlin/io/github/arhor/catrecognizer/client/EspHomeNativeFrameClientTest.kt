package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.impl.EspHomeNativeFrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeClientException
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeStateHandler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Optional

class EspHomeNativeFrameClientTest {

    @Test
    fun `maps native camera bytes to frame payload`() {
        val frameClient = EspHomeNativeFrameClient(config(), factory = { FakeConnection(byteArrayOf(1, 2, 3)) })

        val payload = frameClient.fetchFrame()

        payload.bytes shouldBe byteArrayOf(1, 2, 3)
        payload.contentType shouldBe "image/jpeg"
    }

    @Test
    fun `maps esphome failures to frame source errors`() {
        val frameClient = EspHomeNativeFrameClient(
            config(),
            factory = { throw EspHomeClientException("native failure") },
        )

        val error = shouldThrow<FrameSourceError> {
            frameClient.fetchFrame()
        }

        error.code shouldBe "FRAME_FETCH_FAILED"
        error.retriable shouldBe true
        error.message shouldBe "Failed to fetch ESPHome camera frame from esp32-cam.local:6053"
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
        config.encryption.enabled shouldBe true
        config.encryption.key shouldBe key
    }

    private class FakeConnection(private val image: ByteArray) : EspHomeConnection {
        override fun deviceInfo() = error("not used")
        override fun fetchCameraImage(single: Boolean): ByteArray = image
        override fun listEntities(): List<EspHomeEntity> = emptyList()
        override fun subscribeStates(handler: EspHomeStateHandler) = Unit
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
            override fun detector() = object : RecognizerConfig.Detector {
                override fun modelPath() = "classpath:/models/yolo11n.onnx"
                override fun imageSize() = 640
                override fun confidenceThreshold() = 0.50
                override fun iouThreshold() = 0.45
                override fun className() = "cat"
            }
        }
}
