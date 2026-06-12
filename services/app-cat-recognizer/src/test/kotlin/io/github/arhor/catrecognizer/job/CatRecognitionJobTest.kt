package io.github.arhor.catrecognizer.job

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.scheduler.Scheduled
import org.junit.jupiter.api.Test

class CatRecognitionJobTest {

    @Test
    fun `detect uses configured poll interval and skips overlapping executions`() {
        val method = CatRecognitionJob::class.java.getMethod("detect")
        val scheduled = method.getAnnotation(Scheduled::class.java)

        scheduled shouldNotBe null
        scheduled.every shouldBe "{cat-recognizer.worker.poll-interval}"
        scheduled.concurrentExecution shouldBe Scheduled.ConcurrentExecution.SKIP
    }
}
