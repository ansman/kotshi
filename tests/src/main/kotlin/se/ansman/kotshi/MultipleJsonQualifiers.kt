package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier

@JsonQualifier
annotation class WrappedInObject

@JsonQualifier
annotation class WrappedInArray

@JsonSerializable
data class MultipleJsonQualifiers(@WrappedInObject @WrappedInArray val string: String)