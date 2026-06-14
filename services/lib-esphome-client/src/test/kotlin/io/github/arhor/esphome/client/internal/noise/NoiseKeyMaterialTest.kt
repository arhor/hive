package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class NoiseKeyMaterialTest {

    @Test
    fun `decodes base64 32 byte key`() {
        val expected = ByteArray(32) { index -> index.toByte() }
        val encoded = Base64.getEncoder().encodeToString(expected)

        val decoded = NoiseKeyMaterial.decodeBase64(encoded)

        assertContentEquals(expected, decoded)
    }

    @Test
    fun `rejects missing and invalid key material`() {
        assertFailsWith<EspHomeProtocolException> {
            NoiseKeyMaterial.decodeBase64(null)
        }
        assertFailsWith<EspHomeProtocolException> {
            NoiseKeyMaterial.decodeBase64("not base64")
        }
        assertFailsWith<EspHomeProtocolException> {
            NoiseKeyMaterial.decodeBase64(Base64.getEncoder().encodeToString(ByteArray(31)))
        }
    }
}
