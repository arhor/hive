package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable

@Serializable
data class RecognitionError(
    val code: String,
    val message: String,
    val retriable: Boolean,
)
