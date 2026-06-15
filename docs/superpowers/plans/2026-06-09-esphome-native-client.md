# ESPHome Native Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a JVM-native plaintext ESPHome client library and wire `app-cat-recognizer` so it can fetch ESP32-CAM
frames through the native API.

**Architecture:** Keep ESPHome protocol logic in `lib-esphome-client`, split into codec, transport, protocol
client, public models, and exceptions. Keep `app-cat-recognizer` behind its existing `FrameClient` abstraction and
select HTTP snapshot or native API through config.

**Tech Stack:** Kotlin/JVM 25, Gradle Kotlin DSL, protobuf-kotlin, JUnit 5 through Kotlin test, Quarkus config/Arc in
the app module.

---

## File Structure

Create or modify these files:

- Modify: `gradle/libs.versions.toml` - add protobuf plugin/library aliases and JUnit platform test dependencies if
  needed.
- Modify: `lib-esphome-client/build.gradle.kts` - use catalog aliases, configure tests, and remove the generated
  `Main.kt` app entry point from the design surface.
- Delete: `lib-esphome-client/src/main/kotlin/Main.kt` - generated IDE sample code.
- Modify: `lib-esphome-client/src/main/proto/api.proto` - add JVM package generation options.
- Modify: `lib-esphome-client/src/main/proto/api_options.proto` - add matching JVM package generation options.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClientConfig.kt` - public config
  model.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt` - public client and
  connection interfaces plus default factory.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeDeviceInfo.kt` - device info model.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClientException.kt` - typed
  exception hierarchy.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageType.kt` - message
  IDs used in this slice.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrame.kt` - message type
  plus payload bytes.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrameCodec.kt` - plaintext
  frame encode/decode and varint logic.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeTransport.kt` - transport
  interface.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/PlaintextEspHomeTransport.kt` -
  socket transport implementation.
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClient.kt` -
  hello/connect, device info, camera image, disconnect.
- Create: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrameCodecTest.kt`
- Create: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClientTest.kt`
- Modify: `app-cat-recognizer/build.gradle.kts` - add dependency on `:lib-esphome-client`.
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt` - add camera
  source and native API config.
- Modify: `app-cat-recognizer/src/main/resources/application.properties` - add default native API values and keep HTTP
  snapshot as default source.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/EspHomeNativeFrameClient.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/FrameClientProducer.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/HttpSnapshotCameraClient.kt` -
  CDI qualifier for the HTTP snapshot implementation.
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/NativeApiCameraClient.kt` - CDI
  qualifier for the ESPHome native implementation.
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/SnapshotFrameClient.kt` - add
  HTTP qualifier while keeping it a CDI bean.
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/EspHomeNativeFrameClientTest.kt`
- Modify tests that directly inject `FrameClient` only if CDI selection makes them ambiguous.

## Task 1: Build Setup And Generated Sample Removal

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `lib-esphome-client/build.gradle.kts`
- Delete: `lib-esphome-client/src/main/kotlin/Main.kt`
- Modify: `lib-esphome-client/src/main/proto/api.proto`
- Modify: `lib-esphome-client/src/main/proto/api_options.proto`

- [ ] **Step 1: Add catalog entries**

Add protobuf aliases to `gradle/libs.versions.toml`:

```toml
[versions]
protobuf = "4.35.0"
protobuf-plugin = "0.10.0"

[libraries]
protobuf-kotlin = { module = "com.google.protobuf:protobuf-kotlin", version.ref = "protobuf" }
protobuf-protoc = { module = "com.google.protobuf:protoc", version.ref = "protobuf" }

[plugins]
protobuf = { id = "com.google.protobuf", version.ref = "protobuf-plugin" }
```

Keep existing entries and sort consistently with the current file.

- [ ] **Step 2: Update library build file**

Replace hard-coded protobuf coordinates in `lib-esphome-client/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

group = "io.github.arhor.esphome.client"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
        javaParameters = true
    }
    jvmToolchain(25)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("kotlin")
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.protobuf.kotlin)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Delete generated sample**

Delete `lib-esphome-client/src/main/kotlin/Main.kt`.

