package io.github.arhor.esphome.client.async.model;

public sealed interface EspHomeEvent {
    record CameraImage(int key, byte[] data, boolean done) implements EspHomeEvent {}

    record DeviceInfo(
        String name,
        String macAddress,
        String esphomeVersion,
        String model,
        String manufacturer,
        String friendlyName
    ) implements EspHomeEvent {}
}
