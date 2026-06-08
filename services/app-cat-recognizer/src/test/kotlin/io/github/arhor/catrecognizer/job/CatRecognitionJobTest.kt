package io.github.arhor.catrecognizer.job

import io.quarkus.scheduler.Scheduled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CatRecognitionJobTest {

    @Test
    fun `detect uses configured poll interval and skips overlapping executions`() {
        val method = CatRecognitionJob::class.java.getMethod("detect")
        val scheduled = method.getAnnotation(Scheduled::class.java)

        assertNotNull(scheduled)
        assertEquals("{cat-recognizer.worker.poll-interval}", scheduled.every)
        assertEquals(Scheduled.ConcurrentExecution.SKIP, scheduled.concurrentExecution)
    }
}
