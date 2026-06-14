package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.model.EspHomeEntity
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
            EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE to entityPayload(
                ListEntitiesAlarmControlPanelResponse.newBuilder()
            ),
            EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE to entityPayload(ListEntitiesTextResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE to entityPayload(ListEntitiesDateResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE to entityPayload(ListEntitiesTimeResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE to entityPayload(ListEntitiesEventResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE to entityPayload(ListEntitiesValveResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE to entityPayload(ListEntitiesDateTimeResponse.newBuilder()),
            EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE to entityPayload(ListEntitiesUpdateResponse.newBuilder()),
        )

        val mapped = cases.map { (messageType, payload) -> EspHomeEntityMapper.map(messageType, payload) }

        assertIs<EspHomeEntity.BinarySensor>(mapped[0])
        assertIs<EspHomeEntity.Cover>(mapped[1])
        assertIs<EspHomeEntity.Fan>(mapped[2])
        assertIs<EspHomeEntity.Light>(mapped[3])
        assertIs<EspHomeEntity.Sensor>(mapped[4])
        assertIs<EspHomeEntity.Switch>(mapped[5])
        assertIs<EspHomeEntity.TextSensor>(mapped[6])
        assertIs<EspHomeEntity.Camera>(mapped[7])
        assertIs<EspHomeEntity.Climate>(mapped[8])
        assertIs<EspHomeEntity.Number>(mapped[9])
        assertIs<EspHomeEntity.Select>(mapped[10])
        assertIs<EspHomeEntity.Siren>(mapped[11])
        assertIs<EspHomeEntity.Lock>(mapped[12])
        assertIs<EspHomeEntity.Button>(mapped[13])
        assertIs<EspHomeEntity.MediaPlayer>(mapped[14])
        assertIs<EspHomeEntity.AlarmControlPanel>(mapped[15])
        assertIs<EspHomeEntity.Text>(mapped[16])
        assertIs<EspHomeEntity.Date>(mapped[17])
        assertIs<EspHomeEntity.Time>(mapped[18])
        assertIs<EspHomeEntity.Event>(mapped[19])
        assertIs<EspHomeEntity.Valve>(mapped[20])
        assertIs<EspHomeEntity.DateTime>(mapped[21])
        assertIs<EspHomeEntity.Update>(mapped[22])
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

        assertIs<EspHomeEntity.Service>(entity)
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

    private fun entityPayload(builder: ListEntitiesBinarySensorResponse.Builder) =
        builder.withCommon(1).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesCoverResponse.Builder) = builder.withCommon(2).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesFanResponse.Builder) = builder.withCommon(3).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesLightResponse.Builder) = builder.withCommon(4).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSensorResponse.Builder) = builder.withCommon(5).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesSwitchResponse.Builder) = builder.withCommon(6).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesTextSensorResponse.Builder) =
        builder.withCommon(7).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesCameraResponse.Builder) = builder.withCommon(8).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesClimateResponse.Builder) =
        builder.withCommon(9).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesNumberResponse.Builder) =
        builder.withCommon(10).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesSelectResponse.Builder) =
        builder.withCommon(11).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesSirenResponse.Builder) = builder.withCommon(12).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesLockResponse.Builder) = builder.withCommon(13).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesButtonResponse.Builder) =
        builder.withCommon(14).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesMediaPlayerResponse.Builder) =
        builder.withCommon(15).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesAlarmControlPanelResponse.Builder) =
        builder.withCommon(16).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesTextResponse.Builder) = builder.withCommon(17).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesDateResponse.Builder) = builder.withCommon(18).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesTimeResponse.Builder) = builder.withCommon(19).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesEventResponse.Builder) = builder.withCommon(20).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesValveResponse.Builder) = builder.withCommon(21).build().toByteArray()
    private fun entityPayload(builder: ListEntitiesDateTimeResponse.Builder) =
        builder.withCommon(22).build().toByteArray()

    private fun entityPayload(builder: ListEntitiesUpdateResponse.Builder) =
        builder.withCommon(23).build().toByteArray()

    private fun ListEntitiesBinarySensorResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesCoverResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesFanResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesLightResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesSensorResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesSwitchResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesTextSensorResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesCameraResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesClimateResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesNumberResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesSelectResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesSirenResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesLockResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesButtonResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesMediaPlayerResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesAlarmControlPanelResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesTextResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesDateResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesTimeResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesEventResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesValveResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesDateTimeResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")

    private fun ListEntitiesUpdateResponse.Builder.withCommon(index: Int) =
        setKey(index).setObjectId("object_$index").setName("Entity $index")
}
