package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.ClimateFanMode;
import io.github.arhor.esphome.client.async.ClimateMode;
import io.github.arhor.esphome.client.async.ClimatePreset;
import io.github.arhor.esphome.client.async.ClimateSwingMode;
import io.github.arhor.esphome.client.async.ColorMode;
import io.github.arhor.esphome.client.async.EntityCategory;
import io.github.arhor.esphome.client.async.MediaPlayerFormat;
import io.github.arhor.esphome.client.async.MediaPlayerFormatPurpose;
import io.github.arhor.esphome.client.async.NumberMode;
import io.github.arhor.esphome.client.async.SensorLastResetType;
import io.github.arhor.esphome.client.async.SensorStateClass;
import io.github.arhor.esphome.client.async.ServiceArgType;
import io.github.arhor.esphome.client.async.ServiceArgument;
import io.github.arhor.esphome.client.async.TextMode;
import io.github.arhor.esphome.client.proto.ListEntitiesServicesArgument;

import java.util.List;

final class EspHomeProtoTypeMapper {

    static EntityCategory mapEntityCategory(
        final io.github.arhor.esphome.client.proto.EntityCategory value
    ) {
        if (value == null) {
            return EntityCategory.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> EntityCategory.UNKNOWN;
            case ENTITY_CATEGORY_NONE -> EntityCategory.NONE;
            case ENTITY_CATEGORY_CONFIG -> EntityCategory.CONFIG;
            case ENTITY_CATEGORY_DIAGNOSTIC -> EntityCategory.DIAGNOSTIC;

        };
    }

    static ColorMode mapColorMode(final io.github.arhor.esphome.client.proto.ColorMode value) {
        if (value == null) {
            return ColorMode.UNKNOWN;
        }
        return switch (value) {
            case COLOR_MODE_UNKNOWN, UNRECOGNIZED -> ColorMode.UNKNOWN;
            case COLOR_MODE_ON_OFF -> ColorMode.ON_OFF;
            case COLOR_MODE_BRIGHTNESS -> ColorMode.BRIGHTNESS;
            case COLOR_MODE_WHITE -> ColorMode.WHITE;
            case COLOR_MODE_COLOR_TEMPERATURE -> ColorMode.COLOR_TEMPERATURE;
            case COLOR_MODE_COLD_WARM_WHITE -> ColorMode.COLD_WARM_WHITE;
            case COLOR_MODE_RGB -> ColorMode.RGB;
            case COLOR_MODE_RGB_WHITE -> ColorMode.RGB_WHITE;
            case COLOR_MODE_RGB_COLOR_TEMPERATURE -> ColorMode.RGB_COLOR_TEMPERATURE;
            case COLOR_MODE_RGB_COLD_WARM_WHITE -> ColorMode.RGB_COLD_WARM_WHITE;
        };
    }

