package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.Locale

@Path("/api/debug")
@Produces(MediaType.APPLICATION_JSON)
class DebugResource @Inject constructor(
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/config")
    fun config(): RuntimeConfigSummary =
        RuntimeConfigSummary(
            workerEnabled = config.worker().enabled(),
            pollInterval = config.worker().pollInterval().toFriendlyString(),
            failureBackoff = config.worker().failureBackoff().toFriendlyString(),
            detectionMode = config.detection().mode().name.lowercase(Locale.ROOT),
            snapshotConfigured = config.camera().snapshotUrl().isNotBlank(),
        )
}

@Serializable
data class RuntimeConfigSummary(
    val workerEnabled: Boolean,
    val pollInterval: String,
    val failureBackoff: String,
    val detectionMode: String,
    val snapshotConfigured: Boolean,
)

internal fun Duration.toFriendlyString(): String =
    if (toNanosPart() == 0) {
        "${seconds}s"
    } else {
        "${toMillis()}ms"
    }
