package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.github.arhor.esphome.client.proto.CameraImageResponse;
import io.github.arhor.esphome.client.proto.DeviceInfoResponse;

public class EspHomeEventMapper {

    public static EspHomeEvent map(Object message) {
        return switch (message) {
            case CameraImageResponse res -> new EspHomeEvent.CameraImage(
                res.getKey(),
                res.getData().toByteArray(),
                res.getDone()
            );
            case DeviceInfoResponse res -> new EspHomeEvent.DeviceInfo(
                res.getName(),
                res.getMacAddress(),
                res.getEsphomeVersion(),
                res.getModel(),
                res.getManufacturer(),
                res.getFriendlyName()
            );
            case null, default -> null;
        };
    }
}
