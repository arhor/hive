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
import java.util.concurrent.atomic.AtomicReference;

public class NettyEspHomeConnection implements EspHomeConnection {
    public static final Flow.Subscription NO_OP_SUBSCRIPTION = new Flow.Subscription() {
        @Override
        public void request(long n) {}

        @Override
        public void cancel() {}
    };

    enum ConnectionState {
        ACTIVE,
        CLOSING,
        CLOSED,
    }

    private final List<EspHomeSubscription> subscriptions;
    private final Channel channel;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.ACTIVE);

    public NettyEspHomeConnection(
        final List<EspHomeSubscription> subscriptions,
        final Channel channel
    ) {
        this.subscriptions = Objects.requireNonNull(subscriptions);
        this.channel = Objects.requireNonNull(channel);
        channel.closeFuture().addListener(_ -> onChannelClosed());
    }

    @Override
    public CompletableFuture<Void> send(final EspHomeCommand command) {
        if (!acceptsOperations()) {
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
            if (acceptsOperations()) {
                subscribe(channel, subscriptions, subscriber);
            } else {
                subscriber.onSubscribe(NO_OP_SUBSCRIPTION);
                subscriber.onError(new IllegalStateException("Client is not connected"));
            }
        };
    }

    @Override
    public void close() {
        if (beginClosing()) {
            try {
                channel.close().awaitUninterruptibly();
            } finally {
                state.set(ConnectionState.CLOSED);
            }
        }
    }

    /* ------------------------------------------ Internal implementation ------------------------------------------- */

    private boolean acceptsOperations() {
        return state.get() == ConnectionState.ACTIVE && channel.isActive();
    }

    private boolean beginClosing() {
        while (true) {
            final var current = state.get();
            if (current == ConnectionState.CLOSED || current == ConnectionState.CLOSING) {
                return false;
            }
            if (state.compareAndSet(ConnectionState.ACTIVE, ConnectionState.CLOSING)) {
                cleanupSubscriptions();
                return true;
            }
        }
    }

    private void onChannelClosed() {
        beginClosing();
        state.set(ConnectionState.CLOSED);
    }

    private void cleanupSubscriptions() {
        for (EspHomeSubscription sub : subscriptions) {
            sub.cancel();
        }
        subscriptions.clear();
    }

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
