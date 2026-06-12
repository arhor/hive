package io.github.arhor.catrecognizer.model

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant

class CoreModelShapeTest {

    private val json = Json

    @Test
    fun `frame payload compares byte content`() {
        val left = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val right = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val differentBytes = FramePayload(
            bytes = byteArrayOf(1, 2, 4),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )

        left shouldBe right
        left.hashCode() shouldBe right.hashCode()
        left shouldNotBe differentBytes
    }

    @Test
    fun `frame payload changes when content type differs`() {
        val base = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val withContentType = base.copy(contentType = "image/jpeg")

        base shouldNotBe withContentType
    }

    @Test
    fun `frame payload changes when observed at differs`() {
        val base = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val observedLater = base.copy(observedAt = Instant.parse("2026-06-05T12:00:01Z"))

        base shouldNotBe observedLater
    }

    @Test
    fun `detection outcomes preserve nullable confidence through serialization`() {
        val present = DetectionOutcome.Present(
            confidence = null,
            boundingBoxes = listOf(BoundingBox(x = 5, y = 10, width = 40, height = 50)),
        )
        val absent = DetectionOutcome.Absent(confidence = null)
        val unknown = DetectionOutcome.Unknown(reason = "stub detector")

        json.decodeFromString(
            DetectionOutcome.serializer(),
            json.encodeToString(DetectionOutcome.serializer(), present),
        ) shouldBe present

        json.decodeFromString(
            DetectionOutcome.serializer(),
            json.encodeToString(DetectionOutcome.serializer(), absent),
        ) shouldBe absent

        json.decodeFromString(
            DetectionOutcome.serializer(),
            json.encodeToString(DetectionOutcome.serializer(), unknown),
        ) shouldBe unknown
    }

    @Test
    fun `bounding box round trips through json`() {
        val box = BoundingBox(x = 10, y = 20, width = 50, height = 60)

        val encoded = json.encodeToString(box)
        val decoded = json.decodeFromString<BoundingBox>(encoded)

        decoded shouldBe box
        encoded shouldContain "\"x\":10"
        encoded shouldContain "\"width\":50"
    }

    @Test
    fun `recognition result round trips through json with nullable fields`() {
        val result = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = null,
            source = "camera",
            error = RecognitionError(
                code = "stub",
                message = "stub detector",
                retriable = false,
            ),
        )

        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<RecognitionResult>(encoded)

        decoded shouldBe result
        encoded shouldContain "\"source\":\"camera\""
    }

    @Test
    fun `recognition result with bounding boxes round trips through json`() {
        val boxes = listOf(BoundingBox(x = 5, y = 10, width = 40, height = 50))
        val result = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-11T10:00:00Z"),
            confidence = 0.87,
            source = "snapshot",
            boundingBoxes = boxes,
        )

        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<RecognitionResult>(encoded)

        decoded shouldBe result
        encoded shouldContain "\"boundingBoxes\""
    }
}
