package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier

@JsonQualifier
annotation class QualifierWithDefaults(val value: String = "n/a")
