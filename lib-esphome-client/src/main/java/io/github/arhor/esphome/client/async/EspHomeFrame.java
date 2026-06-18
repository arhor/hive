package io.github.arhor.esphome.client.async;

import io.netty.buffer.ByteBuf;

public record EspHomeFrame(int messageType, ByteBuf payload) {}
