package io.github.arhor.catrecognizer.config

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@QuarkusTest
class RecognizerConfigBindingTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds simplified defaults from application properties`() {
        assertEquals(Duration.ofSeconds(5), config.worker().pollInterval())

        assertEquals(Duration.ofSeconds(30), config.state().staleAfter())
        assertTrue(config.debug().manualTriggerEnabled())

        assertEquals(Duration.ofSeconds(2), config.camera().connectTimeout())
        assertEquals(Duration.ofSeconds(5), config.camera().readTimeout())
        assertEquals("http://esp32-cam.local/snapshot", config.camera().snapshotUrl())
        assertEquals(RecognizerConfig.CameraSource.HTTP_SNAPSHOT, config.camera().source())
        assertEquals("esp32-cam.local", config.camera().nativeApi().host())
        assertEquals(6053, config.camera().nativeApi().port())
        assertEquals(Duration.ofSeconds(2), config.camera().nativeApi().connectTimeout())
        assertEquals(Duration.ofSeconds(5), config.camera().nativeApi().readTimeout())
        assertFalse(config.camera().nativeApi().encryption().enabled())
        assertFalse(config.camera().nativeApi().encryption().key().isPresent)
    }
}
