package io.github.arhor.esphome.client.internal

import com.google.protobuf.ByteString
import io.github.arhor.esphome.client.EspHomeAuthenticationException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.HelloResponse
import io.github.arhor.esphome.client.proto.ListEntitiesDoneResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSensorResponse
import io.github.arhor.esphome.client.proto.ListEntitiesSwitchResponse
import io.github.arhor.esphome.client.proto.PingRequest
import io.github.arhor.esphome.client.proto.SensorStateResponse
import io.github.arhor.esphome.client.proto.SwitchStateResponse
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

    @Test
    fun `listEntities aggregates discovery responses until done`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE,
                ListEntitiesSensorResponse.newBuilder()
                    .setKey(1)
                    .setObjectId("temperature")
                    .setName("Temperature")
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE,
                ListEntitiesSwitchResponse.newBuilder()
                    .setKey(2)
                    .setObjectId("relay")
                    .setName("Relay")
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE,
                ListEntitiesDoneResponse.newBuilder().build().toByteArray(),
            ),
        )

        val entities = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()

        assertEquals(EspHomeMessageType.LIST_ENTITIES_REQUEST, transport.sent.single().messageType)
        assertEquals(listOf("temperature", "relay"), entities.map { it.objectId })
    }

    @Test
    fun `listEntities responds to ping while waiting for discovery done`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.PING_REQUEST,
                PingRequest.newBuilder().build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE,
                ListEntitiesDoneResponse.newBuilder().build().toByteArray(),
            ),
        )

        val entities = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()

        assertEquals(emptyList(), entities)
        assertEquals(EspHomeMessageType.LIST_ENTITIES_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.PING_RESPONSE, transport.sent[1].messageType)
    }

    @Test
    fun `listEntities rejects unexpected messages`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DEVICE_INFO_RESPONSE,
                DeviceInfoResponse.newBuilder().build().toByteArray(),
            ),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).listEntities()
        }

        assertEquals("Expected ESPHome entity discovery message but received 10", error.message)
    }

    @Test
    fun `subscribeStates dispatches states in receive order`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.SENSOR_STATE_RESPONSE,
                SensorStateResponse.newBuilder()
                    .setKey(1)
                    .setState(21.5f)
                    .build()
                    .toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.SWITCH_STATE_RESPONSE,
                SwitchStateResponse.newBuilder()
                    .setKey(2)
                    .setState(true)
                    .build()
                    .toByteArray(),
            ),
        )
        val received = mutableListOf<Int>()
        val stop = RuntimeException("stop after two states")

        val error = assertFailsWith<RuntimeException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates { state ->
                received += state.key
                if (received.size == 2) throw stop
            }
        }

        assertEquals(stop, error)
        assertEquals(listOf(1, 2), received)
        assertEquals(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, transport.sent.single().messageType)
    }

    @Test
    fun `subscribeStates responds to ping while waiting for states`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.PING_REQUEST,
                PingRequest.newBuilder().build().toByteArray(),
            ),
            EspHomeFrame(
                EspHomeMessageType.SWITCH_STATE_RESPONSE,
                SwitchStateResponse.newBuilder()
                    .setKey(2)
                    .setState(true)
                    .build()
                    .toByteArray(),
            ),
        )
        val stop = RuntimeException("stop after state")

        val error = assertFailsWith<RuntimeException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates {
                throw stop
            }
        }

        assertEquals(stop, error)
        assertEquals(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.PING_RESPONSE, transport.sent[1].messageType)
    }

    @Test
    fun `subscribeStates rejects unexpected messages`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DEVICE_INFO_RESPONSE,
                DeviceInfoResponse.newBuilder().build().toByteArray(),
            ),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates {}
        }

        assertEquals("Expected ESPHome state message but received 10", error.message)
    }

    @Test
    fun `subscribeStates acknowledges disconnect request before returning`() {
        val transport = FakeTransport(
            EspHomeFrame(
                EspHomeMessageType.DISCONNECT_REQUEST,
                ByteArray(0),
            ),
        )

        EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).subscribeStates {}

        assertEquals(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.DISCONNECT_RESPONSE, transport.sent[1].messageType)
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
