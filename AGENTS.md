# Repository Guidelines

## Project Structure & Module Organization

This repository has two active layers. Root-level infrastructure lives in `docker-compose.yml`, `homeassistant/config/`,
`esphome/config/`, and `.env.example`. Application code lives under `services/`, currently with one Gradle module:
`app-cat-recognizer/`. Kotlin sources are in `src/main/kotlin`, config in `src/main/resources`, JVM tests in
`src/test/kotlin`, and Quarkus integration/native tests in `src/native-test/kotlin`. Design and plan notes belong in
`docs/superpowers/`.

## Build, Test, and Development Commands

Use the repo root for infrastructure commands:

- `docker compose config` validates the Compose stack.
- `docker compose up -d` starts Home Assistant and ESPHome.
- `docker compose pull` refreshes pinned images before restart.

Use `services/` for application work:

- `./gradlew :app-cat-recognizer:quarkusDev` runs the service with hot reload.
- `./gradlew :app-cat-recognizer:test` runs the JVM test suite.
- `./gradlew :app-cat-recognizer:quarkusIntTest` runs integration tests.
- `./gradlew :app-cat-recognizer:build` produces the full Quarkus build artifact.

## Coding Style & Naming Conventions

Prefer 4-space indentation in Kotlin and YAML. Keep Kotlin packages under `io.github.arhor...`, classes in `PascalCase`,
functions and properties in `camelCase`, and REST resources named `*Resource`. Use lowercase, service-oriented directory
names such as `homeassistant/` and `esphome/`. Keep Compose and Home Assistant changes explicit; avoid hidden magic in
scripts when declarative config will do.

## Testing Guidelines

Quarkus tests use JUnit 5 with Rest Assured. Name JVM tests `*Test` and integration/native tests `*IT`, matching the
existing `GreetingResourceTest` and `GreetingResourceIT` pattern. Add or update tests with every behavior change. Run
`:app-cat-recognizer:test` before opening a PR; run `:app-cat-recognizer:quarkusIntTest` when touching HTTP behavior,
packaging, or container settings.

## Commit & Pull Request Guidelines

Recent history mixes plain subjects with Conventional Commit prefixes, but `feat:`, `fix:`, `docs:`, and `chore:` are
already in use and should be preferred. Keep commits focused and imperative, for example: `fix: pin esphome image` or
`docs: update cat recognizer setup`. PRs should describe the affected area, list verification commands, link any related
issue or spec, and include screenshots only when UI/dashboard behavior changes.

## Security & Configuration Tips

Do not commit `.env` or host-specific secrets. Keep image tags configurable through environment files, and prefer
Tailscale or another private network path over exposing smart-home services directly to the public internet.
