package io.github.arhor.esphome.client.internal

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.EspHomeStateHandler
import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.exception.EspHomeAuthenticationException
import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import io.github.arhor.esphome.client.internal.transport.EspHomeTransport
import io.github.arhor.esphome.client.model.EspHomeDeviceInfo
import io.github.arhor.esphome.client.model.EspHomeEntity
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.HelloResponse
import io.github.arhor.esphome.client.proto.cameraImageRequest
import io.github.arhor.esphome.client.proto.connectRequest
import io.github.arhor.esphome.client.proto.deviceInfoRequest
import io.github.arhor.esphome.client.proto.disconnectRequest
import io.github.arhor.esphome.client.proto.disconnectResponse
import io.github.arhor.esphome.client.proto.helloRequest
import io.github.arhor.esphome.client.proto.listEntitiesRequest
import io.github.arhor.esphome.client.proto.pingResponse
import io.github.arhor.esphome.client.proto.subscribeStatesRequest
import java.io.ByteArrayOutputStream

class ProtobufConnection(
    private val config: EspHomeClientConfig,
    private val transport: EspHomeTransport,
) : EspHomeConnection {

    fun initialize() {
        send(EspHomeMessageType.HELLO_REQUEST, helloRequest {
            clientInfo = config.clientName
            apiVersionMajor = config.apiVersionMajor
            apiVersionMinor = config.apiVersionMinor
        })

        val hello = expect(EspHomeMessageType.HELLO_RESPONSE, HelloResponse.parser())
        if (hello.apiVersionMajor != 1) {
            throw EspHomeProtocolException("Unsupported ESPHome API major version: ${hello.apiVersionMajor}")
        }

        send(EspHomeMessageType.CONNECT_REQUEST, connectRequest {
            password = config.password.orEmpty()
        })

        val connect = expect(EspHomeMessageType.CONNECT_RESPONSE, ConnectResponse.parser())
        if (connect.invalidPassword) {
            throw EspHomeAuthenticationException("ESPHome device rejected the configured password")
        }
    }

    override fun deviceInfo(): EspHomeDeviceInfo {
        send(EspHomeMessageType.DEVICE_INFO_REQUEST, deviceInfoRequest { })

        val response = expect(EspHomeMessageType.DEVICE_INFO_RESPONSE, DeviceInfoResponse.parser())

        return EspHomeDeviceInfo(
            name = response.name,
            macAddress = response.macAddress,
            esphomeVersion = response.esphomeVersion,
            model = response.model,
        )
    }

    override fun fetchCameraImage(single: Boolean): ByteArray {
        send(EspHomeMessageType.CAMERA_IMAGE_REQUEST, cameraImageRequest {
            this.single = single
            this.stream = !single
        })

        val output = ByteArrayOutputStream()
        while (true) {
            val response = expect(EspHomeMessageType.CAMERA_IMAGE_RESPONSE, CameraImageResponse.parser())

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
        send(EspHomeMessageType.LIST_ENTITIES_REQUEST, listEntitiesRequest { })

        val entities = mutableListOf<EspHomeEntity>()

        while (true) {
            (val type, val data) = transport.receive()

            when (type) {
                EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE -> {
                    break
                }

                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE, pingResponse { })
                }

                in ENTITY_DISCOVERY_MESSAGE_TYPES -> {
                    entities += EspHomeEntityMapper.map(type, data)
                }

                else -> {
                    throw EspHomeProtocolException(
                        "Expected ESPHome entity discovery message but received $type",
                    )
                }
            }
        }
        return entities
    }

    override fun subscribeStates(handler: EspHomeStateHandler) {
        send(EspHomeMessageType.SUBSCRIBE_STATES_REQUEST, subscribeStatesRequest { })

        while (true) {
            val frame = transport.receive()
            when (frame.type) {
                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE, pingResponse { })
                }

                EspHomeMessageType.DISCONNECT_REQUEST -> {
                    send(EspHomeMessageType.DISCONNECT_RESPONSE, disconnectResponse { })
                    return
                }

                in ENTITY_STATE_MESSAGE_TYPES -> handler.onState(
                    EspHomeStateMapper.map(
                        frame.type,
                        frame.data
                    )
                )

                else -> throw EspHomeProtocolException(
                    "Expected ESPHome state message but received ${frame.type}",
                )
            }
        }
    }

    override fun close() {
        try {
            send(EspHomeMessageType.DISCONNECT_REQUEST, disconnectRequest { })
        } finally {
            transport.close()
        }
    }

    // -------------------------------------------- internal implementation --------------------------------------------

    private fun send(messageType: Int, payload: MessageLite) {
        transport.send(EspHomeFrame(messageType, payload.toByteArray()))
    }

    private fun <T> expect(messageType: Int, parser: Parser<T>): T {
        while (true) {
            (val type, val data) = transport.receive()

            when (type) {
                messageType -> {
                    return parser.parseFrom(data)
                }

                EspHomeMessageType.PING_REQUEST -> {
                    send(EspHomeMessageType.PING_RESPONSE, pingResponse { })
                }

                else -> {
                    throw EspHomeProtocolException(
                        "Expected ESPHome message $messageType but received $type",
                    )
                }
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
