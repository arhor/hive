package io.github.arhor.esphome.client.async;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /* ------------------------------------------ Internal implementation ------------------------------------------- */

    private ChannelInitializer<SocketChannel> createChannelInitializer(
        final List<EspHomeSubscription> subscriptions,
        final CompletableFuture<EspHomeConnection> resultFuture
    ) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                var pipeline = ch.pipeline();

                // 1. Inbound traffic decoders (framing and Protobuf)
                pipeline.addLast("frameDecoder", new EspHomeVarIntFrameDecoder());
                pipeline.addLast("protobufDecoder", new EspHomeProtobufDecoder());

                // 2. Outbound traffic encoders (commands -> Protobuf -> VarInt framing)
                pipeline.addLast("frameEncoder", new EspHomeVarIntFrameEncoder());
                pipeline.addLast("protobufEncoder", new EspHomeProtobufEncoder());

                // 3. Temporary Handshake Handler
                // It will replace itself with NettyEspHomeEventHandler upon successful login
                pipeline.addLast("handshakeHandler", new EspHomeHandshakeHandler(resultFuture, subscriptions, config));
            }
        };
    }
}
