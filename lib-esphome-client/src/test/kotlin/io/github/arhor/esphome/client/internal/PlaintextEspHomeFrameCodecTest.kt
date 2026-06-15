package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.codec.PlaintextEspHomeFrameCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaintextEspHomeFrameCodecTest {

    @Test
    fun `encodes empty payload frame`() {
        val frame = EspHomeFrame(type = 7, data = ByteArray(0))

        val bytes = PlaintextEspHomeFrameCodec.encode(frame)

        assertContentEquals(byteArrayOf(0x00, 0x00, 0x07), bytes)
    }

    @Test
    fun `decodes frame with multi-byte varint payload size`() {
        val payload = ByteArray(130) { index -> index.toByte() }
        val encoded = byteArrayOf(0x00, 0x82.toByte(), 0x01, 0x2d) + payload

        val frame = PlaintextEspHomeFrameCodec.decode(encoded.inputStream())

        assertEquals(45, frame.type)
        assertContentEquals(payload, frame.data)
    }

    @Test
    fun `rejects invalid plaintext indicator`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            PlaintextEspHomeFrameCodec.decode(byteArrayOf(0x01, 0x00, 0x07).inputStream())
        }

        assertEquals("Invalid ESPHome plaintext frame indicator: 0x01", error.message)
    }

    @Test
    fun `rejects truncated payload`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            PlaintextEspHomeFrameCodec.decode(byteArrayOf(0x00, 0x03, 0x07, 0x01).inputStream())
        }

        assertEquals("ESPHome frame ended before payload was complete", error.message)
    }
}
