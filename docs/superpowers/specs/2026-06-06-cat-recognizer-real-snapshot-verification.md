# Cat Recognizer Real Snapshot Verification Spec

**Date:** 2026-06-06

## Goal

Verify the existing `app-cat-recognizer` snapshot-first service against a real ESP32-CAM snapshot endpoint without
changing its architecture or adding real recognition logic.

This step exists to prove the service plumbing end-to-end before any CV/ML work begins:

- Quarkus config loading accepts runtime overrides for a private camera snapshot URL.
- `SnapshotFrameSource` can fetch bytes from an ESP32-CAM HTTP snapshot endpoint.
- The manual recognition trigger exercises the frame-source and detector seam on demand.
- The latest-recognition endpoint reflects the most recent manual or worker result.
- Health and readiness endpoints expose the current service state.
- The debug config endpoint confirms safe runtime configuration details without leaking private URLs.

## What Real ESP32 Snapshot Verification Means

Real snapshot verification means running `app-cat-recognizer` locally with `quarkusDev`, pointing `cat-recognizer.camera.snapshot-url` at an operator-provided ESP32-CAM snapshot URL, and using HTTP requests to confirm that the service can fetch an image and produce a recognition result through the current stub detector.

The detector is still intentionally stubbed. For deterministic plumbing checks, operators should use `cat-recognizer.detection.mode=ALWAYS_PRESENT` or `cat-recognizer.detection.mode=ALWAYS_ABSENT` instead of relying on `STUB`.

## Expected Operator Workflow

1. Confirm the ESP32-CAM snapshot URL works outside the service, for example in a browser or with `curl`.
2. Start the service with runtime overrides for the snapshot URL and detector mode.
3. Call the debug config endpoint and confirm a snapshot URL is configured and the expected detector mode is active.
4. Check the latest-recognition endpoint before a manual run to understand the current cached state.
5. Trigger recognition manually with `POST /api/recognition/run`.
6. Re-check the latest-recognition endpoint and confirm it reflects the manual run result.
7. Check liveness and readiness.
8. If the camera is unreachable, use the returned frame-source error and readiness details to troubleshoot DNS, network, or URL shape issues.

## Required Configuration

The service already defines these relevant Quarkus configuration keys:

```properties
cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.detection.mode=STUB
cat-recognizer.worker.enabled=true
cat-recognizer.debug.manual-trigger-enabled=true
```

For local real-device verification, prefer runtime overrides instead of committing local values:

```bash
CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL="http://esp32-cam.local/snapshot"
CAT_RECOGNIZER_DETECTION_MODE="ALWAYS_PRESENT"
CAT_RECOGNIZER_WORKER_ENABLED="false"
CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED="true"
```

The snapshot URL may be a `.local` hostname or a private IP address. Do not commit real device IPs, credentials, tokens, Wi-Fi details, or other host-specific values.

## Expected HTTP Endpoints

Local `quarkusDev` verification should use these endpoints:

- `GET /api/debug/config`
- `GET /api/recognition/latest`
- `POST /api/recognition/run`
- `GET /q/health/live`
- `GET /q/health/ready`

Expected behavior:

- `/api/debug/config` reports `snapshotConfigured=true` and the configured detector mode.
- `POST /api/recognition/run` returns a recognition result.
- With `ALWAYS_PRESENT`, the result status is `DETECTED` when the snapshot fetch succeeds.
- With `ALWAYS_ABSENT`, the result status is `NOT_DETECTED` when the snapshot fetch succeeds.
- With an unreachable camera URL, the result status becomes `UNKNOWN` with a frame-source error, and readiness may become `DOWN` depending on cached state.
- `/api/recognition/latest` reflects the latest manual or worker result.

## Out Of Scope

This step explicitly does not include:

- Real cat detection, computer vision, or machine learning.
- OpenCV, ONNX, TensorFlow, Python, model files, image datasets, or cat-specific recognition.
- Home Assistant integration.
- MQTT publishing or subscription.
- New persistence, image storage, stream ingestion, or camera discovery.
- Automated tests that require a real ESP32-CAM.
- Hardcoding private camera URLs in source or committed config.

## Acceptance Criteria

- A real-snapshot verification spec and task plan exist under `docs/superpowers/`.
- The README or docs explain how to run `app-cat-recognizer` with a runtime snapshot URL override.
- The documented commands cover debug config, latest recognition, manual recognition, liveness, and readiness.
- Any example config uses placeholders only and does not include private URLs or secrets.
- Existing automated tests still pass, and any new tests avoid real-device dependencies.
- Manual real-device verification results are recorded by the operator when a camera endpoint is available.
- No CV/ML, Home Assistant, MQTT, service redesign, or default detector-mode change is introduced.
