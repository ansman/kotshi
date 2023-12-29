package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> Moshi.adapter(): JsonAdapter<T> = adapter(typeOf<T>().javaType)