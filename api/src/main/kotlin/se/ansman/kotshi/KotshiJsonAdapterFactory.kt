package se.ansman.kotshi

/**
 * Annotation to be applied on an abstract class that implements
 * [JsonAdapter.Factory][com.squareup.moshi.JsonAdapter.Factory] to tell Kotshi to generate a factory containing
 * all adapters in this module.
 *
 * Example:
 * ```kotlin
 * @KotshiJsonAdapterFactory
 * abstract class ApplicationJsonAdapterFactory : JsonAdapter.Factory {
 *     companion object {
 *         val INSTANCE = KotshiApplicationJsonAdapterFactory()
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class KotshiJsonAdapterFactory