package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.EspHomeClient;
import io.github.arhor.esphome.client.async.EspHomeConnection;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufDecoder;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufEncoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedFrameDecoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedFrameEncoder;
import io.github.arhor.esphome.client.async.internal.codec.plaintext.EspHomeVarIntFrameDecoder;
import io.github.arhor.esphome.client.async.internal.codec.plaintext.EspHomeVarIntFrameEncoder;
import io.github.arhor.esphome.client.async.internal.noise.NoiseKeyMaterial;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class NettyEspHomeClient implements EspHomeClient {

    private static final Logger log = Logger.getLogger(NettyEspHomeClient.class.getName());

    enum ClientState {
        ACTIVE,
        CLOSED,
    }

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Config config;
    private final AtomicReference<ClientState> state = new AtomicReference<>(ClientState.ACTIVE);

    public NettyEspHomeClient(final Config config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<EspHomeConnection> connect() {
        if (state.get() != ClientState.ACTIVE) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client is closed"));
        }

        log.fine(() -> "Connecting to " + config.host() + ":" + config.port()
            + " (connectTimeout=" + config.connectTimeout() + ", readTimeout=" + config.readTimeout()
            + ", encrypted=" + config.encryption().enabled() + ")");

        var bootstrap = new Bootstrap();
        var subscriptions = new CopyOnWriteArrayList<EspHomeSubscription>();
        var resultFuture = new CompletableFuture<EspHomeConnection>();

        bootstrap
            .group(workerGroup)
            .channelFactory(NioSocketChannel::new)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(config.connectTimeout().toMillis()))
            .handler(createChannelInitializer(subscriptions, resultFuture))
            .connect(config.host(), config.port())
            .addListener((future) -> {
                if (future.isSuccess()) {
                    log.fine(() -> "TCP connection established to " + config.host() + ":" + config.port());
                } else {
                    log.warning(() -> "TCP connection failed: " + future.cause());
                    resultFuture.completeExceptionally(future.cause());
                }
            });

        return resultFuture;
    }

    @Override
    public void close() {
        if (state.compareAndSet(ClientState.ACTIVE, ClientState.CLOSED)) {
            log.fine("Closing NettyEspHomeClient, shutting down worker group");
            workerGroup.shutdownGracefully();
        }
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

        pipeline.addLast("readTimeout", new ReadTimeoutHandler(Math.toIntExact(config.readTimeout().toMillis()), TimeUnit.MILLISECONDS));
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

        pipeline.addLast("readTimeout", new ReadTimeoutHandler(Math.toIntExact(config.readTimeout().toMillis()), TimeUnit.MILLISECONDS));
        pipeline.addLast("encryptedFrameDecoder", new EspHomeEncryptedFrameDecoder());
        pipeline.addLast("encryptedFrameEncoder", new EspHomeEncryptedFrameEncoder());
        pipeline.addLast("noiseHandshakeHandler", new EspHomeNoiseHandshakeHandler(psk, config, subscriptions, resultFuture));
    }
}
