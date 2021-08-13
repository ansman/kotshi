package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import kotlin.reflect.KClass

@JsonQualifier
annotation class QualifierWithArgumentsArray(vararg val args: KClass<out Annotation>)