package se.ansman.kotshi

import com.squareup.moshi.Json

@JsonSerializable
data class CustomNames(
    @Json(name = "jsonProp1") val prop1: String,
    @field:Json(name = "jsonProp2") val prop2: String
)
