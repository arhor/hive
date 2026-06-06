package io.github.arhor.catrecognizer.detection

import io.github.arhor.catrecognizer.config.RecognizerConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

@ApplicationScoped
class CatDetectorProducer @Inject constructor(
    private val config: RecognizerConfig,
    private val stubCatDetector: StubCatDetector,
    private val openCvCatDetector: OpenCvCatDetector,
) {

    @Produces
    @ApplicationScoped
    fun catDetector(): CatDetector =
        when (config.detection().mode()) {
            DetectionMode.STUB,
            DetectionMode.ALWAYS_PRESENT,
            DetectionMode.ALWAYS_ABSENT,
                -> CatDetector { frame -> stubCatDetector.detect(frame) }

            DetectionMode.OPENCV ->
                CatDetector { frame -> openCvCatDetector.detect(frame) }
        }
}
