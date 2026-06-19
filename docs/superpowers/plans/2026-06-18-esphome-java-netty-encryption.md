# ESPHome Java/Netty Encryption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. **Update checkboxes in this file as work completes — this file is the source of truth, not conversation memory.**

**Goal:** Add ESPHome Noise encrypted transport to the Java/Netty async client (`lib-esphome-client/src/main/java/.../async/`) while keeping the Kotlin blocking client unchanged and plaintext as the default.

**Architecture:** Encryption is a Netty pipeline concern. A transient `EspHomeNoiseHandshakeHandler` runs before the ESPHome Hello/Connect handshake, then installs per-channel cipher state and encrypted frame/payload handlers. The protobuf layer (`EspHomeProtobufEncoder`/`Decoder`, `EspHomeHandshakeHandler`, `NettyEspHomeEventHandler`) stays transport-agnostic and unchanged. Separate wire handlers for encrypted vs plaintext framing (no combined codec).

**Tech Stack:** Java 25, Netty 4.1, JDK crypto (`X25519`, `ChaCha20-Poly1305`, `HmacSHA256`, `SHA-256`), JUnit 5, Gradle, protobuf-java (generated from existing `.proto` files).

**Reference spec:** `docs/superpowers/specs/2026-06-10-esphome-encryption-design.md`

**Reference Kotlin implementation (do not delete during this work):**
- `lib-esphome-client/src/main/kotlin/.../internal/noise/*`
- `lib-esphome-client/src/main/kotlin/.../internal/codec/EncryptedEspHomeFrameCodec.kt`
- `lib-esphome-client/src/main/kotlin/.../internal/transport/EncryptedTransport.kt`

**Decisions locked for this plan:**
1. Keep all existing Kotlin code — Java port is additive.
2. Add connect and read timeouts to the Java async client config and Netty pipeline.
3. Do not merge frame + payload into a combined codec — keep separate handlers.

---

## Progress Overview

Mark stages complete here as you finish them:

- [x] **Stage 0:** Build setup (Java tests, Netty handler dep)
- [x] **Stage 1:** Java Noise crypto port + unit tests
- [x] **Stage 2:** Encrypted wire frame handlers + unit tests
- [x] **Stage 3:** Encrypted payload handlers + unit tests
- [x] **Stage 4:** Noise handshake handler + pipeline reconfiguration
- [x] **Stage 5:** Config (encryption + timeouts) + pipeline branching
- [x] **Stage 6:** End-to-end Netty loopback test
- [x] **Stage 7:** Final verification + plan sign-off

---

## File Structure

### Create

| File | Responsibility |
|------|----------------|
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseConstants.java` | Protocol constants |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseKeyMaterial.java` | Base64 PSK decode/validate |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseCipherState.java` | ChaCha20-Poly1305 per-channel cipher |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseHandshakeState.java` | NNpsk0 initiator/responder handshake |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeEncryptionConfig.java` | Encryption enable + key config record |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeChannelAttributes.java` | Netty `AttributeKey`s for ciphers |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameDecoder.java` | `[0x01][BE size][payload]` decoder |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameEncoder.java` | Wire encoder for encrypted payloads |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadDecoder.java` | Decrypt → `EspHomeFrame` |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadEncoder.java` | `EspHomeFrame` → encrypt |
| `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeNoiseHandshakeHandler.java` | Pre-ESPHome Noise state machine |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseKeyMaterialTest.java` | Key material tests |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseCipherStateTest.java` | Cipher tests |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseHandshakeStateTest.java` | Handshake tests |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameCodecTest.java` | Wire codec tests (via handlers or package-private helpers) |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadCodecTest.java` | Payload encrypt/decrypt tests |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/EspHomeClientConfigTest.java` | Config validation tests |
| `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/NettyEncryptedClientTest.java` | Full loopback integration test |

### Modify

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `junit-jupiter`, `netty-handler` aliases |
| `lib-esphome-client/build.gradle.kts` | Java test deps, `netty-handler`, enable JUnit Platform for Java tests |
| `lib-esphome-client/src/main/java/.../async/EspHomeClientConfig.java` | Add `EspHomeEncryptionConfig encryption`, `Duration connectTimeout`, `Duration readTimeout` |
| `lib-esphome-client/src/main/java/.../async/NettyEspHomeClient.java` | Branch pipeline; apply timeouts |

### Do not modify (this slice)

