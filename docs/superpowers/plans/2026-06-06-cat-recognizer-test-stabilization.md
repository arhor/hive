# Cat Recognizer Test Stabilization Plan

> **For agentic workers:** Implement this plan task-by-task using checkbox (`- [ ]`) syntax for progress tracking. Keep production edits minimal and test-driven.

**Goal:** Stabilize the existing `app-cat-recognizer` skeleton with deterministic tests around the current stub
detector, recognition state, orchestration, worker lifecycle, REST API, health checks, and integration smoke surface
before any real CV/ML work.

**Scope guardrails:** Real cat recognition, OpenCV, ONNX, TensorFlow, Python/model files, Docker Compose integration, MQTT/Home Assistant publishing, and architecture redesign are explicitly out of scope.

---

## Files To Create Or Change

### Documentation

- Create: `docs/superpowers/specs/2026-06-06-cat-recognizer-test-stabilization.md`
- Create: `docs/superpowers/plans/2026-06-06-cat-recognizer-test-stabilization.md`

### Test files

- Create/update: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/detection/StubCatDetectorTest.kt`
- Create/update: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/state/LatestRecognitionStateTest.kt`
- Create/update:
  `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/recognition/RecognitionOrchestratorTest.kt`
- Create/update: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/bootstrap/WorkerLifecycleTest.kt`
- Create/update: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceTest.kt`
- Create/update: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/health/HealthEndpointsTest.kt`
- Create/update: `app-cat-recognizer/src/native-test/kotlin/io/github/arhor/catrecognizer/web/RecognitionResourceIT.kt`

### Production files

- Change only if tests expose a real skeleton defect, and keep fixes narrow.

---

## Tasks

- [ ] Inspect existing `docs/superpowers/` specs and plans to match naming, date, and section style.
- [ ] Add the test-stabilization spec describing motivation, behavior under test, out-of-scope work, verification commands, and acceptance criteria.
- [ ] Add this implementation plan with task-oriented steps and the files expected to change.
- [ ] Add `StubCatDetectorTest` cases for `ALWAYS_PRESENT`, `ALWAYS_ABSENT`, and `STUB` outcomes.
- [ ] Add `LatestRecognitionStateTest` cases for initial defaults, worker flags, success reset behavior, and failure counting/error storage.
- [ ] Add `RecognitionOrchestratorTest` cases for present, absent, unknown, frame-source failure, detector failure, and state success/failure recording.
- [ ] Add deterministic `WorkerLifecycleTest` cases for disabled startup, enabled task submission, success/error delay selection, submission rollback, termination success, and termination timeout behavior.
- [ ] Add Quarkus REST tests for `GET /api/recognition/latest` and enabled `POST /api/recognition/run` behavior using test profile configuration overrides where needed.
- [ ] Add Quarkus health endpoint tests that assert readiness is reachable and includes `worker-readiness` plus `frame-source` checks without depending on check ordering.
- [ ] Add or update a minimal native/integration smoke test for the packaged recognition API and health surface if the module still expects `quarkusIntTest` coverage.
- [ ] Remove leftover starter `GreetingResourceTest` / `GreetingResourceIT` files if they still exist and reference deleted starter endpoints.
- [ ] Run `./gradlew :app-cat-recognizer:test`.
- [ ] Run `./gradlew :app-cat-recognizer:quarkusIntTest`.
- [ ] Run `./gradlew :app-cat-recognizer:build`.
- [ ] Commit focused changes. Suggested commits:
  - `docs: add cat recognizer test stabilization plan`
  - `test: stabilize cat recognizer skeleton`

---

## Verification

Run:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:quarkusIntTest
./gradlew :app-cat-recognizer:build
```

## Completion Notes

The implementation is complete when the tests cover the existing skeleton behavior, all verification commands pass, and no CV/ML dependencies or Docker Compose integration have been added.
