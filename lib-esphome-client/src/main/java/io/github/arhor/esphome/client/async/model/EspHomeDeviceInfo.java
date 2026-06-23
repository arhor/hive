package io.github.arhor.esphome.client.async.model;

public record EspHomeDeviceInfo(
    boolean usesPassword,
    String name,
    String macAddress,
    String esphomeVersion,
    String model
) {}
