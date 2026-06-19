package io.github.arhor.esphome.client.async.internal.codec.plaintext;

import io.github.arhor.esphome.client.async.internal.EspHomeFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class EspHomeVarIntFrameEncoder extends MessageToByteEncoder<EspHomeFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, EspHomeFrame frame, ByteBuf out) throws Exception {
        try {
            // 1. Пишем индикатор Plaintext протокола
            out.writeByte(0x00);

            // 2. Пишем размер полезной нагрузки (VarInt)
            int payloadSize = frame.payload().readableBytes();
            writeVarInt(out, payloadSize);

            // 3. Пишем тип сообщения (VarInt)
            writeVarInt(out, frame.messageType());

            // 4. Пишем сам сериализованный Protobuf payload
            out.writeBytes(frame.payload());
        } finally {
            // Освобождаем промежуточный буфер, созданный на предыдущем шаге
            frame.payload().release();
        }
    }

    /**
     * Прямая запись VarInt в буфер Netty.
     */
    private void writeVarInt(ByteBuf out, int value) {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }
}
