package se.ansman.kotshi

import com.squareup.moshi.Json

/**
 * Can be added to a data class property or enum instance to specify the name that is used when serializing it to and
 * from JSON.
 *
 * This is useful when integrating with an API that uses snake_case for example.
 *
 * Using this is equivalent to using the [Json] annotation with the benefit that this annotation can be stripped using
 * code shrinkers because of its retention.
 *
 * Example:
 * ```
 * @JsonSerializable
 * data class User(
 *   @JsonProperty(name = "full_name")
 *   val fullName: String,
 * )
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@ExperimentalKotshiApi
annotation class JsonProperty(val name: String)