- Kotlin client, transport, noise packages
- `EspHomeHandshakeHandler.java` (logic unchanged)
- `EspHomeProtobufEncoder.java` / `EspHomeProtobufDecoder.java`
- `NettyEspHomeConnection.java`, `NettyEspHomeEventHandler.java`
- `app-cat-recognizer` (Java client not wired to app yet)

---

## Protocol Reference (copy into implementation)

### Noise parameters

```
Protocol:  Noise_NNpsk0_25519_ChaChaPoly_SHA256
Prologue:  "NoiseAPIInit\u0000\u0000" (US-ASCII bytes)
PSK:       exactly 32 bytes (base64-decoded from config key)
Pattern:   message 1 = psk,e ; message 2 = e,ee
Role:      client = initiator only (responder needed for tests)
```

### Encrypted wire frame (handshake + data)

```
[0x01][size_high][size_low][payload...]   // size is 16-bit big-endian, max 65535
```

### Noise pre-handshake sequence (initiator/client)

```
1. Client → Server: encode(empty payload)           // [0x01,0x00,0x00]
2. Server → Client: payload[0] == 0x01              // protocol selection, e.g. [0x01,'c',0x00]
3. Client → Server: encode([0x00] + writeMessage())  // first Noise message
4. Server → Client: payload[0] == 0x00 → readMessage(rest)
                   payload[0] == 0x01 → reject (reason = rest as UTF-8)
5. split() → sendCipher, receiveCipher
```

### Encrypted data frame (after Noise)

Inner plaintext before encrypt:

```
[type_hi][type_lo][data_len_hi][data_len_lo][protobuf bytes...]
```

Outer wire: same `[0x01][BE size][ciphertext]` where ciphertext = ChaCha20-Poly1305(inner, AD=empty).

Max inner protobuf payload: `65535 - 4 - 16 = 65515` bytes.

### Plaintext wire (unchanged, for comparison)

```
[0x00][varint payloadSize][varint messageType][protobuf...]
```

---

## Netty Pipeline Design

### Plaintext (current — extract into `initPlaintextChannel`)

```
Inbound:  frameDecoder → protobufDecoder → handshakeHandler
Outbound: frameEncoder ← protobufEncoder
```

Optional timeout handlers (added in Stage 5):

```
Inbound head:  readTimeoutHandler (after socket)
Outbound tail: (none for connect — use Bootstrap option)
```

### Encrypted Phase 1 — Noise handshake only

```
Inbound:  readTimeoutHandler → encryptedFrameDecoder → noiseHandshakeHandler
Outbound: encryptedFrameEncoder ← noiseHandshakeHandler
```

### Encrypted Phase 2 — after Noise completes (noise handler reconfigures pipeline)

```
Inbound:  readTimeoutHandler → encryptedFrameDecoder → encryptedPayloadDecoder → protobufDecoder → handshakeHandler
Outbound: encryptedPayloadEncoder ← protobufEncoder ← encryptedFrameEncoder
```

`EspHomeNoiseHandshakeHandler.onComplete()`:
1. Set `EspHomeChannelAttributes.SEND_CIPHER` and `RECEIVE_CIPHER` on channel.
2. Add `encryptedPayloadDecoder`, `protobufDecoder`, `protobufEncoder`, `encryptedPayloadEncoder`, `handshakeHandler` before/after self as needed.
3. Remove self from pipeline.
4. Do **not** complete the connection `CompletableFuture` — `EspHomeHandshakeHandler` still owns that.

---

## Stage 0: Build Setup

### Task 0.1: Add Gradle dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `lib-esphome-client/build.gradle.kts`

- [ ] **Step 1: Add catalog entries**

In `gradle/libs.versions.toml`:

```toml
[versions]
junit = "5.12.2"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
netty-handler = { module = "io.netty:netty-handler", version.ref = "netty" }
```

- [ ] **Step 2: Update lib-esphome-client build**

In `lib-esphome-client/build.gradle.kts` dependencies block:

```kotlin
implementation(libs.netty.handler)

testImplementation(libs.junit.jupiter)
```

Ensure tests block:

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew :lib-esphome-client:compileJava :lib-esphome-client:compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Stage 0 complete** — check Progress Overview box

---

## Stage 1: Java Noise Crypto Port

Port logic from Kotlin `internal/noise/*` to Java `async/noise/*`. Behavior must match Kotlin tests byte-for-byte.

### Task 1.1: NoiseConstants + NoiseKeyMaterial

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseConstants.java`
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseKeyMaterial.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseKeyMaterialTest.java`

- [ ] **Step 1: Write failing tests**

