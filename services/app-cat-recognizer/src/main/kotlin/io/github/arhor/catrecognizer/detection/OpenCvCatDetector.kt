package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import jakarta.enterprise.context.ApplicationScoped
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

@ApplicationScoped
class OpenCvCatDetector {

    fun detect(frame: FramePayload): DetectionOutcome {
        val input = byteBuffer(frame.bytes)
        val decoded = Imgcodecs.imdecode(input, Imgcodecs.IMREAD_COLOR)

        if (decoded.empty()) {
            input.release()
            decoded.release()
            throw IllegalStateException("OpenCV failed to decode frame")
        }

        val grayscale = Mat()

        try {
            Imgproc.cvtColor(decoded, grayscale, Imgproc.COLOR_BGR2GRAY)

            if (grayscale.empty()) {
                throw IllegalStateException("OpenCV placeholder processing failed")
            }

            grayscale.total()

            return DetectionOutcome.Unknown(reason = "opencv placeholder detector")
        } catch (error: IllegalStateException) {
            throw error
        } catch (_: Exception) {
            throw IllegalStateException("OpenCV placeholder processing failed")
        } finally {
            grayscale.release()
            decoded.release()
            input.release()
        }
    }

    private fun byteBuffer(bytes: ByteArray): MatOfByte =
        MatOfByte().apply {
            fromArray(*bytes)
        }
}