- [ ] **Step 4: Add generated protobuf package options**

In `lib-esphome-client/src/main/proto/api.proto`, immediately after `syntax = "proto3";`, add:

```proto
option java_package = "io.github.arhor.esphome.client.proto";
option java_multiple_files = true;
```

In `lib-esphome-client/src/main/proto/api_options.proto`, immediately after `syntax = "proto2";`, add:

```proto
option java_package = "io.github.arhor.esphome.client.proto";
option java_multiple_files = true;
```

This prevents generated Java classes from landing in the default package, which named Kotlin packages cannot use
reliably.

- [ ] **Step 5: Verify build setup**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml \
        lib-esphome-client/build.gradle.kts \
        lib-esphome-client/src/main/kotlin/Main.kt \
        lib-esphome-client/src/main/proto/api.proto \
        lib-esphome-client/src/main/proto/api_options.proto
git commit -m "chore: prepare esphome client module"
```

## Task 2: Plaintext Frame Codec

**Files:**

- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClientException.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrame.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrameCodec.kt`
- Create: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeFrameCodecTest.kt`

- [ ] **Step 1: Write failing codec tests**

Create `EspHomeFrameCodecTest.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeProtocolException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EspHomeFrameCodecTest {

    @Test
    fun `encodes empty payload frame`() {
        val frame = EspHomeFrame(messageType = 7, payload = ByteArray(0))

        val bytes = EspHomeFrameCodec.encode(frame)

        assertContentEquals(byteArrayOf(0x00, 0x00, 0x07), bytes)
    }

    @Test
    fun `decodes frame with multi-byte varint payload size`() {
        val payload = ByteArray(130) { index -> index.toByte() }
        val encoded = byteArrayOf(0x00, 0x82.toByte(), 0x01, 0x2d) + payload

        val frame = EspHomeFrameCodec.decode(encoded.inputStream())

        assertEquals(45, frame.messageType)
        assertContentEquals(payload, frame.payload)
    }

    @Test
    fun `rejects invalid plaintext indicator`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeFrameCodec.decode(byteArrayOf(0x01, 0x00, 0x07).inputStream())
        }

        assertEquals("Invalid ESPHome plaintext frame indicator: 0x01", error.message)
    }

    @Test
    fun `rejects truncated payload`() {
        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeFrameCodec.decode(byteArrayOf(0x00, 0x03, 0x07, 0x01).inputStream())
        }

        assertEquals("ESPHome frame ended before payload was complete", error.message)
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test --tests '*EspHomeFrameCodecTest'
```

Expected: compile failure because `EspHomeFrame`, `EspHomeFrameCodec`, and `EspHomeProtocolException` do not exist.

- [ ] **Step 3: Implement exception hierarchy**

Create `EspHomeClientException.kt`:

```kotlin
package io.github.arhor.esphome.client

open class EspHomeClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class EspHomeTransportException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)

class EspHomeProtocolException(
    message: String,
    cause: Throwable? = null,
) : EspHomeClientException(message, cause)

class EspHomeAuthenticationException(
    message: String,
) : EspHomeClientException(message)
```

- [ ] **Step 4: Implement frame model and codec**

Create `EspHomeFrame.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

data class EspHomeFrame(
    val messageType: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is EspHomeFrame &&
            messageType == other.messageType &&
            payload.contentEquals(other.payload)

    override fun hashCode(): Int =
        31 * messageType + payload.contentHashCode()
}
```

Create `EspHomeFrameCodec.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeProtocolException
import java.io.ByteArrayOutputStream
import java.io.InputStream

object EspHomeFrameCodec {

    private const val PLAINTEXT_INDICATOR = 0x00
    private const val MAX_PAYLOAD_SIZE = 2 * 1024 * 1024

