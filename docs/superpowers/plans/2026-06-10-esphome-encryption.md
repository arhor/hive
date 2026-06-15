# ESPHome Encryption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explicit ESPHome Noise encryption support to the Kotlin native client while preserving plaintext as the default.

**Architecture:** Implement ESPHome Noise crypto as internal library code, add an encrypted transport that satisfies the existing `EspHomeTransport` interface, and wire app configuration into `EspHomeClientConfig`. The protocol client remains transport-agnostic.

**Tech Stack:** Kotlin/JVM 25, JDK crypto (`X25519`, `ChaCha20-Poly1305`, `HmacSHA256`), Gradle, Quarkus config mapping, kotlin-test.

---

## File Structure

- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/noise/NoiseConstants.kt` for
  protocol constants.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/noise/NoiseKeyMaterial.kt` for
  base64 key decoding.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/noise/NoiseCipherState.kt` for
  ChaCha20-Poly1305 state.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/noise/NoiseHandshakeState.kt` for
  NNpsk0 handshake state.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EncryptedEspHomeFrameCodec.kt` for
  encrypted frame wrapping.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EncryptedEspHomeTransport.kt` for
  socket transport.
- Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClientConfig.kt` to add encryption
  config.
- Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt` to choose encrypted
  transport when enabled.
- Modify `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt` to add native API
  encryption config.
- Modify `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/EspHomeNativeFrameClient.kt` to
  pass encryption settings through.
- Modify `app-cat-recognizer/src/main/resources/application.properties` to set encryption disabled by default.

## Task 1: Noise Key And Cipher

- [ ] Write failing tests for base64 key validation and cipher encrypt/decrypt/authentication behavior.
- [ ] Verify the tests fail because `NoiseKeyMaterial` and `NoiseCipherState` do not exist.
- [ ] Implement key decoding and ChaCha20-Poly1305 cipher state.
- [ ] Run focused tests and verify they pass.

## Task 2: Noise Handshake

- [ ] Write failing tests for initiator/responder NNpsk0 handshake compatibility using the ESPHome prologue.
- [ ] Verify the tests fail because `NoiseHandshakeState` does not exist.
- [ ] Implement handshake state, HKDF, X25519 key agreement, and cipher splitting.
- [ ] Run focused tests and verify they pass.

## Task 3: Encrypted Frames And Transport

- [ ] Write failing tests for encrypted frame wrapping and a loopback encrypted transport send/receive.
- [ ] Verify the tests fail because encrypted frame/transport types do not exist.
- [ ] Implement encrypted frame codec and encrypted socket transport.
- [ ] Run focused tests and verify they pass.

## Task 4: Library Config And Transport Selection

- [ ] Write failing tests for default plaintext config, encrypted config validation, and `DefaultEspHomeClient` transport selection.
- [ ] Verify the tests fail because encryption config is not present.
- [ ] Add `EspHomeEncryptionConfig` and transport selection logic.
- [ ] Run focused tests and full `:lib-esphome-client:test`.

## Task 5: App Config Wiring

- [ ] Write failing config binding and native frame client tests for encryption settings.
- [ ] Verify the tests fail because app encryption config is not present.
- [ ] Add Quarkus config mapping, default disabled property, and pass settings into `EspHomeClientConfig`.
- [ ] Run focused app tests and full `:app-cat-recognizer:test`.

## Task 6: Verification And Review

- [ ] Run `./gradlew :lib-esphome-client:test`.
- [ ] Run `./gradlew :app-cat-recognizer:test`.
- [ ] Run `./gradlew :app-cat-recognizer:build`.
- [ ] Request code review and fix important findings with regression tests.
- [ ] Confirm no committed config contains a real encryption key.
