package io.github.arhor.esphome.client.async;

import io.github.arhor.esphome.client.async.internal.NettyEspHomeClient;
import io.github.arhor.esphome.client.async.model.EspHomeEvent;
import io.github.arhor.esphome.client.async.model.EspHomeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live integration tests against a real ESPHome device at 192.168.0.14:6053.
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

    private Logger logger;
    private EspHomeClient client;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger(getClass().getName());
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
            logger.info("Connected: " + conn);
        }
    }

    @Test
    void fetchesDeviceInfo() throws Exception {
        try (var conn = client.connect().get(15, TimeUnit.SECONDS)) {
            var deviceInfoFuture = new CompletableFuture<EspHomeEvent.DeviceInfo>();

            conn.observeEvents().subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                public void onNext(EspHomeEvent event) {
                    if (event instanceof EspHomeEvent.DeviceInfo info) {
                        deviceInfoFuture.complete(info);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    deviceInfoFuture.completeExceptionally(t);
                }

                @Override
                public void onComplete() {}
            });

            conn.send(new EspHomeMessage.GetDeviceInfo());

            var info = deviceInfoFuture.get(10, TimeUnit.SECONDS);

            logger.info(
                () -> """
                    
                    === Device Info ===
                      name:            %s
                      mac:             %s
                      esphome version: %s
                      model:           %s
                      manufacturer:    %s
                      friendly name:   %s""".formatted(
                    info.name(),
                    info.macAddress(),
                    info.esphomeVersion(),
                    info.model(),
                    info.manufacturer(),
                    info.friendlyName()
                )
            );

            assertNotNull(info.name());
            assertFalse(info.name().isBlank());
        }
    }
}
