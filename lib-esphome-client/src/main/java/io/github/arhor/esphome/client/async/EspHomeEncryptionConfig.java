package io.github.arhor.esphome.client.async;

public record EspHomeEncryptionConfig(
    boolean enabled,
    String key
) {
    public EspHomeEncryptionConfig {
        if (enabled && (key == null || key.isBlank())) {
            throw new IllegalArgumentException("key must be configured when encryption is enabled");
        }
    }

    public static EspHomeEncryptionConfig disabled() {
        return new EspHomeEncryptionConfig(false, null);
    }
}
