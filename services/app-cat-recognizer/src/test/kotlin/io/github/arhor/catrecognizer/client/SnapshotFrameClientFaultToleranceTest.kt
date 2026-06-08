package io.github.arhor.catrecognizer.client

import org.eclipse.microprofile.faulttolerance.Retry
import org.eclipse.microprofile.faulttolerance.Timeout
import kotlin.test.Test
import kotlin.test.assertNotNull

class SnapshotFrameClientFaultToleranceTest {

    @Test
    fun `fetchFrame is annotated with retry and timeout`() {
        val method = SnapshotFrameClient::class.java.getMethod("fetchFrame")

        assertNotNull(method.getAnnotation(Retry::class.java))
        assertNotNull(method.getAnnotation(Timeout::class.java))
    }
}
