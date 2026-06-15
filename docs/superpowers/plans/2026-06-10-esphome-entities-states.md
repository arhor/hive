# ESPHome Entity Discovery And State Subscription Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add read-only ESPHome entity discovery and state subscription support for every entity family present in the checked-in `api.proto`.

**Architecture:** Keep the existing transport boundary unchanged. Add public typed model families, internal mappers from message IDs plus protobuf payloads to those models, then extend `EspHomeProtocolClient` with synchronous `listEntities()` and blocking `subscribeStates(handler)` operations.

**Tech Stack:** Kotlin/JVM, Gradle, generated Java protobuf classes from `lib-esphome-client/src/main/proto/api.proto`,
Kotlin test.

---

## File Structure

- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeEntity.kt`
  - Public sealed entity models and common `EspHomeEntity` interface.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeState.kt`
  - Public sealed state models and `EspHomeStateHandler`.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapper.kt`
  - Internal mapping from discovery message IDs and payloads to typed entities.
- Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapper.kt`
  - Internal mapping from state message IDs and payloads to typed states.
- Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt`
  - Add `listEntities()` and `subscribeStates(handler)` to `EspHomeConnection`.
- Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageType.kt`
  - Add all discovery and entity state message IDs used by this slice.
- Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClient.kt`
  - Implement discovery aggregation and state subscription loop.
- Modify `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClientTest.kt`
  - Add protocol-level tests for discovery, subscription, ping, and unexpected messages.
- Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageTypeTest.kt`
  - Verify message ID constants against `api.proto`.
- Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapperTest.kt`
  - Verify every discovery message family maps to the expected public model.
- Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapperTest.kt`
  - Verify every entity state message family maps to the expected public model.

## Task 1: Public API And Model Surface

**Files:**

- Modify: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeEntity.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeState.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/EspHomeModelApiTest.kt`

- [ ] **Step 1: Write the failing public API test**

Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/EspHomeModelApiTest.kt`:

```kotlin
package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.proto.BinarySensorStateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class EspHomeModelApiTest {

    @Test
    fun `entity models expose common fields and raw protobuf`() {
        val raw = ListEntitiesBinarySensorResponse.newBuilder()
            .setKey(11)
            .setObjectId("motion")
            .setName("Motion")
            .build()

        val entity: EspHomeEntity = EspHomeBinarySensorEntity(
            key = raw.key,
            objectId = raw.objectId,
            name = raw.name,
            raw = raw,
        )

        assertEquals(11, entity.key)
        assertEquals("motion", entity.objectId)
        assertEquals("Motion", entity.name)
        assertSame(raw, (entity as EspHomeBinarySensorEntity).raw)
    }

    @Test
    fun `discovery only entities are part of entity model family`() {
        val cameraRaw = ListEntitiesCameraResponse.newBuilder()
            .setKey(44)
            .setObjectId("front_camera")
            .setName("Front Camera")
            .build()
        val serviceRaw = ListEntitiesServicesResponse.newBuilder()
            .setKey(42)
            .setName("play_rtttl")
            .build()

        val camera: EspHomeEntity = EspHomeCameraEntity(
            key = cameraRaw.key,
            objectId = cameraRaw.objectId,
            name = cameraRaw.name,
            raw = cameraRaw,
        )
        val service: EspHomeEntity = EspHomeServiceEntity(
            key = serviceRaw.key,
            objectId = serviceRaw.name,
            name = serviceRaw.name,
            raw = serviceRaw,
        )

        assertEquals("front_camera", camera.objectId)
        assertEquals("play_rtttl", service.name)
    }

    @Test
    fun `state models expose key and raw protobuf`() {
        val raw = BinarySensorStateResponse.newBuilder()
            .setKey(11)
            .setState(true)
            .build()

        val state: EspHomeState = EspHomeBinarySensorState(
            key = raw.key,
            raw = raw,
        )

        assertEquals(11, state.key)
        assertSame(raw, (state as EspHomeBinarySensorState).raw)
    }

    @Test
    fun `state handler is a functional interface`() {
        var observed: EspHomeState? = null
        val handler = EspHomeStateHandler { observed = it }
        val state = EspHomeBinarySensorState(
            key = 1,
            raw = BinarySensorStateResponse.newBuilder().setKey(1).build(),
        )

        handler.onState(state)

        assertEquals(state, observed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.EspHomeModelApiTest'
```

Expected: FAIL to compile because `EspHomeEntity`, `EspHomeState`, entity models, state models, and `EspHomeStateHandler` do not exist yet.

- [ ] **Step 3: Add public API and models**

Modify `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt`:

```kotlin
interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
    fun listEntities(): List<EspHomeEntity>
    fun subscribeStates(handler: EspHomeStateHandler)
}
```

Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeEntity.kt`:

```kotlin
package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.proto.ListEntitiesAlarmControlPanelResponse
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesButtonResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse
import io.github.arhor.esphome.client.proto.ListEntitiesClimateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCoverResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesEventResponse
import io.github.arhor.esphome.client.proto.ListEntitiesFanResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLightResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLockResponse
import io.github.arhor.esphome.client.proto.ListEntitiesMediaPlayerResponse
import io.github.arhor.esphome.client.proto.ListEntitiesNumberResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSelectResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSirenResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesUpdateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesValveResponse

sealed interface EspHomeEntity {
    val key: Int
    val objectId: String
    val name: String
}

data class EspHomeBinarySensorEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesBinarySensorResponse) : EspHomeEntity
data class EspHomeCoverEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesCoverResponse) : EspHomeEntity
data class EspHomeFanEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesFanResponse) : EspHomeEntity
data class EspHomeLightEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesLightResponse) : EspHomeEntity
data class EspHomeSensorEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesSensorResponse) : EspHomeEntity
data class EspHomeSwitchEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesSwitchResponse) : EspHomeEntity
data class EspHomeTextSensorEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesTextSensorResponse) : EspHomeEntity
data class EspHomeServiceEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesServicesResponse) : EspHomeEntity
data class EspHomeCameraEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesCameraResponse) : EspHomeEntity
data class EspHomeClimateEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesClimateResponse) : EspHomeEntity
data class EspHomeNumberEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesNumberResponse) : EspHomeEntity
data class EspHomeSelectEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesSelectResponse) : EspHomeEntity
data class EspHomeSirenEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesSirenResponse) : EspHomeEntity
data class EspHomeLockEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesLockResponse) : EspHomeEntity
data class EspHomeButtonEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesButtonResponse) : EspHomeEntity
data class EspHomeMediaPlayerEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesMediaPlayerResponse) : EspHomeEntity
data class EspHomeAlarmControlPanelEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesAlarmControlPanelResponse) : EspHomeEntity
data class EspHomeTextEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesTextResponse) : EspHomeEntity
data class EspHomeDateEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesDateResponse) : EspHomeEntity
data class EspHomeTimeEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesTimeResponse) : EspHomeEntity
data class EspHomeEventEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesEventResponse) : EspHomeEntity
data class EspHomeValveEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesValveResponse) : EspHomeEntity
data class EspHomeDateTimeEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesDateTimeResponse) : EspHomeEntity
data class EspHomeUpdateEntity(override val key: Int, override val objectId: String, override val name: String, val raw: ListEntitiesUpdateResponse) : EspHomeEntity
```

Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeState.kt`:

