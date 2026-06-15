# ESPHome Native Client Design

## Goal

Implement `lib-esphome-client` as a reusable Kotlin library for the ESPHome native API, using
`hjdhjd/esphome-client` as the behavioral reference, and migrate `app-cat-recognizer` away from the Node.js-native
client path toward JVM-native ESPHome communication.

The first usable slice should support plaintext ESPHome native API connections only. API encryption is intentionally
out of scope for this slice, but the library must keep a clear transport boundary so Noise encryption can be added later
without changing the app-facing API.

## Scope

In scope:

- Build a Kotlin/JVM ESPHome native API client under `lib-esphome-client`
- Use the existing checked-in ESPHome `api.proto` and generated protobuf classes
- Implement the plaintext ESPHome frame format over TCP
- Implement hello/connect, device info, camera image request/response, and clean disconnect
- Aggregate camera image chunks into one image byte array
- Add focused tests for frame coding, protocol flow, response routing, and camera image aggregation
- Integrate `app-cat-recognizer` through its existing `FrameClient` boundary
- Keep the existing HTTP snapshot client as a configurable fallback while native API support settles

Out of scope:

- Noise protocol / `api.encryption` support
- Full entity discovery, state subscriptions, commands, logs, Bluetooth, or voice assistant support
- Home Assistant integration changes
- ESPHome firmware changes beyond any local/manual operator choice to disable `api.encryption`
- Real-device automated tests in CI
- Reintroducing Node.js as a runtime dependency

## Reference Implementation

The Node implementation at `https://github.com/hjdhjd/esphome-client` is the reference for protocol behavior and
library shape, not a dependency to execute from the JVM service. The Kotlin implementation should port only the needed
plaintext protocol and camera workflow in this slice.

The reference library documents broad support for ESPHome message types and Noise encryption. This design deliberately
narrows the first JVM implementation to the cat recognizer's immediate need: fetching a camera frame from the ESP32-CAM
over the ESPHome native API.

## Recommended Architecture

Use a camera-focused plaintext native client.

This balances near-term usefulness with a stable library boundary. A protocol-wide port would add a large amount of
untested surface before the cat recognizer benefits from it. A one-off socket helper inside `app-cat-recognizer` would be
faster initially, but it would make future encryption and reuse harder and would waste the existing library module.

## Module Design

`lib-esphome-client` should expose a small synchronous Kotlin API:

```kotlin
interface EspHomeClient : AutoCloseable {
    fun connect(): EspHomeConnection
}

interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
}
```

Supporting public model types:

- `EspHomeClientConfig`: host, port, client name, connect timeout, read timeout, and optional plaintext password
- `EspHomeDeviceInfo`: selected fields from `DeviceInfoResponse`
- `EspHomeClientException`: base exception for protocol, transport, timeout, authentication, and disconnect failures

Internal components:

- `EspHomeTransport`: send and receive raw ESPHome protocol messages
- `PlaintextEspHomeTransport`: TCP socket transport using the plaintext frame format
- `EspHomeFrameCodec`: encode and decode ESPHome plaintext frames
- `EspHomeProtocolClient`: performs hello/connect, request/response exchange, disconnect, and camera chunk aggregation
- `EspHomeMessageType`: centralized mapping for the message IDs needed by this slice

The transport interface must carry message type plus protobuf payload bytes. It must not expose socket details to the
protocol client. A future encrypted implementation should be able to satisfy the same interface after the Noise
handshake.

## Plaintext Protocol

The plaintext transport uses the ESPHome native API frame shape:

```text
[0x00 indicator][payload-size varint][message-type varint][protobuf payload]
```

The frame codec should be independent of sockets and covered by unit tests for:

- one-byte and multi-byte varints
- empty payload frames
- frames with known message IDs
- truncated frame handling
- invalid indicator handling

Frame decoding should reject malformed data with a protocol exception. It should avoid unbounded allocations by applying
a conservative maximum payload size. The initial maximum can be sized for camera JPEG chunks while still being explicit
and configurable internally.

## Protocol Flow

Connection startup:

1. Open TCP connection to `host:port`, defaulting to port `6053`.
2. Send `HelloRequest` with a repo-specific client name and supported API version.
3. Read `HelloResponse` and reject unsupported major protocol versions.
4. Send `ConnectRequest`, including plaintext password only if configured.
5. Read `ConnectResponse` and reject invalid password.

