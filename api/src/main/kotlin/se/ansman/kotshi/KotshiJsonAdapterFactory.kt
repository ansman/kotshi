package se.ansman.kotshi

/**
 * Annotation to be applied on an abstract class that implements
 * [JsonAdapter.Factory][com.squareup.moshi.JsonAdapter.Factory] to tell Kotshi to generate a factory containing
 * all adapters in this module.
 *
 * Example:
 * ```kotlin
 * @KotshiJsonAdapterFactory
 * object ApplicationJsonAdapterFactory : JsonAdapter.Factory by KotshiApplicationJsonAdapterFactory
 * ```
 *
 * @param useAdaptersForPrimitives A flag to enable or disable the use of adapters on a module basis. Since using
 *                                 adapters is worse for performance don't enable it unless you need it. This flag can
 *                                 be overridden in each adapter using [JsonSerializable.useAdaptersForPrimitives].
 *
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class KotshiJsonAdapterFactory(
    val useAdaptersForPrimitives: Boolean = false
)