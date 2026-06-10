package io.github.arhor.esphome.client.internal

import com.google.protobuf.ByteString
import io.github.arhor.esphome.client.EspHomeAuthenticationException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.HelloResponse
import io.github.arhor.esphome.client.proto.PingRequest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EspHomeProtocolClientTest {

    @Test
    fun `connect sends hello and connect requests`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.HELLO_RESPONSE,
                HelloResponse.newBuilder().setApiVersionMajor(1).build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.CONNECT_RESPONSE,
                ConnectResponse.newBuilder().setInvalidPassword(false).build().toByteArray(),
            ),
        )

        EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).connect()

        assertEquals(EspHomeMessageType.HELLO_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.CONNECT_REQUEST, transport.sent[1].messageType)
    }

    @Test
    fun `connect rejects invalid password`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.HELLO_RESPONSE,
                HelloResponse.newBuilder().setApiVersionMajor(1).build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.CONNECT_RESPONSE,
                ConnectResponse.newBuilder().setInvalidPassword(true).build().toByteArray(),
            ),
        )

        assertFailsWith<EspHomeAuthenticationException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).connect()
        }
    }

    @Test
    fun `deviceInfo maps response`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DEVICE_INFO_RESPONSE,
                DeviceInfoResponse.newBuilder()
                    .setName("esp32-cam")
                    .setMacAddress("AA:BB")
                    .setEsphomeVersion("2026.5.3")
                    .setModel("esp32cam")
                    .build()
                    .toByteArray(),
            ),
        )

        val info = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).deviceInfo()

        assertEquals("esp32-cam", info.name)
        assertEquals("AA:BB", info.macAddress)
        assertEquals("2026.5.3", info.esphomeVersion)
        assertEquals("esp32cam", info.model)
    }

    @Test
    fun `fetchCameraImage aggregates chunks until done`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.CAMERA_IMAGE_RESPONSE,
                CameraImageResponse.newBuilder()
                    .setData(ByteString.copyFrom(byteArrayOf(1, 2)))
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.CAMERA_IMAGE_RESPONSE,
                CameraImageResponse.newBuilder()
                    .setData(ByteString.copyFrom(byteArrayOf(3)))
                    .setDone(true)
                    .build()
                    .toByteArray(),
            ),
        )

        val image = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).fetchCameraImage()

        assertContentEquals(byteArrayOf(1, 2, 3), image)
        assertEquals(EspHomeMessageType.CAMERA_IMAGE_REQUEST, transport.sent.single().messageType)
    }

    @Test
    fun `fetchCameraImage responds to ping while waiting for chunks`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.PING_REQUEST,
                PingRequest.newBuilder().build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.CAMERA_IMAGE_RESPONSE,
                CameraImageResponse.newBuilder()
                    .setData(ByteString.copyFrom(byteArrayOf(4)))
                    .setDone(true)
                    .build()
                    .toByteArray(),
            ),
        )

        val image = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).fetchCameraImage()

        assertContentEquals(byteArrayOf(4), image)
        assertEquals(EspHomeMessageType.CAMERA_IMAGE_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.PING_RESPONSE, transport.sent[1].messageType)
    }

    @Test
    fun `fetchCameraImage rejects done without data`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.CAMERA_IMAGE_RESPONSE,
                CameraImageResponse.newBuilder().setDone(true).build().toByteArray(),
            ),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).fetchCameraImage()
        }

        assertEquals("ESPHome camera response completed without image data", error.message)
    }

    private class FakeTransport(vararg frames: EspHomeFrame) : EspHomeTransport {
        val sent = mutableListOf<EspHomeFrame>()
        private val incoming = ArrayDeque(frames.toList())

        override fun send(frame: EspHomeFrame) {
            sent += frame
        }

        override fun receive(): EspHomeFrame =
            incoming.removeFirst()

        override fun close() = Unit
    }
}
