# Cat Recognizer OpenCV Detector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an `OPENCV` detector mode that decodes JPEG/PNG snapshot bytes into an OpenCV `Mat`, runs a tiny deterministic placeholder operation, returns `UNKNOWN` on success, and preserves the existing detector seam plus failure mapping behavior.

**Architecture:** Keep `CatDetector` as the only recognition seam. Add a dedicated `OpenCvCatDetector` plus a small CDI producer that selects the active detector from config, leaving `RecognitionOrchestrator` unchanged. Prove the behavior in JVM tests and use the existing orchestrator exception mapping for detector failures.

**Tech Stack:** Kotlin, Quarkus 3, CDI/Arc, Quarkiverse `quarkus-opencv`, JUnit 5, QuarkusTest, Gradle

---

## File Structure

### Files To Modify

- `services/app-cat-recognizer/build.gradle.kts`
  Adds the `quarkus-opencv` dependency.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt`
  Adds the `OPENCV` enum value.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt`
  Narrows the stub detector so it only handles the three existing non-OpenCV modes.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`
  Keeps coverage for `STUB`, `ALWAYS_PRESENT`, and `ALWAYS_ABSENT` after the mode expansion.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`
  Proves default config remains `STUB` and that `OPENCV` binds when overridden.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`
  Adds an `OPENCV`-mode failure-mapping test without coupling the orchestrator test to OpenCV internals.

### Files To Create

- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetector.kt`
  Decodes bytes into a `Mat`, runs the grayscale placeholder, and returns `DetectionOutcome.Unknown("opencv placeholder detector")`.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetectorProducer.kt`
  Selects the active detector bean for the configured mode and exposes a single injected `CatDetector`.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetectorTest.kt`
  JVM tests for JPEG decode, PNG decode, deterministic placeholder execution, and invalid-image failure handling.

## Tasks

### Task 1: Add The OpenCV Dependency And Mode Coverage

**Files:**
- Modify: `services/app-cat-recognizer/build.gradle.kts`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`

- [ ] **Step 1: Write the failing config-binding test for `OPENCV`**

```kotlin
@Test
fun `binds opencv detection mode when overridden`() {
    assertEquals(DetectionMode.OPENCV, config.detection().mode())
}

class OpenCvProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "cat-recognizer.detection.mode" to "OPENCV",
    )
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest
```

Expected: FAIL because `DetectionMode.OPENCV` does not exist yet.

- [ ] **Step 3: Add the enum value and the dependency**

```kotlin
enum class DetectionMode {
    STUB,
    ALWAYS_PRESENT,
    ALWAYS_ABSENT,
    OPENCV,
}
```

```kotlin
dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform))

    implementation("io.quarkiverse.opencv:quarkus-opencv")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-container-image-docker")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-smallrye-health")
}
```

- [ ] **Step 4: Update the config-binding test class for both default and override coverage**

```kotlin
@QuarkusTest
class RecognizerConfigBindingTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds defaults from application properties`() {
        assertEquals(DetectionMode.STUB, config.detection().mode())
    }
}

@QuarkusTest
@TestProfile(RecognizerConfigBindingOpenCvTest.Profile::class)
class RecognizerConfigBindingOpenCvTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds opencv detection mode when overridden`() {
        assertEquals(DetectionMode.OPENCV, config.detection().mode())
    }

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf(
            "cat-recognizer.detection.mode" to "OPENCV",
        )
    }
}
```

- [ ] **Step 5: Run the targeted config test to verify it passes**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingOpenCvTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git -C /home/arhor/Projects/hive add \
  services/app-cat-recognizer/build.gradle.kts \
  services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/DetectionMode.kt \
  services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt
git -C /home/arhor/Projects/hive commit -m "feat: add opencv detection mode"
```

### Task 2: Split Detector Responsibilities And Add CDI Selection

**Files:**
- Create: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetectorProducer.kt`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt`
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`

- [ ] **Step 1: Write the failing stub-detector test that proves legacy modes still work after the new enum arrives**

```kotlin
@Test
fun `stub detector still reports unknown in stub mode`() {
    val detector = StubCatDetector(config(DetectionMode.STUB))

    assertEquals(
        DetectionOutcome.Unknown(reason = "stub detector"),
        detector.detect(sampleFrame),
    )
}
```

Add a second assertion that `ALWAYS_PRESENT` and `ALWAYS_ABSENT` still behave as before.

- [ ] **Step 2: Run the stub-detector test to verify the current implementation is now incomplete**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.detection.StubCatDetectorTest
```

Expected: FAIL once `DetectionMode.OPENCV` exists and the `when` expression in `StubCatDetector` is no longer exhaustive.

