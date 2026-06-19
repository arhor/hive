package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.EspHomeClientConfig;
import io.github.arhor.esphome.client.async.EspHomeConnection;
import io.github.arhor.esphome.client.async.exception.EspHomeAuthenticationException;
import io.github.arhor.esphome.client.async.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.proto.ConnectRequest;
import io.github.arhor.esphome.client.proto.ConnectResponse;
import io.github.arhor.esphome.client.proto.HelloRequest;
import io.github.arhor.esphome.client.proto.HelloResponse;
import io.github.arhor.esphome.client.proto.PingRequest;
import io.github.arhor.esphome.client.proto.PingResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class EspHomeHandshakeHandler extends SimpleChannelInboundHandler<Object> {

    private enum Step {
        WAITING_FOR_HELLO,
        WAITING_FOR_CONNECT,
        DONE
    }

    private final CompletableFuture<EspHomeConnection> completion;
    private final List<EspHomeSubscription> subscriptions;
    private final EspHomeClientConfig config;

    private Step step = Step.WAITING_FOR_HELLO;

    public EspHomeHandshakeHandler(
        final CompletableFuture<EspHomeConnection> completion,
        final List<EspHomeSubscription> subscriptions,
        final EspHomeClientConfig config
    ) {
        this.completion = completion;
        this.subscriptions = subscriptions;
        this.config = config;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            ctx.executor().execute(() -> sendHelloRequest(ctx));
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        sendHelloRequest(ctx);
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
        switch (msg) {
            case PingRequest _ -> onPingRequest(ctx);
            case HelloResponse res when step == Step.WAITING_FOR_HELLO -> onHelloResponse(ctx, res);
            case ConnectResponse res when step == Step.WAITING_FOR_CONNECT -> onConnectResponse(ctx, res);
            case null, default -> onUnknownState(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        fail(ctx, cause);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (!completion.isDone()) {
            completion.completeExceptionally(new EspHomeProtocolException("Connection closed during ESPHome handshake"));
        }
        ctx.fireChannelInactive();
    }

    /* ------------------------------------------ Internal implementation ------------------------------------------- */

    private void sendHelloRequest(final ChannelHandlerContext ctx) {
        if (step != Step.WAITING_FOR_HELLO) {
            return;
        }
        ctx.channel().writeAndFlush(
            HelloRequest.newBuilder()
                .setClientInfo(config.clientName())
                .setApiVersionMajor(config.apiVersionMajor())
                .setApiVersionMinor(config.apiVersionMinor())
                .build()
        );
    }

    private void onPingRequest(final ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(PingResponse.getDefaultInstance());
    }

    private void onHelloResponse(final ChannelHandlerContext ctx, final HelloResponse hello) {
        final var deviceApiVersion = hello.getApiVersionMajor();

        if (deviceApiVersion != config.apiVersionMajor()) {
            fail(ctx, new EspHomeProtocolException("Unsupported ESPHome API major version: " + deviceApiVersion));
            return;
        }

        step = Step.WAITING_FOR_CONNECT;

        ctx.channel().writeAndFlush(
            (config.password() == null)
                ? ConnectRequest.getDefaultInstance()
                : ConnectRequest.newBuilder().setPassword(config.password()).build()
        );
    }

    private void onConnectResponse(final ChannelHandlerContext ctx, final ConnectResponse connect) {
        if (connect.getInvalidPassword()) {
            fail(ctx, new EspHomeAuthenticationException("ESPHome device rejected the configured password"));
            return;
        }

        step = Step.DONE;

        var channel = ctx.channel();
        var connection = new NettyEspHomeConnection(subscriptions, channel);

        // 3. Central handler that routes incoming data into the reactive stream
        // We pass the list of active subscriptions so the handler knows where to push events
        ctx.pipeline().replace(this, "eventHandler", new NettyEspHomeEventHandler(subscriptions));

        completion.complete(connection);
    }

    private void onUnknownState(final ChannelHandlerContext ctx, final Object msg) {
        final var msgClass = msg != null ? msg.getClass().getName() : null;
        final var errorText = "Unexpected ESPHome message during handshake: " + msgClass + ", step: " + step;
        final var exception = new EspHomeProtocolException(errorText);

        fail(ctx, exception);
    }

    private void fail(final ChannelHandlerContext ctx, final Throwable error) {
        completion.completeExceptionally(error);
        ctx.close();
    }
}
