package io.github.arhor.catrecognizer.config

import io.smallrye.config.ConfigMapping
import java.time.Duration
import java.util.Optional

@ConfigMapping(prefix = "cat-recognizer")
interface RecognizerConfig {

    fun worker(): Worker

    fun camera(): Camera

    fun state(): State

    fun debug(): Debug

    fun detector(): Detector

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
        fun encryption(): Encryption
    }

    interface Encryption {
        fun enabled(): Boolean
        fun key(): Optional<String>
    }

    interface State {
        fun staleAfter(): Duration
    }

    interface Debug {
        fun manualTriggerEnabled(): Boolean
        fun uploadEnabled(): Boolean
    }

    interface Detector {
        fun modelPath(): String
        fun imageSize(): Int
        fun confidenceThreshold(): Double
        fun iouThreshold(): Double
        fun className(): String
    }

    enum class CameraSource {
        HTTP_SNAPSHOT,
        NATIVE_API,
    }
}
