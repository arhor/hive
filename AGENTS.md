# Repository Guidelines

## Project Structure & Module Organization

The root combines infrastructure and application code:

- `docker-compose.yml` defines the Linux host-network stack; `docker-compose.mac.yml` provides Docker Desktop port
  mappings.
- `homeassistant/config/` and `esphome/config/` hold declarative smart-home and ESP32-CAM configuration.
- `app-cat-recognizer/` is a Quarkus/Kotlin service. Kotlin sources live under
  `src/main/kotlin/io/github/arhor/catrecognizer/`, the static web UI and application configuration under
  `src/main/resources/`, and tests under `src/test/kotlin/`.
- `lib-esphome-client/` is an ESPHome native API library. It contains the existing synchronous Kotlin implementation
  and an asynchronous Netty implementation in Java. Sources are under `src/main/kotlin/` and `src/main/java/`,
  Protobuf schemas under `src/main/proto/`, and Kotlin and Java tests under their corresponding `src/test/` trees.
- `docs/superpowers/specs/` contains design specifications; `docs/superpowers/plans/` contains implementation plans.

Generated Gradle, Quarkus, Home Assistant, and ESPHome runtime files must remain untracked.

## Build, Test, and Development Commands

Run Gradle commands from the repository root:

- `./gradlew :app-cat-recognizer:quarkusDev` — run the service with Quarkus hot reload.
- `./gradlew :app-cat-recognizer:test` — run recognizer tests.
- `./gradlew :app-cat-recognizer:build` — build the Quarkus JVM distribution used by its Dockerfile.
- `./gradlew :lib-esphome-client:test` — run ESPHome client tests.
- `./gradlew test` — run the normal test tasks for both modules.

The build uses a Java 25 toolchain. Dependency and plugin versions belong in `gradle/libs.versions.toml`, not module
build scripts.

Infrastructure commands:

- `docker compose config` — validate the effective Compose stack.
- `docker compose build cat-recognizer` — build the image after the Gradle application build.
- `docker compose up -d` — start the Linux stack.
- `docker compose -f docker-compose.yml -f docker-compose.mac.yml up -d` — start with the macOS override.
- `docker compose pull` — refresh upstream Home Assistant and ESPHome images.

## Coding Style & Naming Conventions

Use 4-space indentation in Kotlin, Java, and YAML. Keep packages under `io.github.arhor...`; use `PascalCase` for
types and `camelCase` for methods, functions, and properties. REST endpoint classes currently use the `*Controller`
suffix. Follow the style of the language and package being changed; do not mechanically convert between the
synchronous Kotlin and asynchronous Java ESPHome APIs.

Prefer explicit Compose, Home Assistant, and ESPHome configuration over helper scripts. Keep top-level service
directories lowercase and service-oriented.

## Testing Guidelines

Both modules run on JUnit 5:

- Recognizer tests use JUnit 5, Quarkus Test where container-backed behavior is needed, Rest Assured for HTTP, and
  Kotest assertions.
- ESPHome client tests include Kotlin `kotlin.test` suites and Java JUnit 5 suites.
- Test classes use the `*Test` suffix.

Add or update tests for every behavior change. Run the touched module's test task at minimum; run `./gradlew test`
when a change crosses module boundaries.

`LiveCameraIntegrationTest` is tagged `live` and uses a real camera endpoint, but the Gradle test task does not
currently exclude that tag. Treat it as a manual integration test and report this caveat when verification reaches
it. Never make routine or CI verification depend on access to that device. Do not add more fixed local addresses;
make new live-test configuration runtime-driven.

## Configuration and API Notes

Recognizer defaults live in `app-cat-recognizer/src/main/resources/application.properties`. Quarkus maps a property
such as `cat-recognizer.camera.native-api.host` to
`CAT_RECOGNIZER_CAMERA_NATIVE_API_HOST`. The default camera source is `NATIVE_API`; `HTTP_SNAPSHOT` is the alternative.
The bundled YOLO11 ONNX model is the active detector—there are no current stub detector modes or worker-enabled flag.

Application endpoints are rooted at `/api`. Health endpoints therefore use `/api/q/health/live` and
`/api/q/health/ready`. When adding endpoints, follow the existing controller tests and update the README.

## Commit & Pull Request Guidelines

Prefer focused, imperative Conventional Commit subjects such as `feat:`, `fix:`, `docs:`, `chore:`, or `refactor:`.
PR descriptions should identify affected modules, list verification commands, and link relevant documents from
`docs/superpowers/`. Include screenshots only for visible UI or dashboard changes.

## Security & Configuration

Never commit `.env`, `secrets.yaml`, Wi-Fi credentials, API encryption keys, OTA passwords, tokens, or host-specific
addresses. Use `.env.example` only for safe placeholders and use runtime configuration for local overrides. Before
editing ESPHome configuration, check that sensitive values are referenced through `!secret`. Prefer Tailscale or
another private network path over exposing smart-home services publicly.

Preserve unrelated local changes and generated device archives. In particular, do not edit or remove files under
`esphome/config/archive/` unless the task explicitly includes them.
