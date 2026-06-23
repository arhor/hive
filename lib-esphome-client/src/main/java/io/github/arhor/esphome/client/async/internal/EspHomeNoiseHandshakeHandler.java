package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.EspHomeClient;
import io.github.arhor.esphome.client.async.EspHomeConnection;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufDecoder;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufEncoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedPayloadDecoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedPayloadEncoder;
import io.github.arhor.esphome.client.async.internal.exception.EspHomeProtocolException;
import io.github.arhor.esphome.client.async.internal.noise.NoiseHandshakeState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class EspHomeNoiseHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private enum Step {
        SEND_INIT,
        WAIT_SERVER_HELLO,
        SEND_CLIENT_HANDSHAKE,
        WAIT_SERVER_HANDSHAKE
    }

    private final NoiseHandshakeState handshake;
    private final EspHomeClient.Config config;
    private final List<EspHomeSubscription> subscriptions;
    private final CompletableFuture<EspHomeConnection> connectionFuture;

    private Step step = Step.SEND_INIT;
    private boolean initSent;

    public EspHomeNoiseHandshakeHandler(
        final byte[] psk,
        final EspHomeClient.Config config,
        final List<EspHomeSubscription> subscriptions,
        final CompletableFuture<EspHomeConnection> connectionFuture
    ) {
        this.handshake = NoiseHandshakeState.initiator(psk);
        this.config = config;
        this.subscriptions = subscriptions;
        this.connectionFuture = connectionFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            ensureInitSent(ctx);
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        ensureInitSent(ctx);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
        switch (step) {
            case WAIT_SERVER_HELLO -> onServerHello(ctx, msg);
            case WAIT_SERVER_HANDSHAKE -> onServerHandshake(ctx, msg);
            default -> fail(ctx, new EspHomeProtocolException(
                "Unexpected ESPHome encrypted frame during Noise handshake: step=" + step
            ));
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        fail(ctx, cause);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(
                new EspHomeProtocolException("Connection closed during ESPHome Noise handshake")
            );
        }
        ctx.fireChannelInactive();
    }

    private void ensureInitSent(final ChannelHandlerContext ctx) {
        if (initSent || step != Step.SEND_INIT) {
            return;
        }
        initSent = true;
        ctx.writeAndFlush(ctx.alloc().buffer(0));
        step = Step.WAIT_SERVER_HELLO;
    }

    private void onServerHello(final ChannelHandlerContext ctx, final ByteBuf msg) {
        if (msg.readableBytes() == 0 || msg.getByte(msg.readerIndex()) != 0x01) {
            fail(ctx, new EspHomeProtocolException(
                "ESPHome encrypted server hello selected an unsupported protocol"
            ));
            return;
        }

        step = Step.WAIT_SERVER_HANDSHAKE;
        sendClientHandshake(ctx);
    }

    private void sendClientHandshake(final ChannelHandlerContext ctx) {
        final var noiseMessage = handshake.writeMessage();
        final var payload = ctx.alloc().buffer(1 + noiseMessage.length);
        payload.writeByte(0x00);
        payload.writeBytes(noiseMessage);
        ctx.writeAndFlush(payload);
    }

    private void onServerHandshake(final ChannelHandlerContext ctx, final ByteBuf msg) {
        if (msg.readableBytes() == 0) {
            fail(ctx, new EspHomeProtocolException("ESPHome encrypted handshake response was empty"));
            return;
        }

        final var first = msg.getByte(msg.readerIndex());
        if (first == 0x01) {
            final var reasonBytes = ByteBufUtil.getBytes(msg, msg.readerIndex() + 1, msg.readableBytes() - 1);
            fail(ctx, new EspHomeProtocolException(
                "ESPHome encrypted handshake was rejected: " + new String(reasonBytes, StandardCharsets.UTF_8)
            ));
            return;
        }
        if (first != 0x00) {
            fail(ctx, new EspHomeProtocolException("ESPHome encrypted handshake response had invalid prefix"));
            return;
        }

        final var noiseBytes = ByteBufUtil.getBytes(msg, msg.readerIndex() + 1, msg.readableBytes() - 1);
        handshake.readMessage(noiseBytes);
        completeHandshake(ctx);
    }

    private void completeHandshake(final ChannelHandlerContext ctx) {
        final var channel = ctx.channel();

        channel.attr(EspHomeChannelAttributes.SEND_CIPHER).set(handshake.getSendCipher());
        channel.attr(EspHomeChannelAttributes.RECEIVE_CIPHER).set(handshake.getReceiveCipher());

        final var pipeline = ctx.pipeline();

        pipeline.addAfter("encryptedFrameDecoder", "encryptedPayloadDecoder", new EspHomeEncryptedPayloadDecoder());
        pipeline.addAfter("encryptedPayloadDecoder", "protobufDecoder", new EspHomeProtobufDecoder());
        pipeline.addBefore("noiseHandshakeHandler", "handshakeHandler", new EspHomeHandshakeHandler(connectionFuture, subscriptions, config));
        pipeline.addBefore("handshakeHandler", "protobufEncoder", new EspHomeProtobufEncoder());
        pipeline.addBefore("protobufEncoder", "encryptedPayloadEncoder", new EspHomeEncryptedPayloadEncoder());
        pipeline.remove(this);
    }

    private void fail(final ChannelHandlerContext ctx, final Throwable error) {
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(error);
        }
        ctx.close();
    }
}
