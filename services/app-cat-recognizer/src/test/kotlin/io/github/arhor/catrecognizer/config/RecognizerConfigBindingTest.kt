package io.github.arhor.catrecognizer.config

import io.github.arhor.catrecognizer.detection.DetectionMode
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@QuarkusTest
class RecognizerConfigBindingTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds defaults from application properties`() {
        assertTrue(config.worker().enabled())
        assertEquals(Duration.ofSeconds(5), config.worker().pollInterval())
        assertEquals(Duration.ofSeconds(1), config.worker().initialDelay())
        assertEquals(Duration.ofSeconds(30), config.worker().failureBackoff())

        assertEquals(DetectionMode.STUB, config.detection().mode())

        assertEquals(Duration.ofSeconds(30), config.state().staleAfter())
        assertTrue(config.debug().manualTriggerEnabled())

        assertEquals(Duration.ofSeconds(2), config.camera().connectTimeout())
        assertEquals(Duration.ofSeconds(5), config.camera().readTimeout())
        assertEquals("http://esp32-cam.local/snapshot", config.camera().snapshotUrl())

        assertTrue(config.detection().unknownOnError())
    }
}
