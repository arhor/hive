# Cat Recognizer Quarkus Simplification Design

**Date:** 2026-06-08

## Goal

Simplify `app-cat-recognizer` into a small, tiered Quarkus service that uses:

- `client` for camera snapshot HTTP access
- `service` for one-pass recognition and state updates
- `web` for HTTP and health endpoints
- Quarkus Scheduler for periodic execution
- SmallRye Fault Tolerance for retry and timeout at the snapshot-client boundary
- a single OpenCV detector implementation instead of configurable detector modes

The purpose of this change is to remove custom lifecycle logic, detector indirection, and configuration that no longer
provides useful flexibility.

## Problem Statement

The current in-progress refactor is moving the codebase toward a better package shape, but the service still carries
older abstractions and custom runtime behavior:

- a homemade worker lifecycle with its own executor, sleep loop, startup/shutdown handling, and failure backoff
- configuration flags for enabling or disabling the worker even though the worker is the entire purpose of the service
- multiple detector modes plus a detector factory, even though the service is now expected to use OpenCV directly
- tests that were renamed during the refactor but still point at stale package names and deleted concepts

This complexity is not justified by the actual runtime needs of the service. Quarkus already provides scheduler and
fault-tolerance tooling for the parts that are currently implemented manually.

## Scope

This design covers:

- replacing custom worker lifecycle code with Quarkus scheduling
- moving retry and timeout behavior to SmallRye Fault Tolerance
- simplifying recognition to a single OpenCV detector path
- removing detector-mode configuration and factory indirection
- simplifying in-memory state to only the data that still serves the HTTP and health surfaces
- moving tests into packages that match the refactored production structure
- updating endpoint and health behavior to reflect the simplified runtime model

This design does not cover:

- real cat-classification heuristics or ML model integration
- persistence, metrics, or event publication
- Home Assistant, MQTT, ESPHome, or Docker Compose behavior changes
- changing the repository-level layout

## Constraints

- Keep the intentional tiered package direction: `client`, `domain`, `service`, `web`
- Prefer Quarkus-native features over custom infrastructure where Quarkus already solves the problem
- Keep the HTTP surface small and operationally focused
- Keep failure mapping centralized so the rest of the application does not need multiple error-handling paths
- Keep production code simpler after the change than before it

## Alternatives Considered

### 1. Recommended: fault tolerance on `SnapshotFrameClient.fetchFrame()`

Use `quarkus-smallrye-fault-tolerance` and put retry and timeout on the snapshot HTTP call, while leaving
`CatRecognitionJob` as a thin scheduled trigger.

Why this is the right cut:

- the unreliable boundary is camera communication, not the full recognition flow
- scheduled runs and manual runs share the same retry and timeout behavior
- retries stay scoped to frame acquisition instead of repeating unrelated state writes
- Quarkus supports runtime overrides for fault-tolerance policy through native configuration

### 2. Fault tolerance on `CatRecognitionService.runRecognition()`

Rejected because it is too broad. Retrying the whole recognition pass would re-run detector execution and state-updating
logic when only the camera fetch is actually unreliable.

### 3. Fault tolerance only on the scheduled job

Rejected because it would make scheduled and manual recognition behave differently without a strong operational reason.

## Recommended Architecture

### Runtime Shape

The service should become:

- `CatRecognitionJob`: a thin scheduled entrypoint
- `SnapshotFrameClient`: the HTTP client for camera snapshots, with fault tolerance
- `OpenCvCatDetector`: the only detector bean
- `CatRecognitionService`: one recognition pass that fetches a frame, runs detection, maps outcomes, and records state
- `LatestRecognitionState`: an in-memory cache for latest result and operational status
- `RecognitionController`, `DebugController`, and health checks: thin adapters over service state and configuration

There should be no custom worker loop, no manually managed executor, and no lifecycle bean whose main job is to emulate
a scheduler.

### Scheduling

Use Quarkus Scheduler in `CatRecognitionJob`.

Responsibilities of the job:

- trigger recognition on a fixed cadence
- avoid overlapping executions if that remains operationally useful

Responsibilities the job should not own:

- retry policy
- timeout policy
- thread management
- startup/shutdown orchestration beyond what Quarkus already does
- custom success/failure backoff logic

`cat-recognizer.worker.enabled` should be removed. If the job should not run, the service itself should not run.

`cat-recognizer.worker.failure-backoff` should also be removed because retry timing belongs to fault-tolerance policy,
not to a custom worker loop.

`cat-recognizer.worker.poll-interval` remains useful as scheduler cadence.

`cat-recognizer.worker.initial-delay` should be removed as unnecessary configuration unless implementation proves there
is a concrete startup problem that requires it. The target shape for this cleanup is one cadence setting, not a mini
scheduling DSL.

### Fault Tolerance

`SnapshotFrameClient.fetchFrame()` should be the fault-tolerant boundary.

Start with:

- `@Retry`
- `@Timeout`

