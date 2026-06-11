# Cat Recognizer Upload Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable debug UI flow for uploading a local image and running the existing cat detector against it.

**Architecture:** The existing Quarkus REST service gains a debug-gated multipart endpoint that converts uploaded bytes into a `FramePayload` and maps `OpenCvCatDetector` outcomes into `RecognitionResult` with `source = "upload"`. The static HTML page checks `/api/debug/config` before showing upload controls and renders upload results in a separate preview area so live camera state is unchanged.

**Tech Stack:** Quarkus 3.36 REST, Kotlin, RESTEasy Reactive multipart `FileUpload`, OpenCV detector, kotlinx.serialization, Rest Assured, plain HTML/CSS/JavaScript.

---

## File Structure

**Modified files:**

- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt` — add `uploadEnabled()` to debug config.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt` — expose safe upload flag to the UI.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt` — include `uploadEnabled`.
- `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt` — add gated multipart upload endpoint and detector-result mapping.
- `services/app-cat-recognizer/src/main/resources/application.properties` — default upload disabled, dev profile enabled.
- `services/app-cat-recognizer/src/main/resources/META-INF/resources/index.html` — add upload controls, local preview, upload result rendering, and bounding-box overlay.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt` — cover enabled upload and debug config flag.
- `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerDisabledTest.kt` — cover disabled upload.

No new production files are needed.

---

## Task 1: Add Upload Debug Configuration

**Files:**

- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/domain/RuntimeConfigSummary.kt`
- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/DebugController.kt`
- Modify: `services/app-cat-recognizer/src/main/resources/application.properties`
- Test: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`

- [ ] **Step 1: Write the failing debug config assertion**

In `RecognitionControllerTest`, update `GET debug config returns safe runtime summary`:

```kotlin
@Test
fun `GET debug config returns safe runtime summary`() {
    given()
        .get("/debug/config")
        .then()
        .statusCode(200)
        .body("pollInterval", `is`("500ms"))
        .body("snapshotConfigured", `is`(true))
        .body("manualTriggerEnabled", `is`(true))
        .body("uploadEnabled", `is`(true))
}
```

Also add this override to `RecognitionControllerTest.Profile`:

```kotlin
"cat-recognizer.debug.upload-enabled" to "true",
```

- [ ] **Step 2: Run the focused failing test**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionControllerTest.GET debug config returns safe runtime summary"
```

Expected: fail because `uploadEnabled` is absent or the config mapping does not yet define it.

- [ ] **Step 3: Add config mapping and response field**

In `RecognizerConfig.Debug`, change it to:

```kotlin
interface Debug {
    fun manualTriggerEnabled(): Boolean
    fun uploadEnabled(): Boolean
}
```

In `RuntimeConfigSummary`, change it to:

```kotlin
@Serializable
data class RuntimeConfigSummary(
    val pollInterval: String,
    val snapshotConfigured: Boolean,
    val manualTriggerEnabled: Boolean,
    val uploadEnabled: Boolean,
)
```

In `DebugController.config()`, change the response creation to:

```kotlin
RuntimeConfigSummary(
    pollInterval = config.worker().pollInterval().toFriendlyString(),
    snapshotConfigured = config.camera().snapshotUrl().isNotBlank(),
    manualTriggerEnabled = config.debug().manualTriggerEnabled(),
    uploadEnabled = config.debug().uploadEnabled(),
)
```

In `application.properties`, add:

```properties
cat-recognizer.debug.upload-enabled=false
%dev.cat-recognizer.debug.upload-enabled=true
```

