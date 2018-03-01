package se.ansman.kotshi

import com.squareup.moshi.Json

@JsonSerializable
data class ClassWithJavaKeyword(
    @GetterName("getDefault")
    @Json(name = "default")
    val default: Boolean,
    @GetterName("getInt")
    @Json(name = "int")
    val int: Int,
    @GetterName("someCase")
    @Json(name = "case")
    @get:JvmName("someCase")
    val case: Int
)