package io.github.arhor.catrecognizer.config

import io.smallrye.config.ConfigMapping
import java.time.Duration

@ConfigMapping(prefix = "cat-recognizer")
interface RecognizerConfig {

    fun worker(): Worker

    fun camera(): Camera

    fun state(): State

    fun debug(): Debug

    interface Worker {
        fun pollInterval(): Duration
    }

    interface Camera {
        fun snapshotUrl(): String
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
    }
}