```java
package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class NoiseKeyMaterialTest {

    @Test
    void decodesValidBase64Key() {
        byte[] raw = new byte[32];
        for (int i = 0; i < 32; i++) raw[i] = (byte) (i + 1);
        String encoded = Base64.getEncoder().encodeToString(raw);

        assertArrayEquals(raw, NoiseKeyMaterial.decodeBase64(encoded));
    }

    @Test
    void rejectsBlankKey() {
        assertThrows(EspHomeProtocolException.class, () -> NoiseKeyMaterial.decodeBase64("  "));
    }

    @Test
    void rejectsWrongLength() {
        String encoded = Base64.getEncoder().encodeToString(new byte[16]);
        EspHomeProtocolException ex = assertThrows(
            EspHomeProtocolException.class,
            () -> NoiseKeyMaterial.decodeBase64(encoded)
        );
        assertTrue(ex.getMessage().contains("32 bytes"));
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.noise.NoiseKeyMaterialTest"`
Expected: compilation failure or test errors (classes missing)

- [ ] **Step 3: Implement NoiseConstants**

```java
package io.github.arhor.esphome.client.async.noise;

import java.nio.charset.StandardCharsets;

public final class NoiseConstants {
    public static final int NOISE_MAX_MESSAGE_LENGTH = 65_535;
    public static final int NOISE_PSK_LENGTH = 32;
    public static final int AUTH_TAG_LENGTH = 16;
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";
    public static final String DH_ALGORITHM = "X25519";
    public static final String PROTOCOL_NAME = "Noise_NNpsk0_25519_ChaChaPoly_SHA256";

    public static final byte[] EMPTY = new byte[0];
    public static final byte[] ESPHOME_NOISE_PROLOGUE =
        "NoiseAPIInit\u0000\u0000".getBytes(StandardCharsets.US_ASCII);

    private NoiseConstants() {}
}
```

- [ ] **Step 4: Implement NoiseKeyMaterial**

Mirror Kotlin `NoiseKeyMaterial.kt` — `decodeBase64(String)` throws `EspHomeProtocolException`.

- [ ] **Step 5: Run test — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.noise.NoiseKeyMaterialTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

### Task 1.2: NoiseCipherState

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseCipherState.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseCipherStateTest.java`

- [ ] **Step 1: Write failing tests**

Port assertions from `lib-esphome-client/src/test/kotlin/.../noise/NoiseCipherStateTest.kt`:
- encrypt/decrypt round-trip with same key
- nonce increments (second ciphertext differs)
- tampered ciphertext throws `EspHomeProtocolException`
- plaintext too large throws

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement NoiseCipherState**

Port from Kotlin `NoiseCipherState.kt`:
- `initializeKey(byte[] key)` — copies key, resets nonce to 0
- `encryptWithAd(byte[] ad, byte[] plaintext)` — no-op passthrough if key null
- `decryptWithAd(byte[] ad, byte[] ciphertext)`
- Nonce: 12 bytes, first 4 zero, bytes 4–11 little-endian counter
- Hold one `javax.crypto.Cipher` instance per `NoiseCipherState` object (safe — one object per channel)

- [ ] **Step 4: Run test — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.noise.NoiseCipherStateTest"`

### Task 1.3: NoiseHandshakeState

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/noise/NoiseHandshakeState.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/noise/NoiseHandshakeStateTest.java`

- [ ] **Step 1: Write failing tests**

Port from `NoiseHandshakeStateTest.kt`:
- initiator + responder complete handshake with same PSK
- initiator `sendCipher`/`receiveCipher` are non-null after completion
- wrong PSK causes decrypt failure on second message

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement NoiseHandshakeState**

Port from Kotlin `NoiseHandshakeState.kt`:
- `static NoiseHandshakeState initiator(byte[] psk)`
- `static NoiseHandshakeState responder(byte[] psk)` (tests only)
- `byte[] writeMessage(byte[] payload)` — default empty payload
- `byte[] readMessage(byte[] message)`
- Public fields/getters: `NoiseCipherState getSendCipher()`, `getReceiveCipher()`, `boolean isComplete()`
- Inner `SymmetricState` with HKDF, mixHash, mixKey, encryptAndHash, decryptAndHash, split
- X25519 via `KeyPairGenerator.getInstance("X25519")` and `KeyAgreement`

- [ ] **Step 4: Run test — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.noise.NoiseHandshakeStateTest"`

- [ ] **Stage 1 complete** — check Progress Overview box

---

## Stage 2: Encrypted Wire Frame Handlers

