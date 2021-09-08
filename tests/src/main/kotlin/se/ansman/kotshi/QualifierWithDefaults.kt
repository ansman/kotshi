package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier


@JsonQualifier
annotation class QualifierWithDefaults(
    vararg val vararg: String = ["vararg"],
    val string: String = "Hello"
)