- [ ] **Step 3: Narrow `StubCatDetector` to legacy modes only**

```kotlin
@ApplicationScoped
class StubCatDetector @Inject constructor(
    private val config: RecognizerConfig,
) {

    fun detect(frame: FramePayload): DetectionOutcome =
        when (config.detection().mode()) {
            DetectionMode.ALWAYS_PRESENT -> DetectionOutcome.Present(confidence = 1.0)
            DetectionMode.ALWAYS_ABSENT -> DetectionOutcome.Absent(confidence = 1.0)
            DetectionMode.STUB -> DetectionOutcome.Unknown(reason = "stub detector")
            DetectionMode.OPENCV -> error("StubCatDetector does not support OPENCV mode")
        }
}
```

- [ ] **Step 4: Add the producer that exposes the active `CatDetector` seam**

```kotlin
@ApplicationScoped
class CatDetectorProducer @Inject constructor(
    private val config: RecognizerConfig,
    private val stubCatDetector: StubCatDetector,
    private val openCvCatDetector: OpenCvCatDetector,
) {

    @Produces
    fun catDetector(): CatDetector =
        when (config.detection().mode()) {
            DetectionMode.STUB,
            DetectionMode.ALWAYS_PRESENT,
            DetectionMode.ALWAYS_ABSENT,
                -> CatDetector { frame -> stubCatDetector.detect(frame) }

            DetectionMode.OPENCV ->
                CatDetector { frame -> openCvCatDetector.detect(frame) }
        }
}
```

- [ ] **Step 5: Re-run the stub-detector test to verify legacy mode coverage still passes**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.detection.StubCatDetectorTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git -C /home/arhor/Projects/hive add \
  services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/CatDetectorProducer.kt \
  services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetector.kt \
  services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt
git -C /home/arhor/Projects/hive commit -m "refactor: select detectors by mode"
```

### Task 3: Implement The OpenCV Detector With JVM-Focused TDD

**Files:**
- Create: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetector.kt`
- Create: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetectorTest.kt`

- [ ] **Step 1: Write the failing JPEG/PNG decode tests and the invalid-bytes failure test**

```kotlin
class OpenCvCatDetectorTest {

    @Test
    fun `decodes jpeg bytes and returns placeholder unknown`() {
        val detector = OpenCvCatDetector()

        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(jpegBytes(), "image/jpeg")),
        )
    }

    @Test
    fun `decodes png bytes and returns placeholder unknown`() {
        val detector = OpenCvCatDetector()

        assertEquals(
            DetectionOutcome.Unknown(reason = "opencv placeholder detector"),
            detector.detect(frame(pngBytes(), "image/png")),
        )
    }

    @Test
    fun `rejects invalid image bytes with a safe message`() {
        val detector = OpenCvCatDetector()
        val error = assertFailsWith<IllegalStateException> {
            detector.detect(frame("not-an-image".encodeToByteArray(), "image/jpeg"))
        }

        assertEquals("OpenCV failed to decode frame", error.message)
    }
}
```

Use in-test helpers that generate tiny PNG and JPEG byte arrays with `BufferedImage` plus `ImageIO.write(...)`.

- [ ] **Step 2: Run the detector test to verify it fails**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.detection.OpenCvCatDetectorTest
```

Expected: FAIL because `OpenCvCatDetector` does not exist yet.

- [ ] **Step 3: Implement the minimal detector**

```kotlin
@ApplicationScoped
class OpenCvCatDetector {

    fun detect(frame: FramePayload): DetectionOutcome {
        val input = MatOfByte(*frame.bytes.toTypedArray())
        val decoded = Imgcodecs.imdecode(input, Imgcodecs.IMREAD_COLOR)

        if (decoded.empty()) {
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

- [ ] **Step 4: Tighten the implementation so byte conversion stays explicit and cheap**

Replace the vararg constructor if needed with a helper that fills a `MatOfByte` directly from the `ByteArray`. Keep the final implementation readable and deterministic.

```kotlin
private fun byteBuffer(bytes: ByteArray): MatOfByte =
    MatOfByte().apply { fromArray(*bytes.toTypedArray()) }
```

If `fromArray(*bytes.toTypedArray())` is too noisy or allocates more than needed, use the simplest OpenCV Java API available in this version while keeping the tests unchanged.

- [ ] **Step 5: Re-run the detector test to verify JPEG, PNG, and invalid bytes all pass**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.detection.OpenCvCatDetectorTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git -C /home/arhor/Projects/hive add \
  services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetector.kt \
  services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/OpenCvCatDetectorTest.kt
git -C /home/arhor/Projects/hive commit -m "feat: add opencv detector placeholder"
```

