package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.util.debugK
import io.github.arhor.catrecognizer.util.toDebugSummary
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
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
    private val logger: Logger = Logger.getLogger(OpenCvCatDetector::class.java)

    @PostConstruct
    fun init() {
        val tmp = Files.createTempFile("haarcascade_frontalcatface", ".xml")
        OpenCvCatDetector::class.java
            .getResourceAsStream("/cascades/haarcascade_frontalcatface.xml")!!
            .use { input -> Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING) }
        classifier = CascadeClassifier(tmp.toString())
        logger.debugK { "Loaded OpenCV cat cascade: empty=${classifier.empty()}" }
        Files.delete(tmp)
    }

    fun detect(frame: FramePayload): DetectionOutcome {
        val input = byteBuffer(frame.bytes)
        val decoded = Imgcodecs.imdecode(input, Imgcodecs.IMREAD_COLOR)

        if (decoded.empty()) {
            logger.debugK { "OpenCV failed to decode frame: ${frame.toDebugSummary()}" }
            input.release()
            decoded.release()
            throw IllegalStateException("OpenCV failed to decode frame")
        }

        logger.debugK {
            "OpenCV decoded frame: width=${decoded.cols()}, height=${decoded.rows()}, channels=${decoded.channels()}, ${frame.toDebugSummary()}"
        }

        val grayscale = Mat()
        val detections = MatOfRect()
        val minSize = Size(30.0, 30.0)

        try {
            Imgproc.cvtColor(decoded, grayscale, Imgproc.COLOR_BGR2GRAY)
            Imgproc.equalizeHist(grayscale, grayscale)

            logger.debugK {
                "Running OpenCV cat cascade: grayscaleWidth=${grayscale.cols()}, " +
                    "grayscaleHeight=${grayscale.rows()}, scaleFactor=1.10, minNeighbors=3, " +
                    "minSize=${minSize.width.toInt()}x${minSize.height.toInt()}"
            }

            classifier.detectMultiScale(
                grayscale,
                detections,
                1.1,
                3,
                0,
                minSize,
                Size(),
            )

            val rects = detections.toArray()
            val outcome = if (rects.isEmpty()) {
                DetectionOutcome.Absent(confidence = null)
            } else {
                DetectionOutcome.Present(
                    confidence = null,
                    boundingBoxes = rects.map { r ->
                        BoundingBox(x = r.x, y = r.y, width = r.width, height = r.height)
                    },
                )
            }

            logger.debugK { "OpenCV cat detection completed: ${outcome.toDebugSummary()}" }

            return outcome
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            logger.debugK(error) { "OpenCV processing failed after decoding frame: ${frame.toDebugSummary()}" }
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
