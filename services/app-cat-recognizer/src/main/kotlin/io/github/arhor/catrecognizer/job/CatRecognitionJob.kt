package io.github.arhor.catrecognizer.job

import io.github.arhor.catrecognizer.service.CatRecognitionService
import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.Scheduled.ConcurrentExecution
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class CatRecognitionJob @Inject constructor(
    private val recognitionService: CatRecognitionService,
) {

    @Scheduled(
        every = "{cat-recognizer.worker.poll-interval}",
        concurrentExecution = ConcurrentExecution.SKIP,
    )
    fun detect() {
        recognitionService.runRecognition()
    }
}
