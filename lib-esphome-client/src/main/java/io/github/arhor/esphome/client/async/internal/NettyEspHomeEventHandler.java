package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.proto.DisconnectRequest;
import io.github.arhor.esphome.client.proto.DisconnectResponse;
import io.github.arhor.esphome.client.proto.GetTimeRequest;
import io.github.arhor.esphome.client.proto.GetTimeResponse;
import io.github.arhor.esphome.client.proto.PingRequest;
import io.github.arhor.esphome.client.proto.PingResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

@ChannelHandler.Sharable
public class NettyEspHomeEventHandler extends SimpleChannelInboundHandler<Object> {

    private final List<EspHomeSubscription> subscriptions;

    public NettyEspHomeEventHandler(final List<EspHomeSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DisconnectRequest) {
            ctx.writeAndFlush(DisconnectResponse.getDefaultInstance())
                .addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (msg instanceof DisconnectResponse) {
            ctx.close();
            return;
        }
        if (msg instanceof PingRequest) {
            ctx.writeAndFlush(PingResponse.getDefaultInstance());
            return;
        }
        if (msg instanceof GetTimeRequest) {
            ctx.writeAndFlush(
                GetTimeResponse.newBuilder()
                    .setEpochSeconds((int) (System.currentTimeMillis() / 1000))
                    .build()
            );
            return;
        }

        var domainEvent = EspHomeEventMapper.map(msg);
        if (domainEvent == null) {
            return;
        }

        for (var subscription : subscriptions) {
            subscription.onEventDecoded(domainEvent);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        for (var subscription : subscriptions) {
            try {
                subscription.onError(cause);
            } catch (Exception _) {
                // protect the loop from erroneous subscribers
            }
        }

        ctx.writeAndFlush(DisconnectRequest.getDefaultInstance())
            .addListener(ChannelFutureListener.CLOSE);
    }
}
