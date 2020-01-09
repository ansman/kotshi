package se.ansman.kotshi

/**
 * Annotation to be placed on enum an enum value to indicate that specifies it as the default value
 * if the enum value is invalid.
 *
 * Only one value can be annotated.
 *
 * If no entry is annotated the adapter will throw an exception if an unknown constant is encountered.
 *
 * The annotation should only be placed on Kotlin data classes.
 * [JsonQualifiers][com.squareup.moshi.JsonQualifier] are supported and so is the [Json][com.squareup.moshi.Json]
 * annotation. They can be placed on either the property field or the property parameter.
 *
 * Example:
 * ```
 * @JsonSerializable
 * enum class SomeEnum {
 *     @Json(name = "some-value")
 *     SOME_VALUE,
 *     @Json(name = "some-other-value")
 *     SOME_OTHER_VALUE,
 *     @JsonDefault
 *     UNKNOWN
 * }
 * ```
 */
@Target(AnnotationTarget.FIELD)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonDefaultValue(
    val useAdaptersForPrimitives: PrimitiveAdapters = PrimitiveAdapters.DEFAULT
)