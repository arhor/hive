package io.github.arhor.esphome.client.async.codec.encrypted;

import io.github.arhor.esphome.client.async.EspHomeProtocolException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class EspHomeEncryptedFrameEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final int ENCRYPTED_INDICATOR = 0x01;
    private static final int MAX_PAYLOAD_SIZE = 65_535;

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteBuf payload, final ByteBuf out) {
        final var payloadSize = payload.readableBytes();
        if (payloadSize > MAX_PAYLOAD_SIZE) {
            throw new EspHomeProtocolException("encrypted payload is too large");
        }

        out.writeByte(ENCRYPTED_INDICATOR);
        out.writeShort(payloadSize);
        out.writeBytes(payload, payload.readerIndex(), payloadSize);
    }
}
