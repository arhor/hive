package io.github.arhor.esphome.client.internal.codec

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.EspHomeFrame
import io.github.arhor.esphome.client.internal.noise.NoiseCipherState
import io.github.arhor.esphome.client.internal.noise.NoiseConstants
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object EncryptedEspHomeFrameCodec {

    private const val ENCRYPTED_INDICATOR = 0x01
    private const val MAX_PAYLOAD_SIZE = 65_535
    private const val FIXED_DATA_HEADER_SIZE = 4
    private const val MAX_ENCRYPTED_PROTOBUF_PAYLOAD_SIZE =
        MAX_PAYLOAD_SIZE - FIXED_DATA_HEADER_SIZE - NoiseConstants.AUTH_TAG_LENGTH

    fun encode(payload: ByteArray): ByteArray {
        require(payload.size <= MAX_PAYLOAD_SIZE) { "encrypted payload is too large" }

        val output = ByteArrayOutputStream(3 + payload.size)
        output.write(ENCRYPTED_INDICATOR)
        output.write((payload.size ushr 8) and 0xff)
        output.write(payload.size and 0xff)
        output.write(payload)
        return output.toByteArray()
    }

    fun decode(input: InputStream): ByteArray {
        val indicator = input.read()
        if (indicator < 0) {
            throw EspHomeProtocolException("ESPHome encrypted frame ended before indicator was read")
        }
        if (indicator != ENCRYPTED_INDICATOR) {
            throw EspHomeProtocolException("Invalid ESPHome encrypted frame indicator: 0x%02x".format(indicator))
        }

        val high = input.read()
        val low = input.read()
        if (high < 0 || low < 0) {
            throw EspHomeProtocolException("ESPHome encrypted frame ended before payload size was complete")
        }

        val payloadSize = (high shl 8) or low
        val payload = input.readNBytes(payloadSize)
        if (payload.size != payloadSize) {
            throw EspHomeProtocolException("ESPHome encrypted frame ended before payload was complete")
        }
        return payload
    }

    fun encodeFrame(frame: EspHomeFrame, cipher: NoiseCipherState): ByteArray {
        if (frame.type !in 0..MAX_PAYLOAD_SIZE) {
            throw EspHomeProtocolException("ESPHome encrypted message type is out of range: ${frame.type}")
        }
        if (frame.data.size > MAX_ENCRYPTED_PROTOBUF_PAYLOAD_SIZE) {
            throw EspHomeProtocolException("ESPHome encrypted payload is too large: ${frame.data.size} bytes")
        }

        val plaintext = ByteArrayOutputStream(4 + frame.data.size)
        plaintext.write((frame.type ushr 8) and 0xff)
        plaintext.write(frame.type and 0xff)
        plaintext.write((frame.data.size ushr 8) and 0xff)
        plaintext.write(frame.data.size and 0xff)
        plaintext.write(frame.data)
        return encode(cipher.encryptWithAd(NoiseConstants.EMPTY, plaintext.toByteArray()))
    }

    fun decodeFrame(input: InputStream, cipher: NoiseCipherState): EspHomeFrame {
        val plaintext = cipher.decryptWithAd(NoiseConstants.EMPTY, decode(input))
        if (plaintext.size < 4) {
            throw EspHomeProtocolException("ESPHome encrypted data frame is too short")
        }

        val messageType = ((plaintext[0].toInt() and 0xff) shl 8) or (plaintext[1].toInt() and 0xff)
        val payloadSize = ((plaintext[2].toInt() and 0xff) shl 8) or (plaintext[3].toInt() and 0xff)
        if (payloadSize > plaintext.size - 4) {
            throw EspHomeProtocolException("ESPHome encrypted data length exceeds decrypted payload")
        }
        return EspHomeFrame(
            type = messageType,
            data = plaintext.copyOfRange(4, 4 + payloadSize),
        )
    }
}
