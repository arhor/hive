# Cat Recognizer Quarkus Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify `app-cat-recognizer` into a scheduler-driven Quarkus service that uses SmallRye Fault
Tolerance at the snapshot client boundary and a single OpenCV detection path.

**Architecture:** Keep the intentional `client` / `domain` / `service` / `web` package direction, but delete custom
lifecycle and detector-selection infrastructure. `CatRecognitionJob` triggers work on a fixed cadence,
`SnapshotFrameClient` owns fault-tolerant camera access, `OpenCvCatDetector` is the only detector, and controllers plus
health checks read a smaller in-memory state model.

**Tech Stack:** Kotlin, Quarkus 3, Quarkus Scheduler, SmallRye Fault Tolerance, Quarkiverse OpenCV, JUnit 5,
QuarkusTest, Rest Assured, Gradle

---

### Task 1: Simplify Configuration and Dependency Wiring

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `app-cat-recognizer/build.gradle.kts`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`
- Modify: `app-cat-recognizer/src/main/resources/application.properties`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`

- [ ] **Step 1: Write the failing config-binding test for the simplified shape**

```kotlin
package io.github.arhor.catrecognizer.config

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@QuarkusTest
class RecognizerConfigBindingTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds simplified defaults from application properties`() {
        assertEquals(Duration.ofSeconds(5), config.worker().pollInterval())
        assertEquals(Duration.ofSeconds(30), config.state().staleAfter())
        assertTrue(config.debug().manualTriggerEnabled())

        assertEquals(Duration.ofSeconds(2), config.camera().connectTimeout())
        assertEquals(Duration.ofSeconds(5), config.camera().readTimeout())
        assertEquals("http://esp32-cam.local/snapshot", config.camera().snapshotUrl())
    }
}
```

- [ ] **Step 2: Run the config test to verify it fails**

Run: `./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest`

Expected: FAIL because `RecognizerConfig` and `application.properties` still expose `worker.enabled`, `initialDelay`,
`failureBackoff`, detection mode, and `unknownOnError`.

- [ ] **Step 3: Remove obsolete config surfaces and add the fault-tolerance dependency alias**

```toml
# gradle/libs.versions.toml
[libraries]
quarkus-smallrye-fault-tolerance = { module = "io.quarkus:quarkus-smallrye-fault-tolerance" }
```

```kotlin
// app-cat-recognizer/build.gradle.kts
dependencies {
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.container.image.docker)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.opencv)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.kotlin)
    implementation(libs.quarkus.rest.kotlin.serialization)
    implementation(libs.quarkus.scheduler)
    implementation(libs.quarkus.smallrye.fault.tolerance)
    implementation(libs.quarkus.smallrye.health)
}
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt
@ConfigMapping(prefix = "cat-recognizer")
interface RecognizerConfig {
    fun worker(): Worker
    fun camera(): Camera
    fun state(): State
    fun debug(): Debug

    interface Worker {
        fun pollInterval(): Duration
    }

    interface Camera {
        fun snapshotUrl(): String
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
    }
}
```

```properties
# app-cat-recognizer/src/main/resources/application.properties
quarkus.package.jar.enabled=false
quarkus.native.enabled=true
quarkus.native.container-build=true
quarkus.container-image.build=true
quarkus.container-image.name=cat-recognizer
quarkus.container-image.tag=native

cat-recognizer.worker.poll-interval=5S
cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.camera.connect-timeout=2S
cat-recognizer.camera.read-timeout=5S
cat-recognizer.state.stale-after=30S
cat-recognizer.debug.manual-trigger-enabled=true
```

- [ ] **Step 4: Run the config test to verify it passes**

Run: `./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml \
  app-cat-recognizer/build.gradle.kts \
  app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt \
  app-cat-recognizer/src/main/resources/application.properties \
  app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt
git commit -m "refactor: simplify cat recognizer config surface"
```

### Task 2: Replace Custom Worker Semantics with Quarkus Scheduler and Client-Level Fault Tolerance

**Files:**

- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/job/CatRecognitionJob.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClient.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/job/CatRecognitionJobTest.kt`
- Create:
  `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClientFaultToleranceTest.kt`
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/CatRecognitionJobTest.kt`

- [ ] **Step 1: Write the failing scheduler and annotation tests**

```kotlin
package io.github.arhor.catrecognizer.job

