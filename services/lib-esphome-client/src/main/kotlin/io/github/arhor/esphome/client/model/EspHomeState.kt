package io.github.arhor.esphome.client.model

import io.github.arhor.esphome.client.proto.AlarmControlPanelState
import io.github.arhor.esphome.client.proto.AlarmControlPanelStateResponse
import io.github.arhor.esphome.client.proto.BinarySensorStateResponse
import io.github.arhor.esphome.client.proto.ClimateAction
import io.github.arhor.esphome.client.proto.ClimateFanMode
import io.github.arhor.esphome.client.proto.ClimateMode
import io.github.arhor.esphome.client.proto.ClimatePreset
import io.github.arhor.esphome.client.proto.ClimateStateResponse
import io.github.arhor.esphome.client.proto.ClimateSwingMode
import io.github.arhor.esphome.client.proto.ColorMode
import io.github.arhor.esphome.client.proto.CoverOperation
import io.github.arhor.esphome.client.proto.CoverStateResponse
import io.github.arhor.esphome.client.proto.DateStateResponse
import io.github.arhor.esphome.client.proto.DateTimeStateResponse
import io.github.arhor.esphome.client.proto.EventResponse
import io.github.arhor.esphome.client.proto.FanDirection
import io.github.arhor.esphome.client.proto.FanStateResponse
import io.github.arhor.esphome.client.proto.LightStateResponse
import io.github.arhor.esphome.client.proto.LockState
import io.github.arhor.esphome.client.proto.LockStateResponse
import io.github.arhor.esphome.client.proto.MediaPlayerState
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
import io.github.arhor.esphome.client.proto.ValveOperation
import io.github.arhor.esphome.client.proto.ValveStateResponse

sealed interface EspHomeState {
    val key: Int

    @JvmInline
    value class BinarySensor(val raw: BinarySensorStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Boolean
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Cover(val raw: CoverStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val position: Float
            get() = raw.position

        val tilt: Float
            get() = raw.tilt

        val currentOperation: CoverOperation?
            get() = raw.currentOperation
    }

    @JvmInline
    value class Fan(val raw: FanStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key
        val state: Boolean
            get() = raw.state

        val oscillating: Boolean
            get() = raw.oscillating

        val direction: FanDirection?
            get() = raw.direction

        val speedLevel: Int
            get() = raw.speedLevel

        val presetMode: String
            get() = raw.presetMode
    }

    @JvmInline
    value class Light(val raw: LightStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Boolean
            get() = raw.state

        val brightness: Float
            get() = raw.brightness

        val colorMode: ColorMode?
            get() = raw.colorMode

        val colorBrightness: Float
            get() = raw.colorBrightness

        val red: Float
            get() = raw.red

        val green: Float
            get() = raw.green

        val blue: Float
            get() = raw.blue

        val white: Float
            get() = raw.white

        val colorTemperature: Float
            get() = raw.colorTemperature

        val coldWhite: Float
            get() = raw.coldWhite

        val warmWhite: Float
            get() = raw.warmWhite

        val effect: String
            get() = raw.effect
    }

    @JvmInline
    value class Sensor(val raw: SensorStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Float
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Switch(val raw: SwitchStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Boolean
            get() = raw.state
    }

    @JvmInline
    value class TextSensor(val raw: TextSensorStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: String
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Climate(val raw: ClimateStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val mode: ClimateMode?
            get() = raw.mode

        val currentTemperature: Float
            get() = raw.currentTemperature

        val targetTemperature: Float
            get() = raw.targetTemperature

        val targetTemperatureLow: Float
            get() = raw.targetTemperatureLow

        val targetTemperatureHigh: Float
            get() = raw.targetTemperatureHigh

        val action: ClimateAction?
            get() = raw.action

        val fanMode: ClimateFanMode?
            get() = raw.fanMode

        val swingMode: ClimateSwingMode?
            get() = raw.swingMode

        val customFanMode: String
            get() = raw.customFanMode

        val preset: ClimatePreset?
            get() = raw.preset

        val customPreset: String
            get() = raw.customPreset

        val currentHumidity: Float
            get() = raw.currentHumidity

        val targetHumidity: Float
            get() = raw.targetHumidity
    }

    @JvmInline
    value class Number(val raw: NumberStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Float
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Select(val raw: SelectStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: String
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Siren(val raw: SirenStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: Boolean
            get() = raw.state
    }

    @JvmInline
    value class Lock(val raw: LockStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: LockState?
            get() = raw.state
    }

    @JvmInline
    value class MediaPlayer(val raw: MediaPlayerStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: MediaPlayerState?
            get() = raw.state

        val volume: Float
            get() = raw.volume

        val muted: Boolean
            get() = raw.muted
    }

    @JvmInline
    value class AlarmControlPanel(val raw: AlarmControlPanelStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: AlarmControlPanelState?
            get() = raw.state
    }

    @JvmInline
    value class Text(val raw: TextStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val state: String
            get() = raw.state

        val missingState: Boolean
            get() = raw.missingState
    }

    @JvmInline
    value class Date(val raw: DateStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val missingState: Boolean
            get() = raw.missingState

        val year: Int
            get() = raw.year

        val month: Int
            get() = raw.month

        val day: Int
            get() = raw.day

    }

    @JvmInline
    value class Time(val raw: TimeStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val missingState: Boolean
            get() = raw.missingState

        val hour: Int
            get() = raw.hour

        val minute: Int
            get() = raw.minute

        val second: Int
            get() = raw.second
    }

    @JvmInline
    value class Event(val raw: EventResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val eventType: String
            get() = raw.eventType
    }

    @JvmInline
    value class Valve(val raw: ValveStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val position: Float
            get() = raw.position

        val currentOperation: ValveOperation?
            get() = raw.currentOperation
    }

    @JvmInline
    value class DateTime(val raw: DateTimeStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val missingState: Boolean
            get() = raw.missingState

        val epochSeconds: Int
            get() = raw.epochSeconds
    }

    @JvmInline
    value class Update(val raw: UpdateStateResponse) : EspHomeState {
        override val key: Int
            get() = raw.key

        val missingState: Boolean
            get() = raw.missingState

        val inProgress: Boolean
            get() = raw.inProgress

        val hasProgress: Boolean
            get() = raw.hasProgress

        val progress: Float
            get() = raw.progress

        val currentVersion: String
            get() = raw.currentVersion

        val latestVersion: String
            get() = raw.latestVersion

        val title: String
            get() = raw.title

        val releaseSummary: String
            get() = raw.releaseSummary

        val releaseUrl: String
            get() = raw.releaseUrl
    }
}
