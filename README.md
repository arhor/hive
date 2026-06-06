# hive

Docker-based smart home foundation with one folder per service and a root-level Compose stack.

## Services

- Home Assistant
- ESPHome
- Cat Recognizer (`services/app-cat-recognizer`)

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
└── services/
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

3. Build the cat recognizer native executable from `services/`:

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

## Updating Images

Update the pinned image tags in `.env`, then recreate the stack:

```bash
docker compose pull
docker compose up -d
```

The cat recognizer runtime image is built locally by Compose from the native executable generated under `services/app-cat-recognizer/build/`.

## Adding More Services

Follow the same pattern:

- create a top-level folder for the service, or add application modules under `services/`
- put service config/data inside the appropriate folder
- define the service in the root `docker-compose.yml`
