package io.github.arhor.esphome.client.async;

import com.google.protobuf.MessageLite;
import io.github.arhor.esphome.client.async.internal.EspHomeChannelAttributes;
import io.github.arhor.esphome.client.async.internal.EspHomeConnection;
import io.github.arhor.esphome.client.async.internal.NettyEspHomeConnectionManager;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufDecoder;
import io.github.arhor.esphome.client.async.internal.codec.EspHomeProtobufEncoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedFrameDecoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedFrameEncoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedPayloadDecoder;
import io.github.arhor.esphome.client.async.internal.codec.encrypted.EspHomeEncryptedPayloadEncoder;
import io.github.arhor.esphome.client.async.internal.noise.NoiseHandshakeState;
import io.github.arhor.esphome.client.proto.ConnectResponse;
import io.github.arhor.esphome.client.proto.HelloRequest;
import io.github.arhor.esphome.client.proto.HelloResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NettyEncryptedClientTest {

    @Test
    void connectsOverEncryptedNativeApiAndCompletesHandshake() throws Exception {
        final var psk = new byte[32];
        for (var index = 0; index < 32; index++) {
            psk[index] = (byte) (index + 1);
        }

        final var serverGotInit = new AtomicBoolean();
        final var serverGotHello = new AtomicBoolean();

        final EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            final var server = new ServerBootstrap()
                .group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        ch.pipeline().addLast("encryptedFrameDecoder", new EspHomeEncryptedFrameDecoder());
                        ch.pipeline().addLast("encryptedFrameEncoder", new EspHomeEncryptedFrameEncoder());
                        ch.pipeline().addLast(
                            "serverNoiseHandler",
                            new ServerNoiseHandshakeHandler(psk, serverGotInit, serverGotHello)
                        );
                    }
                });

            final var serverChannel = server.bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
            final var port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

            final var config = new EspHomeClientConfig(
                "127.0.0.1",
                port,
                "test-client",
                null,
                new EspHomeClientConfig.EncryptionConfig(true, Base64.getEncoder().encodeToString(psk)),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                EspHomeClientConfig.API_VERSION_MAJOR,
                EspHomeClientConfig.API_VERSION_MINOR
            );

            try (var client = new NettyEspHomeConnectionManager(config)) {
                final EspHomeConnection connection = client.connect().get(15, TimeUnit.SECONDS);
                assertNotNull(connection);
                assertTrue(serverGotInit.get(), "server should receive noise init frame");
                connection.close();
            } catch (Exception exception) {
                fail(
                    "connection failed: " + exception
                        + ", serverGotInit=" + serverGotInit.get()
                        + ", serverGotHello=" + serverGotHello.get(),
                    exception
                );
            }
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    private static final class ServerNoiseHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private enum Step { WAIT_INIT, WAIT_CLIENT_HANDSHAKE, COMPLETE }

        private final NoiseHandshakeState handshake;
        private final AtomicBoolean serverGotInit;
        private final AtomicBoolean serverGotHello;
        private Step step = Step.WAIT_INIT;

        private ServerNoiseHandshakeHandler(
            final byte[] psk,
            final AtomicBoolean serverGotInit,
            final AtomicBoolean serverGotHello
        ) {
            this.handshake = NoiseHandshakeState.responder(psk);
            this.serverGotInit = serverGotInit;
            this.serverGotHello = serverGotHello;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
            switch (step) {
                case WAIT_INIT -> onInit(ctx, msg);
                case WAIT_CLIENT_HANDSHAKE -> onClientHandshake(ctx, msg);
                default -> ctx.close();
            }
        }

        private void onInit(final ChannelHandlerContext ctx, final ByteBuf msg) {
            if (msg.readableBytes() != 0) {
                ctx.close();
                return;
            }

            serverGotInit.set(true);

            final var serverHello = ctx.alloc().buffer(3);
            serverHello.writeByte(0x01);
            serverHello.writeByte('c');
            serverHello.writeByte(0x00);
            ctx.writeAndFlush(serverHello);
            step = Step.WAIT_CLIENT_HANDSHAKE;
        }

        private void onClientHandshake(final ChannelHandlerContext ctx, final ByteBuf msg) {
            if (msg.readableBytes() == 0 || msg.getByte(msg.readerIndex()) != 0x00) {
                ctx.close();
                return;
            }

            final var noiseBytes = ByteBufUtil.getBytes(msg, msg.readerIndex() + 1, msg.readableBytes() - 1);
            handshake.readMessage(noiseBytes);

            final var response = handshake.writeMessage();
            final var payload = ctx.alloc().buffer(1 + response.length);
            payload.writeByte(0x00);
            payload.writeBytes(response);
            ctx.writeAndFlush(payload).addListener(future -> {
                if (!future.isSuccess()) {
                    ctx.close();
                    return;
                }
                completeHandshake(ctx);
            });
        }

        private void completeHandshake(final ChannelHandlerContext ctx) {
            ctx.channel().attr(EspHomeChannelAttributes.SEND_CIPHER).set(handshake.getSendCipher());
            ctx.channel().attr(EspHomeChannelAttributes.RECEIVE_CIPHER).set(handshake.getReceiveCipher());

            final var pipeline = ctx.pipeline();
            pipeline.addAfter("encryptedFrameDecoder", "encryptedPayloadDecoder", new EspHomeEncryptedPayloadDecoder());
            pipeline.addAfter("encryptedPayloadDecoder", "protobufDecoder", new EspHomeProtobufDecoder());
            pipeline.addBefore("serverNoiseHandler", "appHandler", new ServerAppHandler(serverGotHello));
            pipeline.addBefore("appHandler", "protobufEncoder", new EspHomeProtobufEncoder());
            pipeline.addBefore("protobufEncoder", "encryptedPayloadEncoder", new EspHomeEncryptedPayloadEncoder());
            pipeline.remove(this);
            step = Step.COMPLETE;
        }
    }

    private static final class ServerAppHandler extends SimpleChannelInboundHandler<Object> {

        private final AtomicBoolean serverGotHello;

        private ServerAppHandler(final AtomicBoolean serverGotHello) {
            this.serverGotHello = serverGotHello;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
            switch (msg) {
                case HelloRequest _ -> {
                    serverGotHello.set(true);
                    ctx.writeAndFlush(
                    HelloResponse.newBuilder()
                        .setApiVersionMajor(EspHomeClientConfig.API_VERSION_MAJOR)
                        .setApiVersionMinor(EspHomeClientConfig.API_VERSION_MINOR)
                        .setName("test-device")
                        .build()
                    );
                }
                case io.github.arhor.esphome.client.proto.ConnectRequest _ -> ctx.writeAndFlush(
                    ConnectResponse.newBuilder()
                        .setInvalidPassword(false)
                        .build()
                );
                case MessageLite _ -> ctx.close();
                default -> ctx.close();
            }
        }
    }
}
