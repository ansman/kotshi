package se.ansman.kotshi

import com.squareup.moshi.Json

@JsonSerializable
data class IgnoredProperties(
    @Json(ignore = false)
    val property1: String,
    @Json(ignore = true)
    val ignoredProperty: String = "foo",
)
