package io.github.arhor.catrecognizer.config

import io.smallrye.config.ConfigMapping
import java.time.Duration

@ConfigMapping(prefix = "cat-recognizer")
interface RecognizerConfig {

    fun worker(): Worker

    fun camera(): Camera

    fun detection(): Detection

    fun state(): State

    fun debug(): Debug

    interface Worker {
        fun enabled(): Boolean
        fun pollInterval(): Duration
        fun initialDelay(): Duration
        fun failureBackoff(): Duration
    }

    interface Camera {
        fun snapshotUrl(): String
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
    }

    interface Detection {
        fun mode(): io.github.arhor.catrecognizer.detection.DetectionMode
        fun unknownOnError(): Boolean
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
    }
}