These replace VarInt framing in encrypted mode. Logic mirrors `EncryptedEspHomeFrameCodec.encode/decode` (outer layer only).

### Task 2.1: EspHomeEncryptedFrameDecoder + EspHomeEncryptedFrameEncoder

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameDecoder.java`
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameEncoder.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedFrameCodecTest.java`

- [ ] **Step 1: Write failing tests**

Port wire-layer tests from `EncryptedEspHomeFrameCodecTest.kt` (indicator, BE length, truncation, invalid indicator). Test via Netty `EmbeddedChannel`:

```java
@Test
void encodesAndDecodesRoundTrip() {
    EmbeddedChannel ch = new EmbeddedChannel(
        new EspHomeEncryptedFrameEncoder(),
        new EspHomeEncryptedFrameDecoder()
    );
    ByteBuf payload = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    assertTrue(ch.writeOutbound(payload));

    ByteBuf encoded = ch.readOutbound();
    assertEquals(0x01, encoded.readByte());
    assertEquals(0, encoded.readUnsignedByte());
    assertEquals(3, encoded.readUnsignedByte());
    assertArrayEquals(new byte[]{1, 2, 3}, ByteBufUtil.getBytes(encoded));

    assertTrue(ch.writeInbound(encoded.retainedDuplicate()));
    ByteBuf decoded = ch.readInbound();
    assertArrayEquals(new byte[]{1, 2, 3}, ByteBufUtil.getBytes(decoded));
    decoded.release();
    encoded.release();
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement EspHomeEncryptedFrameDecoder**

`extends ByteToMessageDecoder`:
- Require indicator `0x01` (throw `EspHomeProtocolException` wrapped or use `CorruptedFrameException` — prefer `EspHomeProtocolException` for consistency with async exceptions)
- Read 16-bit BE size; if incomplete, reset reader index and return
- Emit `ByteBuf` payload (retained slice)
- Constants: `ENCRYPTED_INDICATOR = 0x01`, `MAX_PAYLOAD_SIZE = 65535`

- [ ] **Step 4: Implement EspHomeEncryptedFrameEncoder**

`extends MessageToByteEncoder<ByteBuf>`:
- Write `[0x01][size_hi][size_lo][payload bytes]`
- Release input in `finally` (match `EspHomeVarIntFrameEncoder` pattern)

- [ ] **Step 5: Run test — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.codec.encrypted.EspHomeEncryptedFrameCodecTest"`

- [ ] **Stage 2 complete** — check Progress Overview box

---

## Stage 3: Encrypted Payload Handlers

Bridge between wire `ByteBuf` and existing `EspHomeFrame` record.

### Task 3.1: EspHomeChannelAttributes

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeChannelAttributes.java`

- [ ] **Step 1: Create attribute keys**

```java
package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.noise.NoiseCipherState;
import io.netty.util.AttributeKey;

public final class EspHomeChannelAttributes {
    public static final AttributeKey<NoiseCipherState> SEND_CIPHER =
        AttributeKey.valueOf("esphome.sendCipher");
    public static final AttributeKey<NoiseCipherState> RECEIVE_CIPHER =
        AttributeKey.valueOf("esphome.receiveCipher");

    private EspHomeChannelAttributes() {}
}
```

### Task 3.2: EspHomeEncryptedPayloadDecoder + EspHomeEncryptedPayloadEncoder

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadDecoder.java`
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadEncoder.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/codec/encrypted/EspHomeEncryptedPayloadCodecTest.java`

- [ ] **Step 1: Write failing tests**

Port from `EncryptedEspHomeFrameCodecTest.kt` encrypt/decrypt frame tests:
- encrypted inner header `[0x00, 0x2d, 0x00, 0x03, 1, 2, 3]` for type 45
- decode type 46 payload `[9, 8]`
- reject payload > 65515 bytes

Test via `EmbeddedChannel` with ciphers set on channel attributes:

```java
NoiseCipherState send = new NoiseCipherState();
send.initializeKey(key);
NoiseCipherState recv = new NoiseCipherState();
recv.initializeKey(key);

EmbeddedChannel ch = new EmbeddedChannel(
    new EspHomeEncryptedFrameEncoder(),
    new EspHomeEncryptedFrameDecoder(),
    new EspHomeEncryptedPayloadEncoder(),
    new EspHomeEncryptedPayloadDecoder()
);
ch.attr(EspHomeChannelAttributes.SEND_CIPHER).set(send);
ch.attr(EspHomeChannelAttributes.RECEIVE_CIPHER).set(recv);
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement EspHomeEncryptedPayloadEncoder**

