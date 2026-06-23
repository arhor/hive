package io.github.arhor.esphome.client.async;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EspHomeClientConfigTest {

    @Test
    void encryptionEnabledRequiresKey() {
        assertThrows(IllegalArgumentException.class, () -> new EspHomeClient.Config.Encryption(true, null));
    }

    @Test
    void defaultEncryptionDisabled() {
        final var config = new EspHomeClient.Config("host", 6053, "client", null);
        assertFalse(config.encryption().enabled());
    }

    @Test
    void rejectsZeroConnectTimeout() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EspHomeClient.Config(
                "host",
                6053,
                "client",
                null,
                EspHomeClient.Config.Encryption.disabled(),
                Duration.ZERO,
                Duration.ofSeconds(5),
                EspHomeClient.API_VERSION_MAJOR,
                EspHomeClient.API_VERSION_MINOR
            )
        );
    }
}