```kotlin
package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.proto.AlarmControlPanelStateResponse
import io.github.arhor.esphome.client.proto.BinarySensorStateResponse
import io.github.arhor.esphome.client.proto.ClimateStateResponse
import io.github.arhor.esphome.client.proto.CoverStateResponse
import io.github.arhor.esphome.client.proto.DateStateResponse
import io.github.arhor.esphome.client.proto.DateTimeStateResponse
import io.github.arhor.esphome.client.proto.EventResponse
import io.github.arhor.esphome.client.proto.FanStateResponse
import io.github.arhor.esphome.client.proto.LightStateResponse
import io.github.arhor.esphome.client.proto.LockStateResponse
import io.github.arhor.esphome.client.proto.MediaPlayerStateResponse
import io.github.arhor.esphome.client.proto.NumberStateResponse
import io.github.arhor.esphome.client.proto.SelectStateResponse
import io.github.arhor.esphome.client.proto.SensorStateResponse
import io.github.arhor.esphome.client.proto.SirenStateResponse
import io.github.arhor.esphome.client.proto.SwitchStateResponse
import io.github.arhor.esphome.client.proto.TextSensorStateResponse
import io.github.arhor.esphome.client.proto.TextStateResponse
import io.github.arhor.esphome.client.proto.TimeStateResponse
import io.github.arhor.esphome.client.proto.UpdateStateResponse
import io.github.arhor.esphome.client.proto.ValveStateResponse

fun interface EspHomeStateHandler {
    fun onState(state: EspHomeState)
}

sealed interface EspHomeState {
    val key: Int
}

data class EspHomeBinarySensorState(override val key: Int, val raw: BinarySensorStateResponse) : EspHomeState
data class EspHomeCoverState(override val key: Int, val raw: CoverStateResponse) : EspHomeState
data class EspHomeFanState(override val key: Int, val raw: FanStateResponse) : EspHomeState
data class EspHomeLightState(override val key: Int, val raw: LightStateResponse) : EspHomeState
data class EspHomeSensorState(override val key: Int, val raw: SensorStateResponse) : EspHomeState
data class EspHomeSwitchState(override val key: Int, val raw: SwitchStateResponse) : EspHomeState
data class EspHomeTextSensorState(override val key: Int, val raw: TextSensorStateResponse) : EspHomeState
data class EspHomeClimateState(override val key: Int, val raw: ClimateStateResponse) : EspHomeState
data class EspHomeNumberState(override val key: Int, val raw: NumberStateResponse) : EspHomeState
data class EspHomeSelectState(override val key: Int, val raw: SelectStateResponse) : EspHomeState
data class EspHomeSirenState(override val key: Int, val raw: SirenStateResponse) : EspHomeState
data class EspHomeLockState(override val key: Int, val raw: LockStateResponse) : EspHomeState
data class EspHomeMediaPlayerState(override val key: Int, val raw: MediaPlayerStateResponse) : EspHomeState
data class EspHomeAlarmControlPanelState(override val key: Int, val raw: AlarmControlPanelStateResponse) : EspHomeState
data class EspHomeTextState(override val key: Int, val raw: TextStateResponse) : EspHomeState
data class EspHomeDateState(override val key: Int, val raw: DateStateResponse) : EspHomeState
data class EspHomeTimeState(override val key: Int, val raw: TimeStateResponse) : EspHomeState
data class EspHomeEventState(override val key: Int, val raw: EventResponse) : EspHomeState
data class EspHomeValveState(override val key: Int, val raw: ValveStateResponse) : EspHomeState
data class EspHomeDateTimeState(override val key: Int, val raw: DateTimeStateResponse) : EspHomeState
data class EspHomeUpdateState(override val key: Int, val raw: UpdateStateResponse) : EspHomeState
```

- [ ] **Step 4: Add temporary stubs to keep `EspHomeProtocolClient` compiling**

In `EspHomeProtocolClient`, add these methods after `fetchCameraImage()`:

```kotlin
override fun listEntities(): List<EspHomeEntity> =
    throw UnsupportedOperationException("ESPHome entity discovery is not implemented yet")

override fun subscribeStates(handler: EspHomeStateHandler) {
    throw UnsupportedOperationException("ESPHome state subscription is not implemented yet")
}
```

Add imports:

```kotlin
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeStateHandler
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.EspHomeModelApiTest'
```

Expected: PASS.

## Task 2: Message Type Constants

**Files:**