`extends MessageToMessageEncoder<EspHomeFrame>`:
- Read `sendCipher` from `ctx.channel().attr(SEND_CIPHER)`
- Build 4-byte header + copy payload into temp buffer (use `ctx.alloc().buffer(4 + payload.readableBytes())`)
- Encrypt with `cipher.encryptWithAd(NoiseConstants.EMPTY, plaintextBytes)`
- Output encrypted `ByteBuf` for frame encoder
- Validate size limits (mirror Kotlin)

- [ ] **Step 4: Implement EspHomeEncryptedPayloadDecoder**

`extends MessageToMessageDecoder<ByteBuf>`:
- Read `receiveCipher` from channel attr
- Decrypt ciphertext bytes
- Parse `[type: u16 BE][len: u16 BE][data]`
- Emit `new EspHomeFrame(messageType, payloadBuf)` where `payloadBuf` is retained slice from allocator
- Release input ciphertext in `finally`

- [ ] **Step 5: Run test — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.codec.encrypted.EspHomeEncryptedPayloadCodecTest"`

- [ ] **Stage 3 complete** — check Progress Overview box

---

## Stage 4: Noise Handshake Handler

### Task 4.1: EspHomeNoiseHandshakeHandler

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeNoiseHandshakeHandler.java`

- [ ] **Step 1: Define handler skeleton**

```java
public final class EspHomeNoiseHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private enum Step { SEND_INIT, WAIT_SERVER_HELLO, SEND_CLIENT_HANDSHAKE, WAIT_SERVER_HANDSHAKE }

    private final byte[] psk;
    private final EspHomeClientConfig config;
    private final List<EspHomeSubscription> subscriptions;
    private final CompletableFuture<EspHomeConnection> connectionFuture;

    private Step step = Step.SEND_INIT;
    private NoiseHandshakeState handshake;

    // constructor stores deps, handshake = NoiseHandshakeState.initiator(psk)
}
```

- [ ] **Step 2: Implement channelActive**

On `SEND_INIT`:
- Write empty encrypted frame: frame encoder expects `ByteBuf` — write `Unpooled.EMPTY_BUFFER` through pipeline OR call helper that writes `[0x01,0x00,0x00]` directly via `writeAndFlush`
- Set step = `WAIT_SERVER_HELLO`

Note: simplest approach — in `channelActive`, write raw bytes `[0x01,0x00,0x00]` with `ctx.writeAndFlush(Unpooled.wrappedBuffer(...))` before frame encoder is strictly outbound-only. **Preferred:** emit `Unpooled.EMPTY_BUFFER` outbound so `EspHomeEncryptedFrameEncoder` wraps it.

- [ ] **Step 3: Implement channelRead0 for each step**

**WAIT_SERVER_HELLO:**
- Require `payload.readableBytes() >= 1 && payload.getByte(0) == 0x01`
- Else fail with `EspHomeProtocolException("ESPHome encrypted server hello selected an unsupported protocol")`
- step = `SEND_CLIENT_HANDSHAKE`
- Build client message: `byte[] msg = handshake.writeMessage()`; prepend `0x00`; wrap in `ByteBuf`; `writeAndFlush`

**WAIT_SERVER_HANDSHAKE:**
- Read first byte:
  - `0x01` → read rest as UTF-8 reason → `EspHomeProtocolException("ESPHome encrypted handshake was rejected: " + reason)`
  - `0x00` → `handshake.readMessage(remainingBytes)`
- Call package-private package method `completeHandshake(ctx)`

- [ ] **Step 4: Implement completeHandshake(ctx)**

```java
private void completeHandshake(ChannelHandlerContext ctx) {
    ctx.channel().attr(EspHomeChannelAttributes.SEND_CIPHER).set(handshake.getSendCipher());
    ctx.channel().attr(EspHomeChannelAttributes.RECEIVE_CIPHER).set(handshake.getReceiveCipher());

    ChannelPipeline p = ctx.pipeline();
    p.addAfter("encryptedFrameDecoder", "encryptedPayloadDecoder", new EspHomeEncryptedPayloadDecoder());
    p.addAfter("encryptedPayloadDecoder", "protobufDecoder", new EspHomeProtobufDecoder());
    p.addBefore("encryptedFrameEncoder", "encryptedPayloadEncoder", new EspHomeEncryptedPayloadEncoder());
    p.addBefore("encryptedPayloadEncoder", "protobufEncoder", new EspHomeProtobufEncoder());
    p.addAfter("protobufDecoder", "handshakeHandler",
        new EspHomeHandshakeHandler(connectionFuture, subscriptions, config));

    p.remove(this);
}
```

Adjust handler names to match actual pipeline names set in `NettyEspHomeClient`.

- [ ] **Step 5: Implement error paths**

- `exceptionCaught` → `connectionFuture.completeExceptionally(cause)` + `ctx.close()`
- `channelInactive` before future done → `EspHomeProtocolException("Connection closed during ESPHome Noise handshake")`

- [ ] **Step 6: Manual smoke via unit test prep**

Defer full integration to Stage 6; verify compilation:

Run: `./gradlew :lib-esphome-client:compileJava`

- [ ] **Stage 4 complete** — check Progress Overview box

---

## Stage 5: Config + Pipeline Branching + Timeouts

### Task 5.1: EspHomeEncryptionConfig + EspHomeClientConfig

**Files:**
- Create: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeEncryptionConfig.java`
- Modify: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/EspHomeClientConfig.java`
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/EspHomeClientConfigTest.java`

