package io.github.arhor.catrecognizer.client.impl

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.config.RecognizerConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

@ApplicationScoped
class FrameClientProducer @Inject constructor(
    private val config: RecognizerConfig,
    @param:HttpSnapshotCameraClient private val snapshotFrameClient: FrameClient,
    @param:NativeApiCameraClient private val nativeFrameClient: FrameClient,
) {

    @Produces
    @ApplicationScoped
    fun frameClient(): FrameClient =
        when (config.camera().source()) {
            RecognizerConfig.CameraSource.HTTP_SNAPSHOT -> snapshotFrameClient
            RecognizerConfig.CameraSource.NATIVE_API -> nativeFrameClient
        }
}
