# hive

A self-hosted smart-home stack combining Home Assistant, ESPHome, and a cat-recognition service for an ESP32-CAM.
The repository contains both the Docker Compose infrastructure and the JVM applications used by the recognizer.

## Components

- **Home Assistant** — automation and smart-home UI, with configuration under `homeassistant/config/`.
- **ESPHome** — ESP32 device management, with the camera definition under `esphome/config/`.
- **Cat Recognizer** — Quarkus/Kotlin service that reads ESP32-CAM frames, runs the bundled YOLO11 ONNX model, and
  exposes a small web UI and REST API.
- **ESPHome client** — Kotlin/Java library implementing the ESPHome native Protobuf API, including plaintext and Noise
  encrypted transports. The recognizer uses its synchronous client; an asynchronous Netty API is also under
  development.

## Repository Layout

```text
.
├── app-cat-recognizer/       # Quarkus application, web UI, and YOLO model
├── lib-esphome-client/       # ESPHome native API library and generated Protobuf types
├── homeassistant/config/     # Home Assistant configuration
├── esphome/config/           # ESPHome device configuration
├── docs/superpowers/         # Design specifications and implementation plans
├── docker-compose.yml        # Linux/host-network stack
├── docker-compose.mac.yml    # Docker Desktop networking override
├── gradle/                   # Gradle wrapper and version catalog
└── .env.example              # Local Compose environment template
```

## Prerequisites

- JDK 25 (the Gradle modules use a Java 25 toolchain)
- Docker with the Compose plugin
- An ESP32-CAM running ESPHome, reachable from the development or Docker host

Keep Wi-Fi credentials, API encryption keys, OTA passwords, private addresses, and other host-specific values out of
version control. Use an uncommitted `.env`, ESPHome `secrets.yaml`, or runtime environment overrides.

## Local Development

Run commands from the repository root.

```bash
./gradlew :app-cat-recognizer:quarkusDev
```

The recognizer defaults to the ESPHome native API at `esp32-cam.local:6053`. Override Quarkus configuration with
environment variables when the device address or transport differs:

```bash
CAT_RECOGNIZER_CAMERA_SOURCE=NATIVE_API \
CAT_RECOGNIZER_CAMERA_NATIVE_API_HOST=esp32-cam.local \
CAT_RECOGNIZER_CAMERA_NATIVE_API_PORT=6053 \
CAT_RECOGNIZER_CAMERA_NATIVE_API_ENCRYPTION_ENABLED=false \
./gradlew :app-cat-recognizer:quarkusDev
```

To use the camera's HTTP snapshot endpoint instead:

```bash
CAT_RECOGNIZER_CAMERA_SOURCE=HTTP_SNAPSHOT \
CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL=http://esp32-cam.local:8081/ \
./gradlew :app-cat-recognizer:quarkusDev
```

If native API encryption is enabled on the device, also set
`CAT_RECOGNIZER_CAMERA_NATIVE_API_ENCRYPTION_KEY` at runtime. Do not put the key in tracked files.

Open `http://localhost:8080/` for the detection UI. It supports live results and test-image uploads.

## Docker Compose

Create the local environment file, build the Quarkus JVM artifact, validate the stack, and start it:

```bash
cp .env.example .env
./gradlew :app-cat-recognizer:build
docker compose config
docker compose build cat-recognizer
docker compose up -d
```

The cat-recognizer Dockerfile copies the prebuilt `app-cat-recognizer/build/quarkus-app/` output, so the Gradle build
must run before the Compose image build.

Linux uses host networking. On macOS, apply the Docker Desktop override:

```bash
docker compose -f docker-compose.yml -f docker-compose.mac.yml up -d
```

Service URLs:

- Home Assistant: `http://<host>:8123`
- ESPHome dashboard: `http://<host>:6052`
- Cat Recognizer: `http://<host>:8080`

Prefer a private network such as Tailscale for remote access instead of exposing these ports publicly.

## Cat Recognizer API

Quarkus serves application endpoints below `/api`:

- `GET /api/recognition/latest` — latest result and worker status
- `POST /api/recognition/run` — trigger a recognition cycle when manual triggers are enabled
- `POST /api/recognition/upload` — detect a cat in multipart form field `image` when uploads are enabled
- `GET /api/frame/latest` — latest captured JPEG, or `204 No Content`
- `GET /api/debug/config` — non-secret runtime configuration summary
- `GET /api/q/health/live` — liveness
- `GET /api/q/health/ready` — readiness

Example checks:

```bash
curl http://localhost:8080/api/debug/config
curl http://localhost:8080/api/recognition/latest
curl -X POST http://localhost:8080/api/recognition/run
curl -F image=@/path/to/test-image.jpg http://localhost:8080/api/recognition/upload
curl http://localhost:8080/api/q/health/live
```

The background recognition job runs every five seconds by default. Configuration defaults are defined in
`app-cat-recognizer/src/main/resources/application.properties`; each dotted Quarkus property can be overridden with
its uppercase, underscore-separated environment variable form.

## Build and Test

```bash
./gradlew test
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
./gradlew :lib-esphome-client:test
```

The ESPHome client contains a `@Tag("live")` camera test with a host-specific endpoint. It is not currently excluded
by the Gradle test task, so it requires the configured device to be reachable. Treat it as a manual integration test;
the build should eventually separate it from the normal unit-test workflow.

For infrastructure-only changes:

```bash
docker compose config
```

## Updating and Operating the Stack

The Home Assistant and ESPHome services currently track their upstream `stable` tags. Pull and recreate them with:

```bash
docker compose pull
docker compose up -d
```

After recognizer code changes, rebuild the application artifact and local image before recreating that service:

```bash
./gradlew :app-cat-recognizer:build
docker compose build cat-recognizer
docker compose up -d cat-recognizer
```