- [ ] **Step 1: Write failing config tests**

```java
@Test
void encryptionEnabledRequiresKey() {
    assertThrows(IllegalArgumentException.class, () ->
        new EspHomeEncryptionConfig(true, null));
}

@Test
void defaultEncryptionDisabled() {
    var config = new EspHomeClientConfig("host", 6053, "client", null);
    assertFalse(config.encryption().enabled());
}

@Test
void rejectsZeroConnectTimeout() {
    assertThrows(IllegalArgumentException.class, () ->
        new EspHomeClientConfig("host", 6053, "client", null,
            EspHomeEncryptionConfig.disabled(), Duration.ZERO, Duration.ofSeconds(5)));
}
```

- [ ] **Step 2: Implement EspHomeEncryptionConfig**

```java
public record EspHomeEncryptionConfig(boolean enabled, String key) {
    public EspHomeEncryptionConfig {
        if (enabled && (key == null || key.isBlank())) {
            throw new IllegalArgumentException("key must be configured when encryption is enabled");
        }
    }
    public static EspHomeEncryptionConfig disabled() {
        return new EspHomeEncryptionConfig(false, null);
    }
}
```

- [ ] **Step 3: Extend EspHomeClientConfig**

Add fields with defaults mirroring Kotlin config:

```java
public record EspHomeClientConfig(
    String host,
    int port,
    String clientName,
    String password,
    EspHomeEncryptionConfig encryption,
    Duration connectTimeout,
    Duration readTimeout,
    int apiVersionMajor,
    int apiVersionMinor
) {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    public EspHomeClientConfig(String host, int port, String clientName, String password) {
        this(host, port, clientName, password,
            EspHomeEncryptionConfig.disabled(),
            DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT,
            API_VERSION_MAJOR, API_VERSION_MINOR);
    }

    public EspHomeClientConfig {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("host must not be blank");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("port must be between 1 and 65535");
        if (connectTimeout.isZero() || connectTimeout.isNegative())
            throw new IllegalArgumentException("connectTimeout must be positive");
        if (readTimeout.isZero() || readTimeout.isNegative())
            throw new IllegalArgumentException("readTimeout must be positive");
        if (encryption == null) encryption = EspHomeEncryptionConfig.disabled();
    }

    public int connectTimeoutMillis() { return Math.toIntExact(connectTimeout.toMillis()); }
    public int readTimeoutMillis() { return Math.toIntExact(readTimeout.toMillis()); }
}
```

- [ ] **Step 4: Run config tests — expect PASS**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.EspHomeClientConfigTest"`

### Task 5.2: NettyEspHomeClient pipeline branching

**Files:**
- Modify: `lib-esphome-client/src/main/java/io/github/arhor/esphome/client/async/NettyEspHomeClient.java`

- [ ] **Step 1: Extract initPlaintextChannel**

Move existing `initChannel` body into private method `initPlaintextChannel(SocketChannel ch, ...)`.

Add timeout handlers:

```java
import io.netty.handler.timeout.ReadTimeoutHandler;

// at start of inbound pipeline:
pipeline.addLast("readTimeout", new ReadTimeoutHandler(config.readTimeoutMillis(), TimeUnit.MILLISECONDS));

// on bootstrap:
.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeoutMillis())
```

- [ ] **Step 2: Implement initEncryptedChannel**

