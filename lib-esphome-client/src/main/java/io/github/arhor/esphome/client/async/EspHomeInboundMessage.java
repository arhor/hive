package io.github.arhor.esphome.client.async;

import com.google.protobuf.MessageLite;

record EspHomeInboundMessage(
    int messageType,
    MessageLite protobuf
) {}
