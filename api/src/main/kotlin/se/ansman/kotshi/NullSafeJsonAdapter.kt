package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

abstract class NullSafeJsonAdapter<T: Any> protected constructor(private val toString: String) : JsonAdapter<T>() {
    final override fun toString(): String = toString
}