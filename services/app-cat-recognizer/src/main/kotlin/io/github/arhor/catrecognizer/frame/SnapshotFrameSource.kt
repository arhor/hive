package io.github.arhor.catrecognizer.frame

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.frame.model.FramePayload
import io.github.arhor.catrecognizer.frame.model.FrameSourceError
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@ApplicationScoped
class SnapshotFrameSource @Inject constructor(
    private val config: RecognizerConfig,
) : FrameSource {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(config.camera().connectTimeout())
        .build()

    override fun fetchFrame(): FramePayload {
        val snapshotUrl = config.camera().snapshotUrl()

        return try {
            val response = client.send(
                HttpRequest.newBuilder(URI.create(snapshotUrl))
                    .timeout(config.camera().readTimeout())
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray(),
            )

            if (response.statusCode() in 200..299) {
                FramePayload(
                    bytes = response.body(),
                    contentType = response.headers().firstValue("Content-Type").orElse(null),
                    observedAt = Instant.now(),
                )
            } else {
                throw FrameSourceError(
                    code = "FRAME_FETCH_FAILED",
                    message = "Failed to fetch snapshot: HTTP ${response.statusCode()}",
                    retriable = true,
                )
            }
        } catch (exception: FrameSourceError) {
            throw exception
        } catch (exception: Exception) {
            throw FrameSourceError(
                code = "FRAME_FETCH_FAILED",
                message = "Failed to fetch snapshot from $snapshotUrl",
                retriable = true,
                cause = exception,
            )
        }
    }
}
