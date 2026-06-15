# Cat Recognizer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Quarkus starter scaffold in `app-cat-recognizer` with a snapshot-first cat recognizer skeleton
that runs mainly as a background worker, keeps the latest result in memory, and exposes minimal operational HTTP and
health endpoints.

**Architecture:** The service stays as one Quarkus module with clear seams between frame acquisition, detection, recognition orchestration, worker lifecycle, state, and thin HTTP/health adapters. The first version uses a generic `FrameSource`, a config-driven stub detector, and a polling worker so the later ML detector can replace only one component instead of rewriting the service.

**Tech Stack:** Quarkus REST, SmallRye Health, Kotlin serialization, JUnit 5, Rest Assured, Kotlin coroutines test utilities already present in the module, and the JDK HTTP client.

---

## File Structure

### Existing files to modify or remove

- Modify: `app-cat-recognizer/src/main/resources/application.properties`
  - Add typed defaults for worker, camera, detection, state, and debug flags.
- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/GreetingResource.kt`
  - Remove the starter REST endpoint.
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/GreetingResourceTest.kt`
  - Remove the starter HTTP test.
- Delete: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/GreetingResourceIT.kt`
  - Remove the starter native smoke test.

### Main source files to create

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`
  - Nested config mapping for `worker`, `camera`, `detection`, `state`, and `debug`.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetector.kt`
  - Detector interface.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt`
  - Enum for `STUB`, `ALWAYS_PRESENT`, and `ALWAYS_ABSENT`.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt`
  - First concrete detector driven by config.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/model/DetectionOutcome.kt`
  - Detector-level sealed result.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/FrameSource.kt`
  - Frame acquisition interface.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSource.kt`
  - Snapshot URL implementation using the JDK HTTP client.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FramePayload.kt`
  - Frame bytes and metadata.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FrameSourceError.kt`
  - Domain-specific source failure type.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestrator.kt`
  - Runs one recognition cycle and maps errors.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/CatPresenceStatus.kt`
  - Public result enum.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionError.kt`
  - API-safe error representation.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionResult.kt`
  - Public recognition result.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionState.kt`
  - In-memory latest result plus worker metadata.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycle.kt`
  - Startup/shutdown and polling loop.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/RecognitionResource.kt`
  - Latest result and manual trigger endpoints.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/DebugResource.kt`
  - Runtime config summary endpoint.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/WorkerReadinessCheck.kt`
  - Readiness from cached worker state.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/FrameSourceHealthCheck.kt`
  - Health details around recent frame-source failures.

### Test files to create

- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSourceTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycleTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt`
- Create: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceIT.kt`

## Task 1: Add Config, Core Models, and the Stub Detector

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetector.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/model/DetectionOutcome.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/FrameSource.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FramePayload.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FrameSourceError.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/CatPresenceStatus.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionError.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionResult.kt`
- Modify: `app-cat-recognizer/src/main/resources/application.properties`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`

- [ ] **Step 1: Write the failing detector tests**

```kotlin
package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class StubCatDetectorTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
    )

    @Test
    fun `always present mode reports present`() {
        val detector = StubCatDetector(config(DetectionMode.ALWAYS_PRESENT))

        assertEquals(
            DetectionOutcome.Present(confidence = 1.0),
            detector.detect(sampleFrame),
        )
    }

    @Test
    fun `always absent mode reports absent`() {
        val detector = StubCatDetector(config(DetectionMode.ALWAYS_ABSENT))

        assertEquals(
            DetectionOutcome.Absent(confidence = 1.0),
            detector.detect(sampleFrame),
        )
    }

    @Test
    fun `stub mode reports unknown`() {
        val detector = StubCatDetector(config(DetectionMode.STUB))

        assertEquals(
            DetectionOutcome.Unknown(reason = "stub detector"),
            detector.detect(sampleFrame),
        )
    }

    private fun config(mode: DetectionMode): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = java.time.Duration.ofSeconds(5)
                override fun initialDelay() = java.time.Duration.ofSeconds(1)
                override fun failureBackoff() = java.time.Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = java.time.Duration.ofSeconds(2)
                override fun readTimeout() = java.time.Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = mode
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = java.time.Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.detection.StubCatDetectorTest"
```

