package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.proto.BinarySensorStateResponse
import io.github.arhor.esphome.client.proto.DateStateResponse
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse
import io.github.arhor.esphome.client.proto.UpdateStateResponse
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

        val entity: EspHomeEntity = EspHomeEntity.BinarySensor(raw)

        assertEquals(11, entity.key)
        assertEquals("motion", entity.objectId)
        assertEquals("Motion", entity.name)
        assertSame(raw, (entity as EspHomeEntity.BinarySensor).raw)
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

        val camera: EspHomeEntity = EspHomeEntity.Camera(cameraRaw)
        val service: EspHomeEntity = EspHomeEntity.Service(serviceRaw)

        assertEquals("front_camera", camera.objectId)
        assertEquals("play_rtttl", service.name)
    }

    @Test
    fun `state models expose key and raw protobuf`() {
        val raw = BinarySensorStateResponse.newBuilder()
            .setKey(11)
            .setState(true)
            .build()

        val state: EspHomeState = EspHomeState.BinarySensor(
            raw = raw,
        )

        assertEquals(11, state.key)
        assertSame(raw, (state as EspHomeState.BinarySensor).raw)
    }

    @Test
    fun `state models expose primary protobuf fields directly`() {
        val binary = EspHomeState.BinarySensor(
            raw = BinarySensorStateResponse.newBuilder()
                .setKey(1)
                .setState(true)
                .setMissingState(false)
                .build(),
        )
        val date = EspHomeState.Date(
            raw = DateStateResponse.newBuilder()
                .setKey(2)
                .setYear(2026)
                .setMonth(6)
                .setDay(10)
                .build(),
        )
        val update = EspHomeState.Update(
            raw = UpdateStateResponse.newBuilder()
                .setKey(3)
                .setInProgress(true)
                .setLatestVersion("2026.6.0")
                .build(),
        )

        assertEquals(true, binary.state)
        assertEquals(false, binary.missingState)
        assertEquals(2026, date.year)
        assertEquals(6, date.month)
        assertEquals(10, date.day)
        assertEquals(true, update.inProgress)
        assertEquals("2026.6.0", update.latestVersion)
    }

    @Test
    fun `state handler is a functional interface`() {
        var observed: EspHomeState? = null
        val handler = EspHomeStateHandler { observed = it }
        val state = EspHomeState.BinarySensor(
            raw = BinarySensorStateResponse.newBuilder().setKey(1).build(),
        )

        handler.onState(state)

        assertEquals(state, observed)
    }
}
