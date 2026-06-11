# Cat Detection UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an HTML page to the cat-recognizer service that displays the latest processed frame and draws a red
rectangle around each detected cat.

**Architecture:** The backend gains a `/frame/latest` endpoint that serves the most recently fetched camera frame as raw
bytes and the existing `/recognition/latest` endpoint is extended with bounding-box coordinates. The HTML page (served
as a Quarkus static resource) polls both endpoints, renders the frame on a `<canvas>`, and overlays red rectangles using
JavaScript. `OpenCvCatDetector` is upgraded from a placeholder to a real Haar-cascade detector that returns bounding
boxes.

**Tech Stack:** Quarkus 3.x, Kotlin, OpenCV (quarkus-opencv), kotlinx.serialization, plain HTML5/Canvas/JS (no
framework)

---

## File Structure

**New files:**

- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/BoundingBox.kt` — serializable
  bounding-box value type
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/FrameController.kt` — serves
  latest raw frame bytes at `GET /frame/latest`
- `services/app-cat-recognizer/src/main/resources/cascades/haarcascade_frontalcatface.xml` — OpenCV cat-face Haar
  cascade
- `services/app-cat-recognizer/src/main/resources/META-INF/resources/index.html` — the live viewer page
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/FrameControllerTest.kt`

**Modified files:**

- `domain/DetectionOutcome.kt` — `Present` gets `boundingBoxes: List<BoundingBox>`
- `domain/RecognitionResult.kt` — add `boundingBoxes: List<BoundingBox>?`
- `web/RecognitionLatestResponse.kt` — add `boundingBoxes: List<BoundingBox>?`
- `web/WorkerSummary.kt` — no change (context reference only)
- `service/LatestRecognitionState.kt` — `Snapshot` gains `frameBytes: ByteArray?`
- `service/CatRecognitionService.kt` — propagate bounding boxes; store frame in state
- `service/OpenCvCatDetector.kt` — load Haar cascade; return real bounding boxes
- `model/CoreModelShapeTest.kt` — update for `boundingBoxes` field
- `service/LatestRecognitionStateTest.kt` — add frame-bytes storage assertions
- `service/CatRecognitionServiceTest.kt` — update `DetectionOutcome.Present` calls
- `service/OpenCvCatDetectorTest.kt` — remove placeholder assertions, add bbox assertions
- `web/RecognitionControllerTest.kt` — update mocks to include `boundingBoxes`

---

## Task 1: Add `BoundingBox` domain class

**Files:**

- Create: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/BoundingBox.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/model/CoreModelShapeTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `CoreModelShapeTest`:

```kotlin
@Test
fun `bounding box round trips through json`() {
    val box = BoundingBox(x = 10, y = 20, width = 50, height = 60)

    val encoded = json.encodeToString(box)
    val decoded = json.decodeFromString<BoundingBox>(encoded)

    assertEquals(box, decoded)
    assertTrue(encoded.contains("\"x\":10"))
    assertTrue(encoded.contains("\"width\":50"))
}
```

Add import `import io.github.arhor.catrecognizer.domain.BoundingBox`.

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.model.CoreModelShapeTest.bounding box round trips through json" 2>&1 | tail -20
```

Expected: FAIL — `Unresolved reference: BoundingBox`

- [ ] **Step 3: Create `BoundingBox.kt`**

```kotlin
package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.model.CoreModelShapeTest.bounding box round trips through json" 2>&1 | tail -10
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/BoundingBox.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/model/CoreModelShapeTest.kt
git commit -m "feat: add BoundingBox domain type"
```

---

## Task 2: Add bounding boxes to `DetectionOutcome.Present` and `RecognitionResult`

**Files:**

- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/DetectionOutcome.kt`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionResult.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/model/CoreModelShapeTest.kt`

- [ ] **Step 1: Write the failing test**

Update the `detection outcomes preserve nullable confidence through serialization` test in `CoreModelShapeTest` so
`DetectionOutcome.Present` includes `boundingBoxes`:

```kotlin
@Test
fun `detection outcomes preserve nullable confidence through serialization`() {
    val present = DetectionOutcome.Present(
        confidence = null,
        boundingBoxes = listOf(BoundingBox(x = 5, y = 10, width = 40, height = 50)),
    )
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
```

Also add a new test for `RecognitionResult` round-trip with bounding boxes:

```kotlin
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

    assertEquals(result, decoded)
    assertTrue(encoded.contains("\"boundingBoxes\""))
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.model.CoreModelShapeTest" 2>&1 | tail -20
```

Expected: FAIL — compilation errors about missing `boundingBoxes` parameter

- [ ] **Step 3: Update `DetectionOutcome.kt`**

```kotlin
package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface DetectionOutcome {

    @Serializable
    data class Present(
        val confidence: Double?,
        val boundingBoxes: List<BoundingBox> = emptyList(),
    ) : DetectionOutcome

    @Serializable
    data class Absent(
        val confidence: Double?,
    ) : DetectionOutcome

    @Serializable
    data class Unknown(
        val reason: String,
    ) : DetectionOutcome
}
```

- [ ] **Step 4: Update `RecognitionResult.kt`**

```kotlin
package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RecognitionResult(
    val status: CatPresenceStatus,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant,
    val confidence: Double? = null,
    val source: String,
    val error: RecognitionError? = null,
    val boundingBoxes: List<BoundingBox>? = null,
)
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.model.CoreModelShapeTest" 2>&1 | tail -10
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/DetectionOutcome.kt \
        services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionResult.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/model/CoreModelShapeTest.kt
git commit -m "feat: add bounding boxes to DetectionOutcome.Present and RecognitionResult"
```

---

## Task 3: Add bounding boxes to `RecognitionLatestResponse` and extend `LatestRecognitionState` with frame bytes

**Files:**

- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/RecognitionLatestResponse.kt`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionState.kt`
- Modify:
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionStateTest.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`

- [ ] **Step 1: Write the failing test for frame bytes in state**

Add to `LatestRecognitionStateTest`:

```kotlin
@Test
fun `frame bytes are stored alongside success result`() {
    val state = LatestRecognitionState()
    val result = RecognitionResult(
        status = CatPresenceStatus.DETECTED,
        observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        confidence = 0.9,
        source = "snapshot",
    )
    val frameBytes = "fake-jpeg".encodeToByteArray()

    state.recordSuccess(result, frameBytes)

    val snapshot = state.snapshot()
    assertTrue(snapshot.frameBytes!!.contentEquals(frameBytes))
}

@Test
fun `frame bytes are stored alongside failure result`() {
    val state = LatestRecognitionState()
    val result = RecognitionResult(
        status = CatPresenceStatus.UNKNOWN,
        observedAt = Instant.parse("2026-06-11T10:00:00Z"),
        source = "snapshot",
        error = RecognitionError("FRAME_FETCH_FAILED", "camera down", true),
    )
    val frameBytes = "fake-jpeg".encodeToByteArray()

    state.recordFailure(result, frameBytes)

    assertTrue(state.snapshot().frameBytes!!.contentEquals(frameBytes))
}

@Test
fun `frame bytes are null when no frame has been stored`() {
    assertNull(LatestRecognitionState().snapshot().frameBytes)
}
```

Add import `import kotlin.test.assertTrue`.

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.service.LatestRecognitionStateTest" 2>&1 | tail -20
```

Expected: FAIL — `recordSuccess` and `recordFailure` don't accept a second parameter

- [ ] **Step 3: Update `LatestRecognitionState.kt`**

