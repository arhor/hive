# Cat Recognizer Service Design

**Date:** 2026-06-05

## Goal

Design the initial architecture for `services/app-cat-recognizer` as a Quarkus/Kotlin service that polls an ESP32-CAM near a cat feeder and determines whether a cat is present.

The first implementation goal is intentionally narrow:

- `DETECTED`
- `NOT_DETECTED`
- `UNKNOWN`

This design focuses on a minimal, testable service shape that works as a background worker, exposes limited HTTP endpoints for operations and debugging, reuses Quarkus health support, and remains easy to extend later with real ML-based recognition and home automation integrations.

## Scope

This design covers:

- The recommended application shape
- Package structure under `src/main/kotlin`
- Core components and their responsibilities
- Configuration model
- Recognition data model
- Worker lifecycle and polling behavior
- HTTP/debug API shape
- Testing strategy
- Native-image constraints
- Future extension points

This design does not cover:

- Real cat detection implementation
- OpenCV or ONNX integration
- MQTT or Home Assistant publishing
- Database storage
- Image or frame persistence
- Motion sensors or stream-based ingestion

## Constraints

- The service is a Quarkus + Kotlin application
- The module lives in `services/app-cat-recognizer`
- The service should behave mainly as a background worker
- HTTP support is for health, debug, and manual triggering, not as the primary purpose
- Existing Quarkus health support should be reused
- The first version should be snapshot-first, not stream-first
- The architecture should preserve a generic frame-source abstraction
- The design should stay boring, testable, and native-image-friendly

## Recommended Application Shape

The service should be a **hybrid worker with a small HTTP surface**.

Shape:

- A background worker periodically polls a snapshot endpoint from the ESP32-CAM
- Recognition runs in the background without an external trigger
- A lightweight REST API exposes operational and debug information
- Quarkus health endpoints report liveness and readiness using existing support

Why this shape fits:

- The core problem is continuous observation, not request-driven processing
- A worker-first design matches the intended autonomous behavior near the feeder
- A small HTTP layer makes debugging and operations practical without introducing MQTT or Home Assistant dependencies yet
- Quarkus already provides the needed HTTP and health infrastructure with little complexity cost
- This shape remains straightforward to compile as a native image

Alternatives considered:

1. Pure HTTP-triggered service: rejected because it would force scheduling and orchestration into external callers too early.
2. Full event-pipeline architecture: rejected because it would add abstractions for future integrations before the first useful service behavior exists.

## Snapshot-First Ingestion Strategy

The first version should treat **snapshot polling** as the only real frame acquisition mechanism.

Design principle:

- Expose a generic `FrameSource` abstraction
- Implement only `SnapshotFrameSource` initially
- Defer stream ingestion until a later version proves it is needed

Rationale:

- Snapshot polling is simpler to reason about operationally and easier to test
- It avoids early investment in stream decoding, buffering, and long-lived connection handling
- It provides a stable seam for later adding MJPEG or RTSP-based implementations behind the same interface

Stream ingestion should be considered a later extension, not a co-equal design target for the first implementation.

## Package Structure

Recommended structure under `services/app-cat-recognizer/src/main/kotlin`:

```text
io/github/arhor/catrecognizer/
  bootstrap/
    WorkerLifecycle.kt
  config/
    RecognizerConfig.kt
  detection/
    CatDetector.kt
    DetectionMode.kt
    model/
      DetectionOutcome.kt
  frame/
    FrameSource.kt
    SnapshotFrameSource.kt
    model/
      FramePayload.kt
      FrameSourceError.kt
  recognition/
    RecognitionOrchestrator.kt
    model/
      CatPresenceStatus.kt
      RecognitionError.kt
      RecognitionResult.kt
  state/
    LatestRecognitionState.kt
  web/
    RecognitionResource.kt
    DebugResource.kt
  health/
    FrameSourceHealthCheck.kt
    WorkerReadinessCheck.kt
```

Structure principles:

- Organize by responsibility, not by framework stereotype
- Keep transport concerns (`web`, `health`) thin
- Keep the service core centered in `frame`, `detection`, `recognition`, and `state`
- Keep everything in the existing Gradle module for now

