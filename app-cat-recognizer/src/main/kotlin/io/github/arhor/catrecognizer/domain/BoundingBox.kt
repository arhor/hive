package io.github.arhor.catrecognizer.domain

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