import io.quarkus.scheduler.Scheduled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CatRecognitionJobTest {

    @Test
    fun `detect uses configured poll interval and skips overlapping executions`() {
        val method = CatRecognitionJob::class.java.getMethod("detect")
        val scheduled = method.getAnnotation(Scheduled::class.java)

        assertNotNull(scheduled)
        assertEquals("{cat-recognizer.worker.poll-interval}", scheduled.every())
        assertEquals(Scheduled.ConcurrentExecution.SKIP, scheduled.concurrentExecution())
    }
}
```

```kotlin
package io.github.arhor.catrecognizer.client

import org.eclipse.microprofile.faulttolerance.Retry
import org.eclipse.microprofile.faulttolerance.Timeout
import kotlin.test.Test
import kotlin.test.assertNotNull

class SnapshotFrameClientFaultToleranceTest {

    @Test
    fun `fetchFrame is annotated with retry and timeout`() {
        val method = SnapshotFrameClient::class.java.getMethod("fetchFrame")

        assertNotNull(method.getAnnotation(Retry::class.java))
        assertNotNull(method.getAnnotation(Timeout::class.java))
    }
}
```

- [ ] **Step 2: Run the focused tests to verify they fail**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.job.CatRecognitionJobTest --tests io.github.arhor.catrecognizer.client.SnapshotFrameClientFaultToleranceTest`

Expected: FAIL because the old bootstrap test shape still exists and `SnapshotFrameClient.fetchFrame()` is not yet
annotated.

- [ ] **Step 3: Make the job thin and add fault-tolerance annotations to the client**

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/job/CatRecognitionJob.kt
@ApplicationScoped
class CatRecognitionJob @Inject constructor(
    private val recognitionService: CatRecognitionService,
) {

    @Scheduled(
        every = "{cat-recognizer.worker.poll-interval}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
    )
    fun detect() {
        recognitionService.runRecognition()
    }
}
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClient.kt
@ApplicationScoped
open class SnapshotFrameClient @Inject constructor(
    private val config: RecognizerConfig,
) : FrameClient {

    @Retry
    @Timeout(3_000)
    override fun fetchFrame(): FramePayload {
        val snapshotUrl = config.camera().snapshotUrl()

        return try {
            val response = client.send(
                HttpRequest.newBuilder(URI.create(snapshotUrl))
                    .timeout(config.camera().readTimeout())
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray(),
            )

            if (response.statusCode() in 200..299) {
                FramePayload(
                    bytes = response.body(),
                    contentType = response.headers().firstValue("Content-Type").orElse(null),
                    observedAt = Instant.now(),
                )
            } else {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "Failed to fetch snapshot: HTTP ${response.statusCode()}",
                    retriable = true,
                )
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch snapshot from $snapshotUrl",
                retriable = true,
                cause = exception,
            )
        } catch (exception: FrameSourceError) {
            throw exception
        } catch (exception: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch snapshot from $snapshotUrl",
                retriable = true,
                cause = exception,
            )
        }
    }
}
```

```properties
# app-cat-recognizer/src/main/resources/application.properties
quarkus.fault-tolerance."io.github.arhor.catrecognizer.client.SnapshotFrameClient/fetchFrame".retry.max-retries=2
quarkus.fault-tolerance."io.github.arhor.catrecognizer.client.SnapshotFrameClient/fetchFrame".retry.delay=250
quarkus.fault-tolerance."io.github.arhor.catrecognizer.client.SnapshotFrameClient/fetchFrame".timeout.value=3000
```

- [ ] **Step 4: Run the focused tests to verify they pass**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.job.CatRecognitionJobTest --tests io.github.arhor.catrecognizer.client.SnapshotFrameClientFaultToleranceTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/job/CatRecognitionJob.kt \
    app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClient.kt \
    app-cat-recognizer/src/main/resources/application.properties \
    app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/job/CatRecognitionJobTest.kt \
    app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClientFaultToleranceTest.kt \
    app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/CatRecognitionJobTest.kt
git commit -m "refactor: use quarkus scheduler and client fault tolerance"
```

### Task 3: Remove Detector Modes and Simplify Service-State Flow

**Files:**

- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/DetectionMode.kt`
- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatDetector.kt`
- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatDetectorFactory.kt`
- Delete: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/StubCatDetector.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionService.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetector.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionState.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionResult.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionServiceTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionStateTest.kt`
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/CatRecognitionServiceTest.kt`
- Delete: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt`

- [ ] **Step 1: Write the failing service and state tests**

