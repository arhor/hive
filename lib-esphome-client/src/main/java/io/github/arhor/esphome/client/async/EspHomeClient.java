package io.github.arhor.esphome.client.async;

import java.util.concurrent.CompletableFuture;

public interface EspHomeClient extends AutoCloseable {

    CompletableFuture<EspHomeConnection> connect();
}
