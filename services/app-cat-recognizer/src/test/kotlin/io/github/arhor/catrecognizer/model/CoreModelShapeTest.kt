package io.github.arhor.catrecognizer.model

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertNotEquals(left, differentBytes)
    }

    @Test
    fun `frame payload changes when content type differs`() {
        val base = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val withContentType = base.copy(contentType = "image/jpeg")

        assertNotEquals(base, withContentType)
    }

    @Test
    fun `frame payload changes when observed at differs`() {
        val base = FramePayload(
            bytes = byteArrayOf(1, 2, 3),
            contentType = null,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        )
        val observedLater = base.copy(observedAt = Instant.parse("2026-06-05T12:00:01Z"))

        assertNotEquals(base, observedLater)
    }

    @Test
    fun `detection outcomes preserve nullable confidence through serialization`() {
        val present = DetectionOutcome.Present(confidence = null)
        val absent = DetectionOutcome.Absent(confidence = null)
        val unknown = DetectionOutcome.Unknown(reason = "stub detector")

        assertEquals(
            present,
            json.decodeFromString(
                DetectionOutcome.serializer(),
                json.encodeToString(DetectionOutcome.serializer(), present),
            ),
        )
        assertEquals(
            absent,
            json.decodeFromString(
                DetectionOutcome.serializer(),
                json.encodeToString(DetectionOutcome.serializer(), absent),
            ),
        )
        assertEquals(
            unknown,
            json.decodeFromString(
                DetectionOutcome.serializer(),
                json.encodeToString(DetectionOutcome.serializer(), unknown),
            ),
        )
    }

    @Test
    fun `bounding box round trips through json`() {
        val box = BoundingBox(x = 10, y = 20, width = 50, height = 60)

        val encoded = json.encodeToString(box)
        val decoded = json.decodeFromString<BoundingBox>(encoded)

        assertEquals(box, decoded)
        assertTrue(encoded.contains("\"x\":10"))
        assertTrue(encoded.contains("\"width\":50"))
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

        assertEquals(result, decoded)
        assertTrue(encoded.contains("\"source\":\"camera\""))
    }
}
