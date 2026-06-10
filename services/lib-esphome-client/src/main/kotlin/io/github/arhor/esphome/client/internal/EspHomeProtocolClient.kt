package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeAuthenticationException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.EspHomeDeviceInfo
import io.github.arhor.esphome.client.EspHomeEntity
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.EspHomeStateHandler
import io.github.arhor.esphome.client.proto.CameraImageRequest
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectRequest
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoRequest
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.DisconnectRequest
import io.github.arhor.esphome.client.proto.DisconnectResponse
import io.github.arhor.esphome.client.proto.HelloRequest
import io.github.arhor.esphome.client.proto.HelloResponse
import io.github.arhor.esphome.client.proto.ListEntitiesRequest
import io.github.arhor.esphome.client.proto.PingResponse
import io.github.arhor.esphome.client.proto.SubscribeStatesRequest
import java.io.ByteArrayOutputStream

class EspHomeProtocolClient(
    private val config: EspHomeClientConfig,
    private val transport: EspHomeTransport,
) : EspHomeConnection {

    fun connect() {
        send(EspHomeMessageType.HELLO_REQUEST) {
            HelloRequest.newBuilder()
                .setClientInfo(config.clientName)
                .setApiVersionMajor(1)
                .setApiVersionMinor(10)
                .build()
                .toByteArray()
        }
        val hello = expect(EspHomeMessageType.HELLO_RESPONSE) {
            HelloResponse.parseFrom(it)
        }
        if (hello.apiVersionMajor != 1) {
            throw EspHomeProtocolException("Unsupported ESPHome API major version: ${hello.apiVersionMajor}")
        }

        send(EspHomeMessageType.CONNECT_REQUEST) {
            ConnectRequest.newBuilder()
                .setPassword(config.password.orEmpty())
                .build()
                .toByteArray()
        }
        val connect = expect(EspHomeMessageType.CONNECT_RESPONSE) {
            ConnectResponse.parseFrom(it)
        }
        if (connect.invalidPassword) {
            throw EspHomeAuthenticationException("ESPHome device rejected the configured password")
        }
    }

    override fun deviceInfo(): EspHomeDeviceInfo {
        send(EspHomeMessageType.DEVICE_INFO_REQUEST) {
            DeviceInfoRequest.newBuilder().build().toByteArray()
        }
        val response = expect(EspHomeMessageType.DEVICE_INFO_RESPONSE) {
            DeviceInfoResponse.parseFrom(it)
        }
        return EspHomeDeviceInfo(
            name = response.name,
            macAddress = response.macAddress,
            esphomeVersion = response.esphomeVersion,
            model = response.model,
        )
    }

    override fun fetchCameraImage(single: Boolean): ByteArray {
        send(EspHomeMessageType.CAMERA_IMAGE_REQUEST) {
            CameraImageRequest.newBuilder()
                .setSingle(single)
                .setStream(!single)
                .build()
                .toByteArray()
        }

        val output = ByteArrayOutputStream()
        while (true) {
            val response = expect(EspHomeMessageType.CAMERA_IMAGE_RESPONSE) {
                CameraImageResponse.parseFrom(it)
            }
            output.write(response.data.toByteArray())
            if (response.done) {
                val image = output.toByteArray()
                if (image.isEmpty()) {
                    throw EspHomeProtocolException("ESPHome camera response completed without image data")
                }
                return image
            }
        }
    }

    override fun listEntities(): List<EspHomeEntity> {
        send(EspHomeMessageType.LIST_ENTITIES_REQUEST) {
            ListEntitiesRequest.newBuilder().build().toByteArray()
        }
        val entities = mutableListOf<EspHomeEntity>()

        while (true) {
            val frame = transport.receive()
            when (frame.messageType) {
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE -> {
                    return entities
                }

                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE) {
                        PingResponse.newBuilder().build().toByteArray()
                    }
                }

                in ENTITY_DISCOVERY_MESSAGE_TYPES -> {
                    entities += EspHomeEntityMapper.map(
                        frame.messageType,
                        frame.payload,
                    )
                }

                else -> {
                    throw EspHomeProtocolException(
                        "Expected ESPHome entity discovery message but received ${frame.messageType}",
                    )
                }
            }
        }
    }

    override fun subscribeStates(handler: EspHomeStateHandler) {
        send(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST) {
            SubscribeStatesRequest.newBuilder().build().toByteArray()
        }

        while (true) {
            val frame = transport.receive()
            when (frame.messageType) {
                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE) {
                        PingResponse.newBuilder().build().toByteArray()
                    }
                }

                EspHomeMessageType.DISCONNECT_REQUEST -> {
                    send(EspHomeMessageType.DISCONNECT_RESPONSE) {
                        DisconnectResponse.newBuilder().build().toByteArray()
                    }
                    return
                }

                in ENTITY_STATE_MESSAGE_TYPES -> handler.onState(
                    EspHomeStateMapper.map(
                        frame.messageType,
                        frame.payload
                    )
                )

                else -> throw EspHomeProtocolException(
                    "Expected ESPHome state message but received ${frame.messageType}",
                )
            }
        }
    }

    override fun close() {
        runCatching {
            send(EspHomeMessageType.DISCONNECT_REQUEST) {
                DisconnectRequest.newBuilder().build().toByteArray()
            }
        }
        transport.close()
    }

    private fun send(messageType: Int, payload: () -> ByteArray) {
        transport.send(EspHomeFrame(messageType, payload()))
    }

    private fun <T> expect(messageType: Int, parser: (ByteArray) -> T): T {
        while (true) {
            val frame = transport.receive()
            when (frame.messageType) {
                messageType -> return parser(frame.payload)
                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE) {
                        PingResponse.newBuilder().build().toByteArray()
                    }
                }

                else -> throw EspHomeProtocolException(
                    "Expected ESPHome message $messageType but received ${frame.messageType}",
                )
            }
        }
    }

    private companion object {
        val ENTITY_DISCOVERY_MESSAGE_TYPES = setOf(
            EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE,
            EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE,
        )

        val ENTITY_STATE_MESSAGE_TYPES = setOf(
            EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE,
            EspHomeMessageType.COVER_STATE_RESPONSE,
            EspHomeMessageType.FAN_STATE_RESPONSE,
            EspHomeMessageType.LIGHT_STATE_RESPONSE,
            EspHomeMessageType.SENSOR_STATE_RESPONSE,
            EspHomeMessageType.SWITCH_STATE_RESPONSE,
            EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE,
            EspHomeMessageType.CLIMATE_STATE_RESPONSE,
            EspHomeMessageType.NUMBER_STATE_RESPONSE,
            EspHomeMessageType.SELECT_STATE_RESPONSE,
            EspHomeMessageType.SIREN_STATE_RESPONSE,
            EspHomeMessageType.LOCK_STATE_RESPONSE,
            EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE,
            EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE,
            EspHomeMessageType.TEXT_STATE_RESPONSE,
            EspHomeMessageType.DATE_STATE_RESPONSE,
            EspHomeMessageType.TIME_STATE_RESPONSE,
            EspHomeMessageType.EVENT_RESPONSE,
            EspHomeMessageType.VALVE_STATE_RESPONSE,
            EspHomeMessageType.DATETIME_STATE_RESPONSE,
            EspHomeMessageType.UPDATE_STATE_RESPONSE,
        )
    }
}