- Modify: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageType.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageTypeTest.kt`

- [ ] **Step 1: Write the failing message ID test**

Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageTypeTest.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class EspHomeMessageTypeTest {

    @Test
    fun `contains discovery message ids from api proto`() {
        assertEquals(11, EspHomeMessageType.LIST_ENTITIES_REQUEST)
        assertEquals(12, EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE)
        assertEquals(13, EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE)
        assertEquals(14, EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE)
        assertEquals(15, EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE)
        assertEquals(16, EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE)
        assertEquals(17, EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE)
        assertEquals(18, EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE)
        assertEquals(19, EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE)
        assertEquals(41, EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE)
        assertEquals(43, EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE)
        assertEquals(46, EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE)
        assertEquals(49, EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE)
        assertEquals(52, EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE)
        assertEquals(55, EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE)
        assertEquals(58, EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE)
        assertEquals(61, EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE)
        assertEquals(63, EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE)
        assertEquals(94, EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE)
        assertEquals(97, EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE)
        assertEquals(100, EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE)
        assertEquals(103, EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE)
        assertEquals(107, EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE)
        assertEquals(109, EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE)
        assertEquals(112, EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE)
        assertEquals(116, EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE)
    }

    @Test
    fun `contains entity state message ids from api proto`() {
        assertEquals(20, EspHomeMessageType.SUBSCRIBE_STATES_REQUEST)
        assertEquals(21, EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE)
        assertEquals(22, EspHomeMessageType.COVER_STATE_RESPONSE)
        assertEquals(23, EspHomeMessageType.FAN_STATE_RESPONSE)
        assertEquals(24, EspHomeMessageType.LIGHT_STATE_RESPONSE)
        assertEquals(25, EspHomeMessageType.SENSOR_STATE_RESPONSE)
        assertEquals(26, EspHomeMessageType.SWITCH_STATE_RESPONSE)
        assertEquals(27, EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE)
        assertEquals(47, EspHomeMessageType.CLIMATE_STATE_RESPONSE)
        assertEquals(50, EspHomeMessageType.NUMBER_STATE_RESPONSE)
        assertEquals(53, EspHomeMessageType.SELECT_STATE_RESPONSE)
        assertEquals(56, EspHomeMessageType.SIREN_STATE_RESPONSE)
        assertEquals(59, EspHomeMessageType.LOCK_STATE_RESPONSE)
        assertEquals(64, EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE)
        assertEquals(95, EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE)
        assertEquals(98, EspHomeMessageType.TEXT_STATE_RESPONSE)
        assertEquals(101, EspHomeMessageType.DATE_STATE_RESPONSE)
        assertEquals(104, EspHomeMessageType.TIME_STATE_RESPONSE)
        assertEquals(108, EspHomeMessageType.EVENT_RESPONSE)
        assertEquals(110, EspHomeMessageType.VALVE_STATE_RESPONSE)
        assertEquals(113, EspHomeMessageType.DATETIME_STATE_RESPONSE)
        assertEquals(117, EspHomeMessageType.UPDATE_STATE_RESPONSE)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeMessageTypeTest'
```

Expected: FAIL to compile because most constants do not exist.

- [ ] **Step 3: Add message IDs**

Update `EspHomeMessageType.kt` to:

```kotlin
package io.github.arhor.esphome.client.internal

object EspHomeMessageType {
    const val HELLO_REQUEST = 1
    const val HELLO_RESPONSE = 2
    const val CONNECT_REQUEST = 3
    const val CONNECT_RESPONSE = 4
    const val DISCONNECT_REQUEST = 5
    const val DISCONNECT_RESPONSE = 6
    const val PING_REQUEST = 7
    const val PING_RESPONSE = 8
    const val DEVICE_INFO_REQUEST = 9
    const val DEVICE_INFO_RESPONSE = 10
    const val LIST_ENTITIES_REQUEST = 11
    const val LIST_ENTITIES_BINARY_SENSOR_RESPONSE = 12
    const val LIST_ENTITIES_COVER_RESPONSE = 13
    const val LIST_ENTITIES_FAN_RESPONSE = 14
    const val LIST_ENTITIES_LIGHT_RESPONSE = 15
    const val LIST_ENTITIES_SENSOR_RESPONSE = 16
    const val LIST_ENTITIES_SWITCH_RESPONSE = 17
    const val LIST_ENTITIES_TEXT_SENSOR_RESPONSE = 18
    const val LIST_ENTITIES_DONE_RESPONSE = 19
    const val SUBSCRIBE_STATES_REQUEST = 20
    const val BINARY_SENSOR_STATE_RESPONSE = 21
    const val COVER_STATE_RESPONSE = 22
    const val FAN_STATE_RESPONSE = 23
    const val LIGHT_STATE_RESPONSE = 24
    const val SENSOR_STATE_RESPONSE = 25
    const val SWITCH_STATE_RESPONSE = 26
    const val TEXT_SENSOR_STATE_RESPONSE = 27
    const val LIST_ENTITIES_SERVICES_RESPONSE = 41
    const val CAMERA_IMAGE_RESPONSE = 44
    const val CAMERA_IMAGE_REQUEST = 45
    const val LIST_ENTITIES_CAMERA_RESPONSE = 43
    const val LIST_ENTITIES_CLIMATE_RESPONSE = 46
    const val CLIMATE_STATE_RESPONSE = 47
    const val LIST_ENTITIES_NUMBER_RESPONSE = 49
    const val NUMBER_STATE_RESPONSE = 50
    const val LIST_ENTITIES_SELECT_RESPONSE = 52
    const val SELECT_STATE_RESPONSE = 53
    const val LIST_ENTITIES_SIREN_RESPONSE = 55
    const val SIREN_STATE_RESPONSE = 56
    const val LIST_ENTITIES_LOCK_RESPONSE = 58
    const val LOCK_STATE_RESPONSE = 59
    const val LIST_ENTITIES_BUTTON_RESPONSE = 61
    const val LIST_ENTITIES_MEDIA_PLAYER_RESPONSE = 63
    const val MEDIA_PLAYER_STATE_RESPONSE = 64
    const val LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE = 94
    const val ALARM_CONTROL_PANEL_STATE_RESPONSE = 95
    const val LIST_ENTITIES_TEXT_RESPONSE = 97
    const val TEXT_STATE_RESPONSE = 98
    const val LIST_ENTITIES_DATE_RESPONSE = 100
    const val DATE_STATE_RESPONSE = 101
    const val LIST_ENTITIES_TIME_RESPONSE = 103
    const val TIME_STATE_RESPONSE = 104
    const val LIST_ENTITIES_EVENT_RESPONSE = 107
    const val EVENT_RESPONSE = 108
    const val LIST_ENTITIES_VALVE_RESPONSE = 109
    const val VALVE_STATE_RESPONSE = 110
    const val LIST_ENTITIES_DATETIME_RESPONSE = 112
    const val DATETIME_STATE_RESPONSE = 113
    const val LIST_ENTITIES_UPDATE_RESPONSE = 116
    const val UPDATE_STATE_RESPONSE = 117
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeMessageTypeTest'
```

Expected: PASS.

## Task 3: Entity Discovery Mapper

**Files:**

- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapper.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapperTest.kt`

- [ ] **Step 1: Write the failing entity mapper test**

Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapperTest.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeAlarmControlPanelEntity
import io.github.arhor.esphome.client.EspHomeBinarySensorEntity
import io.github.arhor.esphome.client.EspHomeButtonEntity
import io.github.arhor.esphome.client.EspHomeCameraEntity
import io.github.arhor.esphome.client.EspHomeClimateEntity
import io.github.arhor.esphome.client.EspHomeCoverEntity
import io.github.arhor.esphome.client.EspHomeDateEntity
import io.github.arhor.esphome.client.EspHomeDateTimeEntity
import io.github.arhor.esphome.client.EspHomeEventEntity
import io.github.arhor.esphome.client.EspHomeFanEntity
import io.github.arhor.esphome.client.EspHomeLightEntity
import io.github.arhor.esphome.client.EspHomeLockEntity
import io.github.arhor.esphome.client.EspHomeMediaPlayerEntity
import io.github.arhor.esphome.client.EspHomeNumberEntity
import io.github.arhor.esphome.client.EspHomeSelectEntity
import io.github.arhor.esphome.client.EspHomeSensorEntity
import io.github.arhor.esphome.client.EspHomeServiceEntity
import io.github.arhor.esphome.client.EspHomeSirenEntity
import io.github.arhor.esphome.client.EspHomeSwitchEntity
import io.github.arhor.esphome.client.EspHomeTextEntity
import io.github.arhor.esphome.client.EspHomeTextSensorEntity
import io.github.arhor.esphome.client.EspHomeTimeEntity
import io.github.arhor.esphome.client.EspHomeUpdateEntity
import io.github.arhor.esphome.client.EspHomeValveEntity
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.proto.ListEntitiesAlarmControlPanelResponse
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesButtonResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse
import io.github.arhor.esphome.client.proto.ListEntitiesClimateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCoverResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesEventResponse
import io.github.arhor.esphome.client.proto.ListEntitiesFanResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLightResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLockResponse
import io.github.arhor.esphome.client.proto.ListEntitiesMediaPlayerResponse
import io.github.arhor.esphome.client.proto.ListEntitiesNumberResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSelectResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSirenResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesUpdateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesValveResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class EspHomeEntityMapperTest {

    @Test
    fun `maps every discovery family to an entity model`() {
        val cases = listOf(
            EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE to entityPayload(ListEntitiesBinarySensorResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE to entityPayload(ListEntitiesCoverResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE to entityPayload(ListEntitiesFanResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE to entityPayload(ListEntitiesLightResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE to entityPayload(ListEntitiesSensorResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE to entityPayload(ListEntitiesSwitchResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE to entityPayload(ListEntitiesTextSensorResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE to entityPayload(ListEntitiesCameraResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE to entityPayload(ListEntitiesClimateResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE to entityPayload(ListEntitiesNumberResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE to entityPayload(ListEntitiesSelectResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE to entityPayload(ListEntitiesSirenResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE to entityPayload(ListEntitiesLockResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE to entityPayload(ListEntitiesButtonResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE to entityPayload(ListEntitiesMediaPlayerResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE to entityPayload(ListEntitiesAlarmControlPanelResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE to entityPayload(ListEntitiesTextResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE to entityPayload(ListEntitiesDateResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE to entityPayload(ListEntitiesTimeResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE to entityPayload(ListEntitiesEventResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE to entityPayload(ListEntitiesValveResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE to entityPayload(ListEntitiesDateTimeResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE to entityPayload(ListEntitiesUpdateResponse.newBuilder()),
        )

        val mapped = cases.map { (messageType, payload) -> EspHomeEntityMapper.map(messageType, payload) }

        assertIs<EspHomeBinarySensorEntity>(mapped[0])
        assertIs<EspHomeCoverEntity>(mapped[1])
        assertIs<EspHomeFanEntity>(mapped[2])
        assertIs<EspHomeLightEntity>(mapped[3])
        assertIs<EspHomeSensorEntity>(mapped[4])
        assertIs<EspHomeSwitchEntity>(mapped[5])
        assertIs<EspHomeTextSensorEntity>(mapped[6])
        assertIs<EspHomeCameraEntity>(mapped[7])
        assertIs<EspHomeClimateEntity>(mapped[8])
        assertIs<EspHomeNumberEntity>(mapped[9])
        assertIs<EspHomeSelectEntity>(mapped[10])
        assertIs<EspHomeSirenEntity>(mapped[11])
        assertIs<EspHomeLockEntity>(mapped[12])
        assertIs<EspHomeButtonEntity>(mapped[13])
        assertIs<EspHomeMediaPlayerEntity>(mapped[14])
        assertIs<EspHomeAlarmControlPanelEntity>(mapped[15])
        assertIs<EspHomeTextEntity>(mapped[16])
        assertIs<EspHomeDateEntity>(mapped[17])
        assertIs<EspHomeTimeEntity>(mapped[18])
        assertIs<EspHomeEventEntity>(mapped[19])
        assertIs<EspHomeValveEntity>(mapped[20])
        assertIs<EspHomeDateTimeEntity>(mapped[21])
        assertIs<EspHomeUpdateEntity>(mapped[22])
        mapped.forEachIndexed { index, entity ->
            assertEquals(index + 1, entity.key)
            assertEquals("object_${index + 1}", entity.objectId)
            assertEquals("Entity ${index + 1}", entity.name)
        }
    }

    @Test
    fun `maps service discovery as service entity`() {
        val raw = ListEntitiesServicesResponse.newBuilder()
            .setKey(91)
            .setName("play_rtttl")
            .build()

        val entity = EspHomeEntityMapper.map(
            EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE,
            raw.toByteArray(),
        )

        assertIs<EspHomeServiceEntity>(entity)
        assertEquals(91, entity.key)
        assertEquals("play_rtttl", entity.objectId)
        assertEquals("play_rtttl", entity.name)
        assertEquals(raw, entity.raw)
    }

    @Test
    fun `rejects non discovery message type`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeEntityMapper.map(EspHomeMessageType.PING_REQUEST, ByteArray(0))
        }

        assertEquals("Unsupported ESPHome entity discovery message: 7", error.message)
    }

    @Test
    fun `wraps malformed discovery payloads as protocol errors`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeEntityMapper.map(
                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE,
                byteArrayOf(0x0a, 0x10, 0x01),
            )
        }

        assertEquals("Malformed ESPHome entity discovery payload for message 16", error.message)
    }

    private fun entityPayload(builder: ListEntitiesBinarySensorResponse.Builder) = builder.withCommon(1).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesCoverResponse.Builder) = builder.withCommon(2).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesFanResponse.Builder) = builder.withCommon(3).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesLightResponse.Builder) = builder.withCommon(4).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSensorResponse.Builder) = builder.withCommon(5).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSwitchResponse.Builder) = builder.withCommon(6).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesTextSensorResponse.Builder) = builder.withCommon(7).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesCameraResponse.Builder) = builder.withCommon(8).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesClimateResponse.Builder) = builder.withCommon(9).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesNumberResponse.Builder) = builder.withCommon(10).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSelectResponse.Builder) = builder.withCommon(11).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSirenResponse.Builder) = builder.withCommon(12).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesLockResponse.Builder) = builder.withCommon(13).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesButtonResponse.Builder) = builder.withCommon(14).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesMediaPlayerResponse.Builder) = builder.withCommon(15).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesAlarmControlPanelResponse.Builder) = builder.withCommon(16).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesTextResponse.Builder) = builder.withCommon(17).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesDateResponse.Builder) = builder.withCommon(18).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesTimeResponse.Builder) = builder.withCommon(19).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesEventResponse.Builder) = builder.withCommon(20).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesValveResponse.Builder) = builder.withCommon(21).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesDateTimeResponse.Builder) = builder.withCommon(22).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesUpdateResponse.Builder) = builder.withCommon(23).build().toByteArray()

    private fun ListEntitiesBinarySensorResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesCoverResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesFanResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesLightResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesSensorResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesSwitchResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesTextSensorResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesCameraResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesClimateResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesNumberResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesSelectResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesSirenResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesLockResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesButtonResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesMediaPlayerResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesAlarmControlPanelResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesTextResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesDateResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesTimeResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesEventResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesValveResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesDateTimeResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
    private fun ListEntitiesUpdateResponse.Builder.withCommon(index: Int) = setKey(index).setObjectId("object_$index").setName("Entity $index")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeEntityMapperTest'
```

