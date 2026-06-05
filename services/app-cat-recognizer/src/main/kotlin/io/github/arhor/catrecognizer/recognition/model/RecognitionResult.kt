package io.github.arhor.catrecognizer.recognition.model

import io.github.arhor.catrecognizer.detection.DetectionMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class RecognitionResult(
    val status: CatPresenceStatus,
    @Serializable(with = InstantIso8601Serializer::class)
    val observedAt: Instant,
    val confidence: Double? = null,
    val detectorMode: DetectionMode,
    val source: String,
    val error: RecognitionError? = null,
)

object InstantIso8601Serializer : KSerializer<Instant> {

    override val descriptor = PrimitiveSerialDescriptor(
        serialName = "Instant",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())
}