Camera fetch:

1. Send `CameraImageRequest(single = true, stream = false)`.
2. Read messages until `CameraImageResponse.done == true`.
3. Append each non-empty `CameraImageResponse.data` chunk in order.
4. Return the aggregated bytes.
5. Ignore unrelated ping/device messages only when doing so is explicitly safe; unexpected terminal messages should fail
   the request.

Device info:

1. Send `DeviceInfoRequest`.
2. Read `DeviceInfoResponse`.
3. Map selected fields into `EspHomeDeviceInfo`.

Disconnect:

1. Best-effort send `DisconnectRequest`.
2. Close the underlying socket in all cases.

## App Integration

`app-cat-recognizer` should keep depending on its existing `FrameClient` abstraction.

Add a native implementation:

- `EspHomeNativeFrameClient` in the cat recognizer module
- It depends on `lib-esphome-client`
- It maps native camera bytes into `FramePayload`
- It maps client/protocol failures into existing `FrameSourceError`
- It keeps observed time behavior aligned with the current snapshot client

Keep `SnapshotFrameClient` available and make the source configurable:

```properties
cat-recognizer.camera.source=HTTP_SNAPSHOT
cat-recognizer.camera.snapshot-url=http://esp32-cam.local/snapshot
cat-recognizer.camera.native-api.host=esp32-cam.local
cat-recognizer.camera.native-api.port=6053
cat-recognizer.camera.native-api.connect-timeout=2S
cat-recognizer.camera.native-api.read-timeout=5S
```

The default should remain conservative during migration. `HTTP_SNAPSHOT` can stay default until native API is manually
verified against the device. The operator can opt into `NATIVE_API` through Quarkus config.

## Configuration And Security

The repo must not commit real private IPs, Wi-Fi credentials, or host-specific secrets.

Because this slice skips `api.encryption`, real device verification requires the operator to use firmware/config where
plaintext native API is available. The application config should not store the existing ESPHome encryption key for this
first slice.

Timeouts should remain configurable at the app boundary. Library defaults should be safe, but app configuration should
own operational values.

## Testing

Use test-first implementation.

Library tests:

- `EspHomeFrameCodecTest`: plaintext frame encode/decode and malformed input behavior
- `EspHomeProtocolClientTest`: hello/connect and disconnect behavior over fake transport
- `EspHomeCameraImageTest`: chunk aggregation, done handling, empty image failure, and unexpected message handling
- `EspHomeDeviceInfoTest`: device info request/response mapping

App tests:

- config binding for camera source and native API settings
- native frame client maps image bytes into `FramePayload`
- native frame client maps client exceptions into retriable `FrameSourceError`
- existing snapshot client tests remain valid

Verification commands:

```bash
./gradlew :lib-esphome-client:test
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

Real-device verification remains manual and should be documented in the final handoff if performed.

## Error Handling

Library exceptions should distinguish:

- transport connection failures
- socket read/write timeouts
- malformed frame/protobuf data
- invalid password
- unsupported protocol version
- device disconnect before response completion
- camera response completed without image bytes

The app should continue exposing camera acquisition failures as `FrameSourceError` with code `FRAME_FETCH_FAILED` and
`retriable = true`, matching the current HTTP snapshot behavior.

## Future Encryption Path

Encryption should be added later by implementing an encrypted `EspHomeTransport` that performs the Noise handshake and
then exposes the same message-level send/receive contract as `PlaintextEspHomeTransport`.

The app-facing API should not change when encryption is added. Expected future configuration additions:

```properties
cat-recognizer.camera.native-api.encryption.enabled=true
cat-recognizer.camera.native-api.encryption.key=<base64 key from local secret/env only>
```

The future encrypted transport can be validated against the same protocol-client tests by reusing the fake transport
tests and adding Noise-specific handshake/vector tests separately.

## Acceptance Criteria

- `lib-esphome-client` has no Node.js runtime dependency
- plaintext ESPHome frames are encoded and decoded by tested Kotlin code
- the library can perform hello/connect, device info, camera image fetch, and disconnect over a transport abstraction
- `app-cat-recognizer` can select native API frame acquisition through configuration
- HTTP snapshot acquisition remains available as a fallback
- all added behavior has focused unit tests
- no real secrets or private device addresses are committed
