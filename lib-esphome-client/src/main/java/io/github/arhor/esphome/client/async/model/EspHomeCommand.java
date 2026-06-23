package io.github.arhor.esphome.client.async.model;

public sealed interface EspHomeCommand {
    record GetCameraImage(boolean single, boolean stream) implements EspHomeCommand {}

    record GetDeviceInfo() implements EspHomeCommand {}
}
