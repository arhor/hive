package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.codec.encrypted.EspHomeEncryptedFrameDecoder;
import io.github.arhor.esphome.client.async.codec.encrypted.EspHomeEncryptedFrameEncoder;
import io.github.arhor.esphome.client.async.codec.plaintext.EspHomeVarIntFrameDecoder;
import io.github.arhor.esphome.client.async.codec.plaintext.EspHomeVarIntFrameEncoder;
import io.github.arhor.esphome.client.async.noise.NoiseKeyMaterial;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class NettyEspHomeClient implements EspHomeClient {

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final EspHomeClientConfig config;
    private volatile boolean isClosed = false;

    public NettyEspHomeClient(final EspHomeClientConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<EspHomeConnection> connect() {
        var bootstrap = new Bootstrap();
        var subscriptions = new CopyOnWriteArrayList<EspHomeSubscription>();
        var resultFuture = new CompletableFuture<EspHomeConnection>();

        bootstrap
            .group(workerGroup)
            .channelFactory(NioSocketChannel::new)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeoutMillis())
            .handler(createChannelInitializer(subscriptions, resultFuture))
            .connect(config.host(), config.port())
            .addListener((future) -> {
                if (!future.isSuccess()) {
                    resultFuture.completeExceptionally(future.cause());
                }
            });

        return resultFuture;
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        workerGroup.shutdownGracefully();
    }

    private ChannelInitializer<Channel> createChannelInitializer(
        final List<EspHomeSubscription> subscriptions,
        final CompletableFuture<EspHomeConnection> resultFuture
    ) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final Channel ch) {
                if (config.encryption().enabled()) {
                    initEncryptedChannel(ch, subscriptions, resultFuture);
                } else {
                    initPlaintextChannel(ch, subscriptions, resultFuture);
                }
            }
        };
    }

    private void initPlaintextChannel(
        final Channel ch,
        final List<EspHomeSubscription> subscriptions,
        final CompletableFuture<EspHomeConnection> resultFuture
    ) {
        var pipeline = ch.pipeline();

        pipeline.addLast("readTimeout", new ReadTimeoutHandler(config.readTimeoutMillis(), TimeUnit.MILLISECONDS));
        pipeline.addLast("frameDecoder", new EspHomeVarIntFrameDecoder());
        pipeline.addLast("protobufDecoder", new EspHomeProtobufDecoder());
        pipeline.addLast("frameEncoder", new EspHomeVarIntFrameEncoder());
        pipeline.addLast("protobufEncoder", new EspHomeProtobufEncoder());
        pipeline.addLast("handshakeHandler", new EspHomeHandshakeHandler(resultFuture, subscriptions, config));
    }

    private void initEncryptedChannel(
        final Channel ch,
        final List<EspHomeSubscription> subscriptions,
        final CompletableFuture<EspHomeConnection> resultFuture
    ) {
        final var psk = NoiseKeyMaterial.decodeBase64(config.encryption().key());
        final var pipeline = ch.pipeline();

        pipeline.addLast("readTimeout", new ReadTimeoutHandler(config.readTimeoutMillis(), TimeUnit.MILLISECONDS));
        pipeline.addLast("encryptedFrameDecoder", new EspHomeEncryptedFrameDecoder());
        pipeline.addLast("encryptedFrameEncoder", new EspHomeEncryptedFrameEncoder());
        pipeline.addLast("noiseHandshakeHandler", new EspHomeNoiseHandshakeHandler(psk, config, subscriptions, resultFuture));
    }
}
