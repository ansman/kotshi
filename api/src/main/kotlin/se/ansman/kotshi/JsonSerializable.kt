package se.ansman.kotshi

/**
 * Annotation to be placed on classes that Kotshi should generate [JsonAdapters][com.squareup.moshi.JsonAdapter] for.
 *
 * The annotation should only be placed on Kotlin data classes.
 * [JsonQualifiers][com.squareup.moshi.JsonQualifier] are supported and so is the [Json][com.squareup.moshi.Json]
 * annotation. They can be placed on either the property field or the property parameter.
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
 *     @Json(name = "sign_up_date")
 *     val signUpDate: Date,
 *     // This field has a json qualifier applied, the generated adapter will request an adapter with the qualifier.
 *     @NullIfEmpty
 *     val jobTitle: String?
 * )
 * ```
 *
 * @param useAdaptersForPrimitives A flag to enable/disable the use of adapters to read and write primitive values.
 *                                 The default value is the same as [KotshiJsonAdapterFactory.useAdaptersForPrimitives].
 *                                 If you don't actually need it it's better to not use adapters for performance reasons.
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonSerializable(
    val useAdaptersForPrimitives: PrimitiveAdapters = PrimitiveAdapters.DEFAULT
)