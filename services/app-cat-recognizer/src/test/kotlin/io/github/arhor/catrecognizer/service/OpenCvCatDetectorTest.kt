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

@QuarkusTest
class OpenCvCatDetectorTest {

    @Inject
    lateinit var detector: OpenCvCatDetector

    @Test
    fun `decodes jpeg bytes and returns placeholder unknown`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(jpegBytes(), "image/jpeg")),
        )
    }

    @Test
    fun `decodes png bytes and returns placeholder unknown`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(pngBytes(), "image/png")),
        )
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
            observedAt = Instant.parse("2026-06-08T12:00:00Z"),
        )

    private fun jpegBytes(): ByteArray = encodedImage("jpg")

    private fun pngBytes(): ByteArray = encodedImage("png")

    private fun encodedImage(format: String): ByteArray {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)

        image.setRGB(0, 0, Color.BLACK.rgb)
        image.setRGB(1, 0, Color.WHITE.rgb)
        image.setRGB(0, 1, Color.RED.rgb)
        image.setRGB(1, 1, Color.BLUE.rgb)

        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, format, output)) { "Unable to encode $format fixture" }
        return output.toByteArray()
    }
}
