package io.github.arhor.esphome.client.async.internal.codec.encrypted;

import io.github.arhor.esphome.client.async.internal.EspHomeChannelAttributes;
import io.github.arhor.esphome.client.async.internal.EspHomeFrame;
import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.async.internal.noise.NoiseConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public final class EspHomeEncryptedPayloadEncoder extends MessageToMessageEncoder<EspHomeFrame> {

    private static final int MAX_PAYLOAD_SIZE = 65_535;
    private static final int FIXED_DATA_HEADER_SIZE = 4;
    private static final int MAX_ENCRYPTED_PROTOBUF_PAYLOAD_SIZE =
        MAX_PAYLOAD_SIZE - FIXED_DATA_HEADER_SIZE - NoiseConstants.AUTH_TAG_LENGTH;

    @Override
    protected void encode(
        final ChannelHandlerContext ctx,
        final EspHomeFrame frame,
        final List<Object> out
    ) {
        final var cipher = ctx.channel().attr(EspHomeChannelAttributes.SEND_CIPHER).get();
        if (cipher == null) {
            throw new EspHomeProtocolException("ESPHome send cipher is not initialized");
        }

        final var messageType = frame.messageType();
        if (messageType < 0 || messageType > MAX_PAYLOAD_SIZE) {
            throw new EspHomeProtocolException("ESPHome encrypted message type is out of range: " + messageType);
        }

        final ByteBuf payload = frame.payload();
        final var payloadSize = payload.readableBytes();
        if (payloadSize > MAX_ENCRYPTED_PROTOBUF_PAYLOAD_SIZE) {
            throw new EspHomeProtocolException("ESPHome encrypted payload is too large: " + payloadSize + " bytes");
        }

        try {
            final var plaintext = ctx.alloc().buffer(FIXED_DATA_HEADER_SIZE + payloadSize);
            plaintext.writeShort(messageType);
            plaintext.writeShort(payloadSize);
            plaintext.writeBytes(payload, payload.readerIndex(), payloadSize);

            final var encrypted = cipher.encryptWithAd(NoiseConstants.EMPTY, ByteBufUtil.getBytes(plaintext));
            plaintext.release();

            out.add(ctx.alloc().buffer(encrypted.length).writeBytes(encrypted));
        } finally {
            payload.release();
        }
    }
}