```kotlin
package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FramePayload
import io.github.arhor.catrecognizer.domain.FrameSourceError
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatRecognitionServiceTest {

    private val sampleFrame = FramePayload(
        bytes = "frame".encodeToByteArray(),
        contentType = "image/jpeg",
        observedAt = Instant.parse("2026-06-08T12:00:00Z"),
    )

    @Test
    fun `maps unknown detector outcome and records failure`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient { sampleFrame },
            detector = OpenCvCatDetectorStub(DetectionOutcome.Unknown("opencv placeholder detector")),
            state = state,
        )

        val result = service.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertNull(result.confidence)
        assertEquals("DETECTOR_UNKNOWN", result.error?.code)
        assertEquals(1, state.snapshot().consecutiveFailures)
    }

    @Test
    fun `maps frame source error and records failure`() {
        val state = LatestRecognitionState()
        val service = CatRecognitionService(
            frameClient = FrameClient {
                throw FrameSourceError("FRAME_FETCH_FAILED", "camera unavailable", true)
            },
            detector = OpenCvCatDetectorStub(DetectionOutcome.Unknown("ignored")),
            state = state,
        )

        val result = service.runRecognition()

        assertEquals(CatPresenceStatus.UNKNOWN, result.status)
        assertEquals("FRAME_FETCH_FAILED", result.error?.code)
        assertEquals(1, state.snapshot().consecutiveFailures)
    }

    private class OpenCvCatDetectorStub(
        private val outcome: DetectionOutcome,
    ) : OpenCvCatDetector() {
        override fun detect(frame: FramePayload): DetectionOutcome = outcome
    }
}
```

```kotlin
package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LatestRecognitionStateTest {

    @Test
    fun `initial snapshot only exposes recognition state defaults`() {
        val snapshot = LatestRecognitionState().snapshot()

        assertNull(snapshot.latestResult)
        assertNull(snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }

    @Test
    fun `success resets error state`() {
        val state = LatestRecognitionState()
        val failure = RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = Instant.parse("2026-06-08T11:59:00Z"),
            confidence = null,
            source = "snapshot",
            error = RecognitionError("FRAME_FETCH_FAILED", "camera unavailable", true),
        )
        val success = RecognitionResult(
            status = CatPresenceStatus.DETECTED,
            observedAt = Instant.parse("2026-06-08T12:00:00Z"),
            confidence = 0.9,
            source = "snapshot",
        )

        state.recordFailure(failure)
        state.recordSuccess(success)

        val snapshot = state.snapshot()
        assertEquals(success, snapshot.latestResult)
        assertEquals(success.observedAt, snapshot.lastSuccessAt)
        assertNull(snapshot.lastError)
        assertEquals(0, snapshot.consecutiveFailures)
    }
}
```

- [ ] **Step 2: Run the focused tests to verify they fail**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.service.CatRecognitionServiceTest --tests io.github.arhor.catrecognizer.service.LatestRecognitionStateTest`

Expected: FAIL because the service still depends on detector modes/config and state still exposes worker flags.

- [ ] **Step 3: Delete mode infrastructure and make the service depend directly on OpenCV**

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetector.kt
@ApplicationScoped
open class OpenCvCatDetector {

    open fun detect(frame: FramePayload): DetectionOutcome {
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
}
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionService.kt
@ApplicationScoped
open class CatRecognitionService @Inject constructor(
    private val frameClient: FrameClient,
    private val detector: OpenCvCatDetector,
    private val state: LatestRecognitionState,
) {

    open fun runRecognition(): RecognitionResult {
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
            if (result.error == null) state.recordSuccess(result) else state.recordFailure(result)
            result
        } catch (error: FrameSourceError) {
            val result = RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = Instant.now(),
                confidence = null,
                source = "snapshot",
                error = RecognitionError(error.code, error.message, error.retriable),
            )
            state.recordFailure(result)
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
            state.recordFailure(result)
            result
        }
    }
}
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionState.kt
@ApplicationScoped
class LatestRecognitionState {

    data class Snapshot(
        val latestResult: RecognitionResult? = null,
        val lastSuccessAt: Instant? = null,
        val lastError: RecognitionError? = null,
        val consecutiveFailures: Int = 0,
    )

    private val state = AtomicReference(Snapshot())

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

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionResult.kt
@Serializable
data class RecognitionResult(
    val status: CatPresenceStatus,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant,
    val confidence: Double?,
    val source: String,
    val error: RecognitionError? = null,
)
```

