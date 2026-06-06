package io.github.arhor.catrecognizer.web

import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Duration

class DebugResourceFormattingTest {

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
