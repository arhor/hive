package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeAuthenticationException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.EspHomeDeviceInfo
import io.github.arhor.esphome.client.EspHomeProtocolException
import io.github.arhor.esphome.client.proto.CameraImageRequest
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectRequest
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoRequest
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.DisconnectRequest
import io.github.arhor.esphome.client.proto.HelloRequest
import io.github.arhor.esphome.client.proto.HelloResponse
import io.github.arhor.esphome.client.proto.PingResponse
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
}
