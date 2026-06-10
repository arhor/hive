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
        fun source(): CameraSource
        fun snapshotUrl(): String
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
        fun nativeApi(): NativeApi
    }

    interface NativeApi {
        fun host(): String
        fun port(): Int
        fun connectTimeout(): Duration
        fun readTimeout(): Duration
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
    }

    enum class CameraSource {
        HTTP_SNAPSHOT,
        NATIVE_API,
    }
}
