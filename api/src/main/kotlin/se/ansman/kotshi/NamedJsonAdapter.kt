package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

/**
 * A [JsonAdapter] that implements `toString`. This class is here to avoid adding one extra method per generated
 * adapter.
 *
 * This class should not be considered part of the Kotshi's public API and can change at at any time without notice.
 *
 * @param T The type that this adapter can serialize and deserialize. Cannot be nullable.
 * @param toString The value that should be returned from [toString].
 */
abstract class NamedJsonAdapter<T: Any> protected constructor(private val toString: String) : JsonAdapter<T>() {
    final override fun toString(): String = toString
}