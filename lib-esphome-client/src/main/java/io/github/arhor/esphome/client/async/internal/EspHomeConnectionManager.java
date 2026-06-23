package io.github.arhor.esphome.client.async.internal;

import java.util.concurrent.CompletableFuture;

public interface EspHomeConnectionManager extends AutoCloseable {

    CompletableFuture<EspHomeConnection> connect();
}
