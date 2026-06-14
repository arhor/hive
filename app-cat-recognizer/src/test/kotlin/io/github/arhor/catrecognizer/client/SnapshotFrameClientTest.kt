package io.github.arhor.catrecognizer.client

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.client.impl.SnapshotFrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.time.Duration
import java.util.Optional

class SnapshotFrameClientTest {

    private var server: HttpServer? = null

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun `fetches snapshot bytes and content type`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/snapshot") { exchange ->
                val body = byteArrayOf(1, 2, 3)
                exchange.responseHeaders.add("Content-Type", "image/jpeg")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))
        val frame = frameClient.fetchFrame()

        frame.bytes shouldBe byteArrayOf(1, 2, 3)
        frame.contentType shouldBe "image/jpeg"
    }

    @Test
    fun `maps camera failures to frame source errors`() {
        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:1/snapshot"))

        val error = shouldThrow<FrameSourceError> {
            frameClient.fetchFrame()
        }

        error.code shouldBe "FRAME_FETCH_FAILED"
        error.retriable shouldBe true
    }

    @Test
    fun `maps non-2xx snapshot responses to retriable frame source errors`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/snapshot") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
            start()
        }

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))

        val error = shouldThrow<FrameSourceError> {
            frameClient.fetchFrame()
        }

        error.code shouldBe "FRAME_FETCH_FAILED"
        error.retriable shouldBe true
        error.message shouldContain "HTTP 404"
    }

    @Test
    fun `restores interrupt flag when snapshot fetch is interrupted`() {
        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:1/snapshot"))

        try {
            Thread.currentThread().interrupt()

            val error = shouldThrow<FrameSourceError> {
                frameClient.fetchFrame()
            }

            error.code shouldBe "FRAME_FETCH_FAILED"
            error.retriable shouldBe true
            error.message shouldContain "127.0.0.1:1/snapshot"
            Thread.currentThread().isInterrupted shouldBe true
            error.cause.shouldBeInstanceOf<InterruptedException>()
        } finally {
            Thread.interrupted()
        }
    }

    private fun config(snapshotUrl: String): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun pollInterval() = Duration.ofSeconds(5)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun source() = RecognizerConfig.CameraSource.HTTP_SNAPSHOT
                override fun snapshotUrl() = snapshotUrl
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

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
                override fun uploadEnabled() = false
            }

            override fun detector() = object : RecognizerConfig.Detector {
                override fun modelPath() = "classpath:/models/yolo11n.onnx"
                override fun imageSize() = 640
                override fun confidenceThreshold() = 0.50
                override fun iouThreshold() = 0.45
                override fun className() = "cat"
            }
        }
}
