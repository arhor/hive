package io.github.arhor.esphome.client.async.codec.encrypted;

import io.github.arhor.esphome.client.async.EspHomeChannelAttributes;
import io.github.arhor.esphome.client.async.EspHomeFrame;
import io.github.arhor.esphome.client.async.EspHomeProtocolException;
import io.github.arhor.esphome.client.async.noise.NoiseConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public final class EspHomeEncryptedPayloadDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(
        final ChannelHandlerContext ctx,
        final ByteBuf ciphertext,
        final List<Object> out
    ) {
        final var cipher = ctx.channel().attr(EspHomeChannelAttributes.RECEIVE_CIPHER).get();
        if (cipher == null) {
            throw new EspHomeProtocolException("ESPHome receive cipher is not initialized");
        }

        final var encrypted = ByteBufUtil.getBytes(ciphertext);
        final var plaintext = cipher.decryptWithAd(NoiseConstants.EMPTY, encrypted);
        if (plaintext.length < 4) {
            throw new EspHomeProtocolException("ESPHome encrypted data frame is too short");
        }

        final var messageType = ((plaintext[0] & 0xff) << 8) | (plaintext[1] & 0xff);
        final var payloadSize = ((plaintext[2] & 0xff) << 8) | (plaintext[3] & 0xff);
        if (payloadSize > plaintext.length - 4) {
            throw new EspHomeProtocolException("ESPHome encrypted data length exceeds decrypted payload");
        }

        final ByteBuf payload = ctx.alloc().buffer(payloadSize);
        payload.writeBytes(plaintext, 4, payloadSize);
        out.add(new EspHomeFrame(messageType, payload));
    }
}
