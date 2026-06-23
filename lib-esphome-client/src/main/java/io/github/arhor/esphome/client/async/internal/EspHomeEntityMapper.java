package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.EspHomeEntity;
import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.proto.ListEntitiesAlarmControlPanelResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesButtonResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesClimateResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesCoverResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesDateResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesDateTimeResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesEventResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesFanResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesLightResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesLockResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesMediaPlayerResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesNumberResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesSelectResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesServicesResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesSirenResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesTextResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesTextSensorResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesTimeResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesUpdateResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesValveResponse;

public class EspHomeEntityMapper {

    public static EspHomeEntity map(final Object message) {
        return switch (message) {
            case ListEntitiesBinarySensorResponse res -> new EspHomeEntity.BinarySensor(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getDeviceClass(),
                res.getIsStatusBinarySensor(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory()
            );

            case ListEntitiesCoverResponse res -> new EspHomeEntity.Cover(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getAssumedState(),
                res.getSupportsPosition(),
                res.getSupportsTilt(),
                res.getDeviceClass(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory(),
                res.getSupportsStop()
            );

            case ListEntitiesFanResponse res -> new EspHomeEntity.Fan(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getSupportsOscillation(),
                res.getSupportsSpeed(),
                res.getSupportsDirection(),
                res.getSupportedSpeedCount(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory(),
                res.getSupportedPresetModesList()
            );

            case ListEntitiesLightResponse res -> new EspHomeEntity.Light(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getSupportedColorModesList(),
                res.getMinMireds(),
                res.getMaxMireds(),
                res.getEffectsList(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory()
            );

            case ListEntitiesSensorResponse res -> new EspHomeEntity.Sensor(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getUnitOfMeasurement(),
                res.getAccuracyDecimals(),
                res.getForceUpdate(),
                res.getDeviceClass(),
                res.getStateClass(),
                res.getLegacyLastResetType(),
                res.getDisabledByDefault(),
                res.getEntityCategory()
            );

            case ListEntitiesSwitchResponse res -> new EspHomeEntity.Switch(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getAssumedState(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass()
            );

            case ListEntitiesTextSensorResponse res -> new EspHomeEntity.TextSensor(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass()
            );

            case ListEntitiesServicesResponse res -> new EspHomeEntity.Service(
                res.getKey(),
                res.getName(),
                res.getName(),
                res.getArgsList()
            );

            case ListEntitiesCameraResponse res -> new EspHomeEntity.Camera(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory()
            );

            case ListEntitiesClimateResponse res -> new EspHomeEntity.Climate(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getSupportsCurrentTemperature(),
                res.getSupportsTwoPointTargetTemperature(),
                res.getSupportedModesList(),
                res.getVisualMinTemperature(),
                res.getVisualMaxTemperature(),
                res.getVisualTargetTemperatureStep(),
                res.getLegacySupportsAway(),
                res.getSupportsAction(),
                res.getSupportedFanModesList(),
                res.getSupportedSwingModesList(),
                res.getSupportedCustomFanModesList(),
                res.getSupportedPresetsList(),
                res.getSupportedCustomPresetsList(),
                res.getDisabledByDefault(),
                res.getIcon(),
                res.getEntityCategory(),
                res.getVisualCurrentTemperatureStep(),
                res.getSupportsCurrentHumidity(),
                res.getSupportsTargetHumidity(),
                res.getVisualMinHumidity(),
                res.getVisualMaxHumidity()
            );

            case ListEntitiesNumberResponse res -> new EspHomeEntity.Number(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getMinValue(),
                res.getMaxValue(),
                res.getStep(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getUnitOfMeasurement(),
                res.getMode(),
                res.getDeviceClass()
            );

            case ListEntitiesSelectResponse res -> new EspHomeEntity.Select(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getOptionsList(),
                res.getDisabledByDefault(),
                res.getEntityCategory()
            );

            case ListEntitiesSirenResponse res -> new EspHomeEntity.Siren(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getTonesList(),
                res.getSupportsDuration(),
                res.getSupportsVolume(),
                res.getEntityCategory()
            );

            case ListEntitiesLockResponse res -> new EspHomeEntity.Lock(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getAssumedState(),
                res.getSupportsOpen(),
                res.getRequiresCode(),
                res.getCodeFormat()
            );

            case ListEntitiesButtonResponse res -> new EspHomeEntity.Button(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass()
            );

            case ListEntitiesMediaPlayerResponse res -> new EspHomeEntity.MediaPlayer(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getSupportsPause(),
                res.getSupportedFormatsList()
            );

            case ListEntitiesAlarmControlPanelResponse res -> new EspHomeEntity.AlarmControlPanel(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getSupportedFeatures(),
                res.getRequiresCode(),
                res.getRequiresCodeToArm()
            );

            case ListEntitiesTextResponse res -> new EspHomeEntity.Text(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getMinLength(),
                res.getMaxLength(),
                res.getPattern(),
                res.getMode()
            );

            case ListEntitiesDateResponse res -> new EspHomeEntity.Date(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory()
            );

            case ListEntitiesTimeResponse res -> new EspHomeEntity.Time(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory()
            );

            case ListEntitiesEventResponse res -> new EspHomeEntity.Event(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass(),
                res.getEventTypesList()
            );

            case ListEntitiesValveResponse res -> new EspHomeEntity.Valve(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass(),
                res.getAssumedState(),
                res.getSupportsPosition(),
                res.getSupportsStop()
            );

            case ListEntitiesDateTimeResponse res -> new EspHomeEntity.DateTime(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory()
            );

            case ListEntitiesUpdateResponse res -> new EspHomeEntity.Update(
                res.getKey(),
                res.getObjectId(),
                res.getName(),
                res.getUniqueId(),
                res.getIcon(),
                res.getDisabledByDefault(),
                res.getEntityCategory(),
                res.getDeviceClass()
            );

            case null, default -> throw new EspHomeProtocolException(
                "Unsupported entity discovery message: " + (
                    message != null
                        ? message.getClass().getName()
                        : null
                )
            );
        };
    }

    private EspHomeEntityMapper() {}
}