This matches the Quarkus SmallRye Fault Tolerance guide, which frames these annotations around unreliable external
communication and supports runtime policy overrides through Quarkus-native configuration.
Source: https://quarkus.io/guides/smallrye-fault-tolerance

The initial implementation does not need a custom failure-backoff mechanism. Retry count, retry delay, and timeout
should be controlled through Quarkus configuration rather than custom code paths.

`@CircuitBreaker` is optional and should only be added if repeated camera failures create a clear operational need for
fast-fail behavior.

### Detection Simplification

The service should no longer support multiple detector modes.

Remove:

- `DetectionMode`
- `RecognizerConfig.detection().mode()`
- mode-based detector tests
- `CatDetectorFactory`
- `StubCatDetector`
- any config and debug output that only exists to surface detector mode selection

Keep:

- `OpenCvCatDetector` as the one real detector implementation

Remove `CatDetector` too. Once detector modes and factory selection are gone, the interface stops buying anything and
only preserves abstraction for its own sake. `CatRecognitionService` should depend directly on `OpenCvCatDetector`.

### State Model

`LatestRecognitionState` should keep only data that serves the API and health surfaces:

- latest recognition result
- last successful observation time
- last error
- consecutive failure count

Remove worker lifecycle flags.

Specifically:

- remove `workerEnabled`
- remove `workerRunning`

The default state should stay safe and empty at startup.

### Controllers and Health Checks

`RecognitionController` should continue to expose:

- latest recognition state
- a manual trigger endpoint for explicit operational/debug runs

The response should stop exposing fields that only existed for deleted concepts, especially detector-mode and
worker-enabled flags.

`DebugController` should expose only useful runtime summary values, such as:

- poll interval
- whether a snapshot URL is configured
- whether manual trigger is enabled

Health checks should continue to use cached state only. They must not trigger live recognition work.

Health should describe actual service condition:

- warming up
- fresh
- stale
- failing due to repeated snapshot or detector failures

Health should not describe custom worker lifecycle status because that lifecycle is being removed.

## Error Handling

`SnapshotFrameClient` should keep mapping camera and HTTP issues into `FrameSourceError`.

`CatRecognitionService` should remain the place that maps:

- successful OpenCV detection outcomes to domain results
- frame-source errors to `UNKNOWN` plus the upstream error code
- unexpected detector failures to `DETECTOR_FAILED`

The service should not reintroduce custom retry logic. Fault tolerance handles transient fetch failures before they
become mapped domain failures.

## Configuration

The target configuration after simplification should look roughly like this:

- camera snapshot URL and HTTP timeouts
- scheduler cadence
- state stale-after threshold
- debug/manual-trigger flag
- fault-tolerance settings for the snapshot client method

The following should be removed:

- worker enabled flag
- worker failure backoff
- worker initial delay
- detection mode
- unknown-on-error

Fault-tolerance policy should be configurable through Quarkus-native keys for the snapshot client method rather than
custom recognizer config.

## Testing Strategy

### Package Alignment

Tests should be moved so package declarations match the refactored production layout:

- client tests under `.../client`
- service tests under `.../service`
- web tests under `.../web`
- health and config tests under packages that match the actual production classes

Tests should not keep stale package names after the production classes moved.

### Behavioral Coverage

Keep and update tests for:

- `SnapshotFrameClient` HTTP fetch behavior and error mapping
- `OpenCvCatDetector` decode and failure behavior
- `CatRecognitionService` mapping of success, frame fetch failure, and detector failure
- `LatestRecognitionState` defaults, success handling, and failure counting
- recognition HTTP endpoints
- debug endpoint
- health endpoint behavior based on cached state

Remove or rewrite tests for deleted concepts:

- `WorkerLifecycle`
- detector-mode configuration
- stub-detector branches
- state fields that no longer exist

### Verification Commands

Run:

```bash
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:quarkusIntTest
./gradlew :app-cat-recognizer:build
```

## Acceptance Criteria

- The service uses Quarkus Scheduler instead of a custom worker lifecycle implementation.
- The service uses `quarkus-smallrye-fault-tolerance` for snapshot-client retry and timeout.
- `cat-recognizer.worker.enabled` and `cat-recognizer.worker.failure-backoff` are removed.
- Detector-mode configuration and factory indirection are removed.
- The service uses OpenCV directly through one detector implementation.
- State, debug output, and recognition payloads no longer expose deleted worker or detector-mode concepts.
- Tests live in packages that match the refactored production structure.
- Tests for deleted concepts are removed or rewritten instead of being renamed superficially.
- JVM and integration verification pass for the module after the cleanup.

## Expected Result

After this change, `app-cat-recognizer` should be easier to understand as:

- a scheduled Quarkus service
- a single camera snapshot client with Quarkus-managed resiliency
- a single OpenCV detection path
- a small state cache
- a thin operational HTTP surface

The code should be simpler not because responsibilities disappeared, but because each responsibility is handled in the
layer or framework that actually owns it.
