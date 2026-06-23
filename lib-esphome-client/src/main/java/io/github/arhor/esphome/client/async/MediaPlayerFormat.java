package io.github.arhor.esphome.client.async;

public record MediaPlayerFormat(
    String format,
    int sampleRate,
    int numChannels,
    MediaPlayerFormatPurpose purpose,
    int sampleBytes
) {}