- [ ] **Step 4: Run the focused passing test**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionControllerTest.GET debug config returns safe runtime summary"
```

Expected: pass.

---

## Task 2: Add Gated Upload Endpoint

**Files:**

- Modify: `services/app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/web/controller/RecognitionController.kt`
- Test: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`
- Test: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerDisabledTest.kt`

- [ ] **Step 1: Write the enabled upload test**

Add imports to `RecognitionControllerTest`:

```kotlin
import java.io.File
import java.nio.file.Files
```

Add this test to `RecognitionControllerTest`:

```kotlin
@Test
fun `POST upload returns recognition result when upload testing is enabled`() {
    val image = Files.createTempFile("cat-recognizer-upload", ".jpg").toFile()
    image.writeBytes("uploaded-frame".encodeToByteArray())

    try {
        given()
            .multiPart("image", image, "image/jpeg")
            .post("/recognition/upload")
            .then()
            .statusCode(200)
            .body("status", `is`("DETECTED"))
            .body("observedAt", not(nullValue()))
            .body("confidence", `is`(0.91f))
            .body("source", `is`("upload"))
            .body("error", nullValue())
            .body("boundingBoxes[0].x", `is`(10))
            .body("boundingBoxes[0].y", `is`(20))
            .body("boundingBoxes[0].width", `is`(80))
            .body("boundingBoxes[0].height", `is`(100))
    } finally {
        image.delete()
    }
}
```

- [ ] **Step 2: Write the disabled upload test**

In `RecognitionControllerDisabledTest`, add:

```kotlin
@Test
fun `POST upload returns forbidden when upload testing is disabled`() {
    given()
        .multiPart("image", "frame.jpg", "uploaded-frame".encodeToByteArray(), "image/jpeg")
        .post("/recognition/upload")
        .then()
        .statusCode(403)
}
```

In `RecognitionControllerDisabledTest.Profile`, add:

```kotlin
"cat-recognizer.debug.upload-enabled" to "false",
```

- [ ] **Step 3: Run the focused failing tests**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionControllerTest.POST upload returns recognition result when upload testing is enabled" --tests "io.github.arhor.catrecognizer.web.RecognitionControllerDisabledTest.POST upload returns forbidden when upload testing is disabled"
```

Expected: fail with HTTP 404 because `/recognition/upload` does not exist.

- [ ] **Step 4: Implement the upload endpoint**

Add imports to `RecognitionController`:

```kotlin
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.CatPresenceStatus
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import io.github.arhor.catrecognizer.domain.RecognitionError
import io.github.arhor.catrecognizer.domain.RecognitionResult
import io.github.arhor.catrecognizer.service.OpenCvCatDetector
import jakarta.ws.rs.Consumes
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files
import java.time.Instant
```

Inject `OpenCvCatDetector`:

```kotlin
class RecognitionController @Inject constructor(
    private val recognitionService: CatRecognitionService,
    private val state: LatestRecognitionState,
    private val config: RecognizerConfig,
    private val detector: OpenCvCatDetector,
)
```

Add this method and helper methods inside `RecognitionController`:

```kotlin
@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
fun upload(@RestForm("image") image: FileUpload?): Response {
    if (!config.debug().uploadEnabled()) {
        return Response.status(Response.Status.FORBIDDEN).build()
    }

    if (image == null || image.size() <= 0) {
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    val observedAt = Instant.now()
    val frame = FramePayload(
        bytes = Files.readAllBytes(image.uploadedFile()),
        contentType = image.contentType(),
        observedAt = observedAt,
    )

    return Response.ok(detectUpload(frame)).build()
}

private fun detectUpload(frame: FramePayload): RecognitionResult =
    try {
        when (val outcome = detector.detect(frame)) {
            is DetectionOutcome.Present -> RecognitionResult(
                status = CatPresenceStatus.DETECTED,
                observedAt = frame.observedAt,
                confidence = outcome.confidence,
                source = "upload",
                boundingBoxes = outcome.boundingBoxes.ifEmpty { null },
            )

            is DetectionOutcome.Absent -> RecognitionResult(
                status = CatPresenceStatus.NOT_DETECTED,
                observedAt = frame.observedAt,
                confidence = outcome.confidence,
                source = "upload",
            )

            is DetectionOutcome.Unknown -> RecognitionResult(
                status = CatPresenceStatus.UNKNOWN,
                observedAt = frame.observedAt,
                confidence = null,
                source = "upload",
                error = RecognitionError(
                    code = "DETECTOR_UNKNOWN",
                    message = outcome.reason,
                    retriable = false,
                ),
            )
        }
    } catch (error: Exception) {
        RecognitionResult(
            status = CatPresenceStatus.UNKNOWN,
            observedAt = frame.observedAt,
            confidence = null,
            source = "upload",
            error = RecognitionError(
                code = "DETECTOR_FAILED",
                message = error.message ?: "Detector execution failed",
                retriable = false,
            ),
        )
    }
```

