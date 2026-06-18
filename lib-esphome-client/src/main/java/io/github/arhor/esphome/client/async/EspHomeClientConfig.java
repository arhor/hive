package io.github.arhor.esphome.client.async;

public record EspHomeClientConfig(
    String host,
    int port,
    String clientName,
    String password,
    int apiVersionMajor,
    int apiVersionMinor
) {
    public static final int API_VERSION_MAJOR = 1;
    public static final int API_VERSION_MINOR = 10;

    public EspHomeClientConfig(
        String host,
        int port,
        String clientName,
        String password
    ) {
        this(host, port, clientName, password, API_VERSION_MAJOR, API_VERSION_MINOR);
    }
}
