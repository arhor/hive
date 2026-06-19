package io.github.arhor.esphome.client.async.exception;

public class EspHomeTransportException extends EspHomeClientException {

    public EspHomeTransportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EspHomeTransportException(final String message) {
        super(message);
    }
}
