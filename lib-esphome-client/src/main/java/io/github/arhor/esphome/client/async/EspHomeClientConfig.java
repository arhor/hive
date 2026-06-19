package io.github.arhor.esphome.client.async;

import java.time.Duration;

public record EspHomeClientConfig(
    String host,
    int port,
    String clientName,
    String password,
    EncryptionConfig encryption,
    Duration connectTimeout,
    Duration readTimeout,
    int apiVersionMajor,
    int apiVersionMinor
) {
    public static final int API_VERSION_MAJOR = 1;
    public static final int API_VERSION_MINOR = 10;
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    public EspHomeClientConfig(
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
            EncryptionConfig.disabled(),
            DEFAULT_CONNECT_TIMEOUT,
            DEFAULT_READ_TIMEOUT,
            API_VERSION_MAJOR,
            API_VERSION_MINOR
        );
    }

    public EspHomeClientConfig {
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
            encryption = EncryptionConfig.disabled();
        }
    }

    public int connectTimeoutMillis() {
        return Math.toIntExact(connectTimeout.toMillis());
    }

    public int readTimeoutMillis() {
        return Math.toIntExact(readTimeout.toMillis());
    }

    public record EncryptionConfig(
        boolean enabled,
        String key
    ) {
        public EncryptionConfig {
            if (enabled && (key == null || key.isBlank())) {
                throw new IllegalArgumentException("key must be configured when encryption is enabled");
            }
        }

        public static EncryptionConfig disabled() {
            return new EncryptionConfig(false, null);
        }
    }
}
