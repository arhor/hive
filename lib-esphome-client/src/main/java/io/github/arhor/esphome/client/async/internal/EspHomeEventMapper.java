package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.github.arhor.esphome.client.proto.CameraImageResponse;

public class EspHomeEventMapper {

    public static EspHomeEvent map(Object message) {
        return switch (message) {
            case CameraImageResponse res -> new EspHomeEvent.CameraImage(
                res.getKey(),
                res.getData().toByteArray(),
                res.getDone()
            );
            case null, default -> null;
        };
    }
}
