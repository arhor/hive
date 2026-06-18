package io.github.arhor.esphome.client.async;

public sealed interface EspHomeEvent {
    record CameraImage(int key, byte[] data, boolean done) implements EspHomeEvent {}
}
