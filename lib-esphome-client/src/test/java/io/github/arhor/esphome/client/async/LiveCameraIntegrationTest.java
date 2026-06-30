package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.internal.NettyEspHomeClient;
import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.github.arhor.esphome.client.async.model.EspHomeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live integration tests against a real ESPHome camera at 192.168.0.14:6053.
 * <p>
 * Run: ./gradlew :lib-esphome-client:test --tests "*.LiveCameraIntegrationTest" -i
 * <p>
 * Tagged "live" — excluded from CI by default.
 */
@Tag("live")
class LiveCameraIntegrationTest {

    @BeforeAll
    static void configureLogging() throws IOException {
        try (InputStream cfg = LiveCameraIntegrationTest.class.getClassLoader()
                .getResourceAsStream("logging-live.properties")) {
            if (cfg != null) {
                LogManager.getLogManager().readConfiguration(cfg);
            }
        }
    }

    private EspHomeClient client;

    @BeforeEach
    void setUp() {
        client = new NettyEspHomeClient(new EspHomeClient.Config(
            "192.168.0.14",
            6053,
            "hive-live-test",
            null
        ));
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
    }

    @Test
    void connects() throws Exception {
        try (var conn = client.connect().get(15, TimeUnit.SECONDS)) {
            assertNotNull(conn);
            System.out.println("Connected: " + conn);
        }
    }

    @Test
    void fetchesSingleCameraImage() throws Exception {
        try (var conn = client.connect().get(15, TimeUnit.SECONDS)) {
            var imageFuture = new CompletableFuture<byte[]>();
            var buffer = new ByteArrayOutputStream();

            conn.observeEvents().subscribe(new Flow.Subscriber<>() {
                private Flow.Subscription sub;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.sub = subscription;
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(EspHomeEvent event) {
                    if (event instanceof EspHomeEvent.CameraImage(var key, var data, var done)) {
                        buffer.write(data, 0, data.length);
                        System.out.printf("  chunk: key=%d size=%d done=%b%n", key, data.length, done);
                        if (done) {
                            sub.cancel();
                            imageFuture.complete(buffer.toByteArray());
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    imageFuture.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    if (!imageFuture.isDone()) {
                        imageFuture.complete(buffer.toByteArray());
                    }
                }
            });

            conn.send(new EspHomeMessage.GetCameraImage(true, false));

            var jpeg = imageFuture.get(15, TimeUnit.SECONDS);

            System.out.println("=== Camera Image ===");
            System.out.println("  total size: " + jpeg.length + " bytes");
            assertTrue(jpeg.length > 0, "image must not be empty");

            var out = Paths.get(System.getProperty("java.io.tmpdir"), "esphome-camera-snapshot.jpg");
            Files.write(out, jpeg);
            System.out.println("  saved to: " + out);
        }
    }

    @Test
    void observesEventsAfterConnect() throws Exception {
        try (var conn = client.connect().get(15, TimeUnit.SECONDS)) {
            var firstEvent = new CompletableFuture<EspHomeEvent>();

            conn.observeEvents().subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                public void onNext(EspHomeEvent event) {
                    System.out.println("  event: " + event);
                    firstEvent.complete(event);
                }

                @Override
                public void onError(Throwable t) {
                    firstEvent.completeExceptionally(t);
                }

                @Override
                public void onComplete() {}
            });

            conn.send(new EspHomeMessage.GetCameraImage(true, false));

            var event = firstEvent.get(15, TimeUnit.SECONDS);
            assertNotNull(event);
        }
    }
}
