package io.github.arhor.catrecognizer.frame.model

class FrameSourceError(
    val code: String,
    override val message: String,
    val retriable: Boolean,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