Expected: FAIL with unresolved references for `RecognizerConfig`, `DetectionMode`, `StubCatDetector`, `DetectionOutcome`, and `FramePayload`.

- [ ] **Step 3: Add config mapping, domain models, and the stub detector**

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`

```kotlin
package io.github.arhor.catrecognizer.config

import io.github.arhor.catrecognizer.detection.DetectionMode
import io.smallrye.config.ConfigMapping
import java.time.Duration

@ConfigMapping(prefix = "cat-recognizer")
interface RecognizerConfig {
    fun worker(): Worker
    fun camera(): Camera
    fun detection(): Detection
    fun state(): State
    fun debug(): Debug

    interface Worker {
        fun enabled(): Boolean
        fun pollInterval(): Duration
        fun initialDelay(): Duration
        fun failureBackoff(): Duration
    }

    interface Camera {
        fun snapshotUrl(): String
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
    }

    interface Detection {
        fun mode(): DetectionMode
        fun unknownOnError(): Boolean
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
    }
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt`

```kotlin
package io.github.arhor.catrecognizer.detection

enum class DetectionMode {
    STUB,
    ALWAYS_PRESENT,
    ALWAYS_ABSENT,
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/model/DetectionOutcome.kt`

```kotlin
package io.github.arhor.catrecognizer.detection.model

sealed interface DetectionOutcome {
    data class Present(val confidence: Double?) : DetectionOutcome
    data class Absent(val confidence: Double?) : DetectionOutcome
    data class Unknown(val reason: String) : DetectionOutcome
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FramePayload.kt`

```kotlin
package io.github.arhor.catrecognizer.frame.model

import java.time.Instant

data class FramePayload(
    val bytes: ByteArray,
    val contentType: String?,
    val observedAt: Instant,
)
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FrameSourceError.kt`

```kotlin
package io.github.arhor.catrecognizer.frame.model

class FrameSourceError(
    val code: String,
    override val message: String,
    val retriable: Boolean,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/FrameSource.kt`

```kotlin
package io.github.arhor.catrecognizer.frame

import io.github.arhor.catrecognizer.frame.model.FramePayload

fun interface FrameSource {
    fun fetchFrame(): FramePayload
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetector.kt`

```kotlin
package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload

fun interface CatDetector {
    fun detect(frame: FramePayload): DetectionOutcome
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/CatPresenceStatus.kt`

```kotlin
package io.github.arhor.catrecognizer.recognition.model

enum class CatPresenceStatus {
    DETECTED,
    NOT_DETECTED,
    UNKNOWN,
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionError.kt`

```kotlin
package io.github.arhor.catrecognizer.recognition.model

import kotlinx.serialization.Serializable

@Serializable
data class RecognitionError(
    val code: String,
    val message: String,
    val retriable: Boolean,
)
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionResult.kt`

```kotlin
package io.github.arhor.catrecognizer.recognition.model

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RecognitionResult(
    val status: CatPresenceStatus,
    val observedAt: Instant,
    val confidence: Double?,
    val detectorMode: String,
    val source: String,
    val error: RecognitionError? = null,
)
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt`

```kotlin
package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.model.FramePayload
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class StubCatDetector(
    private val config: RecognizerConfig,
) : CatDetector {

    override fun detect(frame: FramePayload): DetectionOutcome =
        when (config.detection().mode()) {
            DetectionMode.ALWAYS_PRESENT -> DetectionOutcome.Present(confidence = 1.0)
            DetectionMode.ALWAYS_ABSENT -> DetectionOutcome.Absent(confidence = 1.0)
            DetectionMode.STUB -> DetectionOutcome.Unknown(reason = "stub detector")
        }
}
```

`app-cat-recognizer/src/main/resources/application.properties`

```properties
quarkus.package.jar.enabled=false
quarkus.native.enabled=true
quarkus.native.container-build=true
quarkus.container-image.build=true
quarkus.container-image.name=cat-recognizer
quarkus.container-image.tag=native

cat-recognizer.worker.enabled=true
cat-recognizer.worker.poll-interval=5S
cat-recognizer.worker.initial-delay=1S
cat-recognizer.worker.failure-backoff=30S

cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.camera.connect-timeout=2S
cat-recognizer.camera.read-timeout=5S

cat-recognizer.detection.mode=STUB
cat-recognizer.detection.unknown-on-error=true

cat-recognizer.state.stale-after=30S
cat-recognizer.debug.manual-trigger-enabled=true
```

- [ ] **Step 4: Run the detector test again**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.detection.StubCatDetectorTest"
```

Expected: PASS.

- [ ] **Step 5: Commit the core model and detector scaffold**

```bash
git add app-cat-recognizer/src/main/resources/application.properties \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetector.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/model/DetectionOutcome.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/FrameSource.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FramePayload.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/model/FrameSourceError.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/CatPresenceStatus.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionError.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/model/RecognitionResult.kt \
    app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt
git commit -m "feat: add cat recognizer core models"
```

## Task 2: Implement the Snapshot Frame Source

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSource.kt`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSourceTest.kt`

- [ ] **Step 1: Write the failing frame source tests**

```kotlin
package io.github.arhor.catrecognizer.frame

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SnapshotFrameSourceTest {

    private var server: HttpServer? = null

    @AfterTest
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun `fetches snapshot bytes and content type`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/snapshot") { exchange ->
                val body = byteArrayOf(1, 2, 3)
                exchange.responseHeaders.add("Content-Type", "image/jpeg")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        val frameSource = SnapshotFrameSource(config("http://localhost:${server!!.address.port}/snapshot"))

        val frame = frameSource.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), frame.bytes)
        assertEquals("image/jpeg", frame.contentType)
    }

    @Test
    fun `maps camera failures to frame source errors`() {
        val frameSource = SnapshotFrameSource(config("http://127.0.0.1:1/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameSource.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
    }

    private fun config(snapshotUrl: String): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofSeconds(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = snapshotUrl
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = DetectionMode.STUB
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
```

- [ ] **Step 2: Run the frame source test to verify it fails**

Run :

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.frame.SnapshotFrameSourceTest"
```

Expected: FAIL with unresolved reference `SnapshotFrameSource`.

- [ ] **Step 3: Implement the snapshot frame source**

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSource.kt`

```kotlin
package io.github.arhor.catrecognizer.frame

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import jakarta.enterprise.context.ApplicationScoped
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@ApplicationScoped
class SnapshotFrameSource(
    private val config: RecognizerConfig,
) : FrameSource {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(config.camera().connectTimeout())
        .build()

    override fun fetchFrame(): FramePayload {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.camera().snapshotUrl()))
            .timeout(config.camera().readTimeout())
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

            if (response.statusCode() !in 200..299) {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "Snapshot endpoint returned HTTP ${response.statusCode()}",
                    retriable = true,
                )
            }

            FramePayload(
                bytes = response.body(),
                contentType = response.headers().firstValue("Content-Type").orElse(null),
                observedAt = Instant.now(),
            )
        } catch (error: FrameSourceError) {
            throw error
        } catch (error: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch snapshot from ${config.camera().snapshotUrl()}",
                retriable = true,
                cause = error,
            )
        }
    }
}
```

- [ ] **Step 4: Run the frame source tests again**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.frame.SnapshotFrameSourceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit the snapshot frame source**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSource.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameSourceTest.kt
git commit -m "feat: add snapshot frame source"
```

## Task 3: Add Latest State and the Recognition Orchestrator

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionState.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestrator.kt`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`

- [ ] **Step 1: Write the failing state and orchestrator tests**

`app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt`

```kotlin
package io.github.arhor.catrecognizer.state

import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LatestRecognitionStateTest {

    @Test
    fun `records success and resets failure count`() {
        val state = LatestRecognitionState()
        val result = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = 0.9,
            detectorMode = "ALWAYS_PRESENT",
            source = "snapshot",
        )

        state.markWorkerEnabled(true)
        state.markWorkerRunning(true)
        state.recordSuccess(result)

        val snapshot = state.snapshot()
        assertEquals(result, snapshot.latestResult)
        assertEquals(0, snapshot.consecutiveFailures)
        assertNotNull(snapshot.lastSuccessAt)
    }

    @Test
    fun `records failure with latest error`() {
        val state = LatestRecognitionState()
        val failure = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-05T12:00:00Z"),
            confidence = null,
            detectorMode = "STUB",
            source = "snapshot",
            error = RecognitionError(
                code = "FRAME_FETCH_FAILED",
                message = "camera unavailable",
                retriable = true,
            ),
        )

        state.recordFailure(failure)

        val snapshot = state.snapshot()
        assertEquals(1, snapshot.consecutiveFailures)
        assertEquals("FRAME_FETCH_FAILED", snapshot.lastError?.code)
    }
}
```

`app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`

```kotlin
package io.github.arhor.catrecognizer.recognition

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecognitionOrchestratorTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-05T12:00:00Z"),
    )

    @Test
    fun `maps present detection to detected result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource { sampleFrame },
            detector = CatDetector { DetectionOutcome.Present(confidence = 0.9) },
            state = LatestRecognitionState(),
            config = config(DetectionMode.ALWAYS_PRESENT),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.DETECTED, result.status)
        assertEquals(0.9, result.confidence)
        assertNull(result.error)
    }

    @Test
    fun `maps frame source failure to unknown result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "camera unavailable",
                    retriable = true,
                )
            },
            detector = CatDetector { DetectionOutcome.Present(confidence = 1.0) },
            state = LatestRecognitionState(),
            config = config(DetectionMode.STUB),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("FRAME_FETCH_FAILED", result.error?.code)
    }

