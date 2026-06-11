package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@ApplicationScoped
class OpenCvCatDetector {

    private lateinit var classifier: CascadeClassifier

    @PostConstruct
    fun init() {
        val tmp = Files.createTempFile("haarcascade_frontalcatface", ".xml")
        OpenCvCatDetector::class.java
            .getResourceAsStream("/cascades/haarcascade_frontalcatface.xml")!!
            .use { input -> Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING) }
        classifier = CascadeClassifier(tmp.toString())
        Files.delete(tmp)
    }

    fun detect(frame: FramePayload): DetectionOutcome {
        val input = byteBuffer(frame.bytes)
        val decoded = Imgcodecs.imdecode(input, Imgcodecs.IMREAD_COLOR)

        if (decoded.empty()) {
            input.release()
            decoded.release()
            throw IllegalStateException("OpenCV failed to decode frame")
        }

        val grayscale = Mat()
        val detections = MatOfRect()

        try {
            Imgproc.cvtColor(decoded, grayscale, Imgproc.COLOR_BGR2GRAY)
            Imgproc.equalizeHist(grayscale, grayscale)

            classifier.detectMultiScale(
                grayscale,
                detections,
                1.1,
                3,
                0,
                Size(30.0, 30.0),
                Size(),
            )

            val rects = detections.toArray()

            return if (rects.isEmpty()) {
                DetectionOutcome.Absent(confidence = null)
            } else {
                DetectionOutcome.Present(
                    confidence = null,
                    boundingBoxes = rects.map { r ->
                        BoundingBox(x = r.x, y = r.y, width = r.width, height = r.height)
                    },
                )
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (_: Exception) {
            throw IllegalStateException("OpenCV processing failed")
        } finally {
            detections.release()
            grayscale.release()
            decoded.release()
            input.release()
        }
    }

    private fun byteBuffer(bytes: ByteArray): MatOfByte =
        MatOfByte().apply { fromArray(*bytes) }
}
