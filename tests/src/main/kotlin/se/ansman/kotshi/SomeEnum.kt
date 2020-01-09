package se.ansman.kotshi

import com.squareup.moshi.Json

@JsonSerializable
enum class SomeEnum {
    VALUE1,
    VALUE2,
    @Json(name = "VALUE3-alt")
    VALUE3,
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
    VALUE5
}
