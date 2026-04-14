package app.myfaq.shared.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Serializer that accepts both JSON integers and quoted strings, returning an Int.
 * The v4.0 search endpoint returns `"id": "1"` and `"category_id": "15"` as strings.
 */
internal object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                return element.intOrNull
                    ?: element.content.toIntOrNull()
                    ?: 0
            }
        }
        return decoder.decodeInt()
    }

    override fun serialize(
        encoder: Encoder,
        value: Int,
    ) = encoder.encodeInt(value)
}

/**
 * Search result from `/search`.
 * v4.0 returns paginated wrapper. `id` and `category_id` arrive as
 * quoted strings in the JSON, hence the [FlexibleIntSerializer].
 */
@Serializable
data class SearchResult(
    @Serializable(with = FlexibleIntSerializer::class)
    val id: Int = 0,
    val lang: String? = null,
    @SerialName("category_id")
    @Serializable(with = FlexibleIntSerializer::class)
    val categoryId: Int = 0,
    val question: String = "",
    val answer: String? = null,
    val link: String? = null,
)

/**
 * Popular search term from `/searches/popular`.
 * v4.0 returns a plain array (NOT paginated).
 */
@Serializable
data class PopularSearch(
    val id: Int = 0,
    @SerialName("searchterm")
    val searchTerm: String = "",
    val number: String = "0",
    val lang: String? = null,
) {
    /** Convenience: `number` arrives as string in the JSON. */
    val count: Int get() = number.toIntOrNull() ?: 0
}
