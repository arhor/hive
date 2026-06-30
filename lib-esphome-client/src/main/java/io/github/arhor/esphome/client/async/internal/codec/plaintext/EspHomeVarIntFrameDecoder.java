package io.github.arhor.esphome.client.async.internal.codec.plaintext;

import io.github.arhor.esphome.client.async.internal.EspHomeFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;
import java.util.logging.Logger;

public class EspHomeVarIntFrameDecoder extends ByteToMessageDecoder {

    private static final Logger log = Logger.getLogger(EspHomeVarIntFrameDecoder.class.getName());

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Минимальный фрейм: 1 байт (индикатор) + 1 байт (размер) + 1 байт (тип)
        if (in.readableBytes() < 3) {
            return;
        }

        log.finest(() -> "decode() readable=" + in.readableBytes());

        // Фиксируем позицию для возможного отката, если пакет пришел не полностью
        in.markReaderIndex();

        // 1. Проверяем plaintext-индикатор (всегда 0x00)
        byte indicator = in.readByte();
        if (indicator != 0x00) {
            log.warning(() -> "Invalid frame indicator byte: 0x" + Integer.toHexString(indicator & 0xFF));
            in.resetReaderIndex();
            throw new CorruptedFrameException("Invalid ESPHome frame indicator: " + indicator);
        }

        // 2. Безопасно читаем размер полезной нагрузки (VarInt)
        int payloadSize = readVarIntSafe(in);
        if (payloadSize == -1) {
            in.resetReaderIndex();
            return; // Ждем дозагрузки VarInt размера
        }

        // 3. Безопасно читаем тип сообщения (VarInt)
        int messageType = readVarIntSafe(in);
        if (messageType == -1) {
            in.resetReaderIndex();
            return; // Ждем дозагрузки VarInt типа
        }

        // 4. Проверяем, весь ли payload доехал из сети
        if (in.readableBytes() < payloadSize) {
            in.resetReaderIndex();
            return; // Ждем оставшиеся байты payload
        }

        // Вырезаем точный кусок данных под payload (Netty увеличивает refCount для нового буфера)
        ByteBuf payload = in.readBytes(payloadSize);

        log.fine(() -> "Frame decoded: type=" + messageType + " payloadSize=" + payloadSize);

        // Передаем собранный фрейм следующему декодеру
        out.add(new EspHomeFrame(messageType, payload));
    }

    /**
     * Считывает VarInt без риска выбросить BufferUnderflowException.
     * Возвращает -1, если байты в буфере закончились до завершения VarInt.
     */
    private int readVarIntSafe(ByteBuf buf) {
        int result = 0;
        int shift = 0;
        int startReaderIndex = buf.readerIndex();

        while (shift < 32) {
            if (!buf.isReadable()) {
                buf.readerIndex(startReaderIndex);
                return -1; 
            }
            byte b = buf.readByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new CorruptedFrameException("VarInt is too long (malformed ESPHome protocol)");
    }
}
