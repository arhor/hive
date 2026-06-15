# ESPHome Entity Discovery And State Subscription Design

## Goal

Grow `lib-esphome-client` toward full `hjdhjd/esphome-client` parity by adding read-only support for every
entity discovery and state subscription message family present in the checked-in ESPHome `api.proto`.

This slice should preserve the current camera-focused API while introducing a broader Java/Kotlin client surface that can
discover key-bearing entities and stream entity state updates over either plaintext or encrypted transports.

## Scope

In scope:

- Add entity discovery for all `LIST_ENTITIES_*_RESPONSE` message types in `api.proto`
- Add state subscription dispatch for all entity `*_STATE_RESPONSE` messages and `EVENT_RESPONSE`
- Keep support transport-agnostic, so plaintext and encrypted connections behave the same after connect
- Expose typed Kotlin models for every supported entity and state family
- Preserve access to generated protobuf messages inside models where a full hand-written mapping would add churn
- Respond to ping requests while waiting for discovery and state messages
- Add focused TDD coverage for discovery aggregation, state dispatch, ping handling, and unexpected message handling

Out of scope for this slice:

- Entity command requests
- Log subscriptions
- User-defined service execution
- Home Assistant service/state bridging
- Bluetooth proxy messages
- Voice assistant messages
- Dynamic Noise key update
- Automatic encrypted-to-plaintext fallback
- Real-device CI tests

## Public API

Extend `EspHomeConnection` with discovery and state subscription methods:

```kotlin
interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
    fun listEntities(): List<EspHomeEntity>
    fun subscribeStates(handler: EspHomeStateHandler)
}
```

`listEntities()` is a synchronous request/response operation. It sends `LIST_ENTITIES_REQUEST`, reads entity discovery
responses until `LIST_ENTITIES_DONE_RESPONSE`, and returns entities in the order reported by the device.

`subscribeStates(handler)` sends `SUBSCRIBE_STATES_REQUEST`, then enters a blocking read loop until the connection closes
or the caller interrupts/closes the connection. Each state message is decoded and delivered to the supplied handler.

The initial handler shape should be simple:

```kotlin
fun interface EspHomeStateHandler {
    fun onState(state: EspHomeState)
}
```

This keeps the API usable from Java and Kotlin without introducing coroutines or reactive libraries in this slice.

## Entity And State Models

Add sealed model families:

```kotlin
sealed interface EspHomeEntity {
    val key: Int
    val objectId: String
    val name: String
}

sealed interface EspHomeState {
    val key: Int
}
```

Each entity/state type should have a data class named after the ESPHome family, for example:

- `EspHomeBinarySensorEntity` / `EspHomeBinarySensorState`
- `EspHomeCameraEntity`
- `EspHomeClimateEntity` / `EspHomeClimateState`
- `EspHomeCoverEntity` / `EspHomeCoverState`
- `EspHomeDateEntity` / `EspHomeDateState`
- `EspHomeDateTimeEntity` / `EspHomeDateTimeState`
- `EspHomeEventEntity` / `EspHomeEventState`
- `EspHomeFanEntity` / `EspHomeFanState`
- `EspHomeLightEntity` / `EspHomeLightState`
- `EspHomeLockEntity` / `EspHomeLockState`
- `EspHomeMediaPlayerEntity` / `EspHomeMediaPlayerState`
- `EspHomeNumberEntity` / `EspHomeNumberState`
- `EspHomeSelectEntity` / `EspHomeSelectState`
- `EspHomeSensorEntity` / `EspHomeSensorState`
- `EspHomeSirenEntity` / `EspHomeSirenState`
- `EspHomeSwitchEntity` / `EspHomeSwitchState`
- `EspHomeTextEntity` / `EspHomeTextState`
- `EspHomeTextSensorEntity` / `EspHomeTextSensorState`
- `EspHomeTimeEntity` / `EspHomeTimeState`
- `EspHomeUpdateEntity` / `EspHomeUpdateState`
- `EspHomeValveEntity` / `EspHomeValveState`
- `EspHomeAlarmControlPanelEntity` / `EspHomeAlarmControlPanelState`
- `EspHomeButtonEntity` for button discovery

