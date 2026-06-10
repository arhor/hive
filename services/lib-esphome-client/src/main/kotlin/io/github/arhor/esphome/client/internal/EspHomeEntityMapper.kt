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
                    ListEntitiesBinarySensorResponse.parseFrom(payload).let {
                        EspHomeBinarySensorEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE ->
                    ListEntitiesCoverResponse.parseFrom(payload).let {
                        EspHomeCoverEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE ->
                    ListEntitiesFanResponse.parseFrom(payload).let {
                        EspHomeFanEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE ->
                    ListEntitiesLightResponse.parseFrom(payload).let {
                        EspHomeLightEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE ->
                    ListEntitiesSensorResponse.parseFrom(payload).let {
                        EspHomeSensorEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE ->
                    ListEntitiesSwitchResponse.parseFrom(payload).let {
                        EspHomeSwitchEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE ->
                    ListEntitiesTextSensorResponse.parseFrom(payload).let {
                        EspHomeTextSensorEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE ->
                    ListEntitiesServicesResponse.parseFrom(payload).let {
                        EspHomeServiceEntity(it.key, it.name, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE ->
                    ListEntitiesCameraResponse.parseFrom(payload).let {
                        EspHomeCameraEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE ->
                    ListEntitiesClimateResponse.parseFrom(payload).let {
                        EspHomeClimateEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE ->
                    ListEntitiesNumberResponse.parseFrom(payload).let {
                        EspHomeNumberEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE ->
                    ListEntitiesSelectResponse.parseFrom(payload).let {
                        EspHomeSelectEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE ->
                    ListEntitiesSirenResponse.parseFrom(payload).let {
                        EspHomeSirenEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE ->
                    ListEntitiesLockResponse.parseFrom(payload).let {
                        EspHomeLockEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE ->
                    ListEntitiesButtonResponse.parseFrom(payload).let {
                        EspHomeButtonEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE ->
                    ListEntitiesMediaPlayerResponse.parseFrom(payload).let {
                        EspHomeMediaPlayerEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE ->
                    ListEntitiesAlarmControlPanelResponse.parseFrom(payload).let {
                        EspHomeAlarmControlPanelEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE ->
                    ListEntitiesTextResponse.parseFrom(payload).let {
                        EspHomeTextEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE ->
                    ListEntitiesDateResponse.parseFrom(payload).let {
                        EspHomeDateEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE ->
                    ListEntitiesTimeResponse.parseFrom(payload).let {
                        EspHomeTimeEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE ->
                    ListEntitiesEventResponse.parseFrom(payload).let {
                        EspHomeEventEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE ->
                    ListEntitiesValveResponse.parseFrom(payload).let {
                        EspHomeValveEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE ->
                    ListEntitiesDateTimeResponse.parseFrom(payload).let {
                        EspHomeDateTimeEntity(it.key, it.objectId, it.name, it)
                    }
                EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE ->
                    ListEntitiesUpdateResponse.parseFrom(payload).let {
                        EspHomeUpdateEntity(it.key, it.objectId, it.name, it)
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
