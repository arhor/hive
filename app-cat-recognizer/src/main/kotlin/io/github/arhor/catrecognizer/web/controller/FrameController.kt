package io.github.arhor.catrecognizer.web.controller

import io.github.arhor.catrecognizer.service.LatestRecognitionState
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/frame")
class FrameController @Inject constructor(
    private val state: LatestRecognitionState,
) {

    @GET
    @Path("/latest")
    fun latest(): Response {
        val bytes = state.snapshot().frameBytes
            ?: return Response.noContent().build()

        return Response.ok(bytes)
            .header("Content-Type", "image/jpeg")
            .header("Cache-Control", "no-store")
            .build()
    }
}
