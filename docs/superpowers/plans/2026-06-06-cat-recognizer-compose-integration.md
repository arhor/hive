# Cat Recognizer Compose Integration Plan

> **For agentic workers:** Implement this plan task-by-task using checkbox (`- [ ]`) syntax for progress tracking. Keep the work limited to root Compose integration for the existing snapshot-first cat recognizer service.

**Goal:** Add `services/app-cat-recognizer` to the root Docker Compose stack so it can be configured through `.env`, built from the existing Quarkus native runner, started with the root stack, and reached from the host network.

**Scope guardrails:** Real CV/ML, OpenCV, ONNX, TensorFlow, Python/model files, MQTT publishing, Home Assistant entities/automations/dashboards, ESPHome changes, detector implementation changes, architecture redesign, and camera stream support are explicitly out of scope.

---

## Files To Create Or Change

### Documentation

- Create: `docs/superpowers/specs/2026-06-06-cat-recognizer-compose-integration.md`
- Create: `docs/superpowers/plans/2026-06-06-cat-recognizer-compose-integration.md`

### Root infrastructure

- Update: `docker-compose.yml`
- Update: `.env.example`
- Update only if needed: `.gitignore`

### Application packaging/configuration

- Inspect/update only if needed: `services/app-cat-recognizer/build.gradle.kts`
- Inspect/update only if needed: `services/app-cat-recognizer/src/main/resources/application.properties`
- Inspect/reuse: `services/app-cat-recognizer/src/main/docker/Dockerfile.native`
- Inspect only: `services/app-cat-recognizer/src/main/docker/Dockerfile.jvm`

### Repository documentation

- Update: `README.md`

---

## Tasks

- [ ] Inspect existing `docs/superpowers/` specs and plans to match naming, date, and section style.
- [ ] Add the Compose-integration spec describing motivation, configuration, runtime behavior, verification endpoints, out-of-scope work, and acceptance criteria.
- [ ] Add this implementation plan with small executable tasks, file list, scope guardrails, and exact verification commands.
- [ ] Inspect `services/app-cat-recognizer/build.gradle.kts` for Quarkus container-image and packaging behavior.
- [ ] Inspect `services/app-cat-recognizer/src/main/resources/application.properties` for runtime defaults and env-var-mappable config keys.
- [ ] Inspect Dockerfiles under `services/app-cat-recognizer/src/main/docker/` and choose the native runtime Dockerfile for Docker/Compose usage.
- [ ] Update `docker-compose.yml` with a `cat-recognizer` service using `container_name: cat-recognizer`, `restart: unless-stopped`, host networking, and declarative environment configuration.
- [ ] Ensure the Compose service does not hard-code real camera URLs, local IPs, secrets, MQTT settings, or Home Assistant integration.
- [ ] Update `.env.example` with safe placeholders/defaults for `CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL`, `CAT_RECOGNIZER_DETECTION_MODE`, `CAT_RECOGNIZER_WORKER_ENABLED`, and `CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED`.
- [ ] Confirm the Compose environment names map to Quarkus config keys: `cat-recognizer.camera.snapshot-url`, `cat-recognizer.detection.mode`, `cat-recognizer.worker.enabled`, and `cat-recognizer.debug.manual-trigger-enabled`.
- [ ] Keep `application.properties` defaults aligned with native Docker usage and avoid switching the service to JVM packaging.
- [ ] Update `README.md` with the new service list entry, `.env` setup, access URLs/endpoints, and basic verification commands.
- [ ] Confirm `.gitignore` already ignores `.env` and generated build artifacts, or update it narrowly if needed.
- [ ] Run `docker compose config` from the repository root.
- [ ] Run `./gradlew :app-cat-recognizer:test` from `services/`.
- [ ] Run `./gradlew :app-cat-recognizer:build` from `services/`.
- [ ] If possible, run `docker compose build cat-recognizer` from the repository root.
- [ ] If possible, run `docker compose up -d cat-recognizer` from the repository root.
- [ ] If the container starts, verify `curl http://localhost:8080/q/health/live` from the repository root.
- [ ] If the container starts, verify `curl http://localhost:8080/api/recognition/latest` from the repository root.
- [ ] Document any Docker, network, dependency, or runtime limitation that prevents full container verification.
- [ ] Commit focused changes with a Conventional Commit-style subject.

---

## Verification

Run from the repository root:

```bash
docker compose config
docker compose build cat-recognizer
docker compose up -d cat-recognizer
curl http://localhost:8080/q/health/live
curl http://localhost:8080/api/recognition/latest
```

Run from `services/`:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

## Completion Notes

The implementation is complete when the Compose service can be validated from the root stack, application tests/build pass or blockers are documented, and no real CV/ML, MQTT, Home Assistant entity/automation/dashboard, detector replacement, architecture redesign, or camera stream support has been added.
