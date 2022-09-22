package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier

/**
 * Registers the annotated type in the Kotshi json adapter factory. All registered adapters take priority over all
 * generated adapters.
 *
 * The annotated type must be either an object or a class and must extend [JsonAdapter].
 * It can also be a generic class in which case you can accept an array of types for the actual arguments as well as
 * an instance of Moshi itself, however both are optional.
 *
 * If the type is annotated with a [JsonQualifier] then the adapter will only handle requests with that qualifier.
 *
 * Example:
 * ```
 * // Registers this adapter for the type `Instant`.
 * @RegisterJsonAdapter
 * object InstantAdapter : JsonAdapter<Instant>() {
 *   ...
 * }
 *
 * // Registers this adapter for the type `Instant` with the qualifier ISO8601.
 * @JsonQualifier
 * annotation class ISO8601
 *
 * @RegisterJsonAdapter
 * @ISO8601
 * object InstantAsISO8601Adapter : JsonAdapter<Instant>() {
 *   ...
 * }
 *
 * // Registers the adapter for the generic type `GenericType<T>`.
 * @RegisterJsonAdapter
 * class GenericAdapter<T>(types: Array<Type>, moshi: Moshi) : JsonAdapter<GenericType<T>>() {
 *   private val delegateAdapter = moshi.adapter<T>(types[0])
 *   ...
 * }
 * ```
 *
 * @param priority The priority of the adapter compared to other registered adapters. The type checks will be sorted by
 *                 the priority in descending order. This is useful if there are overlaps in types (for example if one
 *                 adapter handles `List<String>` and the other `List<T>`). The default priority is 0.
 *                 Adapters with the same priority is sorted deterministically, but arbitrarily.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@ExperimentalKotshiApi
annotation class RegisterJsonAdapter(
    val priority: Int = 0,
)