package io.github.arhor.catrecognizer.service

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.util.debugK
import io.github.arhor.catrecognizer.util.toDebugSummary
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.math.max
import kotlin.math.min

@ApplicationScoped
class YoloOnnxCatDetector(
    private val config: RecognizerConfig,
) : CatDetector {

    private val logger: Logger = Logger.getLogger(YoloOnnxCatDetector::class.java)
    private lateinit var environment: OrtEnvironment
    private lateinit var session: OrtSession
    private lateinit var inputName: String
    private var temporaryModelPath: Path? = null

    @PostConstruct
    fun init() {
        val modelPath = resolveModelPath(config.detector().modelPath())
        require(Files.isRegularFile(modelPath)) { "YOLO ONNX model not found: $modelPath" }

        environment = OrtEnvironment.getEnvironment()
        session = environment.createSession(modelPath.toString(), OrtSession.SessionOptions())
        inputName = session.inputNames.first()

        logger.info("Loaded YOLO ONNX model: $modelPath")
    }

    override fun detect(frame: FramePayload): DetectionOutcome {
        val input = MatOfByte().apply { fromArray(*frame.bytes) }
        val decoded = Imgcodecs.imdecode(input, Imgcodecs.IMREAD_COLOR)

        if (decoded.empty()) {
            input.release()
            decoded.release()
            throw IllegalStateException("OpenCV failed to decode frame")
        }

        try {
            val modelInput = preprocess(decoded, config.detector().imageSize())

            OnnxTensor.createTensor(environment, modelInput.tensor).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { result ->
                    val detections = parseDetections(
                        output = result.get(0).value,
                        originalWidth = decoded.cols(),
                        originalHeight = decoded.rows(),
                        modelWidth = modelInput.width,
                        modelHeight = modelInput.height,
                    )

                    val outcome = if (detections.isEmpty()) {
                        DetectionOutcome.Absent(confidence = null)
                    } else {
                        DetectionOutcome.Present(
                            confidence = detections.maxOf { it.confidence },
                            boundingBoxes = detections.map { it.box },
                        )
                    }

                    logger.debugK { "YOLO ONNX cat detection completed: ${outcome.toDebugSummary()}" }

                    return outcome
                }
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            logger.debugK {
                "YOLO ONNX processing failed after decoding frame: ${frame.toDebugSummary()}. Error: ${error.message}"
            }
            throw IllegalStateException("YOLO ONNX processing failed")
        } finally {
            decoded.release()
            input.release()
        }
    }

    @PreDestroy
    fun close() {
        if (::session.isInitialized) {
            session.close()
        }
        temporaryModelPath?.let {
            try {
                Files.deleteIfExists(it)
            } catch (error: Exception) {
                logger.debugK { "Failed to delete temporary YOLO ONNX model: $it. Error: ${error.message}" }
            }
        }
    }

    private fun resolveModelPath(configuredPath: String): Path {
        if (!configuredPath.startsWith(CLASSPATH_PREFIX)) {
            return Path.of(configuredPath)
        }

        val resourcePath = configuredPath.removePrefix(CLASSPATH_PREFIX).removePrefix("/")
        val temp = Files.createTempFile("yolo11n-", ".onnx")

        YoloOnnxCatDetector::class.java.classLoader
            .getResourceAsStream(resourcePath)
            ?.use { input ->
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING)
            }
            ?: throw IllegalStateException("YOLO ONNX model resource not found: $configuredPath")

        temporaryModelPath = temp
        return temp
    }

    private fun preprocess(image: Mat, imageSize: Int): ModelInput {
        val resized = Mat()
        val rgb = Mat()

        try {
            Imgproc.resize(image, resized, Size(imageSize.toDouble(), imageSize.toDouble()))
            Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB)

            val tensor = Array(1) { Array(3) { Array(imageSize) { FloatArray(imageSize) } } }
            val pixel = ByteArray(3)

            for (y in 0 until imageSize) {
                for (x in 0 until imageSize) {
                    rgb.get(y, x, pixel)
                    tensor[0][0][y][x] = ((pixel[0].toInt() and 0xFF) / 255.0).toFloat()
                    tensor[0][1][y][x] = ((pixel[1].toInt() and 0xFF) / 255.0).toFloat()
                    tensor[0][2][y][x] = ((pixel[2].toInt() and 0xFF) / 255.0).toFloat()
                }
            }

            return ModelInput(width = imageSize, height = imageSize, tensor = tensor)
        } finally {
            rgb.release()
            resized.release()
        }
    }

    private fun parseDetections(
        output: Any,
        originalWidth: Int,
        originalHeight: Int,
        modelWidth: Int,
        modelHeight: Int,
    ): List<YoloDetection> {
        val rows = yoloRows(output)
        val className = config.detector().className()
        val classIndex = cocoClassNames.indexOf(className)
        require(classIndex >= 0) { "Unsupported detector class: $className" }

        val threshold = config.detector().confidenceThreshold()
        val scaleX = originalWidth.toDouble() / modelWidth
        val scaleY = originalHeight.toDouble() / modelHeight

        val detections = rows.mapNotNull { row ->
            if (row.size <= 4 + classIndex) {
                null
            } else {
                val confidence = row[4 + classIndex].toDouble()
                if (confidence < threshold) {
                    null
                } else {
                    detection(row, confidence, scaleX, scaleY, originalWidth, originalHeight)
                }
            }
        }

        return suppressOverlaps(detections, config.detector().iouThreshold())
    }

    private fun yoloRows(output: Any): List<FloatArray> {
        val outputArray = output as? Array<*>
            ?: throw IllegalStateException("Unsupported YOLO output type: ${output::class.java.name}")
        val batch = outputArray.singleOrNull() as? Array<*>
            ?: throw IllegalStateException("Expected single-batch YOLO output")
        val first = batch.firstOrNull() as? FloatArray
            ?: throw IllegalStateException("Expected YOLO output rows")

        return if (batch.size == YOLO_OUTPUT_VALUES && first.size > YOLO_OUTPUT_VALUES) {
            List(first.size) { column ->
                FloatArray(batch.size) { row -> (batch[row] as FloatArray)[column] }
            }
        } else {
            batch.map { row ->
                row as? FloatArray ?: throw IllegalStateException("Expected YOLO output row")
            }
        }
    }

    private fun detection(
        row: FloatArray,
        confidence: Double,
        scaleX: Double,
        scaleY: Double,
        originalWidth: Int,
        originalHeight: Int,
    ): YoloDetection? {
        val centerX = row[0].toDouble()
        val centerY = row[1].toDouble()
        val width = row[2].toDouble()
        val height = row[3].toDouble()
        val x1 = ((centerX - width / 2.0) * scaleX).coerceIn(0.0, originalWidth.toDouble())
        val y1 = ((centerY - height / 2.0) * scaleY).coerceIn(0.0, originalHeight.toDouble())
        val x2 = ((centerX + width / 2.0) * scaleX).coerceIn(0.0, originalWidth.toDouble())
        val y2 = ((centerY + height / 2.0) * scaleY).coerceIn(0.0, originalHeight.toDouble())
        val box = BoundingBox(
            x = x1.toInt(),
            y = y1.toInt(),
            width = max(0, (x2 - x1).toInt()),
            height = max(0, (y2 - y1).toInt()),
        )

        return YoloDetection(confidence = confidence, box = box).takeIf {
            box.width > 0 && box.height > 0
        }
    }

    private fun suppressOverlaps(detections: List<YoloDetection>, iouThreshold: Double): List<YoloDetection> {
        val kept = mutableListOf<YoloDetection>()

        for (candidate in detections.sortedByDescending { it.confidence }) {
            if (kept.none { intersectionOverUnion(candidate.box, it.box) > iouThreshold }) {
                kept += candidate
            }
        }

        return kept
    }

    private fun intersectionOverUnion(a: BoundingBox, b: BoundingBox): Double {
        val ax2 = a.x + a.width
        val ay2 = a.y + a.height
        val bx2 = b.x + b.width
        val by2 = b.y + b.height
        val intersectionWidth = max(0, min(ax2, bx2) - max(a.x, b.x))
        val intersectionHeight = max(0, min(ay2, by2) - max(a.y, b.y))
        val intersectionArea = intersectionWidth * intersectionHeight
        val unionArea = a.width * a.height + b.width * b.height - intersectionArea

        return if (unionArea <= 0) 0.0 else intersectionArea.toDouble() / unionArea
    }

    private data class ModelInput(
        val width: Int,
        val height: Int,
        val tensor: Array<Array<Array<FloatArray>>>,
    )

    private data class YoloDetection(
        val confidence: Double,
        val box: BoundingBox,
    )

    private companion object {
        const val CLASSPATH_PREFIX = "classpath:"
        const val YOLO_OUTPUT_VALUES = 84

        val cocoClassNames = listOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake",
            "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
            "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush",
        )
    }
}
