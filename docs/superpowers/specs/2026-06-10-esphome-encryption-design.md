# ESPHome Encryption Design

## Goal

Add explicit ESPHome native API encryption support to `services/lib-esphome-client` and expose it through
`app-cat-recognizer` without changing the existing camera-facing `FrameClient` contract.

This slice supports ESPHome's Noise-based encrypted transport only when the operator enables it and provides a base64
32-byte encryption key. Plaintext remains the default. There is no automatic fallback from encrypted to plaintext in this
slice because silent fallback can hide a misconfigured or downgraded device connection.

## Scope

In scope:

- Implement the ESPHome Noise handshake pattern `Noise_NNpsk0_25519_ChaChaPoly_SHA256`
- Use ESPHome's prologue `NoiseAPIInit\u0000\u0000`
- Add encrypted ESPHome frame coding: `[0x01][size_high][size_low][encrypted_payload]`
- Add an encrypted `EspHomeTransport` implementation behind the existing transport boundary
- Keep `EspHomeProtocolClient` working with decrypted `EspHomeFrame` values
- Add Quarkus config for encryption enablement and key material
- Keep secrets out of committed config files
- Add focused unit and loopback tests for crypto, encrypted framing, transport selection, and app config

Out of scope:

- Automatic encrypted/plaintext negotiation
- Entity discovery, state subscriptions, commands, logs, Bluetooth, or voice assistant support
- Real-device CI tests
- Storing or committing actual ESPHome encryption keys

## Architecture

`EspHomeProtocolClient` should remain message-oriented. It sends and receives `EspHomeFrame` values and should not know
whether the underlying socket is plaintext or encrypted.

The encryption implementation adds a second socket transport:

```text
DefaultEspHomeClient
  -> PlaintextEspHomeTransport when encryption is disabled
  -> EncryptedEspHomeTransport when encryption is enabled
       -> NoiseHandshakeState
       -> EncryptedEspHomeFrameCodec
```

`EncryptedEspHomeTransport` owns the TCP socket, performs the two-message Noise handshake, and then encrypts or decrypts
the plaintext ESPHome frame bytes produced by `EspHomeFrameCodec`.

## Crypto Design

The Noise implementation should be small and internal:

- `NoiseKeyMaterial` decodes base64 key material and requires exactly 32 bytes.
- `NoiseCipherState` handles ChaCha20-Poly1305 with a 96-bit nonce whose first four bytes are zero and last eight bytes
  are a little-endian counter.
- `NoiseHandshakeState` implements initiator and responder roles for tests, but production uses only the initiator role.
- `NoiseHandshakeState` follows the NNpsk0 pattern:
  - message 1: `psk, e`
  - message 2: `e, ee`

JDK crypto should be used directly. If the local JDK does not support `X25519` and `ChaCha20-Poly1305`, tests will expose
that immediately and the implementation should fail with a clear `EspHomeProtocolException` or `EspHomeTransportException`.

## Configuration

Library config:

```kotlin
data class EspHomeEncryptionConfig(
    val enabled: Boolean = false,
    val key: String? = null,
)
```

`EspHomeClientConfig` should include `encryption: EspHomeEncryptionConfig = EspHomeEncryptionConfig()`.

App config:

```properties
cat-recognizer.camera.native-api.encryption.enabled=false
```

The key should be supplied by environment or another external Quarkus config source, for example:

```text
CAT_RECOGNIZER_CAMERA_NATIVE_API_ENCRYPTION_KEY=<base64 key>
```

The app should fail when encrypted mode is enabled without a valid key.

## Error Handling

Encrypted handshake failures, invalid key material, authentication tag failures, malformed encrypted frames, and socket
read/write failures should become existing ESPHome client exception types. `EspHomeNativeFrameClient` should continue to
map these failures into `FrameSourceError(code = "FRAME_FETCH_FAILED", retriable = true)`.

## Testing

Use TDD for each layer:

- Key material tests for base64 decoding and length validation
- Cipher tests for encrypt/decrypt round trips and authentication failure
- Handshake tests using in-process initiator/responder states with the same key
- Encrypted frame codec tests for indicator, length, and truncation handling
- Loopback encrypted transport test that sends an `EspHomeFrame` over an in-process socket pair
- Config binding tests for disabled default and external key mapping
- Native frame client tests verifying the encryption config is passed into `EspHomeClientConfig`

Final verification remains:

```bash
cd services
./gradlew :lib-esphome-client:test
./gradlew :app-cat-recognizer:test
./gradlew :app-cat-recognizer:build
```

## Acceptance Criteria

- Plaintext behavior remains unchanged by default
- Encrypted mode requires explicit enablement and valid key material
- The protocol client remains transport-agnostic
- No actual encryption key is committed
- Focused tests cover crypto, frame coding, transport, config, and app mapping
- Full library/app tests and app build pass
