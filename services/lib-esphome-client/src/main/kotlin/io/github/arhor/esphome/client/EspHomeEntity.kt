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

data class EspHomeBinarySensorEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesBinarySensorResponse,
) : EspHomeEntity

data class EspHomeCoverEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesCoverResponse,
) : EspHomeEntity

data class EspHomeFanEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesFanResponse,
) : EspHomeEntity

data class EspHomeLightEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesLightResponse,
) : EspHomeEntity

data class EspHomeSensorEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesSensorResponse,
) : EspHomeEntity

data class EspHomeSwitchEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesSwitchResponse,
) : EspHomeEntity

data class EspHomeTextSensorEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesTextSensorResponse,
) : EspHomeEntity

data class EspHomeServiceEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesServicesResponse,
) : EspHomeEntity

data class EspHomeCameraEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesCameraResponse,
) : EspHomeEntity

data class EspHomeClimateEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesClimateResponse,
) : EspHomeEntity

data class EspHomeNumberEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesNumberResponse,
) : EspHomeEntity

data class EspHomeSelectEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesSelectResponse,
) : EspHomeEntity

data class EspHomeSirenEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesSirenResponse,
) : EspHomeEntity

data class EspHomeLockEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesLockResponse,
) : EspHomeEntity

data class EspHomeButtonEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesButtonResponse,
) : EspHomeEntity

data class EspHomeMediaPlayerEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesMediaPlayerResponse,
) : EspHomeEntity

data class EspHomeAlarmControlPanelEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesAlarmControlPanelResponse,
) : EspHomeEntity

data class EspHomeTextEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesTextResponse,
) : EspHomeEntity

data class EspHomeDateEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesDateResponse,
) : EspHomeEntity

data class EspHomeTimeEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesTimeResponse,
) : EspHomeEntity

data class EspHomeEventEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesEventResponse,
) : EspHomeEntity

data class EspHomeValveEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesValveResponse,
) : EspHomeEntity

data class EspHomeDateTimeEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesDateTimeResponse,
) : EspHomeEntity

data class EspHomeUpdateEntity(
    override val key: Int,
    override val objectId: String,
    override val name: String,
    val raw: ListEntitiesUpdateResponse,
) : EspHomeEntity
