package io.github.arhor.esphome.client.async.internal;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.github.arhor.esphome.client.proto.CameraImageRequest;
import io.github.arhor.esphome.client.proto.CameraImageResponse;
import io.github.arhor.esphome.client.proto.ConnectRequest;
import io.github.arhor.esphome.client.proto.ConnectResponse;
import io.github.arhor.esphome.client.proto.DeviceInfoRequest;
import io.github.arhor.esphome.client.proto.DeviceInfoResponse;
import io.github.arhor.esphome.client.proto.HelloRequest;
import io.github.arhor.esphome.client.proto.HelloResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesAlarmControlPanelResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesBinarySensorResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesButtonResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesCameraResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesClimateResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesCoverResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesDateResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesDateTimeResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesDoneResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesEventResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesFanResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesLightResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesLockResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesMediaPlayerResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesNumberResponse;
import io.github.arhor.esphome.client.proto.ListEntitiesRequest;
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
import io.github.arhor.esphome.client.proto.PingRequest;
import io.github.arhor.esphome.client.proto.PingResponse;

import java.util.Map;

public class EspHomeProtobufRegistry {

    private static final class LazyParserRegistry {
        private static final Map<Integer, Parser<? extends MessageLite>> TYPE_TO_PARSER = Map.ofEntries(
            Map.entry(EspHomeMessageType.HELLO_REQUEST, HelloRequest.parser()),
            Map.entry(EspHomeMessageType.HELLO_RESPONSE, HelloResponse.parser()),
            Map.entry(EspHomeMessageType.CONNECT_REQUEST, ConnectRequest.parser()),
            Map.entry(EspHomeMessageType.CONNECT_RESPONSE, ConnectResponse.parser()),
            Map.entry(EspHomeMessageType.PING_REQUEST, PingRequest.parser()),
            Map.entry(EspHomeMessageType.PING_RESPONSE, PingResponse.parser()),
            Map.entry(EspHomeMessageType.DEVICE_INFO_REQUEST, DeviceInfoRequest.parser()),
            Map.entry(EspHomeMessageType.DEVICE_INFO_RESPONSE, DeviceInfoResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_REQUEST, ListEntitiesRequest.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE, ListEntitiesBinarySensorResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE, ListEntitiesCoverResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE, ListEntitiesFanResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE, ListEntitiesLightResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE, ListEntitiesSensorResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE, ListEntitiesSwitchResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE, ListEntitiesTextSensorResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE, ListEntitiesDoneResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE, ListEntitiesServicesResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE, ListEntitiesCameraResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE, ListEntitiesClimateResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE, ListEntitiesNumberResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE, ListEntitiesSelectResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE, ListEntitiesSirenResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE, ListEntitiesLockResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE, ListEntitiesButtonResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE, ListEntitiesMediaPlayerResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE, ListEntitiesAlarmControlPanelResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE, ListEntitiesTextResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE, ListEntitiesDateResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE, ListEntitiesTimeResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE, ListEntitiesEventResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE, ListEntitiesValveResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE, ListEntitiesDateTimeResponse.parser()),
            Map.entry(EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE, ListEntitiesUpdateResponse.parser()),
            Map.entry(EspHomeMessageType.CAMERA_IMAGE_RESPONSE, CameraImageResponse.parser()),
            Map.entry(EspHomeMessageType.CAMERA_IMAGE_REQUEST, CameraImageRequest.parser())
        );
    }

    private static final class LazyTypeRegistry {
        private static final Map<Class<? extends MessageLite>, Integer> CLASS_TO_TYPE = Map.ofEntries(
            Map.entry(HelloRequest.class, EspHomeMessageType.HELLO_REQUEST),
            Map.entry(HelloResponse.class, EspHomeMessageType.HELLO_RESPONSE),
            Map.entry(ConnectRequest.class, EspHomeMessageType.CONNECT_REQUEST),
            Map.entry(ConnectResponse.class, EspHomeMessageType.CONNECT_RESPONSE),
            Map.entry(PingRequest.class, EspHomeMessageType.PING_REQUEST),
            Map.entry(PingResponse.class, EspHomeMessageType.PING_RESPONSE),
            Map.entry(DeviceInfoRequest.class, EspHomeMessageType.DEVICE_INFO_REQUEST),
            Map.entry(DeviceInfoResponse.class, EspHomeMessageType.DEVICE_INFO_RESPONSE),
            Map.entry(ListEntitiesRequest.class, EspHomeMessageType.LIST_ENTITIES_REQUEST),
            Map.entry(ListEntitiesBinarySensorResponse.class, EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE),
            Map.entry(ListEntitiesCoverResponse.class, EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE),
            Map.entry(ListEntitiesFanResponse.class, EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE),
            Map.entry(ListEntitiesLightResponse.class, EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE),
            Map.entry(ListEntitiesSensorResponse.class, EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE),
            Map.entry(ListEntitiesSwitchResponse.class, EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE),
            Map.entry(ListEntitiesTextSensorResponse.class, EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE),
            Map.entry(ListEntitiesDoneResponse.class, EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE),
            Map.entry(ListEntitiesServicesResponse.class, EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE),
            Map.entry(ListEntitiesCameraResponse.class, EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE),
            Map.entry(ListEntitiesClimateResponse.class, EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE),
            Map.entry(ListEntitiesNumberResponse.class, EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE),
            Map.entry(ListEntitiesSelectResponse.class, EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE),
            Map.entry(ListEntitiesSirenResponse.class, EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE),
            Map.entry(ListEntitiesLockResponse.class, EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE),
            Map.entry(ListEntitiesButtonResponse.class, EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE),
            Map.entry(ListEntitiesMediaPlayerResponse.class, EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE),
            Map.entry(ListEntitiesAlarmControlPanelResponse.class, EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE),
            Map.entry(ListEntitiesTextResponse.class, EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE),
            Map.entry(ListEntitiesDateResponse.class, EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE),
            Map.entry(ListEntitiesTimeResponse.class, EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE),
            Map.entry(ListEntitiesEventResponse.class, EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE),
            Map.entry(ListEntitiesValveResponse.class, EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE),
            Map.entry(ListEntitiesDateTimeResponse.class, EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE),
            Map.entry(ListEntitiesUpdateResponse.class, EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE),
            Map.entry(CameraImageRequest.class, EspHomeMessageType.CAMERA_IMAGE_REQUEST),
            Map.entry(CameraImageResponse.class, EspHomeMessageType.CAMERA_IMAGE_RESPONSE)
        );
    }

    public static Parser<? extends MessageLite> getParser(final int type) {
        return LazyParserRegistry.TYPE_TO_PARSER.get(type);
    }

    public static int getMessageType(final Class<? extends MessageLite> clazz) {
        return LazyTypeRegistry.CLASS_TO_TYPE.getOrDefault(clazz, -1);
    }
}
