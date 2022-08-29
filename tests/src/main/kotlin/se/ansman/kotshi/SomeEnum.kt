package se.ansman.kotshi

import com.squareup.moshi.Json

@JsonSerializable
enum class SomeEnum {
    VALUE1,
    VALUE2,
    @OptIn(ExperimentalKotshiApi::class)
    @JsonProperty(name = "VALUE3-alt")
    VALUE3,
    @Json(name = "VALUE4-alt")
    VALUE4,
    VALUE5
}

@JsonSerializable
enum class SomeEnumWithFallback {
    VALUE1,
    VALUE2,
    @JsonDefaultValue
    VALUE3,
    VALUE4,
    VALUE5;
}
