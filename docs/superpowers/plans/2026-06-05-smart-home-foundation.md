# Smart Home Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an initial Docker-based smart home foundation in this repository with pinned Home Assistant and ESPHome services, root-level Compose orchestration, service-local config folders, and bootstrap documentation.

**Architecture:** Keep the stack intentionally simple: one root `docker-compose.yml`, one top-level folder per service, and repo-local bind mounts for service configuration. Shared conventions such as pinned image tags and timezone live in `.env`, while validation uses `docker compose config` instead of application-specific test code.

**Tech Stack:** Docker Compose, Home Assistant container image, ESPHome container image, Markdown documentation

---

### Task 1: Scaffold repository layout and shared environment defaults

**Files:**
- Create: `.env.example`
- Create: `.gitignore`
- Create: `homeassistant/config/.gitkeep`
- Create: `esphome/config/.gitkeep`

- [ ] **Step 1: Write the initial shared environment file**

Create `.env.example` with pinned image tags and shared timezone:

```dotenv
TZ=Europe/Warsaw
HOMEASSISTANT_IMAGE=ghcr.io/home-assistant/home-assistant:2026.5.3
ESPHOME_IMAGE=ghcr.io/esphome/esphome:2026.5.2
```

- [ ] **Step 2: Ignore the local runtime environment file**

Create `.gitignore`:

```gitignore
.env
```

- [ ] **Step 3: Create the service config directories**

Create placeholder files so Git tracks the empty config directories:

```text
homeassistant/config/.gitkeep
esphome/config/.gitkeep
```

- [ ] **Step 4: Verify the scaffolded directories exist**

Run:

```bash
find homeassistant esphome -maxdepth 2 -type f | sort
sed -n '1,40p' .gitignore
```

Expected output:

```text
esphome/config/.gitkeep
homeassistant/config/.gitkeep
.env
```

- [ ] **Step 5: Commit the directory and env scaffold**

Run:

```bash
git add .env.example .gitignore homeassistant/config/.gitkeep esphome/config/.gitkeep
git commit -m "chore: add smart home service scaffold"
```

Expected output:

```text
[branch] chore: add smart home service scaffold
```

### Task 2: Add the root Compose stack with Home Assistant and ESPHome

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Write the Compose file**

Create `docker-compose.yml`:

```yaml
services:
  homeassistant:
    container_name: homeassistant
    image: ${HOMEASSISTANT_IMAGE}
    restart: unless-stopped
    environment:
      TZ: ${TZ}
    ports:
      - "8123:8123"
    volumes:
      - ./homeassistant/config:/config
    networks:
      - smarthome

  esphome:
    container_name: esphome
    image: ${ESPHOME_IMAGE}
    restart: unless-stopped
    environment:
      TZ: ${TZ}
    ports:
      - "6052:6052"
    volumes:
      - ./esphome/config:/config
    networks:
      - smarthome

networks:
  smarthome:
    name: smarthome
```

- [ ] **Step 2: Verify Compose resolves with the env file**

Run:

```bash
cp .env.example .env
docker compose config
```

Expected output:

```text
Validated Compose output showing:
- homeassistant image resolved to ghcr.io/home-assistant/home-assistant:2026.5.3
- esphome image resolved to ghcr.io/esphome/esphome:2026.5.2
- bind mounts for ./homeassistant/config and ./esphome/config
- published ports 8123 and 6052
```

- [ ] **Step 3: If validation fails because Docker is unavailable, capture the exact failure**

Run:

```bash
docker compose config
```

Expected failure mode if Docker is missing:

```text
docker: command not found
```

or if the daemon is unavailable:

```text
Cannot connect to the Docker daemon
```

In either case, stop and report the environment issue instead of assuming the stack is valid.

- [ ] **Step 4: Commit the Compose foundation**

Run:

```bash
git add docker-compose.yml
git commit -m "feat: add initial smart home compose stack"
```

Expected output:

```text
[branch] feat: add initial smart home compose stack
```

### Task 3: Rewrite the README for bootstrap and maintenance

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Replace the placeholder README with stack documentation**

Update `README.md` to:

```markdown
# hive

Docker-based smart home foundation with one folder per service and a root-level Compose stack.

## Services

- Home Assistant
- ESPHome

## Repository Layout

~~~text
.
├── docker-compose.yml
├── .env.example
├── .gitignore
├── homeassistant/
│   └── config/
└── esphome/
    └── config/
~~~

## Bootstrap

1. Copy the environment template:

   ~~~bash
   cp .env.example .env
   ~~~

2. Validate the Compose file:

   ~~~bash
   docker compose config
   ~~~

3. Start the stack:

   ~~~bash
   docker compose up -d
   ~~~

## Access

- Home Assistant: `http://<host-ip>:8123`
- ESPHome: `http://<host-ip>:6052`

Use Tailscale to reach the host remotely from your phone instead of exposing these services publicly.

## Updating Images

Update the pinned image tags in `.env`, then recreate the stack:

~~~bash
docker compose pull
docker compose up -d
~~~

## Adding More Services

Follow the same pattern:

- create a top-level folder for the service
- put service config/data inside that folder
- define the service in the root `docker-compose.yml`
```

- [ ] **Step 2: Verify the README content is present**

Run:

```bash
sed -n '1,220p' README.md
```

Expected output:

```text
README includes sections for Services, Repository Layout, Bootstrap, Access, Updating Images, and Adding More Services
```

- [ ] **Step 3: Commit the documentation update**

Run:

```bash
git add README.md
git commit -m "docs: add smart home bootstrap guide"
```

Expected output:

```text
[branch] docs: add smart home bootstrap guide
```

### Task 4: Run final verification on the full scaffold

**Files:**
- Verify: `.gitignore`
- Verify: `.env.example`
- Verify: `docker-compose.yml`
- Verify: `README.md`
- Verify: `homeassistant/config/.gitkeep`
- Verify: `esphome/config/.gitkeep`

- [ ] **Step 1: Verify the working tree contains the expected files**

Run:

```bash
find . -maxdepth 3 -type f | sort
```

Expected output:

```text
./.env
./.env.example
./.gitignore
./README.md
./docker-compose.yml
./esphome/config/.gitkeep
./homeassistant/config/.gitkeep
```

- [ ] **Step 2: Run full Compose validation again**

Run:

```bash
docker compose config
```

Expected output:

```text
Resolved Compose configuration with two services and one shared network, exit code 0
```

- [ ] **Step 3: Check Git status is clean except for intentional local-only files**

Run:

```bash
git status --short
```

Expected output:

```text
No output, or only the local `.env` file if it is intentionally left untracked
```

- [ ] **Step 4: Commit any final follow-up adjustments**

Run:

```bash
git add .gitignore .env.example docker-compose.yml README.md homeassistant/config/.gitkeep esphome/config/.gitkeep
git commit -m "chore: finalize smart home foundation"
```

Expected output:

```text
[branch] chore: finalize smart home foundation
```
