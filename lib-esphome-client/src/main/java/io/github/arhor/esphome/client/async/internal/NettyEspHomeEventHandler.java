package io.github.arhor.esphome.client.async.internal;

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
        // Шаг 1: Конвертируем сырой Protobuf DTO в доменную модель (Слой мапперов)
        var domainEvent = EspHomeEventMapper.map(msg);
        if (domainEvent == null) {
            return; // Сообщение не поддерживается или отфильтровано
        }

        // Шаг 2: Распределяем доменное событие по подпискам с учетом их Backpressure
        for (var subscription : subscriptions) {
            subscription.onEventDecoded(domainEvent);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 1. Оповещаем все доменные подписки о том, что сеть упала.
        // Каждая подписка внутри себя вызовет subscriber.onError(cause)
        for (var subscription : subscriptions) {
            try {
                subscription.onError(cause);
            } catch (Exception _) {
                // Защищаем цикл от сбойных кастомных подписчиков
            }
        }

        // 2. Закрываем сетевой контекст
        ctx.close();
    }
}
