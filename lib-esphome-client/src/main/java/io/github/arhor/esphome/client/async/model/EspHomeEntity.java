package io.github.arhor.esphome.client.async.model;

import java.util.List;

public sealed interface EspHomeEntity {

    int key();

    String objectId();

    String name();

    record BinarySensor(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String deviceClass,
        boolean isStatusBinarySensor,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Cover(
        int key,
        String objectId,
        String name,
        String uniqueId,
        boolean assumedState,
        boolean supportsPosition,
        boolean supportsTilt,
        String deviceClass,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory,
        boolean supportsStop
    ) implements EspHomeEntity {}

    record Fan(
        int key,
        String objectId,
        String name,
        String uniqueId,
        boolean supportsOscillation,
        boolean supportsSpeed,
        boolean supportsDirection,
        int supportedSpeedCount,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory,
        List<String> supportedPresetModes
    ) implements EspHomeEntity {}

    record Light(
        int key,
        String objectId,
        String name,
        String uniqueId,
        List<ColorMode> supportedColorModes,
        float minMireds,
        float maxMireds,
        List<String> effects,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Sensor(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        String unitOfMeasurement,
        int accuracyDecimals,
        boolean forceUpdate,
        String deviceClass,
        SensorStateClass stateClass,
        SensorLastResetType legacyLastResetType,
        boolean disabledByDefault,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Switch(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean assumedState,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass
    ) implements EspHomeEntity {}

    record TextSensor(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass
    ) implements EspHomeEntity {}

    record Service(
        int key,
        String objectId,
        String name,
        List<ServiceArgument> args
    ) implements EspHomeEntity {}

    record Camera(
        int key,
        String objectId,
        String name,
        String uniqueId,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Climate(
        int key,
        String objectId,
        String name,
        String uniqueId,
        boolean supportsCurrentTemperature,
        boolean supportsTwoPointTargetTemperature,
        List<ClimateMode> supportedModes,
        float visualMinTemperature,
        float visualMaxTemperature,
        float visualTargetTemperatureStep,
        boolean legacySupportsAway,
        boolean supportsAction,
        List<ClimateFanMode> supportedFanModes,
        List<ClimateSwingMode> supportedSwingModes,
        List<String> supportedCustomFanModes,
        List<ClimatePreset> supportedPresets,
        List<String> supportedCustomPresets,
        boolean disabledByDefault,
        String icon,
        EntityCategory entityCategory,
        float visualCurrentTemperatureStep,
        boolean supportsCurrentHumidity,
        boolean supportsTargetHumidity,
        float visualMinHumidity,
        float visualMaxHumidity
    ) implements EspHomeEntity {}

    record Number(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        float minValue,
        float maxValue,
        float step,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String unitOfMeasurement,
        NumberMode mode,
        String deviceClass
    ) implements EspHomeEntity {}

    record Select(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        List<String> options,
        boolean disabledByDefault,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Siren(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        List<String> tones,
        boolean supportsDuration,
        boolean supportsVolume,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Lock(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        boolean assumedState,
        boolean supportsOpen,
        boolean requiresCode,
        String codeFormat
    ) implements EspHomeEntity {}

    record Button(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass
    ) implements EspHomeEntity {}

    record MediaPlayer(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        boolean supportsPause,
        List<MediaPlayerFormat> supportedFormats
    ) implements EspHomeEntity {}

    record AlarmControlPanel(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        int supportedFeatures,
        boolean requiresCode,
        boolean requiresCodeToArm
    ) implements EspHomeEntity {}

    record Text(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        int minLength,
        int maxLength,
        String pattern,
        TextMode mode
    ) implements EspHomeEntity {}

    record Date(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Time(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Event(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass,
        List<String> eventTypes
    ) implements EspHomeEntity {}

    record Valve(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass,
        boolean assumedState,
        boolean supportsPosition,
        boolean supportsStop
    ) implements EspHomeEntity {}

    record DateTime(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory
    ) implements EspHomeEntity {}

    record Update(
        int key,
        String objectId,
        String name,
        String uniqueId,
        String icon,
        boolean disabledByDefault,
        EntityCategory entityCategory,
        String deviceClass
    ) implements EspHomeEntity {}
}
