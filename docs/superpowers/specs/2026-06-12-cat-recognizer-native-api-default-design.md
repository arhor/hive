# Cat Recognizer Native API Default Design

## Goal

Use the ESPHome native API as the default camera source for `app-cat-recognizer`, backed by
`services/lib-esphome-client`, while keeping the existing HTTP snapshot source available as an explicit configuration
choice.

## Scope

The recognizer should continue to fetch frames through the existing `FrameClient` boundary. `NATIVE_API` becomes the
default `cat-recognizer.camera.source`, and `HTTP_SNAPSHOT` remains supported for operators who deliberately select it.

There is no automatic fallback between sources. A native API failure should be reported as a native frame fetch failure,
and an HTTP snapshot failure should be reported as an HTTP snapshot failure.

## Configuration

Default application configuration should set:

- `cat-recognizer.camera.source=NATIVE_API`
- `cat-recognizer.camera.native-api.host=esp32-cam.local`
- `cat-recognizer.camera.native-api.port=6053`
- native API connect/read timeouts and encryption settings

HTTP snapshot settings stay in the config mapping and properties file so `cat-recognizer.camera.source=HTTP_SNAPSHOT`
continues to work without code changes.

## Client Selection

Keep `FrameClientProducer` as the single selection point. It should select exactly one implementation from
`RecognizerConfig.Camera.source()`:

- `NATIVE_API` uses `EspHomeNativeFrameClient`
- `HTTP_SNAPSHOT` uses `SnapshotFrameClient`

The native client should build `EspHomeClientConfig` from `cat-recognizer.camera.native-api.*` and call
`DefaultEspHomeClient(config).connect().fetchCameraImage(single = true)`.

## Debug And Health

Existing readiness behavior remains based on worker state and does not need source-specific logic.

The debug config endpoint may keep reporting whether the HTTP snapshot URL is configured for compatibility, but the
runtime source should be visible in tests or diagnostics so the default change is easy to verify.

## Tests

Update focused tests to cover:

- config binding defaults to `NATIVE_API`
- native API host, port, timeout, and encryption config binding
- native client maps camera bytes and failures through `FramePayload` and `FrameSourceError`
- `FrameClientProducer` selects native and HTTP clients only according to explicit source config
- HTTP snapshot client tests continue to pass when `HTTP_SNAPSHOT` is selected
