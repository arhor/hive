# Smart Home Foundation Design

**Date:** 2026-06-05

## Goal

Turn this repository into the initial foundation for a Docker-based smart home setup, with a root-level Compose file and one dedicated folder per service. The first two services are Home Assistant and ESPHome.

## Scope

This design covers:

- Repository layout for the initial stack
- Docker Compose structure and conventions
- Service definitions for Home Assistant and ESPHome
- Baseline configuration files and bootstrap documentation

This design does not cover:

- Reverse proxy or public ingress
- TLS, authentication hardening, or SSO
- Automated backups
- Monitoring, logging, or update automation
- Additional services beyond Home Assistant and ESPHome

## Constraints

- Access is local-network only
- Remote use will happen through Tailscale, not through public internet exposure
- Image versions should be pinned to explicit releases
- The repository should use clear, predictable host paths such as `./homeassistant/config`

## Repository Structure

The repository will use a flat, service-oriented layout:

```text
.
├── docker-compose.yml
├── .env.example
├── README.md
├── homeassistant/
│   └── config/
└── esphome/
    └── config/
```

Rationale:

- The root Compose file keeps the stack discoverable and easy to operate
- Each service owns a single top-level folder
- Service data stays in repo-local paths, which makes backup and migration straightforward
- The layout scales naturally as more home services are added later

## Compose Design

The root `docker-compose.yml` will define:

- A single user-defined network shared by all services in the stack
- One service entry for `homeassistant`
- One service entry for `esphome`
- Host bind mounts that map directly into each service folder
- Explicit published ports for local-network access

Baseline conventions:

- `restart: unless-stopped` for both services
- Explicit `container_name` values
- Pinned image tags controlled through `.env`
- Shared environment variables such as timezone also controlled through `.env`
- No Compose-level dependency that implies application coupling beyond network membership

The stack will prefer clarity over abstraction. No anchors, includes, or multi-file Compose layering are needed at this stage.

## Service Design

### Home Assistant

Purpose:

- Acts as the central smart home controller and UI

Compose characteristics:

- Published port `8123`
- Config mount: `./homeassistant/config:/config`
- Timezone passed from environment

Notes:

- The initial implementation should use standard bridge networking with published port `8123` to keep the scaffold simple and consistent with the rest of the stack.
- If discovery or device-integration requirements later justify host networking, that can be introduced as a deliberate follow-up change rather than part of the foundation scaffold.
- Home Assistant and ESPHome integration happens inside Home Assistant after startup, not through special Compose wiring.

### ESPHome

Purpose:

- Provides firmware management and dashboard access for ESP-based devices

Compose characteristics:

- Published port `6052`
- Config mount: `./esphome/config:/config`
- Timezone passed from environment

Notes:

- ESPHome remains operationally independent from Home Assistant at the Compose level
- Any Home Assistant integration happens after the stack is running

## Configuration Files

The initial scaffold should include minimal starter files so the mounted directories exist intentionally and the repo communicates expected ownership clearly.

Planned bootstrap files:

- `.env.example` with pinned image version variables and timezone
- `homeassistant/config/` directory, created empty or with a minimal placeholder file if needed to preserve the directory in git
- `esphome/config/` directory, created empty or with a minimal placeholder file if needed to preserve the directory in git

The initial setup should avoid inventing application-specific configuration content unless it is required for the containers to boot cleanly. The goal is to provide structure first, not prematurely customize the applications.

## Networking and Access

- The stack is intended for access on the local network only
- Ports are published on the Docker host for browser/mobile access
- Tailscale is the remote access path for the phone, so public ingress is intentionally out of scope
- No reverse proxy is included in the initial foundation

## Operations and Bootstrap

The initial `README.md` should document:

- Folder conventions for adding more services later
- How to copy `.env.example` to `.env`
- How to start the stack with `docker compose up -d`
- How to validate the configuration with `docker compose config`
- Where Home Assistant and ESPHome will be reachable after startup
- How to update pinned image versions in the future

## Verification Strategy

The initial scaffold should be verified with lightweight operational checks:

- `docker compose config` must succeed
- The repo structure must match the documented service-oriented convention
- The Compose file must resolve bind mounts to `./homeassistant/config` and `./esphome/config`

Container runtime verification is useful, but the core acceptance target for this initial task is a valid scaffold that is ready to boot.

## Future Extensions

The structure should make it straightforward to add more service folders later, such as:

- MQTT broker
- Zigbee coordinator service
- Reverse proxy
- Backups
- Monitoring

Those additions should follow the same convention:

- one folder per service
- config/data mounts nested under that folder
- service defined in the root Compose file

## Recommended Implementation Outcome

The first implementation pass should produce:

- A root `docker-compose.yml`
- A root `.env.example`
- A revised `README.md`
- `homeassistant/config/`
- `esphome/config/`

That result is intentionally minimal, opinionated, and easy to extend.
