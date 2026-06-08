package io.github.arhor.catrecognizer.web

import kotlinx.serialization.Serializable

@Serializable
data class RuntimeConfigSummary(
    val pollInterval: String,
    val snapshotConfigured: Boolean,
    val manualTriggerEnabled: Boolean,
)
