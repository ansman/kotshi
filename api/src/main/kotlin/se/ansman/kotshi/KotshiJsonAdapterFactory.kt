package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter

/**
 * Annotation to be applied on an abstract class that implements
 * [JsonAdapter.Factory] to tell Kotshi to generate a factory containing
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
 * @param serializeNulls Enable or disable [null serialization][JsonWriter.serializeNulls] for adapters in this factory.
 *                       Only applied to adapters that don't specify an explicit value.
 *
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class KotshiJsonAdapterFactory(
    val useAdaptersForPrimitives: Boolean = false,
    val serializeNulls: SerializeNulls = SerializeNulls.DEFAULT
)