No additional module split is warranted at this stage.

## Core Components

### Frame Source

`FrameSource` is the abstraction that returns a frame on demand.

Responsibilities:

- Fetch one frame from the configured source
- Attach capture or fetch timestamp metadata
- Normalize source-specific failures into domain-level errors

Initial implementation:

- `SnapshotFrameSource`

Future implementations:

- MJPEG source
- RTSP-backed source
- Motion-triggered source

The rest of the service should depend only on `FrameSource`, not on camera transport details.

### Detection Abstraction

`CatDetector` is the abstraction that classifies a frame.

Responsibilities:

- Accept a `FramePayload`
- Return a detector-level outcome describing cat presence or inability to classify

Initial implementation options:

- `stub`
- `always_present`
- `always_absent`

These implementations are intentionally simple and useful for validating orchestration, API behavior, and deployment without adding ML dependencies.

Future implementations:

- OpenCV-backed detector
- ONNX-backed detector
- Composite or fallback detector

### Recognition Orchestration

`RecognitionOrchestrator` coordinates a single recognition cycle.

Responsibilities:

1. Request a frame from `FrameSource`
2. Invoke `CatDetector`
3. Convert the result into the external `RecognitionResult` model
4. Update in-memory latest state
5. Return the result to callers such as the worker or manual-trigger endpoint

This component is the main application service. It should contain the workflow, but not scheduling logic or REST-specific concerns.

### In-Memory Latest State

`LatestRecognitionState` stores the latest known result and worker metadata in memory.

Responsibilities:

- Store the latest recognition result
- Track last successful recognition time
- Track last failure information
- Track worker running state
- Track consecutive failure count

Why this is sufficient now:

- The first version only needs operational visibility
- No persistence or history is required yet
- It supports readiness checks and HTTP debug endpoints cleanly

### HTTP and Debug API

REST resources should remain minimal wrappers around orchestration and state.

`RecognitionResource` responsibilities:

- Return the latest recognition state
- Allow a one-shot manual recognition trigger

`DebugResource` responsibilities:

- Return safe runtime configuration and worker summary information
- Help diagnose whether the worker and frame source are configured as expected

The HTTP layer should not own recognition logic.

### Health and Readiness

Quarkus SmallRye Health should be reused instead of inventing custom health mechanisms.

Suggested checks:

- `WorkerReadinessCheck`
- `FrameSourceHealthCheck`

Responsibilities:

- Reflect whether the service is ready to perform recognition work
- Surface degraded status if the worker is enabled but repeatedly failing
- Avoid performing a fresh camera call on every health request

Health checks should rely on cached worker state rather than trigger live recognition activity.

### Configuration

`RecognizerConfig` should provide typed configuration access for the application.

Responsibilities:

- Centralize configuration keys and defaults
- Avoid scattered raw property reads
- Make testing simpler by providing one configuration seam

## Configuration Keys

Recommended keys:

```properties
cat-recognizer.worker.enabled=true
cat-recognizer.worker.poll-interval=5s
cat-recognizer.worker.initial-delay=1s
cat-recognizer.worker.failure-backoff=30s

cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.camera.connect-timeout=2s
cat-recognizer.camera.read-timeout=5s

cat-recognizer.detection.mode=stub
cat-recognizer.detection.unknown-on-error=true

cat-recognizer.state.stale-after=30s
cat-recognizer.debug.manual-trigger-enabled=true
```

Meaning of the essential keys:

- `cat-recognizer.camera.snapshot-url`: URL used by the snapshot frame source
- `cat-recognizer.worker.enabled`: enables or disables the background polling worker
- `cat-recognizer.worker.poll-interval`: normal success-path polling cadence
- `cat-recognizer.worker.initial-delay`: initial startup delay before the first poll
- `cat-recognizer.worker.failure-backoff`: wait period after a failed cycle
- `cat-recognizer.detection.mode`: selects the detector implementation
- `cat-recognizer.state.stale-after`: defines when the latest state should be treated as stale for readiness/debug purposes
- `cat-recognizer.debug.manual-trigger-enabled`: allows manual recognition through HTTP

