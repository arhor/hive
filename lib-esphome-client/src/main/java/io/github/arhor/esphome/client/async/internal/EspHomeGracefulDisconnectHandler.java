package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.proto.DisconnectRequest;
import io.github.arhor.esphome.client.proto.DisconnectResponse;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.TimeUnit;

public final class EspHomeGracefulDisconnectHandler extends ChannelDuplexHandler {

    private static final int DISCONNECT_TIMEOUT_SECONDS = 5;

    // Accessed only from the channel's event loop thread — no synchronization needed.
    private ChannelPromise pendingClose;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof DisconnectRequest) {
            // Server-initiated disconnect: acknowledge and close.
            ctx.writeAndFlush(DisconnectResponse.getDefaultInstance())
                .addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (msg instanceof DisconnectResponse) {
            // Response to our DisconnectRequest: complete the pending close.
            if (pendingClose != null) {
                final var promise = pendingClose;
                pendingClose = null;
                ctx.close(promise);
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) {
        if (pendingClose != null) {
            // Disconnect already in flight — chain onto the existing promise.
            pendingClose.addListener(f -> {
                if (f.isSuccess()) {
                    promise.setSuccess();
                } else {
                    promise.setFailure(f.cause());
                }
            });
            return;
        }

        if (!ctx.channel().isActive()) {
            ctx.close(promise);
            return;
        }

        pendingClose = promise;

        ctx.writeAndFlush(DisconnectRequest.getDefaultInstance()).addListener(f -> {
            if (!f.isSuccess()) {
                // Write failed — force close immediately.
                pendingClose = null;
                ctx.close(promise);
                return;
            }
            // Fallback: force close if no DisconnectResponse arrives in time.
            ctx.executor().schedule(() -> {
                if (pendingClose != null) {
                    pendingClose = null;
                    ctx.close(promise);
                }
            }, DISCONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        });
    }
}
