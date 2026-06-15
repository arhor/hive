# Repository Guidelines

## Project Structure & Module Organization

The repository root holds both infrastructure and application code. Infrastructure lives in `docker-compose.yml`,
`homeassistant/config/`, `esphome/config/`, and `.env.example`. Two Gradle modules sit directly at the root:

- **`app-cat-recognizer/`** — Quarkus + Kotlin service that detects cats via ONNX/YOLOv8 and streams ESP32-CAM frames.
  Sources in `src/main/kotlin/io/github/arhor/catrecognizer/`, config in `src/main/resources/`,
  tests in `src/test/kotlin/io/github/arhor/catrecognizer/`.
- **`lib-esphome-client/`** — Pure Kotlin library for ESPHome Protobuf communication (including Noise encryption).
  Sources in `src/main/kotlin/io/github/arhor/esphome/client/`, protos in `src/main/proto/`,
  tests in `src/test/kotlin/io/github/arhor/esphome/client/`.

Design specifications belong in `docs/superpowers/specs/` and implementation plans in `docs/superpowers/plans/`.

## Build, Test, and Development Commands

Run all Gradle commands from the repository root (the Gradle wrapper is at `./gradlew`):

- `./gradlew :app-cat-recognizer:quarkusDev` — hot-reload dev mode for the Quarkus service.
- `./gradlew :app-cat-recognizer:test` — runs the JVM unit test suite.
- `./gradlew :app-cat-recognizer:build` — produces the full Quarkus build artifact.
- `./gradlew :lib-esphome-client:test` — runs the ESPHome client library tests.
- `./gradlew test` — runs all tests across both modules.

Infrastructure commands (also from the repository root):

- `docker compose config` — validates the Compose stack.
- `docker compose up -d` — starts Home Assistant, ESPHome, and the cat-recognizer service.
- `docker compose pull` — refreshes pinned images before restart.

## Coding Style & Naming Conventions

Prefer 4-space indentation in Kotlin and YAML. Keep Kotlin packages under `io.github.arhor...`, classes in `PascalCase`,
functions and properties in `camelCase`, and REST controllers named `*Resource`. Use lowercase, service-oriented
directory
names such as `homeassistant/` and `esphome/`. Keep Compose and Home Assistant changes explicit; avoid hidden magic in
scripts when declarative config will do.

The version catalog at `gradle/libs.versions.toml` is the single source of truth for dependency versions; do not
hardcode
versions in `build.gradle.kts` files.

## Testing Guidelines

Both modules use JUnit 5. `app-cat-recognizer` tests are annotated with `@QuarkusTest` and use Rest Assured plus Kotest
assertions. `lib-esphome-client` tests use `kotlin.test`. All test classes follow the `*Test` naming convention — there
are currently no `*IT` integration test classes. Add or update tests with every behavior change. Run
`./gradlew :app-cat-recognizer:test` before opening a PR that touches the Quarkus service; run
`./gradlew :lib-esphome-client:test` before opening a PR that touches the ESPHome client library.

## Commit & Pull Request Guidelines

Prefer Conventional Commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`. Keep commits focused and
imperative — for example: `fix: pin esphome image` or `docs: update cat recognizer setup`. PRs should describe the
affected module, list verification commands, link any related spec or plan from `docs/superpowers/`, and include
screenshots only when UI or dashboard behavior changes.

## Security & Configuration Tips

Do not commit `.env` or host-specific secrets. Keep image tags and credentials configurable through `.env` (use
`.env.example` as the template). Prefer Tailscale or another private network path over exposing smart-home services
directly to the public internet.
