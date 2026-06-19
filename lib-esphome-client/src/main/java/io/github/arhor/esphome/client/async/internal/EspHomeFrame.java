package io.github.arhor.esphome.client.async.internal;

import io.netty.buffer.ByteBuf;

public record EspHomeFrame(int messageType, ByteBuf payload) {}
