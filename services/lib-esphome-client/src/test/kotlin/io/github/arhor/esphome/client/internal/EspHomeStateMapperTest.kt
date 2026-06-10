package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.EspHomeState
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class EspHomeStateMapperTest {

    @Test
    fun `maps every entity state family to a state model`() {
        val cases = listOf(
            EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE to BinarySensorStateResponse.newBuilder().setKey(1).build()
                .toByteArray(),
            EspHomeMessageType.COVER_STATE_RESPONSE to CoverStateResponse.newBuilder().setKey(2).build().toByteArray(),
            EspHomeMessageType.FAN_STATE_RESPONSE to FanStateResponse.newBuilder().setKey(3).build().toByteArray(),
            EspHomeMessageType.LIGHT_STATE_RESPONSE to LightStateResponse.newBuilder().setKey(4).build().toByteArray(),
            EspHomeMessageType.SENSOR_STATE_RESPONSE to SensorStateResponse.newBuilder().setKey(5).build()
                .toByteArray(),
            EspHomeMessageType.SWITCH_STATE_RESPONSE to SwitchStateResponse.newBuilder().setKey(6).build()
                .toByteArray(),
            EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE to TextSensorStateResponse.newBuilder().setKey(7).build()
                .toByteArray(),
            EspHomeMessageType.CLIMATE_STATE_RESPONSE to ClimateStateResponse.newBuilder().setKey(8).build()
                .toByteArray(),
            EspHomeMessageType.NUMBER_STATE_RESPONSE to NumberStateResponse.newBuilder().setKey(9).build()
                .toByteArray(),
            EspHomeMessageType.SELECT_STATE_RESPONSE to SelectStateResponse.newBuilder().setKey(10).build()
                .toByteArray(),
            EspHomeMessageType.SIREN_STATE_RESPONSE to SirenStateResponse.newBuilder().setKey(11).build().toByteArray(),
            EspHomeMessageType.LOCK_STATE_RESPONSE to LockStateResponse.newBuilder().setKey(12).build().toByteArray(),
            EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE to MediaPlayerStateResponse.newBuilder().setKey(13).build()
                .toByteArray(),
            EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE to AlarmControlPanelStateResponse.newBuilder()
                .setKey(14).build().toByteArray(),
            EspHomeMessageType.TEXT_STATE_RESPONSE to TextStateResponse.newBuilder().setKey(15).build().toByteArray(),
            EspHomeMessageType.DATE_STATE_RESPONSE to DateStateResponse.newBuilder().setKey(16).build().toByteArray(),
            EspHomeMessageType.TIME_STATE_RESPONSE to TimeStateResponse.newBuilder().setKey(17).build().toByteArray(),
            EspHomeMessageType.EVENT_RESPONSE to EventResponse.newBuilder().setKey(18).build().toByteArray(),
            EspHomeMessageType.VALVE_STATE_RESPONSE to ValveStateResponse.newBuilder().setKey(19).build().toByteArray(),
            EspHomeMessageType.DATETIME_STATE_RESPONSE to DateTimeStateResponse.newBuilder().setKey(20).build()
                .toByteArray(),
            EspHomeMessageType.UPDATE_STATE_RESPONSE to UpdateStateResponse.newBuilder().setKey(21).build()
                .toByteArray(),
        )

        val mapped = cases.map { (messageType, payload) -> EspHomeStateMapper.map(messageType, payload) }

        assertIs<EspHomeState.BinarySensor>(mapped[0])
        assertIs<EspHomeState.Cover>(mapped[1])
        assertIs<EspHomeState.Fan>(mapped[2])
        assertIs<EspHomeState.Light>(mapped[3])
        assertIs<EspHomeState.Sensor>(mapped[4])
        assertIs<EspHomeState.Switch>(mapped[5])
        assertIs<EspHomeState.TextSensor>(mapped[6])
        assertIs<EspHomeState.Climate>(mapped[7])
        assertIs<EspHomeState.Number>(mapped[8])
        assertIs<EspHomeState.Select>(mapped[9])
        assertIs<EspHomeState.Siren>(mapped[10])
        assertIs<EspHomeState.Lock>(mapped[11])
        assertIs<EspHomeState.MediaPlayer>(mapped[12])
        assertIs<EspHomeState.AlarmControlPanel>(mapped[13])
        assertIs<EspHomeState.Text>(mapped[14])
        assertIs<EspHomeState.Date>(mapped[15])
        assertIs<EspHomeState.Time>(mapped[16])
        assertIs<EspHomeState.Event>(mapped[17])
        assertIs<EspHomeState.Valve>(mapped[18])
        assertIs<EspHomeState.DateTime>(mapped[19])
        assertIs<EspHomeState.Update>(mapped[20])
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
