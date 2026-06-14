package io.github.arhor.catrecognizer.config

import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class RecognizerConfigBindingTest {

    @Inject
    lateinit var config: RecognizerConfig

    @Test
    fun `binds simplified defaults from application properties`() {
        config.worker().pollInterval() shouldBe Duration.ofSeconds(5)

        config.state().staleAfter() shouldBe Duration.ofSeconds(30)
        config.debug().manualTriggerEnabled() shouldBe true
        config.debug().uploadEnabled() shouldBe true

        config.camera().connectTimeout() shouldBe Duration.ofSeconds(2)
        config.camera().readTimeout() shouldBe Duration.ofSeconds(5)
        config.camera().snapshotUrl() shouldBe "http://esp32-cam.local/snapshot"
        config.camera().source() shouldBe RecognizerConfig.CameraSource.NATIVE_API
        config.camera().nativeApi().host() shouldBe "esp32-cam.local"
        config.camera().nativeApi().port() shouldBe 6053
        config.camera().nativeApi().connectTimeout() shouldBe Duration.ofSeconds(2)
        config.camera().nativeApi().readTimeout() shouldBe Duration.ofSeconds(5)
        config.camera().nativeApi().encryption().enabled() shouldBe false
        config.camera().nativeApi().encryption().key().isPresent shouldBe false

        config.detector().modelPath() shouldBe "classpath:/models/yolo11n.onnx"
        config.detector().imageSize() shouldBe 640
        config.detector().confidenceThreshold() shouldBe 0.50
        config.detector().iouThreshold() shouldBe 0.45
        config.detector().className() shouldBe "cat"
    }
}
