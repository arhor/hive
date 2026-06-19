package io.github.arhor.esphome.client.async.codec.encrypted;

import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public final class EspHomeEncryptedFrameDecoder extends ByteToMessageDecoder {

    private static final int ENCRYPTED_INDICATOR = 0x01;
    private static final int MAX_PAYLOAD_SIZE = 65_535;

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        if (in.readableBytes() < 3) {
            return;
        }

        in.markReaderIndex();

        final var indicator = in.readUnsignedByte();
        if (indicator != ENCRYPTED_INDICATOR) {
            in.resetReaderIndex();
            throw new EspHomeProtocolException(
                String.format("Invalid ESPHome encrypted frame indicator: 0x%02x", indicator)
            );
        }

        if (in.readableBytes() < 2) {
            in.resetReaderIndex();
            return;
        }

        final var payloadSize = in.readUnsignedShort();
        if (payloadSize > MAX_PAYLOAD_SIZE) {
            throw new EspHomeProtocolException("ESPHome encrypted frame payload is too large: " + payloadSize);
        }

        if (in.readableBytes() < payloadSize) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(payloadSize));
    }
}
