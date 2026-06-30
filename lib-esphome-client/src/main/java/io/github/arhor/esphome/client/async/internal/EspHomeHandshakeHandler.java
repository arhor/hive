package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.EspHomeClient;
import io.github.arhor.esphome.client.async.EspHomeConnection;
import io.github.arhor.esphome.client.async.internal.exception.EspHomeAuthenticationException;
import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.proto.ConnectRequest;
import io.github.arhor.esphome.client.proto.ConnectResponse;
import io.github.arhor.esphome.client.proto.GetTimeRequest;
import io.github.arhor.esphome.client.proto.GetTimeResponse;
import io.github.arhor.esphome.client.proto.HelloRequest;
import io.github.arhor.esphome.client.proto.HelloResponse;
import io.github.arhor.esphome.client.proto.PingRequest;
import io.github.arhor.esphome.client.proto.PingResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class EspHomeHandshakeHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = Logger.getLogger(EspHomeHandshakeHandler.class.getName());

    // ESPHome 2026.1.0 removed ConnectRequest/ConnectResponse — device authenticates
    // immediately after HelloResponse. API minor version 14 is the threshold.
    private static final int API_MINOR_NO_CONNECT_REQUEST = 14;

    private enum Step {
        WAITING_FOR_HELLO,
        WAITING_FOR_CONNECT,
        DONE
    }

    private final CompletableFuture<EspHomeConnection> completion;
    private final List<EspHomeSubscription> subscriptions;
    private final EspHomeClient.Config config;

    private Step step = Step.WAITING_FOR_HELLO;

    public EspHomeHandshakeHandler(
        final CompletableFuture<EspHomeConnection> completion,
        final List<EspHomeSubscription> subscriptions,
        final EspHomeClient.Config config
    ) {
        this.completion = completion;
        this.subscriptions = subscriptions;
        this.config = config;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        log.fine(() -> "HandshakeHandler added to pipeline, channel active=" + ctx.channel().isActive());
        if (ctx.channel().isActive()) {
            ctx.executor().execute(() -> sendHelloRequest(ctx));
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log.fine(() -> "Channel active: " + ctx.channel().remoteAddress());
        sendHelloRequest(ctx);
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
        log.fine(() -> "Handshake received: " + msg.getClass().getSimpleName() + " (step=" + step + ")");
        switch (msg) {
            case PingRequest _ -> onPingRequest(ctx);
            case GetTimeRequest _ -> onGetTimeRequest(ctx);
            case HelloResponse res when step == Step.WAITING_FOR_HELLO -> onHelloResponse(ctx, res);
            case ConnectResponse res when step == Step.WAITING_FOR_CONNECT -> onConnectResponse(ctx, res);
            case null, default -> onUnknownState(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.warning(() -> "Exception during handshake (step=" + step + "): " + cause);
        fail(ctx, cause);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        log.fine(() -> "Channel went inactive during handshake (step=" + step + ", done=" + completion.isDone() + ")");
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
        log.fine(() -> "Sending HelloRequest (clientName=" + config.clientName()
            + ", api=" + config.apiVersionMajor() + "." + config.apiVersionMinor() + ")");
        ctx.channel().writeAndFlush(
            HelloRequest.newBuilder()
                .setClientInfo(config.clientName())
                .setApiVersionMajor(config.apiVersionMajor())
                .setApiVersionMinor(config.apiVersionMinor())
                .build()
        );
    }

    private void onPingRequest(final ChannelHandlerContext ctx) {
        log.fine("Received PingRequest, sending PingResponse");
        ctx.channel().writeAndFlush(PingResponse.getDefaultInstance());
    }

    private void onGetTimeRequest(final ChannelHandlerContext ctx) {
        final int epochSeconds = (int) (System.currentTimeMillis() / 1000);
        log.fine(() -> "Received GetTimeRequest, responding with epoch=" + epochSeconds);
        ctx.channel().writeAndFlush(
            GetTimeResponse.newBuilder().setEpochSeconds(epochSeconds).build()
        );
    }

    private void onHelloResponse(final ChannelHandlerContext ctx, final HelloResponse hello) {
        final int serverMajor = hello.getApiVersionMajor();
        final int serverMinor = hello.getApiVersionMinor();
        log.fine(() -> "Received HelloResponse (serverApi=%d.%d, name=%s)".formatted(serverMajor, serverMinor, hello.getName()));

        if (serverMajor != config.apiVersionMajor()) {
            log.warning(() -> "API major version mismatch: expected " + config.apiVersionMajor() + ", got " + serverMajor);
            fail(ctx, new EspHomeProtocolException("Unsupported ESPHome API major version: " + serverMajor));
            return;
        }

        if (serverMinor >= API_MINOR_NO_CONNECT_REQUEST) {
            // ESPHome 2026.1.0+: device is already authenticated after HelloResponse,
            // ConnectRequest/ConnectResponse no longer exist.
            if (config.password() != null) {
                log.warning("Password configured but server API >= 1.14 does not support password auth — ignoring");
            }
            log.fine("Server API >= 1.14: completing handshake directly after HelloResponse");
            completeHandshake(ctx);
        } else {
            step = Step.WAITING_FOR_CONNECT;
            log.fine("Server API < 1.14: sending ConnectRequest");
            ctx.channel().writeAndFlush(
                (config.password() == null)
                    ? ConnectRequest.getDefaultInstance()
                    : ConnectRequest.newBuilder().setPassword(config.password()).build()
            );
        }
    }

    private void onConnectResponse(final ChannelHandlerContext ctx, final ConnectResponse connect) {
        log.fine(() -> "Received ConnectResponse (invalidPassword=" + connect.getInvalidPassword() + ")");
        if (connect.getInvalidPassword()) {
            fail(ctx, new EspHomeAuthenticationException("ESPHome device rejected the configured password"));
            return;
        }
        completeHandshake(ctx);
    }

    private void completeHandshake(final ChannelHandlerContext ctx) {
        step = Step.DONE;
        log.fine("Handshake complete, promoting to event handler");

        final var channel = ctx.channel();
        final var connection = new NettyEspHomeConnection(subscriptions, channel);

        ctx.pipeline().addBefore(ctx.name(), "gracefulDisconnect", new EspHomeGracefulDisconnectHandler());
        ctx.pipeline().replace(this, "eventHandler", new NettyEspHomeEventHandler(subscriptions));
        completion.complete(connection);
    }

    private void onUnknownState(final ChannelHandlerContext ctx, final Object msg) {
        final var msgClass = msg != null ? msg.getClass().getName() : null;
        final var errorText = "Unexpected ESPHome message during handshake: " + msgClass + ", step: " + step;
        log.warning(errorText);
        final var exception = new EspHomeProtocolException(errorText);

        fail(ctx, exception);
    }

    private void fail(final ChannelHandlerContext ctx, final Throwable error) {
        log.warning(() -> "Handshake failed: " + error);
        completion.completeExceptionally(error);
        ctx.close();
    }
}
