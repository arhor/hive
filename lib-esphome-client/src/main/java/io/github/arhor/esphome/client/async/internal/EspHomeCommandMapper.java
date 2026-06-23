package io.github.arhor.esphome.client.async.internal;

import com.google.protobuf.MessageLite;
import io.github.arhor.esphome.client.async.model.EspHomeCommand;
import io.github.arhor.esphome.client.proto.CameraImageRequest;
import io.github.arhor.esphome.client.proto.DeviceInfoRequest;

public class EspHomeCommandMapper {

    public static MessageLite map(EspHomeCommand command) {
        return switch (command) {
            case EspHomeCommand.GetCameraImage(var single, var stream) -> CameraImageRequest.newBuilder()
                .setSingle(single)
                .setStream(stream)
                .build();

            case EspHomeCommand.GetDeviceInfo _ -> DeviceInfoRequest.getDefaultInstance();
        };
    }
}
