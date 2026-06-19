package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.internal.noise.NoiseCipherState;
import io.netty.util.AttributeKey;

public final class EspHomeChannelAttributes {

    public static final AttributeKey<NoiseCipherState> SEND_CIPHER =
        AttributeKey.valueOf("esphome.sendCipher");
    public static final AttributeKey<NoiseCipherState> RECEIVE_CIPHER =
        AttributeKey.valueOf("esphome.receiveCipher");

    private EspHomeChannelAttributes() {}
}
