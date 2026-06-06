# Cat Recognizer Test Stabilization Spec

**Date:** 2026-06-06

## Goal

Stabilize the existing `services/app-cat-recognizer` skeleton with focused tests before adding real computer-vision or machine-learning behavior.

The current service already has the planned snapshot-first shape: typed configuration, frame acquisition, stub detection modes, recognition orchestration, in-memory latest state, worker lifecycle, REST/debug resources, and health checks. The next step is to lock those seams down with deterministic tests so later CV/ML work can replace the detector implementation without changing the surrounding service contract.

## Why Stabilize First

The skeleton is intentionally boring infrastructure for future recognition work. Stabilizing it first is valuable because:

- The detector seam must be trusted before swapping in a real model.
- The orchestrator must consistently map detector and camera outcomes to API-facing statuses and errors.
- The latest-state cache drives health, debugging, and future automation integrations.
- The worker lifecycle should be deterministic in tests so build verification does not depend on real sleeps or camera access.
- API and health tests provide a safety net around the public service surface while the internals remain stubbed.

## Behavior To Cover

Tests should cover these existing behaviors:

- `StubCatDetector` maps each configured `DetectionMode` to the expected `DetectionOutcome`.
- `LatestRecognitionState` starts with safe defaults, records worker enabled/running flags, resets failure state after success, and increments failure state after errors.
- `RecognitionOrchestrator` maps:
  - present detections to `DETECTED`
  - absent detections to `NOT_DETECTED`
  - unknown detections to `UNKNOWN` with `DETECTOR_UNKNOWN`
  - frame-source failures to the frame-source error code, especially `FRAME_FETCH_FAILED`
  - unexpected detector failures to `DETECTOR_FAILED`
- Orchestrator results are written back to `LatestRecognitionState` as success or failure.
- `WorkerLifecycle` handles disabled workers, enabled worker submission, success/error delays, submission rollback, and stop behavior without long sleeps.
- REST endpoints expose the latest recognition state and allow manual recognition when explicitly enabled.
- Readiness/health endpoints are reachable and include the custom `worker-readiness` and `frame-source` checks.
- Native/integration smoke coverage remains aligned with the non-starter API surface if the module expects an integration test.

## Out Of Scope

This stabilization does not include:

- Real cat recognition, computer vision, ML inference, or confidence calibration.
- OpenCV, ONNX, TensorFlow, Python, model files, or image fixtures for model validation.
- Docker Compose, Home Assistant, MQTT, ESPHome, or root infrastructure integration.
- Architecture redesign beyond minimal production fixes required by tests.
- Persistent history, databases, metrics dashboards, or image retention.

## Verification Commands

Run commands from `services/`:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:quarkusIntTest
./gradlew :app-cat-recognizer:build
```

## Acceptance Criteria

- Documentation exists for this test-stabilization slice under `docs/superpowers/`.
- Tests exist for detector, state, orchestrator, worker lifecycle, REST API, health endpoints, and native/integration smoke behavior.
- Starter `GreetingResourceTest` / `GreetingResourceIT` files are absent if their endpoints are no longer present.
- The verification commands above pass from `services/`.
- Production changes, if any, are minimal and directly justified by failing tests.
- No real CV/ML dependencies, model artifacts, or Docker Compose integration are introduced.
