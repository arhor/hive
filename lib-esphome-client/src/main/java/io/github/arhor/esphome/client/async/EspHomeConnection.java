package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.github.arhor.esphome.client.async.model.EspHomeMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public interface EspHomeConnection extends AutoCloseable {

    CompletableFuture<Void> send(EspHomeMessage command);

    Flow.Publisher<EspHomeEvent> observeEvents();
}