- [ ] **Step 4: Run the focused tests to verify they pass**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.service.CatRecognitionServiceTest --tests io.github.arhor.catrecognizer.service.LatestRecognitionStateTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionService.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionState.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionResult.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/CatRecognitionServiceTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/LatestRecognitionStateTest.kt
git add -u app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/DetectionMode.kt \
           app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatDetector.kt \
           app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/CatDetectorFactory.kt \
           app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/service/StubCatDetector.kt \
           app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt \
           app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/CatRecognitionServiceTest.kt \
           app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt
git commit -m "refactor: remove detector modes from cat recognizer"
```

### Task 4: Simplify Web and Health Payloads Around Recognition State

**Files:**

- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionLatestResponse.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/WorkerSummary.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/health/WorkerReadinessCheck.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/health/FrameSourceHealthCheck.kt`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerDisabledTest.kt`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/DebugControllerFormattingTest.kt`
- Modify: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerIT.kt`

- [ ] **Step 1: Write the failing HTTP and health assertions for the trimmed payload**

```kotlin
@Test
fun `GET latest omits deleted worker and detector mode fields`() {
    given()
        .`when`().get("/api/recognition/latest")
        .then()
        .statusCode(200)
        .body("status", `is`("DETECTED"))
        .body("$", not(hasKey("detectorMode")))
        .body("worker", not(hasKey("enabled")))
        .body("worker", not(hasKey("running")))
}
```

```kotlin
@Test
fun `GET debug config returns simplified runtime summary`() {
    given()
        .`when`().get("/api/debug/config")
        .then()
        .statusCode(200)
        .body("pollInterval", `is`("5s"))
        .body("snapshotConfigured", `is`(true))
        .body("manualTriggerEnabled", `is`(true))
}
```

```kotlin
@Test
fun `health endpoint still exposes frame source and worker readiness checks`() {
    given()
        .`when`().get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("checks.name", hasItems("worker-readiness", "frame-source"))
}
```

- [ ] **Step 2: Run the web and health tests to verify they fail**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.web.RecognitionControllerTest --tests io.github.arhor.catrecognizer.web.RecognitionControllerDisabledTest --tests io.github.arhor.catrecognizer.health.HealthEndpointsTest --tests io.github.arhor.catrecognizer.web.DebugControllerFormattingTest`

Expected: FAIL because the current payloads and health checks still expose deleted worker and detector-mode concepts.

- [ ] **Step 3: Update DTOs, controllers, and health checks to match the simplified state model**

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/WorkerSummary.kt
@Serializable
data class WorkerSummary(
    val lastSuccessAt: Instant? = null,
    val consecutiveFailures: Int = 0,
    val lastErrorCode: String? = null,
)
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionLatestResponse.kt
@Serializable
data class RecognitionLatestResponse(
    val status: CatPresenceStatus? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant? = null,
    val confidence: Double? = null,
    val source: String? = null,
    val error: RecognitionError? = null,
    val worker: WorkerSummary = WorkerSummary(),
)
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt
@Serializable
data class RuntimeConfigSummary(
    val pollInterval: String,
    val snapshotConfigured: Boolean,
    val manualTriggerEnabled: Boolean,
)
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt
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
    )
}
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt
fun config(): RuntimeConfigSummary =
    RuntimeConfigSummary(
        pollInterval = config.worker().pollInterval().toFriendlyString(),
        snapshotConfigured = config.camera().snapshotUrl().isNotBlank(),
        manualTriggerEnabled = config.debug().manualTriggerEnabled(),
    )
```

```kotlin
// app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/health/WorkerReadinessCheck.kt
override fun call(): HealthCheckResponse {
    val snapshot = state.snapshot()
    val lastError = snapshot.lastError
    val lastSuccessAt = snapshot.lastSuccessAt

    if (lastError != null && snapshot.consecutiveFailures > 0) {
        return HealthCheckResponse.named(NAME)
            .down()
            .withData("state", "failing")
            .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
            .withData("errorCode", lastError.code)
            .build()
    }

    if (lastSuccessAt == null) {
        return HealthCheckResponse.named(NAME)
            .up()
            .withData("state", "warming-up")
            .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
            .build()
    }

    val isFresh = Duration.between(lastSuccessAt, Instant.now()) <= config.state().staleAfter()
    return if (isFresh) {
        HealthCheckResponse.named(NAME)
            .up()
            .withData("state", "fresh")
            .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
            .build()
    } else {
        HealthCheckResponse.named(NAME)
            .down()
            .withData("state", "stale")
            .withData("consecutiveFailures", snapshot.consecutiveFailures.toLong())
            .build()
    }
}
```

- [ ] **Step 4: Run the web and health tests to verify they pass**

Run:
`./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.web.RecognitionControllerTest --tests io.github.arhor.catrecognizer.web.RecognitionControllerDisabledTest --tests io.github.arhor.catrecognizer.health.HealthEndpointsTest --tests io.github.arhor.catrecognizer.web.DebugControllerFormattingTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RecognitionLatestResponse.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/WorkerSummary.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/health/WorkerReadinessCheck.kt \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/health/FrameSourceHealthCheck.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerDisabledTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/DebugControllerFormattingTest.kt \
        app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerIT.kt
