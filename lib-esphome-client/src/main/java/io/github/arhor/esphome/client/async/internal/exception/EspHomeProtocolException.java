package io.github.arhor.esphome.client.async.internal.exception;

public class EspHomeProtocolException extends EspHomeClientException {
    public EspHomeProtocolException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EspHomeProtocolException(final String message) {
        super(message);
    }
}
