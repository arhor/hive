package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Optional

class FrameClientProducerTest {

    private val httpClient = FrameClient {
        FramePayload(byteArrayOf(1), "image/jpeg", Instant.EPOCH)
    }
    private val nativeClient = FrameClient {
        FramePayload(byteArrayOf(2), "image/jpeg", Instant.EPOCH)
    }

    @Test
    fun `selects native client when source is native api`() {
        val producer = FrameClientProducer(config(RecognizerConfig.CameraSource.NATIVE_API), httpClient, nativeClient)

        producer.frameClient() shouldBeSameInstanceAs nativeClient
    }

    @Test
    fun `selects http client when source is http snapshot`() {
        val producer =
            FrameClientProducer(config(RecognizerConfig.CameraSource.HTTP_SNAPSHOT), httpClient, nativeClient)

        producer.frameClient() shouldBeSameInstanceAs httpClient
    }

    private fun config(source: RecognizerConfig.CameraSource): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = error("not used")
            override fun camera() = object : RecognizerConfig.Camera {
                override fun source() = source
                override fun snapshotUrl() = "http://example.test/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
                override fun nativeApi() = object : RecognizerConfig.NativeApi {
                    override fun host() = "esp32-cam.local"
                    override fun port() = 6053
                    override fun connectTimeout() = Duration.ofSeconds(2)
                    override fun readTimeout() = Duration.ofSeconds(5)
                    override fun encryption() = object : RecognizerConfig.Encryption {
                        override fun enabled() = false
                        override fun key(): Optional<String> = Optional.empty()
                    }
                }
            }

            override fun state() = error("not used")
            override fun debug() = error("not used")
            override fun detector() = error("not used")
        }
}
