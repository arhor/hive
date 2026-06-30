package io.github.arhor.esphome.client.async.internal.codec;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.github.arhor.esphome.client.async.internal.EspHomeFrame;
import io.github.arhor.esphome.client.async.internal.EspHomeProtobufRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.logging.Logger;

public class EspHomeProtobufDecoder extends MessageToMessageDecoder<EspHomeFrame> {

    private static final Logger log = Logger.getLogger(EspHomeProtobufDecoder.class.getName());

    @Override
    protected void decode(
        final ChannelHandlerContext ctx,
        final EspHomeFrame frame,
        final List<Object> out
    ) throws Exception {

        final ByteBuf payload = frame.payload();
        try {
            final var type = frame.messageType();
            final var parser = EspHomeProtobufRegistry.getParser(type);

            if (parser != null) {
                final var message = parseMessage(payload, parser);
                log.fine(() -> "Decoded: " + message.getClass().getSimpleName());
                out.add(message);
            } else {
                log.warning(() -> "No parser registered for message type " + type + ", dropping frame");
                ctx.fireUserEventTriggered("Unknown message type: " + type);
            }
        } finally {
            payload.release();
        }
    }

    private static MessageLite parseMessage(
        final ByteBuf payload,
        final Parser<? extends MessageLite> parser
    ) throws InvalidProtocolBufferException {

        final var length = payload.readableBytes();
        final var rIndex = payload.readerIndex();

        MessageLite message;

        if (payload.hasArray()) {
            final var array = payload.array();
            final var offset = payload.arrayOffset() + rIndex;

            message = parser.parseFrom(array, offset, length);
        } else if (payload.nioBufferCount() == 1) {
            final var buffer = payload.nioBuffer(rIndex, length);

            message = parser.parseFrom(buffer);
        } else {
            final var slice = payload.slice(rIndex, length);
            final var input = new ByteBufInputStream(slice, false);

            message = parser.parseFrom(input);
        }
        return message;
    }
}