    static List<ColorMode> mapColorModes(
        final List<io.github.arhor.esphome.client.proto.ColorMode> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapColorMode).toList();
    }

    static SensorStateClass mapSensorStateClass(
        final io.github.arhor.esphome.client.proto.SensorStateClass value
    ) {
        if (value == null) {
            return SensorStateClass.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> SensorStateClass.UNKNOWN;
            case STATE_CLASS_NONE -> SensorStateClass.NONE;
            case STATE_CLASS_MEASUREMENT -> SensorStateClass.MEASUREMENT;
            case STATE_CLASS_TOTAL_INCREASING -> SensorStateClass.TOTAL_INCREASING;
            case STATE_CLASS_TOTAL -> SensorStateClass.TOTAL;
        };
    }

    static SensorLastResetType mapSensorLastResetType(
        final io.github.arhor.esphome.client.proto.SensorLastResetType value
    ) {
        if (value == null) {
            return SensorLastResetType.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> SensorLastResetType.UNKNOWN;
            case LAST_RESET_NONE -> SensorLastResetType.NONE;
            case LAST_RESET_NEVER -> SensorLastResetType.NEVER;
            case LAST_RESET_AUTO -> SensorLastResetType.AUTO;
        };
    }

    static ClimateMode mapClimateMode(final io.github.arhor.esphome.client.proto.ClimateMode value) {
        if (value == null) {
            return ClimateMode.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> ClimateMode.UNKNOWN;
            case CLIMATE_MODE_OFF -> ClimateMode.OFF;
            case CLIMATE_MODE_HEAT_COOL -> ClimateMode.HEAT_COOL;
            case CLIMATE_MODE_COOL -> ClimateMode.COOL;
            case CLIMATE_MODE_HEAT -> ClimateMode.HEAT;
            case CLIMATE_MODE_FAN_ONLY -> ClimateMode.FAN_ONLY;
            case CLIMATE_MODE_DRY -> ClimateMode.DRY;
            case CLIMATE_MODE_AUTO -> ClimateMode.AUTO;
        };
    }

    static List<ClimateMode> mapClimateModes(
        final List<io.github.arhor.esphome.client.proto.ClimateMode> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapClimateMode).toList();
    }

    static ClimateFanMode mapClimateFanMode(
        final io.github.arhor.esphome.client.proto.ClimateFanMode value
    ) {
        if (value == null) {
            return ClimateFanMode.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> ClimateFanMode.UNKNOWN;
            case CLIMATE_FAN_ON -> ClimateFanMode.ON;
            case CLIMATE_FAN_OFF -> ClimateFanMode.OFF;
            case CLIMATE_FAN_AUTO -> ClimateFanMode.AUTO;
            case CLIMATE_FAN_LOW -> ClimateFanMode.LOW;
            case CLIMATE_FAN_MEDIUM -> ClimateFanMode.MEDIUM;
            case CLIMATE_FAN_HIGH -> ClimateFanMode.HIGH;
            case CLIMATE_FAN_MIDDLE -> ClimateFanMode.MIDDLE;
            case CLIMATE_FAN_FOCUS -> ClimateFanMode.FOCUS;
            case CLIMATE_FAN_DIFFUSE -> ClimateFanMode.DIFFUSE;
            case CLIMATE_FAN_QUIET -> ClimateFanMode.QUIET;
        };
    }

    static List<ClimateFanMode> mapClimateFanModes(
        final List<io.github.arhor.esphome.client.proto.ClimateFanMode> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapClimateFanMode).toList();
    }

    static ClimateSwingMode mapClimateSwingMode(
        final io.github.arhor.esphome.client.proto.ClimateSwingMode value
    ) {
        if (value == null) {
            return ClimateSwingMode.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> ClimateSwingMode.UNKNOWN;
            case CLIMATE_SWING_OFF -> ClimateSwingMode.OFF;
            case CLIMATE_SWING_BOTH -> ClimateSwingMode.BOTH;
            case CLIMATE_SWING_VERTICAL -> ClimateSwingMode.VERTICAL;
            case CLIMATE_SWING_HORIZONTAL -> ClimateSwingMode.HORIZONTAL;
        };
    }

    static List<ClimateSwingMode> mapClimateSwingModes(
        final List<io.github.arhor.esphome.client.proto.ClimateSwingMode> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapClimateSwingMode).toList();
    }

    static ClimatePreset mapClimatePreset(final io.github.arhor.esphome.client.proto.ClimatePreset value) {
        if (value == null) {
            return ClimatePreset.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> ClimatePreset.UNKNOWN;
            case CLIMATE_PRESET_NONE -> ClimatePreset.NONE;
            case CLIMATE_PRESET_HOME -> ClimatePreset.HOME;
            case CLIMATE_PRESET_AWAY -> ClimatePreset.AWAY;
            case CLIMATE_PRESET_BOOST -> ClimatePreset.BOOST;
            case CLIMATE_PRESET_COMFORT -> ClimatePreset.COMFORT;
            case CLIMATE_PRESET_ECO -> ClimatePreset.ECO;
            case CLIMATE_PRESET_SLEEP -> ClimatePreset.SLEEP;
            case CLIMATE_PRESET_ACTIVITY -> ClimatePreset.ACTIVITY;
        };
    }

    static List<ClimatePreset> mapClimatePresets(
        final List<io.github.arhor.esphome.client.proto.ClimatePreset> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapClimatePreset).toList();
    }

    static NumberMode mapNumberMode(final io.github.arhor.esphome.client.proto.NumberMode value) {
        if (value == null) {
            return NumberMode.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> NumberMode.UNKNOWN;
            case NUMBER_MODE_AUTO -> NumberMode.AUTO;
            case NUMBER_MODE_BOX -> NumberMode.BOX;
            case NUMBER_MODE_SLIDER -> NumberMode.SLIDER;
        };
    }

    static TextMode mapTextMode(final io.github.arhor.esphome.client.proto.TextMode value) {
        if (value == null) {
            return TextMode.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> TextMode.UNKNOWN;
            case TEXT_MODE_TEXT -> TextMode.TEXT;
            case TEXT_MODE_PASSWORD -> TextMode.PASSWORD;
        };
    }

    static ServiceArgType mapServiceArgType(
        final io.github.arhor.esphome.client.proto.ServiceArgType value
    ) {
        if (value == null) {
            return ServiceArgType.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> ServiceArgType.UNKNOWN;
            case SERVICE_ARG_TYPE_BOOL -> ServiceArgType.BOOL;
            case SERVICE_ARG_TYPE_INT -> ServiceArgType.INT;
            case SERVICE_ARG_TYPE_FLOAT -> ServiceArgType.FLOAT;
            case SERVICE_ARG_TYPE_STRING -> ServiceArgType.STRING;
            case SERVICE_ARG_TYPE_BOOL_ARRAY -> ServiceArgType.BOOL_ARRAY;
            case SERVICE_ARG_TYPE_INT_ARRAY -> ServiceArgType.INT_ARRAY;
            case SERVICE_ARG_TYPE_FLOAT_ARRAY -> ServiceArgType.FLOAT_ARRAY;
            case SERVICE_ARG_TYPE_STRING_ARRAY -> ServiceArgType.STRING_ARRAY;
        };
    }

    static ServiceArgument mapServiceArgument(final ListEntitiesServicesArgument value) {
        return new ServiceArgument(value.getName(), mapServiceArgType(value.getType()));
    }

    static List<ServiceArgument> mapServiceArguments(final List<ListEntitiesServicesArgument> values) {
        return values.stream().map(EspHomeProtoTypeMapper::mapServiceArgument).toList();
    }

    static MediaPlayerFormatPurpose mapMediaPlayerFormatPurpose(
        final io.github.arhor.esphome.client.proto.MediaPlayerFormatPurpose value
    ) {
        if (value == null) {
            return MediaPlayerFormatPurpose.UNKNOWN;
        }
        return switch (value) {
            case UNRECOGNIZED -> MediaPlayerFormatPurpose.UNKNOWN;
            case MEDIA_PLAYER_FORMAT_PURPOSE_DEFAULT -> MediaPlayerFormatPurpose.DEFAULT;
            case MEDIA_PLAYER_FORMAT_PURPOSE_ANNOUNCEMENT -> MediaPlayerFormatPurpose.ANNOUNCEMENT;
        };
    }

    static MediaPlayerFormat mapMediaPlayerFormat(
        final io.github.arhor.esphome.client.proto.MediaPlayerSupportedFormat value
    ) {
        return new MediaPlayerFormat(
            value.getFormat(),
            value.getSampleRate(),
            value.getNumChannels(),
            mapMediaPlayerFormatPurpose(value.getPurpose()),
            value.getSampleBytes()
        );
    }

    static List<MediaPlayerFormat> mapMediaPlayerFormats(
        final List<io.github.arhor.esphome.client.proto.MediaPlayerSupportedFormat> values
    ) {
        return values.stream().map(EspHomeProtoTypeMapper::mapMediaPlayerFormat).toList();
    }

    private EspHomeProtoTypeMapper() {}
}
