# Cat Recognizer Real Snapshot Verification Plan

> **For agentic workers:** Implement this plan task-by-task using checkbox (`- [ ]`) syntax for progress tracking. Keep real-device verification manual; CI must not depend on a camera being online.

**Goal:** Make it easy and reliable to run the existing `services/app-cat-recognizer` service against a real ESP32-CAM snapshot URL and verify config loading, snapshot HTTP fetch, manual recognition, latest-state reporting, health/readiness, and debug config behavior end-to-end.

**Scope guardrails:** Real CV/ML, OpenCV, ONNX, TensorFlow, Python/model files, Home Assistant integration, MQTT, service redesign, stream ingestion, and automated real-camera tests are explicitly out of scope.

---

## Files To Create Or Change

### Documentation

- [ ] Create: `docs/superpowers/specs/2026-06-06-cat-recognizer-real-snapshot-verification.md`
- [ ] Create: `docs/superpowers/plans/2026-06-06-cat-recognizer-real-snapshot-verification.md`
- [ ] Update if useful: `README.md`
- [ ] Update if useful: `.env.example`

### Application/Test Files

- [ ] Inspect: `services/app-cat-recognizer/src/main/resources/application.properties`
- [ ] Update tests only if no existing test proves the camera snapshot URL is configurable.
- [ ] Do not add tests that require a real ESP32-CAM.
- [ ] Avoid production code changes unless inspection finds a real blocker for runtime configuration or manual verification.

---

## Tasks

- [ ] Inspect `README.md`, `AGENTS.md`, `docker-compose.yml`, `services/app-cat-recognizer`, and existing `docs/superpowers/` files before changing anything.
- [ ] Confirm the existing application is snapshot-first and uses `SnapshotFrameSource` plus `cat-recognizer.camera.snapshot-url`.
- [ ] Confirm `cat-recognizer.camera.snapshot-url` can be overridden at runtime through Quarkus environment-variable mapping as `CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL`.
- [ ] Confirm the detector remains stub-only and supports deterministic `ALWAYS_PRESENT` and `ALWAYS_ABSENT` modes for plumbing verification.
- [ ] Add the real-snapshot verification spec with motivation, workflow, required config, endpoints, out-of-scope work, and acceptance criteria.
- [ ] Add this task-oriented implementation/verification plan.
- [ ] Add or update README/docs guidance for running `quarkusDev` with an overridden snapshot URL.
- [ ] Include an environment-variable example for `quarkusDev`.
- [ ] Include a JVM/Quarkus system-property example for `quarkusDev`.
- [ ] Keep example URLs as placeholders such as `http://esp32-cam.local/snapshot`; do not commit private device URLs, credentials, Wi-Fi details, or tokens.
- [ ] Add a minimal config override test only if existing tests do not prove the snapshot URL can be configured.
- [ ] Run automated verification from `services/`.
- [ ] Perform manual verification against a real ESP32-CAM snapshot URL only if one is available in the operator environment.
- [ ] Document any manual verification blocker, observed error, and likely next fix.
- [ ] Commit focused changes with a Conventional Commit-style subject.

---

## Run Commands From `services/`

### Environment Variable Override

```bash
CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL="http://esp32-cam.local/snapshot" \
CAT_RECOGNIZER_DETECTION_MODE="ALWAYS_PRESENT" \
CAT_RECOGNIZER_WORKER_ENABLED="false" \
CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED="true" \
./gradlew :app-cat-recognizer:quarkusDev
```

### JVM/Quarkus System Property Override

```bash
./gradlew \
  -Dcat-recognizer.camera.snapshot-url="http://esp32-cam.local/snapshot" \
  -Dcat-recognizer.detection.mode="ALWAYS_PRESENT" \
  -Dcat-recognizer.worker.enabled="false" \
  -Dcat-recognizer.debug.manual-trigger-enabled="true" \
  :app-cat-recognizer:quarkusDev
```

Use `ALWAYS_ABSENT` instead of `ALWAYS_PRESENT` when you want a successful snapshot fetch to produce `NOT_DETECTED`.

---

## Local Curl Verification

Run these while `quarkusDev` is running:

```bash
curl http://localhost:8080/api/debug/config
curl http://localhost:8080/api/recognition/latest
curl -X POST http://localhost:8080/api/recognition/run
curl http://localhost:8080/api/recognition/latest
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready
```

Expected behavior:

- `/api/debug/config` reports `snapshotConfigured=true` and the expected `detectionMode`.
- `POST /api/recognition/run` returns a recognition result.
- With `ALWAYS_PRESENT`, the manual result status should be `DETECTED` when the snapshot fetch succeeds.
- With `ALWAYS_ABSENT`, the manual result status should be `NOT_DETECTED` when the snapshot fetch succeeds.
- With an unreachable camera URL, the manual result should become `UNKNOWN` with a `FRAME_FETCH_FAILED` frame-source error.
- Readiness may become `DOWN` after camera failures depending on cached state and staleness.
- `/api/recognition/latest` should reflect the latest manual or worker result.

---

## Troubleshooting DNS And Network Issues

- If `esp32-cam.local` does not resolve, try the ESP32-CAM private IP address temporarily as a runtime override only.
- Confirm the snapshot endpoint in a browser or with `curl http://esp32-cam.local/snapshot` before blaming the service.
- Confirm the service host and ESP32-CAM are on the same network/VLAN and that client isolation is not blocking traffic.
- Check whether ESPHome/Home Assistant exposes `/snapshot` directly or whether the actual snapshot URL differs.
- If the camera endpoint requires credentials or tokens, pass them only through local runtime config and do not commit them.
- If running inside Docker later, remember that container networking, host networking, DNS, and mDNS behavior may differ from `quarkusDev`.

---

## Automated Verification

Run from `services/`:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:quarkusIntTest
./gradlew :app-cat-recognizer:build
```

Real-device verification remains manual and should not be added to CI.

## Completion Notes

The implementation is complete when documentation explains the real snapshot workflow, automated tests pass without camera dependencies, optional manual curl results are recorded, and no real CV/ML, Home Assistant integration, MQTT, or hardcoded private camera configuration has been added.
