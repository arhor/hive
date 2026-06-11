package io.github.arhor.catrecognizer.util

import io.github.arhor.catrecognizer.client.model.FramePayload
import io.github.arhor.catrecognizer.domain.BoundingBox
import io.github.arhor.catrecognizer.domain.DetectionOutcome
import java.security.MessageDigest

fun FramePayload.toDebugSummary(): String =
    "contentType=$contentType, bytes=${bytes.size}, sha256=${bytes.sha256Prefix()}, observedAt=$observedAt"

fun DetectionOutcome.toDebugSummary(): String =
    when (this) {
        is DetectionOutcome.Present ->
            "type=Present, confidence=$confidence, boxes=${boundingBoxes.size}, boxDetails=${boundingBoxes.toDebugSummary()}"

        is DetectionOutcome.Absent ->
            "type=Absent, confidence=$confidence"

        is DetectionOutcome.Unknown ->
            "type=Unknown, reason=$reason"
    }

private fun List<BoundingBox>.toDebugSummary(): String =
    joinToString(prefix = "[", postfix = "]") { box ->
        "(x=${box.x}, y=${box.y}, w=${box.width}, h=${box.height})"
    }

private fun ByteArray.sha256Prefix(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { "%02x".format(it) }
        .take(12)