```java
private void initEncryptedChannel(
    SocketChannel ch,
    List<EspHomeSubscription> subscriptions,
    CompletableFuture<EspHomeConnection> resultFuture
) {
    byte[] psk = NoiseKeyMaterial.decodeBase64(config.encryption().key());
    var pipeline = ch.pipeline();

    pipeline.addLast("readTimeout", new ReadTimeoutHandler(config.readTimeoutMillis(), TimeUnit.MILLISECONDS));
    pipeline.addLast("encryptedFrameDecoder", new EspHomeEncryptedFrameDecoder());
    pipeline.addLast("encryptedFrameEncoder", new EspHomeEncryptedFrameEncoder());
    pipeline.addLast("noiseHandshakeHandler",
        new EspHomeNoiseHandshakeHandler(psk, config, subscriptions, resultFuture));
}
```

Note: encoder is added before handshake handler so outbound writes from handler pass through encoder. Order for outbound in Netty is reverse — verify with Netty convention: last added is first outbound. **Correct order:**

```java
pipeline.addLast("readTimeout", ...);
pipeline.addLast("encryptedFrameDecoder", new EspHomeEncryptedFrameDecoder());
pipeline.addLast("noiseHandshakeHandler", ...);
pipeline.addLast("encryptedFrameEncoder", new EspHomeEncryptedFrameEncoder());
```

Inbound: socket → readTimeout → frameDecoder → noiseHandler
Outbound: noiseHandler → frameEncoder → socket

- [ ] **Step 3: Branch in createChannelInitializer**

```java
if (config.encryption().enabled()) {
    initEncryptedChannel(ch, subscriptions, resultFuture);
} else {
    initPlaintextChannel(ch, subscriptions, resultFuture);
}
```

Also add `CONNECT_TIMEOUT_MILLIS` to bootstrap in `connect()`:

```java
.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeoutMillis())
```

- [ ] **Step 4: Add read timeout handling**

Ensure `ReadTimeoutHandler` triggers `exceptionCaught` in downstream handlers. `EspHomeNoiseHandshakeHandler` and `EspHomeHandshakeHandler` already close on exception. Optionally map `ReadTimeoutException` to `EspHomeTransportException` in handlers.

- [ ] **Step 5: Compile check**

Run: `./gradlew :lib-esphome-client:compileJava`

- [ ] **Stage 5 complete** — check Progress Overview box

---

## Stage 6: End-to-End Netty Loopback Test

### Task 6.1: NettyEncryptedClientTest

**Files:**
- Create: `lib-esphome-client/src/test/java/io/github/arhor/esphome/client/async/NettyEncryptedClientTest.java`

- [ ] **Step 1: Write failing integration test**

Mirror `EncryptedTransportTest.kt` using Netty `ServerBootstrap` instead of `ServerSocket`:

```java
@Test
void connectsOverEncryptedNativeApiAndExchangesFrames() throws Exception {
    byte[] psk = new byte[32];
    for (int i = 0; i < 32; i++) psk[i] = (byte) (i + 1);

    EventLoopGroup group = new NioEventLoopGroup(1);
    try {
        // Server: noise responder + encrypted frame codec
        // 1. decode empty init
        // 2. send encode([0x01, 'c', 0x00])
        // 3. decode client handshake, readMessage, send encode([0x00] + writeMessage())
        // 4. decode encrypted HelloRequest (message type 1) — optional: full hello/connect
        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicInteger port = new AtomicInteger();

        ServerBootstrap server = new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override protected void initChannel(SocketChannel ch) { /* noise responder pipeline */ }
            });

        Channel serverChannel = server.bind(0).sync().channel();
        port.set(((InetSocketAddress) serverChannel.localAddress()).getPort());
        serverReady.countDown();

        EspHomeClientConfig config = new EspHomeClientConfig(
            "127.0.0.1", port.get(), "test-client", null,
            new EspHomeEncryptionConfig(true, Base64.getEncoder().encodeToString(psk)),
            Duration.ofSeconds(2), Duration.ofSeconds(2)
        );

        NettyEspHomeClient client = new NettyEspHomeClient(config);
        EspHomeConnection connection = client.connect().get(5, TimeUnit.SECONDS);

        // Assert connection non-null; optionally send ping command and observe event
        connection.close();
        client.close();
    } finally {
        group.shutdownGracefully().sync();
    }
}
```

Minimum acceptance: client completes Noise handshake **and** ESPHome Hello/Connect (connection future succeeds).