`EspHomeCameraEntity`, `EspHomeButtonEntity`, and `EspHomeServiceEntity` are discovery-only in this slice because the
checked-in `api.proto` does not define corresponding entity state response messages for them.

Discovery includes `ListEntitiesServicesResponse`, but services are not entities with state. Represent service discovery
as `EspHomeServiceEntity` under `EspHomeEntity` so `listEntities()` reflects the complete discovery stream. Executing
those services remains out of scope.

The typed models should expose the common fields directly. To avoid a large and brittle mapping pass in the first parity
slice, each model should also retain the generated protobuf response:

```kotlin
data class EspHomeSensorEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesSensorResponse,
) : EspHomeEntity
```

State models should follow the same pattern and expose obvious primary state fields directly where they are stable, while
retaining the raw generated message for full protocol access. Home Assistant and Bluetooth messages that happen to contain
`StateResponse` in their names are not part of `EspHomeState`; those protocol families remain out of scope.

## Message Routing

Expand `EspHomeMessageType` to include every entity discovery and state message used by this slice:

- Base discovery flow: `LIST_ENTITIES_REQUEST`, `LIST_ENTITIES_DONE_RESPONSE`
- All `LIST_ENTITIES_*_RESPONSE` message types in `api.proto`
- `SUBSCRIBE_STATES_REQUEST`
- All entity `*_STATE_RESPONSE` message types in `api.proto`
- `EVENT_RESPONSE`

Keep the protocol client message-oriented. `EspHomeProtocolClient` should continue to depend only on `EspHomeTransport`
and generated protobuf classes; it should not know whether frames are plaintext or encrypted.

Discovery routing:

1. Send `ListEntitiesRequest`.
2. Read frames until `ListEntitiesDoneResponse`.
3. Parse every known discovery response into the matching `EspHomeEntity`.
4. Reply to `PingRequest` with `PingResponse`.
5. Fail with `EspHomeProtocolException` on unexpected message types.

State subscription routing:

1. Send `SubscribeStatesRequest`.
2. Read frames in a loop.
3. Parse every known state response into the matching `EspHomeState`.
4. Deliver each decoded state to `EspHomeStateHandler`.
5. Reply to `PingRequest` with `PingResponse`.
6. Treat `DisconnectRequest` or transport close as connection termination.
7. Fail with `EspHomeProtocolException` on unexpected non-maintenance message types.

## Error Handling

Use existing exception types:

- `EspHomeProtocolException` for unknown or malformed discovery/state messages
- `EspHomeTransportException` for read/write failures from the transport
- `EspHomeAuthenticationException` remains startup-only

`listEntities()` should never return partial results after a protocol error. If a device disconnects before
`LIST_ENTITIES_DONE_RESPONSE`, the call should fail.

`subscribeStates(handler)` is intentionally blocking. A handler exception should stop the subscription and propagate to
the caller, because silently swallowing consumer failures would hide broken automation logic.

## Testing Strategy

Use TDD for every behavior change.

Library tests:

- `EspHomeProtocolClientTest` adds discovery aggregation tests using fake transport frames
- Discovery tests cover at least one entity from each discovery family and verify all are routed
- State tests cover at least one state from each state family and verify handler dispatch order
- Ping tests verify discovery and subscription respond with `PING_RESPONSE` while waiting
- Unexpected-message tests verify discovery/subscription fail clearly on unrelated message types
- Close/termination tests verify subscription exits or propagates transport failure predictably

Extract mapping logic into focused helpers if it keeps `EspHomeProtocolClient` from becoming a large message switch. Cover
those helpers with `EspHomeEntityMapperTest` and `EspHomeStateMapperTest`.

Verification commands:

```bash
./gradlew :lib-esphome-client:test
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

## Acceptance Criteria

- Existing camera and device-info behavior remains compatible
- `listEntities()` returns typed entries for every discovery message family present in `api.proto`
- `subscribeStates(handler)` dispatches typed entries for every state message family present in `api.proto`
- Discovery and subscription work through the existing transport abstraction
- Ping handling remains active during long-running reads
- Tests are written before implementation and cover all new routing families
- No Node.js runtime dependency is introduced
