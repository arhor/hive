package io.github.arhor.esphome.client.internal

import com.google.protobuf.InvalidProtocolBufferException
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

internal object EspHomeEntityMapper {

    fun map(messageType: Int, payload: ByteArray): EspHomeEntity =
        try {
            when (messageType) {
                EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE ->
                    ListEntitiesBinarySensorResponse.parseFrom(payload).let {
                        EspHomeEntity.BinarySensor(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE ->
                    ListEntitiesCoverResponse.parseFrom(payload).let {
                        EspHomeEntity.Cover(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE ->
                    ListEntitiesFanResponse.parseFrom(payload).let {
                        EspHomeEntity.Fan(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE ->
                    ListEntitiesLightResponse.parseFrom(payload).let {
                        EspHomeEntity.Light(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE ->
                    ListEntitiesSensorResponse.parseFrom(payload).let {
                        EspHomeEntity.Sensor(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE ->
                    ListEntitiesSwitchResponse.parseFrom(payload).let {
                        EspHomeEntity.Switch(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE ->
                    ListEntitiesTextSensorResponse.parseFrom(payload).let {
                        EspHomeEntity.TextSensor(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE ->
                    ListEntitiesServicesResponse.parseFrom(payload).let {
                        EspHomeEntity.Service(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE ->
                    ListEntitiesCameraResponse.parseFrom(payload).let {
                        EspHomeEntity.Camera(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE ->
                    ListEntitiesClimateResponse.parseFrom(payload).let {
                        EspHomeEntity.Climate(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE ->
                    ListEntitiesNumberResponse.parseFrom(payload).let {
                        EspHomeEntity.Number(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE ->
                    ListEntitiesSelectResponse.parseFrom(payload).let {
                        EspHomeEntity.Select(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE ->
                    ListEntitiesSirenResponse.parseFrom(payload).let {
                        EspHomeEntity.Siren(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE ->
                    ListEntitiesLockResponse.parseFrom(payload).let {
                        EspHomeEntity.Lock(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE ->
                    ListEntitiesButtonResponse.parseFrom(payload).let {
                        EspHomeEntity.Button(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE ->
                    ListEntitiesMediaPlayerResponse.parseFrom(payload).let {
                        EspHomeEntity.MediaPlayer(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE ->
                    ListEntitiesAlarmControlPanelResponse.parseFrom(payload).let {
                        EspHomeEntity.AlarmControlPanel(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE ->
                    ListEntitiesTextResponse.parseFrom(payload).let {
                        EspHomeEntity.Text(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE ->
                    ListEntitiesDateResponse.parseFrom(payload).let {
                        EspHomeEntity.Date(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE ->
                    ListEntitiesTimeResponse.parseFrom(payload).let {
                        EspHomeEntity.Time(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE ->
                    ListEntitiesEventResponse.parseFrom(payload).let {
                        EspHomeEntity.Event(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE ->
                    ListEntitiesValveResponse.parseFrom(payload).let {
                        EspHomeEntity.Valve(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE ->
                    ListEntitiesDateTimeResponse.parseFrom(payload).let {
                        EspHomeEntity.DateTime(it)
                    }

                EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE ->
                    ListEntitiesUpdateResponse.parseFrom(payload).let {
                        EspHomeEntity.Update(it)
                    }

                else -> throw EspHomeProtocolException("Unsupported ESPHome entity discovery message: $messageType")
            }
        } catch (exception: InvalidProtocolBufferException) {
            throw EspHomeProtocolException(
                "Malformed ESPHome entity discovery payload for message $messageType",
                exception,
            )
        }
}