- [ ] **Step 5: Run the focused passing tests**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionControllerTest.POST upload returns recognition result when upload testing is enabled" --tests "io.github.arhor.catrecognizer.web.RecognitionControllerDisabledTest.POST upload returns forbidden when upload testing is disabled"
```

Expected: pass.

---

## Task 3: Cover Detector Failures Through Upload

**Files:**

- Modify: `services/app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionControllerTest.kt`

- [ ] **Step 1: Write the invalid upload test**

Add this test to `RecognitionControllerTest`:

```kotlin
@Test
fun `POST upload returns unknown result when detector rejects image bytes`() {
    QuarkusMock.installMockForType(
        object : OpenCvCatDetector() {
            override fun detect(frame: FramePayload): DetectionOutcome =
                throw IllegalStateException("OpenCV failed to decode frame")
        },
        OpenCvCatDetector::class.java,
    )

    given()
        .multiPart("image", "invalid.jpg", "not-an-image".encodeToByteArray(), "image/jpeg")
        .post("/recognition/upload")
        .then()
        .statusCode(200)
        .body("status", `is`("UNKNOWN"))
        .body("source", `is`("upload"))
        .body("error.code", `is`("DETECTOR_FAILED"))
        .body("error.message", `is`("OpenCV failed to decode frame"))
        .body("error.retriable", `is`(false))
}
```

- [ ] **Step 2: Run the focused test**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test --tests "io.github.arhor.catrecognizer.web.RecognitionControllerTest.POST upload returns unknown result when detector rejects image bytes"
```

Expected: pass after Task 2 implementation.

---

## Task 4: Add Upload UI

**Files:**

- Modify: `services/app-cat-recognizer/src/main/resources/META-INF/resources/index.html`

- [ ] **Step 1: Add upload markup**

Below the live view `<div id="meta"></div>`, add:

```html
<section id="upload-section" hidden>
    <h2>Upload test image</h2>
    <form id="upload-form">
        <input id="upload-input" type="file" accept="image/*" required>
        <button id="upload-button" type="submit">Detect cat</button>
    </form>
    <div id="upload-status" class="unknown">Choose an image to test</div>
    <div id="upload-frame-container">
        <img id="upload-img" alt="Uploaded test image preview">
        <canvas id="upload-bbox-canvas"></canvas>
    </div>
</section>
```

- [ ] **Step 2: Add upload styles**

Extend the existing selectors so the upload preview shares live-view styling:

```css
#frame-container,
#upload-frame-container {
    position: relative;
    display: inline-block;
}
#frame-img,
#upload-img {
    display: block;
    max-width: 90vw;
}
#bbox-canvas,
#upload-bbox-canvas {
    position: absolute;
    top: 0;
    left: 0;
    pointer-events: none;
}
```

Add upload-specific styles:

```css
h2 {
    margin: 24px 0 8px;
    font-size: 1rem;
}
#upload-section {
    margin-top: 20px;
    width: min(90vw, 720px);
    text-align: center;
}
#upload-form {
    margin-bottom: 12px;
}
#upload-status {
    margin-bottom: 12px;
    padding: 6px 16px;
    border-radius: 4px;
    background: #333;
}
#upload-status.detected { background: #8b0000; }
#upload-status.absent   { background: #1a4a1a; }
#upload-status.unknown  { background: #333; }
#upload-img:not([src]) {
    display: none;
}
```

- [ ] **Step 3: Add upload JavaScript**

After the existing DOM constants, add:

```javascript
const uploadSection = document.getElementById('upload-section');
const uploadForm    = document.getElementById('upload-form');
const uploadInput   = document.getElementById('upload-input');
const uploadButton  = document.getElementById('upload-button');
const uploadImg     = document.getElementById('upload-img');
const uploadCanvas  = document.getElementById('upload-bbox-canvas');
const uploadCtx     = uploadCanvas.getContext('2d');
const uploadStatus  = document.getElementById('upload-status');

let uploadBoxes = [];
```

