package se.ansman.kotshi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonWriter

/**
 * Annotation to be placed on classes that Kotshi should generate a [JsonAdapter] for.
 *
 * The annotation should only be placed on Kotlin data classes or Kotlin enums.
 *
 * To customize the name of a property when serialized, use the [JsonProperty] annotation on the property or enum
 * instance;
 *
 * [JsonQualifier] is supported and so is the [Json] annotation. They can be placed on either the property field or the
 * property parameter.
 *
 * Example:
 * ```
 * @JsonSerializable
 * data class Person(
 *     val name: String,
 *     val email: String?,
 *     // This property uses a custom getter name which requires two annotations
 *     @get:JvmName("hasVerifiedAccount") @Getter("hasVerifiedAccount")
 *     val hasVerifiedAccount: Boolean,
 *     // This property has a different name in the Json than here so @Json must be applied
 *     @JsonProperty(name = "sign_up_date")
 *     val signUpDate: Date,
 *     // This field has a json qualifier applied, the generated adapter will request an
 *     // adapter with the qualifier.
 *     @NullIfEmpty
 *     val jobTitle: String?
 * )
 * ```
 *
 * @param useAdaptersForPrimitives A flag to enable/disable the use of adapters to read and write primitive values.
 *                                 The default value is the same as [KotshiJsonAdapterFactory.useAdaptersForPrimitives].
 *                                 If you don't actually need it, it's better to not use adapters for performance reasons.
 * @param serializeNulls Enable or disable [null serialization][JsonWriter.serializeNulls] for this adapter and child
 *                       adapters (unless they override it).
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonSerializable(
    val useAdaptersForPrimitives: PrimitiveAdapters = PrimitiveAdapters.DEFAULT,
    val serializeNulls: SerializeNulls = SerializeNulls.DEFAULT
)