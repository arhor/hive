package io.github.arhor.esphome.client.internal.noise

import io.github.arhor.esphome.client.exception.EspHomeProtocolException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.XECPublicKey
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPublicKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class NoiseHandshakeState private constructor(
    private val initiator: Boolean,
    private val psk: ByteArray,
) {

    lateinit var sendCipher: NoiseCipherState
        private set
    lateinit var receiveCipher: NoiseCipherState
        private set
    var isComplete: Boolean = false
        private set

    private val symmetricState = SymmetricState()
    private var ephemeralPrivateKey: PrivateKey? = null
    private var remotePublicKey: PublicKey? = null
    private var patternIndex: Int = 0

    init {
        if (psk.size != NoiseConstants.NOISE_PSK_LENGTH) {
            throw EspHomeProtocolException("Noise PSK must be ${NoiseConstants.NOISE_PSK_LENGTH} bytes")
        }
        symmetricState.mixHash(NoiseConstants.ESPHOME_NOISE_PROLOGUE)
    }

    fun writeMessage(payload: ByteArray = NoiseConstants.EMPTY): ByteArray {
        ensureInProgress()
        val output = mutableListOf<ByteArray>()
        when (patternIndex++) {
            0 -> {
                symmetricState.mixKeyAndHash(psk)
                val keyPair = generateKeyPair()
                ephemeralPrivateKey = keyPair.privateKey
                output += keyPair.publicKeyRaw
                symmetricState.mixHash(keyPair.publicKeyRaw)
                symmetricState.mixKey(keyPair.publicKeyRaw)
            }
            1 -> {
                val keyPair = generateKeyPair()
                ephemeralPrivateKey = keyPair.privateKey
                output += keyPair.publicKeyRaw
                symmetricState.mixHash(keyPair.publicKeyRaw)
                symmetricState.mixKey(keyPair.publicKeyRaw)
                symmetricState.mixKey(diffieHellman())
            }
            else -> throw EspHomeProtocolException("Noise handshake has no pattern to write")
        }

        val encryptedPayload = symmetricState.encryptAndHash(payload)
        if (encryptedPayload.isNotEmpty()) {
            output += encryptedPayload
        }
        if (patternIndex >= 2 && !initiator) {
            split()
        }
        return output.concat()
    }

    fun readMessage(message: ByteArray): ByteArray {
        ensureInProgress()
        var offset = 0
        val payload = when (patternIndex++) {
            0 -> {
                symmetricState.mixKeyAndHash(psk)
                offset += readRemoteEphemeral(message, offset)
                symmetricState.decryptAndHash(message.copyOfRange(offset, message.size))
            }
            1 -> {
                offset += readRemoteEphemeral(message, offset)
                symmetricState.mixKey(diffieHellman())
                symmetricState.decryptAndHash(message.copyOfRange(offset, message.size))
            }
            else -> throw EspHomeProtocolException("Noise handshake has no pattern to read")
        }

        if (patternIndex >= 2 && initiator) {
            split()
        }
        return payload
    }

    private fun readRemoteEphemeral(message: ByteArray, offset: Int): Int {
        if (message.size - offset < X25519_KEY_SIZE) {
            throw EspHomeProtocolException("Noise handshake message ended before ephemeral key was complete")
        }
        val rawKey = message.copyOfRange(offset, offset + X25519_KEY_SIZE)
        symmetricState.mixHash(rawKey)
        symmetricState.mixKey(rawKey)
        remotePublicKey = importPublicKey(rawKey)
        return X25519_KEY_SIZE
    }

    private fun diffieHellman(): ByteArray {
        val privateKey = ephemeralPrivateKey
            ?: throw EspHomeProtocolException("Noise handshake is missing local ephemeral key")
        val publicKey = remotePublicKey
            ?: throw EspHomeProtocolException("Noise handshake is missing remote ephemeral key")

        return try {
            val agreement = KeyAgreement.getInstance(NoiseConstants.DH_ALGORITHM)
            agreement.init(privateKey)
            agreement.doPhase(publicKey, true)
            agreement.generateSecret()
        } catch (exception: Exception) {
            throw EspHomeProtocolException("Noise X25519 key agreement failed", exception)
        }
    }

    private fun split() {
        val keys = symmetricState.split()
        val first = NoiseCipherState().apply { initializeKey(keys.first) }
        val second = NoiseCipherState().apply { initializeKey(keys.second) }
        if (initiator) {
            sendCipher = first
            receiveCipher = second
        } else {
            receiveCipher = first
            sendCipher = second
        }
        isComplete = true
    }

    private fun ensureInProgress() {
        if (isComplete || patternIndex >= 2) {
            throw EspHomeProtocolException("Noise handshake is already complete")
        }
    }

    private class SymmetricState {
        private var chainingKey: ByteArray = protocolNameHash()
        private var handshakeHash: ByteArray = protocolNameHash()
        private val cipherState = NoiseCipherState()

        fun mixHash(data: ByteArray) {
            handshakeHash = sha256(handshakeHash + data)
        }

        fun mixKey(inputKeyMaterial: ByteArray) {
            val output = hkdf(chainingKey, inputKeyMaterial, 2)
            chainingKey = output[0]
            cipherState.initializeKey(output[1])
        }

        fun mixKeyAndHash(inputKeyMaterial: ByteArray) {
            val output = hkdf(chainingKey, inputKeyMaterial, 3)
            chainingKey = output[0]
            mixHash(output[1])
            cipherState.initializeKey(output[2])
        }

        fun encryptAndHash(plaintext: ByteArray): ByteArray {
            val ciphertext = cipherState.encryptWithAd(handshakeHash, plaintext)
            mixHash(ciphertext)
            return ciphertext
        }

        fun decryptAndHash(ciphertext: ByteArray): ByteArray {
            val plaintext = cipherState.decryptWithAd(handshakeHash, ciphertext)
            mixHash(ciphertext)
            return plaintext
        }

        fun split(): Pair<ByteArray, ByteArray> {
            val output = hkdf(chainingKey, NoiseConstants.EMPTY, 2)
            return output[0] to output[1]
        }
    }

    private data class NoiseKeyPair(
        val privateKey: PrivateKey,
        val publicKeyRaw: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NoiseKeyPair

            if (privateKey != other.privateKey) return false
            if (!publicKeyRaw.contentEquals(other.publicKeyRaw)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = privateKey.hashCode()
            result = 31 * result + publicKeyRaw.contentHashCode()
            return result
        }
    }

    companion object {
        private const val X25519_KEY_SIZE = 32

        fun initiator(psk: ByteArray): NoiseHandshakeState =
            NoiseHandshakeState(initiator = true, psk = psk.copyOf())

        fun responder(psk: ByteArray): NoiseHandshakeState =
            NoiseHandshakeState(initiator = false, psk = psk.copyOf())

        private fun protocolNameHash(): ByteArray {
            val protocolName = NoiseConstants.PROTOCOL_NAME.encodeToByteArray()
            if (protocolName.size <= 32) {
                return ByteArray(32).also { protocolName.copyInto(it) }
            }
            return sha256(protocolName)
        }

        private fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance(NoiseConstants.HASH_ALGORITHM).digest(bytes)

        private fun hkdf(chainingKey: ByteArray, inputKeyMaterial: ByteArray, outputs: Int): List<ByteArray> {
            val tempKey = hmac(chainingKey, inputKeyMaterial)
            val result = mutableListOf<ByteArray>()
            var previous = NoiseConstants.EMPTY
            for (index in 1..outputs) {
                previous = hmac(tempKey, previous + byteArrayOf(index.toByte()))
                result += previous
            }
            return result
        }

        private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
            val mac = Mac.getInstance(NoiseConstants.HMAC_ALGORITHM)
            mac.init(SecretKeySpec(key, NoiseConstants.HMAC_ALGORITHM))
            return mac.doFinal(data)
        }

        private fun generateKeyPair(): NoiseKeyPair {
            val generator = KeyPairGenerator.getInstance(NoiseConstants.DH_ALGORITHM)
            val keyPair = generator.generateKeyPair()
            val publicKey = keyPair.public as XECPublicKey
            return NoiseKeyPair(
                privateKey = keyPair.private,
                publicKeyRaw = bigIntegerToLittleEndian(publicKey.u, X25519_KEY_SIZE),
            )
        }

        private fun importPublicKey(rawKey: ByteArray): PublicKey {
            val spec = XECPublicKeySpec(
                NamedParameterSpec(NoiseConstants.DH_ALGORITHM),
                littleEndianToBigInteger(rawKey),
            )
            return KeyFactory.getInstance(NoiseConstants.DH_ALGORITHM).generatePublic(spec)
        }

        private fun littleEndianToBigInteger(bytes: ByteArray): BigInteger {
            val reversed = bytes.reversedArray()
            return BigInteger(1, reversed)
        }

        @Suppress("SameParameterValue")
        private fun bigIntegerToLittleEndian(value: BigInteger, size: Int): ByteArray {
            val bigEndian = value.toByteArray()
            val output = ByteArray(size)
            var outputIndex = 0
            for (index in bigEndian.indices.reversed()) {
                if (outputIndex >= size) break
                output[outputIndex++] = bigEndian[index]
            }
            return output
        }

        private fun List<ByteArray>.concat(): ByteArray {
            val output = ByteArray(sumOf { it.size })
            var offset = 0
            for (bytes in this) {
                bytes.copyInto(output, destinationOffset = offset)
                offset += bytes.size
            }
            return output
        }
    }
}