Expected: FAIL to compile because `EspHomeEntityMapper` does not exist.

- [ ] **Step 3: Implement the mapper**

Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeEntityMapper.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
import io.github.arhor.esphome.client.EspHomeAlarmControlPanelEntity
import io.github.arhor.esphome.client.EspHomeBinarySensorEntity
import io.github.arhor.esphome.client.EspHomeButtonEntity
import io.github.arhor.esphome.client.EspHomeCameraEntity
import io.github.arhor.esphome.client.EspHomeClimateEntity
import io.github.arhor.esphome.client.EspHomeCoverEntity
import io.github.arhor.esphome.client.EspHomeDateEntity
import io.github.arhor.esphome.client.EspHomeDateTimeEntity
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeEventEntity
import io.github.arhor.esphome.client.EspHomeFanEntity
import io.github.arhor.esphome.client.EspHomeLightEntity
import io.github.arhor.esphome.client.EspHomeLockEntity
import io.github.arhor.esphome.client.EspHomeMediaPlayerEntity
import io.github.arhor.esphome.client.EspHomeNumberEntity
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.EspHomeSelectEntity
import io.github.arhor.esphome.client.EspHomeSensorEntity
import io.github.arhor.esphome.client.EspHomeServiceEntity
import io.github.arhor.esphome.client.EspHomeSirenEntity
import io.github.arhor.esphome.client.EspHomeSwitchEntity
import io.github.arhor.esphome.client.EspHomeTextEntity
import io.github.arhor.esphome.client.EspHomeTextSensorEntity
import io.github.arhor.esphome.client.EspHomeTimeEntity
import io.github.arhor.esphome.client.EspHomeUpdateEntity
import io.github.arhor.esphome.client.EspHomeValveEntity
import io.github.arhor.esphome.client.proto.ListEntitiesAlarmControlPanelResponse
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesButtonResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse
import io.github.arhor.esphome.client.proto.ListEntitiesClimateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCoverResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDateTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesEventResponse
import io.github.arhor.esphome.client.proto.ListEntitiesFanResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLightResponse
import io.github.arhor.esphome.client.proto.ListEntitiesLockResponse
import io.github.arhor.esphome.client.proto.ListEntitiesMediaPlayerResponse
import io.github.arhor.esphome.client.proto.ListEntitiesNumberResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSelectResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSirenResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTextSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesTimeResponse
import io.github.arhor.esphome.client.proto.ListEntitiesUpdateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesValveResponse

internal object EspHomeEntityMapper {