Add these helper functions:

```javascript
function drawBoxSet(targetImg, targetCanvas, targetCtx, boxes) {
    targetCanvas.width  = targetImg.naturalWidth;
    targetCanvas.height = targetImg.naturalHeight;
    targetCanvas.style.width  = targetImg.width  + 'px';
    targetCanvas.style.height = targetImg.height + 'px';

    targetCtx.clearRect(0, 0, targetCanvas.width, targetCanvas.height);
    targetCtx.strokeStyle = 'red';
    targetCtx.lineWidth   = 3;

    for (const box of boxes) {
        targetCtx.strokeRect(box.x, box.y, box.width, box.height);
    }
}

function renderStatus(target, data) {
    switch (data.status) {
        case 'DETECTED':
            target.textContent = 'Cat detected' +
                (data.confidence != null
                    ? ' (' + (data.confidence * 100).toFixed(0) + '%)'
                    : '');
            target.className = 'detected';
            break;
        case 'NOT_DETECTED':
            target.textContent = 'No cat detected';
            target.className = 'absent';
            break;
        default:
            target.textContent = data.error
                ? 'Unknown (' + data.error.code + ')'
                : 'Unknown';
            target.className = 'unknown';
    }
}
```

Change `drawBoxes()` to:

```javascript
function drawBoxes() {
    drawBoxSet(img, canvas, ctx, pendingBoxes);
}
```

In `poll()`, replace the `switch (data.status) { ... }` block with:

```javascript
renderStatus(statusBar, data);
```

Add upload behavior:

```javascript
async function loadDebugConfig() {
    const res = await fetch('/api/debug/config');
    if (!res.ok) return;

    const data = await res.json();
    uploadSection.hidden = !data.uploadEnabled;
}

uploadImg.addEventListener('load', () => {
    drawBoxSet(uploadImg, uploadCanvas, uploadCtx, uploadBoxes);
});

uploadInput.addEventListener('change', () => {
    uploadBoxes = [];
    uploadCtx.clearRect(0, 0, uploadCanvas.width, uploadCanvas.height);

    const file = uploadInput.files[0];
    if (!file) return;

    uploadImg.src = URL.createObjectURL(file);
    uploadStatus.textContent = 'Ready to test';
    uploadStatus.className = 'unknown';
});

uploadForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const file = uploadInput.files[0];
    if (!file) return;

    uploadButton.disabled = true;
    uploadStatus.textContent = 'Detecting...';
    uploadStatus.className = 'unknown';

    try {
        const body = new FormData();
        body.append('image', file);

        const res = await fetch('/api/recognition/upload', {
            method: 'POST',
            body,
        });

        if (!res.ok) {
            uploadStatus.textContent = 'Upload failed (' + res.status + ')';
            uploadStatus.className = 'unknown';
            return;
        }

        const data = await res.json();
        uploadBoxes = data.boundingBoxes || [];
        renderStatus(uploadStatus, data);
        drawBoxSet(uploadImg, uploadCanvas, uploadCtx, uploadBoxes);
    } catch (err) {
        uploadStatus.textContent = 'Upload failed';
        uploadStatus.className = 'unknown';
    } finally {
        uploadButton.disabled = false;
    }
});
```

Before `poll();`, add:

```javascript
loadDebugConfig();
```

- [ ] **Step 4: Run a smoke test build for resources and Kotlin**

Run:

```bash
cd services && ./gradlew :app-cat-recognizer:test
```

Expected: all JVM tests pass.

---

## Final Verification

- [ ] Run the full JVM test suite:

```bash
cd services && ./gradlew :app-cat-recognizer:test
```

Expected: all tests pass.

- [ ] Optionally run the dev server and manually verify:

```bash
cd services && ./gradlew :app-cat-recognizer:quarkusDev
```

Open the static UI, confirm upload controls are visible in dev, upload a local image, and confirm the result status plus red bounding boxes render in the upload preview.

Do not create a git commit unless the user explicitly asks for one.
