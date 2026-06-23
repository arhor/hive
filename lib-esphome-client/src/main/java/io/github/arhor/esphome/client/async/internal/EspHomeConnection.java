package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.model.EspHomeCommand;
import io.github.arhor.esphome.client.async.model.EspHomeEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public interface EspHomeConnection extends AutoCloseable {

    CompletableFuture<Void> send(EspHomeCommand command);

    Flow.Publisher<EspHomeEvent> observeEvents();
}
