# Cat Recognizer Native API Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make ESPHome native API the default camera source while keeping HTTP snapshot available only when explicitly
configured.

**Architecture:** Keep the existing `FrameClient` abstraction and `FrameClientProducer` source switch. The
implementation only changes defaults and adds tests around explicit source selection; it does not introduce fallback
behavior or remove the HTTP client.

**Tech Stack:** Kotlin, Quarkus config mappings, CDI producers/qualifiers, JUnit 5/kotlin-test, Gradle.

---

## File Structure

- Modify `services/app-cat-recognizer/src/main/resources/application.properties`: change the default camera source to
  `NATIVE_API`, leaving HTTP snapshot properties available.
- Modify
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`:
  expect native API as the default source while retaining assertions for HTTP snapshot settings.
- Create `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/FrameClientProducerTest.kt`:
  verify exact source-based client selection with no fallback.
- Optionally modify
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`: if
  diagnostics are updated to expose the camera source, assert the new field.

### Task 1: Pin The Native API Default

**Files:**

- Modify:
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`
- Modify: `services/app-cat-recognizer/src/main/resources/application.properties`

- [ ] **Step 1: Write the failing config binding assertion**

Change the default source assertion in `RecognizerConfigBindingTest`:

```kotlin
assertEquals(RecognizerConfig.CameraSource.NATIVE_API, config.camera().source())
```

Keep these assertions in the same test because HTTP remains configurable:

```kotlin
assertEquals("http://esp32-cam.local/snapshot", config.camera().snapshotUrl())
assertEquals("esp32-cam.local", config.camera().nativeApi().host())
assertEquals(6053, config.camera().nativeApi().port())
assertEquals(Duration.ofSeconds(2), config.camera().nativeApi().connectTimeout())
assertEquals(Duration.ofSeconds(5), config.camera().nativeApi().readTimeout())
assertFalse(config.camera().nativeApi().encryption().enabled())
assertFalse(config.camera().nativeApi().encryption().key().isPresent)
```

- [ ] **Step 2: Run the focused test to verify RED**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest
```

Expected: FAIL because `application.properties` still binds `cat-recognizer.camera.source=HTTP_SNAPSHOT`.

- [ ] **Step 3: Change the default source**

In `application.properties`, change:

```properties
cat-recognizer.camera.source=HTTP_SNAPSHOT
```

to:

```properties
cat-recognizer.camera.source=NATIVE_API
```

Do not remove these HTTP settings:

```properties
cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.camera.connect-timeout=2S
cat-recognizer.camera.read-timeout=5S
```

- [ ] **Step 4: Run the focused test to verify GREEN**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.config.RecognizerConfigBindingTest
```

Expected: PASS.

### Task 2: Prove Explicit Client Selection

**Files:**

- Create: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/FrameClientProducerTest.kt`
- Read: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/FrameClientProducer.kt`

- [ ] **Step 1: Write failing producer tests**

Create `FrameClientProducerTest.kt`:

```kotlin
package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.impl.FrameClientProducer
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.client.model.FramePayload
import java.time.Duration
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertSame

class FrameClientProducerTest {

    private val httpClient = FrameClient {
        FramePayload(byteArrayOf(1), "image/jpeg", Instant.EPOCH)
    }
    private val nativeClient = FrameClient {
        FramePayload(byteArrayOf(2), "image/jpeg", Instant.EPOCH)
    }

    @Test
    fun `selects native client when source is native api`() {
        val producer = FrameClientProducer(config(RecognizerConfig.CameraSource.NATIVE_API), httpClient, nativeClient)

        assertSame(nativeClient, producer.frameClient())
    }

    @Test
    fun `selects http client when source is http snapshot`() {
        val producer =
            FrameClientProducer(config(RecognizerConfig.CameraSource.HTTP_SNAPSHOT), httpClient, nativeClient)

        assertSame(httpClient, producer.frameClient())
    }

    private fun config(source: RecognizerConfig.CameraSource): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = error("not used")
            override fun camera() = object : RecognizerConfig.Camera {
                override fun source() = source
                override fun snapshotUrl() = "http://example.test/snapshot"
                override fun connectTimeout() = Duration.ofSeconds(2)
                override fun readTimeout() = Duration.ofSeconds(5)
                override fun nativeApi() = object : RecognizerConfig.NativeApi {
                    override fun host() = "esp32-cam.local"
                    override fun port() = 6053
                    override fun connectTimeout() = Duration.ofSeconds(2)
                    override fun readTimeout() = Duration.ofSeconds(5)
                    override fun encryption() = object : RecognizerConfig.Encryption {
                        override fun enabled() = false
                        override fun key(): Optional<String> = Optional.empty()
                    }
                }
            }
            override fun state() = error("not used")
            override fun debug() = error("not used")
            override fun detector() = error("not used")
        }
}
```

- [ ] **Step 2: Run the producer test to verify RED or compile issue**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.client.FrameClientProducerTest
```

Expected: either PASS immediately because `FrameClientProducer` already has the desired behavior, or a compile/access
failure if the constructor cannot be called from the test package. If it passes immediately, keep the test as
characterization coverage for the existing desired behavior.

- [ ] **Step 3: Make the minimal implementation change if needed**

If the test cannot access `FrameClientProducer`, move the test package to `io.github.arhor.catrecognizer.client.impl`
and update the package declaration:

```kotlin
package io.github.arhor.catrecognizer.client.impl
```

Keep imports for `FrameClient`, `FramePayload`, and `RecognizerConfig`.

- [ ] **Step 4: Run the producer test to verify GREEN**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.client.impl.FrameClientProducerTest
```

Expected: PASS.

### Task 3: Optional Diagnostics Check

**Files:**

- Read: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt`
- Read: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt`
- Modify only if needed:
  `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`

- [ ] **Step 1: Decide whether diagnostics need code changes**

Read `DebugController.config()`. If it only reports `snapshotConfigured`, it is still acceptable under the approved
spec. Do not add a new API field unless the user asks or existing tests require it.

- [ ] **Step 2: If no diagnostics change is needed, run existing debug coverage**

Run:

```bash
./gradlew :app-cat-recognizer:test --tests io.github.arhor.catrecognizer.web.RecognitionControllerTest
```

Expected: PASS after the default-source update.

### Task 4: Full Verification

**Files:**

- No additional edits expected.

- [ ] **Step 1: Run the app test suite**

Run:

```bash
./gradlew :app-cat-recognizer:test
```

Expected: PASS.

- [ ] **Step 2: Inspect final diff**

Run:

```bash
git diff -- services/app-cat-recognizer/src/main/resources/application.properties services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client
```

Expected: diff shows only the native default config change and focused tests. Existing unrelated user changes in
`esphome/config/esp32-cam.yaml` and `WorkerReadinessCheck.kt` must remain untouched.
