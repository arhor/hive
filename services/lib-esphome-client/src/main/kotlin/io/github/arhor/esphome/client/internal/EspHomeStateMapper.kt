package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
import io.github.arhor.esphome.client.EspHomeAlarmControlPanelState
import io.github.arhor.esphome.client.EspHomeBinarySensorState
import io.github.arhor.esphome.client.EspHomeClimateState
import io.github.arhor.esphome.client.EspHomeCoverState
import io.github.arhor.esphome.client.EspHomeDateState
import io.github.arhor.esphome.client.EspHomeDateTimeState
import io.github.arhor.esphome.client.EspHomeEventState
import io.github.arhor.esphome.client.EspHomeFanState
import io.github.arhor.esphome.client.EspHomeLightState
import io.github.arhor.esphome.client.EspHomeLockState
import io.github.arhor.esphome.client.EspHomeMediaPlayerState
import io.github.arhor.esphome.client.EspHomeNumberState
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.EspHomeSelectState
import io.github.arhor.esphome.client.EspHomeSensorState
import io.github.arhor.esphome.client.EspHomeSirenState
import io.github.arhor.esphome.client.EspHomeState
import io.github.arhor.esphome.client.EspHomeSwitchState
import io.github.arhor.esphome.client.EspHomeTextSensorState
import io.github.arhor.esphome.client.EspHomeTextState
import io.github.arhor.esphome.client.EspHomeTimeState
import io.github.arhor.esphome.client.EspHomeUpdateState
import io.github.arhor.esphome.client.EspHomeValveState
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
                    AlarmControlPanelStateResponse.parseFrom(payload).let {
                        EspHomeAlarmControlPanelState(it.key, it)
                    }
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
