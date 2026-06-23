package io.github.arhor.esphome.client.async.internal;

import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.netty.channel.Channel;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EspHomeSubscription implements Flow.Subscription {

    private final Flow.Subscriber<? super EspHomeEvent> subscriber;
    private final Channel channel; // Netty channel used to control inbound reading

    // A good fit for producer-consumer handoff between the Netty thread and the subscriber
    private final LinkedTransferQueue<EspHomeEvent> queue = new LinkedTransferQueue<>();
    private final AtomicLong demand = new AtomicLong(0);
    private final AtomicBoolean isDraining = new AtomicBoolean(false);

    private static final int HI_WATERMARK = 5000;
    private static final int LO_WATERMARK = 1000;

    public EspHomeSubscription(
        final Flow.Subscriber<? super EspHomeEvent> subscriber,
        final Channel channel
    ) {
        this.subscriber = subscriber;
        this.channel = channel;
    }

    // Called by the subscriber when it is ready to receive n more items
    @Override
    public void request(long n) {
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("Demand must be > 0"));
            return;
        }
        demand.addAndGet(n);
        drain();

        // If demand exists again and the queue has drained enough, resume reading from the socket
        if (queue.size() < LO_WATERMARK && !channel.config().isAutoRead()) {
            channel.config().setAutoRead(true);
        }
    }

    @Override
    public void cancel() {
        demand.set(0);
        queue.clear();
        // Optional: also unregister this subscription from the event source
    }

    // Called from the Netty EventLoop when a new event has been decoded
    public void onEventDecoded(EspHomeEvent event) {
        // Try direct handoff first; if that is not possible, enqueue the event
        if (!queue.tryTransfer(event)) {
            queue.offer(event);

            // Apply backpressure by pausing socket reads when the queue grows too large
            if (queue.size() > HI_WATERMARK && channel.config().isAutoRead()) {
                channel.config().setAutoRead(false);
            }
        }
        drain();
    }

    // Custom hook for propagating transport-level errors to the subscriber
    public void onError(Throwable cause) {
        // Clear buffered events because the stream is terminating
        queue.clear();

        // Forward the error to the downstream subscriber
        subscriber.onError(cause);
    }

    // -------------------------------------------- internal implementation --------------------------------------------

    // Safe delivery loop that ensures only one thread drains the queue at a time
    private void drain() {
        if (!isDraining.compareAndSet(false, true)) {
            return; // Another thread is already delivering events
        }

        try {
            while (demand.get() > 0) {
                EspHomeEvent event = queue.poll();
                if (event == null) {
                    break; // No more buffered events to deliver
                }

                demand.decrementAndGet();
                subscriber.onNext(event);
            }
        } finally {
            isDraining.set(false);
            // If new demand or events arrived while draining, run again
            if (demand.get() > 0 && !queue.isEmpty()) {
                drain();
            }
        }
    }
}
