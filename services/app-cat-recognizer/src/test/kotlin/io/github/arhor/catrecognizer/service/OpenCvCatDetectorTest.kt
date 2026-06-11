package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@QuarkusTest
class OpenCvCatDetectorTest {

    @Inject
    lateinit var detector: OpenCvCatDetector

    @Test
    fun `returns absent for plain color jpeg with no cat`() {
        val result = detector.detect(frame(solidColorJpeg(), "image/jpeg"))
        assertIs<DetectionOutcome.Absent>(result)
    }

    @Test
    fun `returns absent for plain color png with no cat`() {
        val result = detector.detect(frame(solidColorPng(), "image/png"))
        assertIs<DetectionOutcome.Absent>(result)
    }

    @Test
    fun `rejects invalid image bytes with a safe message`() {
        val error = assertFailsWith<IllegalStateException> {
            detector.detect(frame("not-an-image".encodeToByteArray(), "image/jpeg"))
        }
        assertEquals("OpenCV failed to decode frame", error.message)
    }

    private fun frame(bytes: ByteArray, contentType: String): FramePayload =
        FramePayload(
            bytes = bytes,
            contentType = contentType,
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        )

    private fun solidColorJpeg(): ByteArray = encodedImage("jpg")

    private fun solidColorPng(): ByteArray = encodedImage("png")

    private fun encodedImage(format: String): ByteArray {
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until 64) for (y in 0 until 64) image.setRGB(x, y, Color.GRAY.rgb)
        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, format, output)) { "Unable to encode $format fixture" }
        return output.toByteArray()
    }
}
