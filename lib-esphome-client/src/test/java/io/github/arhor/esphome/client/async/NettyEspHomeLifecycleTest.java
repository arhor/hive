package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.internal.NettyEspHomeClient;
import io.github.arhor.esphome.client.async.internal.NettyEspHomeConnection;
import io.github.arhor.esphome.client.async.model.EspHomeCommand;
import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyEspHomeLifecycleTest {

    @Test
    void connectFailsAfterClientClose() {
        final var config = new EspHomeClient.Config("127.0.0.1", 1, "test-client", null);
        final var client = new NettyEspHomeClient(config);

        client.close();

        final var connectFuture = client.connect();
        final var error = assertThrows(ExecutionException.class, connectFuture::get);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Client is closed", error.getCause().getMessage());
    }

    @Test
    void sendFailsAfterConnectionClose() {
        final var channel = new EmbeddedChannel();
        final var connection = new NettyEspHomeConnection(new CopyOnWriteArrayList<>(), channel);

        connection.close();

        final var sendFuture = connection.send(new EspHomeCommand.GetDeviceInfo());
        final var error = assertThrows(ExecutionException.class, sendFuture::get);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Client is not connected", error.getCause().getMessage());
    }

    @Test
    void observeEventsFailsAfterConnectionClose() {
        final var channel = new EmbeddedChannel();
        final var connection = new NettyEspHomeConnection(new CopyOnWriteArrayList<>(), channel);
        final var subscriberFailed = new AtomicBoolean();

        connection.close();

        connection.observeEvents().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {}

            @Override
            public void onNext(final EspHomeEvent item) {}

            @Override
            public void onError(final Throwable throwable) {
                subscriberFailed.set(true);
                assertInstanceOf(IllegalStateException.class, throwable);
                assertEquals("Client is not connected", throwable.getMessage());
            }

            @Override
            public void onComplete() {}
        });

        assertTrue(subscriberFailed.get());
    }

    @Test
    void remoteChannelCloseRejectsNewOperations() {
        final var channel = new EmbeddedChannel();
        final var connection = new NettyEspHomeConnection(new CopyOnWriteArrayList<>(), channel);

        channel.close();

        final var sendFuture = connection.send(new EspHomeCommand.GetDeviceInfo());
        final var error = assertThrows(ExecutionException.class, sendFuture::get);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Client is not connected", error.getCause().getMessage());
    }

    @Test
    void repeatedCloseIsIdempotent() {
        final var channel = new EmbeddedChannel();
        final var connection = new NettyEspHomeConnection(new CopyOnWriteArrayList<>(), channel);

        connection.close();
        connection.close();

        assertTrue(channel.closeFuture().isDone());
    }

    @Test
    void repeatedClientCloseIsIdempotent() {
        final var config = new EspHomeClient.Config(
            "127.0.0.1",
            1,
            "test-client",
            null,
            EspHomeClient.Config.Encryption.disabled(),
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            EspHomeClient.API_VERSION_MAJOR,
            EspHomeClient.API_VERSION_MINOR
        );
        final var client = new NettyEspHomeClient(config);

        client.close();
        client.close();

        final var connectFuture = client.connect();
        final var error = assertThrows(ExecutionException.class, connectFuture::get);
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }
}
