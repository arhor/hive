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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class NettyEspHomeClient implements EspHomeClient {

    private static final Logger log = Logger.getLogger(NettyEspHomeClient.class.getName());

    enum ClientState {
        ACTIVE,
        /**
         * Draining active connections before shutting down the worker group.
         * New connect() calls are rejected in this state.
         *
         * Future: connect() calls that already passed the ACTIVE check before
         * CLOSING was set will still complete and register into activeConnections
         * after the drain loop finishes, missing graceful shutdown. Resolving
         * this fully requires tracking in-flight (TCP-connecting, not-yet-
         * established) connections — e.g. a set of pending CompletableFutures —
         * so close() can cancel or await them before proceeding to CLOSED.
         */
        CLOSING,
        CLOSED,
    }

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Set<NettyEspHomeConnection> activeConnections = ConcurrentHashMap.newKeySet();
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

        resultFuture.whenComplete((connection, _) -> {
            if (connection instanceof NettyEspHomeConnection nettyConnection) {
                activeConnections.add(nettyConnection);
                nettyConnection
                    .channel()
                    .closeFuture()
                    .addListener(_ -> activeConnections.remove(nettyConnection));
            }
        });

        return resultFuture;
    }

    @Override
    public void close() {
        if (state.compareAndSet(ClientState.ACTIVE, ClientState.CLOSING)) {
            log.fine(() -> "Closing NettyEspHomeClient: draining " + activeConnections.size() + " active connection(s)");
            for (var connection : activeConnections) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warning(() -> "Error closing connection during client shutdown: " + e);
                }
            }
            workerGroup.shutdownGracefully();
            state.set(ClientState.CLOSED);
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
