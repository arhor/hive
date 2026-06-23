package io.github.arhor.esphome.client.async.internal;

import com.google.protobuf.MessageLite;
import io.github.arhor.esphome.client.async.model.EspHomeMessage;
import io.github.arhor.esphome.client.proto.CameraImageRequest;
import io.github.arhor.esphome.client.proto.DeviceInfoRequest;

public class EspHomeCommandMapper {

    public static MessageLite map(EspHomeMessage command) {
        return switch (command) {
            case EspHomeMessage.GetCameraImage(var single, var stream) -> CameraImageRequest.newBuilder()
                .setSingle(single)
                .setStream(stream)
                .build();

            case EspHomeMessage.GetDeviceInfo _ -> DeviceInfoRequest.getDefaultInstance();
        };
    }
}
