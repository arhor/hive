package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.util.toFriendlyString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration

class DebugControllerFormattingTest {

    @Test
    fun `formats whole seconds compactly`() {
        Duration.ofSeconds(5).toFriendlyString() shouldBe "5s"
    }

    @Test
    fun `preserves sub-second values in milliseconds`() {
        Duration.ofMillis(500).toFriendlyString() shouldBe "500ms"
        Duration.ofMillis(1500).toFriendlyString() shouldBe "1500ms"
    }
}