```kotlin
package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@ApplicationScoped
class LatestRecognitionState {

    data class Snapshot(
        val latestResult: RecognitionResult? = null,
        val lastSuccessAt: Instant? = null,
        val lastError: RecognitionError? = null,
        val consecutiveFailures: Int = 0,
        val frameBytes: ByteArray? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Snapshot) return false
            return latestResult == other.latestResult &&
                lastSuccessAt == other.lastSuccessAt &&
                lastError == other.lastError &&
                consecutiveFailures == other.consecutiveFailures &&
                (frameBytes == null && other.frameBytes == null ||
                    frameBytes != null && other.frameBytes != null && frameBytes.contentEquals(other.frameBytes))
        }

        override fun hashCode(): Int {
            var result = latestResult?.hashCode() ?: 0
            result = 31 * result + (lastSuccessAt?.hashCode() ?: 0)
            result = 31 * result + (lastError?.hashCode() ?: 0)
            result = 31 * result + consecutiveFailures
            result = 31 * result + (frameBytes?.contentHashCode() ?: 0)
            return result
        }
    }

    private val state = AtomicReference(Snapshot())

    fun recordSuccess(result: RecognitionResult, frameBytes: ByteArray? = null) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastSuccessAt = result.observedAt,
                lastError = null,
                consecutiveFailures = 0,
                frameBytes = frameBytes ?: it.frameBytes,
            )
        }
    }

    fun recordFailure(result: RecognitionResult, frameBytes: ByteArray? = null) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastError = result.error,
                consecutiveFailures = it.consecutiveFailures + 1,
                frameBytes = frameBytes ?: it.frameBytes,
            )
        }
    }

    fun snapshot(): Snapshot = state.get()
}
```

- [ ] **Step 4: Update `RecognitionLatestResponse.kt`** to add `boundingBoxes`

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.InstantIso8601Serializer
import io.github.arhor.catrecognizer.domain.RecognitionError
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RecognitionLatestResponse(
    val status: CatPresenceStatus?,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant?,
    val confidence: Double?,
    val source: String?,
    val error: RecognitionError?,
    val worker: WorkerSummary,
    val boundingBoxes: List<BoundingBox>?,
)
```

- [ ] **Step 5: Update `RecognitionController.kt`** to pass `boundingBoxes` from result

In the `latest()` function, add `boundingBoxes = result?.boundingBoxes` to the `RecognitionLatestResponse(...)` call:

```kotlin
@GET
@Path("/latest")
fun latest(): RecognitionLatestResponse {
    val snapshot = state.snapshot()
    val result = snapshot.latestResult
    return RecognitionLatestResponse(
        status = result?.status,
        observedAt = result?.observedAt,
        confidence = result?.confidence,
        source = result?.source,
        error = result?.error,
        worker = WorkerSummary(
            lastSuccessAt = snapshot.lastSuccessAt,
            consecutiveFailures = snapshot.consecutiveFailures,
            lastErrorCode = snapshot.lastError?.code,
        ),
        boundingBoxes = result?.boundingBoxes,
    )
}
```

- [ ] **Step 6: Update `RecognitionControllerTest.kt`** — add `boundingBoxes` assertions

In `RecognitionControllerTest`, update the `installRecognitionMocks` function at the bottom so the mock detector returns
bounding boxes:

```kotlin
QuarkusMock.installMockForType(
    object : OpenCvCatDetector() {
        override fun detect(frame: FramePayload): DetectionOutcome =
            DetectionOutcome.Present(
                confidence = 0.91,
                boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
            )
    },
    OpenCvCatDetector::class.java,
)
```

Also update the `setUp` in `RecognitionControllerTest` — call `state.recordSuccess(result)` (no change needed, signature
is backward-compatible). But the stored result needs `boundingBoxes`. Change the `setUp` `RecognitionResult` to include
bounding boxes:

```kotlin
state.recordSuccess(
    RecognitionResult(
        status = CatPresenceStatus.DETECTED,
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
        confidence = 0.91,
        source = "snapshot",
        boundingBoxes = listOf(BoundingBox(x = 10, y = 20, width = 80, height = 100)),
    ),
)
```

Add to the `GET latest returns flattened recognition payload` test body:

```kotlin
.body("boundingBoxes[0].x", `is`(10))
.body("boundingBoxes[0].y", `is`(20))
.body("boundingBoxes[0].width", `is`(80))
.body("boundingBoxes[0].height", `is`(100))
```

Add import `import io.github.arhor.catrecognizer.domain.BoundingBox`.

- [ ] **Step 7: Run all tests**

```bash
cd services && ./gradlew :app-cat-recognizer:test 2>&1 | tail -20
```

Expected: PASS (all tests green)

- [ ] **Step 8: Commit**

```bash
git add services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionLatestResponse.kt \
        services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionState.kt \
        services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionStateTest.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt
