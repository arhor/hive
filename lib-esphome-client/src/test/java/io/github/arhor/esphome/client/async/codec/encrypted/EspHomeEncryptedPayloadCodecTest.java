package io.github.arhor.esphome.client.async.codec.encrypted;

import io.github.arhor.esphome.client.async.EspHomeChannelAttributes;
import io.github.arhor.esphome.client.async.EspHomeFrame;
import io.github.arhor.esphome.client.async.EspHomeProtocolException;
import io.github.arhor.esphome.client.async.noise.NoiseCipherState;
import io.github.arhor.esphome.client.async.noise.NoiseConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EspHomeEncryptedPayloadCodecTest {

    @Test
    void encryptsMessageTypeAndPayloadLengthInFixedHeader() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var sender = new NoiseCipherState();
        sender.initializeKey(key);
        final var receiver = new NoiseCipherState();
        receiver.initializeKey(key);

        final var encodeChannel = new EmbeddedChannel(
            new EspHomeEncryptedFrameEncoder(),
            new EspHomeEncryptedPayloadEncoder()
        );
        encodeChannel.attr(EspHomeChannelAttributes.SEND_CIPHER).set(sender);
        assertTrue(encodeChannel.writeOutbound(new EspHomeFrame(45, Unpooled.wrappedBuffer(new byte[]{1, 2, 3}))));

        final ByteBuf wire = encodeChannel.readOutbound();
        final var wireBytes = ByteBufUtil.getBytes(wire);
        wire.release();

        assertEquals(0x01, wireBytes[0]);
        final var payloadSize = ((wireBytes[1] & 0xff) << 8) | (wireBytes[2] & 0xff);
        final var encryptedPayload = new byte[payloadSize];
        System.arraycopy(wireBytes, 3, encryptedPayload, 0, payloadSize);

        final var decryptedPayload = receiver.decryptWithAd(NoiseConstants.EMPTY, encryptedPayload);
        assertArrayEquals(new byte[]{0x00, 0x2d, 0x00, 0x03, 1, 2, 3}, decryptedPayload);
    }

    @Test
    void decodesEncryptedFrameFromFixedHeader() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var sender = new NoiseCipherState();
        sender.initializeKey(key);
        final var receiver = new NoiseCipherState();
        receiver.initializeKey(key);

        final var encryptedPayload = sender.encryptWithAd(
            NoiseConstants.EMPTY,
            new byte[]{0x00, 0x2e, 0x00, 0x02, 9, 8}
        );

        final var channel = encryptedChannel(sender, receiver);
        final var wireFrame = new byte[3 + encryptedPayload.length];
        wireFrame[0] = 0x01;
        wireFrame[1] = (byte) ((encryptedPayload.length >>> 8) & 0xff);
        wireFrame[2] = (byte) (encryptedPayload.length & 0xff);
        System.arraycopy(encryptedPayload, 0, wireFrame, 3, encryptedPayload.length);
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(wireFrame)));

        final EspHomeFrame frame = channel.readInbound();
        assertEquals(46, frame.messageType());
        assertArrayEquals(new byte[]{9, 8}, ByteBufUtil.getBytes(frame.payload()));
        frame.payload().release();
    }

    @Test
    void rejectsPayloadsTooLargeForEncryptedDataFrame() {
        final var key = new byte[32];
        for (var index = 0; index < 32; index++) {
            key[index] = (byte) (index + 1);
        }
        final var sender = new NoiseCipherState();
        sender.initializeKey(key);
        final var receiver = new NoiseCipherState();
        receiver.initializeKey(key);

        final var channel = encryptedChannel(sender, receiver);
        final var exception = assertThrows(
            EncoderException.class,
            () -> channel.writeOutbound(new EspHomeFrame(45, Unpooled.wrappedBuffer(new byte[65_516])))
        );
        assertInstanceOf(EspHomeProtocolException.class, exception.getCause());
        assertEquals("ESPHome encrypted payload is too large: 65516 bytes", exception.getCause().getMessage());
    }

    private static EmbeddedChannel encryptedChannel(final NoiseCipherState send, final NoiseCipherState receive) {
        final var channel = new EmbeddedChannel(
            new EspHomeEncryptedFrameDecoder(),
            new EspHomeEncryptedPayloadDecoder(),
            new EspHomeEncryptedFrameEncoder(),
            new EspHomeEncryptedPayloadEncoder()
        );
        channel.attr(EspHomeChannelAttributes.SEND_CIPHER).set(send);
        channel.attr(EspHomeChannelAttributes.RECEIVE_CIPHER).set(receive);
        return channel;
    }
}