Non-goals for configuration right now:

- Multi-camera support
- Dynamic detector loading
- Persisted historical retention settings

## Data Model

### Cat Presence Status

```kotlin
enum class CatPresenceStatus {
    DETECTED,
    NOT_DETECTED,
    UNKNOWN
}
```

This is the public result state for the first version.

### Recognition Result

Recommended shape:

```kotlin
data class RecognitionResult(
    val status: CatPresenceStatus,
    val observedAt: Instant,
    val confidence: Double?,
    val detectorMode: String,
    val source: String,
    val error: RecognitionError? = null
)
```

Field guidance:

- `status`: the current domain classification
- `observedAt`: when the frame was captured or fetched
- `confidence`: nullable because the initial detector may not provide a real score
- `detectorMode`: records which detector implementation produced the result
- `source`: identifies the input mechanism, initially `"snapshot"`
- `error`: provides structured context when the status is `UNKNOWN` due to failure

### Error Representation

Recommended shape:

```kotlin
data class RecognitionError(
    val code: String,
    val message: String,
    val retriable: Boolean
)
```

Suggested initial error codes:

- `FRAME_FETCH_FAILED`
- `DETECTOR_FAILED`
- `INVALID_CONFIGURATION`
- `WORKER_DISABLED`

Important distinction:

- `UNKNOWN` is the recognition outcome
- `error` explains the operational reason behind that outcome when applicable

This avoids conflating domain state with transport or detector failures.

## Worker Lifecycle

### Startup Behavior

- Validate required configuration during startup
- If the worker is disabled, do not begin polling
- If the worker is enabled, schedule polling after the configured initial delay
- Do not block full application startup on achieving the first successful recognition

This keeps service startup predictable and operationally safe.

### Shutdown Behavior

- Stop scheduling new polling cycles
- Allow an in-flight cycle to finish cleanly or be cancelled safely
- Mark worker state as stopping or stopped

Shutdown should be quiet and deterministic.

### Error Handling

Recognition failures should not crash the process.

On failure:

- Record a `RecognitionResult` with `status=UNKNOWN`
- Attach structured error information
- Increment consecutive failure count
- Preserve enough metadata for readiness and debugging

The service should degrade visibly rather than terminate.

### Polling and Backoff Strategy

The first version should use a simple fixed-interval strategy.

Behavior:

- On success, wait `poll-interval`
- On failure, wait `failure-backoff`
- Reset consecutive failure count after a success

Why fixed backoff is recommended:

- It is simple to understand and test
- It is sufficient for a single camera source
- It avoids premature scheduling complexity

Exponential backoff can be added later if repeated failure behavior proves problematic.

## API Design

The HTTP surface should stay small and operationally focused.

### `GET /api/recognition/latest`

Purpose:

- Return the latest known recognition result and worker summary

Example response:

```json
{
  "status": "NOT_DETECTED",
  "observedAt": "2026-06-05T12:00:15Z",
  "confidence": null,
  "detectorMode": "stub",
  "source": "snapshot",
  "error": null,
  "worker": {
    "enabled": true,
    "running": true,
    "lastSuccessAt": "2026-06-05T12:00:15Z",
    "consecutiveFailures": 0
  }
}
```

### `POST /api/recognition/run`

Purpose:

- Trigger one recognition cycle manually for debugging or validation

Notes:

- This endpoint should be optional behind `cat-recognizer.debug.manual-trigger-enabled`
- It should reuse the same `RecognitionOrchestrator` path as the background worker

Example response:

```json
{
  "status": "UNKNOWN",
  "observedAt": "2026-06-05T12:01:02Z",
  "confidence": null,
  "detectorMode": "stub",
  "source": "snapshot",
  "error": {
    "code": "FRAME_FETCH_FAILED",
    "message": "Timed out fetching snapshot",
    "retriable": true
  }
}
```

### `GET /api/debug/config`

Purpose:

- Return a safe summary of runtime configuration relevant to operations

Example response:

```json
{
  "workerEnabled": true,
  "pollInterval": "5s",
  "failureBackoff": "30s",
  "detectionMode": "stub",
  "snapshotConfigured": true
}
```

This endpoint should expose operationally useful information while avoiding secrets or internal-only details.

### Health Endpoints

Reuse Quarkus defaults:

- `/q/health`
- `/q/health/live`
- `/q/health/ready`

No service-specific health endpoint is needed for the first version.

## Testing Strategy

The design should favor unit testing first, with a small number of integration tests.

### Unit Tests

Focus on:

- `RecognitionOrchestrator`
- `LatestRecognitionState`
- Worker lifecycle and polling logic
- Configuration mapping validation where useful

Verify:

- Correct mapping from detector outputs into public recognition results
- Failure conversion into `UNKNOWN` with structured error information
- Consecutive failure tracking and reset behavior
- Worker enabled and disabled behavior
- Polling and backoff decisions

Use mocks or fakes for:

- `FrameSource`
- `CatDetector`
- clock or time source if timestamps need deterministic assertions

### Integration Tests

Add a small set of Quarkus integration tests for:

- `GET /api/recognition/latest`
- `POST /api/recognition/run`
- health endpoint behavior under healthy and degraded state

Guidance:

- Replace real camera access with fake beans or test doubles
- Avoid any dependency on a live ESP32-CAM in automated tests

### Native Tests

Keep native testing limited to smoke coverage initially.

A single native/integration test is sufficient once the starter scaffold endpoints are replaced:

- application boots
- health endpoint responds
- latest-recognition endpoint responds

## Native-Image Considerations

The design should keep native compilation straightforward.

Preferred choices:

- CDI-managed beans with explicit wiring
- Small interfaces and explicit implementations
- Quarkus REST and existing Kotlin serialization support
- Plain configuration mapping
- Simple scheduling and state management

Avoid for now:

- OpenCV
- ONNX Runtime
- MQTT clients
- reflection-heavy plugin mechanisms
- complex reactive stream frameworks
- file persistence stacks
- databases

The first version should behave like a conventional Quarkus service with a background loop and a few HTTP endpoints, not like a plugin host or data platform.

## Future Extension Points

The design deliberately leaves narrow seams for the most likely next steps.

### Real Cat Detector

Replace or add a `CatDetector` implementation without changing orchestration, API, or state storage.

### Individual Cat Recognition

Add a second-stage recognizer later, for example an identity recognizer that runs only after presence is already `DETECTED`.

This keeps:

- presence detection
- individual identification

as separate concerns.

### MQTT and Home Assistant Integration

Add an outbound publishing seam later, such as `RecognitionPublisher`, that reacts to recognition results.

This should remain downstream of recognition rather than being embedded into the detector or frame source.

### Saving Samples for Retraining

Add optional sample-capture policy and archive components later if low-confidence or unknown cases need to be stored for analysis.

This should be a side effect of recognition, not part of the core detection path.

### Motion-Triggered Detection

Add a new triggering mechanism or frame-source implementation later if polling proves too noisy or inefficient.

The current orchestration design should allow motion-triggered operation without changing the recognition core.

## Recommended First Implementation Boundary

The first implementation should include only:

- `FrameSource` and `SnapshotFrameSource`
- `CatDetector` with stub-style detector modes
- `RecognitionOrchestrator`
- `LatestRecognitionState`
- one worker lifecycle bean
- one recognition API resource
- health/readiness checks
- typed configuration

This is enough to replace the Quarkus starter scaffold with a real service skeleton while deliberately excluding:

- actual ML detection
- persistence
- MQTT publishing
- stream ingestion
- sample archiving

## Recommended Outcome

The initial version should produce a service that:

- starts as a normal Quarkus app
- runs mainly as a background polling worker
- fetches camera snapshots through a replaceable frame-source abstraction
- runs a replaceable detector abstraction
- keeps the latest recognition result in memory
- exposes a minimal operational HTTP surface
- remains simple enough for native-image compilation

That result is intentionally narrow, easy to reason about, and well-positioned for later ML and smart-home integration work.