git commit -m "feat: expose bounding boxes in recognition API and persist frame bytes in state"
```

---

## Task 4: Update `CatRecognitionService` to propagate bounding boxes and store frames

**Files:**

- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionService.kt`
- Modify:
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `CatRecognitionServiceTest`:

```kotlin
@Test
fun `maps present detector outcome with bounding boxes and records success`() {
    val state = LatestRecognitionState()
    val boxes = listOf(BoundingBox(x = 5, y = 10, width = 40, height = 50))
    val service = CatRecognitionService(
        frameClient = FrameClient { sampleFrame },
        detector = detectorStub(DetectionOutcome.Present(confidence = 0.87, boundingBoxes = boxes)),
        state = state,
    )

    val result = service.runRecognition()

    assertEquals(CatPresenceStatus.DETECTED, result.status)
    assertEquals(0.87, result.confidence)
    assertEquals(boxes, result.boundingBoxes)
    assertEquals(0, state.snapshot().consecutiveFailures)
    assertTrue(state.snapshot().frameBytes!!.contentEquals(sampleFrame.bytes))
}

@Test
fun `maps absent detector outcome and records success`() {
    val state = LatestRecognitionState()
    val service = CatRecognitionService(
        frameClient = FrameClient { sampleFrame },
        detector = detectorStub(DetectionOutcome.Absent(confidence = 0.95)),
        state = state,
    )

    val result = service.runRecognition()

    assertEquals(CatPresenceStatus.NOT_DETECTED, result.status)
    assertEquals(0.95, result.confidence)
    assertNull(result.boundingBoxes)
    assertEquals(0, state.snapshot().consecutiveFailures)
}
```

Add imports:

```kotlin
import io.github.arhor.catrecognizer.domain.BoundingBox
import kotlin.test.assertTrue
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.service.CatRecognitionServiceTest" 2>&1 | tail -20
```

Expected: FAIL — `frameBytes` not stored, `boundingBoxes` not propagated

- [ ] **Step 3: Update `CatRecognitionService.kt`**

```kotlin
package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
class CatRecognitionService @Inject constructor(
    private val frameClient: FrameClient,
    private val detector: OpenCvCatDetector,
    private val state: LatestRecognitionState,
) {

    fun runRecognition(): RecognitionResult {
        var frame: FramePayload? = null
        return try {
            frame = frameClient.fetchFrame()
            val outcome = detector.detect(frame)
            val result = when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "snapshot",
                    boundingBoxes = outcome.boundingBoxes.ifEmpty { null },
                )

                is DetectionOutcome.Absent -> RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    source = "snapshot",
                )

                is DetectionOutcome.Unknown -> RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = frame.observedAt,
                    confidence = null,
                    source = "snapshot",
                    error = RecognitionError(
                        code = "DETECTOR_UNKNOWN",
                        message = outcome.reason,
                        retriable = false,
                    ),
                )
            }

            if (result.error == null) {
                state.recordSuccess(result, frame.bytes)
            } else {
                state.recordFailure(result, frame.bytes)
            }

            result
        } catch (error: FrameSourceError) {
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.now(),
                confidence = null,
                source = "snapshot",
                error = RecognitionError(
                    code = error.code,
                    message = error.message,
                    retriable = error.retriable,
                ),
            )
            state.recordFailure(result, frame?.bytes)
            result
        } catch (error: Exception) {
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = frame?.observedAt ?: Instant.now(),
                confidence = null,
                source = "snapshot",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = error.message ?: "Detector execution failed",
                    retriable = false,
                ),
            )
            state.recordFailure(result, frame?.bytes)
            result
        }
    }
}
```

