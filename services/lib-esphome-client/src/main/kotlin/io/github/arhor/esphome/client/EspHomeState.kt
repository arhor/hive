package io.github.arhor.esphome.client

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

fun interface EspHomeStateHandler {
    fun onState(state: EspHomeState)
}

sealed interface EspHomeState {
    val key: Int
}

data class EspHomeBinarySensorState(
    override val key: Int,
    val raw: BinarySensorStateResponse,
) : EspHomeState {
    val state: Boolean get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeCoverState(
    override val key: Int,
    val raw: CoverStateResponse,
) : EspHomeState {
    val position: Float get() = raw.position
    val tilt: Float get() = raw.tilt
    val currentOperation get() = raw.currentOperation
}

data class EspHomeFanState(
    override val key: Int,
    val raw: FanStateResponse,
) : EspHomeState {
    val state: Boolean get() = raw.state
    val oscillating: Boolean get() = raw.oscillating
    val direction get() = raw.direction
    val speedLevel: Int get() = raw.speedLevel
    val presetMode: String get() = raw.presetMode
}

data class EspHomeLightState(
    override val key: Int,
    val raw: LightStateResponse,
) : EspHomeState {
    val state: Boolean get() = raw.state
    val brightness: Float get() = raw.brightness
    val colorMode get() = raw.colorMode
    val colorBrightness: Float get() = raw.colorBrightness
    val red: Float get() = raw.red
    val green: Float get() = raw.green
    val blue: Float get() = raw.blue
    val white: Float get() = raw.white
    val colorTemperature: Float get() = raw.colorTemperature
    val coldWhite: Float get() = raw.coldWhite
    val warmWhite: Float get() = raw.warmWhite
    val effect: String get() = raw.effect
}

data class EspHomeSensorState(
    override val key: Int,
    val raw: SensorStateResponse,
) : EspHomeState {
    val state: Float get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeSwitchState(
    override val key: Int,
    val raw: SwitchStateResponse,
) : EspHomeState {
    val state: Boolean get() = raw.state
}

data class EspHomeTextSensorState(
    override val key: Int,
    val raw: TextSensorStateResponse,
) : EspHomeState {
    val state: String get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeClimateState(
    override val key: Int,
    val raw: ClimateStateResponse,
) : EspHomeState {
    val mode get() = raw.mode
    val currentTemperature: Float get() = raw.currentTemperature
    val targetTemperature: Float get() = raw.targetTemperature
    val targetTemperatureLow: Float get() = raw.targetTemperatureLow
    val targetTemperatureHigh: Float get() = raw.targetTemperatureHigh
    val action get() = raw.action
    val fanMode get() = raw.fanMode
    val swingMode get() = raw.swingMode
    val customFanMode: String get() = raw.customFanMode
    val preset get() = raw.preset
    val customPreset: String get() = raw.customPreset
    val currentHumidity: Float get() = raw.currentHumidity
    val targetHumidity: Float get() = raw.targetHumidity
}

data class EspHomeNumberState(
    override val key: Int,
    val raw: NumberStateResponse,
) : EspHomeState {
    val state: Float get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeSelectState(
    override val key: Int,
    val raw: SelectStateResponse,
) : EspHomeState {
    val state: String get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeSirenState(
    override val key: Int,
    val raw: SirenStateResponse,
) : EspHomeState {
    val state: Boolean get() = raw.state
}

data class EspHomeLockState(
    override val key: Int,
    val raw: LockStateResponse,
) : EspHomeState {
    val state get() = raw.state
}

data class EspHomeMediaPlayerState(
    override val key: Int,
    val raw: MediaPlayerStateResponse,
) : EspHomeState {
    val state get() = raw.state
    val volume: Float get() = raw.volume
    val muted: Boolean get() = raw.muted
}

data class EspHomeAlarmControlPanelState(
    override val key: Int,
    val raw: AlarmControlPanelStateResponse,
) : EspHomeState {
    val state get() = raw.state
}

data class EspHomeTextState(
    override val key: Int,
    val raw: TextStateResponse,
) : EspHomeState {
    val state: String get() = raw.state
    val missingState: Boolean get() = raw.missingState
}

data class EspHomeDateState(
    override val key: Int,
    val raw: DateStateResponse,
) : EspHomeState {
    val missingState: Boolean get() = raw.missingState
    val year: Int get() = raw.year
    val month: Int get() = raw.month
    val day: Int get() = raw.day
}

data class EspHomeTimeState(
    override val key: Int,
    val raw: TimeStateResponse,
) : EspHomeState {
    val missingState: Boolean get() = raw.missingState
    val hour: Int get() = raw.hour
    val minute: Int get() = raw.minute
    val second: Int get() = raw.second
}

data class EspHomeEventState(
    override val key: Int,
    val raw: EventResponse,
) : EspHomeState {
    val eventType: String get() = raw.eventType
}

data class EspHomeValveState(
    override val key: Int,
    val raw: ValveStateResponse,
) : EspHomeState {
    val position: Float get() = raw.position
    val currentOperation get() = raw.currentOperation
}

data class EspHomeDateTimeState(
    override val key: Int,
    val raw: DateTimeStateResponse,
) : EspHomeState {
    val missingState: Boolean get() = raw.missingState
    val epochSeconds: Int get() = raw.epochSeconds
}

data class EspHomeUpdateState(
    override val key: Int,
    val raw: UpdateStateResponse,
) : EspHomeState {
    val missingState: Boolean get() = raw.missingState
    val inProgress: Boolean get() = raw.inProgress
    val hasProgress: Boolean get() = raw.hasProgress
    val progress: Float get() = raw.progress
    val currentVersion: String get() = raw.currentVersion
    val latestVersion: String get() = raw.latestVersion
    val title: String get() = raw.title
    val releaseSummary: String get() = raw.releaseSummary
    val releaseUrl: String get() = raw.releaseUrl
}
