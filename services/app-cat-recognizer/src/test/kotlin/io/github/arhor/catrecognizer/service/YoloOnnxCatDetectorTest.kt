package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO

@QuarkusTest
class YoloOnnxCatDetectorTest {

    @Inject
    lateinit var detector: CatDetector

    @Test
    fun `returns absent for plain color jpeg with no cat`() {
        detector.shouldBeInstanceOf<YoloOnnxCatDetector>()
        val result = detector.detect(frame(solidColorJpeg(), "image/jpeg"))
        result.shouldBeInstanceOf<DetectionOutcome.Absent>()
    }

    @Test
    fun `rejects invalid image bytes with a safe message`() {
        val error = shouldThrow<IllegalStateException> {
            detector.detect(frame("not-an-image".encodeToByteArray(), "image/jpeg"))
        }
        error.message shouldBe "OpenCV failed to decode frame"
    }

    private fun frame(bytes: ByteArray, contentType: String): FramePayload =
        FramePayload(
            bytes = bytes,
            contentType = contentType,
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        )

    private fun solidColorJpeg(): ByteArray {
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until 64) for (y in 0 until 64) image.setRGB(x, y, Color.GRAY.rgb)
        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, "jpg", output)) { "Unable to encode jpg fixture" }
        return output.toByteArray()
    }
}
