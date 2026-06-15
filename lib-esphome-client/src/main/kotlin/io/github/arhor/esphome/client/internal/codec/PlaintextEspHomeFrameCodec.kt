package io.github.arhor.esphome.client.internal.codec

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.EspHomeFrame
import java.io.ByteArrayOutputStream
import java.io.InputStream

object PlaintextEspHomeFrameCodec {

    private const val PLAINTEXT_INDICATOR = 0x00
    private const val MAX_PAYLOAD_SIZE = 2 * 1024 * 1024

    fun encode(frame: EspHomeFrame): ByteArray {
        require(frame.type in 0..0xffff) { "messageType must fit into uint16" }
        require(frame.data.size <= MAX_PAYLOAD_SIZE) { "payload is too large" }

        val output = ByteArrayOutputStream(1 + 5 + 3 + frame.data.size)
        output.write(PLAINTEXT_INDICATOR)
        writeVarInt(output, frame.data.size)
        writeVarInt(output, frame.type)
        output.write(frame.data)
        return output.toByteArray()
    }

    fun decode(input: InputStream): EspHomeFrame {
        val indicator = input.read()
        if (indicator < 0) {
            throw EspHomeProtocolException("ESPHome frame ended before indicator was read")
        }
        if (indicator != PLAINTEXT_INDICATOR) {
            throw EspHomeProtocolException("Invalid ESPHome plaintext frame indicator: 0x%02x".format(indicator))
        }

        val payloadSize = readVarInt(input, "payload size")
        if (payloadSize > MAX_PAYLOAD_SIZE) {
            throw EspHomeProtocolException("ESPHome frame payload is too large: $payloadSize bytes")
        }
        val messageType = readVarInt(input, "message type")
        val payload = input.readNBytes(payloadSize)
        if (payload.size != payloadSize) {
            throw EspHomeProtocolException("ESPHome frame ended before payload was complete")
        }
        return EspHomeFrame(type = messageType, data = payload)
    }

    private fun writeVarInt(output: ByteArrayOutputStream, value: Int) {
        var remaining = value
        while (remaining >= 0x80) {
            output.write((remaining and 0x7f) or 0x80)
            remaining = remaining ushr 7
        }
        output.write(remaining)
    }

    private fun readVarInt(input: InputStream, label: String): Int {
        var result = 0
        var shift = 0
        while (shift <= 28) {
            val next = input.read()
            if (next < 0) {
                throw EspHomeProtocolException("ESPHome frame ended before $label was complete")
            }
            result = result or ((next and 0x7f) shl shift)
            if ((next and 0x80) == 0) {
                return result
            }
            shift += 7
        }
        throw EspHomeProtocolException("ESPHome frame $label varint is too long")
    }
}
