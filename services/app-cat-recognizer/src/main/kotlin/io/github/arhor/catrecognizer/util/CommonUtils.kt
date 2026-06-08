package io.github.arhor.catrecognizer.util

import java.time.Duration

fun Duration.toFriendlyString(): String =
    if (toNanosPart() == 0) {
        "${seconds}s"
    } else {
        "${toMillis()}ms"
    }
