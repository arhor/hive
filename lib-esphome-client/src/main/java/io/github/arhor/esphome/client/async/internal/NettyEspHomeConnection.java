package io.github.arhor.esphome.client.async.internal;

import com.google.protobuf.MessageLite;
import io.github.arhor.esphome.client.async.EspHomeCommand;
import io.github.arhor.esphome.client.async.EspHomeConnection;
import io.github.arhor.esphome.client.async.EspHomeEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class NettyEspHomeConnection implements EspHomeConnection {
    public static final Flow.Subscription NO_OP_SUBSCRIPTION = new Flow.Subscription() {
        @Override
        public void request(long n) {}

        @Override
        public void cancel() {}
    };

    private final List<EspHomeSubscription> subscriptions;
    private final Channel channel;

    private volatile boolean isClosed = false;

    public NettyEspHomeConnection(
        final List<EspHomeSubscription> subscriptions,
        final Channel channel
    ) {
        this.subscriptions = Objects.requireNonNull(subscriptions);
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public CompletableFuture<Void> send(final EspHomeCommand command) {
        if (isClosed || !channel.isActive()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client is not connected"));
        }
        CompletableFuture<Void> commandFuture = new CompletableFuture<>();

        try {
            // Transform the domain command into a Protobuf DTO
            MessageLite protobufDto = EspHomeCommandMapper.map(command);

            // Write to the Netty channel asynchronously
            channel.writeAndFlush(protobufDto).addListener((ChannelFuture future) -> {
                if (future.isSuccess()) {
                    commandFuture.complete(null);
                } else {
                    commandFuture.completeExceptionally(future.cause());
                }
            });
        } catch (Throwable t) {
            // Propagate mapping errors or other unexpected exceptions
            commandFuture.completeExceptionally(t);
        }

        return commandFuture;
    }

    @Override
    public Flow.Publisher<EspHomeEvent> observeEvents() {
        return (subscriber) -> {
            if (isClosed || !channel.isActive()) {
                subscriber.onSubscribe(NO_OP_SUBSCRIPTION);
                subscriber.onError(new IllegalStateException("Client is not connected"));
                return;
            }
            subscribe(channel, subscriptions, subscriber);
        };
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;

        // Cancel all active subscriptions
        for (EspHomeSubscription sub : subscriptions) {
            sub.cancel();
        }
        subscriptions.clear();

        // Close the network channel
        channel
            .close()
            .awaitUninterruptibly();
    }

    /* ------------------------------------------ Internal implementation ------------------------------------------- */

    private void subscribe(
        final Channel channel,
        final List<EspHomeSubscription> subscriptions,
        final Flow.Subscriber<? super EspHomeEvent> subscriber
    ) {
        var subscription = new EspHomeSubscription(subscriber, channel);

        subscriptions.add(subscription);
        subscriber.onSubscribe(subscription);
    }
}
