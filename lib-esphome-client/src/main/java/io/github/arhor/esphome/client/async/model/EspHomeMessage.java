package io.github.arhor.esphome.client.async.model;

public sealed interface EspHomeMessage {
    record GetCameraImage(
        boolean single,
        boolean stream
    ) implements EspHomeMessage {}

    record GetDeviceInfo() implements EspHomeMessage {}
}