### Task 4: Prove Existing Recognition Failure Mapping Still Holds

**Files:**
- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`

- [ ] **Step 1: Write the failing orchestrator regression test for `OPENCV` mode**

```kotlin
@Test
fun `maps opencv detector failure through existing detector failed behavior`() {
    val state = LatestRecognitionState()
    val orchestrator = orchestrator(
        state = state,
        detectorMode = DetectionMode.OPENCV,
        detector = CatDetector { error("OpenCV failed to decode frame") },
    )

    val result = orchestrator.runRecognition()
    val snapshot = state.snapshot()

    assertEquals(CatPresenceStatus.UNKNOWN, result.status)
    assertEquals("opencv", result.detectorMode)
    assertEquals("DETECTOR_FAILED", result.error?.code)
    assertEquals("OpenCV failed to decode frame", result.error?.message)
    assertEquals(true, result.error?.retriable)
    assertEquals(1, snapshot.consecutiveFailures)
}
```

- [ ] **Step 2: Run the orchestrator test to verify it fails for the right reason**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.recognition.RecognitionOrchestratorTest
```

Expected: FAIL before the new enum and config path are fully wired through the tests.

- [ ] **Step 3: Make the smallest test/support changes needed**

Keep `RecognitionOrchestrator` production code unchanged unless the new mode exposes a real bug. The intended steady state is still:

```kotlin
private fun detectorMode(): String =
    config.detection().mode().name.lowercase(Locale.ROOT)
```

If any test helpers hardcode legacy modes, update only those helpers.

- [ ] **Step 4: Re-run the orchestrator test to verify the failure mapping passes unchanged**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.recognition.RecognitionOrchestratorTest
```

Expected: PASS, with no production changes to `RecognitionOrchestrator` unless a real regression is discovered.

- [ ] **Step 5: Commit**

```bash
git -C /home/arhor/Projects/hive add \
  services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt
git -C /home/arhor/Projects/hive commit -m "test: cover opencv detector failure mapping"
```

### Task 5: Run Full Verification And Clean Up Any Integration Breakage

**Files:**
- Review: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceTest.kt`
- Review if needed: `services/app-cat-recognizer/src/main/resources/application.properties`

- [ ] **Step 1: Run the full JVM test suite**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test
```

Expected: PASS.

- [ ] **Step 2: Run the full module build to prove packaging compatibility still holds**

Run:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:build
```

Expected: PASS, including the current native-oriented build path.

- [ ] **Step 3: Fix only integration breakage revealed by the full run**

Likely hotspots if something fails:

- Quarkus bean ambiguity around `CatDetector`
- OpenCV native library loading in JVM tests
- native packaging configuration triggered by the new dependency
- Quarkus test mocks that assume `StubCatDetector` is the only detector bean

Keep fixes narrow. Do not broaden the feature into real classification or API changes.

- [ ] **Step 4: Re-run the exact failing command until it passes cleanly**

Run whichever command failed in Step 1 or Step 2, then re-run both:

```bash
cd /home/arhor/Projects/hive/services
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

Expected: PASS on both commands.

- [ ] **Step 5: Commit the final integration adjustments**

```bash
git -C /home/arhor/Projects/hive add services/app-cat-recognizer
git -C /home/arhor/Projects/hive commit -m "fix: integrate opencv detector mode"
```

## Self-Review

- Spec coverage:
  `quarkus-opencv` dependency is covered in Task 1.
  `DetectionMode.OPENCV` plus clean detector selection is covered in Tasks 1 and 2.
  JPEG/PNG decode plus deterministic grayscale placeholder is covered in Task 3.
  unchanged orchestrator failure mapping is covered in Task 4.
  JVM and build verification are covered in Task 5.
- Placeholder scan:
  No `TODO`, `TBD`, or “implement later” markers remain in the task steps.
  Each code-changing task includes explicit code or an explicit constraint on what must stay unchanged.
- Type consistency:
  `DetectionMode.OPENCV`, `OpenCvCatDetector`, `CatDetectorProducer`, and `DetectionOutcome.Unknown("opencv placeholder detector")` are named consistently across tasks.

## Notes

- Keep the default `cat-recognizer.detection.mode=STUB` in `application.properties` for this change.
- Do not add HTTP endpoints, debug payload changes, or new config trees unless verification exposes a real blocker.
- If `RecognitionResourceTest` needs bean-mocking updates because `CatDetectorProducer` becomes the injected seam, make the smallest change that preserves the current endpoint assertions.
