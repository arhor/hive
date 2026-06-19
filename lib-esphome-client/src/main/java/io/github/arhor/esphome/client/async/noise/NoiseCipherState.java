package io.github.arhor.esphome.client.async.noise;

import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public final class NoiseCipherState {

    private byte[] key;
    private long nonce;

    public void initializeKey(final byte[] newKey) {
        this.key = newKey == null ? null : newKey.clone();
        nonce = 0;
    }

    public boolean hasKey() {
        return key != null;
    }

    public byte[] encryptWithAd(final byte[] associatedData, final byte[] plaintext) {
        if (key == null) {
            return plaintext;
        }
        if (plaintext.length + NoiseConstants.AUTH_TAG_LENGTH > NoiseConstants.NOISE_MAX_MESSAGE_LENGTH) {
            throw new EspHomeProtocolException("Noise plaintext is too large: " + plaintext.length + " bytes");
        }
        return runCipher(Cipher.ENCRYPT_MODE, associatedData, plaintext);
    }

    public byte[] decryptWithAd(final byte[] associatedData, final byte[] ciphertext) {
        if (key == null) {
            return ciphertext;
        }
        if (ciphertext.length < NoiseConstants.AUTH_TAG_LENGTH) {
            throw new EspHomeProtocolException("Noise ciphertext is too short");
        }
        return runCipher(Cipher.DECRYPT_MODE, associatedData, ciphertext);
    }

    private byte[] runCipher(final int mode, final byte[] associatedData, final byte[] input) {
        try {
            final var cipher = Cipher.getInstance(NoiseConstants.CIPHER_ALGORITHM);
            cipher.init(
                mode,
                new SecretKeySpec(key, "ChaCha20"),
                new IvParameterSpec(nonceBytes())
            );
            cipher.updateAAD(associatedData);
            final var output = cipher.doFinal(input);
            nonce += 1;
            return output;
        } catch (GeneralSecurityException exception) {
            throw new EspHomeProtocolException("Noise cipher operation failed", exception);
        }
    }

    private byte[] nonceBytes() {
        final var bytes = new byte[12];
        var remaining = nonce;
        for (var index = 4; index < 12; index++) {
            bytes[index] = (byte) (remaining & 0xffL);
            remaining >>>= 8;
        }
        return bytes;
    }
}
