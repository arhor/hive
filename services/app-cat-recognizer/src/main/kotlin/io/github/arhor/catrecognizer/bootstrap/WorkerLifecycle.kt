package io.github.arhor.catrecognizer.bootstrap

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.recognition.RecognitionOrchestrator
import io.github.arhor.catrecognizer.recognition.model.RecognitionResult
import io.github.arhor.catrecognizer.state.LatestRecognitionState
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ApplicationScoped
class WorkerLifecycle @Inject constructor(
    private val config: RecognizerConfig,
    private val orchestrator: RecognitionOrchestrator,
    private val state: LatestRecognitionState,
) {

    private val running = AtomicBoolean(false)

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cat-recognizer-worker").apply {
            isDaemon = true
        }
    }

    fun onStart(@Observes event: StartupEvent) {
        state.markWorkerEnabled(config.worker().enabled())
        if (!config.worker().enabled()) {
            state.markWorkerRunning(false)
            return
        }

        if (!running.compareAndSet(false, true)) {
            return
        }

        state.markWorkerRunning(true)
        executor.submit { runLoop() }
    }

    fun onShutdown(@Observes event: ShutdownEvent) {
        running.set(false)
        state.markWorkerRunning(false)
        executor.shutdownNow()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    internal fun delayFor(result: RecognitionResult): Duration =
        if (result.error == null) {
            config.worker().pollInterval()
        } else {
            config.worker().failureBackoff()
        }

    private fun runLoop() {
        try {
            sleep(config.worker().initialDelay())
            while (running.get()) {
                val result = orchestrator.runRecognition()
                if (!running.get()) {
                    break
                }
                sleep(delayFor(result))
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            running.set(false)
            state.markWorkerRunning(false)
        }
    }

    private fun sleep(duration: Duration) {
        if (duration.isZero || duration.isNegative) {
            return
        }
        Thread.sleep(duration.toMillis())
    }
}
