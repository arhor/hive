package io.github.arhor.catrecognizer.client

import io.github.arhor.catrecognizer.client.impl.SnapshotFrameClient
import io.kotest.matchers.shouldNotBe
import org.eclipse.microprofile.faulttolerance.Retry
import org.eclipse.microprofile.faulttolerance.Timeout
import org.junit.jupiter.api.Test

class SnapshotFrameClientFaultToleranceTest {

    @Test
    fun `fetchFrame is annotated with retry and timeout`() {
        val method = SnapshotFrameClient::class.java.getMethod("fetchFrame")

        method.getAnnotation(Retry::class.java) shouldNotBe null
        method.getAnnotation(Timeout::class.java) shouldNotBe null
    }
}
