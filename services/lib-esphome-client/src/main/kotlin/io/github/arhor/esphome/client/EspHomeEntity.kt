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

    data class BinarySensor(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesBinarySensorResponse,
    ) : EspHomeEntity

    data class Cover(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesCoverResponse,
    ) : EspHomeEntity

    data class Fan(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesFanResponse,
    ) : EspHomeEntity

    data class Light(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesLightResponse,
    ) : EspHomeEntity

    data class Sensor(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesSensorResponse,
    ) : EspHomeEntity

    data class Switch(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesSwitchResponse,
    ) : EspHomeEntity

    data class TextSensor(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesTextSensorResponse,
    ) : EspHomeEntity

    data class Service(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesServicesResponse,
    ) : EspHomeEntity

    data class Camera(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesCameraResponse,
    ) : EspHomeEntity

    data class Climate(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesClimateResponse,
    ) : EspHomeEntity

    data class Number(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesNumberResponse,
    ) : EspHomeEntity

    data class Select(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesSelectResponse,
    ) : EspHomeEntity

    data class Siren(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesSirenResponse,
    ) : EspHomeEntity

    data class Lock(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesLockResponse,
    ) : EspHomeEntity

    data class Button(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesButtonResponse,
    ) : EspHomeEntity

    data class MediaPlayer(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesMediaPlayerResponse,
    ) : EspHomeEntity

    data class AlarmControlPanel(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesAlarmControlPanelResponse,
    ) : EspHomeEntity

    data class Text(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesTextResponse,
    ) : EspHomeEntity

    data class Date(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesDateResponse,
    ) : EspHomeEntity

    data class Time(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesTimeResponse,
    ) : EspHomeEntity

    data class Event(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesEventResponse,
    ) : EspHomeEntity

    data class Valve(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesValveResponse,
    ) : EspHomeEntity

    data class DateTime(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesDateTimeResponse,
    ) : EspHomeEntity

    data class Update(
        override val key: Int,
        override val objectId: String,
        override val name: String,
        val raw: ListEntitiesUpdateResponse,
    ) : EspHomeEntity
}