- [ ] **Step 4: Update existing `CatRecognitionServiceTest` tests** that construct `DetectionOutcome.Present` without
  `boundingBoxes` (they'll still compile since `boundingBoxes` has a default, but update the
  `maps unknown detector outcome` test's `detectorStub` call — no change needed since it uses
  `DetectionOutcome.Unknown`).

Also update the `maps detector exception to detector failed` test mock to be consistent:

```kotlin
private fun detectorStub(outcome: DetectionOutcome): OpenCvCatDetector =
    object : OpenCvCatDetector() {
        override fun detect(frame: FramePayload): DetectionOutcome = outcome
    }
```

(No change needed — this is already correct.)

- [ ] **Step 5: Run all tests**

```bash
cd services && ./gradlew :app-cat-recognizer:test 2>&1 | tail -20
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionService.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionServiceTest.kt
git commit -m "feat: propagate bounding boxes through recognition service and store frames in state"
```

---

## Task 5: Add `FrameController` to serve the latest frame

**Files:**

- Create: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/FrameController.kt`
- Create: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/FrameControllerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `FrameControllerTest.kt`:

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.LatestRecognitionState
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@TestProfile(FrameControllerTest.Profile::class)
class FrameControllerTest {

    @Inject
    lateinit var state: LatestRecognitionState

    @Test
    fun `GET frame latest returns 204 when no frame has been stored`() {
        given()
            .get("/frame/latest")
            .then()
            .statusCode(204)
    }

    @Test
    fun `GET frame latest returns jpeg bytes with correct content type`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        state.recordSuccess(
            RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = Instant.parse("2026-06-11T10:00:00Z"),
                confidence = 0.9,
                source = "snapshot",
            ),
            jpeg,
        )

        given()
            .get("/frame/latest")
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .header("Cache-Control", "no-store")
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.camera.snapshot-url" to "http://example.test/snapshot",
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.FrameControllerTest" 2>&1 | tail -20
```

Expected: FAIL — 404, controller doesn't exist

