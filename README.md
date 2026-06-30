# hive

Docker-based smart home foundation with one folder per service and a root-level Compose stack.

## Services

- Home Assistant
- ESPHome
- Cat Recognizer (`app-cat-recognizer`)

## Repository Layout

```text
.
├── docker-compose.yml
├── .env.example
├── .gitignore
├── homeassistant/
│   └── config/
├── esphome/
│   └── config/
└── app-cat-recognizer/
```

## Bootstrap

1. Copy the environment template:

   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set the cat recognizer snapshot URL for your private ESP32-CAM endpoint:

   ```dotenv
   CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL=http://esp32-cam.local/snapshot
   CAT_RECOGNIZER_DETECTION_MODE=STUB
   CAT_RECOGNIZER_WORKER_ENABLED=false
   CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED=true
   ```

   Do not commit real local IPs, tokens, secrets, or host-specific `.env` values.

3. Build the cat recognizer native executable:

   ```bash
   ./gradlew :app-cat-recognizer:build
   ```

4. Validate the Compose file from the repository root:

   ```bash
   docker compose config
   ```

5. Build and start the stack from the repository root:

   ```bash
   docker compose build cat-recognizer
   docker compose up -d
   ```

   On macOS, use the Docker Desktop override file instead:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.mac.yml up -d
   ```

## Access

- Home Assistant: `http://<host-ip>:8123`
- ESPHome: `http://<host-ip>:6052`
- Cat Recognizer: `http://<host-ip>:8080`

Cat recognizer endpoints for basic verification:

- `GET http://<host-ip>:8080/api/recognition/latest`
- `POST http://<host-ip>:8080/api/recognition/run`
- `GET http://<host-ip>:8080/api/debug/config`
- `GET http://<host-ip>:8080/q/health/live`
- `GET http://<host-ip>:8080/q/health/ready`

Use Tailscale to reach the host remotely from your phone instead of exposing these services publicly.


## Cat Recognizer Real Snapshot Verification

Use `quarkusDev` when you want to test the existing snapshot-first plumbing against a real ESP32-CAM snapshot endpoint
before building or starting the full Compose stack. Prefer runtime overrides so private camera URLs, tokens, and local
IPs never get committed.

Environment-variable override example:

```bash
CAT_RECOGNIZER_CAMERA_SNAPSHOT_URL="http://esp32-cam.local/snapshot" \
CAT_RECOGNIZER_DETECTION_MODE="ALWAYS_PRESENT" \
CAT_RECOGNIZER_WORKER_ENABLED="false" \
CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED="true" \
./gradlew :app-cat-recognizer:quarkusDev
```

Quarkus system-property override example:

```bash
./gradlew \
  -Dcat-recognizer.camera.snapshot-url="http://esp32-cam.local/snapshot" \
  -Dcat-recognizer.detection.mode="ALWAYS_PRESENT" \
  -Dcat-recognizer.worker.enabled="false" \
  -Dcat-recognizer.debug.manual-trigger-enabled="true" \
  :app-cat-recognizer:quarkusDev
```

For deterministic plumbing checks with the current stub detector, use `ALWAYS_PRESENT` to expect `DETECTED` or `ALWAYS_ABSENT` to expect `NOT_DETECTED` after a successful snapshot fetch. The default `STUB` mode may still return `UNKNOWN`, which is acceptable for the skeleton but less useful for end-to-end camera verification.

While `quarkusDev` is running, verify the local endpoints:

```bash
curl http://localhost:8080/api/debug/config
curl http://localhost:8080/api/recognition/latest
curl -X POST http://localhost:8080/api/recognition/run
curl http://localhost:8080/api/recognition/latest
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready
```

Expected results:

- `GET /api/debug/config` reports `snapshotConfigured=true` and the selected detector mode.
- `POST /api/recognition/run` returns a recognition result.
- `ALWAYS_PRESENT` returns `DETECTED` when the snapshot fetch succeeds.
- `ALWAYS_ABSENT` returns `NOT_DETECTED` when the snapshot fetch succeeds.
- An unreachable camera URL returns `UNKNOWN` with a frame-source error; readiness may become `DOWN` depending on cached state.

Troubleshooting tips:

- If `esp32-cam.local` does not resolve, try the device IP address as a local runtime override only.
- Confirm the snapshot URL in a browser or with `curl` before debugging the service.
- Confirm the service host and ESP32-CAM are on the same network.
- Check whether ESPHome/Home Assistant exposes `/snapshot` directly or whether the actual snapshot URL differs.
- Container networking can differ from `quarkusDev`, especially for mDNS and host-only routes.

## Verification

Run from the repository root:

```bash
docker compose config
docker compose build cat-recognizer
docker compose up -d cat-recognizer
curl http://localhost:8080/q/health/live
curl http://localhost:8080/api/recognition/latest
```

Run:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

## Updating Images

Update the pinned image tags in `.env`, then recreate the stack:

```bash
docker compose pull
docker compose up -d
```

The cat recognizer runtime image is built locally by Compose from the native executable generated under
`app-cat-recognizer/build/`.

## Adding More Services

Follow the same pattern:

- create a top-level folder for the service, or add application modules
- put service config/data inside the appropriate folder
- define the service in the root `docker-compose.yml`

## Notes

local camera address: http://192.168.0.14:6053
