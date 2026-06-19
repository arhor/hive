package io.github.arhor.catrecognizer.client.impl

import io.github.arhor.catrecognizer.client.FrameClient
import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.domain.FrameSourceError
import io.github.arhor.esphome.client.config.EspHomeClientConfig
import io.github.arhor.esphome.client.internal.EspHomeClientDefault
import io.github.arhor.esphome.client.internal.EspHomeConnectionFactory
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant

@ApplicationScoped
@NativeApiCameraClient
class EspHomeNativeFrameClient @Inject constructor(
    private val config: RecognizerConfig,
) : FrameClient {

    private var factory: EspHomeConnectionFactory = { EspHomeClientDefault(it).connect() }

    internal constructor(
        config: RecognizerConfig,
        factory: EspHomeConnectionFactory,
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
            encryption = EspHomeClientConfig.EncryptionConfig(
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
        } catch (ex: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch ESPHome camera frame from ${nativeApi.host()}:${nativeApi.port()}",
                retriable = true,
                cause = ex,
            )
        }
    }
}
