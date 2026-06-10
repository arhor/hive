package io.github.arhor.catrecognizer.client.impl

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.DefaultEspHomeClient
import io.github.arhor.esphome.client.EspHomeClientConfig
import io.github.arhor.esphome.client.EspHomeConnection
import io.github.arhor.esphome.client.EspHomeEncryptionConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
@NativeApiCameraClient
class EspHomeNativeFrameClient @Inject constructor(
    private val config: RecognizerConfig,
) : FrameClient {

    private var factory: (EspHomeClientConfig) -> EspHomeConnection = { DefaultEspHomeClient(it).connect() }

    internal constructor(
        config: RecognizerConfig,
        factory: (EspHomeClientConfig) -> EspHomeConnection,
    ) : this(config) {
        this.factory = factory
    }

    override fun fetchFrame(): FramePayload {
        val nativeApi = config.camera().nativeApi()
        val clientConfig = EspHomeClientConfig(
            host = nativeApi.host(),
            port = nativeApi.port(),
            connectTimeout = nativeApi.connectTimeout(),
            readTimeout = nativeApi.readTimeout(),
            encryption = EspHomeEncryptionConfig(
                enabled = nativeApi.encryption().enabled(),
                key = nativeApi.encryption().key().orElse(null),
            ),
        )

        return try {
            factory(clientConfig).use { connection ->
                FramePayload(
                    bytes = connection.fetchCameraImage(single = true),
                    contentType = "image/jpeg",
                    observedAt = Instant.now(),
                )
            }
        } catch (exception: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch ESPHome camera frame from ${nativeApi.host()}:${nativeApi.port()}",
                retriable = true,
                cause = exception,
            )
        }
    }
}