git commit -m "refactor: simplify cat recognizer web and health payloads"
```

### Task 5: Move Tests Into the Refactored Package Structure and Run Full Verification

**Files:**

- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClientTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetectorTest.kt`
- Move: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/frame/SnapshotFrameClientTest.kt`
- Move: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetectorTest.kt`
- Delete: any remaining stale test files under `bootstrap/`, `detection/`, `frame/`, `recognition/`, or `state/` that no
  longer match production packages

- [ ] **Step 1: Write the failing moved-package tests with correct imports**

```kotlin
package io.github.arhor.catrecognizer.client

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SnapshotFrameClientTest {
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

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))
        val frame = frameClient.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), frame.bytes)
        assertEquals("image/jpeg", frame.contentType)
    }

    @Test
    fun `maps camera failures to frame source errors`() {
        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:1/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
    }

    @Test
    fun `maps non-2xx snapshot responses to retriable frame source errors`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/snapshot") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
            start()
        }

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
        assertTrue(error.message.contains("HTTP 404"))
    }

    private fun config(snapshotUrl: String): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun pollInterval() = Duration.ofSeconds(5)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = snapshotUrl
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
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

```kotlin
package io.github.arhor.catrecognizer.client

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.client.SnapshotFrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SnapshotFrameClientTest {
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

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))
        val frame = frameClient.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), frame.bytes)
        assertEquals("image/jpeg", frame.contentType)
    }

    @Test
    fun `maps camera failures to frame source errors`() {
        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:1/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
    }

    @Test
    fun `maps non-2xx snapshot responses to retriable frame source errors`() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/snapshot") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
            start()
        }

        val frameClient = SnapshotFrameClient(config("http://127.0.0.1:${server!!.address.port}/snapshot"))

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
        assertTrue(error.message.contains("HTTP 404"))
    }

    private fun config(snapshotUrl: String): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = object : RecognizerConfig.Worker {
                override fun pollInterval() = Duration.ofSeconds(5)
            }

            override fun camera() = object : RecognizerConfig.Camera {
                override fun snapshotUrl() = snapshotUrl
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
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

```kotlin
package io.github.arhor.catrecognizer.service

import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.FramePayload
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
    fun `returns placeholder unknown for decodable jpeg frame`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(jpegBytes(), "image/jpeg")),
        )
    }

    @Test
    fun `returns placeholder unknown for decodable png frame`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(pngBytes(), "image/png")),
        )
    }

    @Test
    fun `fails on invalid image bytes`() {
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
```

- [ ] **Step 2: Run the full JVM suite to verify package mismatches still fail**

Run: `./gradlew :app-cat-recognizer:test`

Expected: FAIL until all remaining stale package declarations, imports, and file locations are aligned with `client`,
`service`, and `web`.

- [ ] **Step 3: Move the tests, fix imports, and delete stale duplicates**

```kotlin
// app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClientTest.kt
package io.github.arhor.catrecognizer.client

import com.sun.net.httpserver.HttpServer
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.catrecognizer.domain.FramePayload
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
    fun `returns placeholder unknown for decodable frame`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(jpegBytes(), "image/jpeg")),
        )
    }

    @Test
    fun `returns placeholder unknown for decodable png frame`() {
        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(pngBytes(), "image/png")),
        )
    }

    @Test
    fun `fails on invalid image bytes`() {
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
```

- [ ] **Step 4: Run full verification and confirm all three commands pass**

Run: `./gradlew :app-cat-recognizer:test`
Expected: PASS

Run: `./gradlew :app-cat-recognizer:quarkusIntTest`
Expected: PASS

Run: `./gradlew :app-cat-recognizer:build`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/SnapshotFrameClientTest.kt \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/service/OpenCvCatDetectorTest.kt
git add -u app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer
git commit -m "test: align cat recognizer tests with simplified quarkus design"
```
