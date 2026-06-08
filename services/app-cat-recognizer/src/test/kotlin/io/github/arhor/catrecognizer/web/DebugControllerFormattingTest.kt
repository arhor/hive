package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.util.toFriendlyString
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugControllerFormattingTest {

    @Test
    fun `formats whole seconds compactly`() {
        assertEquals("5s", Duration.ofSeconds(5).toFriendlyString())
    }

    @Test
    fun `preserves sub-second values in milliseconds`() {
        assertEquals("500ms", Duration.ofMillis(500).toFriendlyString())
        assertEquals("1500ms", Duration.ofMillis(1500).toFriendlyString())
    }
}
