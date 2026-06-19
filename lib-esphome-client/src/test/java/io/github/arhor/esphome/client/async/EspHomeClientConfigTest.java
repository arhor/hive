package io.github.arhor.esphome.client.async;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EspHomeClientConfigTest {

    @Test
    void encryptionEnabledRequiresKey() {
        assertThrows(IllegalArgumentException.class, () -> new EspHomeClientConfig.EncryptionConfig(true, null));
    }

    @Test
    void defaultEncryptionDisabled() {
        final var config = new EspHomeClientConfig("host", 6053, "client", null);
        assertFalse(config.encryption().enabled());
    }

    @Test
    void rejectsZeroConnectTimeout() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EspHomeClientConfig(
                "host",
                6053,
                "client",
                null,
                EspHomeClientConfig.EncryptionConfig.disabled(),
                Duration.ZERO,
                Duration.ofSeconds(5),
                EspHomeClientConfig.API_VERSION_MAJOR,
                EspHomeClientConfig.API_VERSION_MINOR
            )
        );
    }
}
