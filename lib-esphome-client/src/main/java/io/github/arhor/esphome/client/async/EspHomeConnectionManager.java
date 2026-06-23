package io.github.arhor.esphome.client.async;

import java.util.concurrent.CompletableFuture;

public interface EspHomeConnectionManager extends AutoCloseable {

    CompletableFuture<EspHomeConnection> connect();
}
