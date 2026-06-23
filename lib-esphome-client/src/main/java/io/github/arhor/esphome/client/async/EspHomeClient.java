package io.github.arhor.esphome.client.async;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface EspHomeClient extends AutoCloseable {

    int API_VERSION_MAJOR = 1;
    int API_VERSION_MINOR = 10;
    Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    CompletableFuture<EspHomeConnection> connect();

    record Config(
        String host,
        int port,
        String clientName,
        String password,
        Encryption encryption,
        Duration connectTimeout,
        Duration readTimeout,
        int apiVersionMajor,
        int apiVersionMinor
    ) {
        public Config(
            final String host,
            final int port,
            final String clientName,
            final String password
        ) {
            this(
                host,
                port,
                clientName,
                password,
                Encryption.disabled(),
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_READ_TIMEOUT,
                API_VERSION_MAJOR,
                API_VERSION_MINOR
            );
        }

        public Config {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("host must not be blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("port must be between 1 and 65535");
            }
            if (connectTimeout.isZero() || connectTimeout.isNegative()) {
                throw new IllegalArgumentException("connectTimeout must be positive");
            }
            if (readTimeout.isZero() || readTimeout.isNegative()) {
                throw new IllegalArgumentException("readTimeout must be positive");
            }
            if (encryption == null) {
                encryption = Encryption.disabled();
            }
        }

        public record Encryption(
            boolean enabled,
            String key
        ) {
            public Encryption {
                if (enabled && (key == null || key.isBlank())) {
                    throw new IllegalArgumentException("key must be configured when encryption is enabled");
                }
            }

            public static Encryption disabled() {
                return new Encryption(false, null);
            }
        }
    }
}
