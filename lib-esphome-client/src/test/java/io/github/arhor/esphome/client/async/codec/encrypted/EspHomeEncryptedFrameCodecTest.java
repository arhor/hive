package io.github.arhor.esphome.client.async.codec.encrypted;

import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EspHomeEncryptedFrameCodecTest {

    @Test
    void encodesEncryptedPayloadWithBigEndianLength() {
        final var channel = new EmbeddedChannel(new EspHomeEncryptedFrameEncoder());
        assertTrue(channel.writeOutbound(Unpooled.wrappedBuffer(new byte[]{1, 2, 3})));

        final ByteBuf encoded = channel.readOutbound();
        assertEquals(0x01, encoded.readByte());
        assertEquals(0, encoded.readUnsignedByte());
        assertEquals(3, encoded.readUnsignedByte());
        assertArrayEquals(new byte[]{1, 2, 3}, ByteBufUtil.getBytes(encoded));
        encoded.release();
    }

    @Test
    void decodesEncryptedPayload() {
        final var channel = new EmbeddedChannel(new EspHomeEncryptedFrameDecoder());
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{0x01, 0x00, 0x02, 4, 5})));

        final ByteBuf payload = channel.readInbound();
        assertArrayEquals(new byte[]{4, 5}, ByteBufUtil.getBytes(payload));
        payload.release();
    }

    @Test
    void encodesAndDecodesRoundTrip() {
        final var channel = new EmbeddedChannel(
            new EspHomeEncryptedFrameEncoder(),
            new EspHomeEncryptedFrameDecoder()
        );
        assertTrue(channel.writeOutbound(Unpooled.wrappedBuffer(new byte[]{1, 2, 3})));

        final ByteBuf encoded = channel.readOutbound();
        assertTrue(channel.writeInbound(encoded.retainedDuplicate()));

        final ByteBuf decoded = channel.readInbound();
        assertArrayEquals(new byte[]{1, 2, 3}, ByteBufUtil.getBytes(decoded));
        decoded.release();
        encoded.release();
    }

    @Test
    void rejectsInvalidIndicator() {
        final var channel = new EmbeddedChannel(new EspHomeEncryptedFrameDecoder());
        final var exception = assertThrows(
            DecoderException.class,
            () -> channel.writeInbound(Unpooled.wrappedBuffer(new byte[]{0x00, 0x00, 0x02, 4, 5}))
        );
        assertInstanceOf(EspHomeProtocolException.class, exception.getCause());
        assertEquals("Invalid ESPHome encrypted frame indicator: 0x00", exception.getCause().getMessage());
    }

    @Test
    void rejectsTruncatedPayload() {
        final var channel = new EmbeddedChannel(new EspHomeEncryptedFrameDecoder());
        channel.writeOneInbound(Unpooled.wrappedBuffer(new byte[]{0x01, 0x00, 0x02}));
        assertNull(channel.readInbound());
    }
}
