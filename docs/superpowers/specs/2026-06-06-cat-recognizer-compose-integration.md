# Cat Recognizer Compose Integration Spec

**Date:** 2026-06-06

## Goal

Add the existing `app-cat-recognizer` Quarkus service to the root Docker Compose stack so it can run beside Home
Assistant and ESPHome using the same repository-level infrastructure workflow.

The cat recognizer already has a snapshot-first skeleton with REST, debug, health, worker, frame-source, and
stub-detection seams. This slice only wires that existing service into Compose and environment configuration; it does
not change recognition behavior.

## Why Add It To The Root Stack

The root `docker-compose.yml` is the operational entry point for the home server. Adding `cat-recognizer` there is
useful because:

- The service can be started with the same `docker compose up -d` command as Home Assistant and ESPHome.
- The ESP32 snapshot URL and debug/runtime toggles can be configured through `.env` instead of source files.
- Host-network mode keeps local ESP32-CAM and service endpoints simple for Raspberry Pi or other home-server
  deployments.
- The service becomes visible in `docker compose config`, making future infrastructure changes easier to verify before
  adding MQTT or Home Assistant integration.

## Required Configuration

The root `.env` file should provide safe, local deployment values for:

- `CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL`, mapped to `cat-recognizer.camera.snapshot-url`
- `CAT_RECOGNIZER_DETECTION_MODE`, mapped to `cat-recognizer.detection.mode`
- `CAT_RECOGNIZER_WORKER_ENABLED`, mapped to `cat-recognizer.worker.enabled`
- `CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED`, mapped to `cat-recognizer.debug.manual-trigger-enabled`

The repository template must not include real camera IPs, tokens, secrets, or host-specific URLs. Defaults should remain
safe for local development and easy to override.

## Expected Runtime Behavior

- `cat-recognizer` is defined as a root Compose service with `container_name: cat-recognizer`.
- The service restarts with `restart: unless-stopped`.
- The service uses host networking unless a concrete blocker is found.
- The service image is built from the existing Quarkus module using the native runtime Dockerfile instead of the JVM
  Dockerfile or a separate application architecture.
- Runtime configuration is passed declaratively through Compose environment variables.
- HTTP endpoints are reachable from the host on the Quarkus HTTP port.

## Health And API Endpoints To Verify

After the service is running, verify these endpoints from the host:

- `GET /api/recognition/latest`
- `POST /api/recognition/run`
- `GET /api/debug/config`
- `GET /q/health/live`
- `GET /q/health/ready`

## Out Of Scope

This integration does not include:

- Real cat recognition, computer vision, machine learning, confidence calibration, OpenCV, ONNX, TensorFlow, Python,
  model files, or detector replacement.
- MQTT publishing or Home Assistant entity, automation, REST sensor, or dashboard integration.
- ESPHome changes.
- Camera stream support.
- Persistent history, databases, image retention, or architecture redesign.

## Acceptance Criteria

- Documentation exists for this Compose-integration slice under `docs/superpowers/`.
- The root `docker-compose.yml` includes a `cat-recognizer` service with host-network runtime access and
  environment-based configuration.
- `.env.example` documents the cat recognizer configuration without host-specific values or secrets.
- `README.md` lists the service, setup, endpoints, and basic verification commands.
- `docker compose config` passes from the repository root.
- `./gradlew :app-cat-recognizer:test` and `./gradlew :app-cat-recognizer:build` pass, producing the
  native runner for the Compose image build, or any environmental blocker is documented.
- Container build/runtime verification is attempted when Docker is available, with exact results recorded.
- No real CV/ML, MQTT, Home Assistant automation/entity, detector implementation, or camera stream changes are
  introduced.
