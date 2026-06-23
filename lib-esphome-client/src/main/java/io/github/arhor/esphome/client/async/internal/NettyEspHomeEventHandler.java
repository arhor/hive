package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.proto.PingRequest;
import io.github.arhor.esphome.client.proto.PingResponse;
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
        if (msg instanceof PingRequest) {
            ctx.writeAndFlush(PingResponse.getDefaultInstance());
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

        ctx.close();
    }
}
