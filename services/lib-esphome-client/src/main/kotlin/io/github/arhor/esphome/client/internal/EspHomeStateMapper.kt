package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
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

internal object EspHomeStateMapper {

    fun map(messageType: Int, payload: ByteArray): EspHomeState =
        try {
            when (messageType) {
                EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE ->
                    BinarySensorStateResponse.parseFrom(payload).let {
                        EspHomeState.BinarySensor(it)
                    }

                EspHomeMessageType.COVER_STATE_RESPONSE ->
                    CoverStateResponse.parseFrom(payload).let {
                        EspHomeState.Cover(it)
                    }

                EspHomeMessageType.FAN_STATE_RESPONSE ->
                    FanStateResponse.parseFrom(payload).let {
                        EspHomeState.Fan(it)
                    }

                EspHomeMessageType.LIGHT_STATE_RESPONSE ->
                    LightStateResponse.parseFrom(payload).let {
                        EspHomeState.Light(it)
                    }

                EspHomeMessageType.SENSOR_STATE_RESPONSE ->
                    SensorStateResponse.parseFrom(payload).let {
                        EspHomeState.Sensor(it)
                    }

                EspHomeMessageType.SWITCH_STATE_RESPONSE ->
                    SwitchStateResponse.parseFrom(payload).let {
                        EspHomeState.Switch(it)
                    }

                EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE ->
                    TextSensorStateResponse.parseFrom(payload).let {
                        EspHomeState.TextSensor(it)
                    }

                EspHomeMessageType.CLIMATE_STATE_RESPONSE ->
                    ClimateStateResponse.parseFrom(payload).let {
                        EspHomeState.Climate(it)
                    }

                EspHomeMessageType.NUMBER_STATE_RESPONSE ->
                    NumberStateResponse.parseFrom(payload).let {
                        EspHomeState.Number(it)
                    }

                EspHomeMessageType.SELECT_STATE_RESPONSE ->
                    SelectStateResponse.parseFrom(payload).let {
                        EspHomeState.Select(it)
                    }

                EspHomeMessageType.SIREN_STATE_RESPONSE ->
                    SirenStateResponse.parseFrom(payload).let {
                        EspHomeState.Siren(it)
                    }

                EspHomeMessageType.LOCK_STATE_RESPONSE ->
                    LockStateResponse.parseFrom(payload).let {
                        EspHomeState.Lock(it)
                    }

                EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE ->
                    MediaPlayerStateResponse.parseFrom(payload).let {
                        EspHomeState.MediaPlayer(it)
                    }

                EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE ->
                    AlarmControlPanelStateResponse.parseFrom(payload).let {
                        EspHomeState.AlarmControlPanel(it)
                    }

                EspHomeMessageType.TEXT_STATE_RESPONSE ->
                    TextStateResponse.parseFrom(payload).let {
                        EspHomeState.Text(it)
                    }

                EspHomeMessageType.DATE_STATE_RESPONSE ->
                    DateStateResponse.parseFrom(payload).let {
                        EspHomeState.Date(it)
                    }

                EspHomeMessageType.TIME_STATE_RESPONSE ->
                    TimeStateResponse.parseFrom(payload).let {
                        EspHomeState.Time(it)
                    }

                EspHomeMessageType.EVENT_RESPONSE ->
                    EventResponse.parseFrom(payload).let {
                        EspHomeState.Event(it)
                    }

                EspHomeMessageType.VALVE_STATE_RESPONSE ->
                    ValveStateResponse.parseFrom(payload).let {
                        EspHomeState.Valve(it)
                    }

                EspHomeMessageType.DATETIME_STATE_RESPONSE ->
                    DateTimeStateResponse.parseFrom(payload).let {
                        EspHomeState.DateTime(it)
                    }

                EspHomeMessageType.UPDATE_STATE_RESPONSE ->
                    UpdateStateResponse.parseFrom(payload).let {
                        EspHomeState.Update(it)
                    }

                else -> throw EspHomeProtocolException("Unsupported ESPHome state message: $messageType")
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw EspHomeProtocolException(
                "Malformed ESPHome state payload for message $messageType",
                exception,
            )
        }
}
