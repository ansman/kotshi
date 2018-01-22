package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

abstract class NullSafeJsonAdapter<T: Any>(private val toString: String) : JsonAdapter<T>() {
    override fun toString(): String = toString
}