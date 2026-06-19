package io.github.arhor.esphome.client.async.internal.codec;

import com.google.protobuf.MessageLite;
import io.github.arhor.esphome.client.async.internal.EspHomeFrame;
import io.github.arhor.esphome.client.async.internal.EspHomeProtobufRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class EspHomeProtobufEncoder extends MessageToMessageEncoder<MessageLite> {

    @Override
    protected void encode(
        final ChannelHandlerContext ctx,
        final MessageLite msg,
        final List<Object> out
    ) throws Exception {
        // Определяем ID типа по классу Protobuf-сообщения
        int messageType = EspHomeProtobufRegistry.getMessageType(msg.getClass());
        if (messageType == -1) {
            throw new IllegalArgumentException("Unsupported protobuf message type: " + msg.getClass());
        }

        // Сериализуем Protobuf в массив байт и оборачиваем в Netty буфер
        byte[] protobufBytes = msg.toByteArray();
        ByteBuf payload = Unpooled.wrappedBuffer(protobufBytes);

        // Передаем промежуточный фрейм на слой упаковки в VarInt
        out.add(new EspHomeFrame(messageType, payload));
    }
}