- [ ] **Step 3: Create `FrameController.kt`**

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.service.LatestRecognitionState
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/frame")
class FrameController @Inject constructor(
    private val state: LatestRecognitionState,
) {

    @GET
    @Path("/latest")
    fun latest(): Response {
        val bytes = state.snapshot().frameBytes
            ?: return Response.noContent().build()

        return Response.ok(bytes)
            .header("Content-Type", "image/jpeg")
            .header("Cache-Control", "no-store")
            .build()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.FrameControllerTest" 2>&1 | tail -10
```

Expected: PASS

- [ ] **Step 5: Run all tests**

```bash
cd services && ./gradlew :app-cat-recognizer:test 2>&1 | tail -10
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/FrameController.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/FrameControllerTest.kt
git commit -m "feat: add /frame/latest endpoint to serve latest camera frame"
```

---

## Task 6: Bundle Haar cascade and implement real OpenCV cat detection

**Files:**

- Create: `services/app-cat-recognizer/src/main/resources/cascades/haarcascade_frontalcatface.xml` (download from
  OpenCV)
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetector.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetectorTest.kt`

- [ ] **Step 1: Download the Haar cascade XML**

```bash
mkdir -p services/app-cat-recognizer/src/main/resources/cascades
curl -L "https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/haarcascade_frontalcatface.xml" \
    -o services/app-cat-recognizer/src/main/resources/cascades/haarcascade_frontalcatface.xml
wc -l services/app-cat-recognizer/src/main/resources/cascades/haarcascade_frontalcatface.xml
```

Expected: file written, line count > 1000

- [ ] **Step 2: Update the test**

Replace the full content of `OpenCvCatDetectorTest.kt`:

```kotlin
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
    fun `returns absent for plain color image with no cat`() {
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

    @Test
    fun `bounding boxes are empty when no cat is detected`() {
        val result = detector.detect(frame(solidColorJpeg(), "image/jpeg"))
        assertIs<DetectionOutcome.Absent>(result)
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
```

- [ ] **Step 3: Run the new tests to verify they fail**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.service.OpenCvCatDetectorTest" 2>&1 | tail -20
```

Expected: FAIL — detector still returns `Unknown("opencv placeholder detector")`

- [ ] **Step 4: Implement real detection in `OpenCvCatDetector.kt`**

```kotlin
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
                        BoundingBox(x = r.x, y = r.y, width = r.width.toInt(), height = r.height.toInt())
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
```

- [ ] **Step 5: Run the detector tests**

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.service.OpenCvCatDetectorTest" 2>&1 | tail -10
```

Expected: PASS

- [ ] **Step 6: Update `RecognitionControllerTest` mock** — the mock `OpenCvCatDetector` in `installRecognitionMocks`
  overrides `detect()` and still works fine. But the `POST /recognition/run` test expects `DETECTED` status with
  `confidence = 0.91f`. That test relies on the mock (not the real detector). No changes needed.

- [ ] **Step 7: Run all tests**

```bash
cd services && ./gradlew :app-cat-recognizer:test 2>&1 | tail -20
```

Expected: PASS (all tests green)

- [ ] **Step 8: Commit**

```bash
git add services/app-cat-recognizer/src/main/resources/cascades/haarcascade_frontalcatface.xml \
        services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetector.kt \
        services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetectorTest.kt
git commit -m "feat: implement real cat detection with Haar cascade"
```

---

## Task 7: Create the HTML viewer page

**Files:**

- Create: `services/app-cat-recognizer/src/main/resources/META-INF/resources/index.html`

> Note: Quarkus serves static files from `src/main/resources/META-INF/resources/` at the root path `/`. With
`quarkus.http.root-path=/api`, the API lives at `/api/…` and static files are served at `/`. So the page is at
`http://localhost:8080/` and calls `fetch('/api/recognition/latest')` and `fetch('/api/frame/latest')`.

- [ ] **Step 1: Create `index.html`**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cat Detector</title>
    <style>
        body {
            background: #111;
            color: #eee;
            font-family: monospace;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 16px;
            margin: 0;
        }
        h1 { margin-bottom: 8px; font-size: 1.2rem; }
        #status-bar {
            margin-bottom: 12px;
            padding: 6px 16px;
            border-radius: 4px;
            font-size: 1rem;
            background: #333;
        }
        #status-bar.detected   { background: #8b0000; }
        #status-bar.absent     { background: #1a4a1a; }
        #status-bar.unknown    { background: #333; }
        #frame-container {
            position: relative;
            display: inline-block;
        }
        #frame-img {
            display: block;
            max-width: 90vw;
        }
        #bbox-canvas {
            position: absolute;
            top: 0;
            left: 0;
            pointer-events: none;
        }
        #meta {
            margin-top: 10px;
            font-size: 0.8rem;
            color: #aaa;
        }
    </style>