For server-side Hello/Connect responses, reuse protobuf types:
- Respond to `HelloRequest` with `HelloResponse` (api version 1.10)
- Respond to `ConnectRequest` with `ConnectResponse` (invalidPassword=false)

Server pipeline after Noise: same encrypted payload handlers + a `SimpleChannelInboundHandler<MessageLite>` that replies to hello/connect.

- [ ] **Step 2: Run test — expect FAIL initially**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.NettyEncryptedClientTest"`

- [ ] **Step 3: Fix integration issues until PASS**

Common fixes:
- Pipeline handler order (outbound/inbound)
- ByteBuf release leaks (run with `-Dio.netty.leakDetectionLevel=paranoid` if needed)
- Server not encrypting responses with matching cipher direction

- [ ] **Step 4: Full handshake success**

Expected: test passes, connection future completes, no leaked buffers

- [ ] **Stage 6 complete** — check Progress Overview box

---

## Stage 7: Final Verification

### Task 7.1: Full test suite + regression

- [ ] **Step 1: Run all Java async tests**

Run: `./gradlew :lib-esphome-client:test --tests "io.github.arhor.esphome.client.async.*"`
Expected: all pass

- [ ] **Step 2: Run full lib-esphome-client test suite (Kotlin + Java)**

Run: `./gradlew :lib-esphome-client:test`
Expected: all pass — Kotlin tests must remain green (no deletions)

- [ ] **Step 3: Run root test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Verify plaintext path unchanged**

Add or run test with `EspHomeEncryptionConfig.disabled()` connecting to plaintext mock server (optional quick test). Confirm existing plaintext VarInt pipeline still works.

- [ ] **Step 5: Update this plan checkboxes**

Mark all stages in Progress Overview as complete.

- [ ] **Stage 7 complete**

---

## Error Mapping Reference

| Condition | Exception type |
|-----------|----------------|
| Blank/invalid base64 PSK at config | `EspHomeProtocolException` |
| Server hello first byte != 0x01 | `EspHomeProtocolException` |
| Handshake rejection byte 0x01 | `EspHomeProtocolException` with reason |
| ChaCha20-Poly1305 failure | `EspHomeProtocolException` |
| Invalid encrypted indicator | `EspHomeProtocolException` |
| Read timeout | `ReadTimeoutException` → caught, future failed, channel closed |
| Connect timeout | Netty connect future fails → `CompletableFuture` exceptionally |
| Invalid password (post-encryption) | `EspHomeAuthenticationException` (existing handler) |

---

## Performance Notes (implement during Stages 2–3, do not defer)

- One `NoiseCipherState` per channel — never `@Sharable` across channels.
- Reuse 12-byte nonce array inside `NoiseCipherState`.
- Prefer `ByteBuf.hasArray()` / `nioBuffer()` paths in payload decoder before copying to `byte[]` for `Cipher.doFinal`. Copy is acceptable if JDK Cipher requires contiguous array — minimize allocations on data path.
- Pre-size outbound buffers: `ctx.alloc().buffer(3 + ciphertextLength)`.
- Handshake path may allocate freely (cold path, 2 round-trips).

---

## Out of Scope (follow-up work, not this plan)

- Wiring `NettyEspHomeClient` into `app-cat-recognizer` (still uses Kotlin `EspHomeClientDefault`)
- Removing Kotlin transport/noise code
- Combined frame+payload codec optimization
- Automatic encrypted/plaintext negotiation
- Real-device CI tests

---

## Spec Coverage Self-Review

| Spec requirement | Plan task |
|------------------|-----------|
| Noise_NNpsk0_25519_ChaChaPoly_SHA256 | Stage 1 Task 1.3 |
| ESPHome prologue | Stage 1 NoiseHandshakeState init |
| Encrypted frame `[0x01][BE size][payload]` | Stage 2 |
| Encrypted transport behind framing layer | Stages 2–4 |
| Protocol client transport-agnostic | Stages 3–4 (protobuf layer unchanged) |
| Explicit enable + key, no silent fallback | Stage 5 EspHomeEncryptionConfig |
| Plaintext default unchanged | Stage 5 branching |
| Key material 32-byte base64 | Stage 1 Task 1.1 |
| Focused unit + loopback tests | Stages 1–6 |
| Connect/read timeouts | Stage 5 |
| Keep Kotlin code | Decisions + Out of Scope |

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-18-esphome-java-netty-encryption.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per stage/task, review between stages, update checkboxes in this file after each stage.

2. **Inline Execution** — implement stage-by-stage in the current session, marking checkboxes as you go.

**Which approach?**
