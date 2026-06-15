# Cat Recognizer OpenCV Detector Design

**Date:** 2026-06-06

## Goal

Add a minimal OpenCV-backed detector mode to `app-cat-recognizer` that proves:

- the service can use `quarkus-opencv` in JVM mode
- snapshot JPEG and PNG byte arrays can be decoded into an OpenCV image matrix
- a tiny deterministic image-processing placeholder can run on the decoded matrix
- the existing `CatDetector` seam remains the only detector-facing boundary
- existing detector modes continue to work unchanged
- detector failures continue to map through the existing recognition failure behavior

This design is intentionally a proof step, not a real cat classifier.

## Scope

This design covers:

- build and dependency changes needed to add OpenCV support
- detector-mode expansion for a new `OPENCV` mode
- CDI-based detector selection that preserves the existing seam
- minimal OpenCV byte decoding and placeholder processing
- JVM tests that prove decode and execution behavior
- verification expectations for existing modes and native packaging compatibility

This design does not cover:

- real cat classification heuristics
- training data, models, or ML inference
- public API changes
- frame persistence or debug image storage
- replacing the existing recognition error model

## Constraints

- `CatDetector` must remain the seam consumed by `RecognitionOrchestrator`
- `RecognitionOrchestrator` should stay mode-agnostic
- `STUB`, `ALWAYS_PRESENT`, and `ALWAYS_ABSENT` behavior must remain intact
- OpenCV-specific failures should not introduce a second error-mapping path
- the implementation must continue to fit the current Quarkus/Kotlin service shape
- native-oriented packaging must remain build-compatible even if OpenCV behavior is only asserted in JVM tests

## Recommended Approach

Use a dedicated OpenCV detector implementation behind the existing `CatDetector` interface and select the active detector through CDI based on `RecognizerConfig.detection().mode()`.

Why this is the right cut:

- it keeps OpenCV-specific concerns out of the current stub detector
- it preserves `CatDetector` as the only boundary used by recognition flow
- it avoids pushing image-decoding logic into `RecognitionOrchestrator`
- it keeps existing detector modes isolated from new runtime dependencies
- it leaves room for a future real OpenCV detector without rewriting the seam again

Alternatives considered:

1. Extend `StubCatDetector` with an `OPENCV` branch.
   Rejected because it mixes simple placeholder modes with byte decoding and native library usage in one class.
2. Decode and process frames inside `RecognitionOrchestrator`.
   Rejected because it weakens the current detector boundary and couples orchestration to image-processing details.

## Detector Architecture

### Existing Seam

Keep `CatDetector` unchanged:

- input: `FramePayload`
- output: `DetectionOutcome`

`RecognitionOrchestrator` must continue to depend only on this seam and must not gain any OpenCV-specific branches.

### Detection Mode

Add a new enum member:

- `DetectionMode.OPENCV`

Configuration continues to flow through `RecognizerConfig.Detection.mode()`, so no new configuration subtree is needed for this proof step.

### Implementations

Keep the current stub-oriented detector responsible only for:

- `STUB`
- `ALWAYS_PRESENT`
- `ALWAYS_ABSENT`

Add a new `OpenCvCatDetector` responsible only for:

- `OPENCV`

Detector selection should happen through CDI wiring, not by branching inside `RecognitionOrchestrator`.

Implementation choice:

- keep implementation classes focused on their own behavior
- add a small producer bean that returns the active `CatDetector` for the configured mode

This keeps the seam clean while avoiding ambiguous CDI injection when multiple detector implementations exist.

## OpenCV Detector Behavior

`OpenCvCatDetector.detect(frame)` should perform exactly these steps:

1. Validate that the frame bytes can be converted into an OpenCV input buffer.
2. Decode the image bytes into a `Mat` using OpenCV image codecs.
3. Fail if decoding produces an empty or unusable matrix.
4. Run one tiny deterministic placeholder image-processing operation on the decoded matrix.
5. Return `DetectionOutcome.Unknown(reason = "opencv placeholder detector")`.

The placeholder operation for this change should be explicit:

- convert the decoded image to grayscale
- read a deterministic property from the grayscale matrix such as row count, column count, or total pixel count

This proves that OpenCV decoded the bytes into a usable matrix and that a tiny processing path executed against that matrix. The derived value is not exposed and must not affect cat presence status. Successful execution always returns `Unknown`.

## Decode Inputs

The detector must support at least:

- `image/jpeg`
- `image/png`

Support should be based on the actual bytes, not on trusting the content type string alone. The content type can still be useful for test fixtures and diagnostics, but decode success should come from OpenCV, not from MIME branching.

## Failure Handling

OpenCV decode or processing failures should raise a normal exception with a safe, concise message:

- `"OpenCV failed to decode frame"`
- `"OpenCV placeholder processing failed"`

The detector should not construct `RecognitionResult` or `RecognitionError` directly.

Failure mapping remains centralized in `RecognitionOrchestrator`, which already:

- converts detector exceptions into `DETECTOR_FAILED`
- preserves the observed timestamp when a frame was already fetched
- records the failure in `LatestRecognitionState`
- applies `cat-recognizer.detection.unknown-on-error` to the `retriable` flag

This preserves one error path for all detector implementations.

## Build and Packaging

Add the `quarkus-opencv` dependency to the service module.

The design target is:

- prove OpenCV execution in JVM mode through tests
- keep native-oriented packaging build-compatible for the module

This does not require proving OpenCV behavior in native tests in this change. It does require avoiding an implementation shape that obviously breaks the current native build path.

## Testing Strategy

### JVM Detector Tests

Add focused JVM tests for `OpenCvCatDetector` that prove:

- JPEG bytes decode successfully into a usable matrix
- PNG bytes decode successfully into a usable matrix
- the placeholder processing path runs and returns `DetectionOutcome.Unknown("opencv placeholder detector")`
- invalid image bytes fail with a safe exception

These tests should use small in-repo fixture bytes or minimal generated image fixtures, not network calls.

### Existing Mode Regression Tests

Keep and extend current detector/config tests so that:

- `StubCatDetector` still covers `STUB`, `ALWAYS_PRESENT`, and `ALWAYS_ABSENT`
- config binding recognizes the new `OPENCV` enum value when explicitly set
- default config remains unchanged unless we intentionally switch it later

### Orchestrator Failure-Mapping Tests

Add or update orchestrator-level tests to prove that OpenCV-mode detector failures still map through the existing failure path, for example:

- configured mode is `OPENCV`
- detector throws
- result stays `UNKNOWN`
- error code stays `DETECTOR_FAILED`
- retriable behavior still follows `unknownOnError`

This test should validate the seam behavior, not OpenCV internals.

### Verification Commands

Before claiming the change is complete, run fresh verification for:

- `./gradlew :app-cat-recognizer:test`
- `./gradlew :app-cat-recognizer:build`

If native packaging compatibility becomes sensitive after the dependency change, use the build output as the source of truth and address any packaging breakage before closing the work.

## Expected Result

After this change:

- the service will have an `OPENCV` detector mode
- OpenCV can be exercised from the detector layer in JVM mode
- snapshot JPEG and PNG bytes can be decoded into a `Mat`
- placeholder processing proves the decoded matrix is usable
- successful OpenCV execution still reports `UNKNOWN`
- existing modes keep working
- detector crashes and decode failures still surface through the current recognition failure behavior

## Implementation Notes

- Prefer a narrow class layout over a generic detector framework.
- Keep OpenCV-specific code local to the new detector implementation.
- Keep messages deterministic so tests can assert them cleanly.
- Keep comments minimal; the class and method names should carry most of the intent.
