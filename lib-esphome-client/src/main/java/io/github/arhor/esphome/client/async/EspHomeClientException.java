package io.github.arhor.esphome.client.async;

public abstract class EspHomeClientException extends RuntimeException {

    protected EspHomeClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    protected EspHomeClientException(final String message) {
        super(message);
    }
}