    @Test
    fun `maps detector exceptions to unknown result`() {
        val orchestrator = RecognitionOrchestrator(
            frameSource = FrameSource { sampleFrame },
            detector = CatDetector { error("detector crashed") },
            state = LatestRecognitionState(),
            config = config(DetectionMode.STUB),
        )

        val result = orchestrator.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("DETECTOR_FAILED", result.error?.code)
    }

    private fun config(mode: DetectionMode): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = true
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofSeconds(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = mode
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app-cat-recognizer:test \
          --tests "io.github.arhor.catrecognizer.state.LatestRecognitionStateTest" \
          --tests "io.github.arhor.catrecognizer.recognition.RecognitionOrchestratorTest"
```

Expected: FAIL with unresolved references for `LatestRecognitionState` and `RecognitionOrchestrator`.

- [ ] **Step 3: Implement state storage and orchestration**

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionState.kt`

```kotlin
package io.github.arhor.catrecognizer.state

import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@ApplicationScoped
class LatestRecognitionState {

    data class Snapshot(
        val workerEnabled: Boolean = false,
        val workerRunning: Boolean = false,
        val latestResult: RecognitionResult? = null,
        val lastSuccessAt: Instant? = null,
        val lastError: RecognitionError? = null,
        val consecutiveFailures: Int = 0,
    )

    private val state = AtomicReference(Snapshot())

    fun markWorkerEnabled(enabled: Boolean) {
        state.updateAndGet { it.copy(workerEnabled = enabled) }
    }

    fun markWorkerRunning(running: Boolean) {
        state.updateAndGet { it.copy(workerRunning = running) }
    }

    fun recordSuccess(result: RecognitionResult) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastSuccessAt = result.observedAt,
                lastError = null,
                consecutiveFailures = 0,
            )
        }
    }

    fun recordFailure(result: RecognitionResult) {
        state.updateAndGet {
            it.copy(
                latestResult = result,
                lastError = result.error,
                consecutiveFailures = it.consecutiveFailures + 1,
            )
        }
    }

    fun snapshot(): Snapshot = state.get()
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestrator.kt`

```kotlin
package io.github.arhor.catrecognizer.recognition

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

@ApplicationScoped
class RecognitionOrchestrator(
    private val frameSource: FrameSource,
    private val detector: CatDetector,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) {

    fun runRecognition(): RecognitionResult {
        return try {
            val frame = frameSource.fetchFrame()
            val outcome = detector.detect(frame)
            val result = when (outcome) {
                is DetectionOutcome.Present -> RecognitionResult(
                    status = CatPresenceStatus.DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    detectorMode = config.detection().mode().name,
                    source = "snapshot",
                )
                is DetectionOutcome.Absent -> RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = frame.observedAt,
                    confidence = outcome.confidence,
                    detectorMode = config.detection().mode().name,
                    source = "snapshot",
                )
                is DetectionOutcome.Unknown -> RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = frame.observedAt,
                    confidence = null,
                    detectorMode = config.detection().mode().name,
                    source = "snapshot",
                    error = RecognitionError(
                        code = "DETECTOR_UNKNOWN",
                        message = outcome.reason,
                        retriable = false,
                    ),
                )
            }

            if (result.error == null) {
                state.recordSuccess(result)
            } else {
                state.recordFailure(result)
            }

            result
        } catch (error: FrameSourceError) {
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.now(),
                confidence = null,
                detectorMode = config.detection().mode().name,
                source = "snapshot",
                error = RecognitionError(
                    code = error.code,
                    message = error.message,
                    retriable = error.retriable,
                ),
            )
            state.recordFailure(result)
            result
        } catch (error: Exception) {
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.now(),
                confidence = null,
                detectorMode = config.detection().mode().name,
                source = "snapshot",
                error = RecognitionError(
                    code = "DETECTOR_FAILED",
                    message = error.message ?: "Detector execution failed",
                    retriable = config.detection().unknownOnError(),
                ),
            )
            state.recordFailure(result)
            result
        }
    }
}
```

- [ ] **Step 4: Run the state and orchestrator tests again**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.state.LatestRecognitionStateTest" --tests "io.github.arhor.catrecognizer.recognition.RecognitionOrchestratorTest"
```

Expected: PASS.

- [ ] **Step 5: Commit the state and orchestrator**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionState.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestrator.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt
git commit -m "feat: add recognition orchestration"
```

## Task 4: Add the Worker Lifecycle and Polling Loop

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycle.kt`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycleTest.kt`

- [ ] **Step 1: Write the failing worker lifecycle tests**

```kotlin
package io.github.arhor.catrecognizer.bootstrap

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.detection.DetectionMode
import io.github.arhor.catrecognizer.recognition.model.CatPresenceStatus
import io.github.arhor.catrecognizer.recognition.model.RecognitionError
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkerLifecycleTest {

    @Test
    fun `uses poll interval after successful recognition`() {
        val lifecycle = WorkerLifecycle(
            config = config(enabled = true),
            orchestrator = orchestrator(),
            state = LatestRecognitionState(),
        )

        assertEquals(
            Duration.ofSeconds(5),
            lifecycle.delayFor(
                RecognitionResult(
                    status = CatPresenceStatus.NOT_DETECTED,
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                    confidence = null,
                    detectorMode = "STUB",
                    source = "snapshot",
                ),
            ),
        )
    }

    @Test
    fun `uses failure backoff after failed recognition`() {
        val lifecycle = WorkerLifecycle(
            config = config(enabled = true),
            orchestrator = orchestrator(),
            state = LatestRecognitionState(),
        )

        assertEquals(
            Duration.ofSeconds(30),
            lifecycle.delayFor(
                RecognitionResult(
                    status = CatPresenceStatus.UNKNOWN,
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                    confidence = null,
                    detectorMode = "STUB",
                    source = "snapshot",
                    error = RecognitionError(
                        code = "FRAME_FETCH_FAILED",
                        message = "camera unavailable",
                        retriable = true,
                    ),
                ),
            ),
        )
    }

    private fun config(enabled: Boolean): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun enabled() = enabled
                override fun pollInterval() = Duration.ofSeconds(5)
                override fun initialDelay() = Duration.ofSeconds(1)
                override fun failureBackoff() = Duration.ofSeconds(30)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = "http://localhost/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
            }

            override fun detection() = object : RecognizerConfig.Detection {
                override fun mode() = DetectionMode.STUB
                override fun unknownOnError() = true
            }

            override fun state() = object : RecognizerConfig.State {
                override fun staleAfter() = Duration.ofSeconds(30)
            }

            override fun debug() = object : RecognizerConfig.Debug {
                override fun manualTriggerEnabled() = true
            }
        }

    private fun orchestrator(): io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator =
        io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator(
            frameSource = io.github.arhor.catrecognizer.frame.FrameSource {
                io.github.arhor.catrecognizer.frame.model.FramePayload(
                    bytes = byteArrayOf(),
                    contentType = "image/jpeg",
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                )
            },
            detector = io.github.arhor.catrecognizer.detection.CatDetector {
                io.github.arhor.catrecognizer.detection.model.DetectionOutcome.Absent(confidence = null)
            },
            state = LatestRecognitionState(),
            config = config(enabled = true),
        )
}
```

- [ ] **Step 2: Run the worker lifecycle test to verify it fails**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.bootstrap.WorkerLifecycleTest"
```

Expected: FAIL with unresolved reference `WorkerLifecycle`.

- [ ] **Step 3: Add the worker lifecycle**

Create `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycle.kt`

```kotlin
package io.github.arhor.catrecognizer.bootstrap

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ApplicationScoped
class WorkerLifecycle(
    private val config: RecognizerConfig,
    private val orchestrator: RecognitionOrchestrator,
    private val state: LatestRecognitionState,
) {

    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()

    fun onStart(@Observes event: StartupEvent) {
        state.markWorkerEnabled(config.worker().enabled())
        if (!config.worker().enabled()) {
            return
        }

        running.set(true)
        state.markWorkerRunning(true)
        executor.submit {
            sleep(config.worker().initialDelay())
            while (running.get()) {
                val result = orchestrator.runRecognition()
                sleep(delayFor(result))
            }
        }
    }

    fun onStop(@Observes event: ShutdownEvent) {
        running.set(false)
        state.markWorkerRunning(false)
        executor.shutdownNow()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    internal fun delayFor(result: io.github.arhor.catrecognizer.recognition.model.RecognitionResult): Duration =
        if (result.error == null) config.worker().pollInterval() else config.worker().failureBackoff()

    private fun sleep(duration: Duration) {
        try {
            Thread.sleep(duration.toMillis())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
```

- [ ] **Step 4: Run the worker lifecycle test again**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.bootstrap.WorkerLifecycleTest"
```

Expected: PASS.

- [ ] **Step 5: Commit the worker lifecycle**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycle.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycleTest.kt
git commit -m "feat: add cat recognizer worker lifecycle"
```

## Task 5: Replace the Starter Endpoint with Recognition and Debug APIs

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/RecognitionResource.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/DebugResource.kt`
- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/GreetingResource.kt`
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/GreetingResourceTest.kt`
- Test: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceTest.kt`

- [ ] **Step 1: Write the failing resource tests**

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.detection.CatDetector
import io.github.arhor.catrecognizer.detection.model.DetectionOutcome
import io.github.arhor.catrecognizer.frame.FrameSource
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusMock
import io.restassured.RestAssured.given
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class RecognitionResourceTest {

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(
            FrameSource {
                FramePayload(
                    bytes = "frame".encodeToByteArray(),
                    contentType = "image/jpeg",
                    observedAt = Instant.parse("2026-06-05T12:00:00Z"),
                )
            },
            FrameSource::class.java,
        )

        QuarkusMock.installMockForType(
            CatDetector { DetectionOutcome.Absent(confidence = 0.8) },
            CatDetector::class.java,
        )
    }

    @Test
    fun `returns latest recognition payload`() {
        given()
            .`when`().get("/api/recognition/latest")
            .then()
            .statusCode(200)
    }

    @Test
    fun `runs manual recognition`() {
        given()
            .`when`().post("/api/recognition/run")
            .then()
            .statusCode(200)
    }

    @Test
    fun `returns debug config summary`() {
        given()
            .`when`().get("/api/debug/config")
            .then()
            .statusCode(200)
    }
}
```

- [ ] **Step 2: Run the resource tests to verify they fail**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionResourceTest"
```

Expected: FAIL because `/api/recognition/latest`, `/api/recognition/run`, and `/api/debug/config` do not exist.

- [ ] **Step 3: Add the REST resources and remove the greeting resource**

Create `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/RecognitionResource.kt`

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.Serializable

@Path("/api/recognition")
@Produces(MediaType.APPLICATION_JSON)
class RecognitionResource(
    private val orchestrator: RecognitionOrchestrator,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/latest")
    fun latest(): LatestRecognitionResponse {
        val snapshot = state.snapshot()
        return LatestRecognitionResponse(
            result = snapshot.latestResult,
            worker = WorkerStatusResponse(
                enabled = snapshot.workerEnabled,
                running = snapshot.workerRunning,
                lastSuccessAt = snapshot.lastSuccessAt?.toString(),
                consecutiveFailures = snapshot.consecutiveFailures,
                lastErrorCode = snapshot.lastError?.code,
            ),
        )
    }

    @POST
    @Path("/run")
    fun runNow(): RecognitionResult {
        if (!config.debug().manualTriggerEnabled()) {
            throw WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("""{"message":"manual trigger disabled"}""")
                    .build(),
            )
        }

        return orchestrator.runRecognition()
    }
}

@Serializable
data class LatestRecognitionResponse(
    val result: RecognitionResult?,
    val worker: WorkerStatusResponse,
)

@Serializable
data class WorkerStatusResponse(
    val enabled: Boolean,
    val running: Boolean,
    val lastSuccessAt: String?,
    val consecutiveFailures: Int,
    val lastErrorCode: String?,
)
```

Create `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/DebugResource.kt`

```kotlin
package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.Serializable

@Path("/api/debug")
@Produces(MediaType.APPLICATION_JSON)
class DebugResource(
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/config")
    fun config(): DebugConfigResponse =
        DebugConfigResponse(
            workerEnabled = config.worker().enabled(),
            pollInterval = config.worker().pollInterval().toString(),
            failureBackoff = config.worker().failureBackoff().toString(),
            detectionMode = config.detection().mode().name,
            snapshotConfigured = config.camera().snapshotUrl().isNotBlank(),
        )
}

@Serializable
data class DebugConfigResponse(
    val workerEnabled: Boolean,
    val pollInterval: String,
    val failureBackoff: String,
    val detectionMode: String,
    val snapshotConfigured: Boolean,
)
```

Delete `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/GreetingResource.kt`

```text
Delete the file entirely.
```

Delete `app-cat-recognizer/src/test/kotlin/io/github/arhor/GreetingResourceTest.kt`

```text
Delete the file entirely.
```

- [ ] **Step 4: Run the resource tests again**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionResourceTest"
```

Expected: PASS.

- [ ] **Step 5: Commit the recognition and debug API**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/RecognitionResource.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/DebugResource.kt
git rm app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/GreetingResource.kt \
       app-cat-recognizer/src/test/kotlin/io/github/arhor/GreetingResourceTest.kt
git add app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceTest.kt
git commit -m "feat: add recognition debug api"
```

## Task 6: Add Health Checks and Native Smoke Coverage

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/WorkerReadinessCheck.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/FrameSourceHealthCheck.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt`
- Create: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceIT.kt`
- Delete: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/GreetingResourceIT.kt`

- [ ] **Step 1: Write the failing health and native smoke tests**

`app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt`

```kotlin
package io.github.arhor.catrecognizer.health

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthEndpointsTest {

    @Test
    fun `readiness endpoint responds`() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("checks.name", hasItems("worker-readiness", "frame-source"))
    }

    @Test
    fun `liveness endpoint responds`() {
        given()
            .`when`().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }
}
```

`app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceIT.kt`

```kotlin
package io.github.arhor.catrecognizer.web

import io.quarkus.test.junit.QuarkusIntegrationTest

@QuarkusIntegrationTest
class RecognitionResourceIT : RecognitionResourceTest()
```

- [ ] **Step 2: Run the health test to verify it fails**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.health.HealthEndpointsTest"
```

Expected: FAIL because the response does not yet include the `worker-readiness` and `frame-source` checks.

- [ ] **Step 3: Implement readiness and frame-source health**

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/WorkerReadinessCheck.kt`

```kotlin
package io.github.arhor.catrecognizer.health

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.Duration
import java.time.Instant

@Readiness
@ApplicationScoped
class WorkerReadinessCheck(
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
) : HealthCheck {

    override fun call(): HealthCheckResponse {
        val snapshot = state.snapshot()
        if (!snapshot.workerEnabled) {
            return HealthCheckResponse.up("worker-readiness")
        }

        val lastSuccessAt = snapshot.lastSuccessAt
        if (lastSuccessAt == null) {
            return HealthCheckResponse.up("worker-readiness")
                .withData("state", "warming-up")
                .build()
        }

        val fresh = Duration.between(lastSuccessAt, Instant.now()) <= config.state().staleAfter()
        return if (fresh) {
            HealthCheckResponse.up("worker-readiness")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        } else {
            HealthCheckResponse.down("worker-readiness")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        }
    }
}
```

`app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/FrameSourceHealthCheck.kt`

```kotlin
package io.github.arhor.catrecognizer.health

import io.github.arhor.catrecognizer.state.LatestRecognitionState
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness

@Readiness
@ApplicationScoped
class FrameSourceHealthCheck(
    private val state: LatestRecognitionState,
) : HealthCheck {

    override fun call(): HealthCheckResponse {
        val snapshot = state.snapshot()
        val lastError = snapshot.lastError

        return if (lastError == null) {
            HealthCheckResponse.up("frame-source")
                .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
                .build()
        } else {
            HealthCheckResponse.down("frame-source")
                .withData("errorCode", lastError.code)
                .withData("retriable", lastError.retriable)
                .build()
        }
    }
}
```

Delete `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/GreetingResourceIT.kt`

```text
Delete the file entirely.
```

- [ ] **Step 4: Run unit, integration, and native smoke checks**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.health.HealthEndpointsTest"
./gradlew :app-cat-recognizer:quarkusIntTest --tests "io.github.arhor.catrecognizer.web.RecognitionResourceIT"
```

Expected:

- `HealthEndpointsTest`: PASS
- `RecognitionResourceIT`: PASS

- [ ] **Step 5: Commit the health and native coverage**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/WorkerReadinessCheck.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/health/FrameSourceHealthCheck.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt \
        app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceIT.kt
git rm app-cat-recognizer/src/native-test/kotlin/io/github/arhor/GreetingResourceIT.kt
git commit -m "feat: add cat recognizer health checks"
```

## Task 7: Final Full-Module Verification

**Files:**
- Modify: no source changes expected
- Verify: `app-cat-recognizer`

- [ ] **Step 1: Run the full JVM test suite**

Run:

```bash
./gradlew :app-cat-recognizer:test
```

Expected: PASS with all unit and Quarkus JVM tests green.

- [ ] **Step 2: Run the Quarkus integration tests**

Run:

```bash
./gradlew :app-cat-recognizer:quarkusIntTest
```

Expected: PASS with the native/integration smoke tests green.

- [ ] **Step 3: Run the full module build**

Run:

```bash
./gradlew :app-cat-recognizer:build
```

Expected: PASS and produce the Quarkus build artifact.

- [ ] **Step 4: Commit the verification checkpoint**

```bash
git add -A
git commit -m "chore: finalize cat recognizer service skeleton"
```