</head>
<body>
    <h1>Cat Detector Live View</h1>
    <div id="status-bar" class="unknown">Waiting for data…</div>
    <div id="frame-container">
        <img id="frame-img" alt="Latest frame" />
        <canvas id="bbox-canvas"></canvas>
    </div>
    <div id="meta"></div>

    <script>
        const img       = document.getElementById('frame-img');
        const canvas    = document.getElementById('bbox-canvas');
        const ctx       = canvas.getContext('2d');
        const statusBar = document.getElementById('status-bar');
        const meta      = document.getElementById('meta');

        let pendingBoxes = [];

        function drawBoxes() {
            canvas.width  = img.naturalWidth;
            canvas.height = img.naturalHeight;
            canvas.style.width  = img.width  + 'px';
            canvas.style.height = img.height + 'px';

            ctx.clearRect(0, 0, canvas.width, canvas.height);

            const scaleX = img.naturalWidth  > 0 ? img.width  / img.naturalWidth  : 1;
            const scaleY = img.naturalHeight > 0 ? img.height / img.naturalHeight : 1;

            ctx.strokeStyle = 'red';
            ctx.lineWidth   = Math.max(2, Math.round(2 / scaleX));

            for (const box of pendingBoxes) {
                ctx.strokeRect(box.x, box.y, box.width, box.height);
            }
        }

        img.addEventListener('load', drawBoxes);
        window.addEventListener('resize', drawBoxes);

        async function poll() {
            try {
                const res  = await fetch('/api/recognition/latest');
                const data = await res.json();

                pendingBoxes = data.boundingBoxes || [];

                const ts = data.observedAt
                    ? new Date(data.observedAt).toLocaleTimeString()
                    : '—';

                switch (data.status) {
                    case 'DETECTED':
                        statusBar.textContent = '🐱 Cat detected' +
                            (data.confidence != null
                                ? ' (' + (data.confidence * 100).toFixed(0) + '%)'
                                : '');
                        statusBar.className = 'detected';
                        break;
                    case 'NOT_DETECTED':
                        statusBar.textContent = 'No cat detected';
                        statusBar.className = 'absent';
                        break;
                    default:
                        statusBar.textContent = data.error
                            ? 'Unknown (' + data.error.code + ')'
                            : 'Unknown';
                        statusBar.className = 'unknown';
                }

                meta.textContent = 'Last updated: ' + ts +
                    ' | Failures: ' + (data.worker?.consecutiveFailures ?? '—');

                img.src = '/api/frame/latest?t=' + Date.now();
                if (img.complete) drawBoxes();

            } catch (err) {
                statusBar.textContent = 'Connection error';
                statusBar.className = 'unknown';
            }
        }

        poll();
        setInterval(poll, 5000);
    </script>
</body>
</html>
```

- [ ] **Step 2: Build the service and verify the page is served**

```bash
cd services && ./gradlew :app-cat-recognizer:quarkusDev &
sleep 10
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
```

Expected: `200`

Kill the dev server with `kill %1` when done.

- [ ] **Step 3: Commit**

```bash
git add services/app-cat-recognizer/src/main/resources/META-INF/resources/index.html
git commit -m "feat: add HTML live viewer for cat detection"
```

---

## Self-Review

### Spec coverage

| Requirement                                          | Covered by                                                                                       |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| HTML page that shows the currently processed picture | Task 7 — `index.html` polls `/api/frame/latest` and renders it                                   |
| Surrounds detected cat with red square               | Task 7 — canvas overlay draws red `strokeRect` from bounding boxes in `DetectionOutcome.Present` |
| Bounding boxes from detector                         | Task 6 — `OpenCvCatDetector` uses Haar cascade and returns `BoundingBox` list                    |
| API carries bounding box data                        | Tasks 2–4 — boxes flow `DetectionOutcome → RecognitionResult → RecognitionLatestResponse`        |
| Frame persisted for `/frame/latest`                  | Tasks 3–4 — `LatestRecognitionState.Snapshot.frameBytes`, stored in `CatRecognitionService`      |

### Placeholder scan

No TBD, TODO, or "handle edge cases" language found. All code blocks are complete.

### Type consistency

- `BoundingBox(x, y, width, height: Int)` — introduced Task 1, used consistently in Tasks 2, 4, 6, 7 (JS uses same field
  names)
- `DetectionOutcome.Present(confidence, boundingBoxes)` — `boundingBoxes` default `emptyList()`, used consistently
- `LatestRecognitionState.recordSuccess/recordFailure(result, frameBytes?)` — `frameBytes` default `null`, used in Tasks
  3, 4, 5
- `RecognitionLatestResponse.boundingBoxes` — `List<BoundingBox>?`, serialised field name matches JS
  `data.boundingBoxes`