    fun map(messageType: Int, payload: ByteArray): EspHomeEntity =
        try {
            when (messageType) {
                EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE ->
                    ListEntitiesBinarySensorResponse.parseFrom(payload).let { EspHomeBinarySensorEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE ->
                    ListEntitiesCoverResponse.parseFrom(payload).let { EspHomeCoverEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE ->
                    ListEntitiesFanResponse.parseFrom(payload).let { EspHomeFanEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE ->
                    ListEntitiesLightResponse.parseFrom(payload).let { EspHomeLightEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE ->
                    ListEntitiesSensorResponse.parseFrom(payload).let { EspHomeSensorEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE ->
                    ListEntitiesSwitchResponse.parseFrom(payload).let { EspHomeSwitchEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE ->
                    ListEntitiesTextSensorResponse.parseFrom(payload).let { EspHomeTextSensorEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE ->
                    ListEntitiesServicesResponse.parseFrom(payload).let { EspHomeServiceEntity(it.key, it.name, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE ->
                    ListEntitiesCameraResponse.parseFrom(payload).let { EspHomeCameraEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE ->
                    ListEntitiesClimateResponse.parseFrom(payload).let { EspHomeClimateEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE ->
                    ListEntitiesNumberResponse.parseFrom(payload).let { EspHomeNumberEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE ->
                    ListEntitiesSelectResponse.parseFrom(payload).let { EspHomeSelectEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE ->
                    ListEntitiesSirenResponse.parseFrom(payload).let { EspHomeSirenEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE ->
                    ListEntitiesLockResponse.parseFrom(payload).let { EspHomeLockEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE ->
                    ListEntitiesButtonResponse.parseFrom(payload).let { EspHomeButtonEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE ->
                    ListEntitiesMediaPlayerResponse.parseFrom(payload).let { EspHomeMediaPlayerEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE ->
                    ListEntitiesAlarmControlPanelResponse.parseFrom(payload).let { EspHomeAlarmControlPanelEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE ->
                    ListEntitiesTextResponse.parseFrom(payload).let { EspHomeTextEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE ->
                    ListEntitiesDateResponse.parseFrom(payload).let { EspHomeDateEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE ->
                    ListEntitiesTimeResponse.parseFrom(payload).let { EspHomeTimeEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE ->
                    ListEntitiesEventResponse.parseFrom(payload).let { EspHomeEventEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE ->
                    ListEntitiesValveResponse.parseFrom(payload).let { EspHomeValveEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE ->
                    ListEntitiesDateTimeResponse.parseFrom(payload).let { EspHomeDateTimeEntity(it.key, it.objectId, it.name, it) }
                EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE ->
                    ListEntitiesUpdateResponse.parseFrom(payload).let { EspHomeUpdateEntity(it.key, it.objectId, it.name, it) }
                else -> throw EspHomeProtocolException("Unsupported ESPHome entity discovery message: $messageType")
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw EspHomeProtocolException(
                "Malformed ESPHome entity discovery payload for message $messageType",
                exception,
            )
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeEntityMapperTest'
```

Expected: PASS.

## Task 4: State Mapper

**Files:**

- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapper.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapperTest.kt`

- [ ] **Step 1: Write the failing state mapper test**

Create `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapperTest.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.*
import io.github.arhor.esphome.client.proto.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class EspHomeStateMapperTest {

    @Test
    fun `maps every entity state family to a state model`() {
        val cases = listOf(
            EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE to BinarySensorStateResponse.newBuilder().setKey(1).build().toByteArray(),
            EspHomeMessageType.COVER_STATE_RESPONSE to CoverStateResponse.newBuilder().setKey(2).build().toByteArray(),
            EspHomeMessageType.FAN_STATE_RESPONSE to FanStateResponse.newBuilder().setKey(3).build().toByteArray(),
            EspHomeMessageType.LIGHT_STATE_RESPONSE to LightStateResponse.newBuilder().setKey(4).build().toByteArray(),
            EspHomeMessageType.SENSOR_STATE_RESPONSE to SensorStateResponse.newBuilder().setKey(5).build().toByteArray(),
            EspHomeMessageType.SWITCH_STATE_RESPONSE to SwitchStateResponse.newBuilder().setKey(6).build().toByteArray(),
            EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE to TextSensorStateResponse.newBuilder().setKey(7).build().toByteArray(),
            EspHomeMessageType.CLIMATE_STATE_RESPONSE to ClimateStateResponse.newBuilder().setKey(8).build().toByteArray(),
            EspHomeMessageType.NUMBER_STATE_RESPONSE to NumberStateResponse.newBuilder().setKey(9).build().toByteArray(),
            EspHomeMessageType.SELECT_STATE_RESPONSE to SelectStateResponse.newBuilder().setKey(10).build().toByteArray(),
            EspHomeMessageType.SIREN_STATE_RESPONSE to SirenStateResponse.newBuilder().setKey(11).build().toByteArray(),
            EspHomeMessageType.LOCK_STATE_RESPONSE to LockStateResponse.newBuilder().setKey(12).build().toByteArray(),
            EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE to MediaPlayerStateResponse.newBuilder().setKey(13).build().toByteArray(),
            EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE to AlarmControlPanelStateResponse.newBuilder().setKey(14).build().toByteArray(),
            EspHomeMessageType.TEXT_STATE_RESPONSE to TextStateResponse.newBuilder().setKey(15).build().toByteArray(),
            EspHomeMessageType.DATE_STATE_RESPONSE to DateStateResponse.newBuilder().setKey(16).build().toByteArray(),
            EspHomeMessageType.TIME_STATE_RESPONSE to TimeStateResponse.newBuilder().setKey(17).build().toByteArray(),
            EspHomeMessageType.EVENT_RESPONSE to EventResponse.newBuilder().setKey(18).build().toByteArray(),
            EspHomeMessageType.VALVE_STATE_RESPONSE to ValveStateResponse.newBuilder().setKey(19).build().toByteArray(),
            EspHomeMessageType.DATETIME_STATE_RESPONSE to DateTimeStateResponse.newBuilder().setKey(20).build().toByteArray(),
            EspHomeMessageType.UPDATE_STATE_RESPONSE to UpdateStateResponse.newBuilder().setKey(21).build().toByteArray(),
        )

        val mapped = cases.map { (messageType, payload) -> EspHomeStateMapper.map(messageType, payload) }

        assertIs<EspHomeBinarySensorState>(mapped[0])
        assertIs<EspHomeCoverState>(mapped[1])
        assertIs<EspHomeFanState>(mapped[2])
        assertIs<EspHomeLightState>(mapped[3])
        assertIs<EspHomeSensorState>(mapped[4])
        assertIs<EspHomeSwitchState>(mapped[5])
        assertIs<EspHomeTextSensorState>(mapped[6])
        assertIs<EspHomeClimateState>(mapped[7])
        assertIs<EspHomeNumberState>(mapped[8])
        assertIs<EspHomeSelectState>(mapped[9])
        assertIs<EspHomeSirenState>(mapped[10])
        assertIs<EspHomeLockState>(mapped[11])
        assertIs<EspHomeMediaPlayerState>(mapped[12])
        assertIs<EspHomeAlarmControlPanelState>(mapped[13])
        assertIs<EspHomeTextState>(mapped[14])
        assertIs<EspHomeDateState>(mapped[15])
        assertIs<EspHomeTimeState>(mapped[16])
        assertIs<EspHomeEventState>(mapped[17])
        assertIs<EspHomeValveState>(mapped[18])
        assertIs<EspHomeDateTimeState>(mapped[19])
        assertIs<EspHomeUpdateState>(mapped[20])
        mapped.forEachIndexed { index, state ->
            assertEquals(index + 1, state.key)
        }
    }

    @Test
    fun `rejects non state message type`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeStateMapper.map(EspHomeMessageType.LIST_ENTITIES_REQUEST, ByteArray(0))
        }

        assertEquals("Unsupported ESPHome state message: 11", error.message)
    }

    @Test
    fun `wraps malformed state payloads as protocol errors`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeStateMapper.map(
                EspHomeMessageType.SENSOR_STATE_RESPONSE,
                byteArrayOf(0x0d, 0x01),
            )
        }

        assertEquals("Malformed ESPHome state payload for message 25", error.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeStateMapperTest'
```

Expected: FAIL to compile because `EspHomeStateMapper` does not exist.

- [ ] **Step 3: Implement the mapper**

Create `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeStateMapper.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
import io.github.arhor.esphome.client.*
import io.github.arhor.esphome.client.proto.*

internal object EspHomeStateMapper {

    fun map(messageType: Int, payload: ByteArray): EspHomeState =
        try {
            when (messageType) {
                EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE ->
                    BinarySensorStateResponse.parseFrom(payload).let { EspHomeBinarySensorState(it.key, it) }
                EspHomeMessageType.COVER_STATE_RESPONSE ->
                    CoverStateResponse.parseFrom(payload).let { EspHomeCoverState(it.key, it) }
                EspHomeMessageType.FAN_STATE_RESPONSE ->
                    FanStateResponse.parseFrom(payload).let { EspHomeFanState(it.key, it) }
                EspHomeMessageType.LIGHT_STATE_RESPONSE ->
                    LightStateResponse.parseFrom(payload).let { EspHomeLightState(it.key, it) }
                EspHomeMessageType.SENSOR_STATE_RESPONSE ->
                    SensorStateResponse.parseFrom(payload).let { EspHomeSensorState(it.key, it) }
                EspHomeMessageType.SWITCH_STATE_RESPONSE ->
                    SwitchStateResponse.parseFrom(payload).let { EspHomeSwitchState(it.key, it) }
                EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE ->
                    TextSensorStateResponse.parseFrom(payload).let { EspHomeTextSensorState(it.key, it) }
                EspHomeMessageType.CLIMATE_STATE_RESPONSE ->
                    ClimateStateResponse.parseFrom(payload).let { EspHomeClimateState(it.key, it) }
                EspHomeMessageType.NUMBER_STATE_RESPONSE ->
                    NumberStateResponse.parseFrom(payload).let { EspHomeNumberState(it.key, it) }
                EspHomeMessageType.SELECT_STATE_RESPONSE ->
                    SelectStateResponse.parseFrom(payload).let { EspHomeSelectState(it.key, it) }
                EspHomeMessageType.SIREN_STATE_RESPONSE ->
                    SirenStateResponse.parseFrom(payload).let { EspHomeSirenState(it.key, it) }
                EspHomeMessageType.LOCK_STATE_RESPONSE ->
                    LockStateResponse.parseFrom(payload).let { EspHomeLockState(it.key, it) }
                EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE ->
                    MediaPlayerStateResponse.parseFrom(payload).let { EspHomeMediaPlayerState(it.key, it) }
                EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE ->
                    AlarmControlPanelStateResponse.parseFrom(payload).let { EspHomeAlarmControlPanelState(it.key, it) }
                EspHomeMessageType.TEXT_STATE_RESPONSE ->
                    TextStateResponse.parseFrom(payload).let { EspHomeTextState(it.key, it) }
                EspHomeMessageType.DATE_STATE_RESPONSE ->
                    DateStateResponse.parseFrom(payload).let { EspHomeDateState(it.key, it) }
                EspHomeMessageType.TIME_STATE_RESPONSE ->
                    TimeStateResponse.parseFrom(payload).let { EspHomeTimeState(it.key, it) }
                EspHomeMessageType.EVENT_RESPONSE ->
                    EventResponse.parseFrom(payload).let { EspHomeEventState(it.key, it) }
                EspHomeMessageType.VALVE_STATE_RESPONSE ->
                    ValveStateResponse.parseFrom(payload).let { EspHomeValveState(it.key, it) }
                EspHomeMessageType.DATETIME_STATE_RESPONSE ->
                    DateTimeStateResponse.parseFrom(payload).let { EspHomeDateTimeState(it.key, it) }
                EspHomeMessageType.UPDATE_STATE_RESPONSE ->
                    UpdateStateResponse.parseFrom(payload).let { EspHomeUpdateState(it.key, it) }
                else -> throw EspHomeProtocolException("Unsupported ESPHome state message: $messageType")
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw EspHomeProtocolException(
                "Malformed ESPHome state payload for message $messageType",
                exception,
            )
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeStateMapperTest'
```

Expected: PASS.

## Task 5: `listEntities()` Protocol Flow

**Files:**

- Modify: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClient.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClientTest.kt`

- [ ] **Step 1: Write failing discovery aggregation tests**

Append these tests to `EspHomeProtocolClientTest`:

```kotlin
    @Test
    fun `listEntities aggregates discovery responses until done`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE,
                ListEntitiesSensorResponse.newBuilder()
                    .setKey(1)
                    .setObjectId("temperature")
                    .setName("Temperature")
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE,
                ListEntitiesSwitchResponse.newBuilder()
                    .setKey(2)
                    .setObjectId("relay")
                    .setName("Relay")
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE,
                ListEntitiesDoneResponse.newBuilder().build().toByteArray(),
            ),
        )

        val entities = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()

        assertEquals(EspHomeMessageType.LIST_ENTITIES_REQUEST, transport.sent.single().messageType)
        assertEquals(listOf("temperature", "relay"), entities.map { it.objectId })
    }

    @Test
    fun `listEntities responds to ping while waiting for discovery done`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.PING_REQUEST,
                PingRequest.newBuilder().build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE,
                ListEntitiesDoneResponse.newBuilder().build().toByteArray(),
            ),
        )

        val entities = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()

        assertEquals(emptyList(), entities)
        assertEquals(EspHomeMessageType.LIST_ENTITIES_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.PING_RESPONSE, transport.sent[1].messageType)
    }

    @Test
    fun `listEntities rejects unexpected messages`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DEVICE_INFO_RESPONSE,
                DeviceInfoResponse.newBuilder().build().toByteArray(),
            ),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()
        }

        assertEquals("Expected ESPHome entity discovery message but received 10", error.message)
    }
```

Add imports to the test file:

```kotlin
import io.github.arhor.esphome.client.proto.ListEntitiesDoneResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeProtocolClientTest.listEntities*'
```

Expected: FAIL because `listEntities()` still throws `UnsupportedOperationException`.

- [ ] **Step 3: Implement `listEntities()`**

Replace the `listEntities()` stub in `EspHomeProtocolClient` with:

```kotlin
override fun listEntities(): List<EspHomeEntity> {
    send(EspHomeMessageType.LIST_ENTITIES_REQUEST) {
        ListEntitiesRequest.newBuilder().build().toByteArray()
    }

    val entities = mutableListOf<EspHomeEntity>()
    while (true) {
        val frame = transport.receive()
        when (frame.messageType) {
            EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE -> return entities
            EspHomeMessageType.PING_REQUEST -> {
                send(EspHomeMessageType.PING_RESPONSE) {
                    PingResponse.newBuilder().build().toByteArray()
                }
            }
            in ENTITY_DISCOVERY_MESSAGE_TYPES -> entities += EspHomeEntityMapper.map(frame.messageType, frame.payload)
            else -> throw EspHomeProtocolException(
                "Expected ESPHome entity discovery message but received ${frame.messageType}",
            )
        }
    }
}
```

Add imports:

```kotlin
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.proto.ListEntitiesRequest
```

Add this companion object inside `EspHomeProtocolClient`:

```kotlin
private companion object {
    val ENTITY_DISCOVERY_MESSAGE_TYPES = setOf(
        EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE,
        EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE,
    )
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeProtocolClientTest.listEntities*'
```

Expected: PASS.

## Task 6: `subscribeStates(handler)` Protocol Flow

**Files:**

- Modify: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClient.kt`
- Test: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClientTest.kt`

- [ ] **Step 1: Write failing subscription tests**

Append these tests to `EspHomeProtocolClientTest`:

```kotlin
    @Test
    fun `subscribeStates dispatches states in receive order`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.SENSOR_STATE_RESPONSE,
                SensorStateResponse.newBuilder()
                    .setKey(1)
                    .setState(21.5f)
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.SWITCH_STATE_RESPONSE,
                SwitchStateResponse.newBuilder()
                    .setKey(2)
                    .setState(true)
                    .build()
                    .toByteArray(),
            ),
        )
        val received = mutableListOf<Int>()
        val stop = RuntimeException("stop after two states")

        val error = assertFailsWith<RuntimeException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates { state ->
                received += state.key
                if (received.size == 2) throw stop
            }
        }

        assertEquals(stop, error)
        assertEquals(listOf(1, 2), received)
        assertEquals(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, transport.sent.single().messageType)
    }

    @Test
    fun `subscribeStates responds to ping while waiting for states`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.PING_REQUEST,
                PingRequest.newBuilder().build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.SWITCH_STATE_RESPONSE,
                SwitchStateResponse.newBuilder()
                    .setKey(2)
                    .setState(true)
                    .build()
                    .toByteArray(),
            ),
        )
        val stop = RuntimeException("stop after state")

        assertFailsWith<RuntimeException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates {
                throw stop
            }
        }

        assertEquals(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.PING_RESPONSE, transport.sent[1].messageType)
    }

    @Test
    fun `subscribeStates rejects unexpected messages`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DEVICE_INFO_RESPONSE,
                DeviceInfoResponse.newBuilder().build().toByteArray(),
            ),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates {}
        }

        assertEquals("Expected ESPHome state message but received 10", error.message)
    }
```

Add imports:

```kotlin
import io.github.arhor.esphome.client.proto.SensorStateResponse
import io.github.arhor.esphome.client.proto.SwitchStateResponse
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeProtocolClientTest.subscribeStates*'
```

Expected: FAIL because `subscribeStates()` still throws `UnsupportedOperationException`.

- [ ] **Step 3: Implement `subscribeStates(handler)`**

Replace the `subscribeStates()` stub in `EspHomeProtocolClient` with:

```kotlin
override fun subscribeStates(handler: EspHomeStateHandler) {
    send(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST) {
        SubscribeStatesRequest.newBuilder().build().toByteArray()
    }

    while (true) {
        val frame = transport.receive()
        when (frame.messageType) {
            EspHomeMessageType.PING_REQUEST -> {
                send(EspHomeMessageType.PING_RESPONSE) {
                    PingResponse.newBuilder().build().toByteArray()
                }
            }
            EspHomeMessageType.DISCONNECT_REQUEST -> return
            in ENTITY_STATE_MESSAGE_TYPES -> handler.onState(EspHomeStateMapper.map(frame.messageType, frame.payload))
            else -> throw EspHomeProtocolException(
                "Expected ESPHome state message but received ${frame.messageType}",
            )
        }
    }
}
```

Add imports:

```kotlin
import io.github.arhor.esphome.client.EspHomeStateHandler
import io.github.arhor.esphome.client.proto.SubscribeStatesRequest
```

Add this set to the existing companion object:

```kotlin
val ENTITY_STATE_MESSAGE_TYPES = setOf(
    EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE,
    EspHomeMessageType.COVER_STATE_RESPONSE,
    EspHomeMessageType.FAN_STATE_RESPONSE,
    EspHomeMessageType.LIGHT_STATE_RESPONSE,
    EspHomeMessageType.SENSOR_STATE_RESPONSE,
    EspHomeMessageType.SWITCH_STATE_RESPONSE,
    EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE,
    EspHomeMessageType.CLIMATE_STATE_RESPONSE,
    EspHomeMessageType.NUMBER_STATE_RESPONSE,
    EspHomeMessageType.SELECT_STATE_RESPONSE,
    EspHomeMessageType.SIREN_STATE_RESPONSE,
    EspHomeMessageType.LOCK_STATE_RESPONSE,
    EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE,
    EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE,
    EspHomeMessageType.TEXT_STATE_RESPONSE,
    EspHomeMessageType.DATE_STATE_RESPONSE,
    EspHomeMessageType.TIME_STATE_RESPONSE,
    EspHomeMessageType.EVENT_RESPONSE,
    EspHomeMessageType.VALVE_STATE_RESPONSE,
    EspHomeMessageType.DATETIME_STATE_RESPONSE,
    EspHomeMessageType.UPDATE_STATE_RESPONSE,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./gradlew :lib-esphome-client:test --tests 'io.github.arhor.esphome.client.internal.EspHomeProtocolClientTest.subscribeStates*'
```

Expected: PASS.

## Task 7: Full Library Regression

**Files:**

- Existing tests under `lib-esphome-client/src/test/kotlin`

- [ ] **Step 1: Run the full library test suite**

Run:

```bash
./gradlew :lib-esphome-client:test
```

Expected: PASS.

- [ ] **Step 2: Fix any library compile or test failures without changing the approved API**

If a library test fake implements `EspHomeConnection` and fails to compile because the interface gained methods, update
that fake connection with:

```kotlin
override fun listEntities() = emptyList<EspHomeEntity>()
override fun subscribeStates(handler: EspHomeStateHandler) = Unit
```

Add imports where needed:

```kotlin
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeStateHandler
```

- [ ] **Step 3: Re-run the full library test suite**

Run:

```bash
./gradlew :lib-esphome-client:test
```

Expected: PASS.

## Task 8: App Compatibility And Build Verification

**Files:**

- Modify only if compile requires fake connection updates:
  `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/EspHomeNativeFrameClientTest.kt`

- [ ] **Step 1: Run app tests**

Run:

```bash
./gradlew :app-cat-recognizer:test
```

Expected: PASS.

- [ ] **Step 2: Update app fake connection on compile failure**

If `FakeConnection` in `EspHomeNativeFrameClientTest` fails to compile because of new `EspHomeConnection` methods, add:

```kotlin
override fun listEntities() = emptyList<EspHomeEntity>()
override fun subscribeStates(handler: EspHomeStateHandler) = Unit
```

Add imports:

```kotlin
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeStateHandler
```

- [ ] **Step 3: Run app tests again**

Run:

```bash
./gradlew :app-cat-recognizer:test
```

Expected: PASS.

- [ ] **Step 4: Run full app build**

Run:

```bash
./gradlew :app-cat-recognizer:build
```

Expected: PASS.

## Self-Review Checklist

- Spec coverage: Tasks 1-6 implement all model, message, mapper, discovery, and subscription requirements from the spec.
- Scope control: Commands, logs, services execution, Home Assistant bridging, Bluetooth, voice assistant, dynamic Noise key update, and encrypted fallback are not implemented.
- TDD order: Every production change has a failing test step before implementation.
- Verification: Tasks 7-8 run library tests, app tests, and app build.
