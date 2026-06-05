package io.github.arhor.catrecognizer.frame

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SnapshotFrameSourceTest {

    private var server: HttpServer? = null

    @AfterTest
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

        val frameSource = SnapshotFrameSource(config("http://localhost:${server!!.address.port}/snapshot"))

        val frame = frameSource.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), frame.bytes)
        assertEquals("image/jpeg", frame.contentType)
    }

    @Test
    fun `maps camera failures to frame source errors`() {
        val frameSource = SnapshotFrameSource(config("http://127.0.0.1:1/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameSource.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
    }

    private fun config(snapshotUrl: String): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofSeconds(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = snapshotUrl
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = DetectionMode.STUB
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
