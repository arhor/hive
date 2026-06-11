package io.github.arhor.catrecognizer.util

import org.jboss.logging.Logger


inline fun Logger.debugK(message: () -> String) {
    if (isDebugEnabled) {
        debug(message())
    }
}

inline fun Logger.debugK(t: Throwable, message: () -> String) {
    if (isDebugEnabled) {
        debug(message(), t)
    }
}
