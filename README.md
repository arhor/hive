# hive

Docker-based smart home foundation with one folder per service and a root-level Compose stack.

## Services

- Home Assistant
- ESPHome

## Repository Layout

```text
.
├── docker-compose.yml
├── .env.example
├── .gitignore
├── homeassistant/
│   └── config/
└── esphome/
    └── config/
```

## Bootstrap

1. Copy the environment template:

   ```bash
   cp .env.example .env
   ```

2. Validate the Compose file:

   ```bash
   docker compose config
   ```

3. Start the stack:

   ```bash
   docker compose up -d
   ```

## Access

- Home Assistant: `http://<host-ip>:8123`
- ESPHome: `http://<host-ip>:6052`

Use Tailscale to reach the host remotely from your phone instead of exposing these services publicly.

## Updating Images

Update the pinned image tags in `.env`, then recreate the stack:

```bash
docker compose pull
docker compose up -d
```

## Adding More Services

Follow the same pattern:

- create a top-level folder for the service
- put service config/data inside that folder
- define the service in the root `docker-compose.yml`
