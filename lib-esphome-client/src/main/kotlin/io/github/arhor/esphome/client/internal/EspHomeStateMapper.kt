package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.model.EspHomeState
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
                EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE -> {
                    EspHomeState.BinarySensor(BinarySensorStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.COVER_STATE_RESPONSE -> {
                    EspHomeState.Cover(CoverStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.FAN_STATE_RESPONSE -> {
                    EspHomeState.Fan(FanStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.LIGHT_STATE_RESPONSE -> {
                    EspHomeState.Light(LightStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.SENSOR_STATE_RESPONSE -> {
                    EspHomeState.Sensor(SensorStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.SWITCH_STATE_RESPONSE -> {
                    EspHomeState.Switch(SwitchStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE -> {
                    EspHomeState.TextSensor(TextSensorStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.CLIMATE_STATE_RESPONSE -> {
                    EspHomeState.Climate(ClimateStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.NUMBER_STATE_RESPONSE -> {
                    EspHomeState.Number(NumberStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.SELECT_STATE_RESPONSE -> {
                    EspHomeState.Select(SelectStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.SIREN_STATE_RESPONSE -> {
                    EspHomeState.Siren(SirenStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.LOCK_STATE_RESPONSE -> {
                    EspHomeState.Lock(LockStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE -> {
                    EspHomeState.MediaPlayer(MediaPlayerStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE -> {
                    EspHomeState.AlarmControlPanel(AlarmControlPanelStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.TEXT_STATE_RESPONSE -> {
                    EspHomeState.Text(TextStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.DATE_STATE_RESPONSE -> {
                    EspHomeState.Date(DateStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.TIME_STATE_RESPONSE -> {
                    EspHomeState.Time(TimeStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.EVENT_RESPONSE -> {
                    EspHomeState.Event(EventResponse.parseFrom(payload))
                }

                EspHomeMessageType.VALVE_STATE_RESPONSE -> {
                    EspHomeState.Valve(ValveStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.DATETIME_STATE_RESPONSE -> {
                    EspHomeState.DateTime(DateTimeStateResponse.parseFrom(payload))
                }

                EspHomeMessageType.UPDATE_STATE_RESPONSE -> {
                    EspHomeState.Update(UpdateStateResponse.parseFrom(payload))
                }

                else -> {
                    throw EspHomeProtocolException("Unsupported ESPHome state message: $messageType")
                }
            }
        } catch (ex: InvalidProtocolBufferException) {
            throw EspHomeProtocolException("Malformed ESPHome state payload for message $messageType", ex)
        }
}