    fun encode(frame: EspHomeFrame): ByteArray {
        require(frame.messageType in 0..0xffff) { "messageType must fit into uint16" }
        require(frame.payload.size <= MAX_PAYLOAD_SIZE) { "payload is too large" }

        val output = ByteArrayOutputStream(1 + 5 + 3 + frame.payload.size)
        output.write(PLAINTEXT_INDICATOR)
        writeVarInt(output, frame.payload.size)
        writeVarInt(output, frame.messageType)
        output.write(frame.payload)
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
        return EspHomeFrame(messageType = messageType, payload = payload)
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
```

- [ ] **Step 5: Run tests and verify GREEN**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test --tests '*EspHomeFrameCodecTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client \
        lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client
git commit -m "feat: add esphome plaintext frame codec"
```

## Task 3: Transport Boundary And Socket Transport

**Files:**

- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClientConfig.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeTransport.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/PlaintextEspHomeTransport.kt`

- [ ] **Step 1: Add config model**

Create `EspHomeClientConfig.kt`:

```kotlin
package io.github.arhor.esphome.client

import java.time.Duration

data class EspHomeClientConfig(
    val host: String,
    val port: Int = 6053,
    val clientName: String = "hive-lib-esphome-client",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
    val password: String? = null,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be between 1 and 65535" }
        require(!connectTimeout.isNegative && !connectTimeout.isZero) { "connectTimeout must be positive" }
        require(!readTimeout.isNegative && !readTimeout.isZero) { "readTimeout must be positive" }
    }
}
```

- [ ] **Step 2: Add transport interface**

Create `EspHomeTransport.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

interface EspHomeTransport : AutoCloseable {
    fun send(frame: EspHomeFrame)
    fun receive(): EspHomeFrame
}
```

- [ ] **Step 3: Add socket transport**

Create `PlaintextEspHomeTransport.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeTransportException
import java.net.InetSocketAddress
import java.net.Socket

class PlaintextEspHomeTransport private constructor(
    private val socket: Socket,
) : EspHomeTransport {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun send(frame: EspHomeFrame) {
        try {
            output.write(EspHomeFrameCodec.encode(frame))
            output.flush()
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to write ESPHome frame", exception)
        }
    }

    override fun receive(): EspHomeFrame =
        try {
            EspHomeFrameCodec.decode(input)
        } catch (exception: EspHomeTransportException) {
            throw exception
        } catch (exception: Exception) {
            throw EspHomeTransportException("Failed to read ESPHome frame", exception)
        }

    override fun close() {
        socket.close()
    }

    companion object {
        fun connect(config: EspHomeClientConfig): PlaintextEspHomeTransport {
            val socket = Socket()
            try {
                socket.soTimeout = config.readTimeout.toMillis().toInt()
                socket.tcpNoDelay = true
                socket.connect(
                    InetSocketAddress(config.host, config.port),
                    config.connectTimeout.toMillis().toInt(),
                )
                return PlaintextEspHomeTransport(socket)
            } catch (exception: Exception) {
                socket.close()
                throw EspHomeTransportException(
                    "Failed to connect to ESPHome device at ${config.host}:${config.port}",
                    exception,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run library tests**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client
git commit -m "feat: add esphome plaintext transport"
```

## Task 4: Protocol Client Over Fake Transport

**Files:**

- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeClient.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/EspHomeDeviceInfo.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeMessageType.kt`
- Create: `lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClient.kt`
- Create: `lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client/internal/EspHomeProtocolClientTest.kt`

- [ ] **Step 1: Write failing protocol tests**

Create `EspHomeProtocolClientTest.kt` with fake transport tests:

```kotlin
package io.github.arhor.esphome.client.internal

import io.github.arhor.esphome.client.EspHomeAuthenticationException
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeProtocolException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import io.github.arhor.esphome.client.proto.CameraImageResponse
import io.github.arhor.esphome.client.proto.ConnectResponse
import io.github.arhor.esphome.client.proto.DeviceInfoResponse
import io.github.arhor.esphome.client.proto.HelloResponse

class EspHomeProtocolClientTest {

    @Test
    fun `connect sends hello and connect requests`() {
        val transport = FakeTransport(
            EspHomeFrame(EspHomeMessageType.HELLO_RESPONSE, HelloResponse.newBuilder().setApiVersionMajor(1).build().toByteArray()),
            EspHomeFrame(EspHomeMessageType.CONNECT_RESPONSE, ConnectResponse.newBuilder().setInvalidPassword(false).build().toByteArray()),
        )

        EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).connect()

        assertEquals(EspHomeMessageType.HELLO_REQUEST, transport.sent[0].messageType)
        assertEquals(EspHomeMessageType.CONNECT_REQUEST, transport.sent[1].messageType)
    }

    @Test
    fun `connect rejects invalid password`() {
        val transport = FakeTransport(
            EspHomeFrame(EspHomeMessageType.HELLO_RESPONSE, HelloResponse.newBuilder().setApiVersionMajor(1).build().toByteArray()),
            EspHomeFrame(EspHomeMessageType.CONNECT_RESPONSE, ConnectResponse.newBuilder().setInvalidPassword(true).build().toByteArray()),
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
            EspHomeFrame(EspHomeMessageType.CAMERA_IMAGE_RESPONSE, CameraImageResponse.newBuilder().setData(com.google.protobuf.ByteString.copyFrom(byteArrayOf(1, 2))).build().toByteArray()),
            EspHomeFrame(EspHomeMessageType.CAMERA_IMAGE_RESPONSE, CameraImageResponse.newBuilder().setData(com.google.protobuf.ByteString.copyFrom(byteArrayOf(3))).setDone(true).build().toByteArray()),
        )

        val image = EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).fetchCameraImage()

        assertContentEquals(byteArrayOf(1, 2, 3), image)
        assertEquals(EspHomeMessageType.CAMERA_IMAGE_REQUEST, transport.sent.single().messageType)
    }

    @Test
    fun `fetchCameraImage rejects done without data`() {
        val transport = FakeTransport(
            EspHomeFrame(EspHomeMessageType.CAMERA_IMAGE_RESPONSE, CameraImageResponse.newBuilder().setDone(true).build().toByteArray()),
        )

        val error = assertFailsWith<EspHomeProtocolException> {
            EspHomeProtocolClient(EspHomeClientConfig(host = "camera"), transport).fetchCameraImage()
        }

        assertEquals("ESPHome camera response completed without image data", error.message)
    }

    private class FakeTransport(vararg frames: EspHomeFrame) : EspHomeTransport {
        val sent = mutableListOf<EspHomeFrame>()
        private val incoming = ArrayDeque(frames.toList())
        var closed = false

        override fun send(frame: EspHomeFrame) {
            sent += frame
        }

        override fun receive(): EspHomeFrame {
            return incoming.removeFirst()
        }

        override fun close() {
            closed = true
        }
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test --tests '*EspHomeProtocolClientTest'
```

Expected: compile failure because protocol client types do not exist.

- [ ] **Step 3: Add public device info and client API**

Create `EspHomeDeviceInfo.kt`:

```kotlin
package io.github.arhor.esphome.client

data class EspHomeDeviceInfo(
    val name: String,
    val macAddress: String,
    val esphomeVersion: String,
    val model: String,
)
```

Create `EspHomeClient.kt`:

```kotlin
package io.github.arhor.esphome.client

import io.github.arhor.esphome.client.internal.EspHomeProtocolClient
import io.github.arhor.esphome.client.internal.PlaintextEspHomeTransport

interface EspHomeClient : AutoCloseable {
    fun connect(): EspHomeConnection
}

interface EspHomeConnection : AutoCloseable {
    fun deviceInfo(): EspHomeDeviceInfo
    fun fetchCameraImage(single: Boolean = true): ByteArray
}

class DefaultEspHomeClient(
    private val config: EspHomeClientConfig,
) : EspHomeClient {

    private var connection: EspHomeProtocolClient? = null

    override fun connect(): EspHomeConnection {
        val protocol = EspHomeProtocolClient(config, PlaintextEspHomeTransport.connect(config))
        protocol.connect()
        connection = protocol
        return protocol
    }

    override fun close() {
        connection?.close()
        connection = null
    }
}
```

- [ ] **Step 4: Add message IDs and protocol client**

Create `EspHomeMessageType.kt`:

```kotlin
package io.github.arhor.esphome.client.internal

object EspHomeMessageType {
    const val HELLO_REQUEST = 1
    const val HELLO_RESPONSE = 2
    const val CONNECT_REQUEST = 3
    const val CONNECT_RESPONSE = 4
    const val DISCONNECT_REQUEST = 5
    const val DEVICE_INFO_REQUEST = 9
    const val DEVICE_INFO_RESPONSE = 10
    const val CAMERA_IMAGE_RESPONSE = 44
    const val CAMERA_IMAGE_REQUEST = 45
}
```

Create `EspHomeProtocolClient.kt`:

```kotlin
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
        val frame = transport.receive()
        if (frame.messageType != messageType) {
            throw EspHomeProtocolException(
                "Expected ESPHome message $messageType but received ${frame.messageType}",
            )
        }
        return parser(frame.payload)
    }
}
```

Remove unused imports if the compiler reports them.

- [ ] **Step 5: Run tests and verify GREEN**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test --tests '*EspHomeProtocolClientTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run all library tests**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add lib-esphome-client/src/main/kotlin/io/github/arhor/esphome/client \
        lib-esphome-client/src/test/kotlin/io/github/arhor/esphome/client
git commit -m "feat: add esphome protocol client"
```

## Task 5: Cat Recognizer Config Selection

**Files:**

- Modify: `app-cat-recognizer/build.gradle.kts`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt`
- Modify: `app-cat-recognizer/src/main/resources/application.properties`
- Modify: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt`

- [ ] **Step 1: Write failing config test**

Extend `RecognizerConfigBindingTest.kt` assertions:

```kotlin
assertEquals(RecognizerConfig.CameraSource.HTTP_SNAPSHOT, config.camera().source())
assertEquals("esp32-cam.local", config.camera().nativeApi().host())
assertEquals(6053, config.camera().nativeApi().port())
assertEquals(Duration.ofSeconds(2), config.camera().nativeApi().connectTimeout())
assertEquals(Duration.ofSeconds(5), config.camera().nativeApi().readTimeout())
```

- [ ] **Step 2: Run test and verify RED**

Run:

```bash
cd services
./gradlew :app-cat-recognizer:test --tests '*RecognizerConfigBindingTest'
```

Expected: compile failure because `source()` and `nativeApi()` do not exist.

- [ ] **Step 3: Add module dependency**

In `app-cat-recognizer/build.gradle.kts`, add:

```kotlin
implementation(project(":lib-esphome-client"))
```

- [ ] **Step 4: Add config mapping types**

Modify `RecognizerConfig.kt`:

```kotlin
interface Camera {
    fun source(): CameraSource
    fun snapshotUrl(): String
    fun connectTimeout(): Duration
    fun readTimeout(): Duration
    fun nativeApi(): NativeApi
}

interface NativeApi {
    fun host(): String
    fun port(): Int
    fun connectTimeout(): Duration
    fun readTimeout(): Duration
}

enum class CameraSource {
    HTTP_SNAPSHOT,
    NATIVE_API,
}
```

Place `NativeApi` and `CameraSource` inside `RecognizerConfig`.

- [ ] **Step 5: Add default properties**

Add to `application.properties`:

```properties
cat-recognizer.camera.source=HTTP_SNAPSHOT
cat-recognizer.camera.native-api.host=esp32-cam.local
cat-recognizer.camera.native-api.port=6053
cat-recognizer.camera.native-api.connect-timeout=2S
cat-recognizer.camera.native-api.read-timeout=5S
```

- [ ] **Step 6: Run config test and verify GREEN**

Run:

```bash
cd services
./gradlew :app-cat-recognizer:test --tests '*RecognizerConfigBindingTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app-cat-recognizer/build.gradle.kts \
        app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfig.kt \
        app-cat-recognizer/src/main/resources/application.properties \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/config/RecognizerConfigBindingTest.kt
git commit -m "feat: add esphome camera config"
```

## Task 6: Native Frame Client And CDI Producer

**Files:**

- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/EspHomeNativeFrameClient.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/FrameClientProducer.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/HttpSnapshotCameraClient.kt`
- Create: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/NativeApiCameraClient.kt`
- Modify: `app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl/SnapshotFrameClient.kt`
- Create: `app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client/EspHomeNativeFrameClientTest.kt`

- [ ] **Step 1: Write failing native frame client tests**

Create `EspHomeNativeFrameClientTest.kt`:

```kotlin
package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.impl.EspHomeNativeFrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.EspHomeClientException
import io.github.arhor.esphome.client.EspHomeConnection
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EspHomeNativeFrameClientTest {

    @Test
    fun `maps native camera bytes to frame payload`() {
        val frameClient = EspHomeNativeFrameClient(config(), factory = { FakeConnection(byteArrayOf(1, 2, 3)) })

        val payload = frameClient.fetchFrame()

        assertContentEquals(byteArrayOf(1, 2, 3), payload.bytes)
        assertEquals("image/jpeg", payload.contentType)
    }

    @Test
    fun `maps esphome failures to frame source errors`() {
        val frameClient = EspHomeNativeFrameClient(
            config(),
            factory = { throw EspHomeClientException("native failure") },
        )

        val error = assertFailsWith<FrameSourceError> {
            frameClient.fetchFrame()
        }

        assertEquals("FRAME_FETCH_FAILED", error.code)
        assertEquals(true, error.retriable)
        assertEquals("Failed to fetch ESPHome camera frame from esp32-cam.local:6053", error.message)
    }

    private class FakeConnection(private val image: ByteArray) : EspHomeConnection {
        override fun deviceInfo() = error("not used")
        override fun fetchCameraImage(single: Boolean): ByteArray = image
        override fun close() = Unit
    }

    private fun config(): RecognizerConfig =
        object : RecognizerConfig {
            override fun worker() = error("not used")
            override fun camera() = object : RecognizerConfig.Camera {
                override fun source() = RecognizerConfig.CameraSource.NATIVE_API
                override fun snapshotUrl() = "http://example.test/snapshot"
                override fun connectTimeout() = java.time.Duration.ofSeconds(2)
                override fun readTimeout() = java.time.Duration.ofSeconds(5)
                override fun nativeApi() = object : RecognizerConfig.NativeApi {
                    override fun host() = "esp32-cam.local"
                    override fun port() = 6053
                    override fun connectTimeout() = java.time.Duration.ofSeconds(2)
                    override fun readTimeout() = java.time.Duration.ofSeconds(5)
                }
            }
            override fun state() = error("not used")
            override fun debug() = error("not used")
        }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```bash
cd services
./gradlew :app-cat-recognizer:test --tests '*EspHomeNativeFrameClientTest'
```

Expected: compile failure because `EspHomeNativeFrameClient` does not exist.

- [ ] **Step 3: Add CDI qualifiers**

Create `HttpSnapshotCameraClient.kt`:

```kotlin
package io.github.arhor.catrecognizer.client.impl

import jakarta.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FIELD, FUNCTION, VALUE_PARAMETER)
annotation class HttpSnapshotCameraClient
```

Create `NativeApiCameraClient.kt`:

```kotlin
package io.github.arhor.catrecognizer.client.impl

import jakarta.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FIELD, FUNCTION, VALUE_PARAMETER)
annotation class NativeApiCameraClient
```

- [ ] **Step 4: Keep snapshot client as qualified CDI bean**

Add `@HttpSnapshotCameraClient` above `SnapshotFrameClient` and keep `@ApplicationScoped` in place:

```kotlin
@ApplicationScoped
@HttpSnapshotCameraClient
class SnapshotFrameClient @Inject constructor(
    private val config: RecognizerConfig,
) : FrameClient {
```

This keeps the existing SmallRye Fault Tolerance interceptors active while removing the bean from the unqualified
`FrameClient` injection set.

- [ ] **Step 5: Implement native frame client**

Create `EspHomeNativeFrameClient.kt`:

```kotlin
package io.github.arhor.catrecognizer.client.impl

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.DefaultEspHomeClient
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeConnection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
@NativeApiCameraClient
class EspHomeNativeFrameClient @Inject constructor(
    private val config: RecognizerConfig,
) : FrameClient {

    private var factory: (EspHomeClientConfig) -> EspHomeConnection = { DefaultEspHomeClient(it).connect() }

    internal constructor(
        config: RecognizerConfig,
        factory: (EspHomeClientConfig) -> EspHomeConnection,
    ) : this(config) {
        this.factory = factory
    }

    override fun fetchFrame(): FramePayload {
        val nativeApi = config.camera().nativeApi()
        val clientConfig = EspHomeClientConfig(
            host = nativeApi.host(),
            port = nativeApi.port(),
            connectTimeout = nativeApi.connectTimeout(),
            readTimeout = nativeApi.readTimeout(),
        )

        return try {
            factory(clientConfig).use { connection ->
                FramePayload(
                    bytes = connection.fetchCameraImage(single = true),
                    contentType = "image/jpeg",
                    observedAt = Instant.now(),
                )
            }
        } catch (exception: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch ESPHome camera frame from ${nativeApi.host()}:${nativeApi.port()}",
                retriable = true,
                cause = exception,
            )
        }
    }
}
```

- [ ] **Step 6: Add producer for selected default FrameClient**

Create `FrameClientProducer.kt`:

```kotlin
package io.github.arhor.catrecognizer.client.impl

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

@ApplicationScoped
class FrameClientProducer @Inject constructor(
    private val config: RecognizerConfig,
    @HttpSnapshotCameraClient private val snapshotFrameClient: FrameClient,
    @NativeApiCameraClient private val nativeFrameClient: FrameClient,
) {

    @Produces
    @ApplicationScoped
    fun frameClient(): FrameClient =
        when (config.camera().source()) {
            RecognizerConfig.CameraSource.HTTP_SNAPSHOT -> snapshotFrameClient
            RecognizerConfig.CameraSource.NATIVE_API -> nativeFrameClient
        }
}
```

- [ ] **Step 7: Run native frame client test and verify GREEN**

Run:

```bash
cd services
./gradlew :app-cat-recognizer:test --tests '*EspHomeNativeFrameClientTest'
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run app tests**

Run:

```bash
cd services
./gradlew :app-cat-recognizer:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app-cat-recognizer/src/main/kotlin/io/github/arhor/catrecognizer/client/impl \
        app-cat-recognizer/src/test/kotlin/io/github/arhor/catrecognizer/client
git commit -m "feat: add esphome native frame client"
```

## Task 7: Documentation And Full Verification

**Files:**

- Modify: `README.md`

- [ ] **Step 1: Update README runtime example**

In the cat recognizer verification section, add native API opt-in example:

```bash
cd services
CAT_RECOGNIZER_CAMERA_SOURCE="NATIVE_API" \
CAT_RECOGNIZER_CAMERA_NATIVE_API_HOST="esp32-cam.local" \
CAT_RECOGNIZER_CAMERA_NATIVE_API_PORT="6053" \
CAT_RECOGNIZER_WORKER_ENABLED="false" \
CAT_RECOGNIZER_DEBUG_MANUAL_TRIGGER_ENABLED="true" \
./gradlew :app-cat-recognizer:quarkusDev
```

Add one note:

```markdown
Native API mode currently supports plaintext ESPHome API only. If your ESPHome device has `api.encryption` enabled, keep
using `HTTP_SNAPSHOT` or flash/use a local config with plaintext API until encrypted transport support is added.
```

- [ ] **Step 2: Run full verification**

Run:

```bash
cd services
./gradlew :lib-esphome-client:test
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

Expected: each command exits with `BUILD SUCCESSFUL`.

- [ ] **Step 3: Inspect git status**

Run:

```bash
git status --short
```

Expected: only intentional README changes, or clean after commit.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: document esphome native camera mode"
```

## Self-Review Checklist

- [ ] Every requirement from `docs/superpowers/specs/2026-06-09-esphome-native-client-design.md` is covered by a task.
- [ ] Plan contains no `TBD`, `TODO`, "similar to", or placeholder error-handling instructions.
- [ ] Public type names are consistent: `EspHomeClientConfig`, `EspHomeConnection`, `EspHomeDeviceInfo`,
  `EspHomeFrameCodec`, `EspHomeProtocolClient`, `EspHomeNativeFrameClient`.
- [ ] Encryption remains out of scope, with transport boundary preserved for future work.
- [ ] HTTP snapshot remains the default fallback source.
