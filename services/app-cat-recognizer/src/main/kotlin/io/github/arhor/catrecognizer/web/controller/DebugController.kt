package io.github.arhor.catrecognizer.web

import io.github.arhor.catrecognizer.config.RecognizerConfig
import io.github.arhor.catrecognizer.util.toFriendlyString
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
class DebugController @Inject constructor(
    private val config: RecognizerConfig,
) {

    @GET
    @Path("/config")
    fun config(): RuntimeConfigSummary =
        RuntimeConfigSummary(
            pollInterval = config.worker().pollInterval().toFriendlyString(),
            snapshotConfigured = config.camera().snapshotUrl().isNotBlank(),
            manualTriggerEnabled = config.debug().manualTriggerEnabled(),
        )
}
