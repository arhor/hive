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
                    EspHomeEntity.BinarySensor(ListEntitiesBinarySensorResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE ->
                    EspHomeEntity.Cover(ListEntitiesCoverResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE ->
                    EspHomeEntity.Fan(ListEntitiesFanResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE ->
                    EspHomeEntity.Light(ListEntitiesLightResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE ->
                    EspHomeEntity.Sensor(ListEntitiesSensorResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE ->
                    EspHomeEntity.Switch(ListEntitiesSwitchResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE ->
                    EspHomeEntity.TextSensor(ListEntitiesTextSensorResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE ->
                    EspHomeEntity.Service(ListEntitiesServicesResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE ->
                    EspHomeEntity.Camera(ListEntitiesCameraResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE ->
                    EspHomeEntity.Climate(ListEntitiesClimateResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE ->
                    EspHomeEntity.Number(ListEntitiesNumberResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE ->
                    EspHomeEntity.Select(ListEntitiesSelectResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE ->
                    EspHomeEntity.Siren(ListEntitiesSirenResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE ->
                    EspHomeEntity.Lock(ListEntitiesLockResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE ->
                    EspHomeEntity.Button(ListEntitiesButtonResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE ->
                    EspHomeEntity.MediaPlayer(ListEntitiesMediaPlayerResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE ->
                    EspHomeEntity.AlarmControlPanel(ListEntitiesAlarmControlPanelResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE ->
                    EspHomeEntity.Text(ListEntitiesTextResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE ->
                    EspHomeEntity.Date(ListEntitiesDateResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE ->
                    EspHomeEntity.Time(ListEntitiesTimeResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE ->
                    EspHomeEntity.Event(ListEntitiesEventResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE ->
                    EspHomeEntity.Valve(ListEntitiesValveResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE ->
                    EspHomeEntity.DateTime(ListEntitiesDateTimeResponse.parseFrom(payload))

                EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE ->
                    EspHomeEntity.Update(ListEntitiesUpdateResponse.parseFrom(payload))

                else -> throw EspHomeProtocolException("Unsupported ESPHome entity discovery message: $messageType")
            }
        } catch (ex: InvalidProtocolBufferException) {
            throw EspHomeProtocolException("Malformed ESPHome entity discovery payload for message $messageType", ex)
        }
}
