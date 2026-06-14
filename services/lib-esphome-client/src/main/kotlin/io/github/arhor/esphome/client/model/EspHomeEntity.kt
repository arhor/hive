package io.github.arhor.esphome.client.model

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

    @JvmInline
    value class BinarySensor(val raw: ListEntitiesBinarySensorResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Cover(val raw: ListEntitiesCoverResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Fan(val raw: ListEntitiesFanResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Light(val raw: ListEntitiesLightResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Sensor(val raw: ListEntitiesSensorResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Switch(val raw: ListEntitiesSwitchResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class TextSensor(val raw: ListEntitiesTextSensorResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Service(val raw: ListEntitiesServicesResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.name

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Camera(val raw: ListEntitiesCameraResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Climate(val raw: ListEntitiesClimateResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Number(val raw: ListEntitiesNumberResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Select(val raw: ListEntitiesSelectResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Siren(val raw: ListEntitiesSirenResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Lock(val raw: ListEntitiesLockResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Button(val raw: ListEntitiesButtonResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class MediaPlayer(val raw: ListEntitiesMediaPlayerResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class AlarmControlPanel(val raw: ListEntitiesAlarmControlPanelResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Text(val raw: ListEntitiesTextResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Date(val raw: ListEntitiesDateResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Time(val raw: ListEntitiesTimeResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Event(val raw: ListEntitiesEventResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Valve(val raw: ListEntitiesValveResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class DateTime(val raw: ListEntitiesDateTimeResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }

    @JvmInline
    value class Update(val raw: ListEntitiesUpdateResponse) : EspHomeEntity {
        override val key: Int
            get() = raw.key

        override val objectId: String
            get() = raw.objectId

        override val name: String
            get() = raw.name
    }
}
